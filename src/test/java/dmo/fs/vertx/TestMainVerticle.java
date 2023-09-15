package dmo.fs.vertx;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.concurrent.TimeUnit;
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

}
