package golf.handicap.hibernate.db

abstract class SqlConstants {
    companion object {

        const val UPDATE_GOLFER = "update PersistedGolfer set overlapYears = ?1, publicDisplay = ?2 where pin = ?3"

        const val UPDATE_SCORE = "update PersistedScores set netScore = :net, grossScore = :gross, " +
                "adjustedScore = :adjusted, handicap = :handicap, courseTees = :tee " +
                "where pin = :pin and courseSeq = :course and teeTime = :time"

        const val UPDATE_GOLFER_HANDICAP = "update PersistedGolfer " +
                "set overlapYears = ?1, publicDisplay = ?2, country = ?3, state = ?4, handicap = ?5, lastLogin = ?6 " +
                "where pin = ?7"

        const val GOLFER_SCORES = "select score " +
                "from " +
                "PersistedGolfer golfer, " +
                "PersistedCourse course, " +
                "PersistedScores score where " +
                "golfer.pin = score.pin and " +
                "course.courseSeq = score.courseSeq and " +
                ":R" +
                "score.teeTime between :begin and :end " +
                "order by score.teeTime desc"

        const val HANDICAP_DATA = "select score " +
                "from " +
                "PersistedGolfer golfer, " +
                "PersistedCourse course, " +
                "PersistedScores score, " +
                "PersistedRatings rating where " +
                "golfer.pin = :pin and " +
                "golfer.pin = score.pin and " +
                "rating.tee = score.courseTees and " +
                "course.courseSeq = score.courseSeq and " +
                "course.courseSeq = rating.course_seq and " +
                "score.teeTime between :begin and :end " +
                "order by score.teeTime desc"

        const val LAST_SCORE = "SELECT score.teeTime FROM PersistedScores score where pin = :pin " +
                "ORDER BY score.teeTime DESC OFFSET 0 ROWS FETCH FIRST 1 ROW ONLY"

        const val UPDATE_HANDICAP = "update PersistedGolfer set handicap = :handicap " +
                "where pin = :pin"

        const val UPDATE_SCORES = "update PersistedScores set handicap = :handicap, net: netScore " +
                "where courseSeq = :seq and pin = :pin and teeTime = :time"

        const val RESET_SCORES = "update PersistedScores set used = :used where pin = :pin and used = '*'"

        const val SET_USED = "update PersistedScores set used = :used " +
                "where pin = :pin and courseSeq = :seq and teeTime = :time"
    }
}