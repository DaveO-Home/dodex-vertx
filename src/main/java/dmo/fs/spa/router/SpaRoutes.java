package dmo.fs.spa.router;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dmo.fs.spa.SpaApplication;
import dmo.fs.spa.utils.SpaLogin;
import dmo.fs.spa.utils.SpaUtil;
import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Route;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.Session;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.CorsHandler;
import io.vertx.reactivex.ext.web.handler.SessionHandler;
import io.vertx.reactivex.ext.web.sstore.LocalSessionStore;
import io.vertx.reactivex.ext.web.sstore.SessionStore;

public class SpaRoutes {
    private final static Logger logger = LoggerFactory.getLogger(SpaRoutes.class.getName());
    private final static String FAILURE = "{\"status\":\"-99\"}";

    protected Vertx vertx;
    protected Router router;
    protected HttpServer server;
    protected SessionStore sessionStore;

    public SpaRoutes(Vertx vertx, HttpServer server, Router router)
            throws InterruptedException, IOException, SQLException {
        this.vertx = vertx;
        this.router = router;
        this.server = server;
        sessionStore = LocalSessionStore.create(vertx);
        DodexUtil.setVertx(vertx);

        setGetLoginRoute();
        setPutLoginRoute();
        setLogoutRoute();
        setUnregisterLoginRoute();
    }

