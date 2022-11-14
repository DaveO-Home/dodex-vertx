package handicap

import golf.handicap.Course
import java.sql.Connection


class Courses {
    var selectedCourses: MutableList<Course>? = null
    var coursesByState: MutableMap<String, Course>? = null
    var iterator: Iterator<*>? = null
    var currentCourse: Course? = null
    fun init() {
        selectedCourses!!.clear()
        coursesByState!!.clear()
    }

    fun getCourses(connection: Connection?, course_state: String?): List<Course>? {
        if(connection == null) {
            println(connection + " : " + course_state)
        }
        return null
    }
}