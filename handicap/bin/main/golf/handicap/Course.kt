package golf.handicap

import java.io.Serializable
import java.util.*

class Course : Serializable {
  var course_par = 0
  var course_key = 0
  var ratingsCount = 0
  var course_name: String? = null
  var course_country: String? = null
  var course_state: String? = null
  var message: String? = null
  var rating: Rating? = null
  var teeId = 0

  private val ratings: MutableMap<String, Rating> = HashMap<String, Rating>()
  private var iterator: Iterator<*>? = null
  fun init() {
    ratingsCount = 0
    course_key = ratingsCount
    course_par = course_key
    message = null
    course_state = message
    course_name = course_state
    course_country = course_name
    ratings.clear()
    iterator = null
    rating = null
  }

  fun setRating(
      courseKey: Int,
      courseRating: String,
      courseSlope: Int,
      coursePar: Int,
      courseTee: Int,
      courseColor: String
  ) {
    ratings[courseTee.toString()] =
        Rating(courseKey, courseRating, courseSlope, coursePar, courseTee, courseColor)
  }

  val par: Int
    get() = if (rating != null) rating!!.par else 0

  fun getRating(): String? {
    return if (rating != null) rating!!.getRating() else null
  }

  fun getColor(): String? {
    return if (rating != null) rating!!.color else null
  }

  val slope: Int
    get() = if (rating != null) rating!!.slope else 0
  val tee: Int
    get() = if (rating != null) rating!!.tee else 0
  val key: Int
    get() = if (rating != null) rating!!.key else 0

  fun findRating(tees: String?): Int {
    rating = ratings[tees]
    return if (rating == null) -1 else 1
  }

  fun resetIterator() {
    iterator = null
  }

  fun findNextRating(): Int {
    try {
      if (iterator == null) iterator = ratings.keys.iterator()
      rating = ratings[iterator!!.next() as String?]
    } catch (n: NoSuchElementException) {
      message = n.message
      iterator = null
      return -1
    }
    return ratingsCount++
  }

  var courseName: String?
    get() = course_name
    set(course_name) {
      this.course_name = course_name
    }

  var courseCountry: String?
    get() = course_country
    set(course_country) {
      this.course_country = course_country
    }
  var courseState: String?
    get() = course_state
    set(course_state) {
      this.course_state = course_state
    }
  var courseKey: Int
    get() = course_key
    set(course_key) {
      this.course_key = course_key
    }

  inner class Rating : Serializable {
    constructor() {}
    constructor(
        courseKey: Int,
        courseRating: String,
        courseSlope: Int,
        coursePar: Int,
        courseTee: Int,
        color: String
    ) {
      this.courseRating = courseRating
      this.slope = courseSlope
      this.par = coursePar
      tee = courseTee
      this.key = courseKey
      this.color = color
    }

    fun getRating(): String? {
      return courseRating
    }

    var courseRating: String? = null
    var slope = 0
    var par = 0
    var key = 0
    var tee = 0
    var color: String? = null
  }
}
