package dmo.fs.vertx;

import dmo.fs.router.Routes;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.openapi.router.OpenAPIRoute;
import io.vertx.ext.web.openapi.router.RequestExtractor;
import io.vertx.ext.web.openapi.router.RouterBuilder;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.rxjava3.core.Promise;
import io.vertx.rxjava3.core.Vertx;
import org.junit.Rule;
import org.junit.jupiter.api.*;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class TestMainVerticle {
  static Logger logger = LoggerFactory.getLogger(TestMainVerticle.class);
  @Rule
  public Timeout globalTimeout = Timeout.seconds(3);

  static Server server = new Server(8085); // environment variable  "VERTXWEB_ENVIRONMENT=dev" should be set
  VertxTestContext testContext;
  static Vertx vertx = Vertx.vertx();

  @BeforeAll
  static void deployVerticle() {
    System.setProperty("kotlinTest", "false");
    vertx.deployVerticle(server).doOnSuccess(Assertions::assertNotNull);
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
    Routes routes = new Routes(vertx, Server.getServer(), 4);
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
  static Logger logger = LoggerFactory.getLogger(TestOpenApi.class);
  //  static Vertx vertx = Vertx.vertx();
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

  }

  @DisplayName("Openapi Router")
  @Test
  void openApiRouter() throws Throwable {
    VertxTestContext testContext = new VertxTestContext();
    Future<OpenAPIContract> contract = getContract(testContext);

    testContext.awaitCompletion(2, TimeUnit.SECONDS);

    apiContract = contract.result();

    RouterBuilder routerBuilder =
        RouterBuilder.create(TestMainVerticle.vertx.getDelegate(), apiContract, RequestExtractor.withBodyHandler());

    List<OpenAPIRoute> routesList = routerBuilder.getRoutes();
    assertTrue(!routesList.isEmpty(), "OpenApi router initialized");
  }

  @DisplayName("Retrieve Handicap Html")
  @Test
  void returnHandicapHtml() throws Throwable {
    HttpClient httpClient = TestMainVerticle.vertx.getDelegate().createHttpClient();

    Future<HttpClientRequest> request =
        httpClient
            .request(HttpMethod.GET, 8085, "localhost", "/handicap.html");

    Future<HttpClientRequest> fut = request.compose(result -> {
          System.out.println("Composed on Client Request: " + result);
          Promise<HttpClientRequest> promise = Promise.promise();

          promise.complete(result);
          promise.future();
          return Future.succeededFuture(result);
        }
        , m -> {
          m.printStackTrace();
          return null;
        });

    HttpClientRequest req = fut.await();
    req.end();
    HttpClientResponse response = req.response().await();
    String data = response.body().await().toString();

    assertNotEquals("", data);

    boolean containsTitle = data.contains("<title>Golf Handicap Index</title>");

    assertTrue(containsTitle, "Handicap is up");
  }

  Future<OpenAPIContract> getContract(VertxTestContext testContext) {
    return OpenAPIContract
        .from(TestMainVerticle.vertx.getDelegate(), "openapi/groupApi31.yml").onSuccess(openApiContract -> {
          testContext.succeedingThenComplete();
        }).onComplete(openApiContract -> {
          testContext.succeedingThenComplete();
        }).onFailure(err -> {
          testContext.failingThenComplete();
          testContext.failNow(err);
        });
  }
}
