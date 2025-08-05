package golf.handicap.routes

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import dmo.fs.dbh.DbConfiguration
import dmo.fs.dbh.HandicapDatabase
import dmo.fs.dbh.ora.DodexDatabaseOracle
import dmo.fs.dbh.mssql.DodexDatabaseMssql
import dmo.fs.utils.DodexUtils
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
import org.slf4j.LoggerFactory


class GrpcRoutes(vertx: Vertx) : HandicapRoutes {
    val router: Router = Router.router(vertx)
    private val faviconHandler: FaviconHandler = FaviconHandler.create(vertx)
    private val grpcVertx = vertx.delegate
    var promise: Promise<Void> = Promise.promise()


    companion object {
        private val logger = LoggerFactory.getLogger(GrpcRoutes::class.java.name)

        //    private var dodexDatabase: HandicapDatabase? = null
        private val config = MainVerticle.getAlternateConfig()
        private val grpcPort = config.getInteger("grpc4:port")
        private val dodexUtil: DodexUtils = DodexUtils()

        init {
            val defaultDb = dodexUtil.getDefaultDb()
            if ("oracle" == defaultDb) {
                val oracle: DodexDatabaseOracle = DbConfiguration.getDefaultDb()
                oracle.entityManagerSetup()
                oracle.configDatabase()
            } else if ("mssql" == defaultDb) {
                val mssql: DodexDatabaseMssql = DbConfiguration.getDefaultDb()
                mssql.entityManagerSetup()
                mssql.configDatabase()
            } else {
                DbConfiguration.getDefaultDb<HandicapDatabase>()
            }

        }
    }

    override fun getVertxRouter(): Router {
        val staticHandler: StaticHandler = StaticHandler.create("static")
        staticHandler.setCachingEnabled(false)
        staticHandler.setMaxAgeSeconds(0)

        val staticRoute: Route = router.route("/handicap/*").handler(TimeoutHandler.create(2000))
        staticRoute.handler(staticHandler)
        staticRoute.failureHandler { err ->
            logger.error(String.format("FAILURE in static route: %s", err.statusCode()))
        }

        router.route().handler(staticHandler)
        router.route().handler(faviconHandler)



        router.route().handler(getCorsHandler())
        return router
    }

    override fun setRoutePromise(promise: Promise<Void>) {
        this.promise = promise
        grpcServer()
    }

    fun setGrpcPromise(promise: Promise<Void>) {
        this.promise = promise
        grpcServer()
    }

    private fun grpcServer() {
        promise.future().onComplete {
            val service = RxJavaHandicapService()
            val rpcServer: VertxServer =
                VertxServerBuilder.forAddress(grpcVertx, "localhost", grpcPort)
                    .addService(service)
                    .build()

            rpcServer.start()
            logger.warn("gRpc server started on port $grpcPort")
        }
    }

    override fun routes(router: Router): Router { // Create a Router
        router.get("/handicap/courses").produces("application/json").handler {
            it.response().send("{}")
        }

        return router
    }

    public fun getCorsHandler(): CorsHandler {
        val methods = setOf(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.OPTIONS, HttpMethod.HEAD)
        return CorsHandler.create()
//            .allowCredentials(false)
            .allowedMethods(methods)
//        .addOrigin("https://coolapp2.loca.lt")
//        .allowedHeader("https://coolapp2.loca.lt")
//        .addOrigin("https://<your tunnel url>2.loca.lt")
//        .allowedHeader("https://<your tunnel url>2.loca.lt")
            .addOrigins(
                mutableListOf<String?>(
                    "http://localhost:8070",
                    "http://localhost:8087",
                    "http://localhost:7087",
                    "http://localhost:8880",
                    "http://localhost:8085",
                    "http://localhost:8881",        // Virtual Threads Verticle
                    "http://192.168.49.2:30080",    // IP generated from "minikube service vertx-service"
                    "http://192.168.42.2:30070"
                )
            )
            .addOriginWithRegex("^https:\\/\\/\\w+handicap\\d?\\.loophole\\.site$")
            .allowedHeaders(
                mutableSetOf<String?>(
                    "keep-alive",
                    "user-agent",
                    "cache-control",
                    "content-type",
                    "content-transfer-encoding",
                    "x-custom-key",
                    "x-user-agent",
                    "x-grpc-web",
                    "grpc-timeout",
                    "Access-Control-Allow-Origin"
                )
            )
            .exposedHeaders(mutableSetOf<String?>("x-custom-key", "grpc-status", "grpc-message"))
    }

