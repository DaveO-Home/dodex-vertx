package golf.handicap

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

    fun getCourses(connection: Connection?, courseState: String?): List<Course>? {
        if(connection == null) {
            println("$connection : $courseState")
        }
        return null
    }
}