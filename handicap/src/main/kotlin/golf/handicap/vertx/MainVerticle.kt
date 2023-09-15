package golf.handicap.vertx

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import dmo.fs.utils.ColorUtilConstants
import dmo.fs.utils.DodexUtil
import golf.handicap.routes.GrpcRoutes
import golf.handicap.routes.HandicapRoutes
import io.vertx.core.Promise
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.rxjava3.core.AbstractVerticle
import io.vertx.rxjava3.core.Vertx
import io.vertx.rxjava3.ext.web.Router
import java.io.IOException
import java.util.logging.LogManager
import java.util.logging.Logger

class MainVerticle : AbstractVerticle() {
    companion object {
        private val LOGGER = Logger.getLogger(MainVerticle::class.java.name)
        var port = 8888
        private var enableHandicap: Boolean?
        var enableHandicapAdmin: Boolean?
        var enableHandicapPin: String?

        @Throws(IOException::class)
        private fun setupLogging() {
            MainVerticle::class.java.getResourceAsStream("/vertx-default-jul-logging.properties").use { f
                ->
                LogManager.getLogManager().readConfiguration(f)
            }
        }

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
//            setupLogging()
            LOGGER.warning("Initializing Handicap Vertical")

            config = Vertx.currentContext().config()
            val appConfig = getAlternateConfig()

            enableHandicap = config!!.getBoolean("handicap.enableHandicap")
            enableHandicapAdmin = config!!.getBoolean("handicap.enableAdmin")
            enableHandicapPin = config!!.getString("handicap.adminPin")
            var configPort = appConfig.getInteger("handicap.port")
            port = configPort ?: port
            if ("dev" != development) {
                configPort = appConfig.getInteger("prod.handicap.port")
                port = configPort ?: port
            }

            enableHandicap =
                if (enableHandicap == null) appConfig.getBoolean("handicap.enableHandicap")
                else enableHandicap
            enableHandicapAdmin =
                if (enableHandicapAdmin == null) appConfig.getBoolean("handicap.enableAdmin")
                else enableHandicapAdmin
            enableHandicapPin =
                if (enableHandicapPin == null) appConfig.getString("handicap.adminPin")
                else enableHandicapPin
            if (useHandicap || "false" == System.getenv("USE_HANDICAP")) {
                enableHandicap = useHandicap
            }
        }

        fun getAlternateConfig(): JsonObject {
            val jsonMapper = ObjectMapper()
            var node: JsonNode?

            MainVerticle::class.java.getResourceAsStream("/application-conf.json").use { inputStream ->
                node = jsonMapper.readTree(inputStream)
            }

            return JsonObject(node.toString())
        }
    }

    override fun start(startPromise: Promise<Void>) {
        val development = System.getenv("VERTXWEB_ENVIRONMENT")
        System.setProperty("org.jooq.no-logo", "true")
        System.setProperty("org.jooq.no-tips", "true")
        DodexUtil.setVertx(vertx)
        DodexUtil.setEnv(development)

    LOGGER.warning(
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
          if (enableHandicap!!) {
            grpcPromise.complete()
            LOGGER.warning(String.format("%sHandicap Started on port: %s%s",
                ColorUtilConstants.YELLOW, port, ColorUtilConstants.RESET))
          }
        }
        .subscribe({}, { err -> LOGGER.severe(err.message) })
  }
}
