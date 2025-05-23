package golf.handicap.db

import dmo.fs.dbh.DbConfiguration
import dmo.fs.utils.ColorUtilConstants
import golf.handicap.Course
import golf.handicap.generated.tables.references.COURSE
import golf.handicap.generated.tables.references.RATINGS
import handicap.grpc.HandicapData
import handicap.grpc.ListCoursesResponse
import handicap.grpc.Rating
import io.grpc.stub.StreamObserver
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.rxjava3.mysqlclient.MySQLClient
import io.vertx.rxjava3.sqlclient.Tuple
import org.jooq.impl.DSL.*
import org.slf4j.LoggerFactory
import java.sql.SQLException

class PopulateCourse : SqlConstants() {
    companion object {
        private val logger = LoggerFactory.getLogger(PopulateCourse::class.java.name)
        private val regEx = "\\$\\d".toRegex()

        @Throws(SQLException::class)
        @JvmStatic
        fun buildSql() {
            GETCOURSESBYSTATE =
                if (qmark) setupCoursesByState().replace(regEx, "?")
                else setupCoursesByState().replace("\"", "")

            GETCOURSEBYNAME =
                if (qmark) setupCourseByName().replace(regEx, "?")
                else setupCourseByName().replace("\"", "")

            GETCOURSEBYTEE =
                if (qmark) setupCourseByRating().replace(regEx, "?")
                else setupCourseByRating().replace("\"", "")

            GETCOURSEINSERT =
                if (qmark) setupCourseInsert().replace(regEx, "?")
                else setupCourseInsert().replace("\"", "")

            GETRATINGINSERT =
                if (qmark) setupRatingInsert().replace(regEx, "?")
                else setupRatingInsert().replace("\"", "")

            GETRATINGUPDATE =
                if (qmark) setupRatingUpdate().replace(regEx, "?")
                else setupRatingUpdate().replace("\"", "")

            GETSQLITERATINGUPDATE =
                if (qmark) setupSqliteRatingUpdate().replace(regEx, "?")
                else setupSqliteRatingUpdate().replace("\"", "")
        }

        init {}

        @JvmStatic
        private fun setupCoursesByState(): String {
            return create!!.renderNamedParams(
                select(
                    COURSE.COURSE_SEQ,
                    COURSE.COURSE_NAME,
                    COURSE.COURSE_COUNTRY,
                    COURSE.COURSE_STATE,
                    RATINGS.COURSE_SEQ,
                    RATINGS.TEE,
                    RATINGS.TEE_COLOR,
                    RATINGS.TEE_RATING,
                    RATINGS.TEE_SLOPE,
                    RATINGS.TEE_PAR
                )
                    .from(COURSE, RATINGS)
                    .where(COURSE.COURSE_STATE.eq("$").and(COURSE.COURSE_SEQ.eq(RATINGS.COURSE_SEQ)))
            )
        }

        @JvmStatic
        private fun setupCourseByName(): String {
            return create!!.renderNamedParams(
                select(COURSE.COURSE_SEQ, COURSE.COURSE_NAME, COURSE.COURSE_COUNTRY, COURSE.COURSE_STATE)
                    .from(COURSE)
                    .where(COURSE.COURSE_NAME.eq("$"))
                    .and(COURSE.COURSE_COUNTRY.eq("$"))
                    .and(COURSE.COURSE_STATE.eq("$"))
            )
        }

        @JvmStatic
        private fun setupCourseByRating(): String {
            val ratingsSelect: String =
                create!!.renderNamedParams(
                    select(
                        COURSE.COURSE_SEQ,
                        COURSE.COURSE_NAME,
                        COURSE.COURSE_COUNTRY,
                        COURSE.COURSE_STATE,
                        RATINGS.COURSE_SEQ,
                        RATINGS.TEE,
                        RATINGS.TEE_COLOR,
                        RATINGS.TEE_RATING,
                        RATINGS.TEE_SLOPE,
                        RATINGS.TEE_PAR
                    )
                        .from(COURSE, RATINGS)
                        .where(
                            COURSE
                                .COURSE_NAME
                                .eq("$")
                                .and(COURSE.COURSE_COUNTRY.eq("$"))
                                .and(COURSE.COURSE_STATE.eq("$"))
                                .and(COURSE.COURSE_SEQ.eq(RATINGS.COURSE_SEQ))
                                .and(RATINGS.TEE.eq(0))
                        )
                )

            return ratingsSelect
        }

        @JvmStatic
        private fun setupCourseInsert(): String {
            return create!!.renderNamedParams(
                insertInto(COURSE, COURSE.COURSE_NAME, COURSE.COURSE_COUNTRY, COURSE.COURSE_STATE)
                    .values("$", "$", "$").returning(field("COURSE_SEQ"))
            )
        }

        @JvmStatic
        private fun setupRatingInsert(): String {
            return create!!.renderNamedParams(
                insertInto(
                    RATINGS,
                    RATINGS.COURSE_SEQ,
                    RATINGS.TEE,
                    RATINGS.TEE_COLOR,
                    RATINGS.TEE_RATING,
                    RATINGS.TEE_SLOPE,
                    RATINGS.TEE_PAR
                )
                    .values(0, 0, "$", 0.0.toFloat(), 0, 0)
            )
        }

        @JvmStatic
        private fun setupCourseUpdate(): String {
            return create!!.renderNamedParams(
                insertInto(COURSE, COURSE.COURSE_NAME, COURSE.COURSE_STATE).values("$", "$")
            )
        }

        @JvmStatic
        private fun setupRatingUpdate(): String {
            return create!!.renderNamedParams(
                update(RATINGS)
                    .set(RATINGS.TEE_COLOR, "$")
                    .set(RATINGS.TEE_RATING, 0.0.toFloat())
                    .set(RATINGS.TEE_SLOPE, 0)
                    .set(RATINGS.TEE_PAR, 0)
                    .where(RATINGS.COURSE_SEQ.eq(0).and(RATINGS.TEE.eq(0)))
            )
        }

        @JvmStatic
        private fun setupSqliteRatingUpdate(): String {
            return """update RATINGS 
						set TEE_COLOR = ?,
						TEE_RATING = ?,
						TEE_SLOPE = ?,
						TEE_PAR = ?
						where COURSE_SEQ = ?
							and TEE = ?"""
        }
    }

