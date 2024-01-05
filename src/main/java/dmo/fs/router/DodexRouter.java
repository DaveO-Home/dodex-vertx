package dmo.fs.router;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import dmo.fs.db.DbConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dmo.fs.admin.CleanOrphanedUsers;
import dmo.fs.db.DodexDatabase;
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
import io.vertx.rxjava3.core.Promise;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpServer;
import io.vertx.rxjava3.core.http.ServerWebSocket;
import io.vertx.rxjava3.core.shareddata.LocalMap;
import io.vertx.rxjava3.core.shareddata.SharedData;

public class DodexRouter {
    private static final Logger logger = LoggerFactory.getLogger(DodexRouter.class.getName());
    protected Vertx vertx;
    private final Map<String, ServerWebSocket> clients = new ConcurrentHashMap<>();
    private DodexDatabase dodexDatabase;
    private static final String LOGFORMAT = "{}{}{}";
    private KafkaEmitterDodex ke;

    public DodexRouter(final Vertx vertx) throws InterruptedException {
        this.vertx = vertx;
        if(Server.getUseKafka()) {
            ke = new KafkaEmitterDodex();
        }
    }

    public void setWebSocket(final HttpServer server) throws InterruptedException, IOException, SQLException {
        /**
         * You can customize the db config here by: Map = db configuration, Properties =
         * credentials e.g. Map overrideMap = new Map(); Properties overrideProperties =
         * new Properties(); set override or additional values... dodexDatabase =
         * DbConfiguration.getDefaultDb(overrideMap, overrideProperties);
         */
        
        dodexDatabase = DbConfiguration.getDefaultDb();
        if (dodexDatabase != null) {
            dodexDatabase.setVertx(vertx);
        }
        /**
         * Optional auto user cleanup - config in "application-conf.json". When client
         * changes handle when server is down, old users and undelivered messages will
         * be orphaned.
         * 
         * Defaults: off - when turned on 1. execute on start up and every 7 days
         * thereafter. 2. remove users who have not logged in for 90 days.
         */
        final Optional<Context> context = Optional.ofNullable(Vertx.currentContext());
        if (context.isPresent()) {
            final Optional<JsonObject> jsonObject = Optional.ofNullable(Vertx.currentContext().config());
            try {
                final JsonObject config = jsonObject.isPresent() ? jsonObject.get() : new JsonObject();
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

          String  handle = URLDecoder.decode(ParseQueryUtilHelper.getQueryMap(ws.query()).get("handle"), StandardCharsets.UTF_8);
          logger.info(LOGFORMAT, ColorUtilConstants.BLUE_BOLD_BRIGHT, handle, ColorUtilConstants.RESET);

          final DodexUtil dodexUtil = new DodexUtil();

            if (!("/dodex").equals(ws.path())) {
                ws.reject();
            } else {
                final LocalMap<String, String> wsChatSessions = sd.getLocalMap("ws.dodex.sessions");
                final MessageUser messageUser = dodexDatabase.createMessageUser();
                // final PgPool pgPool = dodexDatabase.getPool4();
              wsChatSessions.put(ws.remoteAddress().toString(),
                      URLDecoder.decode(ws.uri(), StandardCharsets.UTF_8));
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
                 * This is websocket onMessage.
                 */
                ws.handler(new Handler<Buffer>() {
                    @Override
                    public void handle(final Buffer data) {
                        final ArrayList<String> onlineUsers = new ArrayList<>();
                        final String message = data.getString(0, data.length());
                        // Checking if message or command
                        final Map<String, String> returnObject = dodexUtil.commandMessage(message);
                        // message with command stripped out
                        final String[] computedMessage = {""};
                        final String[] command = {""};

                        computedMessage[0] = returnObject.get("message");
                        command[0] = returnObject.get("command");

                        final Promise<Long> promise = Promise.promise();
                        promise.complete(-1L);
                        Future<Long> deleted = null;

                        if (command[0].length() > 0 && ";removeuser".equals(command[0])) {
                            try {
                                deleted = dodexDatabase.deleteUser(ws, messageUser);
                            } catch (InterruptedException | SQLException e) {
                                e.printStackTrace();
                                ws.writeTextMessage("Your Previous handle did not delete: " + e.getMessage());
                            }
                        } else {
                            deleted = promise.future();
                        }

                        if (deleted != null) {
                            deleted.onSuccess(handler1 -> {
                                String selectedUsers = "";
                                if (computedMessage[0].length() > 0) {
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
                                                final String handle = query.get("handle");
                                                if (selectedUsers.length() == 0 && command[0].length() == 0) {
                                                    webSocket.writeTextMessage(
                                                            messageUser.getName() + ": " + computedMessage[0]);
                                                    // private message
                                                } else if (Arrays.stream(selectedUsers.split(",")).anyMatch(h -> {
                                                    boolean isMatched = false;
                                                    if (!isMatched) {
                                                        isMatched = h.contains(handle);
                                                    }
                                                    return isMatched;
                                                })) {
                                                    webSocket.writeTextMessage(
                                                            messageUser.getName() + ": " + computedMessage[0]);
                                                    // keep track of delivered messages
                                                    onlineUsers.add(handle);
                                                }
                                            } else {
                                                if (selectedUsers.length() == 0 && command[0].length() > 0) {
                                                    ws.writeTextMessage("Private user not selected");
                                                } else {
                                                    ws.writeTextMessage("ok");
                                                    if (ke != null) {
                                                        if (selectedUsers.length() > 0) {
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
                                if (selectedUsers.length() > 0) {
                                    final List<String> selected = Arrays.asList(selectedUsers.split(","));
                                    final List<String> disconnectedUsers = selected.stream()
                                            .filter(user -> !onlineUsers.contains(user)).collect(Collectors.toList());
                                    // Save private message to send when to-user logs in
                                    if (!disconnectedUsers.isEmpty()) {
                                        Future<Long> future = null;
                                        try {
                                            future = dodexDatabase.addMessage(ws, messageUser, computedMessage[0]);
                                            if (ke != null) {
                                                ke.setValue("undelivered", disconnectedUsers.size());
                                            }
                                            future.onSuccess(key -> {
                                                try {
                                                    dodexDatabase.addUndelivered(ws, disconnectedUsers, key);
                                                } catch (final SQLException e) {
                                                    e.printStackTrace();
                                                }
                                            });
                                        } catch (final SQLException | InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            });
                        }
                    }
                });
                /*
                 * websocket.onConnection()
                 */
                
                handle = "";
                String id = "";
                Map<String, String> query = null;

                query = ParseQueryUtilHelper.getQueryMap(wsChatSessions.get(ws.remoteAddress().toString()));

                handle = query.get("handle");
                id = query.get("id");

                messageUser.setName(handle);
                messageUser.setPassword(id);
                messageUser.setIp(ws.remoteAddress().toString());

                try {
                    final Future<MessageUser> future = dodexDatabase.selectUser(messageUser, ws);

                    future.onSuccess(mUser -> {
                        try {
                            final Future<StringBuilder> userJson = dodexDatabase.buildUsersJson(mUser);
                            userJson.onSuccess(json -> {
                                ws.writeTextMessage("connected:" + json); // Users for private messages
                                if (ke != null) {
                                    ke.setValue("sessions", wsChatSessions.size());
                                }
                                /*
                                 * Send undelivered messages and remove user related messages.
                                 */
                                try {
                                    dodexDatabase.processUserMessages(ws, mUser).onComplete(fut -> {
                                        final int messageCount = fut.result().get("messages");
                                        if (messageCount > 0) {
                                            logger.info(String.format("%sMessages Delivered: %d to %s%s",
                                                    ColorUtilConstants.BLUE_BOLD_BRIGHT, messageCount,
                                                    mUser.getName(), ColorUtilConstants.RESET));
                                            if (ke != null) {
                                                ke.setValue("delivered", messageCount);
                                            }
                                        }
                                    });
                                } catch (final Exception e) {
                                    e.printStackTrace();
                                }
                            });
                        } catch (InterruptedException | SQLException e) {
                            e.printStackTrace();
                        }
                    });

                } catch (InterruptedException | SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
