package dmo.fs.router;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import dmo.fs.db.DbConfiguration;
import dmo.fs.mqtt.DodexMqttServer;

import io.vertx.rxjava3.core.http.ServerWebSocketHandshake;
import org.modellwerkstatt.javaxbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dmo.fs.admin.CleanOrphanedUsers;
import dmo.fs.db.cassandra.DodexCassandra;
import dmo.fs.db.MessageUser;
import dmo.fs.kafka.KafkaEmitterDodex;
import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import dmo.fs.utils.ParseQueryUtilHelper;
import dmo.fs.vertx.Server;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.Context;
import io.vertx.core.Promise;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpServer;
import io.vertx.rxjava3.core.http.ServerWebSocket;
import io.vertx.rxjava3.core.shareddata.LocalMap;
import io.vertx.rxjava3.core.shareddata.SharedData;

public class CassandraRouter {
  private static final Logger logger = LoggerFactory.getLogger(CassandraRouter.class.getName());
  protected Vertx vertx;
  private final Map<String, ServerWebSocket> clients = new ConcurrentHashMap<>();
  private static EventBus eb;
  private DodexMqttServer mqttServer;
  private static Object transport;
  private static final String LOGFORMAT = "{}{}{}";
  private KafkaEmitterDodex ke;

  public CassandraRouter(final Vertx vertx) {
    this.vertx = vertx;
    if (Server.getUseKafka()) {
      ke = new KafkaEmitterDodex();
    }
  }