    fun getCourse(courseMap: HashMap<String, Any>): Future<Course> {
        val promise: Promise<Course> = Promise.promise()
        val course = Course()

        pool!!
            .rxGetConnection()
            .doOnSuccess { conn ->
                val sql = GETCOURSEBYNAME
                val parameters: Tuple = Tuple.tuple()

                parameters.addString(courseMap["courseName"] as String)
                parameters.addString(courseMap["country"] as String)
                parameters.addString(courseMap["state"] as String)
                course.findNextRating()

                conn.preparedQuery(sql)
                    .rxExecute(parameters)
                    .doOnError { err ->
                        logger.error(String.format("Error getting course: %s", err.message))
                    }
                    .doOnSuccess { rows ->
                        for (row in rows) {
                            course.courseKey = row.getInteger(0) // "COURSE_SEQ")
                            course.courseName = row.getString(1) // "COURSE_NAME")
                            course.courseCountry = row.getString(2) // "COURSE_COUNTRY")
                            course.courseState = row.getString(3) // "COURSE_STATE")
                        }
                        promise.complete(course)
                    }
                    .subscribe(
                        { conn.close().subscribe() },
                        { err ->
                            logger.error(
                                String.format(
                                    "%sError querying Course - %s%s %s\n%s",
                                    ColorUtilConstants.RED,
                                    err,
                                    ColorUtilConstants.RESET,
                                    err.stackTraceToString()
                                )
                            )
                            promise.complete(course)
                        }
                    )
            }
            .subscribe()

        return promise.future()
    }

