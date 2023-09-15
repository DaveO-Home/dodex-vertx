package dmo.fs.router;

import dmo.fs.db.GroupOpenApiSql;
import dmo.fs.utils.DodexUtil;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.router.OpenAPIRoute;
import io.vertx.ext.web.openapi.router.RequestExtractor;
import io.vertx.ext.web.openapi.router.RouterBuilder;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.validation.ResponseValidator;
import io.vertx.openapi.validation.ValidatableResponse;
import io.vertx.openapi.validation.ValidatedRequest;
import io.vertx.rxjava3.core.Promise;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.ext.web.handler.*;
import io.vertx.rxjava3.ext.web.sstore.SessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class OpenApiRouter {
    private static final Logger logger = LoggerFactory.getLogger(OpenApiRouter.class.getName());
    
    public static Future<Router> setOpenApiRouter(Vertx vertx) {
        JsonObject config = new JsonObject();
        SessionStore store = SessionStore.create(vertx, config);
        SessionHandler sessionHandler = SessionHandler.create(store);
        Promise<Router> openApiPromise = Promise.promise();
        OpenAPIContract.from(vertx.getDelegate(), "openapi/groupApi31.yml").onSuccess(openApiContract -> {
            RouterBuilder routerBuilder =
                    RouterBuilder.create(vertx.getDelegate(), openApiContract, RequestExtractor.withBodyHandler());
            routerBuilder.rootHandler(sessionHandler.getDelegate());

            List<OpenAPIRoute> routesList = routerBuilder.getRoutes();
            for (OpenAPIRoute openApiRoute : routesList) {
                if("groupById".equals(openApiRoute.getOperation().getOperationId())) {
                  openApiRoute.addHandler(routingContext -> {
                    ValidatedRequest validatedRequest =
                        routingContext.get(RouterBuilder.KEY_META_DATA_VALIDATED_REQUEST);
                    JsonObject getGroupJson = validatedRequest.getBody().getJsonObject();
                    validatedRequest.getHeaders();

                    GroupOpenApiSql groupOpenApiSql = new GroupOpenApiSql();
                    try {
                      groupOpenApiSql.getMembersList(getGroupJson).onSuccess(getGroupObject -> {

                        HttpServerResponse response = routingContext.response();

                        ResponseValidator validator = ResponseValidator.create(vertx.getDelegate(), openApiContract);

                        ValidatableResponse validateResponse =
                            ValidatableResponse.create(200, getGroupObject.toBuffer(), "application/json");

                        validator.validate(validateResponse, "addGroup")
                            .onSuccess(validatedResponse -> validatedResponse.send(response))
                            .onFailure(Throwable::printStackTrace);
                      });
                    } catch (InterruptedException | SQLException | IOException e) {
                      e.printStackTrace();
                    }
                  });
                }

                HttpMethod method = openApiRoute.getOperation().getHttpMethod();

                if (HttpMethod.PUT.equals(method)) {
                    openApiRoute.addHandler(routingContext -> {
                        ValidatedRequest validatedRequest =
                            routingContext.get(RouterBuilder.KEY_META_DATA_VALIDATED_REQUEST);
                        JsonObject addGroupJson = validatedRequest.getBody().getJsonObject();
                        validatedRequest.getHeaders();

                        GroupOpenApiSql groupOpenApiSql = new GroupOpenApiSql();
                        try {
                            groupOpenApiSql.addGroupAndMembers(addGroupJson).onSuccess(addGroupObject -> {

                            HttpServerResponse response = routingContext.response();
                            
                            ResponseValidator validator = ResponseValidator.create(vertx.getDelegate(), openApiContract);
                            
                            ValidatableResponse validateResponse =
                                ValidatableResponse.create(200, addGroupObject.toBuffer(), "application/json");

                            validator.validate(validateResponse, "addGroup")
                                .onSuccess(validatedResponse -> validatedResponse.send(response))
                                .onFailure(Throwable::printStackTrace);
                            });
                        } catch (InterruptedException | SQLException | IOException e) {
                            e.printStackTrace();
                        }
                    });
                }

                if (HttpMethod.DELETE.equals(method)) {
                    openApiRoute.addHandler(routingContext -> {
                        ValidatedRequest validatedRequest =
                            routingContext.get(RouterBuilder.KEY_META_DATA_VALIDATED_REQUEST);
                        JsonObject deleteGroupJson = validatedRequest.getBody().getJsonObject();
                        validatedRequest.getHeaders();

                        GroupOpenApiSql groupOpenApiSql = new GroupOpenApiSql();
                        try {
                            groupOpenApiSql.deleteGroupOrMembers(deleteGroupJson).onSuccess(deleteGroupObject -> {

                                HttpServerResponse response = routingContext.response();

                                ResponseValidator validator = ResponseValidator.create(vertx.getDelegate(), openApiContract);

                                ValidatableResponse validateResponse =
                                    ValidatableResponse.create(200, deleteGroupObject.toBuffer(), "application/json");

                                validator.validate(validateResponse, "addGroup")
                                    .onSuccess(validatedResponse -> validatedResponse.send(response))
                                    .onFailure(Throwable::printStackTrace);
                            });
                        } catch (InterruptedException | SQLException | IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
            /*
            Setting up routing in correct Vertx order
             */
            FaviconHandler faviconHandler = FaviconHandler.create(vertx, "favicon.ico");
            routerBuilder.rootHandler(faviconHandler.getDelegate())
                    .rootHandler(TimeoutHandler.create(2000).getDelegate());
            routerBuilder
                    .rootHandler(SessionHandler.create(SessionStore.create(vertx)).getDelegate());
            if ("dev".equals(DodexUtil.getEnv())) {
                routerBuilder.rootHandler(CorsHandler.create().allowedMethod(HttpMethod.GET)
                        .getDelegate()); /* Need ports 8087 & 9876 */
            }

            routerBuilder.rootHandler(BodyHandler.create().setBodyLimit(10000000)
                    .setDeleteUploadedFilesOnEnd(true).setHandleFileUploads(true).getDelegate());
            routerBuilder.rootHandler(RoutingContext::next);
            
            openApiPromise.complete(routerBuilder.createRouter());

        }).onFailure(Throwable::printStackTrace);
        
        return openApiPromise.future();
    }
}
