
package dmo.fs.vertx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dmo.fs.mqtt.DodexMqttServer;
import dmo.fs.router.CassandraRouter;
import dmo.fs.router.Routes;
import dmo.fs.spa.SpaApplication;
import dmo.fs.spa.router.SpaRoutes;
import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import golf.handicap.vertx.HandicapGrpcServer;
import golf.handicap.vertx.MainVerticle;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.bridge.BridgeOptions;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Route;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.file.FileSystem;
import io.vertx.rxjava3.core.http.HttpServer;
import io.vertx.rxjava3.ext.eventbus.bridge.tcp.TcpEventBusBridge;
import org.modellwerkstatt.javaxbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public class Server extends AbstractVerticle {
  private static final Logger logger;
  private int startupPort = 0;
  private int port = 0;
  private static Vertx rxVertx;

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

  private static final String OS = System.getProperty("os.name").toLowerCase();
  private static String development = System.getenv("VERTXWEB_ENVIRONMENT");
  private static String useKafka = System.getenv("DODEX_KAFKA");
  private static String useMqtt = System.getenv("USE_MQTT");
  private static String useSsl = System.getenv("USE_SSL");
  private static HttpServer server;
  private static JsonObject config;
  private JsonObject alternateConfig;

  @Override
  public void stop() throws Exception {
    super.stop();
  }

  @Override
  public void start(Promise promise) throws Exception {
    super.start();
    if (development == null || development.isEmpty()
        || development.toLowerCase().startsWith("prod")) {
      development = "prod";
    } else if ("dev".equalsIgnoreCase(development)) {
      port = startupPort == 0 ? 8087 : startupPort;
    } else if ("test".equalsIgnoreCase(development)) {
      port = startupPort == 0 ? 8089 : startupPort;
    }

    rxVertx = vertx;
    DodexUtil.setVertx(vertx);

    HttpServerOptions options = new HttpServerOptions();
    options.setLogActivity(true);
    config = Vertx.currentContext().config();
    alternateConfig = getAlternateConfig();

    if (config.getInteger("http.port") == null) {
      config = alternateConfig;
    }
    if (useKafka == null) {
      useKafka = config.getString("dodex.kafka");
    }
    if (useMqtt == null) {
      useMqtt = config.getString("use.mqtt");
    }
    if (useSsl == null) {
      useSsl = config.getString("use.ssl");
    }
    if ("true".toLowerCase().equals(useKafka)) {
      logger.info("{}Using Kafka - make sure it is running with ZooKeeper.{}",
          ColorUtilConstants.GREEN, ColorUtilConstants.RESET);
    }
    /* From application-conf.json - Vertx version defaults to 4 */
    Integer vertxVersion = config.getInteger(development + ".vertx.version",
        config.getInteger("vertx.version") == null ? 4 : config.getInteger("vertx.version"));

    Boolean color = "prod".equalsIgnoreCase(development) ? config.getBoolean("prod.color")
        : config.getBoolean("color");
    if (color != null && !color) {
      ColorUtilConstants.colorOff();
    }

  /* Using virtual threads - java 21
    DeploymentOptions options = new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER);
    vertx.deployVerticle("dmo.fs.vertx.Server", options);
  */
    if (isUnix()) {
      server = configureLinuxOptions(rxVertx);
    } else {
      server = rxVertx.createHttpServer(options);
    }

    Routes routes = new Routes(rxVertx, server, vertxVersion);

    FileSystem fs = rxVertx.fileSystem();

    // Note: development = "prod" in production mode
    // Can override port at execution time with env variable "VERTX_PORT"

    String overridePort = System.getenv("VERTX_PORT") == null ? development + ".http.port"
        : System.getenv("VERTX_PORT");
    int secondaryPort = this.port == 0 ? 8880 : port;
    try {
      port = System.getenv("VERTX_PORT") == null ? config.getInteger(overridePort, secondaryPort)
          : Integer.parseInt(System.getenv("VERTX_PORT"));
    } catch (NumberFormatException ex) {
      logger.info("{}Port Invalid: value: {} -- {}{}",
          ColorUtilConstants.RED, port, ex.getMessage(), ColorUtilConstants.RESET);
      throw ex;
    }

    routes.getRouter().onSuccess(router -> {
      new SpaRoutes(rxVertx, server, router, routes.getFirestore());

      server.getDelegate().requestHandler(router);

      if (!"prod".equalsIgnoreCase(development)) {
        List<Route> routesList = router.getRoutes(); // allRoutes.getRouter().getRoutes();
        for (Route r : routesList) {
          String path = parsePath(r);
          String methods = path + (r.methods() == null ? "" : r.methods());
          logger.info("{}{}{}", ColorUtilConstants.CYAN_BOLD_BRIGHT, methods,
              ColorUtilConstants.RESET);
        }
      }

      try {
        server.rxListen(port).doOnSuccess(result -> {
          logger.info(String.join("", ColorUtilConstants.GREEN_BOLD_BRIGHT,
              "HTTP Started on port: ", Integer.toString(port), ColorUtilConstants.RESET));
          promise.complete();
          try {
            if ("dev".equalsIgnoreCase(development)) {
              String fileStarted = "./server-started";
              if (!fs.existsBlocking(fileStarted)) {
                fs.createFileBlocking(fileStarted);
              }
              checkInstallation(fs);
            }
          } catch (Exception e) {
            logger.error("{}{}{}", ColorUtilConstants.RED_BOLD_BRIGHT, e.getMessage(),
                ColorUtilConstants.RESET);
          }
          /*
            Handicap Verticle
           */
          if (Boolean.TRUE.equals(MainVerticle.getEnableHandicap())) {
            boolean useGrpcServer = alternateConfig.getBoolean("grpc.server") != null ?
                alternateConfig.getBoolean("grpc.server") : false;

            useGrpcServer = System.getenv("GRPC_SERVER") != null ? "true".equals(System.getenv("GRPC_SERVER")) : useGrpcServer;

            useGrpcServer = System.getProperty("GRPC_SERVER") != null ? "true".equals(System.getProperty("GRPC_SERVER")) : useGrpcServer;

            Verticle handicapVerticle;
            if (useGrpcServer) {
              handicapVerticle = new MainVerticle();
            } else {
              handicapVerticle = new HandicapGrpcServer();
            }
            rxVertx.deployVerticle(handicapVerticle).subscribe();
          }
          /*
            Test Java 21 Virtual Threads
           */
          if (Boolean.TRUE.equals(config.getBoolean("dodex.virtual.threads"))) {
            DeploymentOptions options2 = new DeploymentOptions().setThreadingModel(ThreadingModel.VIRTUAL_THREAD);
            VirtualThreadServer vts = new VirtualThreadServer();
            vertx.deployVerticle(vts, options2);
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
    });
    String defaultDb = new DodexUtil().getDefaultDb();
    logger.info("{}{}{}{}{}", ColorUtilConstants.PURPLE_BOLD_BRIGHT, "Using ", defaultDb,
        " database", ColorUtilConstants.RESET);

    if ("cassandra".equals(defaultDb)) {
      if ("true".equals(useMqtt)) {
        new DodexMqttServer();
      } else {
        TcpEventBusBridge bridge = TcpEventBusBridge.create(rxVertx,
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
          logger.error("{}{}{}", ColorUtilConstants.RED_BOLD_BRIGHT, err.getCause().getMessage(), ColorUtilConstants.RESET);
        }).subscribe();
      }
    }
//    return Future.succeededFuture();
  }

  private void setupEventBridge() {
    // Note: development has the value of "prod" when in production
    Integer bridgePort = config.getInteger(development + ".bridge.port");
    EventBus eb = EventBus.create("localhost", bridgePort == null ? 7032 : bridgePort);
    SpaApplication.setEb(eb);
    CassandraRouter.setEb(eb);
    logger.info("{}{}{}", ColorUtilConstants.BLUE_BOLD_BRIGHT,
        "Dodex Connected to Event Bus Bridge", ColorUtilConstants.RESET);
  }

  private HttpServer configureLinuxOptions(Vertx vertx) {
    // Available on Linux
    HttpServerOptions httpServerOptions = new HttpServerOptions().setTcpFastOpen(true).setTcpCork(true)
        .setTcpQuickAck(true).setReusePort(true).setLogActivity(true);

    /* Self signed for testing, per;
       sslshopper.com/article-most-common-java-keytool-keystore-commands.html
    */
    // keytool -genkey -keyalg RSA -alias selfsigned -keystore keystore.jks
    // -storepass some-password -validity 360 -keysize 2048

    if ("true".equals(useSsl)) {
      Buffer selfSignedBuffer = vertx.getDelegate().fileSystem().readFileBlocking("keystore.jks");
      httpServerOptions.setSsl(true)
          .setKeyCertOptions(new JksOptions()
              .setValue(selfSignedBuffer)
              .setPassword("some-password"));
    }
    return vertx.createHttpServer(httpServerOptions);
  }

  private void checkInstallation(FileSystem fs) {
    if ("dev".equalsIgnoreCase(development)) {
      String fileDir = "./src/spa-react/node_modules/";
      if (!fs.existsBlocking(fileDir)) {
        logger.info("{}{}{}", ColorUtilConstants.CYAN_BOLD_BRIGHT,
            "To install the test spa application, execute 'npm install --legacy-peer-deps' in 'src/spa-react/'"
            , ColorUtilConstants.RESET);
      }
      fileDir = "./src/main/resources/static/group/";
      if (!fs.existsBlocking(fileDir)) {
        logger.info("{}{}{}", ColorUtilConstants.CYAN_BOLD_BRIGHT,
            "To install the dodex group addon, execute 'npm run group:prod' in 'handicap/src/grpc/client/'"
            , ColorUtilConstants.RESET);
      }
      fileDir = "./src/main/resources/static/node_modules/";
      if (!fs.existsBlocking(fileDir)) {
        logger.info("{}{}{}", ColorUtilConstants.CYAN_BOLD_BRIGHT,
            "To install dodex , execute 'npm install' in 'src/main/resources/static/'"
            , ColorUtilConstants.RESET);
      }
      fileDir = "./src/firebase/node_modules/";
      if (!fs.existsBlocking(fileDir)) {
        logger.info("{}{}{}", ColorUtilConstants.CYAN_BOLD_BRIGHT,
            "To install the firebase client , execute 'npm install' in 'src/firebase/'"
            , ColorUtilConstants.RESET);
      }
      fileDir = "./handicap/src/grpc/client/node_modules/";
      if (!fs.existsBlocking(fileDir)) {
        logger.info("{}{}{}", ColorUtilConstants.CYAN_BOLD_BRIGHT,
            "To install the gRPC client , execute 'npm install' and 'npm run esbuild:build' in 'handicap/src/grpc/client/'"
            , ColorUtilConstants.RESET);
      }
    }
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

  public static HttpServer getServer() {
    return server;
  }

  public static JsonObject getConfig() {
    return config;
  }

  public static Boolean getUseKafka() {
    return Boolean.parseBoolean(useKafka);
  }

  public static Boolean getUseMqtt() {
    return "true".equals(useMqtt);
  }
  
  public static Vertx getRxVertx() {
    return rxVertx;
  }
}