    fun getCourseWithTee(
        courseMap: HashMap<String, Any>,
        responseObserver: StreamObserver<HandicapData?>
    ): Future<StreamObserver<HandicapData?>> {
        val promise: Promise<StreamObserver<HandicapData?>> = Promise.promise()
        val course = Course()

        pool!!
            .rxGetConnection()
            .doOnSuccess { conn ->
                val sql = GETCOURSEBYTEE //?.uppercase()
                val parameters: Tuple = Tuple.tuple()
                var updateTees = true

                parameters.addString(courseMap["courseName"] as String)
                parameters.addString(courseMap["country"] as String)
                parameters.addString(courseMap["state"] as String)
                parameters.addInteger(courseMap["tee"] as Int)

                conn.preparedQuery(sql)
                    .rxExecute(parameters)
                    .doOnError { err ->
                        logger.warn(String.format("Error getting course: %s", err.message))
                    }
                    .doOnSuccess { rows ->
                        for (row in rows) {
                            course.courseKey = row.getInteger(0) // "COURSE_SEQ")
                            course.courseName = row.getString(1) // "COURSE_NAME")
                            course.courseCountry = row.getString(2) // "COURSE_COUNTRY")
                            course.courseState = row.getString(3) // "COURSE_STATE")
                            course.setRating(
                                row.getInteger(0), // "COURSE_SEQ"),
                                row.getFloat(7).toString(), // "TEE_RATING"),
                                row.getInteger(8), // "TEE_SLOPE"),
                                row.getInteger(9), // "TEE_PAR"),
                                row.getInteger(5), // "TEE"),
                                row.getString(6) // "TEE_COLOR")
                            )

                            if ((courseMap["rating"] as String) == row.getFloat(7).toString() &&
                                (courseMap["slope"] as Int) == row.getInteger(8) &&
                                (courseMap["par"] as Int) == row.getInteger(9) &&
                                (courseMap["color"] as String) == row.getString(6)
                            ) {
                                updateTees = false
                            }
                        }
                        if (rows.size() == 0) {
                            updateTees = false
                            courseMap["status"] = 2
                            setCourse(courseMap, responseObserver).onSuccess {
                                setRating(courseMap, responseObserver).onSuccess {
                                    course.resetIterator()
                                }
                            }
                        } else {
                            if (updateTees) {
                                updateTee(courseMap, responseObserver).onFailure { err ->
                                    logger.error(
                                        String.format(
                                            "%sError updating tees - %s%s %s",
                                            ColorUtilConstants.RED,
                                            err,
                                            ColorUtilConstants.RESET,
                                            err.stackTraceToString()
                                        )
                                    )
                                }
                            }
                        }
                    }
                    .subscribe(
                        {
                            conn.close().subscribe()
                            val jsonString: String = JsonObject(courseMap).toString()
                            responseObserver.onNext(
                                HandicapData.newBuilder()
                                    .setMessage("Success")
                                    .setCmd(2)
                                    .setJson(jsonString)
                                    .build()
                            )
                            promise.complete(responseObserver)
                        },
                        { err ->
                            logger.error(
                                String.format(
                                    "%sError querying Courses Tees - %s%s %s",
                                    ColorUtilConstants.RED,
                                    err,
                                    ColorUtilConstants.RESET,
                                    err.stackTraceToString()
                                )
                            )
                            promise.complete(responseObserver)
                        }
                    )
            }
            .subscribe()

        return promise.future()
    }

    @Throws(SQLException::class, InterruptedException::class)
    fun getCourses(
        course: Course,
        responseObserver: StreamObserver<ListCoursesResponse?>
    ): Future<StreamObserver<ListCoursesResponse?>> {
        val promise: Promise<StreamObserver<ListCoursesResponse?>> = Promise.promise()

        pool!!
            .rxGetConnection()
            .doOnSuccess { conn ->
                val parameters: Tuple = Tuple.tuple()
                parameters.addString(course.courseState)
                val sql: String? = GETCOURSESBYSTATE
                val coursesBuilder = ListCoursesResponse.newBuilder()
                var courseBuilder: handicap.grpc.Course.Builder? = null

                conn.preparedQuery(sql)
                    .rxExecute(parameters)
                    .doOnError { err ->
                        logger.error(String.format("Error getting courses: %s", err.message))
                    }
                    .doOnSuccess { rows ->
                        val ratingTees: Array<Int> = arrayOf(-1, -1, -1, -1, -1)
                        for (row in rows) {
                            if (courseBuilder == null || row!!.getInteger(0) != courseBuilder!!.id) {
                                if (courseBuilder != null) {
                                    setUndefinedTees(ratingTees, courseBuilder!!)
                                    coursesBuilder.addCourses(courseBuilder)
                                }

                                courseBuilder = handicap.grpc.Course.newBuilder()
                                courseBuilder!!.setId(row.getInteger(0)) // "COURSE_SEQ"))
                                courseBuilder!!.setName(row.getString(1)) // "COURSE_NAME"))
                            }

                            val ratingBuilder =
                                Rating.newBuilder()
                                    .setRating(row.getFloat(7).toString()) // "TEE_RATING")
                                    .setSlope(row.getInteger(8)) // "TEE_SLOPE")
                                    .setTee(row.getInteger(5)) // "TEE")
                                    .setColor(row.getString(6)) // "TEE_COLOR")
                                    .setPar(row.getInteger(9)) // "TEE_PAR")
                            courseBuilder!!.addRatings(ratingBuilder)
                            ratingTees[row.getInteger(5)] = row.getInteger(5) // which tees have been added
                        }

                        if (courseBuilder != null) {
                            setUndefinedTees(ratingTees, courseBuilder!!)
                            coursesBuilder.addCourses(courseBuilder)
                        }
                        responseObserver.onNext(coursesBuilder.build())
                        promise.complete(responseObserver)
                    }
                    .subscribe(
                        { conn.close().subscribe() },
                        { err ->
                            logger.error(
                                String.format(
                                    "%sError querying Courses - %s%s\n%s",
                                    ColorUtilConstants.RED,
                                    err,
                                    ColorUtilConstants.RESET,
                                    err.stackTraceToString()
                                )
                            )

                            promise.complete(responseObserver)
                        }
                    )
            }
            .subscribe()
        return promise.future()
    }

