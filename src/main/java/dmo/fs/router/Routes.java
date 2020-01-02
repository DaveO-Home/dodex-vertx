package dmo.fs.router;

import java.io.IOException;
import java.sql.SQLException;

import dmo.fs.utils.DodexUtil;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.FaviconHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.TimeoutHandler;

public class Routes {
	// private final static Logger logger = LoggerFactory.getLogger(Routes.class.getName());
	protected Vertx vertx;
	protected Router router;
	protected HttpServer server;

	public Routes(Vertx vertx, HttpServer server) throws InterruptedException, IOException, SQLException {
		this.vertx = vertx;
		router = Router.router(vertx);
		this.server = server;

		String value = System.getenv("VERTXWEB_ENVIRONMENT");
		if (value != null && value.equals("dev")) {
			DodexUtil.setEnv(value);
		} else {
			DodexUtil.setEnv("prod");
		}

		setFavRoute();
		setStaticRoute();
		setDodexRoute();
		if (DodexUtil.getEnv().equals("dev")) {
			setTestRoute();
		} else {
			setProdRoute();
		}
	}

	public void setTestRoute() {
		Route route = router.routeWithRegex(HttpMethod.GET, "/test[/]?|/test/.*\\.html");
		route.handler(routingContext -> {
			HttpServerResponse response = routingContext.response();
			response.putHeader("content-type", "text/html");
			
			int length = routingContext.request().path().length();
			String path = routingContext.request().path();
			String file = length < 7 ? "test/index.html" : path.substring(1);
			
			response.sendFile(file).end();
		});
	}

	public void setProdRoute() {
		Route route = router.routeWithRegex(HttpMethod.GET, "/dodex[/]?|/dodex/.*\\.html");
		route.handler(routingContext -> {
			HttpServerResponse response = routingContext.response();
			response.putHeader("content-type", "text/html");
			
			int length = routingContext.request().path().length();
			String path = routingContext.request().path();
			String file = length < 8 ? "dodex/index.html" : path.substring(1);

			response.sendFile(file).end();
		});
	}

	public void setStaticRoute() {
		StaticHandler staticHandler = StaticHandler.create();
		staticHandler.setWebRoot("static");
		staticHandler.setCachingEnabled(false);
		router.route("/*").handler(staticHandler).handler(TimeoutHandler.create(2000));
	}

	public void setFavRoute() {
		FaviconHandler faviconHandler = FaviconHandler.create();
		router.route().handler(faviconHandler);
	}

	public void setDodexRoute() throws InterruptedException, IOException, SQLException {
		DodexRouter dodexRouter = new DodexRouter(vertx);
		dodexRouter.setWebSocket(server);
	}

	public Router getRoutes() throws InterruptedException {
		return router;
	}
}
