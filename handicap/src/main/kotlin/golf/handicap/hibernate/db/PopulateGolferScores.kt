package golf.handicap.hibernate.db

import golf.handicap.Golfer
import golf.handicap.hibernate.entities.PersistedRatings
import golf.handicap.hibernate.entities.PersistedScores
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import jakarta.persistence.EntityManager
import jakarta.persistence.NoResultException
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaDelete
import jakarta.persistence.criteria.Root
import org.hibernate.SessionFactory
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.SQLException
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.Year
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*


class PopulateGolferScores : SqlConstants(), IPopulateGolferScores {
    private val teeDate = SimpleDateFormat("yyyy-MM-dd")
    private val teeYear = SimpleDateFormat("yyyy")
    private var beginDate: String? = null
    private var endDate: String? = null
    private var maxRows = 20
    private var gettingData = false
    private var beginGolfDate: Date? = null
    private var endGolfDate: Date? = null
    private var overlapYears = false

    companion object {
        private val LOGGER = LoggerFactory.getLogger(PopulateGolferScores::class.java.name)
    }

    private fun setPeriod(year: String) {
        beginDate = "$year-01-01"
        endDate = (year.toInt() + 1).toString() + "-01-01"
        beginGolfDate = beginDate?.let { teeDate.parse(it, ParsePosition(0)) }
        endGolfDate = endDate?.let { teeDate.parse(it, ParsePosition(0)) }
    }

    fun useCurrentYearOnly(thisYearOnly: Boolean) {
        overlapYears = thisYearOnly
    }

    override fun getGolferScores(golfer: Golfer, rows: Int, sessionFactory: SessionFactory): Map<String, Any?> {
        val em = sessionFactory.createEntityManager()
        val oldRows = this.maxRows
        this.maxRows = rows
        gettingData = true

        val mapData = getGolferScores(golfer, em)

        gettingData = false
        this.maxRows = oldRows

        return mapData
    }

    @Throws(Exception::class)
    fun getGolferScores(golfer: Golfer, em: EntityManager): Map<String, Any?> {
        val tableMap: MutableMap<String, Any> = HashMap()

        val golferPin = golfer.pin
        val previousYear = Year.now().value - 1
        var isCalcHandicap = false
        overlapYears = golfer.overlap
        beginDate = if (overlapYears) "01-01-$previousYear" else beginDate

        val query = if (gettingData) {
            if (golferPin.isNullOrEmpty()) {
                em.createQuery(
                    GOLFER_SCORES.replace(
                        ":R", "golfer.firstName = :first and golfer.lastName = :last and " +
                                "golfer.publicDisplay and "
                    ),
                    PersistedScores::class.java
                )
                    .setMaxResults(maxRows)
                    .setParameter("first", golfer.firstName)
                    .setParameter("last", golfer.lastName)
            } else {
                em.createQuery(
                    GOLFER_SCORES.replace(":R", "golfer.pin = :pin and "), PersistedScores::class.java
                )
                    .setMaxResults(maxRows)
                    .setParameter("pin", golfer.pin)
            }
        }
        /* Used to calculate golfer's handicap */
        else {
            isCalcHandicap = true

            em.createQuery(
                HANDICAP_DATA, PersistedScores::class.java
            ).setMaxResults(maxRows)
                .setParameter("pin", golfer.pin)
        }

        val scoreObjects = query
            .setParameter("begin", dateToLocalDateTime(beginGolfDate!!))
            .setParameter("end", dateToLocalDateTime(endGolfDate!!))
            .resultList

        val tableArray = JsonArray()
        scoreObjects.forEach { score ->
            val tableObject = JsonObject()
            var persistedRating = PersistedRatings()

            if (isCalcHandicap) {
                val ratingSet: Set<PersistedRatings> = score.course?.ratings!!

                for (rating in ratingSet) {
                    if (score.courseTees == rating.tee) {
                        persistedRating = rating
                    }
                }
            }

            if (isCalcHandicap) {
                tableObject.put("PIN", score.pin)
                    .put("TEE_RATING", persistedRating.teeRating.toString())
                    .put("TEE_SLOPE", persistedRating.teeSlope.toString())
                    .put("ADJUSTED_SCORE", score.adjustedScore.toString())
                    .put("TEE_TIME", score.teeTime.toString())
                    .put("COURSE_SEQ", persistedRating.course_seq.toString())
                    .put("TEE_PAR", persistedRating.teePar.toString())
            } else {
                tableObject.put("COURSE_NAME", score.course?.courseName)
                    .put("GROSS_SCORE", score.grossScore.toString())
                    .put("NET_SCORE", BigDecimal(score.netScore.toString()).setScale(1, RoundingMode.UP))
                    .put("ADJUSTED_SCORE", score.adjustedScore.toString())
                    .put("HANDICAP", BigDecimal(score.handicap.toString()).setScale(1, RoundingMode.UP))
                    .put("COURSE_TEES", score.courseTees.toString())
                    .put(
                        "TEE_TIME",
                        if (gettingData) score.teeTime.toString().substring(0, 10) else score.teeTime.toString()
                    )
                    .put("USED", if (score.used == null) "" else score.used.toString())
            }
            tableArray.add(tableObject)
        }

        tableMap["array"] = tableArray
        return tableMap
    }

