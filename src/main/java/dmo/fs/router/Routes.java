package dmo.fs.router;

import java.io.IOException;
import java.sql.SQLException;

import dmo.fs.utils.DodexUtil;
import io.vertx.core.http.HttpMethod;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Route;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.CorsHandler;
import io.vertx.reactivex.ext.web.handler.FaviconHandler;
import io.vertx.reactivex.ext.web.handler.StaticHandler;
import io.vertx.reactivex.ext.web.handler.TimeoutHandler;
import io.vertx.reactivex.ext.web.sstore.LocalSessionStore;
import io.vertx.reactivex.ext.web.sstore.SessionStore;

public class Routes {
	protected Vertx vertx;
	protected Router router;
	protected HttpServer server;
	protected SessionStore sessionStore;
	Integer counter = 0;

	public Routes(Vertx vertx, HttpServer server, Integer vertxVersion) throws InterruptedException, IOException, SQLException {
		this.vertx = vertx;
		router = Router.router(vertx);
		sessionStore = LocalSessionStore.create(vertx);
		this.server = server;

		String value = System.getenv("VERTXWEB_ENVIRONMENT");
		if (value != null && (value.equals("dev") || value.equals("test"))) {
			DodexUtil.setEnv("dev");
		} else {
			DodexUtil.setEnv("prod");
		}
		DodexUtil.setVertx(vertx);
		
		setFavRoute(vertxVersion);
		setStaticRoute();
		setDodexRoute();

		if (DodexUtil.getEnv().equals("dev")) {
			setTestRoute();
		} else {
			setProdRoute();
		}
	}

	public void setTestRoute() {
		Route route = router.routeWithRegex(HttpMethod.GET, "/test/[\\w/-]*\\.html|/test[/]?");
		
		route.handler(routingContext -> {
			routingContext.put("name", "test");
			HttpServerResponse response = routingContext.response();
			response.putHeader("content-type", "text/html");
			
			int length = routingContext.request().path().length();
			String path = routingContext.request().path();
			String file = length < 7 ? "test/index.html" : path.substring(1);
			
			response.sendFile(file);
			response.end();
		});
	}

	public void setProdRoute() {
		Route route = router.routeWithRegex(HttpMethod.GET, "/dodex[/]?|/dodex/.*\\.html");
		route.handler(routingContext -> {
			routingContext.put("name", "prod");
			HttpServerResponse response = routingContext.response();
			response.putHeader("content-type", "text/html");
			
			int length = routingContext.request().path().length();
			String path = routingContext.request().path();
			String file = length < 8 ? "dodex/index.html" : path.substring(1);

			response.sendFile(file);
			response.end();
		});
	}

	public void setStaticRoute() {
		StaticHandler staticHandler = StaticHandler.create();
		staticHandler.setWebRoot("static");
		staticHandler.setCachingEnabled(false);
		
		Route staticRoute = router.route("/*").handler(TimeoutHandler.create(2000));
		if (DodexUtil.getEnv().equals("dev")) {
			staticRoute.handler(CorsHandler.create("*"/*Need ports 8087 & 9876*/).allowedMethod(HttpMethod.GET));
		}
		staticRoute.handler(staticHandler);
	}

	public void setFavRoute(Integer vertxVersion) {
		FaviconHandler faviconHandler = null;

		if (vertxVersion == 4) {
			faviconHandler = FaviconHandler.create(vertx);
		}
		// if (vertxVersion == 3) {
		// 	faviconHandler = FaviconHandler.create();
		// }
		router.route().handler(faviconHandler);
	}

	public void setDodexRoute() throws InterruptedException, IOException, SQLException {
		DodexUtil du = new DodexUtil();
		String defaultDbName = du.getDefaultDb();

        if ("cassandra".equals(defaultDbName)) {
			try {
				CassandraRouter cassandraRouter = new CassandraRouter(vertx);
				cassandraRouter.setWebSocket(server);
			} catch(Exception ex) {
				ex.printStackTrace();
			}
        } else {
			try {
				DodexRouter dodexRouter = new DodexRouter(vertx);
				dodexRouter.setWebSocket(server);
			} catch(Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public Router getRouter() throws InterruptedException {
		return router;
	}
}
