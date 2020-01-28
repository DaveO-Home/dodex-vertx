package dmo.fs.router;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.davidmoten.rx.jdbc.Database;

import dmo.fs.admin.CleanOrphanedUsers;
import dmo.fs.db.DbConfiguration;
import dmo.fs.db.DodexDatabase;
import dmo.fs.db.MessageUser;
import dmo.fs.utils.ConsoleColors;
import dmo.fs.utils.DodexUtil;
import dmo.fs.utils.ParseQuery;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;

public class DodexRouter {
    private final static Logger logger = LoggerFactory.getLogger(DodexRouter.class.getName());
    protected final Vertx vertx;
    private final Hashtable<String, ServerWebSocket> clients = new Hashtable<String, ServerWebSocket>();
    DodexDatabase dodexDatabase = null;

    public DodexRouter(final Vertx vertx) throws InterruptedException {
        this.vertx = vertx;
    }

    public void setWebSocket(final HttpServer server) throws InterruptedException, IOException, SQLException {
        /**
         * You can customize the db config here by: 
         *  Map = db configuration, 
         *  Properties = credentials 
         * e.g. Map overrideMap = new Map(); 
         *      Properties overrideProperties = new Properties(); 
         * set override or additional values... 
         * dodexDatabase = DbConfiguration.getDefaultDb(overrideMap, overrideProperties);
         */
        dodexDatabase = DbConfiguration.getDefaultDb();
        /**
         * Optional auto user cleanup - config in "application-conf.json". When client
         * changes handle when server is down, old users and undelivered messages will
         * be orphaned.
         * 
         * Defaults: off - when turned on 1. execute on start up and every 7 days
         * thereafter. 2. remove users who have not logged in for 90 days.
         */
        final Optional<Context> context = Optional.ofNullable(Vertx.currentContext());
        if(context.isPresent()) {
            final Optional<JsonObject> jsonObject = Optional.ofNullable(Vertx.currentContext().config());
            try {
                final JsonObject config = jsonObject.isPresent() ? jsonObject.get() : new JsonObject();
                final Optional<Boolean> runClean = Optional.ofNullable(config.getBoolean("clean.run"));
                if (runClean.isPresent() && runClean.get()) {
                    final CleanOrphanedUsers clean = new CleanOrphanedUsers();
                    clean.startClean(config);
                }
            } catch (final Exception exception) {
                logger.info("{0}Context Configuration failed...{1}{2} ",
                        new Object[] { ConsoleColors.RED_BOLD_BRIGHT, exception.getMessage(), ConsoleColors.RESET });
            }
        }

        final SharedData sd = vertx.sharedData();
        String startupMessage = "In Production";

        startupMessage = DodexUtil.getEnv().equals("dev") ? "In Development" : startupMessage;
        logger.info("{0}Starting Web Socket...{1}{2} ",
                new Object[] { ConsoleColors.BLUE_BOLD_BRIGHT, startupMessage, ConsoleColors.RESET });

        server.websocketHandler(ws -> {
            try {
                logger.info("{0}Websocket-connection...{2}{1} ",
                        new Object[] { ConsoleColors.BLUE_BOLD_BRIGHT,
                                URLDecoder.decode(ParseQuery.getQueryMap(ws.query()).get("handle"),
                                        StandardCharsets.UTF_8.name()),
                                ConsoleColors.RESET });
            } catch (final UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            MessageUser resultUser = dodexDatabase.createMessageUser();
            final DodexUtil dodexUtil = new DodexUtil();

            if (!ws.path().equals("/dodex")) {
                ws.reject();
            } else {
                final LocalMap<String, String> wsChatSessions = sd.getLocalMap("ws.dodex.sessions");
                final MessageUser messageUser = dodexDatabase.createMessageUser();
                final Database db = dodexDatabase.getDatabase();

                try {
                    wsChatSessions.put(ws.textHandlerID(), URLDecoder.decode(ws.uri(), StandardCharsets.UTF_8.name()));
                } catch (final UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                clients.put(ws.textHandlerID(), ws);

                ws.closeHandler(ch -> {
                    logger.info("{0}Closing ws-connection to client: {2}{1} ", new Object[] {
                            ConsoleColors.BLUE_BOLD_BRIGHT, messageUser.getName(), ConsoleColors.RESET });
                    wsChatSessions.remove(ws.textHandlerID());
                    clients.remove(ws.textHandlerID());
                });
                /*
                 * This is websocket onMessage.
                 */
                ws.handler(new Handler<Buffer>() {
                    @Override
                    public void handle(final Buffer data) {
                        String selectedUsers = null;
                        final ArrayList<String> onlineUsers = new ArrayList<>();
                        final String message = data.getString(0, data.length());
                        // Checking if message or command
                        final Map<String, String> returnObject = dodexUtil.commandMessage(ws, message, messageUser);
                        // message with command stripped out
                        String computedMessage = null;
                        String command = null;

                        computedMessage = returnObject.get("message");
                        command = returnObject.get("command");
                        if (command != null && command.equals(";removeuser")) {
                            try {
                                dodexDatabase.deleteUser(ws, db, messageUser);
                            } catch (InterruptedException | SQLException e) {
                                e.printStackTrace();
                                ws.writeTextMessage("Your Previous handle did not delete: " + e.getMessage());
                            }
                        }

                        if (computedMessage.length() > 0) {
                            // private users to send message
                            selectedUsers = returnObject.get("selectedUsers");
                            final Set<String> websockets = clients.keySet();
                            Map<String, String> query = null;

                            for (final String websocket : websockets) {
                                final ServerWebSocket webSocket = clients.get(websocket);
                                if (!webSocket.isClosed()) {
                                    if (!websocket.equals(ws.textHandlerID())) {
                                        // broadcast message
                                        query = ParseQuery.getQueryMap(wsChatSessions.get(webSocket.textHandlerID()));
                                        final String handle = query.get("handle");
                                        if (selectedUsers == null && command == null) {
                                            webSocket.writeTextMessage(messageUser.getName() + ": " + computedMessage);
                                            // private message
                                        } else if (Arrays.stream(selectedUsers.split(",")).anyMatch(h -> {
                                            boolean isMatched = false;
                                            if (!isMatched) {
                                                isMatched = h.contains(handle);
                                            }
                                            return isMatched;
                                        })) {
                                            webSocket.writeTextMessage(messageUser.getName() + ": " + computedMessage);
                                            // keep track of delivered messages
                                            onlineUsers.add(handle);
                                        }
                                    } else {
                                        if (selectedUsers == null && command != null) {
                                            ws.writeTextMessage("Private user not selected");
                                        } else
                                            ws.writeTextMessage("ok");
                                    }
                                }
                            }
                        }

                        // calculate difference between selected and online users
                        if (selectedUsers != null) {
                            final List<String> selected = Arrays.asList(selectedUsers.split(","));
                            final List<String> disconnectedUsers = selected.stream()
                                    .filter(user -> !onlineUsers.contains(user)).collect(Collectors.toList());
                            // Save private message to send when to-user logs in
                            if (disconnectedUsers.size() > 0) {
                                long key = 0;
                                try {
                                    key = dodexDatabase.addMessage(ws, messageUser, computedMessage, db);
                                } catch (InterruptedException | SQLException e) {
                                    e.printStackTrace();
                                }
                                try {
                                    dodexDatabase.addUndelivered(ws, disconnectedUsers, key, db);
                                } catch (final SQLException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                });
                /*
                 * websocket.onConnection()
                 */
                String handle = "";
                String id = "";
                Map<String, String> query = null;

                query = ParseQuery.getQueryMap(wsChatSessions.get(ws.textHandlerID()));

                handle = query.get("handle");
                id = query.get("id");

                StringBuilder userJson = new StringBuilder();
                messageUser.setName(handle);
                messageUser.setPassword(id);
                messageUser.setIp(ws.remoteAddress().toString());

                try {
                    resultUser = dodexDatabase.selectUser(messageUser, ws, db);
                    userJson = dodexDatabase.buildUsersJson(db, messageUser);
                } catch (InterruptedException | SQLException e) {
                    e.printStackTrace();
                }
                
                ws.writeTextMessage("connected:" + userJson); // Users for private messages
                /*
                 * Send undelivered messages and remove user related messages.
                 */
                dodexDatabase.processUserMessages(ws, db, resultUser);
            }
        });
    }
}
