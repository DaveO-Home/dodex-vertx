
package dmo.fs.db.mongodb;

import dmo.fs.db.MessageUser;
import dmo.fs.utils.DodexUtil;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.BulkOperation;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.mutiny.core.Promise;
import io.vertx.rxjava3.SingleHelper;
import io.vertx.rxjava3.core.http.ServerWebSocket;
import io.vertx.rxjava3.ext.mongo.MongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public abstract class DbMongoBase {
  private static final Logger logger = LoggerFactory.getLogger(DbMongoBase.class.getName());

  public abstract MessageUser createMessageUser();

  private final static DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

  private Uni<MessageUser> addUser(MessageUser messageUser, MongoClient mongoClient) {
    String mongoDate = getMongoDate();
    Promise<MessageUser> addUserPromise = Promise.promise();
    messageUser.setLastLogin(new Timestamp(System.currentTimeMillis()));

    JsonObject insertUser = new JsonObject();
    insertUser
        .put("name", messageUser.getName())
        .put("password", messageUser.getPassword())
        .put("ip", messageUser.getIp())
        .put("last_login", mongoDate);

    mongoClient.insert("user_message", insertUser).doOnSuccess(_id -> {
      messageUser.set_id(_id);
      addUserPromise.complete(messageUser);
    }).doOnError(Throwable::printStackTrace).subscribe();

    return addUserPromise.future();
  }

  private MessageUser updateUser(MessageUser messageUser, MongoClient mongoClient) {
    String mongoDate = getMongoDate();
    messageUser.setLastLogin(new Timestamp(System.currentTimeMillis()));

    JsonObject queryUser = uniqueQuery(messageUser);
    JsonObject updateUser = new JsonObject();
    updateUser.put("$set", new JsonObject().put("last_login", mongoDate));

    mongoClient.findOneAndUpdate("user_message", queryUser, updateUser)
        .doOnSuccess(result -> {
        })
        .doOnError(Throwable::printStackTrace).subscribe();

    return messageUser;
  }

  public Promise<Map<String, Integer>> processUserMessages(ServerWebSocket ws, MessageUser messageUser)
      throws Exception {
    MongoClient mongoClient = MongoClient.createShared(DodexUtil.getVertx(), new JsonObject());
    Promise<Map<String, Integer>> promise = Promise.promise();
    Map<String, Integer> counts = new ConcurrentHashMap<>();
    JsonObject query = new JsonObject();
    JsonObject fields =  new JsonObject().put("from_handle", 1).put("message", 1).put("post_date", 1);
    FindOptions findOptions = new FindOptions().setFields(fields);
    query.put("user_id", new JsonObject().put("$eq", messageUser.get_id()));
    messageUser.setLastLogin(new Timestamp(System.currentTimeMillis()));
    counts.put("messages", 0);

    mongoClient.findWithOptions("message_user", query, findOptions).doOnSuccess(data -> {
      for(JsonObject message : data) {
        String fromHandle = message.getString("from_handle");
        String messagePostdate = message.getString("post_date");
        if(!messagePostdate.endsWith("Z")) {
          messagePostdate = messagePostdate.concat("Z");
        }
        ZonedDateTime postDate = ZonedDateTime.parse(messagePostdate);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd-HH:mm:ss");
        ws.writeTextMessage(fromHandle + postDate.format(formatter) + " " + message.getString("message"));
        Integer count = counts.get("messages") + 1;
        counts.put("messages", count);
      }
    }).doFinally(() -> promise.complete(counts)).doOnError(Throwable::printStackTrace)
        .subscribe(v -> removeMessages(messageUser.get_id(), mongoClient), Throwable::printStackTrace);

    return promise;
  }

  private void removeMessages(String user, MongoClient mongoClient) {
    JsonObject userQuery = new JsonObject().put("user_id", new JsonObject().put("$eq", user));
    mongoClient.removeDocuments("message_user", userQuery)
        .doOnComplete(() -> mongoClient.close().subscribe()).subscribe(v->{}, Throwable::printStackTrace);
  }

  public Promise<MessageUser> deleteUser(MessageUser messageUser)
      throws InterruptedException, ExecutionException {
    MongoClient mongoClient = MongoClient.createShared(DodexUtil.getVertx(), new JsonObject());
    Promise<MessageUser> promise = Promise.promise();

    mongoClient.removeDocument("user_message", uniqueQuery(messageUser)).doOnSuccess(result -> {
          //
        }).doFinally(mongoClient::close)
        .doOnError(Throwable::printStackTrace).subscribe();

    return promise;
  }

  public Promise<MessageUser> addMessage(MessageUser messageUser, String message,
         List<String> undelivered) throws InterruptedException, ExecutionException {
    MongoClient mongoClient = MongoClient.createShared(DodexUtil.getVertx(), new JsonObject());
    String mongoDate = getMongoDate();
    Promise<MessageUser> messagesPromise = Promise.promise();
    Promise<List<BulkOperation>> finishedPromise = Promise.promise();
    List<BulkOperation> bulkList = new ArrayList<>();
    messageUser.setLastLogin(new Timestamp(System.currentTimeMillis()));

    for (String user : undelivered) {
      Single.just(user).subscribe(SingleHelper.toObserver(userName -> {
        Disposable disposable = mongoClient.find("user_message", new JsonObject().put("name", user))
            .doOnSuccess(result -> {
              JsonObject bulkOp = new JsonObject()
                  .put("name", result.getFirst().getString("name"))
                  .put("password", result.getFirst().getString("password"))
                  .put("from_handle", messageUser.getName())
                  .put("message", message)
                  .put("post_date", mongoDate)
                  .put("user_id", result.getFirst().getJsonObject("_id").getString("$oid"));
              bulkList.add(BulkOperation.createInsert(bulkOp));

              if (userName.result().equals(undelivered.getLast())) {
                finishedPromise.complete(bulkList);
              }
            }).doOnError(Throwable::printStackTrace).subscribe();
      }));
    }

    finishedPromise.future().onItem().invoke(bulkData -> {
      messagesPromise.complete(messageUser);
      mongoClient.bulkWrite("message_user", bulkData)
          .doOnError(err -> logger.info("BulkData: {}", err.getMessage()))
          .subscribe(v -> mongoClient.close().subscribe(), Throwable::printStackTrace);
    }).subscribe().asCompletionStage();

    return messagesPromise;
  }

  public Promise<MessageUser> selectUser(MessageUser messageUser)
      throws InterruptedException, SQLException, ExecutionException {
    Promise<MessageUser> promise = Promise.promise();
    MongoClient mongoClient = MongoClient.createShared(DodexUtil.getVertx(), new JsonObject());

    JsonObject query = uniqueQuery(messageUser);

    mongoClient.find("user_message", query).doOnSuccess(result -> {
          if (result.isEmpty()) {
            addUser(messageUser, mongoClient);
          } else {
            messageUser.set_id(result.getFirst().getJsonObject("_id").getString("$oid"));
            MessageUser messageUser2 = updateUser(messageUser, mongoClient);
          }
        })
        .doFinally(() -> {
          mongoClient.close().doOnComplete(() -> promise.complete(messageUser)).subscribe();
        })
        .doOnError(Throwable::printStackTrace).subscribe();

    return promise;
  }

  public Promise<StringBuilder> buildUsersJson(MessageUser messageUser)
      throws InterruptedException {
    MongoClient mongoClient = MongoClient.createShared(DodexUtil.getVertx(), new JsonObject());
    Promise<StringBuilder> promise = Promise.promise();
    JsonArray ja = new JsonArray();
    JsonObject getOthers = new JsonObject();
    getOthers.put("name", new JsonObject().put("$ne", messageUser.getName()));
    FindOptions fo = new FindOptions();
    fo.setSort(new JsonObject().put("name", 1));
    fo.setHint(new JsonObject().put("name", 1).put("password", 2));

    mongoClient.findWithOptions("user_message", getOthers, fo).doOnSuccess(results -> {
      for (JsonObject jo : results) {
        ja.add(new JsonObject().put("name", jo.getString("name")));
      }
    }).doFinally(() -> {
      promise.complete(new StringBuilder(ja.toString()));
      mongoClient.close().subscribe();
    }).doOnError(Throwable::printStackTrace).subscribe();

    return promise;
  }

  private JsonObject uniqueQuery(MessageUser messageUser) {
    return new JsonObject()
        .put("name", new JsonObject().put("$eq", messageUser.getName()))
        .put("$and", new JsonArray().add(new JsonObject()
            .put("password", new JsonObject().put("$eq", messageUser.getPassword()))));
  }

  public static String getMongoDate() {
    return formatter.format(LocalDateTime.now());
  }
}
