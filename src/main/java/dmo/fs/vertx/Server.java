
package dmo.fs.vertx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import org.apache.http.client.methods.HttpOptions;
import org.modellwerkstatt.javaxbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dmo.fs.router.CassandraRouter;
import dmo.fs.router.Routes;
import dmo.fs.spa.SpaApplication;
import dmo.fs.spa.router.SpaRoutes;
import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.BridgeOptions;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.file.FileSystem;
import io.vertx.rxjava3.core.http.HttpServer;
import io.vertx.rxjava3.ext.eventbus.bridge.tcp.TcpEventBusBridge;
import io.vertx.rxjava3.ext.web.Route;

public class Server extends AbstractVerticle {
  private static Logger logger;
  private int startupPort;
  private int port = startupPort = 0;


  public Server() {
    Locale.setDefault(Locale.forLanguageTag("US"));
  }

  public Server(int startupPort) {
    Locale.setDefault(Locale.forLanguageTag("US"));
    this.startupPort = startupPort;
  }

  static {
    File logDir = new File("./logs/");
    if (!(logDir.exists())) {
      logDir.mkdir();
    }

    System.setProperty("java.util.logging.SimpleFormatter.format",
        "[%1$tF %1$tT] [%4$s] %5$s %3$s %n");
    System.setProperty("dmo.fs.level", "INFO");
    System.setProperty("org.jooq.no-logo", "true");
    System.setProperty("org.jooq.no-tips", "true");
    logger = LoggerFactory.getLogger(Server.class.getName());
  }

  private static String OS = System.getProperty("os.name").toLowerCase();
  private static String development = System.getenv("VERTXWEB_ENVIRONMENT");
  private static String useKafka = System.getenv("DODEX_KAFKA");
  private HttpServer server;
  private JsonObject config;

  @Override
  public void start(Promise<Void> promise) throws InterruptedException, URISyntaxException,
      IOException, SQLException, NumberFormatException {
    if (development == null || "".equals(development) || development.toLowerCase().startsWith("prod")) {
      development = "prod";
    } else if ("dev".equalsIgnoreCase(development)) {
      port = startupPort == 0 ? 8087 : startupPort;
    } else if ("test".equalsIgnoreCase(development)) {
      port = startupPort == 0 ? 8089 : startupPort;
    }
    HttpServerOptions options = new HttpServerOptions();
    options.setLogActivity(true);
    config = Vertx.currentContext().config();
    if (config.getInteger("http.port") == null) {
      config = getAlternateConfig();
    }
    if(useKafka == null) {
      useKafka = config.getString("dodex.kafka");
    }
    if("true".toLowerCase().equals(useKafka)) {
      logger.info("{}Using Kafka - make sure it is running with ZooKeeper.{}", ColorUtilConstants.GREEN, ColorUtilConstants.RESET);
    }
    /* From application-conf.json - Vertx version defaults to 4 */
    Integer vertxVersion = config.getInteger(development + ".vertx.version",
        config.getInteger("vertx.version") == null ? 4 : config.getInteger("vertx.version"));

    Boolean color = "prod".equalsIgnoreCase(development) ? config.getBoolean("prod.color")
        : config.getBoolean("color");
    if (color != null && !color) {
      ColorUtilConstants.colorOff();
    }

    if (isUnix()) {
      server = configureLinuxOptions(vertx);
    } else {
      server = vertx.createHttpServer(options);
    }

    Routes routes = new Routes(vertx, server, vertxVersion);

    SpaRoutes allRoutes = new SpaRoutes(vertx, server, routes.getRouter(), routes.getFirestore());

    FileSystem fs = vertx.fileSystem();
    /* For BoxFuse - sqlite3 */
    // fs.mkdir("/efs");
    // Runtime.getRuntime().exec("mount -t nfs4 -o
    // nfsvers=4.1,rsize=1048576,wsize=1048576,hard,timeo=600,retrans=2
    // <your-efs-id>.efs.<your-aws-region>.amazonaws.com:/ /efs").waitFor();
    if(!"prod".equalsIgnoreCase(development)) {
      List<Route> routesList = allRoutes.getRouter().getRoutes();
      for (Route r : routesList) {
        String path = parsePath(r);
        String methods = path + (r.methods() == null ? "" : r.methods());
        logger.info("{}{}{}", ColorUtilConstants.CYAN_BOLD_BRIGHT, methods, ColorUtilConstants.RESET);
      }
    }
    server.requestHandler(allRoutes.getRouter());

    // Note: development = "prod" in production mode
    // Can override port at execution time with env variable "VERTX_PORT"

    String overridePort = System.getenv("VERTX_PORT") == null ? development + ".http.port"
        : System.getenv("VERTX_PORT");
    int secondaryPort = this.port == 0 ? 8880 : port;
    try {
      port = System.getenv("VERTX_PORT") == null ? config.getInteger(overridePort, secondaryPort)
          : Integer.parseInt(System.getenv("VERTX_PORT"));
    } catch (NumberFormatException ex) {
      ex.printStackTrace();
      throw ex;
    }

    try {
      server.rxListen(port).doOnSuccess(result -> {
        logger.info(String.join("", ColorUtilConstants.GREEN_BOLD_BRIGHT, "HTTP Started on port: ",
            Integer.toString(port), ColorUtilConstants.RESET));
        promise.complete();
        try {
          if ("dev".equalsIgnoreCase(development)) {
            String fileStarted = "./server-started";
            if (!fs.existsBlocking(fileStarted)) {
              fs.createFileBlocking(fileStarted);
            }
          }
        } catch (Exception e) {
          logger.error("{}{}{}", ColorUtilConstants.RED_BOLD_BRIGHT, e.getMessage(),
              ColorUtilConstants.RESET);
        }
        if (Boolean.TRUE.equals(golf.handicap.vertx.MainVerticle.getEnableHandicap())) {
          Verticle handicapVerticle = new golf.handicap.vertx.MainVerticle();
          vertx.deployVerticle(handicapVerticle).subscribe();
        }
      }).doOnError(err -> {
        logger.error("{}{}{}", ColorUtilConstants.RED_BOLD_BRIGHT, err.getCause(),
            ColorUtilConstants.RESET);
        promise.fail(err.getCause());
      }).subscribe();
    } catch (Exception e) {
      logger.error("{}{}{}", ColorUtilConstants.RED_BOLD_BRIGHT, e.getMessage(),
          ColorUtilConstants.RESET);
    }

    String defaultDb = new DodexUtil().getDefaultDb();
    logger.info("{}{}{}{}{}", ColorUtilConstants.PURPLE_BOLD_BRIGHT, "Using ", defaultDb,
        " database", ColorUtilConstants.RESET);

    if ("cassandra".equals(defaultDb)) {
      TcpEventBusBridge bridge = TcpEventBusBridge.create(vertx,
          new BridgeOptions().addInboundPermitted(new PermittedOptions().setAddress("vertx"))
              .addOutboundPermitted(new PermittedOptions().setAddress("akka"))
              .addInboundPermitted(new PermittedOptions().setAddress("akka"))
              .addOutboundPermitted(new PermittedOptions().setAddress("vertx")));

      int eventBridgePort = config.getInteger(development + "bridge.port") == null ? 7032
          : config.getInteger(development + "bridge.port");

      bridge.rxListen(eventBridgePort).doOnSuccess(res -> {
        logger.info(String.format("%s%s%d%s", ColorUtilConstants.GREEN_BOLD_BRIGHT,
            "TCP Event Bus Bridge Started: ", eventBridgePort, ColorUtilConstants.RESET));
        setupEventBridge();
      }).doOnError(err -> {
        logger.error(String.format("%s%s%s", ColorUtilConstants.RED_BOLD_BRIGHT,
            err.getCause().getMessage(), ColorUtilConstants.RESET));
      }).subscribe();
    }
  }

