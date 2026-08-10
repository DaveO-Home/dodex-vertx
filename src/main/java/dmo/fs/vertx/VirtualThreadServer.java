package dmo.fs.vertx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dmo.fs.router.Routes;
import dmo.fs.spa.router.SpaRoutes;
import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerConfig;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.ServerSSLOptions;
import io.vertx.ext.web.Router;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpServer;
import io.vertx.rxjava3.ext.web.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Locale;

public class VirtualThreadServer extends AbstractVerticle {
  Logger logger = LoggerFactory.getLogger(VirtualThreadServer.class.getName());
  private HttpServer server;
  private int port = 7089;
  private static String development = System.getenv("VERTXWEB_ENVIRONMENT");

  public VirtualThreadServer() {
    Locale.setDefault(Locale.forLanguageTag("US"));
  }

  public void start(Promise<Void> promise) throws SQLException, IOException, InterruptedException {
    HttpServerOptions options = new HttpServerOptions();
    options.setLogActivity(true);
    DodexUtil dodexUtil = new DodexUtil();
    String defaultDb = dodexUtil.getDefaultDb();

    if (development == null || development.isEmpty()
        || development.toLowerCase().startsWith("prod")) {
      development = "prod";
    } else if ("dev".equalsIgnoreCase(development)) {
      port = 8081;
    } else if ("test".equalsIgnoreCase(development)) {
      port = 7081;
    }

    server = configureOptions(vertx, options);

    Routes routes = new Routes(vertx, server, 5);


    routes.getRouter().onSuccess(router -> {
      if ("oracle".equals(defaultDb) || "mssql".equals(defaultDb)) {
        new SpaRoutes(vertx, server, router, routes.getFirestore());
      }
      server.getDelegate().requestHandler(router);

      serverListen(promise, port);
    });
  }

  private void serverListen(Promise<Void> promise, Integer port) {
    server.listen(port).doOnSuccess(v -> {
          promise.complete();
          logger.info("{}{}{}", ColorUtilConstants.YELLOW, "Virtual Threads Verticle started on " + port, ColorUtilConstants.RESET);
        })
        .doOnError(Throwable::printStackTrace).subscribe();
  }

  private HttpServer configureOptions(Vertx vertx, HttpServerOptions options) throws IOException {
    HttpServer srv;
    JsonObject dodexConfig = getAlternateConfig();

    if (dodexConfig.getBoolean("use.ssl") ||
        "true".equalsIgnoreCase(System.getenv("USE_SSL"))) {
      Buffer selfSignedBuffer = vertx.getDelegate().fileSystem().readFileBlocking("ssl/keystore.jks");
      ServerSSLOptions sslOptions = new ServerSSLOptions().setKeyCertOptions(new JksOptions()
          .setValue(selfSignedBuffer)
          .setPassword("some-password"));
      HttpServerConfig config = new HttpServerConfig()
          .setVersions(HttpVersion.HTTP_1_1);
      return vertx.createHttpServer(config, sslOptions);
    } else {
      if (Server.isUnix()) {
        return vertx.createHttpServer(new HttpServerOptions().setTcpFastOpen(true).setTcpCork(true)
            .setTcpQuickAck(true).setReusePort(true).setLogActivity(true)
        );
      }
      return vertx.createHttpServer(options);
    }
  }

  public void setStaticRoute(Router router) {
    io.vertx.ext.web.Route staticRoute = router.route();
    io.vertx.ext.web.handler.StaticHandler staticHandler = io.vertx.ext.web.handler.StaticHandler.create("static");
    staticHandler.setCachingEnabled(!"dev".equals(DodexUtil.getEnv()));

    router.route("/*").handler(staticHandler)
        .produces("text/plain")
        .produces("text/html")
        .produces("text/markdown")
        .produces("images/*")
        .handler(staticHandler)
    ;

    staticRoute.handler(staticHandler);
    staticRoute.failureHandler(ctx -> {
      ;
      logger.error("{}FAILURE in static route: {} -- {} -- {}{}", ColorUtilConstants.RED_BOLD_BRIGHT, ctx.statusCode(), ctx.currentRoute().getPath(), ctx.pathParams(), ColorUtilConstants.RESET);
      ctx.next();
    });
  }

  private JsonObject getAlternateConfig() throws IOException {
    ObjectMapper jsonMapper = new ObjectMapper();
    JsonNode node;

    try (InputStream in = getClass().getResourceAsStream("/application-conf.json")) {
      node = jsonMapper.readTree(in);
    }

    return new JsonObject(node.toString());
  }

  private String parsePath(io.vertx.ext.web.Route route) {
    if (!route.isRegexPath()) {
      return route.getPath();
    }

    String info = route.toString();

    return info.substring(info.indexOf("pattern=") + 8,
        info.indexOf(',', info.indexOf("pattern=")));
  }

  private String parsePath2(Route route) {
    if (!route.isRegexPath()) {
      return route.getPath();
    }

    String info = route.toString();

    return info.substring(info.indexOf("pattern=") + 8,
        info.indexOf(',', info.indexOf("pattern=")));
  }
}
