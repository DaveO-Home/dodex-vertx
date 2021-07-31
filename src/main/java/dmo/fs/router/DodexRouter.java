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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dmo.fs.admin.CleanOrphanedUsers;
import dmo.fs.db.DbConfiguration;
import dmo.fs.db.DodexDatabase;
import dmo.fs.db.MessageUser;
import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import dmo.fs.utils.ParseQueryUtilHelper;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Context;
import io.vertx.reactivex.core.Promise;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.http.ServerWebSocket;
import io.vertx.reactivex.core.shareddata.LocalMap;
import io.vertx.reactivex.core.shareddata.SharedData;

public class DodexRouter {
    private static final Logger logger = LoggerFactory.getLogger(DodexRouter.class.getName());
    protected Vertx vertx;
    private final Map<String, ServerWebSocket> clients = new ConcurrentHashMap<>();
    private DodexDatabase dodexDatabase;
    private static final String LOGFORMAT = "{}{}{}";

    public DodexRouter(final Vertx vertx) throws InterruptedException {
        this.vertx = vertx;
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

        final Handler<ServerWebSocket> handler = new Handler<ServerWebSocket>() {
            @Override
            public void handle(final ServerWebSocket ws) {

                try {
                    String handle = URLDecoder.decode(ParseQueryUtilHelper.getQueryMap(ws.query()).get("handle"), StandardCharsets.UTF_8.name());
                    logger.info(LOGFORMAT, ColorUtilConstants.BLUE_BOLD_BRIGHT, handle, ColorUtilConstants.RESET);
                } catch (final UnsupportedEncodingException e) {
                    logger.error(LOGFORMAT, ColorUtilConstants.RED_BOLD_BRIGHT, e.getMessage(), ColorUtilConstants.RESET);
                }

                final DodexUtil dodexUtil = new DodexUtil();

                if (!("/dodex").equals(ws.path())) {
                    ws.reject();
                } else {
                    final LocalMap<String, String> wsChatSessions = sd.getLocalMap("ws.dodex.sessions");
                    final MessageUser messageUser = dodexDatabase.createMessageUser();
                    // final PgPool pgPool = dodexDatabase.getPool4();
                    try {
                        wsChatSessions.put(ws.textHandlerID(),
                                URLDecoder.decode(ws.uri(), StandardCharsets.UTF_8.name()));
                    } catch (final UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    clients.put(ws.textHandlerID(), ws);

                    ws.closeHandler(ch -> {
                        logger.info(String.join("", ColorUtilConstants.BLUE_BOLD_BRIGHT,
                                "Closing ws-connection to client: ", messageUser.getName(), ColorUtilConstants.RESET));
                        wsChatSessions.remove(ws.textHandlerID());
                        clients.remove(ws.textHandlerID());
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
                            final String[] computedMessage = { "" };
                            final String[] command = { "" };

                            computedMessage[0] = returnObject.get("message");
                            command[0] = returnObject.get("command");

                            final Promise<Long> promise = Promise.promise();
                            promise.complete(-1l);
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

                            if(deleted != null) {
                                deleted.onSuccess(handler -> {
                                    String selectedUsers = "";
                                    if (computedMessage[0].length() > 0) {
                                        // private users to send message
                                        selectedUsers = returnObject.get("selectedUsers");
                                        final Set<String> websockets = clients.keySet();
                                        Map<String, String> query = null;

                                        for (final String websocket : websockets) {
                                            final ServerWebSocket webSocket = clients.get(websocket);
                                            if (!webSocket.isClosed()) {
                                                if (!websocket.equals(ws.textHandlerID())) {
                                                    // broadcast message
                                                    query = ParseQueryUtilHelper
                                                            .getQueryMap(wsChatSessions.get(webSocket.textHandlerID()));
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
                    String handle = "";
                    String id = "";
                    Map<String, String> query = null;

                    query = ParseQueryUtilHelper.getQueryMap(wsChatSessions.get(ws.textHandlerID()));

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
            }
        };

        server.webSocketHandler(handler);
    }
}
