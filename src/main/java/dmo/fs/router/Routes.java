package dmo.fs.router;

import java.io.IOException;
import java.sql.SQLException;

import com.google.cloud.firestore.Firestore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dmo.fs.utils.ColorUtilConstants;
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
	private static final Logger logger = LoggerFactory.getLogger(Routes.class.getName());
	protected Vertx vertx;
	protected Router router;
	protected HttpServer server;
	protected SessionStore sessionStore;
	Integer counter = 0;
	Firestore firestore;

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
		
		setFavRoute();
		setStaticRoute();
		setDodexRoute();

		if ("dev".equals(DodexUtil.getEnv())) {
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
		});
		route.failureHandler(ctx -> 
			logger.error(String.format("%sFAILURE in /test/ route: %d%s", ColorUtilConstants.RED_BOLD_BRIGHT, ctx.statusCode(), ColorUtilConstants.RESET))
		  );
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
		});
		route.failureHandler(ctx -> 
			logger.error(String.format("%sFAILURE in prod/dodex route: %d%s", ColorUtilConstants.RED_BOLD_BRIGHT, ctx.statusCode(), ColorUtilConstants.RESET))
		  );
	}

	public void setStaticRoute() {
		StaticHandler staticHandler = StaticHandler.create();
		staticHandler.setWebRoot("static");
		staticHandler.setCachingEnabled(false);
        
        router.routeWithRegex("/.*\\.md|" + "/.*/templates/.*")
            .produces("text/plain")
            .produces("text/markdown")
            .handler(ctx -> {
                HttpServerResponse response = ctx.response();
                String acceptableContentType = ctx.getAcceptableContentType();
                response.putHeader("content-type", acceptableContentType);
                response.sendFile(ctx.normalizedPath());
                staticHandler.handle(ctx);
            });
            
		Route staticRoute = router.route("/*").handler(TimeoutHandler.create(2000));
		if ("dev".equals(DodexUtil.getEnv())) {
			staticRoute.handler(CorsHandler.create("*" /* Need ports 8087 & 9876 */ ).allowedMethod(HttpMethod.GET));
		}
		staticRoute.handler(staticHandler);
		staticRoute.failureHandler(ctx -> 
			logger.error(String.format("%sFAILURE in static route: %d%s", ColorUtilConstants.RED_BOLD_BRIGHT, ctx.statusCode(), ColorUtilConstants.RESET))
		  );
	}

	public void setFavRoute() {
		FaviconHandler faviconHandler = null;

		faviconHandler = FaviconHandler.create(vertx);
		
		router.route().handler(faviconHandler);
	}

	public void setDodexRoute() throws InterruptedException, IOException, SQLException {
		DodexUtil du = new DodexUtil();
		String defaultDbName = du.getDefaultDb();

        if ("firebase".equals(defaultDbName)) {
			try {
				FirebaseRouter firebaseRouter = new FirebaseRouter(vertx);
				firebaseRouter.setWebSocket(server);
				firestore = firebaseRouter.getDbf();
			} catch(Exception ex) {
				ex.printStackTrace();
				throw ex;
			}
        } else if ("cassandra".equals(defaultDbName)) {
			try {
				CassandraRouter cassandraRouter = new CassandraRouter(vertx);
				cassandraRouter.setWebSocket(server);
			} catch(Exception ex) {
				ex.printStackTrace();
				throw ex;
			}
        } else {
			try {
				DodexRouter dodexRouter = new DodexRouter(vertx);
				dodexRouter.setWebSocket(server);
			} catch(Exception ex) {
				ex.printStackTrace();
				throw ex;
			}
		}
	}

	public Router getRouter() {
		return router;
	}

	public Firestore getFirestore() {
		return firestore;
	}
}
