package dmo.fs.vertx;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.concurrent.TimeUnit;

// import org.influxdb.BatchOptions;
// import org.influxdb.InfluxDB;
// import org.influxdb.InfluxDBFactory;
// import org.influxdb.dto.Point;
// import org.influxdb.dto.Query;
// import org.influxdb.dto.QueryResult;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dmo.fs.router.Routes;
import io.vertx.core.Future;
import io.vertx.ext.web.openapi.router.OpenAPIRoute;
import io.vertx.ext.web.openapi.router.RequestExtractor;
import io.vertx.ext.web.openapi.router.RouterBuilder;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.rxjava3.core.Vertx;

public class TestMainVerticle {
  static Logger logger = LoggerFactory.getLogger(TestMainVerticle.class);
  @Rule
  public Timeout globalTimeout = Timeout.seconds(3);

  static Server server = new Server(8085); // environment variable  "VERTXWEB_ENVIRONMENT=dev" should be set
  VertxTestContext testContext;
  static Vertx vertx = Vertx.vertx();

  @BeforeAll
  static void deployVerticle() {
    vertx.deployVerticle(server).doOnSuccess(val -> {
      assertNotNull(val);
    });
    System.out.println("@BeforeAll - executes once before all test methods in this class");
  }

  @AfterEach
  void tearDown() {
    if (testContext != null) {
      testContext.completeNow();
    }
  }

  @Test
  @DisplayName("Verticle Deployed")
  void verticleDeployed() throws Throwable {
    assertNotEquals(null, server.deploymentID(), "deployment id should be generated");
  }

  @Test
  @DisplayName("Verticle Configured")
  void verticleConfigured() throws Throwable {
    Routes routes = new Routes(vertx, server.getServer(), 4);
    routes.getRouter().onSuccess(router -> {
      List<io.vertx.ext.web.Route> routesList = router.getRoutes();

      boolean hasPathDodex = false;

      for (io.vertx.ext.web.Route route : routesList) {
        if (!hasPathDodex && route.getPath() != null) {
          hasPathDodex = "/".equals(route.getPath()); // made routes more generic
        }
      }

      assertTrue(hasPathDodex, "verticle configured for dodex route");
    });
  }
}


class TestOpenApi {
  static Logger logger = LoggerFactory.getLogger(TestMainVerticle.class);
  static Vertx vertx = Vertx.vertx();
  OpenAPIContract apiContract;

  @Rule
  public Timeout globalTimeout = Timeout.seconds(3);

  @DisplayName("Openapi Contract")
  @Test
  void openApiContract() throws Throwable {
    VertxTestContext testContext = new VertxTestContext();

    Future<OpenAPIContract> contract = getContract(testContext);

    testContext.awaitCompletion(2, TimeUnit.SECONDS);

    apiContract = contract.result();

    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    } else {
      assertEquals("/addGroup", apiContract.findPath("/addGroup").toString(), "Path in Contract");
    }

  };

  @DisplayName("Openapi Router")
  @Test
  void openApiRouter() throws Throwable {
    VertxTestContext testContext = new VertxTestContext();
    Future<OpenAPIContract> contract = getContract(testContext);

    testContext.awaitCompletion(2, TimeUnit.SECONDS);

    apiContract = contract.result();

    RouterBuilder routerBuilder =
        RouterBuilder.create(vertx.getDelegate(), apiContract, RequestExtractor.withBodyHandler());

    List<OpenAPIRoute> routesList = routerBuilder.getRoutes();
    assertTrue(!routesList.isEmpty(), "OpenApi router initialized");
  }

  Future<OpenAPIContract> getContract(VertxTestContext testContext) {
    return OpenAPIContract
        .from(vertx.getDelegate(), "openapi/groupApi31.yml").onSuccess(openApiContract -> {
          testContext.succeedingThenComplete();
        }).onComplete(openApiContract -> {
          testContext.succeedingThenComplete();
        }).onFailure(err -> {
          testContext.failingThenComplete();
          testContext.failNow(err);
        });
  }

//  @Test
//  void influxDB() throws Throwable {
//    // Create an object to handle the communication with InfluxDB.
//// (best practice tip: reuse the 'influxDB' instance when possible)
//    final String serverURL = "http://127.0.0.1:8086", username = "root", password = "root";
//    final InfluxDB influxDB = InfluxDBFactory.connect(serverURL, username, password);
//    assertTrue(influxDB.ping().isGood(), "InfluxDB ping");
//// Create a database...
//// https://docs.influxdata.com/influxdb/v1.7/query_language/database_management/
//    String databaseName = "NOAA_water_database";
////    influxDB.query(new Query("CREATE DATABASE " + databaseName));
//    influxDB.setDatabase(databaseName);
//
//// ... and a retention policy, if necessary.
//// https://docs.influxdata.com/influxdb/v1.7/query_language/database_management/
//    String retentionPolicyName = "one_day_only";
////    influxDB.query(new Query("CREATE RETENTION POLICY " + retentionPolicyName
////        + " ON " + databaseName + " DURATION 1d REPLICATION 1 DEFAULT"));
////    influxDB.setRetentionPolicy(retentionPolicyName);
//
//// Enable batch writes to get better performance.
//    influxDB.enableBatch(
//        BatchOptions.DEFAULTS
//            .threadFactory(runnable -> {
//              Thread thread = new Thread(runnable);
//              thread.setDaemon(true);
//              return thread;
//            })
//    );
//
//// Close it if your application is terminating or you are not using it anymore.
//    Runtime.getRuntime().addShutdownHook(new Thread(influxDB::close));
//
//// Write points to InfluxDB.
//    influxDB.write(Point.measurement("h2o_feet")
//        .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
//        .tag("location", "santa_monica")
//        .addField("level description", "below 3 feet")
//        .addField("water_level", 2.064d)
//        .build());
//
//    influxDB.write(Point.measurement("h2o_feet")
//        .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
//        .tag("location", "coyote_creek")
//        .addField("level description", "between 6 and 9 feet")
//        .addField("water_level", 8.12d)
//        .build());
//
//// Wait a few seconds in order to let the InfluxDB client
//// write your points asynchronously (note: you can adjust the
//// internal time interval if you need via 'enableBatch' call).
//    Thread.sleep(5_000L);
//
//// Query your data using InfluxQL.
//// https://docs.influxdata.com/influxdb/v1.7/query_language/data_exploration/#the-basic-select-statement
////    QueryResult queryResult = influxDB.query(new Query("SELECT * FROM h2o_feet"));
////
////    System.out.println(queryResult);
//// It will print something like:
//// QueryResult [results=[Result [series=[Series [name=h2o_feet, tags=null,
////      columns=[time, level description, location, water_level],
////      values=[
////         [2020-03-22T20:50:12.929Z, below 3 feet, santa_monica, 2.064],
////         [2020-03-22T20:50:12.929Z, between 6 and 9 feet, coyote_creek, 8.12]
////      ]]], error=null]], error=null]
//  }

}