    public void setGetLoginRoute() {
        SessionHandler sessionHandler = SessionHandler.create(sessionStore);
        Route route = router.route(HttpMethod.GET, "/userlogin").handler(sessionHandler);

        if (DodexUtil.getEnv().equals("dev")) {
            route.handler(CorsHandler.create("*").allowedMethod(HttpMethod.GET));
        }

        route.handler(routingContext -> {
            try {
                SpaApplication spaApplication = new SpaApplication();

                spaApplication.setVertx(vertx);
                spaApplication.setupDatabase().onSuccess(v -> {
                    Session session = routingContext.session();

                    routingContext.put("name", "getlogin");
                    if (session.get("login") != null) {
                        session.remove("login");
                    }

                    HttpServerResponse response = routingContext.response();
                    response.putHeader("content-type", "application/json");

                    final Optional<String> queryData = Optional.ofNullable(routingContext.request().query());

                    if (queryData.isPresent()) {
                        try {
                            Future<SpaLogin> future = spaApplication
                                    .getLogin(URLDecoder.decode(queryData.get(), "UTF-8"));

                            future.onSuccess(result -> {
                                if (result.getId() == null) {
                                    result.setId(0l);
                                }
                                session.put("login", new JsonObject(result.getMap()));
                                response.end(new JsonObject(result.getMap()).encode());
                            });

                            future.onFailure(failed -> {
                                logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT, "Add Login Failed: ",
                                        failed.getMessage(), ColorUtilConstants.RESET));
                                response.end(FAILURE);
                            });

                        } catch (UnsupportedEncodingException | InterruptedException | SQLException e) {
                            logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT,
                                    "Context Configuration failed...: ", e.getMessage(), ColorUtilConstants.RESET));

                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    }
                }).onFailure(err -> {
                    logger.error(String.format("%sError Setting up Database: %s%s", ColorUtilConstants.RED_BOLD_BRIGHT,
                            err, ColorUtilConstants.RESET));
                    err.printStackTrace();
                });

            } catch (/* InterruptedException | IOException | SQLException | */ Exception e1) {
                e1.printStackTrace();
            }
        });
    }

    public void setPutLoginRoute() {
        SessionHandler sessionHandler = SessionHandler.create(sessionStore);
        Route route = router.route(HttpMethod.PUT, "/userlogin").handler(sessionHandler);

        if (DodexUtil.getEnv().equals("dev")) {
            route.handler(CorsHandler.create("*").allowedMethod(HttpMethod.PUT));
        }

        route.handler(BodyHandler.create()).handler(routingContext -> {

            try {

                SpaApplication spaApplication = new SpaApplication();

                spaApplication.setVertx(vertx);
                spaApplication.setupDatabase().onSuccess(v -> {
                    Session session = routingContext.session();

                    routingContext.put("name", "putlogin");
                    if (session.get("login") != null) {
                        session.remove("login");
                    }

                    HttpServerResponse response = routingContext.response();
                    response.putHeader("content-type", "application/json");

                    final Optional<String> bodyData = Optional.ofNullable(routingContext.getBodyAsString());

                    if (bodyData.isPresent()) {
                        try {
                            SpaLogin spaLogin = SpaUtil.createSpaLogin();
                            spaLogin = SpaUtil.parseBody(URLDecoder.decode(routingContext.getBodyAsString(), "UTF-8"),
                                    spaLogin);

                            JsonObject jsonObject = new JsonObject(spaLogin.getMap());
                            Future<SpaLogin> futureLogin = spaApplication.getLogin(jsonObject.encode());

                            futureLogin.onSuccess(result -> {
                                if (result.getStatus().equals("0")) {
                                    result.setStatus("-2");
                                    response.end(new JsonObject(result.getMap()).encode());
                                } else {
                                    Future<SpaLogin> future = null;
                                    try {
                                        future = new SpaApplication().addLogin(routingContext.getBodyAsString());
                                    } catch (InterruptedException | SQLException | IOException e) {
                                        e.printStackTrace();
                                    }

                                    future.onSuccess(result2 -> {
                                        session.put("login", new JsonObject(result2.getMap()));
                                        response.end(new JsonObject(result2.getMap()).encode());
                                    });

                                    future.onFailure(failed -> {
                                        logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT,
                                                "Add Login failed...: ", failed.getMessage(),
                                                ColorUtilConstants.RESET));
                                        response.end(FAILURE);
                                    });
                                }

                            });

                            futureLogin.onFailure(failed -> {
                                logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT,
                                        "Add Login failed...: ", failed.getMessage(), ColorUtilConstants.RESET));
                                response.end(FAILURE);
                            });

                        } catch (InterruptedException | SQLException e) {
                            logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT,
                                    "Context Configuration failed...: ", e.getMessage(), ColorUtilConstants.RESET));

                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    }
                });
            } catch (InterruptedException | IOException | SQLException e1) {
                e1.printStackTrace();
            }
        });
    }

    public void setLogoutRoute() {
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
            response.putHeader("content-type", "application/json");

            final Optional<String> queryData = Optional.ofNullable(routingContext.request().query());
            if (queryData.isPresent()) {
                try {
                    data = String.join("", "{\"status\":\"", status, "\"}");
                } catch (Exception e) {
                    logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT,
                            "Context Configuration failed...: ", e.getMessage(), ColorUtilConstants.RESET));
                }
            }

            if (data == null) {
                data = FAILURE;
            }
            response.end(data);
        });
    }

    public void setUnregisterLoginRoute() {
        SessionHandler sessionHandler = SessionHandler.create(sessionStore);
        Route route = router.route(HttpMethod.DELETE, "/userlogin/unregister").handler(sessionHandler);

        if (DodexUtil.getEnv().equals("dev")) {
            route.handler(CorsHandler.create("*").allowedMethod(HttpMethod.DELETE));
        }

        route.handler(routingContext -> {
            try {

                SpaApplication spaApplication = new SpaApplication();

                spaApplication.setVertx(vertx);
                spaApplication.setupDatabase().onSuccess(v -> {
                    Session session = routingContext.session();

                    routingContext.put("name", "unregisterlogin");

                    if (!session.isEmpty()) {
                        session.destroy();
                    }

                    HttpServerResponse response = routingContext.response();
                    response.putHeader("content-type", "application/json");
                    
                    final Optional<String> queryData = Optional.ofNullable(routingContext.request().query());
                    if (queryData.isPresent()) {
                        try {

                            Future<SpaLogin> future = spaApplication
                                    .unregisterLogin(URLDecoder.decode(queryData.get(), "UTF-8"));

                            future.onSuccess(result -> {
                                session.destroy();
                                response.end(new JsonObject(result.getMap()).encode());
                            });

                            future.onFailure(failed -> {
                                logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT, "Unregister Login failed...: ",
                                        failed.getMessage(), ColorUtilConstants.RESET));
                                response.end(FAILURE);
                            });

                        } catch (Exception e) {
                            logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT,
                                    "Context Configuration failed...: ", e.getMessage(), ColorUtilConstants.RESET));
                            e.printStackTrace();
                        }
                    }
                });
            } catch (InterruptedException | IOException | SQLException e1) {
                e1.printStackTrace();
            }
        });
    }

    public Router getRouter() throws InterruptedException {
        return router;
    }
}
