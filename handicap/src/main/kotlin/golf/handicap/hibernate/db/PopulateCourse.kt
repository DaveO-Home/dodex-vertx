package golf.handicap.hibernate.db

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import golf.handicap.hibernate.entities.PersistedCourse
import golf.handicap.hibernate.entities.PersistedRatings
import handicap.grpc.Course.Builder
import handicap.grpc.Course.newBuilder
import handicap.grpc.HandicapData
import handicap.grpc.ListCoursesResponse
import handicap.grpc.Rating
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import jakarta.persistence.EntityTransaction
import jakarta.persistence.NoResultException
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.transaction.Transactional
import org.hibernate.SessionFactory
import org.slf4j.LoggerFactory
import java.util.*


class PopulateCourse : IPopulateCourse {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(PopulateCourse::class.java.name)
    }

    @Transactional
    override fun getCourseWithTee(
        courseMap: HashMap<String, Any>,
        sessionFactory: SessionFactory
    ): HandicapData? {
        var message = "Success"
        val em = sessionFactory.createEntityManager()
        val builder = em.criteriaBuilder

        val query: CriteriaQuery<PersistedCourse> = builder.createQuery(PersistedCourse::class.java)
        val root: Root<PersistedCourse> = query.from(PersistedCourse::class.java)

        val predicates: Array<Predicate?> = arrayOfNulls(3)
        val keys: HashMap<Int, String> = hashMapOf(0 to "courseName", 1 to "country", 2 to "state")

        courseMap.forEach { (key, value) ->
            keys.forEach { (key2, value2) ->
                if (key == value2) {
                    val theField: String = if (!key.contains("course")) {
                        "course" + key.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault())
                            else it.toString()
                        }
                    } else {
                        key
                    }

                    predicates[key2] = builder.equal(root.get<PersistedCourse>(theField), value)
                }
            }
        }

        query.where(
            predicates[0],
            builder.and(predicates[1]),
            builder.and(predicates[2])
        )

        var courseRatings = PersistedCourse()
        try {
            courseRatings = em.createQuery(query).singleResult
            // check for ratings in courseRatings
            var isNewRating = true
            courseRatings.ratings.forEach { rating ->
                if (rating.tee == courseMap["tee"] as Int) {
                    if ((courseMap["rating"] as String).toFloat() != rating.teeRating ||
                        courseMap["slope"] as Int != rating.teeSlope ||
                        courseMap["par"] as Int != rating.teePar ||
                        courseMap["color"] as String != rating.teeColor
                    ) {
                        throw (NoResultException("update"))
                    } else {
                        isNewRating = false
                    }
                }
            }

            if (isNewRating) {
                throw (NoResultException("new"))
            }
        } catch (nre: NoResultException) {
            val persistedCourse = buildCourse(courseMap)
            val transaction: EntityTransaction = em.transaction
            try {
                transaction.begin()

                when (nre.message) {
                    "new" -> {
                        val ratingsSet = newRating(courseMap, courseRatings)
                        courseRatings.ratings = ratingsSet

                        em.merge(courseRatings)
                    }
                    "update" -> {
                        courseRatings.ratings = updateRating(courseMap, courseRatings)
                        em.merge(courseRatings)
                    }
                    else -> {
                        em.persist(persistedCourse)
                        /* Ratings ID is embedded - need new Course_Seq value to set RatingId */

                        persistedCourse.ratings = buildRating(courseMap, persistedCourse)
                        em.persist(persistedCourse)
                    }
                }
                em.flush()
                transaction.commit()
                courseMap["status"] = 0
            } catch (e: Exception) {
                LOGGER.info("Error Persisting Course/Rating: " + e.message)
                courseMap["status"] = -1
                message = e.message as String
                transaction.rollback()
            } finally {
                em.close()
            }
        } finally {
            if (em.isOpen) {
                em.close()
            }
        }

        val jsonString = JsonObject(courseMap).toString()

        return HandicapData.newBuilder()
            .setMessage(message)
            .setCmd(2)
            .setJson(jsonString)
            .build()
    }

    override fun listCourses(state: String, sessionFactory: SessionFactory): ListCoursesResponse.Builder {
        val coursesBuilder = ListCoursesResponse.newBuilder()
        val em = sessionFactory.createEntityManager()
        val builder = em.criteriaBuilder
        val query: CriteriaQuery<PersistedCourse> = builder.createQuery(PersistedCourse::class.java)
        val root: Root<PersistedCourse> = query.from(PersistedCourse::class.java)

        query.where(builder.equal(root.get<PersistedCourse>("courseState"), state))

        val ratingTees: Array<Int> = arrayOf(-1, -1, -1, -1, -1)
        val courses: List<PersistedCourse>?
        var courseBuilder: Builder? = null

        try {
            courses = em.createQuery(query).resultList

            courses.forEach { course ->
                courseBuilder = newBuilder()
                courseBuilder!!.id = course.courseSeq
                courseBuilder!!.name = course.courseName
                course.ratings.forEach { rating ->
                    val ratingBuilder =
                        Rating.newBuilder()
                            .setRating(rating.teeRating.toString())
                            .setSlope(rating.teeSlope!!)
                            .setTee(rating.tee!!)
                            .setColor(rating.teeColor)
                            .setPar(rating.teePar!!)
                    courseBuilder.addRatings(ratingBuilder)
                    ratingTees[rating.tee!!] = rating.tee!! // which tees have been added
                }
                if (coursesBuilder.isInitialized) {
                    // Generating placeholders for tees not defined
                    setUndefinedTees(ratingTees, courseBuilder!!)
                    coursesBuilder.addCourses(courseBuilder)
                }
            }
            if (courseBuilder == null) {
                coursesBuilder.addCourses(newBuilder())
            }
        } catch (nre: NoResultException) {
            LOGGER.error("Query Courses Error: " + nre.message)
        } finally {
            if (em.isOpen) {
                em.close()
            }
        }

        return coursesBuilder
    }

    override fun getCourses(state: String, sessionFactory: SessionFactory): String {
        val coursesBuilder = ListCoursesResponse.newBuilder()
        val em = sessionFactory.createEntityManager()
        val builder = em.criteriaBuilder
        val query: CriteriaQuery<PersistedCourse> = builder.createQuery(PersistedCourse::class.java)
        val root: Root<PersistedCourse> = query.from(PersistedCourse::class.java)

        query.where(builder.equal(root.get<PersistedCourse>("courseState"), state))

        val ratingTees: Array<Int> = arrayOf(-1, -1, -1, -1, -1)
        val courses: List<PersistedCourse>?
        var courseBuilder: Builder? = null

        try {
            courses = em.createQuery(query).resultList

            courses.forEach { course ->
                courseBuilder = newBuilder()
                courseBuilder!!.id = course.courseSeq
                courseBuilder.name = course.courseName
                course.ratings.forEach { rating ->
                    val ratingBuilder =
                        Rating.newBuilder()
                            .setRating(rating.teeRating.toString())
                            .setSlope(rating.teeSlope!!)
                            .setTee(rating.tee!!)
                            .setColor(rating.teeColor)
                            .setPar(rating.teePar!!)
                    courseBuilder.addRatings(ratingBuilder)
                    ratingTees[rating.tee!!] = rating.tee!! // which tees have been added
                }
                if (coursesBuilder.isInitialized) {
                    // Generating placeholders for tees not defined
                    setUndefinedTees(ratingTees, courseBuilder!!)
                    coursesBuilder.addCourses(courseBuilder)
                }
            }
            if (courseBuilder == null) {
                coursesBuilder.addCourses(newBuilder())
            }
        } catch (nre: NoResultException) {
            LOGGER.error("Query Courses: " + nre.message)
        } finally {
            if (em.isOpen) {
                em.close()
            }
        }

        return convertBuilderToJson(coursesBuilder)
    }

    private fun convertBuilderToJson(coursesBuilder: ListCoursesResponse.Builder): String {
        val coursesObject = JsonObject()
        val allCoursesObject = JsonObject()
        val allCoursesArray = JsonArray()

        coursesBuilder.coursesList.forEach { course ->
            val coursesArray = JsonArray()
            val ratingsArray = JsonArray()
            val courseObject = JsonObject()
            val ratingsObject = JsonObject()

            coursesObject.put("courses", coursesArray)
            coursesArray.add(courseObject)
            courseObject.put("id", course.id).put("name", course.name)

            course.ratingsList.forEach { rating ->
                if (rating.color == null || rating.color.isEmpty()) {
                    ratingsArray.add(ratingsObject.put("tee", rating.tee))
                } else {
                    ratingsArray.add(
                        ratingsObject.put("tee", rating.tee)
                            .put("rating", rating.rating)
                            .put("slope", rating.slope)
                            .put("par", rating.par)
                            .put("color", rating.color)
                    )
                }
            }
            courseObject.put("ratings", ratingsArray)
            allCoursesArray.add(courseObject)
        }
        val courseJsonObject = allCoursesObject.put("courses", allCoursesArray)

        return courseJsonObject.toString()
    }

    private fun setUndefinedTees(
        ratingTees: Array<Int>,
        courseBuilder: Builder
    ) {
        for (int in ratingTees.indices) {
            if (ratingTees[int] == -1) {
                val ratingBuilder = Rating.newBuilder().setTee(int)
                courseBuilder.addRatings(ratingBuilder)
            }
            ratingTees[int] = -1
        }
    }

    fun getRatingMap(json: String): HashMap<String, Any> {
        val mapper = ObjectMapper()

        val ratingMap =
            mapper.readValue(json, object : TypeReference<HashMap<String, Any>>() {})

        val color: String = ratingMap["color"] as String
        if (!color.startsWith("#")) {
            val rgb: List<String> = color.split("(")[1].split(")")[0].split(",")
            val hex = "%02x"

            ratingMap["color"] = String.format(
                "#%s%s%s",
                hex.format(rgb[0].trim().toInt()),
                hex.format(rgb[1].trim().toInt()),
                hex.format(rgb[2].trim().toInt())
            )
                .uppercase()
        }
        return ratingMap
    }

    private fun buildCourse(courseMap: HashMap<String, Any>): PersistedCourse {
        val persistedCourse = PersistedCourse()

        persistedCourse.courseName = courseMap["courseName"].toString()
        persistedCourse.courseState = courseMap["state"].toString()
        persistedCourse.courseCountry = courseMap["country"].toString()

        return persistedCourse
    }

    private fun updateRating(courseMap: HashMap<String, Any>, courseRatings: PersistedCourse): Set<PersistedRatings> {
        courseRatings.ratings.iterator().forEach {
            if (it.tee == courseMap["tee"]) {
                setRating(courseMap, it)
            }
        }
        return courseRatings.ratings
    }

    private fun newRating(courseMap: HashMap<String, Any>, courseRatings: PersistedCourse): Set<PersistedRatings> {
        val persistedRating = PersistedRatings()
        setRating(courseMap, persistedRating)

        persistedRating.ratingsId = PersistedRatings.RatingsId(courseRatings.courseSeq, persistedRating.tee)
        val ratings: Set<PersistedRatings> = courseRatings.ratings.plus(persistedRating)

        return ratings
    }

    private fun buildRating(courseMap: HashMap<String, Any>, persistedCourse: PersistedCourse): Set<PersistedRatings> {
        val persistedRating = PersistedRatings()
        val ratingsSet = HashSet<PersistedRatings>()

        setRating(courseMap, persistedRating)
        persistedRating.course_seq = persistedCourse.courseSeq
        persistedRating.ratingsId = PersistedRatings.RatingsId(persistedCourse.courseSeq, persistedRating.tee)

        ratingsSet.add(persistedRating)

        return ratingsSet
    }

    private fun setRating(courseMap: HashMap<String, Any>, persistedRating: PersistedRatings) {
        persistedRating.teeRating = (courseMap["rating"] as String).toFloat()
        persistedRating.teePar = courseMap["par"] as Int
        persistedRating.tee = courseMap["tee"] as Int
        persistedRating.teeColor = courseMap["color"] as String
        persistedRating.teeSlope = courseMap["slope"] as Int

    }
}
