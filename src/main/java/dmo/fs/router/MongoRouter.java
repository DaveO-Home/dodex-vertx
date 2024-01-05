package dmo.fs.router;

import dmo.fs.db.DbConfiguration;
import dmo.fs.db.mongodb.DodexMongo;
import dmo.fs.db.MessageUser;
import dmo.fs.kafka.KafkaEmitterDodex;
import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import dmo.fs.utils.ParseQueryUtilHelper;
import dmo.fs.vertx.Server;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.mutiny.core.Promise;
import io.vertx.rxjava3.core.Context;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpServer;
import io.vertx.rxjava3.core.http.ServerWebSocket;
import io.vertx.rxjava3.core.shareddata.LocalMap;
import io.vertx.rxjava3.core.shareddata.SharedData;
import io.vertx.rxjava3.ext.mongo.MongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class MongoRouter {
  private static final Logger logger = LoggerFactory.getLogger(MongoRouter.class.getName());
  private static Vertx vertx;
  private final Map<String, ServerWebSocket> clients = new ConcurrentHashMap<>();
  private DodexMongo dodexMongo;
  protected Promise<MongoClient> dbPromise;
  private static final String LOGFORMAT = "{}{}{}{}";
  private static SharedData sd;
  private static LocalMap<Object, Object> wsChatSessions;
  private final Context context;
  private KafkaEmitterDodex ke;

  public MongoRouter(final Vertx vertx) {
    MongoRouter.vertx = vertx;
    if (Server.getUseKafka()) {
      ke = new KafkaEmitterDodex();
    }
    context = Vertx.currentContext();
    sd = vertx.sharedData();
    wsChatSessions = sd.getLocalMap("ws.dodex.sessions");
  }

  public void setWebSocket(final HttpServer server)
      throws InterruptedException, IOException, SQLException, ExecutionException {
    /*
     * You can customize the db config here by: Map = db configuration, Properties = credentials
     * e.g. Map overrideMap = new Map(); Properties overrideProperties = new Properties(); set
     * override or additional values... dodexDatabase =
     * DbConfiguration.getDefaultDb(overrideMap, overrideProperties);
     */
    dodexMongo = DbConfiguration.getDefaultDb();
    dbPromise = dodexMongo.databaseSetup();
    dbPromise.future().onItem().call(mongoClient -> {
//        dodexMongo.setMongoClient(mongoClient);
        return Uni.createFrom().item(mongoClient);
    }).onItem().invoke(mongoClient -> cleanUsers()).subscribeAsCompletionStage();

    String startupMessage = "In Production";

    startupMessage = "dev".equals(DodexUtil.getEnv()) ? "In Development" : startupMessage;
    logger.info(LOGFORMAT, ColorUtilConstants.BLUE_BOLD_BRIGHT, startupMessage,
        ColorUtilConstants.RESET, "");

    Handler<ServerWebSocket> handler = ws -> {

      String handle = URLDecoder.decode(
          ParseQueryUtilHelper.getQueryMap(ws.query()).get("handle"),
          StandardCharsets.UTF_8);
      logger.info(LOGFORMAT, ColorUtilConstants.BLUE_BOLD_BRIGHT, handle,
          ColorUtilConstants.RESET, "");

      final DodexUtil dodexUtil = new DodexUtil();

      if (!"/dodex".equals(ws.path())) {
        ws.close();
      } else {
        final MessageUser messageUser = dodexMongo.createMessageUser();
        wsChatSessions.put(ws.remoteAddress().toString(), URLDecoder
            .decode(ws.uri().toString(), StandardCharsets.UTF_8));
        clients.put(ws.remoteAddress().toString(), ws);
        if (ke != null) {
          ke.setValue("sessions", clients.size());
        }
        ws.closeHandler(ch -> {
          if (logger.isInfoEnabled()) {
            logger.info(String.join("", ColorUtilConstants.BLUE_BOLD_BRIGHT,
                "Closing ws-connection to client: ", messageUser.getName(), ColorUtilConstants.RESET));
          }
          wsChatSessions.remove(ws.remoteAddress().toString());
          clients.remove(ws.remoteAddress().toString());
          if (ke != null) {
            ke.setValue("sessions", wsChatSessions.size());
          }
        });
        /*
         * Web Socket OnMessage
         */
        onMessage(ws, messageUser, dodexUtil);
        /*
         * websocket.onConnection()
         */
        String id;
        Map<String, String> query;

        query = ParseQueryUtilHelper
            .getQueryMap((String) wsChatSessions.get(ws.remoteAddress().toString()));

        handle = query.get("handle");
        id = query.get("id");

        messageUser.setName(handle);
        messageUser.setPassword(id);
        messageUser.setIp(ws.remoteAddress().toString());

        try {
          Promise<MessageUser> future = dodexMongo.selectUser(messageUser);
          future.future().onItem().call(messageUser2 -> {
            try {
              Promise<StringBuilder> userJson =
                  dodexMongo.buildUsersJson(messageUser2);

              userJson.future().onItem().call(json -> {
                ws.writeTextMessage("connected:" + json);
                /*
                 * Send undelivered messages and remove user related messages.
                 */
                try {
                  dodexMongo.processUserMessages(ws, messageUser2).future()
                      .onItem().call(counts -> {
                        final int messageCount = counts.get("messages");
                        if (messageCount > 0) {
                          logger.info(String.format(
                              "%sMessages Delivered: %d to %s%s",
                              ColorUtilConstants.BLUE_BOLD_BRIGHT,
                              messageCount, messageUser.getName(),
                              ColorUtilConstants.RESET));
                          if (ke != null) {
                            ke.setValue("delivered", messageCount);
                          }
                        }
                        return Uni.createFrom().item(0);
                      }).onFailure().invoke(Throwable::printStackTrace)
                      .subscribeAsCompletionStage();
                } catch (Exception e) {
                  e.printStackTrace();
                }
                return Uni.createFrom().item(0);
              }).onFailure().invoke(Throwable::printStackTrace).subscribeAsCompletionStage();
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            return Uni.createFrom().item(0);
          }).onFailure().invoke(Throwable::printStackTrace).subscribeAsCompletionStage();

        } catch (InterruptedException | ExecutionException | SQLException e) {
          e.printStackTrace();
        }
      }
    };
    server.webSocketHandler(handler);
  }

  private void onMessage(ServerWebSocket ws, MessageUser messageUser, DodexUtil dodexUtil) {
    ws.handler(data -> {
      final ArrayList<String> onlineUsers = new ArrayList<>();
      final String message = data.getString(0, data.length());

      // Checking if message or command
      Map<String, String> returnObject = dodexUtil.commandMessage(message);

      // message with command stripped out
      String[] computedMessage = {""};
      String[] command = {""};

      computedMessage[0] = returnObject.get("message");
      command[0] = returnObject.get("command");

      Promise<MessageUser> promise = Promise.promise();
      promise.complete(null);
      Promise<MessageUser> continued = null;

      if (";removeuser".equals(command[0])) {
        try {
          continued = dodexMongo.deleteUser(messageUser);
        } catch (InterruptedException | ExecutionException e) {
          e.printStackTrace();
          ws.writeTextMessage(
              "Your Previous handle did not delete: " + e.getMessage());
        }
      } else {
        continued = promise;
      }
      /*
       * Using Mutiny for reactive Neo4j access
       */
      if (continued != null) {
        continued.future().onItem().call(result -> {
          String selectedUsers = "";
          if (!computedMessage[0].isEmpty()) {
            // private users to send message
            selectedUsers = returnObject.get("selectedUsers");
            final Set<String> websockets = clients.keySet();
            Map<String, String> query = null;

            for (final String websocket : websockets) {
              final ServerWebSocket webSocket = clients.get(websocket);
              if (!webSocket.isClosed()) {
                if (!websocket.equals(ws.remoteAddress().toString())) {
                  // broadcast message
                  query = ParseQueryUtilHelper.getQueryMap((String) wsChatSessions
                      .get(webSocket.textHandlerID()));
                  final String handle = query.get("handle");
                  if (selectedUsers.isEmpty()
                      && command[0].isEmpty()) {
                    webSocket.writeTextMessage(messageUser.getName() + ": "
                        + computedMessage[0]);
                    // private message
                  } else if (Arrays.stream(selectedUsers.split(",")).anyMatch(h -> {
                    boolean isMatched = false;
                    isMatched = h.contains(handle);
                    return isMatched;
                  })) {
                    webSocket.writeTextMessage(messageUser.getName() + ": "
                        + computedMessage[0]);
                    // keep track of delivered messages
                    onlineUsers.add(handle);
                  }
                } else {
                  if (selectedUsers.isEmpty()
                      && !command[0].isEmpty()) {
                    ws.writeTextMessage("Private user not selected");
                  } else {
                    ws.writeTextMessage("ok");
                    if (ke != null) {
                      if (!selectedUsers.isEmpty()) {
                        ke.setValue("private", 1);
                      } else {
                        ke.setValue(1); // broadcast
                      }
                    }
                  }
                }
              }
            }
          }
          // calculate difference between selected and online users
          if (!selectedUsers.isEmpty()) {
            final List<String> selected = Arrays.asList(selectedUsers.split(","));
            final List<String> disconnectedUsers =
                selected.stream().filter(user -> !onlineUsers.contains(user))
                    .collect(Collectors.toList());
            // Save private message to send when to-user logs in
            if (!disconnectedUsers.isEmpty()) {
              try {

                dodexMongo.addMessage(messageUser, computedMessage[0],
                    disconnectedUsers);
                if (ke != null) {
                  ke.setValue("undelivered", disconnectedUsers.size());
                }

              } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                ws.writeTextMessage(
                    "Message delivery failure: " + e.getMessage());
              }
            }
          }
          return Uni.createFrom().item(0);
        }).onFailure().invoke(Throwable::printStackTrace).subscribeAsCompletionStage();
      }
    });
  }

  private void cleanUsers() {
    /*
     * Optional auto user cleanup - config in "application-conf.json". When client
     * changes handle when server is down, old users and undelivered messages will
     * be orphaned.
     *
     * Defaults: off - when turned on 1. execute on start up and every 7 days
     * thereafter. 2. remove users who have not logged in for 90 days.
     */
//    final Optional<Context> vertxContext = Optional.ofNullable(context); // Vertx.currentContext());
//    if (vertxContext.isPresent()) {
//      final Optional<JsonObject> jsonObject = Optional.ofNullable(context.config());
//      try {
//        final JsonObject config = jsonObject.orElseGet(JsonObject::new);
//        final Optional<Boolean> runClean = Optional.ofNullable(config.getBoolean("clean.run"));
//        if (runClean.isPresent() && runClean.get()) {
//          final CleanOrphanedUsersNeo4j clean = new CleanOrphanedUsersNeo4j();
//          clean.startClean(config);
//        }
//      } catch (final Exception exception) {
//        logger.error(LOGFORMAT, ColorUtilConstants.RED_BOLD_BRIGHT, "Context Configuration failed...",
//            exception.getMessage(), ColorUtilConstants.RESET);
//      }
//    }
  }

  public Vertx getVertx() {
    return vertx;
  }

  public void setVertx(Vertx vertx) {
    MongoRouter.vertx = vertx;
  }

  public static void removeWsChatSession(ServerWebSocket ws) {
    wsChatSessions.remove(ws.remoteAddress().toString());
  }
}
