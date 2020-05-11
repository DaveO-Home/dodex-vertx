
package dmo.fs.vertx;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;

import dmo.fs.router.Routes;
import dmo.fs.spa.router.SpaRoutes;
import dmo.fs.utils.ConsoleColors;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Route;

public class Server extends AbstractVerticle {
  private static Logger logger;
  private int port = 0;

  public Server() {
  }

  public Server(int port) {
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

  public void start(Promise<Void> promise)
      throws InterruptedException, URISyntaxException, IOException, SQLException {
    if(development == null || development.equals("")) {
      development = "prod";
    }
    else if(development.toLowerCase().equals("dev")) {
      port = 8087;
    }
    else if(development.toLowerCase().equals("test")) {
      port = 8089;
    }

    if (isUnix()) {
      server = configureLinuxOptions(vertx, true, true, true, true);
    } else {
      server = vertx.createHttpServer();
    }

    Routes routes = new Routes(vertx, server);
    SpaRoutes allRoutes = new SpaRoutes(vertx, server, routes.getRouter());
    FileSystem fs = vertx.fileSystem();
    
    List<Route> routesList = allRoutes.getRouter().getRoutes();

    for (Route r : routesList) {
      logger.info("{0}Using Path {1}{2}",
          new Object[] { ConsoleColors.CYAN_BOLD_BRIGHT, r.getPath(), ConsoleColors.RESET });
    }

    server.requestHandler(allRoutes.getRouter());

    try {
      server.listen(config().getInteger("http.port", this.port == 0 ? 8081 : port), result -> {      
        if (result.succeeded()) {
          Integer port = this.port != 0? this.port : config().getInteger("http.port", 8080);
          logger.info("{0}Started on port: " + port + "{1}",
              new Object[] { ConsoleColors.GREEN_BOLD_BRIGHT, ConsoleColors.RESET });
          promise.complete();
          try {
              if(development.toLowerCase().equals("dev")) {
                Future<Void> future1 = Future.future(promise2 -> {
                  fs.createFile("./server-started", promise2);
                  promise2.complete();
                });
                if(!future1.succeeded()) {
                  throw new Exception("server-started");
                };
              }
          } catch (Exception e) {
            logger.info("{0}Error creating dev file: {1} {2}",
              new Object[] { ConsoleColors.RED_BOLD_BRIGHT, e.getMessage(), ConsoleColors.RESET });
          }
        } else {
          promise.fail(result.cause());
        }
      });
    } catch (Error e) {
      logger.error("{0}Using Path {1}{2}",
          new Object[] { ConsoleColors.RED_BOLD_BRIGHT, e.getMessage(), ConsoleColors.RESET });
    }
  }

  private HttpServer configureLinuxOptions(Vertx vertx, 
      boolean fastOpen, 
      boolean cork, 
      boolean quickAck,
      boolean reusePort) {
    // Available on Linux
    return vertx.createHttpServer(new HttpServerOptions()
        .setTcpFastOpen(fastOpen)
        .setTcpCork(cork)
        .setTcpQuickAck(quickAck)
        .setReusePort(reusePort)
    // https - generate and get signed your certificate
    // Self signed for testing, per; sslshopper.com/article-most-common-java-keytool-keystore-commands.html
    // keytool -genkey -keyalg RSA -alias selfsigned -keystore keystore.jks -storepass password -validity 360 -keysize 2048
        // .setKeyStoreOptions(new JksOptions()
        // .setPath("server-keystore.jks")
        // .setPassword("password"))
        // .setSsl(true)
        );
  }

  public static boolean isWindows() {
    return (OS.indexOf("win") >= 0);
  }

  public static boolean isMac() {
    return (OS.indexOf("mac") >= 0);
  }

  public static boolean isUnix() {
    return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0);
  }

  public static boolean isSolaris() {
    return (OS.indexOf("sunos") >= 0);
  }
}
