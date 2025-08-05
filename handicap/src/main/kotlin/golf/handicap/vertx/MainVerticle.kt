package golf.handicap.vertx

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import dmo.fs.utils.ColorUtilConstants
import dmo.fs.utils.DodexUtil
import dmo.fs.utils.DodexUtils
import golf.handicap.routes.GrpcRoutes
import golf.handicap.routes.HandicapRoutes
import io.vertx.core.Promise
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.rxjava3.core.AbstractVerticle
import io.vertx.rxjava3.core.Vertx
import io.vertx.rxjava3.ext.web.Router
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.nio.file.Paths

class MainVerticle : AbstractVerticle() {

    companion object {
        private val logger = LoggerFactory.getLogger(MainVerticle::class.java.name)
        var port = 8888
        var enableHandicap: Boolean = false
        var enableHandicapAdmin: Boolean? = false
        var enableHandicapPin: String? = ""
        val dbs = arrayOf("neo4j", "firebase", "cassandra", "ibmdb2", "cubrid", "mongo")

        @JvmStatic
        lateinit var rxVertx: Vertx

        private var defaultDb: String = DodexUtil().getDefaultDb()
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
            if(!dbs.contains(defaultDb)) {
                if (Vertx.currentContext() != null &&
                    Vertx.currentContext().config().getBoolean("handicap.enableHandicap") != null
                ) {
                    config = Vertx.currentContext().config()
                }
                val appConfig = getAlternateConfig()

                if (config != null && config!!.getBoolean("handicap.enableHandicap") != null) {
                    enableHandicap = config!!.getBoolean("handicap.enableHandicap")
                    enableHandicapAdmin = config!!.getBoolean("handicap.enableAdmin")
                    enableHandicapPin = config!!.getString("handicap.adminPin")
                }
                var configPort = appConfig.getInteger("handicap.port")
                port = configPort ?: port
                if ("dev" != development && "test" != development) {
                    configPort = appConfig.getInteger("prod.handicap.port")
                    port = configPort ?: port
                }

                enableHandicap =
                    if (config == null) appConfig.getBoolean("handicap.enableHandicap")
                    else enableHandicap
                enableHandicapAdmin =
                    if (enableHandicapAdmin == null) appConfig.getBoolean("handicap.enableAdmin")
                    else enableHandicapAdmin
                enableHandicapPin =
                    if (enableHandicapPin == null) appConfig.getString("handicap.adminPin")
                    else enableHandicapPin
                enableHandicap =
                    if (System.getProperty("USE_HANDICAP") != null) "true" == System.getProperty("USE_HANDICAP")
                    else enableHandicap

                if (useHandicap || "false" == System.getenv("USE_HANDICAP")) {
                    enableHandicap = useHandicap
                }

                if (true != appConfig.getBoolean("grpc.server")) {
                    var useGrpcServer =
                        if (System.getenv("GRPC_SERVER") != null) "true" == System.getProperty("GRPC_SERVER")
                        else false
                    useGrpcServer =
                        if (System.getenv("GRPC_SERVER") != null) "true" == System.getenv("GRPC_SERVER")
                        else useGrpcServer
                    if (enableHandicap && useGrpcServer) {
                        logger.warn("Initializing Handicap Verticle")
                    }
                }
            }
        }

        fun getAlternateConfig(): JsonObject {
            val jsonMapper = ObjectMapper()
            var node: JsonNode? = null
            if("true" == System.getProperty("kotlinTest")) {
                val path = Paths.get("src", "test",  "kotlin", "resources").toFile().absolutePath;
                val bufferedReader: BufferedReader = File("$path/application-conf.json").bufferedReader()
                bufferedReader.use { node = jsonMapper.readTree(it) }
            }
            else {
                HandicapGrpcServer::class.java.getResourceAsStream("/application-conf.json").use {
                    node = jsonMapper.readTree(it)
                }
            }
            return JsonObject(node.toString())
        }
    }

    override fun start(startPromise: Promise<Void>) {
        var development = System.getenv("VERTXWEB_ENVIRONMENT")
        System.setProperty("org.jooq.no-logo", "true")
        System.setProperty("org.jooq.no-tips", "true")

        rxVertx = vertx
        DodexUtils.setVertx(vertx)
        DodexUtils.setEnv(development)
        logger.warn(
            String.format("Disable File Caching: %s", System.getProperty("vertx.disableFileCaching"))
        )

        val routes: HandicapRoutes = GrpcRoutes(vertx)
        val router: Router = routes.getVertxRouter()
        // if using gRPC
        val grpcPromise: Promise<Void> = Promise.promise()
        routes.setRoutePromise(grpcPromise)

        vertx
            .createHttpServer(HttpServerOptions().setLogActivity(true))
            .requestHandler(router)
            .rxListen(port)
            .doOnSuccess {
                startPromise.complete()
                if (enableHandicap) {
                    grpcPromise.complete()
                    logger.warn(
                        String.format(
                            "%sHandicap Started on port: %s%s",
                            ColorUtilConstants.YELLOW, port, ColorUtilConstants.RESET
                        )
                    )
                }
            }.doOnError { err -> {
                err.printStackTrace()
            }}
            .subscribe({}, { err -> logger.error(err.message) })
    }
}
