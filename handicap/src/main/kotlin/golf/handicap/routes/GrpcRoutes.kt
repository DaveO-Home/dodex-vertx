package golf.handicap.routes

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import dmo.fs.db.DbConfiguration
import dmo.fs.db.HandicapDatabase
import dmo.fs.utils.DodexUtil
import golf.handicap.Golfer
import golf.handicap.Handicap
import golf.handicap.db.PopulateCourse
import golf.handicap.db.PopulateGolfer
import golf.handicap.db.PopulateGolferScores
import golf.handicap.db.PopulateScore
import golf.handicap.vertx.MainVerticle
import handicap.grpc.*
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.vertx.core.Promise
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.grpc.VertxServer
import io.vertx.grpc.VertxServerBuilder
import io.vertx.rxjava3.core.Vertx
import io.vertx.rxjava3.ext.web.Route
import io.vertx.rxjava3.ext.web.Router
import io.vertx.rxjava3.ext.web.handler.CorsHandler
import io.vertx.rxjava3.ext.web.handler.FaviconHandler
import io.vertx.rxjava3.ext.web.handler.StaticHandler
import io.vertx.rxjava3.ext.web.handler.TimeoutHandler
import java.util.logging.Logger

class GrpcRoutes(vertx: Vertx) : HandicapRoutes {
  val router: Router = Router.router(vertx)
  private val faviconHandler: FaviconHandler = FaviconHandler.create(vertx)
  private val grpcVertx = DodexUtil.getVertx().delegate
  var promise: Promise<Void> = Promise.promise()

  companion object {
    private val LOGGER = Logger.getLogger(GrpcRoutes::class.java.name)
    var dodexDatabase: HandicapDatabase? = null

    init {
      dodexDatabase = DbConfiguration.getDefaultDb()
    }
  }

  override fun getVertxRouter(): Router {
    val staticHandler: StaticHandler = StaticHandler.create("static")
    staticHandler.setCachingEnabled(false)
    staticHandler.setMaxAgeSeconds(0)

    val staticRoute: Route = router.route("/handicap/*").handler(TimeoutHandler.create(2000))
    staticRoute.handler(staticHandler)
    staticRoute.failureHandler { err ->
        LOGGER.severe(String.format("FAILURE in static route: %s", err.statusCode()))
    }

    router.route().handler(staticHandler)
    router.route().handler(faviconHandler)

    val methods = setOf(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.OPTIONS, HttpMethod.HEAD)
    val corsHandler = CorsHandler.create()
        .allowCredentials(false)
        .allowedMethods(methods)
        .addOrigin("https://coolapp2.loca.lt")
        .allowedHeader("https://coolapp2.loca.lt")
//        .addOrigin("https://<your tunnel url>2.loca.lt")
//        .allowedHeader("https://<your tunnel url>2.loca.lt")
    router.route().handler(corsHandler)
    return router
}

    override fun setRoutePromise(promise: Promise<Void>) {
        this.promise = promise
        grpcServer()
    }

    public fun setGrpcPromise(promise: Promise<Void>) {
        this.promise = promise
        grpcServer()
    }

    private fun grpcServer() {
        val grpcPort = 15001
        promise.future().onComplete {
            var service = HandicapIndexService()
            val rpcServer: VertxServer =
                VertxServerBuilder.forAddress(grpcVertx, "localhost", grpcPort)
                    .addService(service)
                    .build()

            rpcServer.start()
            LOGGER.warning("gRpc server started on port 15001")
        }
    }

    override fun routes(router: Router): Router { // Create a Router
        router.get("/handicap/courses").produces("application/json").handler {
            it.response().send("{}")
        }

        return router
    }

    class HandicapIndexService : HandicapIndexGrpc.HandicapIndexImplBase() {
        override fun listCourses(
            request: Command,
            responseObserver: StreamObserver<ListCoursesResponse?>
        ) {
            val populateCourse: PopulateCourse = PopulateCourse()
            val course: golf.handicap.Course = golf.handicap.Course()
            course.courseState = request.key

            populateCourse.getCourses(course, responseObserver).onSuccess { responseObserve ->
                responseObserve.onCompleted()
            }
        }

        override fun addRating(request: Command, responseObserver: StreamObserver<HandicapData?>) {
            val populateCourse: PopulateCourse = PopulateCourse()
            val mapper: ObjectMapper = ObjectMapper()

            val ratingMap =
                mapper.readValue(request.json, object : TypeReference<HashMap<String, Any>>() {})

            val color: String? = ratingMap.get("color") as String
            if (color != null && !color.startsWith("#")) {
                val rgb: List<String> = color.split("(")[1].split(")")[0].split(",")
                val hex: String = "%02x"

                ratingMap.put(
                    "color",
                    String.format(
                        "#%s%s%s",
                        hex.format(rgb[0].trim().toInt()),
                        hex.format(rgb[1].trim().toInt()),
                        hex.format(rgb[2].trim().toInt())
                    )
                        .uppercase()
                )
            }
            populateCourse
                .getCourseWithTee(ratingMap, responseObserver)
                .onSuccess { responseObserve -> responseObserve.onCompleted() }
                .onFailure{ err ->
                    LOGGER.severe("Error Adding Rating: " + err.message)
                    responseObserver.onCompleted()
                }
        }

