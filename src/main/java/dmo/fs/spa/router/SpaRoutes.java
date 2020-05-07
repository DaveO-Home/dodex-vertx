package dmo.fs.spa.router;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.Optional;

import org.davidmoten.rx.jdbc.Database;

import dmo.fs.spa.SpaApplication;
import dmo.fs.spa.db.SpaDatabase;
import dmo.fs.spa.db.SpaDbConfiguration;
import dmo.fs.utils.ConsoleColors;
import dmo.fs.utils.DodexUtil;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;

public class SpaRoutes {
	private final static Logger logger = LoggerFactory.getLogger(SpaRoutes.class.getName());
	private final static String FAILURE = "{\"status\":\"-99\"}";

	protected Vertx vertx;
	protected Router router;
	protected HttpServer server;
	protected SessionStore sessionStore;
	protected SpaDatabase spaDatabase;
	protected Database db;

	public SpaRoutes(Vertx vertx, HttpServer server, Router router)
			throws InterruptedException, IOException, SQLException {
		this.vertx = vertx;
		this.router = router;
		this.server = server;
		sessionStore = LocalSessionStore.create(vertx);
		spaDatabase = SpaDbConfiguration.getSpaDb();
		db = spaDatabase.getDatabase();

		setGetLoginRoute();
		setPutLoginRoute();
		setLogoutRoute();
		setUnregisterLoginRoute();
	}

	public void setGetLoginRoute() throws InterruptedException, SQLException, IOException {
		SpaApplication spaApplication = new SpaApplication();
		SessionHandler sessionHandler = SessionHandler.create(sessionStore);
		Route route = router.route(HttpMethod.GET, "/userlogin").handler(sessionHandler);

		if (DodexUtil.getEnv().equals("dev")) {
			route.handler(CorsHandler.create("*").allowedMethod(HttpMethod.GET));
		}
		route.handler(routingContext -> {
			JsonObject loginObject = null;
			Session session = routingContext.session();
			String data = null;

			routingContext.put("name", "getlogin");
			if (session.get("login") != null) {
				session.remove("login");
			}

			HttpServerResponse response = routingContext.response();

			final Optional<String> queryData = Optional.ofNullable(routingContext.request().query());

			if (queryData.isPresent()) {
				try {

					loginObject = spaApplication.getLogin(URLDecoder.decode(queryData.get(), "UTF-8"));
					data = loginObject.encode();

					if (loginObject.getString("status").equals("0")) {
						session.put("login", loginObject);
					}

				} catch (UnsupportedEncodingException | InterruptedException | SQLException e) {
					logger.info("{0}Context Configuration failed...{1}{2} ",
							new Object[] { ConsoleColors.RED_BOLD_BRIGHT, e.getMessage(), ConsoleColors.RESET });

				} catch (Exception exception) {
					exception.printStackTrace();
				}
			}

			if (data == null) {
				data = FAILURE;
			}

			response.end(data);
		});
	}

	public void setPutLoginRoute() throws InterruptedException, IOException, SQLException {
		SpaApplication spaApplication = new SpaApplication();
		SessionHandler sessionHandler = SessionHandler.create(sessionStore);
		Route route = router.route(HttpMethod.PUT, "/userlogin").handler(sessionHandler);

		if (DodexUtil.getEnv().equals("dev")) {
			route.handler(CorsHandler.create("*").allowedMethod(HttpMethod.PUT));
		}
		route.handler(BodyHandler.create()).handler(routingContext -> {
			JsonObject loginObject = null;
			Session session = routingContext.session();
			String data = null;

			routingContext.put("name", "putlogin");
			if (session.get("login") != null) {
				session.remove("login");
			}

			HttpServerResponse response = routingContext.response();

			final Optional<String> bodyData = Optional.ofNullable(routingContext.getBodyAsString());

			if (bodyData.isPresent()) {
				try {

					loginObject = spaApplication.addLogin(routingContext.getBodyAsString());
					data = loginObject.encode();

					if (loginObject.getString("status").equals("0")) {
						session.put("login", loginObject);
					}

				} catch (InterruptedException | SQLException e) {
					logger.info("{0}Context Configuration failed...{1}{2} ",
							new Object[] { ConsoleColors.RED_BOLD_BRIGHT, e.getMessage(), ConsoleColors.RESET });

				} catch (Exception exception) {
					exception.printStackTrace();
				}
			}

			if (data == null) {
				data = FAILURE;
			}

			response.end(data);
		});
	}

	public void setLogoutRoute() throws InterruptedException, SQLException, IOException {
		SessionHandler sessionHandler = SessionHandler.create(sessionStore);
		Route route = router.route(HttpMethod.DELETE, "/userlogin").handler(sessionHandler);
		if (DodexUtil.getEnv().equals("dev")) {
			route.handler(CorsHandler.create("*").allowedMethod(HttpMethod.DELETE));
		}
		route.handler(routingContext -> {
			Session session = routingContext.session();
			String data = null;

			routingContext.put("name", "getlogin");
			String status = "0";
			if (!session.isEmpty()) {
				session.destroy();
			} else {
				status = "-3";
			}

			HttpServerResponse response = routingContext.response();

			final Optional<String> queryData = Optional.ofNullable(routingContext.request().query());
			if (queryData.isPresent()) {
				try {

					data = String.join("", "{\"status\":\"", status, "\"}");

				} catch (Exception e) {
					logger.info("{0}Context Configuration failed...{1}{2} ",
							new Object[] { ConsoleColors.RED_BOLD_BRIGHT, e.getMessage(), ConsoleColors.RESET });
					// e.printStackTrace();
				}
			}

			if (data == null) {
				data = FAILURE;
			}
			response.end(data);
		});
	}

	public void setUnregisterLoginRoute() throws InterruptedException, SQLException, IOException {
		SpaApplication spaApplication = new SpaApplication();
		SessionHandler sessionHandler = SessionHandler.create(sessionStore);
		Route route = router.route(HttpMethod.DELETE, "/userlogin/unregister").handler(sessionHandler);

		if (DodexUtil.getEnv().equals("dev")) {
			route.handler(CorsHandler.create("*").allowedMethod(HttpMethod.DELETE));
		}

		route.handler(routingContext -> {
			JsonObject loginObject = null;
			Session session = routingContext.session();
			String data = null;

			routingContext.put("name", "unregisterlogin");

			if (!session.isEmpty()) {
				session.destroy();
			}
			;

			HttpServerResponse response = routingContext.response();

			final Optional<String> queryData = Optional.ofNullable(routingContext.request().query());
			if (queryData.isPresent()) {
				try {

					loginObject = spaApplication.unregisterLogin(URLDecoder.decode(queryData.get(), "UTF-8"));
					data = loginObject.encode();
					session.destroy();

				} catch (Exception e) {
					logger.info("{0}Context Configuration failed...{1}{2} ",
							new Object[] { ConsoleColors.RED_BOLD_BRIGHT, e.getMessage(), ConsoleColors.RESET });
					// e.printStackTrace();
				}
			}

			if (data == null) {
				data = FAILURE;
			}

			response.end(data);
		});
	}

	public Router getRouter() throws InterruptedException {
		return router;
	}
}