  /**
   * You can customize the db config here by: Map = db configuration, Properties =
   * credentials e.g. Map overrideMap = new Map(); Properties overrideProperties =
   * new Properties(); set override or additional values... dodexDatabase =
   * DbConfiguration.getDefaultDb(overrideMap, overrideProperties);
   */
  public void setWebSocket(final HttpServer server) throws InterruptedException, IOException, SQLException {
    DodexCassandra<Object> dodexCassandra = DbConfiguration.getDefaultDb();
    dodexCassandra.setVertx(vertx);

    if (mqttServer != null) {
      transport = mqttServer;
    } else {
      transport = eb;
    }
    /*
     * Optional auto user cleanup - config in "application-conf.json". When client
     * changes handle when server is down, old users and undelivered messages will
     * be orphaned.
     * Defaults: off - when turned on 1. execute on start up and every 7 days
     * thereafter. 2. remove users who have not logged in for 90 days.
     */
    final Optional<Context> context = Optional.ofNullable(Vertx.currentContext());
    if (context.isPresent()) {
      final Optional<JsonObject> jsonObject = Optional.ofNullable(Server.getConfig());
      try {
        JsonObject config = jsonObject.orElseGet(JsonObject::new);
        final Optional<Boolean> runClean = Optional.ofNullable(config.getBoolean("clean.run"));
        if (runClean.isPresent() && runClean.get()) {
          final CleanOrphanedUsers clean = new CleanOrphanedUsers();
          clean.startClean(config);
        }
      } catch (final Exception exception) {
        logger.error(LOGFORMAT, ColorUtilConstants.RED_BOLD_BRIGHT, "Context Configuration failed...",
            ColorUtilConstants.RESET);
      }
    }

    final SharedData sd = vertx.sharedData();
    String startupMessage = "In Production";

    startupMessage = "dev".equals(DodexUtil.getEnv()) ? "In Development" : startupMessage;
    logger.info(LOGFORMAT, ColorUtilConstants.BLUE_BOLD_BRIGHT, startupMessage, ColorUtilConstants.RESET);

    server.webSocketHandler(ws -> {
      String handle = URLDecoder.decode(ParseQueryUtilHelper.getQueryMap(ws.query()).get("handle"),
          StandardCharsets.UTF_8);
      logger.info(LOGFORMAT, ColorUtilConstants.BLUE_BOLD_BRIGHT, handle, ColorUtilConstants.RESET);

      final DodexUtil dodexUtil = new DodexUtil();

      if (!"/dodex".equals(ws.path())) {
        return;
      } else {
        final LocalMap<String, String> wsChatSessions = sd.getLocalMap("ws.dodex.sessions");
        final MessageUser messageUser = dodexCassandra.createMessageUser();

        wsChatSessions.put(ws.remoteAddress().toString(),
            URLDecoder.decode(ws.uri(), StandardCharsets.UTF_8));

        clients.put(ws.remoteAddress().toString(), ws);
        if (ke != null) {
          ke.setValue("sessions", wsChatSessions.size());
        }
        ws.closeHandler(ch -> {
          if (logger.isInfoEnabled()) {
            logger.info(LOGFORMAT, ColorUtilConstants.BLUE_BOLD_BRIGHT,
                "Closing ws-connection to client: " + messageUser.getName(), ColorUtilConstants.RESET);
          }
          wsChatSessions.remove(ws.remoteAddress().toString());
          clients.remove(ws.remoteAddress().toString());
          if (ke != null) {
            ke.setValue("sessions", wsChatSessions.size());
          }
        });
        /*
         * This is websocket onMessage.
         */
        ws.handler(data -> {
          final ArrayList<String> onlineUsers = new ArrayList<>();
          final String message = data.getString(0, data.length());
          // Checking if message or command
          final Map<String, String> returnObject = dodexUtil.commandMessage(message);
          // message with command stripped out
          String[] computedMessage = {""};
          String[] command = {""};

          computedMessage[0] = returnObject.get("message");
          command[0] = returnObject.get("command");

          Promise<mjson.Json> promise = Promise.promise();
          promise.complete(null);
          Future<mjson.Json> deleted = null;

          if (!command[0].isEmpty() && command[0].equals(";removeuser")) {
            try {
              deleted = dodexCassandra.deleteUser(ws, transport, messageUser);
            } catch (InterruptedException | SQLException e) {
              ws.writeTextMessage("Your Previous handle did not delete: " + e.getMessage());
              throw new RuntimeException(e);
            }
          } else {
            deleted = promise.future();
          }

          if (deleted != null) {
            deleted.onSuccess(result -> {
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
                      query = ParseQueryUtilHelper
                          .getQueryMap(wsChatSessions.get(webSocket.remoteAddress().toString()));
                      final String handle1 = query.get("handle");

                      if (selectedUsers.isEmpty() && command[0].isEmpty()) {
                        webSocket.writeTextMessage(
                            messageUser.getName() + ": " + computedMessage[0]);
                        // private message
                      } else if (Arrays.stream(selectedUsers.split(",")).anyMatch(h -> {
                        boolean isMatched = false;
                        if (!isMatched) {
                          isMatched = h.contains(handle1);
                        }
                        return isMatched;
                      })) {
                        webSocket.writeTextMessage(
                            messageUser.getName() + ": " + computedMessage[0]);
                        // keep track of delivered messages
                        onlineUsers.add(handle1);
                      }
                    } else {
                      if (selectedUsers.isEmpty() && !command[0].isEmpty()) {
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
                final List<String> disconnectedUsers = selected.stream()
                    .filter(user -> !onlineUsers.contains(user)).collect(Collectors.toList());
                // Save private message to send when to-user logs in
                if (!disconnectedUsers.isEmpty()) {
                  Future<mjson.Json> future = null;
                  try {
                    future = dodexCassandra.addMessage(ws, messageUser, computedMessage[0],
                        disconnectedUsers, transport);
                    if (ke != null) {
                      ke.setValue("undelivered", disconnectedUsers.size());
                    }
                    future.onSuccess(key -> {
                      if (key != null) {
                        logger.info("Message processes: {}", key);
                      }
                    }).onFailure(Throwable::printStackTrace);
                  } catch (final SQLException | InterruptedException e) {
                    throw new RuntimeException(e);
                  }
                }
              }

            });
          }
        });
        /*
         * websocket.onConnection()
         */
        String handle2;
        String id;
        Map<String, String> query = null;
        query = ParseQueryUtilHelper.getQueryMap(wsChatSessions.get(ws.remoteAddress().toString()));

        handle2 = query.get("handle");
        id = query.get("id");

        messageUser.setName(handle2);
        messageUser.setPassword(id);
        messageUser.setIp(ws.remoteAddress().toString());

        try {
          Future<MessageUser> future = dodexCassandra.selectUser(messageUser, ws, transport);

          future.onSuccess(mUser -> {
            try {
              Future<mjson.Json> userJson = dodexCassandra.buildUsersJson(ws, transport, mUser);

              userJson.onSuccess(json -> {
                ws.writeTextMessage("connected:" + json.toString()); // Users for private messages
                /*
                 * Send undelivered messages and remove user related messages.
                 */
                try {
                  dodexCassandra.processUserMessages(ws, transport, mUser).onComplete(fut -> {
                    mjson.Json undeliveredArray = fut.result();
                    int size = undeliveredArray.asList().size();

                    for (int i = 0; i < size; i++) {
                      mjson.Json msg = undeliveredArray.at(i);

                      String when = new SimpleDateFormat("MM/dd-HH:ss", Locale.getDefault())
                          .format(new Date(msg.at("postdate").asLong()));
                      ws.writeTextMessage(msg.at("fromhandle").toString() + ":" + when + " "
                          + msg.at("message"));
                    }
                    if (size > 0) {
                      logger.info(String.format("%sMessages Delivered: %d to %s%s",
                          ColorUtilConstants.BLUE_BOLD_BRIGHT, size, mUser.getName(),
                          ColorUtilConstants.RESET));
                      if (ke != null) {
                        ke.setValue("delivered", size);
                      }
                    }
                    dodexCassandra.deleteDelivered(ws, transport, mUser).onComplete(result -> {
                      //
                    }).onFailure(Throwable::printStackTrace);
                  }).onFailure(Throwable::printStackTrace);
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              });
            } catch (InterruptedException | SQLException e) {
              throw new RuntimeException(e);
            }
          });

        } catch (InterruptedException | SQLException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  public static EventBus getEb() {
    return eb;
  }

  public void setMqttServer(DodexMqttServer mqttServer) {
    this.mqttServer = mqttServer;
  }

  public static void setEb(EventBus eb) {
    CassandraRouter.eb = eb;
  }
}
