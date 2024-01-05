package dmo.fs.vertx;

import dmo.fs.router.Routes;
import dmo.fs.utils.ColorUtilConstants;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;

public class VirtualThreadServer extends AbstractVerticle {
  Logger logger = LoggerFactory.getLogger(VirtualThreadServer.class.getName());
  private HttpServer server;

  public VirtualThreadServer() {
    Locale.setDefault(Locale.forLanguageTag("US"));
  }

  public void start(Promise<Void> promise) throws SQLException, IOException, InterruptedException {
    HttpServerOptions options = new HttpServerOptions();
    options.setLogActivity(true);

    if (Server.isUnix()) {
      server = configureLinuxOptions(vertx);
    } else {
      server = vertx.createHttpServer(options);
    }
    Router router = Router.router(vertx);
    Route getThreads = router.routeWithRegex(HttpMethod.GET, "/threads/[\\w/-]*\\.html|/threads[/]?");
    getThreads.handler(routingContext -> {
      routingContext.put("name", "threads");
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "text/html");

      int length = routingContext.request().path().length();
      String path = routingContext.request().path();
      String file = length < 9 ? "threads/index.html" : path.substring(1);

      Future.await(response.sendFile(file));
      Future.await(response.end());
    });
    Routes routes = new Routes(router);
    routes.setStaticRoute(router);
    server.requestHandler(router);

    server = Future.await(
        server.listen(8881).onComplete(v -> {
              promise.complete();
              logger.info("{}{}{}",ColorUtilConstants.YELLOW, "Virtual Threads Verticle started on 8881/threads", ColorUtilConstants.RESET);
            })
            .onFailure(Throwable::printStackTrace));
  }

  private HttpServer configureLinuxOptions(Vertx vertx) {
    // Available on Linux
    return vertx.createHttpServer(new HttpServerOptions().setTcpFastOpen(true).setTcpCork(true)
        .setTcpQuickAck(true).setReusePort(true).setLogActivity(true)
    );
  }
}
