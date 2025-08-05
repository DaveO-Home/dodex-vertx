package golf.handicap.hibernate

import golf.handicap.Golfer
import golf.handicap.Score
import golf.handicap.hibernate.db.PopulateGolferScores
import golf.handicap.hibernate.entities.PersistedRatings
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.hibernate.SessionFactory
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class Handicap {
    companion object {
    }

    private val LOGGER = LoggerFactory.getLogger(Handicap::class.java.name)
    private var diffkeys = Array(20) { " " }
    private var diffScores = FloatArray(20) { -100f }
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private var usedClass: Used? = null
    var dateTime: Date? = null
    var index = intArrayOf(0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 8, 9, 10)
    var multiple = .96.toFloat()
    var used: MutableMap<String, Used> = HashMap<String, Used>()
    var key: String = String()
    private var score: Score = Score()

    fun getHandicap(golfer: Golfer, sessionFactory: SessionFactory): MutableMap<String, Any> {
        val em = sessionFactory.createEntityManager()

        val averageSlope = 113
        val golferScores = PopulateGolferScores()

        var numberOfScores = -1
        var adjusted: Float
        var rating: Float
        var slope: Float
        var difference: Float
        var handicapDifferential: Float
        var handicap = BigDecimal(0.0)
        val latestTee: MutableMap<String, Any> = HashMap()

        val handicapData: Map<String, Any?>?
        val transaction = em.transaction
        try {
            handicapData = golferScores.getGolferScores(golfer, em)

            if (((handicapData["array"] ?: JsonArray()) as JsonArray).isEmpty) {
                latestTee["handicap"] = 0.0f
            } else {
                val objects = ((handicapData["array"] ?: JsonArray()) as JsonArray).iterator()
                var i = 0

                do {
                    val jsonObject: JsonObject = objects.next() as JsonObject

                    adjusted = jsonObject.getString("ADJUSTED_SCORE").toFloat()
                    rating = jsonObject.getString("TEE_RATING").toFloat()
                    slope = jsonObject.getString("TEE_SLOPE").toFloat()
                    difference = adjusted - rating
                    handicapDifferential = averageSlope * difference / slope

                    // Used to calculate Net Score
                    if (i == 0) {
                        latestTee["adjusted"] = adjusted
                        latestTee["rating"] = rating
                        latestTee["slope"] = slope
                        latestTee["par"] = jsonObject.getString("TEE_PAR").toInt()
                    }

                    var foundZero = false
                    for (x in 0..19) {
                        if (diffScores[x] == -100.0f) {
                            diffScores[x] = handicapDifferential
                            foundZero = true
                            break
                        }
                    }

                    if (!foundZero && handicapDifferential < diffScores[19]) {
                        diffScores[19] = handicapDifferential
                    }

                    Arrays.sort(diffScores)

                    val localDateTime = LocalDateTime.parse(jsonObject.getString("TEE_TIME"))
                    key = handicapDifferential.toString() + '\t' + formatter.format(
                        localDateTime
                    )
                    key = if (key.indexOf('.') == 1) "0$key" else key
                    diffkeys[i] = key
                    used[key] = Used(
                        jsonObject.getString("PIN"),
                        jsonObject.getString("COURSE_SEQ").toInt(),
                        jsonObject.getString("TEE_TIME")
                    )
                    if (numberOfScores < 19) numberOfScores++
                    i++
                } while (objects.hasNext())

                Arrays.sort(diffkeys)
                handicap = calculateHandicap(numberOfScores)
                golfer.handicap = handicap.toFloat()

                val scoresUpdatedCount = golferScores.setGolferHandicap(golfer, sessionFactory)
                latestTee["count"] = scoresUpdatedCount

                transaction.begin()

                var rowsUpdated = 0
                usedClass = used[diffkeys[19]]
                if (usedClass == null) {
                    latestTee["handicap"] = handicap.toFloat()
                } else {
                    golferScores.clearUsed(usedClass!!.pin, em)
                }
                val diffIterator = diffkeys.reversed().toMutableList().iterator()

                while (diffIterator.hasNext()) {
                    val diffKey = diffIterator.next()
                    usedClass = used[diffKey]
                    if (usedClass != null && usedClass!!.used && diffKey != "") {
                        rowsUpdated += golferScores.setUsed(
                            usedClass!!.pin,
                            usedClass!!.course!!,
                            usedClass!!.teeTime,
                            em
                        )
                    }
                }

                latestTee["handicap"] = handicap.toFloat()

                em.flush()
                transaction.commit()
            }
        } catch (ex: Exception) {
            latestTee["handicap"] = handicap.toFloat()
            transaction.rollback()
            ex.printStackTrace()
        } finally {
            em.close()
        }

        return latestTee;
    }

    private fun getRating(score: Score, em: EntityManager): PersistedRatings {
        if (score.course == null) {
            return PersistedRatings();
        }
        val builder = em.criteriaBuilder

        val query: CriteriaQuery<PersistedRatings> = builder.createQuery(PersistedRatings::class.java)
        val root: Root<PersistedRatings> = query.from(PersistedRatings::class.java)
        val courseId: Predicate = builder.equal(root.get<PersistedRatings>("course_seq"), score.course!!.course_key)
        val courseTee: Predicate = builder.equal(root.get<PersistedRatings>("tee"), score.course!!.teeId)
        query.where(
            courseId,
            builder.and(courseTee)
        )
        return em.createQuery(query).singleResult
    }

    private fun calculateHandicap(numberOfScores: Int): BigDecimal {
        if (diffScores[15] == -100.0f) return BigDecimal(0.0)
        var total = 0.0.toFloat()
        var i = 0
        val j = index[numberOfScores]

        while (i < j) {
            total += diffScores[19 - numberOfScores + i]
            setUsed(diffkeys[19 - numberOfScores + i])
            i++
        }
        total = total / index[numberOfScores] * multiple
        return BigDecimal(total.toString()).setScale(1, RoundingMode.DOWN)
    }

    private fun setUsed(diffkey: String) {
        usedClass = used[diffkey]
        if (usedClass != null) {
            usedClass!!.used = true
            used[diffkey] = usedClass as Used
        }
    }

    fun setScore(score: Score) {
        this.score = score
    }

    inner class Used(var pin: String?, var course: Int?, var teeTime: String) {
        var used = false
        override fun toString(): String {
            return StringBuffer().append(pin).append(" ").append(course).append(" ")
                .append(teeTime).append(" ").append(used)
                .toString()
        }
    }
}