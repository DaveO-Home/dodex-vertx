package golf.handicap.routes

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import dmo.fs.dbh.DbConfiguration
import dmo.fs.dbh.HandicapDatabase
import dmo.fs.dbh.mssql.DodexDatabaseMssql
import dmo.fs.dbh.ora.DodexDatabaseOracle
import dmo.fs.utils.ColorUtilConstants
import dmo.fs.utils.DodexUtils
import golf.handicap.Golfer
import golf.handicap.Handicap
import golf.handicap.db.PopulateCourse
import golf.handicap.db.PopulateGolfer
import golf.handicap.db.PopulateGolferScores
import golf.handicap.db.PopulateScore
import golf.handicap.vertx.HandicapGrpcServer
import golf.handicap.vertx.HandicapGrpcServer.Companion.port
import handicap.grpc.*
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.grpcio.server.GrpcIoServer
import io.vertx.rxjava3.core.Vertx
import io.vertx.rxjava3.ext.web.Route
import io.vertx.rxjava3.ext.web.Router
import io.vertx.rxjava3.ext.web.RoutingContext
import io.vertx.rxjava3.ext.web.handler.CorsHandler
import io.vertx.rxjava3.ext.web.handler.FaviconHandler
import io.vertx.rxjava3.ext.web.handler.StaticHandler
import io.vertx.rxjava3.ext.web.handler.TimeoutHandler
import org.slf4j.LoggerFactory

class GrpcRoutes(vertx: Vertx) : HandicapRoutes {
    val router: Router = Router.router(vertx)
    private val grpcVertx = vertx.delegate
    var promise: Promise<Void> = Promise.promise()

    companion object {
        private val logger = LoggerFactory.getLogger(GrpcRoutes::class.java.name)

        private val config = HandicapGrpcServer.getAlternateConfig()
        private val dodexUtil: DodexUtils = DodexUtils()

        init {
            val defaultDb = dodexUtil.getDefaultDb()
            when (defaultDb) {
                "oracle" -> {
                    val oracle: DodexDatabaseOracle = DbConfiguration.getDefaultDb()
                    oracle.entityManagerSetup()
                    oracle.configDatabase()
                }

                "mssql" -> {
                    val mssql: DodexDatabaseMssql = DbConfiguration.getDefaultDb()
                    mssql.entityManagerSetup()
                    mssql.configDatabase()
                }

                else -> {
                    DbConfiguration.getDefaultDb<HandicapDatabase>()
                }
            }

        }
    }

    /*
        Only need CorsHandler for grpc routing via secondary port.
     */
    override fun getVertxRouter(): Router {
        router.route().handler(getCorsHandler())
        return router
    }

    override fun setRoutePromise(promise: Promise<Void>) {
        this.promise = promise
        grpcServer()
    }

    private fun grpcServer() {
        val vertx = DodexUtils.getVertx()
        val defaultDb = DodexUtils().defaultDb
        val routes: HandicapRoutes = GrpcRoutes(vertx)
        val router: Router = routes.getVertxRouter()

        val grpcServer: GrpcIoServer = GrpcIoServer.server(grpcVertx)

        router.route()
            .consumes("application/grpc-web-text")
            .handler(Handler { rc: RoutingContext? ->
                grpcServer.handle(rc?.delegate!!.request())
            })

        if ("oracle" == defaultDb || "mssql" == defaultDb) {
            grpcServer.addService(HibernateHandicapService())
        } else {
            grpcServer.addService(RxJavaHandicapService())
        }

        grpcVertx
            .createHttpServer(HttpServerOptions().setLogActivity(true))
            .requestHandler(grpcServer)
            .requestHandler(router.delegate)
            .listen(port)
            .onSuccess {
                promise.complete()
                logger.warn(
                    "{}gRPCIo for Handicap Started on port: {}{}",
                    ColorUtilConstants.YELLOW, port, ColorUtilConstants.RESET
                )
            }.onFailure { err -> logger.error(err.message) }
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
}
