package golf.handicap.vertx

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import dmo.fs.utils.ColorUtilConstants
import dmo.fs.utils.DodexUtil
import dmo.fs.utils.DodexUtils
import golf.handicap.routes.GrpcRoutes
import golf.handicap.routes.HandicapRoutes
import golf.handicap.routes.HibernateHandicapService
import golf.handicap.routes.RxJavaHandicapService
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.grpcio.server.GrpcIoServer
import io.vertx.rxjava3.core.AbstractVerticle
import io.vertx.rxjava3.core.Vertx
import io.vertx.rxjava3.ext.web.Router
import io.vertx.rxjava3.ext.web.RoutingContext
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.nio.file.Paths

class HandicapGrpcServer : AbstractVerticle() {
    companion object {
        private val logger = LoggerFactory.getLogger(HandicapGrpcServer::class.java.name)
        var port = 8070
        private var enableHandicap: Boolean? = null
        var enableHandicapAdmin: Boolean? = null
        var handicapAdminPin: String? = null

        val development: String = System.getenv("VERTXWEB_ENVIRONMENT") ?: "prod"
        private val useHandicap = "true" == System.getenv("USE_HANDICAP")
        var config: JsonObject? = null
        private var routes: Router? = null

        @JvmStatic
        fun getEnableHandicap(): Boolean? {
            return this.enableHandicap
        }

        @JvmStatic
        fun setRoutes(routes: Router) {
            this.routes = routes
        }

        init {
            if (Vertx.currentContext() != null &&
                Vertx.currentContext().config().getBoolean("handicap.enableHandicap") != null
            ) {
                config = Vertx.currentContext().config()
            }
            val appConfig = getAlternateConfig()
            if (config != null && config!!.getBoolean("handicap.enableHandicap") != null) {
                enableHandicap = config!!.getBoolean("handicap.enableHandicap")
                enableHandicapAdmin = config!!.getBoolean("handicap.enableAdmin")
                handicapAdminPin = config!!.getString("handicap.adminPin")
            }
            var configPort = appConfig.getInteger("grpc.port")
            port = configPort ?: port
            if ("dev" != development) {
                configPort = appConfig.getInteger("prod.grpc.port")
                port = configPort ?: port
            }

            enableHandicap =
                if (enableHandicap == null) appConfig.getBoolean("handicap.enableHandicap")
                else enableHandicap
            enableHandicapAdmin =
                if (enableHandicapAdmin == null) appConfig.getBoolean("handicap.enableAdmin")
                else enableHandicapAdmin
            handicapAdminPin =
                if (handicapAdminPin == null) appConfig.getString("handicap.adminPin")
                else handicapAdminPin
            enableHandicap =
                if (System.getProperty("USE_HANDICAP") != null) "true" == System.getProperty("USE_HANDICAP")
                else enableHandicap
            if (useHandicap || "false" == System.getenv("USE_HANDICAP")) {
                enableHandicap = useHandicap
            }

            logger.warn("Initializing gRPC Handicap Service")
        }

        private fun getAlternateConfig(): JsonObject {
            val jsonMapper = ObjectMapper()
            var node: JsonNode?
            if ("true" == System.getProperty("kotlinTest")) {
                val path = Paths.get("src", "test", "kotlin", "resources").toFile().absolutePath;
                val bufferedReader: BufferedReader = File("$path/application-conf.json").bufferedReader()
                bufferedReader.use { node = jsonMapper.readTree(it) }
            } else {
                HandicapGrpcServer::class.java.getResourceAsStream("/application-conf.json").use {
                    node = jsonMapper.readTree(it)
                }
            }
            return JsonObject(node.toString())
        }
    }

    override fun start(startPromise: Promise<Void>) {
        DodexUtils.setVertx(vertx)

        val development = System.getenv("VERTXWEB_ENVIRONMENT")
        System.setProperty("org.jooq.no-logo", "true")
        System.setProperty("org.jooq.no-tips", "true")

        DodexUtils.setEnv(development)
        DodexUtil.setEnv(development)
        val defaultDb = DodexUtils().defaultDb

        logger.warn(
            String.format("Disable File Caching: %s", System.getProperty("vertx.disableFileCaching"))
        )

        val routes: HandicapRoutes = GrpcRoutes(vertx)
        val router: Router = routes.getVertxRouter()

        val grpcServer: GrpcIoServer = GrpcIoServer.server(vertx.delegate)

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

        if (enableHandicap!!) {
            vertx.delegate
                .createHttpServer(HttpServerOptions().setLogActivity(true))
                .requestHandler(grpcServer)
                .requestHandler(router.delegate)
                .listen(port)
                .onSuccess {
                    startPromise.complete()

                    logger.warn(
                        String.format(
                            "%sgRPC for Handicap Started on port: %s%s",
                            ColorUtilConstants.YELLOW, port, ColorUtilConstants.RESET
                        )
                    )

                }.onFailure { err -> logger.error(err.message) }
        }
    }
}
