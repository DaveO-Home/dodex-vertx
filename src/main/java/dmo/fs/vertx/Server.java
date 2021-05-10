
package dmo.fs.vertx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.modellwerkstatt.javaxbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dmo.fs.router.CassandraRouter;
import dmo.fs.router.Routes;
import dmo.fs.spa.SpaApplication;
import dmo.fs.spa.router.SpaRoutes;
import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.BridgeOptions;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileSystem;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.eventbus.bridge.tcp.TcpEventBusBridge;
import io.vertx.reactivex.ext.web.Route;

public class Server extends AbstractVerticle {
  private static Logger logger;
  private int port;
  private int eventBridgePort = 7032;

  public Server() {
    Locale.setDefault(new Locale("US"));
  }

  public Server(int port) {
    Locale.setDefault(new Locale("US"));
    this.port = port;
  }

  static {
    File logDir = new File("./logs/");
    if (!(logDir.exists())) {
      logDir.mkdir();
    }

    System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$s] %5$s %3$s %n");
    System.setProperty("dmo.fs.level", "INFO");
    System.setProperty("org.jooq.no-logo", "true");
    logger = LoggerFactory.getLogger(Server.class.getName());
  }

  private static String OS = System.getProperty("os.name").toLowerCase();
  private static String development = System.getenv("VERTXWEB_ENVIRONMENT");
  private HttpServer server;
  private JsonObject config;
  private Integer vertxVersion;

  @Override
  public void start(Promise<Void> promise) throws InterruptedException, URISyntaxException, IOException, SQLException {
    if (development == null || development.equals("")) {
      development = "prod";
    } else if ("dev".equalsIgnoreCase(development)) {
      port = 8087;
    } else if ("test".equalsIgnoreCase(development)) {
      port = 8089;
    }
    config = Vertx.currentContext().config();
    if (config.getInteger("http.port") == null) {
      config = getAlternateConfig();
    }
    /* From application-conf.json - Vertx version defaults to 4 */
    vertxVersion = config.getInteger(development + ".vertx.version",
        config.getInteger("vertx.version") == null ? 4 : config.getInteger("vertx.version"));

    Boolean color = "prod".equalsIgnoreCase(development) ? config.getBoolean("prod.color") : config.getBoolean("color");
    if (color != null && !color) {
      ColorUtilConstants.colorOff();
    }

    if (isUnix()) {
      server = configureLinuxOptions(vertx, true, true, true, true);
    } else {
      server = vertx.createHttpServer();
    }

    Routes routes = new Routes(vertx, server, vertxVersion);

    SpaRoutes allRoutes = new SpaRoutes(vertx, server, routes.getRouter());
    FileSystem fs = vertx.fileSystem();
    List<Route> routesList = allRoutes.getRouter().getRoutes();

    for (Route r : routesList) {
      String path = parsePath(r);
      logger.info(String.join("", ColorUtilConstants.CYAN_BOLD_BRIGHT, path + (r.methods() == null ? "" : r.methods()), ColorUtilConstants.RESET));
    }

    server.requestHandler(allRoutes.getRouter());

    try {
      server.listen(config.getInteger(development + ".http.port", this.port == 0 ? 8080 : port), result -> {
        if (result.succeeded()) {
          Integer port = this.port != 0 ? this.port : config.getInteger(development + ".http.port", 8080);
          logger.info(String.join("", ColorUtilConstants.GREEN_BOLD_BRIGHT, "HTTP Started on port: ", port.toString(),
              ColorUtilConstants.RESET));
          promise.complete();
          try {
            if (development.toLowerCase().equals("dev")) {
              String fileStarted = "./server-started";
              if (!fs.existsBlocking(fileStarted)) {
                fs.createFileBlocking(fileStarted);
              }
            }
          } catch (Exception e) {
            logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT, e.getMessage(), ColorUtilConstants.RESET));
          }
        } else {
          promise.fail(result.cause());
        }
      });
    } catch (Error e) {
      logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT, e.getMessage(), ColorUtilConstants.RESET));
    }

    String defaultDb = new DodexUtil().getDefaultDb();
    logger.info(String.format("%sUsing %s database.%s", ColorUtilConstants.PURPLE_BOLD_BRIGHT, defaultDb, ColorUtilConstants.RESET));
    
    if ("cassandra".equals(defaultDb)) {
      TcpEventBusBridge bridge = TcpEventBusBridge.create(vertx,
          new BridgeOptions().addInboundPermitted(new PermittedOptions().setAddress("vertx"))
              .addOutboundPermitted(new PermittedOptions().setAddress("akka"))
              .addInboundPermitted(new PermittedOptions().setAddress("akka"))
              .addOutboundPermitted(new PermittedOptions().setAddress("vertx")));

      eventBridgePort = config.getInteger(development + "bridge.port") == null ? 7032
          : config.getInteger(development + "bridge.port");

      bridge.listen(eventBridgePort, res -> {
        if (res.succeeded()) {
          logger.info(String.format("%s%s%d%s", ColorUtilConstants.GREEN_BOLD_BRIGHT, "TCP Event Bus Bridge Started: ",
              eventBridgePort, ColorUtilConstants.RESET));
          setupEventBridge();
        } else {
          logger.error(String.format("%s%s%s", ColorUtilConstants.RED_BOLD_BRIGHT, res.cause().getMessage(),
              ColorUtilConstants.RESET));
        }
      });
    }
  }

  private void setupEventBridge() {
    // Note; development has the value of "prod" when in production
    Integer port = config.getInteger(development + ".bridge.port");
    EventBus eb = EventBus.create("localhost", port == null ? 7032 : port);
    SpaApplication.setEb(eb);
    CassandraRouter.setEb(eb);
    logger.info(String.format("%sDodex Connected to Event Bus Bridge%s", ColorUtilConstants.BLUE_BOLD_BRIGHT,
        ColorUtilConstants.RESET));
  }

  private HttpServer configureLinuxOptions(Vertx vertx, boolean fastOpen, boolean cork, boolean quickAck,
      boolean reusePort) {
    // Available on Linux
    return vertx.createHttpServer(new HttpServerOptions().setTcpFastOpen(fastOpen).setTcpCork(cork)
        .setTcpQuickAck(quickAck).setReusePort(reusePort)
    // https - generate and get signed your certificate
    // Self signed for testing, per;
    // sslshopper.com/article-most-common-java-keytool-keystore-commands.html
    // keytool -genkey -keyalg RSA -alias selfsigned -keystore keystore.jks 
    // -storepass password -validity 360 -keysize 2048
        // .setKeyStoreOptions(new JksOptions()
        //     .setPath("keystore.jks") // good until April 9, 2022
        //     .setPassword("apassword"))
        //     .setSsl(true)
    );
  }

  private String parsePath(Route route) {
    if(!route.isRegexPath()) {
        return route.getPath();
    }

    String info = route.toString();
    String pattern = info.substring(info.indexOf("pattern=") + 8, info.indexOf(',', info.indexOf("pattern=")));

    return pattern;
  }

  public static boolean isWindows() {
    return OS.indexOf("win") >= 0;
  }

  public static boolean isMac() {
    return OS.indexOf("mac") >= 0;
  }

  public static boolean isUnix() {
    return OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0;
  }

  public static boolean isSolaris() {
    return OS.indexOf("sunos") >= 0;
  }

  public JsonObject getAlternateConfig() throws IOException {
    ObjectMapper jsonMapper = new ObjectMapper();
    JsonNode node;

    try (InputStream in = getClass().getResourceAsStream("/application-conf.json")) {
      node = jsonMapper.readTree(in);
    }

    JsonObject config = new JsonObject(node.toString());
    return config;
  }

  public HttpServer getServer() {
      return server;
  }
}