        override fun addScore(request: Command, responseObserver: StreamObserver<HandicapData?>) {
            val mapper: ObjectMapper = ObjectMapper()
            val score = mapper.readValue(request.json, object : TypeReference<golf.handicap.Score>() {})

            val populateScore: PopulateScore = PopulateScore()
            populateScore
                .setScore(score, responseObserver)
                .onSuccess { _ ->
                    val handicap = Handicap()

                    handicap
                        .getHandicap(score.golfer!!)
                        .onSuccess { latestTee ->
                            val newHandicap: Float = latestTee.get("handicap") as Float
                            val slope: Float = latestTee.get("slope") as Float
                            val rating: Float = latestTee.get("rating") as Float
                            val par: Int = latestTee.get("par") as Int
                            score.handicap = newHandicap
                            val courseHandicap: Float = newHandicap * slope / 113 + (rating - par)
                            score.netScore = score.grossScore.toFloat() - courseHandicap
                            score.golfer!!.handicap = newHandicap
                            populateScore
                                .setScore(score, responseObserver)
                                .onSuccess { responseObserve ->
                                    responseObserve.onNext(
                                        HandicapData.newBuilder()
                                            .setMessage("Success")
                                            .setCmd(request.getCmd())
                                            .setJson(ObjectMapper().writeValueAsString(score))
                                            .build()
                                    )
                                    responseObserve.onCompleted()
                                }
                                .onFailure { err ->
                                    err.stackTrace
                                    responseObserver.onCompleted()
                                }
                        }
                        .onFailure { err ->
                            err.stackTrace
                            responseObserver.onCompleted()
                        }
                }
                .onFailure { err ->
                    err.stackTrace
                    responseObserver.onCompleted()
                }
        }

        override fun getGolfer(
            request: HandicapSetup,
            responseObserver: StreamObserver<HandicapData?>
        ) {
            if ("Test".equals(request.message)) {
                LOGGER.warning("Got json from Client: " + request.getJson())
            }

            var requestJson = JsonObject(request.json)
            var golfer = requestJson.mapTo(Golfer::class.java)

            val cmd = request.getCmd()
            if (cmd < 0 || cmd > 8) {
                val status: Status = Status.FAILED_PRECONDITION.withDescription("Cmd - Not between 0 and 8")
                responseObserver.onError(status.asRuntimeException())
            } else {
                val populateGolfer = PopulateGolfer()

                populateGolfer.getGolfer(golfer, cmd).onSuccess { resultGolfer ->
                    requestJson = JsonObject.mapFrom(resultGolfer)
                    requestJson.remove("status")
                    requestJson.put("status", resultGolfer.status)
                    if (MainVerticle.enableHandicapAdmin!!) {
                        requestJson.put("adminstatus", 10)
                        requestJson.put("admin", MainVerticle.enableHandicapPin)
                    }

                    responseObserver.onNext(
                        HandicapData.newBuilder()
                            .setMessage(resultGolfer.message)
                            .setCmd(request.getCmd())
                            .setJson(requestJson.toString())
                            .build()
                    )
                    if ("Test".equals(request.message)) {
                        LOGGER.warning("Handicap Data Sent: " + request.json)
                    }
                    responseObserver.onCompleted()
                }
            }
        }

        override fun golferScores(request: Command, responseObserver: StreamObserver<HandicapData?>) {
            val populateScores: PopulateGolferScores = PopulateGolferScores()
            val requestJson = JsonObject(request.json)
            val golfer = requestJson.mapTo(Golfer::class.java)
            if (request.cmd == 10) {
                val names = request.key.split("&#44;")
                golfer.lastName = names[0]
                golfer.firstName = if (names.size > 1) names[1].trim() else ""
                golfer.pin = ""
            }

            populateScores.getGolferScores(golfer, 365)!!.onSuccess { scoresMap ->
                responseObserver.onNext(
                    HandicapData.newBuilder()
                        .setMessage("Success")
                        .setCmd(request.getCmd())
                        .setJson(scoresMap.get("array").toString())
                        .build()
                )

                responseObserver.onCompleted()
            }
        }

        override fun listGolfers(
            request: Command,
            responseObserver: StreamObserver<ListPublicGolfers?>
        ) {
            val populateGolfer: PopulateGolfer = PopulateGolfer()

            populateGolfer.getGolfers(responseObserver).onSuccess { responseObserve ->
                responseObserve.onCompleted()
            }
        }

        override fun removeScore(request: Command, responseObserver: StreamObserver<HandicapData?>) {
            val populateScores: PopulateGolferScores = PopulateGolferScores()
            val requestJson = JsonObject(request.json)
            val golfer = requestJson.mapTo(Golfer::class.java)

            populateScores.removeLastScore(request.key).onSuccess { used ->
                val handicap: Handicap = Handicap()
                handicap.getHandicap(golfer).onSuccess { latestTee ->
                    golfer.handicap = latestTee.get("handicap") as Float
                    val jsonObject = JsonObject.mapFrom(golfer)
                    jsonObject.put("used", used)
                    responseObserver.onNext(
                        HandicapData.newBuilder()
                            .setMessage("Success")
                            .setCmd(request.getCmd())
                            .setJson(jsonObject.toString())
                            .build()
                    )
                    responseObserver.onCompleted()
                }
            }
        }
    }
}