  private void setupEventBridge() {
    // Note: development has the value of "prod" when in production
    Integer bridgePort = config.getInteger(development + ".bridge.port");
    EventBus eb = EventBus.create("localhost", bridgePort == null ? 7032 : bridgePort);
    SpaApplication.setEb(eb);
    CassandraRouter.setEb(eb);
    logger.info("{}{}{}", ColorUtilConstants.BLUE_BOLD_BRIGHT,
        "Dodex Connected to Event Bus Bridge%s", ColorUtilConstants.RESET);
  }

  private HttpServer configureLinuxOptions(Vertx vertx) {
    // Available on Linux
    return vertx.createHttpServer(new HttpServerOptions().setTcpFastOpen(true).setTcpCork(true)
        .setTcpQuickAck(true).setReusePort(true).setLogActivity(true)
    // https - generate and get signed your certificate
    // Self signed for testing, per;
    // sslshopper.com/article-most-common-java-keytool-keystore-commands.html
    // keytool -genkey -keyalg RSA -alias selfsigned -keystore keystore.jks
    // -storepass password -validity 360 -keysize 2048
    // .setKeyStoreOptions(new JksOptions()
    // .setPath("keystore.jks") // good until April 9, 2022
    // .setPassword("apassword"))
    // .setSsl(true)
    );
  }

  private String parsePath(Route route) {
    if (!route.isRegexPath()) {
      return route.getPath();
    }

    String info = route.toString();

    return info.substring(info.indexOf("pattern=") + 8,
        info.indexOf(',', info.indexOf("pattern=")));
  }

  public static boolean isWindows() {
    return OS.contains("win");
  }

  public static boolean isMac() {
    return OS.contains("mac");
  }

  public static boolean isUnix() {
    return OS.contains("nix") || OS.contains("nux") || OS.indexOf("aix") > 0;
  }

  public static boolean isSolaris() {
    return OS.contains("sunos");
  }

  public JsonObject getAlternateConfig() throws IOException {
    ObjectMapper jsonMapper = new ObjectMapper();
    JsonNode node;

    try (InputStream in = getClass().getResourceAsStream("/application-conf.json")) {
      node = jsonMapper.readTree(in);
    }

    return new JsonObject(node.toString());
  }

  public HttpServer getServer() {
    return server;
  }

  public static Boolean getUseKafka() {
    return Boolean.parseBoolean(useKafka);
  }
}
