package dmo.fs.vertx;

import dmo.fs.router.Routes;
import dmo.fs.spa.router.SpaRoutes;
import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpServer;
import io.vertx.rxjava3.ext.web.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
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
      port = 8087;
    } else if ("test".equalsIgnoreCase(development)) {
      port = 7081;
    }
    if (Server.isUnix()) {
      server = configureLinuxOptions(vertx);
    } else {
      server = vertx.createHttpServer(options);
    }

    if ("oracle".equals(defaultDb) || "mssql".equals(defaultDb)) {
      Routes routes = new Routes(vertx, server, 5);

      routes.getRouter().onSuccess(router -> {
        new SpaRoutes(vertx, server, router, routes.getFirestore());

        if (!"prod".equalsIgnoreCase(development)) {
          List<io.vertx.ext.web.Route> routesList = router.getRoutes(); // allRoutes.getRouter().getRoutes();
          for (io.vertx.ext.web.Route r : routesList) {
            String path = parsePath(r);
            String methods = path + (r.methods() == null ? "" : r.methods());
            logger.info("{}{}{}", ColorUtilConstants.CYAN_BOLD_BRIGHT, methods,
                ColorUtilConstants.RESET);
          }
        }

        server.getDelegate().requestHandler(router);

        serverListen(promise, port);
      });
    } else {
      Routes routes = new Routes(vertx, server, 5);
      routes.getRouter().onSuccess(r -> {
        server.getDelegate().requestHandler(r);
        serverListen(promise, port);
      });
    }
  }

  private void serverListen(Promise<Void> promise, Integer port) {
    server.listen(port).doOnSuccess(v -> {
          promise.complete();
          logger.info("{}{}{}", ColorUtilConstants.YELLOW, "Virtual Threads Verticle started on " + port, ColorUtilConstants.RESET);
        })
        .doOnError(Throwable::printStackTrace).subscribe();
  }

  private HttpServer configureLinuxOptions(Vertx vertx) {
    // Available on Linux
    return vertx.createHttpServer(new HttpServerOptions().setTcpFastOpen(true).setTcpCork(true)
        .setTcpQuickAck(true).setReusePort(true).setLogActivity(true)
    );
  }

  public void setStaticRoute(Router router) {
    io.vertx.ext.web.Route staticRoute = router.route();
    io.vertx.ext.web.handler.StaticHandler staticHandler = io.vertx.ext.web.handler.StaticHandler.create("static");
    if ("dev".equals(DodexUtil.getEnv())) {
      staticHandler.setCachingEnabled(false);
    } else {
      staticHandler.setCachingEnabled(true);
    }

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
