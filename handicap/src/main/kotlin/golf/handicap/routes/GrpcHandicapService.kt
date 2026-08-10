package golf.handicap.routes

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import dmo.fs.dbh.DbConfiguration
import dmo.fs.dbh.HandicapDatabase
import golf.handicap.Course
import golf.handicap.Golfer
import golf.handicap.Handicap
import golf.handicap.Score
import golf.handicap.db.PopulateCourse
import golf.handicap.db.PopulateGolfer
import golf.handicap.db.PopulateGolferScores
import golf.handicap.db.PopulateScore
import golf.handicap.vertx.HandicapGrpcServer
import handicap.grpc.*
import io.grpc.Status
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.grpc.server.Service
import org.slf4j.LoggerFactory

class GrpcHandicapService : HandicapIndexGrpcService(), Service {
    private val logger = LoggerFactory.getLogger(GrpcHandicapService::class.java.name)

    init {
        DbConfiguration.getDefaultDb<HandicapDatabase>()
    }

    override fun listCourses(
        request: Command
    ): Future<ListCoursesResponse?> {
        val promise = Promise.promise<ListCoursesResponse?>()
        val populateCourse = PopulateCourse()
        val course = Course()
        course.courseState = request.key

        populateCourse.getCourses(course).onSuccess { listCourses ->
            promise.complete(listCourses)
        }

        return promise.future()
    }

    override fun addRating(request: Command): Future<HandicapData?> {
        val promise = Promise.promise<HandicapData?>()
        val populateCourse = PopulateCourse()
        val mapper = ObjectMapper()

        val ratingMap =
            mapper.readValue(request.json, object : TypeReference<HashMap<String, Any>>() {})

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
        populateCourse
            .getCourseWithTee(ratingMap)
            .onSuccess { handicapData -> promise.complete(handicapData) }
            .onFailure { err ->
                logger.error("Error Adding Rating: " + err.message)
                promise.fail(err)
            }
        return promise.future()
    }

    override fun addScore(request: Command): Future<HandicapData?> {
        val promise = Promise.promise<HandicapData?>()
        val mapper = ObjectMapper()
        val score = mapper.readValue(request.json, object : TypeReference<Score>() {})

        val populateScore = PopulateScore()
        populateScore
            .setScore(score)
            .onSuccess { _ ->
                val handicap = Handicap()
                handicap
                    .getHandicap(score.golfer!!)
                    .onSuccess { latestTee ->
                        val newHandicap: Float = latestTee["handicap"] as Float
                        val slope: Float = latestTee["slope"] as Float
                        val rating: Float = latestTee["rating"] as Float
                        val par: Int = latestTee["par"] as Int
                        score.handicap = newHandicap
                        val courseHandicap: Float = newHandicap * slope / 113 + (rating - par)
                        score.netScore = score.grossScore.toFloat() - courseHandicap
                        score.golfer!!.handicap = newHandicap

                        populateScore
                            .setScore(score)
                            .onSuccess { _ ->
                                promise.complete(
                                    HandicapData.newBuilder()
                                        .setMessage("Success")
                                        .setCmd(request.cmd)
                                        .setJson(ObjectMapper().writeValueAsString(score))
                                        .build()
                                )

                            }
                            .onFailure { err ->
                                err.stackTrace
                                promise.fail(err)
                            }
                    }
                    .onFailure { err ->
                        err.stackTrace
                        promise.fail(err)
                    }
            }
            .onFailure { err ->
                err.stackTrace
                promise.fail(err)
            }
        return promise.future()
    }

    override fun getGolfer(request: HandicapSetup): Future<HandicapData?>? {
        if ("Test" == request.message) {
            logger.warn("Got json from Client: " + request.getJson())
        }

        var requestJson = JsonObject(request.json)
        val golfer = requestJson.mapTo(Golfer::class.java)
        val promise = Promise.promise<HandicapData?>()
        val cmd = request.cmd

        if (cmd !in 0..8) {
            val status: Status = Status.FAILED_PRECONDITION.withDescription("Cmd - Not between 0 and 8")
            throw (status.asRuntimeException())
        } else {
            val populateGolfer = PopulateGolfer()

            populateGolfer.getGolfer(golfer, cmd).onSuccess { resultGolfer ->
                requestJson = JsonObject.mapFrom(resultGolfer)
                requestJson.remove("status")
                requestJson.put("status", resultGolfer.status)
                if (HandicapGrpcServer.enableHandicapAdmin!!) {
                    requestJson.put("adminstatus", 10)
                    requestJson.put("admin", HandicapGrpcServer.handicapAdminPin)
                }
                if ("Test" == request.message) {
                    logger.warn("Handicap Data Sent: " + request.json)
                }

                promise.complete(
                    HandicapData.newBuilder()
                        .setMessage(resultGolfer.message)
                        .setCmd(request.cmd)
                        .setJson(requestJson.toString())
                        .build()
                )
            }
            return promise.future()
        }
    }

    override fun golferScores(request: Command): Future<HandicapData?> {
        val promise = Promise.promise<HandicapData?>()
        val populateScores = PopulateGolferScores()
        val requestJson = JsonObject(request.json)
        val golfer = requestJson.mapTo(Golfer::class.java)

        if (request.cmd == 10) {
            val names = request.key.split("&#44;")
            golfer.lastName = names[0]
            golfer.firstName = if (names.size > 1) names[1].trim() else ""
            golfer.pin = ""
        }

        populateScores.getGolferScores(golfer, 365)!!.onSuccess { scoresMap ->
            promise.complete(
                HandicapData.newBuilder()
                    .setMessage("Success")
                    .setCmd(request.cmd)
                    .setJson(scoresMap["array"].toString())
                    .build()
            )
        }

        return promise.future()
    }

    override fun listGolfers(
        request: Command
    ): Future<ListPublicGolfers?> {
        val populateGolfer = PopulateGolfer()
        val promise = Promise.promise<ListPublicGolfers?>()

        populateGolfer.getGolfers(promise)

        return promise.future()
    }

    override fun removeScore(request: Command): Future<HandicapData?> {
        val populateScores = PopulateGolferScores()
        val requestJson = JsonObject(request.json)
        val golfer = requestJson.mapTo(Golfer::class.java)
        val promise = Promise.promise<HandicapData?>()

        populateScores.removeLastScore(request.key).onSuccess { used ->
            val handicap = Handicap()
            handicap.getHandicap(golfer).onSuccess { latestTee ->
                golfer.handicap = latestTee["handicap"] as Float
                val jsonObject = JsonObject.mapFrom(golfer)
                jsonObject.put("used", used)
                promise.complete(
                    HandicapData.newBuilder()
                        .setMessage("Success")
                        .setCmd(request.cmd)
                        .setJson(jsonObject.toString())
                        .build()
                )
            }
        }

        return promise.future()
    }
}
