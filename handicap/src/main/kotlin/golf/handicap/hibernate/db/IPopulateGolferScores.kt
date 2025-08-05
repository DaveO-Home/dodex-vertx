package golf.handicap.hibernate.db

import golf.handicap.Golfer
import org.hibernate.SessionFactory

interface IPopulateGolferScores {
    @Throws(Exception::class)
    fun getGolferScores(golfer: Golfer, rows: Int, sessionFactory: SessionFactory): Map<String, Any?>
    @Throws(Exception::class)
    fun removeLastScore(golferPIN: String?, sessionFactory: SessionFactory): String
    @Throws(Exception::class)
    fun setGolferHandicap(golfer: Golfer, sessionFactory: SessionFactory): Int
}