    private fun setUndefinedTees(
        ratingTees: Array<Int>,
        courseBuilder: handicap.grpc.Course.Builder
    ) {
        for (int in ratingTees.indices) {
            if (ratingTees[int] == -1) {
                val ratingBuilder = Rating.newBuilder().setTee(int)
                courseBuilder.addRatings(ratingBuilder)
            }
            ratingTees[int] = -1
        }
    }

    @Throws(SQLException::class, InterruptedException::class)
    fun setCourse(
        courseMap: HashMap<String, Any>,
        responseObserver: StreamObserver<HandicapData?>
    ): Future<StreamObserver<HandicapData?>> {
        val ratingPromise: Promise<StreamObserver<HandicapData?>> = Promise.promise()

        getCourse(courseMap).onSuccess { queryedCourse ->
            if (queryedCourse.courseKey == 0) {
                pool!!
                    .rxGetConnection()
                    .flatMap { conn ->
                        conn.rxBegin().doOnSuccess { tx ->
                            val parameters: Tuple = Tuple.tuple()
                            parameters.addString(courseMap["courseName"] as String)
                            parameters.addString(courseMap["country"] as String)
                            parameters.addString(courseMap["state"] as String)
                            conn.preparedQuery(GETCOURSEINSERT)
                                .rxExecute(parameters)
                                .doOnSuccess { rows ->
                                    for (row in rows) {
                                        courseMap["courseKey"] = row.getInteger(0)
                                    }
                                    if (DbConfiguration.isUsingSqlite3()) {
                                        courseMap["courseKey"] = 0
                                    } else if (DbConfiguration.isUsingMariadb()) {
                                        courseMap["courseKey"] = rows.property(MySQLClient.LAST_INSERTED_ID)
                                    }
                                    ratingPromise.complete(responseObserver)
                                    tx.commit().subscribe {
                                        conn.close().subscribe()
                                    }
                                }
                                .doOnError { _ ->
                                    tx.rollback().subscribe()
                                    ratingPromise.complete(responseObserver)
                                }
                                .subscribe(
                                    {},
                                    { err ->
                                        logger.error(
                                            String.format(
                                                "%sError Inserting Course - %s%s %s",
                                                ColorUtilConstants.RED,
                                                err.message,
                                                ColorUtilConstants.RESET,
                                                err.stackTraceToString()
                                            )
                                        )
                                    }
                                )
                        }
                    }
                    .doOnError { err ->
                        logger.error(
                            String.format(
                                "%sError Inserting Course - %s%s -- %s",
                                ColorUtilConstants.RED,
                                err.message,
                                ColorUtilConstants.RESET,
                                err.stackTraceToString()
                            )
                        )
                    }
                    .subscribe()
            } else {
                ratingPromise.complete(responseObserver)
            }
        }
        return ratingPromise.future()
    }

