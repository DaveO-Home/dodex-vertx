package dmo.fs.router;

import com.google.cloud.firestore.Firestore;
import dmo.fs.hib.routes.OpenApiEndpoint;
import dmo.fs.hib.routes.WebSocketEndpoint;
import dmo.fs.kafka.KafkaConsumerDodex;
import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import dmo.fs.vertx.Server;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpServer;
import io.vertx.rxjava3.ext.web.handler.SessionHandler;
import io.vertx.rxjava3.ext.web.sstore.LocalSessionStore;
import io.vertx.rxjava3.ext.web.sstore.SessionStore;
import io.vertx.rxjava3.kafka.client.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class Routes {
  private static final Logger logger = LoggerFactory.getLogger(Routes.class.getName());
  protected Vertx vertx;
  protected Router router;
  protected HttpServer server;
  protected SessionStore sessionStore;
  protected static KafkaProducer<String, Integer> producer;
  Firestore firestore;
  Promise<Router> routerPromise = Promise.promise();
  String defaultDb = new DodexUtil().getDefaultDb();

  public Routes(Router router) throws IOException {
    this.router = router;
  }

  public Routes(Vertx vertx, HttpServer server, Integer vertxVersion)
      throws InterruptedException, IOException, SQLException {
    this.vertx = vertx;
    sessionStore = LocalSessionStore.create(vertx);
    this.server = server;

    String value = System.getenv("VERTXWEB_ENVIRONMENT");
    if (value != null && (value.equals("dev") || value.equals("test"))) {
      DodexUtil.setEnv("dev");
    } else {
      DodexUtil.setEnv("prod");
    }

    Future<Router> routesFuture;
    // @todo: implement group/member openapi for Neo4j, Cassandra & Firebase
    if("oracle".equals(defaultDb) || "mssql".equals(defaultDb)) {
      routesFuture = OpenApiEndpoint.setOpenApiRouter(vertx); // Using blocking Hibernate on Virtual Threads
    } else if("mongo".equals(defaultDb)) {
      routesFuture = OpenApiRouterMongo.setOpenApiRouter(vertx);
    } else {
      routesFuture = OpenApiRouter.setOpenApiRouter(vertx);
    }

    routesFuture.onSuccess(router -> {
      if (Server.getUseKafka()) {
        setMonitorRoute(router);
      }
      setStaticRoute(router);

      try {
        setDodexRoute(router);
      } catch (InterruptedException | IOException | SQLException | ExecutionException e) {
        throw new RuntimeException("OpenApi Route Failure");
      }

      if ("dev".equals(DodexUtil.getEnv())) {
        setTestRoute(router);
      } else {
        setProdRoute(router);
      }

      if (Server.getUseKafka()) {
        Map<String, String> config = new HashMap<>();
        config.put("bootstrap.servers", "localhost:9092");
        config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("value.serializer", "org.apache.kafka.common.serialization.IntegerSerializer");
        config.put("acks", "1");

        producer = KafkaProducer.create(vertx, config);
      }

      routerPromise.complete(router);
    }).onFailure(Throwable::printStackTrace);
  }

  public static KafkaProducer<String, Integer> getProducer() {
    return producer;
  }

  public void setTestRoute(Router router) {
    Route route = router.routeWithRegex(HttpMethod.GET, "/test/[\\w/-]*\\.html|/test[/]?");

    route.handler(routingContext -> {
      routingContext.put("name", "test");
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "text/html");

      int length = routingContext.request().path().length();
      String path = routingContext.request().path();
      String file = length < 7 ? "test/index.html" : path.substring(1);
      response.sendFile(file);
    });
    route.failureHandler(ctx ->
        logger.error("{}FAILURE in /test/ route: {} - {} - {}{}",
            ColorUtilConstants.RED_BOLD_BRIGHT,
            ctx.statusCode(), ctx.currentRoute(), ctx.body(),
            ColorUtilConstants.RESET)
    );
  }

  public void setProdRoute(Router router) {
    Route route = router.routeWithRegex(HttpMethod.GET, "/ddex[/]?|/dodex/.*\\.html");
    route.handler(routingContext -> {
      routingContext.put("name", "prod");
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "text/html");

      int length = routingContext.request().path().length();
      String path = routingContext.request().path();
      String file = length < 8 ? "dodex/index.html" : path.substring(1);

      response.sendFile(file);
    });
    route.failureHandler(ctx ->
        logger.error("{}FAILURE in prod/dodex route: {}{}",
            ColorUtilConstants.RED_BOLD_BRIGHT, ctx.statusCode(), ColorUtilConstants.RESET)
    );
  }

  public void setStaticRoute(Router router) {
    Route staticRoute = router.route();
    StaticHandler staticHandler = StaticHandler.create("static");
    staticHandler.setCachingEnabled(!"dev".equals(DodexUtil.getEnv()));

    router.route("/*").handler(staticHandler)
        .produces("text/plain")
        .produces("text/html")
        .produces("text/markdown")
        .produces("image/*")
        .handler(staticHandler)
    ;

    staticRoute.handler(staticHandler);
    staticRoute.failureHandler(ctx -> {
      logger.error("{}FAILURE in static route: {} -- {} -- {}{}", ColorUtilConstants.RED_BOLD_BRIGHT, ctx.statusCode(), ctx.currentRoute().getPath(), ctx.pathParams(), ColorUtilConstants.RESET);
      ctx.next();
    });

  }

  public void setMonitorRoute(Router router) {
    Route route = router.route(HttpMethod.GET, "/events/:command/:init")
        .produces("application/json");

    route.handler(SessionHandler.create(SessionStore.create(Server.getRxVertx())).getDelegate());
    route.handler(routingContext -> {
      routingContext.put("name", "monitor");
      HttpServerResponse response = routingContext.response();
      String acceptableContentType = routingContext.getAcceptableContentType();
      response.putHeader("content-type", acceptableContentType);
      Session session = routingContext.session();

      try {
        if (session.isEmpty()) {
          session.put("monitor", new KafkaConsumerDodex());
        }
        KafkaConsumerDodex kafkaConsumerDodex = session.get("monitor");

        response.send(kafkaConsumerDodex.list(
            routingContext.pathParam("command"), routingContext.pathParam("init")));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      if ("-1".equals(routingContext.pathParam("init"))) {
        session.destroy();
      }
    });
    route.failureHandler(ctx ->
        logger.error("{}FAILURE in /monitor/ route: {} -- {} -- {} -- {} --{}{}",
            ColorUtilConstants.RED_BOLD_BRIGHT, ctx.statusCode(), ctx.request().method(), ctx.mountPoint(),
            ctx.normalizedPath(), ctx.response().headers().entries().getFirst().getKey(), ColorUtilConstants.RESET)
    );
  }

  public void setDodexRoute(Router router) throws InterruptedException, IOException, SQLException, ExecutionException {
    switch (defaultDb) {
      case "firebase" -> {
        FirebaseRouter firebaseRouter = new FirebaseRouter(vertx);
        firebaseRouter.setWebSocket(server);
        firestore = firebaseRouter.getDbf();
        break;
      }
      case "cassandra" -> {
//        CassandraRouter cassandraRouter = new CassandraRouter(vertx);
//        cassandraRouter.setWebSocket(server);
        break;
      }
      case "neo4j" -> {
        Neo4jRouter neo4jRouter = new Neo4jRouter(vertx);
        neo4jRouter.setWebSocket(server);
        break;
      }
      case "mongo" -> {
        MongoRouter mongoRouter = new MongoRouter(vertx);
        mongoRouter.setWebSocket(server);
        break;
      }
      case "mssql"-> {  // Using blocking Hibernate on Virtual Threads
        WebSocketEndpoint webSocketEndpoint = new WebSocketEndpoint();
        webSocketEndpoint.setWebSocket(server);
        break;
      }
      case "oracle" -> {
        WebSocketEndpoint webSocketEndpoint = new WebSocketEndpoint();
        webSocketEndpoint.setWebSocket(server);
        break;
      }
      case null, default -> {
        DodexRouter dodexRouter = new DodexRouter(vertx);
        dodexRouter.setWebSocket(server);
      }
    }
  }

  public Future<Router> getRouter() {
    return routerPromise.future();
  }

  public Firestore getFirestore() {
    return firestore;
  }
}