    class HandicapIndexService : HandicapIndexGrpc.HandicapIndexImplBase() {
        override fun listCourses(
            request: Command,
            responseObserver: StreamObserver<ListCoursesResponse?>
        ) {
            val populateCourse = PopulateCourse()
            val course: golf.handicap.Course = golf.handicap.Course()
            course.courseState = request.key

            populateCourse.getCourses(course, responseObserver).onSuccess { responseObserve ->
                responseObserve.onCompleted()
            }
        }

        override fun addRating(request: Command, responseObserver: StreamObserver<HandicapData?>) {
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
                .getCourseWithTee(ratingMap, responseObserver)
                .onSuccess { responseObserve -> responseObserve.onCompleted() }
                .onFailure { err ->
                    logger.error("Error Adding Rating: " + err.message)
                    responseObserver.onCompleted()
                }
        }

        override fun addScore(request: Command, responseObserver: StreamObserver<HandicapData?>) {
            val mapper = ObjectMapper()
            val score = mapper.readValue(request.json, object : TypeReference<golf.handicap.Score>() {})

            val populateScore = PopulateScore()
            populateScore
                .setScore(score, responseObserver)
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
                                .setScore(score, responseObserver)
                                .onSuccess { responseObserve ->
                                    responseObserve.onNext(
                                        HandicapData.newBuilder()
                                            .setMessage("Success")
                                            .setCmd(request.cmd)
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
            if ("Test" == request.message) {
                logger.warn("Got json from Client: " + request.getJson())
            }

            var requestJson = JsonObject(request.json)
            val golfer = requestJson.mapTo(Golfer::class.java)

            val cmd = request.cmd
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
                            .setCmd(request.cmd)
                            .setJson(requestJson.toString())
                            .build()
                    )
                    if ("Test" == request.message) {
                        logger.warn("Handicap Data Sent: " + request.json)
                    }
                    responseObserver.onCompleted()
                }
            }
        }

        override fun golferScores(request: Command, responseObserver: StreamObserver<HandicapData?>) {
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
                responseObserver.onNext(
                    HandicapData.newBuilder()
                        .setMessage("Success")
                        .setCmd(request.cmd)
                        .setJson(scoresMap["array"].toString())
                        .build()
                )

                responseObserver.onCompleted()
            }
        }

        override fun listGolfers(
            request: Command,
            responseObserver: StreamObserver<ListPublicGolfers?>
        ) {
            val populateGolfer = PopulateGolfer()

            populateGolfer.getGolfers(responseObserver).onSuccess { responseObserve ->
                responseObserve.onCompleted()
            }
        }

        override fun removeScore(request: Command, responseObserver: StreamObserver<HandicapData?>) {
            val populateScores = PopulateGolferScores()
            val requestJson = JsonObject(request.json)
            val golfer = requestJson.mapTo(Golfer::class.java)

            populateScores.removeLastScore(request.key).onSuccess { used ->
                val handicap = Handicap()
                handicap.getHandicap(golfer).onSuccess { latestTee ->
                    golfer.handicap = latestTee["handicap"] as Float
                    val jsonObject = JsonObject.mapFrom(golfer)
                    jsonObject.put("used", used)
                    responseObserver.onNext(
                        HandicapData.newBuilder()
                            .setMessage("Success")
                            .setCmd(request.cmd)
                            .setJson(jsonObject.toString())
                            .build()
                    )
                    responseObserver.onCompleted()
                }
            }
        }
    }
}