    @Throws(SQLException::class, InterruptedException::class)
    fun setRating(
        courseMap: HashMap<String, Any>,
        responseObserver: StreamObserver<HandicapData?>
    ): Future<StreamObserver<HandicapData?>> {
        val ratingPromise: Promise<StreamObserver<HandicapData?>> = Promise.promise()

        getCourse(courseMap).onSuccess { queryedCourse ->
            if (queryedCourse.courseKey > 0) {
                courseMap["courseKey"] = queryedCourse.courseKey
            }

            if (queryedCourse.courseKey > 0 || courseMap["courseKey"] as Int > 0) {
                pool!!
                    .rxGetConnection()
                    .flatMap { conn ->
                        conn.rxBegin().doOnSuccess { tx ->
                            val parameters: Tuple = Tuple.tuple()
                            parameters.addInteger(courseMap["courseKey"] as Int)
                            parameters.addInteger(courseMap["tee"] as Int)
                            parameters.addString(courseMap["color"] as String)
                            if (DbConfiguration.isUsingPostgres()) {
                                parameters.addFloat((courseMap["rating"] as String).toFloat())
                            } else {
                                parameters.addString(courseMap["rating"] as String)
                            }
                            parameters.addInteger(courseMap["slope"] as Int)
                            parameters.addInteger(courseMap["par"] as Int)
                            conn.preparedQuery(GETRATINGINSERT)
                                .rxExecute(parameters)
                                .doOnSuccess { _ ->
                                    tx.commit()
                                    ratingPromise.complete(responseObserver)
                                }
                                .doOnError { _ ->
                                    tx.rollback()
                                    ratingPromise.complete(responseObserver)
                                }
                                .subscribe(
                                    { conn.close().subscribe() },
                                    { err ->
                                        logger.error(
                                            String.format(
                                                "%sError Inserting Rating - %s%s %s",
                                                ColorUtilConstants.RED,
                                                err.message,
                                                ColorUtilConstants.RESET,
                                                err.stackTraceToString()
                                            )
                                        )
                                        conn.close().subscribe()
                                    }
                                )
                        }
                    }
                    .doOnError { err ->
                        logger.error(
                            String.format(
                                "%sError Inserting Rating - %s%s -- %s",
                                ColorUtilConstants.RED,
                                err,
                                ColorUtilConstants.RESET,
                                err.stackTraceToString()
                            )
                        )
                        ratingPromise.complete(responseObserver)
                    }
                    .subscribe()
            }
        }

        return ratingPromise.future()
    }

    @Throws(SQLException::class, InterruptedException::class)
    fun updateTee(
        courseMap: HashMap<String, Any>,
        responseObserver: StreamObserver<HandicapData?>
    ): Future<StreamObserver<HandicapData?>> {
        val ratingPromise: Promise<StreamObserver<HandicapData?>> = Promise.promise()

        pool!!
            .rxGetConnection()
            .flatMap { conn ->
                conn.rxBegin().doOnSuccess { tx ->
                    val parameters: Tuple = Tuple.tuple()
                    parameters.addString(courseMap["color"] as String)
                    if (DbConfiguration.isUsingPostgres()) {
                        parameters.addFloat((courseMap["rating"] as String).toFloat())
                    } else {
                        parameters.addString(courseMap["rating"] as String)
                    }
                    parameters.addInteger(courseMap["slope"] as Int)
                    parameters.addInteger(courseMap["par"] as Int)
                    parameters.addInteger(courseMap["seq"] as Int)
                    parameters.addInteger(courseMap["tee"] as Int)

                    val sql: String? =
                        if (DbConfiguration.isUsingSqlite3()) GETSQLITERATINGUPDATE else GETRATINGUPDATE
                    conn.preparedQuery(sql)
                        .rxExecute(parameters)
                        .doOnSuccess { _ ->
                            tx.commit().subscribe {
                                conn.close().subscribe()
                            }
                            ratingPromise.complete(responseObserver)
                        }
                        .doOnError { _ ->
                            tx.rollback().subscribe()
                            ratingPromise.complete(responseObserver)
                        }
                        .subscribe(
                            {},
                            { err ->
                                logger.error(
                                    String.format(
                                        "%sError Update Tee Rating - %s%s %s",
                                        ColorUtilConstants.RED,
                                        err.message,
                                        ColorUtilConstants.RESET,
                                        err.stackTraceToString()
                                    )
                                )
                            }
                        )
                }
            }
            .doOnError { err ->
                logger.error(
                    String.format(
                        "%sError Update Rating - %s%s -- %s",
                        ColorUtilConstants.RED,
                        err.message,
                        ColorUtilConstants.RESET,
                        err.stackTraceToString()
                    )
                )
                ratingPromise.complete(responseObserver)
            }
            .subscribe()

        return ratingPromise.future()
    }
}
