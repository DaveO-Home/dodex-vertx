package golf.handicap.hibernate.db

import com.fasterxml.jackson.databind.ObjectMapper
import golf.handicap.Score
import golf.handicap.db.SqlConstants
import golf.handicap.hibernate.db.SqlConstants.Companion.UPDATE_GOLFER
import golf.handicap.hibernate.db.SqlConstants.Companion.UPDATE_SCORE
import golf.handicap.hibernate.entities.PersistedScores
import jakarta.persistence.NoResultException
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.transaction.Transactional
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.query.MutationQuery
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class PopulateScore : SqlConstants(), IPopulateScore {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(PopulateScore::class.java.name)
    }

    @Transactional
    override fun setScore(
        score: Score,
        sessionFactory: SessionFactory
    ): String {
        val em = sessionFactory.createEntityManager()

        val scores = getScoreByTeetime(score, em)

        if (scores.isEmpty()) {
            val persistedScores = buildScore(score)
            val transaction = em.transaction
            try {
                transaction.begin()
                em.persist(persistedScores)
                em.flush()
                transaction.commit()
            } catch (e: Exception) {
                transaction.rollback()
                e.printStackTrace()
                score.status = -1
                score.message = "Persist Score Failed"
            } finally {
                em.close()
            }
        } else { //if(scores.size == 1) {
            em.close()
            updateScore(score, sessionFactory)
        }
        val jsonMapper = ObjectMapper()
        val jsonData = jsonMapper.writeValueAsString(score)
        return jsonData
    }

    private fun getScoreByTeetime(score: Score, em: Session): MutableSet<Score> {
        val scores = mutableSetOf<Score>()
        val builder = em.criteriaBuilder
        val scoreCriteria: CriteriaQuery<PersistedScores> = builder.createQuery(PersistedScores::class.java)
        val root = scoreCriteria.from(PersistedScores::class.java)

        val pinPredicate = builder.equal(root.get<PersistedScores.ScoresId>("pin"), score.golfer!!.pin)
        val teeTimePredicate = builder.equal(
            root.get<PersistedScores>("teeTime"),
            score.teeTime?.let { LocalDateTime.parse(it) })
        val coursePredicate = builder.equal(root.get<PersistedScores.ScoresId>("courseSeq"), score.course!!.course_key)
//        val timePredicate = builder.equal(root.get<PersistedScores.ScoresId>("teeTime"), score.teeTime)

        scoreCriteria.where(
            pinPredicate,
            builder.and(teeTimePredicate),
            builder.and(coursePredicate),
            builder.and(teeTimePredicate)
        )
        try {
            val queryScores = em.createQuery(scoreCriteria).resultList
            for (queryScore in queryScores) {
                val newScore = Score()
                newScore.golfer = golf.handicap.Golfer()
                newScore.course = score.course
                newScore.golfer!!.pin = queryScore.scoresId!!.pin
                newScore.grossScore = queryScore.grossScore!!
                newScore.netScore = queryScore.netScore!!
                newScore.adjustedScore = queryScore.adjustedScore!!
                newScore.teeTime = queryScore.teeTime.toString()
                newScore.handicap = queryScore.handicap!!
                newScore.golfer!!.handicap = queryScore.handicap!!
                newScore.course!!.course_key = queryScore.course!!.courseSeq
                newScore.course!!.teeId = queryScore.courseTees!!
                newScore.tees = queryScore.courseTees!!.toString()
                scores.add(newScore)
            }
        } catch (nre: NoResultException) {
            LOGGER.info(nre.message)
        } catch (e: Exception) {
            LOGGER.error(e.message)
        }

        return scores
    }

    private fun buildScore(score: Score): PersistedScores {
        val persistedScores = PersistedScores()

        persistedScores.grossScore = score.grossScore
        persistedScores.netScore = score.netScore
        persistedScores.adjustedScore = score.adjustedScore
        persistedScores.pin = score.golfer!!.pin
        persistedScores.courseSeq = score.course!!.course_key
        persistedScores.courseTees = score.course!!.teeId
        persistedScores.handicap = score.handicap
        persistedScores.teeTime = score.teeTime?.let { LocalDateTime.parse(it) }
        val scoreId =
            PersistedScores.ScoresId(score.golfer!!.pin!!, score.course!!.course_key, persistedScores.teeTime!!)
        persistedScores.scoresId = scoreId
        persistedScores.used = persistedScores.used ?: ' '
        return persistedScores
    }

    @Transactional
    private fun updateScore(score: Score, sessionFactory: SessionFactory): Score {
        val em = sessionFactory.createEntityManager()
        try {
            em.transaction.begin()

            val query = scoreUpdateQuery(score, em)
            query.executeUpdate()

            em.flush()
            em.transaction.commit()
            em.close()

            updateGolfer(score, sessionFactory)

        } catch (e: Exception) {
            em.transaction.rollback()
            e.printStackTrace()
            score.status = -1
            score.message = "Update Score/Golfer Failed"
        } finally {
            em.close()
        }

        return score
    }

    private fun scoreUpdateQuery(score: Score, em: Session): MutationQuery {
        val teeTime = score.teeTime?.let { LocalDateTime.parse(it) }

        return em.createMutationQuery(UPDATE_SCORE)
            .setParameter("net", score.netScore)
            .setParameter("gross", score.grossScore)
            .setParameter("adjusted", score.adjustedScore)
            .setParameter("handicap", score.handicap)
            .setParameter("course", score.course?.course_key)
            .setParameter("pin", score.golfer?.pin)
            .setParameter("tee", score.course?.teeId)
            .setParameter("time", teeTime)
    }

    @Transactional
    private fun updateGolfer(score: Score, sessionFactory: SessionFactory): Score {
        try {
            val em = sessionFactory.createEntityManager()
            var pin = ""
            score.golfer?.pin.let { pin = it!! }

            em.transaction.begin()

            val query =
                em.createMutationQuery(UPDATE_GOLFER)
                    .setParameter(1, score.golfer!!.overlap)
                    .setParameter(2, score.golfer!!.public)
                    .setParameter(3, score.golfer!!.pin)
            query.executeUpdate()
            em.flush()
            em.transaction.commit()
            em.close()
        } catch (e: Exception) {
            score.status = -1
            score.message = "Update Golfer failed"
            e.printStackTrace()
            throw Exception("Update Golfer Failed: " + e.cause)
        }

        return score
    }
}
