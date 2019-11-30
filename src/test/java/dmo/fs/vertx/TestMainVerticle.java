package dmo.fs.vertx;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Route;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.Timeout;

import dmo.fs.router.Routes;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;


@ExtendWith(VertxExtension.class)
public class TestMainVerticle {
  @Rule
  public Timeout globalTimeout = Timeout.seconds(3);

  Server server = new Server();

  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(server, testContext.succeeding(id -> {
      testContext.completeNow();
    }));
  }

  @Test
  void verticle_deployed(Vertx vertx, VertxTestContext testContext) throws Throwable {
    testContext.completeNow();
  }

  @Test
  void verticle_configured(Vertx vertx, VertxTestContext testContext) throws Throwable {
    String deploymentId = server.deploymentID();
    
    Routes routes = new Routes(vertx, vertx.createHttpServer());
    List<Route> routesList = routes.getRoutes().getRoutes();

    boolean hasPathDodex = false;
    for(Route route : routesList) {
       if(!hasPathDodex) {
        hasPathDodex = route.getPath() == "/dodex";
       }
    }
    assertNotEquals(deploymentId, null, "deployment id should be generated");
    assertTrue(hasPathDodex, "verticle configured for dodex route"); 
    testContext.completeNow();
  }

}