    override fun removeLastScore(golferPIN: String?, sessionFactory: SessionFactory): String {
        val em = sessionFactory.createEntityManager()
        val count: Int
        var used: String
        val query = em.createQuery(LAST_SCORE, LocalDateTime::class.java)
        query.setParameter("pin", golferPIN)

        val localDateTime: LocalDateTime?
        val transaction = em.transaction

        try {
            localDateTime = query.singleResult
            transaction.begin()

            count = deleteUsingTeeTime(golferPIN!!, localDateTime, em)

            transaction.commit()
            em.flush()

            used = count.toString()
        } catch (e: NoResultException) {
            used = "-1"
        } catch (ex: Exception) {
            used = "-1"
            transaction.rollback()
        } finally {
            em.close()
        }

        return used
    }

    private fun deleteUsingTeeTime(golferPIN: String, localDateTime: LocalDateTime, em: EntityManager): Int {
        try {
            val cb: CriteriaBuilder = em.criteriaBuilder
            val cd: CriteriaDelete<PersistedScores> = cb.createCriteriaDelete(PersistedScores::class.java)
            val scores: Root<PersistedScores> = cd.from(PersistedScores::class.java)
            cd.where(
                cb.equal(scores.get<String>("pin"), golferPIN),
                cb.and(cb.equal(scores.get<LocalDateTime>("teeTime"), localDateTime))
            )

            return em.createQuery(cd).executeUpdate()
        } finally {
            //
        }
    }

    @Throws(SQLException::class, InterruptedException::class)
    override fun setGolferHandicap(golfer: Golfer, sessionFactory: SessionFactory): Int {
        val em = sessionFactory.createEntityManager()
        em.clear()
        em.transaction.begin()

        var rowsUpdated: Int = em.createMutationQuery(UPDATE_HANDICAP)
            .setParameter("handicap", golfer.handicap)
            .setParameter("pin", golfer.pin).executeUpdate()

        val score = golfer.score ?: run {
            em.transaction.commit()
            return rowsUpdated
        }

        rowsUpdated += em.createMutationQuery(UPDATE_SCORES)
            .setParameter("handicap", golfer.handicap)
            .setParameter("net", 0.0f)
            .setParameter("seq", score.course?.course_key)
            .setParameter("pin", golfer.pin)
            .setParameter("time", score.teeTime).executeUpdate()

        em.transaction.commit()
        em.flush()
        return rowsUpdated
    }

    @Throws(Exception::class)
    fun setUsed(pin: String?, course: Int, teeTime: String, em: EntityManager): Int {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val teeDate = LocalDateTime.parse("$teeTime:00".replace("T", " "), formatter)

        val count: Int = em.createQuery(SET_USED)
            .setParameter("used", '*')
            .setParameter("seq", course)
            .setParameter("time", teeDate)
            .setParameter("pin", pin).executeUpdate()

        return count
    }

    @Throws(Exception::class)
    fun clearUsed(pin: String?, em: EntityManager): Int {
        val count: Int = em.createQuery(RESET_SCORES)
            .setParameter("used", Character.valueOf(' '))
            .setParameter("pin", pin).executeUpdate()
        em.flush()

        return count
    }

    init {
        setPeriod(teeYear.format(Date()))
    }

    private fun dateToLocalDateTime(date: Date): LocalDateTime {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault())
    }
}
