package dmo.fs.spa.router;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.cloud.firestore.Firestore;
import dmo.fs.spa.SpaApplication;
import dmo.fs.spa.utils.SpaLogin;
import dmo.fs.spa.utils.SpaUtil;
import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpServer;
import io.vertx.rxjava3.core.http.HttpServerResponse;
import io.vertx.rxjava3.ext.web.Route;
import io.vertx.rxjava3.ext.web.Router;
import io.vertx.rxjava3.ext.web.Session;
import io.vertx.rxjava3.ext.web.handler.BodyHandler;
import io.vertx.rxjava3.ext.web.handler.CorsHandler;
import io.vertx.rxjava3.ext.web.handler.SessionHandler;
import io.vertx.rxjava3.ext.web.sstore.LocalSessionStore;
import io.vertx.rxjava3.ext.web.sstore.SessionStore;

public class SpaRoutes {
    private static final Logger logger = LoggerFactory.getLogger(SpaRoutes.class.getName());
    private static final String FAILURE = "{\"status\":\"-99\"}";

    protected Vertx vertx;
    protected Router router;
    protected HttpServer server;
    protected SessionStore sessionStore;
    protected Firestore firestore;

    public SpaRoutes(Vertx vertx, HttpServer server, Router router, Firestore firestore) {
        this.vertx = vertx;
        this.router = router;
        this.server = server;
        this.firestore = firestore;
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

        if ("dev".equals(DodexUtil.getEnv())) {
            route.handler(CorsHandler.create().allowedMethod(HttpMethod.GET));
        }

        route.handler(routingContext -> {
            try {
                SpaApplication spaApplication = new SpaApplication();

                spaApplication.setVertx(vertx);
                spaApplication.setDbf(firestore);
                spaApplication.setupDatabase().onSuccess(v -> {
                    Session session = routingContext.session();

                    routingContext.put("name", "getlogin");
                    if (session.get("login") != null) {
                        session.remove("login");
                    }

                    HttpServerResponse response = routingContext.response();
                    response.putHeader("content-type", "application/json");

                    final Optional<String> queryData =
                            Optional.ofNullable(routingContext.request().query());

                    if (queryData.isPresent()) {
                        try {
                            Future<SpaLogin> future = spaApplication
                                    .getLogin(URLDecoder.decode(queryData.get(), StandardCharsets.UTF_8));

                            future.onSuccess(result -> {
                                if (result.getId() == null) {
                                    result.setId(0L);
                                }
                                session.put("login", new JsonObject(result.getMap()));
                                response.end(new JsonObject(result.getMap()).encode());
                            });

                            future.onFailure(failed -> {
                                logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT,
                                        "Add Login Failed: ", failed.getMessage(),
                                        ColorUtilConstants.RESET));
                                response.end(FAILURE);
                            });

                        } catch (InterruptedException
                                 | SQLException e) {
                            logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT,
                                    "Context Configuration failed...: ", e.getMessage(),
                                    ColorUtilConstants.RESET));

                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    }
                }).onFailure(err -> {
                    logger.error(String.format("%sError Setting up Database: %s%s",
                            ColorUtilConstants.RED_BOLD_BRIGHT, err, ColorUtilConstants.RESET));
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

        if ("dev".equals(DodexUtil.getEnv())) {
            route.handler(CorsHandler.create().allowedMethod(HttpMethod.PUT));
        }

        route.handler(BodyHandler.create()).handler(routingContext -> {

            try {

                SpaApplication spaApplication = new SpaApplication();

                spaApplication.setVertx(vertx);
                spaApplication.setDbf(firestore);
                spaApplication.setupDatabase().onSuccess(v -> {
                    Session session = routingContext.session();

                    routingContext.put("name", "putlogin");
                    if (session.get("login") != null) {
                        session.remove("login");
                    }

                    HttpServerResponse response = routingContext.response();
                    response.putHeader("content-type", "application/json");

                    final Optional<String> bodyData =
                            Optional.ofNullable(routingContext.body().asString()); //getBodyAsString());

                    if (bodyData.isPresent()) {
                        try {
                            SpaLogin spaLoginCreated = SpaUtil.createSpaLogin();
                            SpaLogin spaLogin = SpaUtil.parseBody(
                                    URLDecoder.decode(routingContext.body().asString(), StandardCharsets.UTF_8),
                                    spaLoginCreated);

                            JsonObject jsonObject = new JsonObject(spaLogin.getMap());
                            Future<SpaLogin> futureLogin =
                                    spaApplication.getLogin(jsonObject.encode());

                            futureLogin.onSuccess(result -> {
                                if ("0".equals(result.getStatus())) {
                                    result.setStatus("-2");
                                    response.end(new JsonObject(result.getMap()).encode());
                                } else {
                                    Future<SpaLogin> future = null;
                                    try {
                                        future = new SpaApplication()
                                                .addLogin(routingContext.body().asString());
                                    } catch (InterruptedException | SQLException | IOException
                                            | ExecutionException e) {
                                        e.printStackTrace();
                                    }

                                    assert future != null;
                                    future.onSuccess(result2 -> {
                                        session.put("login", new JsonObject(result2.getMap()));
                                        response.end(new JsonObject(result2.getMap()).encode());
                                    });

                                    future.onFailure(failed -> {
                                        logger.error(String.join("",
                                                ColorUtilConstants.RED_BOLD_BRIGHT,
                                                "Add Login failed...: ", failed.getMessage(),
                                                ColorUtilConstants.RESET));
                                        response.end(FAILURE);
                                    });
                                }

                            });

                            futureLogin.onFailure(failed -> {
                                logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT,
                                        "Add Login failed...: ", failed.getMessage(),
                                        ColorUtilConstants.RESET));
                                response.end(FAILURE);
                            });

                        } catch (InterruptedException | SQLException e) {
                            logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT,
                                    "Context Configuration failed...: ", e.getMessage(),
                                    ColorUtilConstants.RESET));

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
        if ("dev".equals(DodexUtil.getEnv())) {
            route.handler(CorsHandler.create().allowedMethod(HttpMethod.DELETE));
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

            final Optional<String> queryData =
                    Optional.ofNullable(routingContext.request().query());
            if (queryData.isPresent()) {
                try {
                    data = String.join("", "{\"status\":\"", status, "\"}");
                } catch (Exception e) {
                    logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT,
                            "Context Configuration failed...: ", e.getMessage(),
                            ColorUtilConstants.RESET));
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
        Route route =
                router.route(HttpMethod.DELETE, "/userlogin/unregister").handler(sessionHandler);

        if ("dev".equals(DodexUtil.getEnv())) {
            route.handler(CorsHandler.create().allowedMethod(HttpMethod.DELETE));
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

                    final Optional<String> queryData =
                            Optional.ofNullable(routingContext.request().query());
                    if (queryData.isPresent()) {
                        try {

                            Future<SpaLogin> future = spaApplication
                                    .unregisterLogin(URLDecoder.decode(queryData.get(), StandardCharsets.UTF_8));

                            future.onSuccess(result -> {
                                session.destroy();
                                response.end(new JsonObject(result.getMap()).encode());
                            });

                            future.onFailure(failed -> {
                                logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT,
                                        "Unregister Login failed...: ", failed.getMessage(),
                                        ColorUtilConstants.RESET));
                                response.end(FAILURE);
                            });

                        } catch (Exception e) {
                            logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT,
                                    "Context Configuration failed...: ", e.getMessage(),
                                    ColorUtilConstants.RESET));
                            e.printStackTrace();
                        }
                    }
                });
            } catch (InterruptedException | IOException | SQLException e1) {
                e1.printStackTrace();
            }
        });
    }

    public Router getRouter() {
        return router;
    }
}
