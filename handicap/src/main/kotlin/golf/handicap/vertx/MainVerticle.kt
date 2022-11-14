package golf.handicap.vertx

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import dmo.fs.utils.DodexUtil
import dmo.fs.utils.ColorUtilConstants
import golf.handicap.routes.GrpcRoutes
import golf.handicap.routes.HandicapRoutes
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.rxjava3.core.AbstractVerticle
import io.vertx.rxjava3.core.Vertx
import io.vertx.rxjava3.ext.web.Router
import java.io.IOException
import java.util.logging.LogManager
import java.util.logging.Logger

class MainVerticle : AbstractVerticle() {
  companion object {
    private val LOGGER = Logger.getLogger(MainVerticle::class.java.name)
    var port = 8888;
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
    val development = System.getenv("VERTXWEB_ENVIRONMENT")
    val useHandicap = "true".equals(System.getenv("USE_HANDICAP"))
    var config: JsonObject? = null

    @JvmStatic
    public fun getEnableHandicap():Boolean? {
      return this.enableHandicap
    }

    init {
      LOGGER.warning("Initializing Handicap Verticle")
      val objectMapper = DatabindCodec.mapper()
      objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      objectMapper.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
      objectMapper.disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
      val module = JavaTimeModule()
      objectMapper.registerModule(module)

      config = Vertx.currentContext().config()
      val appConfig = getAlternateConfig()

      enableHandicap = config!!.getBoolean("handicap.enableHandicap")
      enableHandicapAdmin = config!!.getBoolean("handicap.enableAdmin")
      enableHandicapPin = config!!.getString("handicap.adminPin")
      var configPort = appConfig.getInteger("handicap.port")
      port = if(null != configPort) configPort else port
      if(!"dev".equals(development)) {
        configPort = appConfig.getInteger("prod.handicap.port")
        port = if(null != configPort) configPort else port
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
      if (useHandicap || "false".equals(System.getenv("USE_HANDICAP"))) {
        enableHandicap = useHandicap
      }
    }

    fun getAlternateConfig(): JsonObject {
      val jsonMapper: ObjectMapper = ObjectMapper()
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
        .createHttpServer()
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
