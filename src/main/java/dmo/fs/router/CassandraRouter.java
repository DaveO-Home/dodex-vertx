package dmo.fs.router;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.modellwerkstatt.javaxbus.EventBus;

import dmo.fs.admin.CleanOrphanedUsers;
import dmo.fs.db.DbConfiguration;
import dmo.fs.db.DodexCassandra;
import dmo.fs.db.MessageUser;
import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import dmo.fs.utils.ParseQueryUtilHelper;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;

public class CassandraRouter {
    private final static Logger logger = LoggerFactory.getLogger(CassandraRouter.class.getName());
    protected final Vertx vertx;
    private Map<String, ServerWebSocket> clients = new ConcurrentHashMap<>();
    DodexCassandra dodexCassandra;
    EventBus eb;
    JsonObject config;

    public CassandraRouter(final Vertx vertx) throws InterruptedException {
        this.vertx = vertx;
    }

    public void setWebSocket(final HttpServer server) throws InterruptedException, IOException, SQLException {
        /**
         * You can customize the db config here by: Map = db configuration, Properties =
         * credentials e.g. Map overrideMap = new Map(); Properties overrideProperties =
         * new Properties(); set override or additional values... dodexDatabase =
         * DbConfiguration.getDefaultDb(overrideMap, overrideProperties);
         */

        dodexCassandra = DbConfiguration.getDefaultDb();
        dodexCassandra.setVertx(vertx);

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
                config = jsonObject.isPresent() ? jsonObject.get() : new JsonObject();
                final Optional<Boolean> runClean = Optional.ofNullable(config.getBoolean("clean.run"));
                if (runClean.isPresent() && runClean.get()) {
                    final CleanOrphanedUsers clean = new CleanOrphanedUsers();
                    clean.startClean(config);
                }
            } catch (final Exception exception) {
                logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT, "Context Configuration failed...",
                        ColorUtilConstants.RESET));
            }
        }

        final SharedData sd = vertx.sharedData();
        String startupMessage = "In Production";

        startupMessage = DodexUtil.getEnv().equals("dev") ? "In Development" : startupMessage;
        logger.info(String.join("", ColorUtilConstants.BLUE_BOLD_BRIGHT, startupMessage, ColorUtilConstants.RESET));

        Handler<ServerWebSocket> handler = new Handler<ServerWebSocket>() {

            @Override
            public void handle(ServerWebSocket ws) {

                try {
                    logger.info(String.join("", ColorUtilConstants.BLUE_BOLD_BRIGHT,
                            URLDecoder.decode(ParseQueryUtilHelper.getQueryMap(ws.query()).get("handle"),
                                    StandardCharsets.UTF_8.name()),
                            ColorUtilConstants.RESET));
                } catch (final UnsupportedEncodingException e) {
                    logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT, e.getMessage(),
                            ColorUtilConstants.RESET));
                }

                final DodexUtil dodexUtil = new DodexUtil();

                if (!ws.path().equals("/dodex")) {
                    ws.reject();
                } else {
                    final LocalMap<String, String> wsChatSessions = sd.getLocalMap("ws.dodex.sessions");
                    final MessageUser messageUser = dodexCassandra.createMessageUser();
                    // final Database db = dodexCassandra.getDatabase();
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
                            String[] computedMessage = { "" };
                            String[] command = { "" };

                            computedMessage[0] = returnObject.get("message");
                            command[0] = returnObject.get("command");

                            Promise<mjson.Json> promise = Promise.promise();
                            promise.complete(null);
                            Future<mjson.Json> deleted = null;

                            if (command[0].length() > 0 && command[0].equals(";removeuser")) {
                                try {
                                    deleted = dodexCassandra.deleteUser(ws, eb, messageUser);
                                } catch (InterruptedException | SQLException e) {
                                    e.printStackTrace();
                                    ws.writeTextMessage("Your Previous handle did not delete: " + e.getMessage());
                                }
                            } else {
                                deleted = promise.future();
                            }

                            deleted.onSuccess(result -> {
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
                                    if (disconnectedUsers.size() > 0) {
                                        Future<mjson.Json> future = null;
                                        try {
                                            future = dodexCassandra.addMessage(ws, messageUser, computedMessage[0],
                                                    disconnectedUsers, eb);
                                            future.onSuccess(key -> {
                                                System.out.println("Message processes:" + key);
                                            }).onFailure(exe -> {
                                                exe.printStackTrace();
                                            });
                                        } catch (final SQLException | InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }

                            });
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
                        setupEventBridge();

                        Future<MessageUser> future = dodexCassandra.selectUser(messageUser, ws, eb);
                        future.onSuccess(mUser -> {
                            try {
                                Future<mjson.Json> userJson = dodexCassandra.buildUsersJson(ws, eb, mUser);

                                userJson.onSuccess(json -> {
                                    ws.writeTextMessage("connected:" + json.toString()); // Users for private messages
                                    /*
                                     * Send undelivered messages and remove user related messages.
                                     */
                                    try {
                                        dodexCassandra.processUserMessages(ws, eb, mUser).onComplete(fut -> {
                                            mjson.Json undeliveredArray = fut.result();
                                            Integer size = undeliveredArray.asList().size();

                                            for (int i = 0; i < size; i++) {
                                                mjson.Json msg = undeliveredArray.at(i);

                                                String when = new SimpleDateFormat("MM/dd-HH:ss")
                                                        .format(new Date(msg.at("postdate").asLong()));
                                                ws.writeTextMessage(msg.at("fromhandle").toString() + ":" + when + " "
                                                        + msg.at("message"));
                                            }
                                            if (size > 0) {
                                                logger.info(String.format("%sMessages Delivered: %d to %s%s",
                                                        ColorUtilConstants.BLUE_BOLD_BRIGHT, size, mUser.getName(),
                                                        ColorUtilConstants.RESET));
                                            }
                                            dodexCassandra.deleteDelivered(ws, eb, mUser).onComplete(result -> {
                                                //
                                            }).onFailure(handler -> {
                                                handler.printStackTrace();
                                            });
                                        }).onFailure(handler -> {
                                            handler.printStackTrace();
                                        });
                                    } catch (Exception e) {
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

    void setupEventBridge() {
        if (eb == null) {
            Integer port = config.getInteger("bridge.port");
            eb = EventBus.create("localhost", port == null ? 7032 : port);
            logger.info(String.format("%sDodex Connected to Event Bus Bridge%s", ColorUtilConstants.BLUE_BOLD_BRIGHT,
                    ColorUtilConstants.RESET));
        }
    }
}
