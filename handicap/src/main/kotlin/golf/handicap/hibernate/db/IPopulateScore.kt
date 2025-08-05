package golf.handicap.hibernate.db

import golf.handicap.Score
import org.hibernate.SessionFactory

interface IPopulateScore {
    fun setScore(score: Score, sessionFactory: SessionFactory): String
}