package dmo.fs.vertx;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.Timeout;

import dmo.fs.router.Routes;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.ext.web.Route;


@ExtendWith(VertxExtension.class)
public class TestMainVerticle {
  @Rule
  public Timeout globalTimeout = Timeout.seconds(3);

  Server server = new Server(8085);

  @BeforeEach
  void deployVerticle(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(server, testContext.succeeding(id -> {
      testContext.completeNow();
    }));
  }

  @Test
  @DisplayName("Verticle Deployed")
  void verticleDeployed(Vertx vertx, VertxTestContext testContext) throws Throwable {
    testContext.completeNow();
  }

  @Test
  @DisplayName("Verticle Configured")
  void verticleConfigured(Vertx vertx, VertxTestContext testContext) throws Throwable {
    String deploymentId = server.deploymentID();
    io.vertx.reactivex.core.Vertx vertx2 = io.vertx.reactivex.core.Vertx.vertx();
    Routes routes = new Routes(vertx2, server.getServer(), 4);
    List<Route> routesList = routes.getRouter().getRoutes();

    boolean hasPathDodex = false;

    for(Route route : routesList) {
      if(!hasPathDodex && route.getPath() != null) {
        hasPathDodex = route.getPath().equals("/");  // made routes more generic
      }
    }

    assertNotEquals(deploymentId, null, "deployment id should be generated");
    assertTrue(hasPathDodex, "verticle configured for dodex route"); 
    testContext.completeNow();
  }

}
