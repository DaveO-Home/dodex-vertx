package dmo.fs.db.mongodb;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.model.ValidationAction;
import com.mongodb.client.model.ValidationLevel;
import dmo.fs.db.DbConfiguration;
import dmo.fs.db.MessageUser;
import dmo.fs.db.MessageUserImpl;
import dmo.fs.utils.DodexUtil;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.CreateCollectionOptions;
import io.vertx.ext.mongo.IndexOptions;
import io.vertx.ext.mongo.ValidationOptions;
import io.vertx.mutiny.core.Promise;
import io.vertx.rxjava3.ext.mongo.MongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class DodexDatabaseMongo extends DbMongo {
  private final static Logger logger = LoggerFactory.getLogger(DodexDatabaseMongo.class.getName());
  protected Properties dbProperties;
  protected Map<String, String> dbOverrideMap = new ConcurrentHashMap<>();
  protected Map<String, String> dbMap;
  protected JsonNode defaultNode;
  protected String webEnv = System.getenv("VERTXWEB_ENVIRONMENT");
  protected DodexUtil dodexUtil = new DodexUtil();
  protected MongoClient mongoClient;
  private final static Boolean debug = false;

  public DodexDatabaseMongo(Map<String, String> dbOverrideMap, Properties dbOverrideProps)
      throws IOException {
    super();

    defaultNode = dodexUtil.getDefaultNode();

    webEnv = webEnv == null || "prod".equals(webEnv) ? "prod" : "dev";

    dbMap = dodexUtil.jsonNodeToMap(defaultNode, webEnv);
    dbProperties = dodexUtil.mapToProperties(dbMap);

    if (dbOverrideProps != null && !dbOverrideProps.isEmpty()) {
      this.dbProperties = dbOverrideProps;
    }
    if (dbOverrideMap != null) {
      this.dbOverrideMap = dbOverrideMap;
    }

    assert dbOverrideMap != null;
    DbConfiguration.mapMerge(dbMap, dbOverrideMap);
  }

  public DodexDatabaseMongo() throws IOException {
    super();

    defaultNode = dodexUtil.getDefaultNode();
    webEnv = webEnv == null || "prod".equals(webEnv) ? "prod" : "dev";
    dbMap = dodexUtil.jsonNodeToMap(defaultNode, webEnv);
    dbProperties = dodexUtil.mapToProperties(dbMap);
  }

  @Override
  public MessageUser createMessageUser() {
    return new MessageUserImpl();
  }

  @Override
  public Promise<MongoClient> databaseSetup() {
    if ("dev".equals(webEnv)) {
      // dbMap.put("dbname", "myDbname"); // this wiil be merged into the default map
      DbConfiguration.configureTestDefaults(dbMap, dbProperties);
    } else {
      DbConfiguration.configureDefaults(dbMap, dbProperties); // Prod
    }

    Promise<MongoClient> promise = Promise.promise();
    /*
      Make sure SuperUser has added the "dodex" user
      "use dodex"
      "db.createUser(
          {
            user: "dodex",
            pwd: passwordPrompt(), // or cleartext password
            roles: [
              { role: "dbOwner", db: "dodex" },
           ]
          }
        )
      "
     */
    JsonObject config = new JsonObject();
    config.put("host", dbMap.get("host")).put("port", Integer.parseInt(dbMap.get("port")))
        .put("username", dbProperties.get("username")).put("password", dbProperties.get("password"))
        .put("authSource", dbProperties.get("authSource"))
        .put("db_name", dbMap.get("dbname"))
        .put("useObjectId", Boolean.parseBoolean(dbMap.get("useObjectId")))
        .put("minPoolSize", Integer.parseInt(dbMap.get("minPoolSize")));

    if (debug) {
      DodexUtil.getVertx().getOrCreateContext().exceptionHandler(Throwable::printStackTrace);
    }

    mongoClient = MongoClient.createShared(DodexUtil.getVertx(), config);

    mongoClient.getCollections().doOnSuccess(collections -> {
      if (!collections.contains("user_message")) {
        JsonObject validator = getUserMessage();

        CreateCollectionOptions cco = new CreateCollectionOptions(validator);
        ValidationOptions vo = new ValidationOptions(validator);
        vo.setValidationAction(ValidationAction.ERROR)
            .setValidationLevel(ValidationLevel.STRICT);
        cco.setValidationOptions(vo);

        mongoClient.createCollectionWithOptions("user_message", cco).doOnComplete(() -> {
          JsonObject namePassword = getNamePasswordIndex();
          IndexOptions indexOptions = new IndexOptions(new JsonObject().put("unique", true).put("sparse", false));

          mongoClient.createIndexWithOptions("user_message", namePassword, indexOptions).subscribe();
        }).subscribe();
      }

      if (!collections.contains("message_user")) {
        JsonObject validator = getMessageUser();
        CreateCollectionOptions cco = new CreateCollectionOptions(validator);
        ValidationOptions vo = new ValidationOptions(validator);
        vo.setValidationAction(ValidationAction.ERROR)
            .setValidationLevel(ValidationLevel.STRICT);
        cco.setValidationOptions(vo);

        mongoClient.createCollectionWithOptions("message_user", cco).doOnComplete(() -> {
          JsonObject userId = getUserIdIndex();
          IndexOptions indexOptions = new IndexOptions(new JsonObject().put("unique", false).put("sparse", false));
          mongoClient.createIndexWithOptions("message_user", userId, indexOptions).subscribe();
        }).subscribe();
      }

      if (!collections.contains("group_member")) {
        JsonObject validator = getGroupMember();
        CreateCollectionOptions cco = new CreateCollectionOptions(validator);
        ValidationOptions vo = new ValidationOptions(validator);
        vo.setValidationAction(ValidationAction.ERROR)
            .setValidationLevel(ValidationLevel.STRICT);
        cco.setValidationOptions(vo);

        mongoClient.createCollectionWithOptions("group_member", cco).doOnComplete(() -> {
          JsonObject groupName = getGroupNameIndex();
          IndexOptions indexOptions = new IndexOptions(new JsonObject().put("unique", false).put("sparse", false));
          mongoClient.createIndexWithOptions("group_member", groupName, indexOptions).subscribe();
        }).subscribe();
      }
      if(debug) {
        logger.info("Collections: {}", collections);
      }

    }).doOnError(Throwable::printStackTrace)
      .doFinally(() -> promise.complete(mongoClient)).subscribe();

    return promise;
  }
  public MongoClient getMongoClient() {
    return mongoClient;
  }
}
