/*
 * Score.java
 *
 * Created on February 17, 2005, 11:39 AM
 */
package golf.handicap

import golf.handicap.Course
import golf.handicap.Golfer
import java.io.Serializable
import java.sql.Timestamp
import java.time.LocalDateTime

class Score
    : Serializable {
    var netScore = 0.0.toFloat()
    var handicap = 0.0.toFloat()
    var grossScore = 0
    var adjustedScore = 0
    var teeTime: String? = null
    var message: String? = null
    var tees: String? = null
    var course: Course? = null
    var golfer: Golfer? = null
    var status = 0
    var scoreId = 0
    fun init() {
        adjustedScore = 0
        grossScore = adjustedScore
        handicap = 0.0.toFloat()
        netScore = handicap
        teeTime = null
        tees = null
        message = tees
        course = null
        golfer = null
        status = adjustedScore
        scoreId = adjustedScore
    }
}