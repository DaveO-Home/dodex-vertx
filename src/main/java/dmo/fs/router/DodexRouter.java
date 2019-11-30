package dmo.fs.router;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.davidmoten.rx.jdbc.Database;

import dmo.fs.db.DbConfiguration;
import dmo.fs.db.DodexDatabase;
import dmo.fs.db.MessageUser;
import dmo.fs.utils.ConsoleColors;
import dmo.fs.utils.DodexUtil;
import dmo.fs.utils.ParseQuery;
import io.vertx.core.http.*;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;

public class DodexRouter {
    private final static Logger logger = LoggerFactory.getLogger(DodexRouter.class.getName());
    protected final Vertx vertx;
    private final Hashtable<String, ServerWebSocket> clients = new Hashtable<String, ServerWebSocket>();
    DodexDatabase dodexDatabase = null;

    public DodexRouter(Vertx vertx) throws InterruptedException {
        this.vertx = vertx;
    }

    public void setWebSocket(HttpServer server) throws InterruptedException, IOException {
        dodexDatabase = DbConfiguration.getDefaultDb();
        
        /*
            You can customize the db config here by: Map = db config, Properties = credentials
            Map overrideMap = new Map();
            Properties overrideProperties = new Properties();
            // set override or additional values...
            dodexDatabase = DbConfiguration.getDefaultDb(overrideMap, overrideProperties);
        */

        SharedData sd = vertx.sharedData();
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
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            MessageUser resultUser = dodexDatabase.createMessageUser();
            DodexUtil dodexUtil = new DodexUtil();

            if (!ws.path().equals("/dodex")) {
                ws.reject();
            } else {
                LocalMap<String, String> wsChatSessions = sd.getLocalMap("ws.dodex.sessions");
                MessageUser messageUser = dodexDatabase.createMessageUser();
                Database db = dodexDatabase.getDatabase();

                try {
                    wsChatSessions.put(ws.textHandlerID(), URLDecoder.decode(ws.uri(), StandardCharsets.UTF_8.name()));
                } catch (UnsupportedEncodingException e) {
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
                        ArrayList<String> onlineUsers = new ArrayList<>();
                        String message = data.getString(0, data.length());
                        // Checking if message or command
                        Map<String, String> returnObject = dodexUtil.commandMessage(ws, message, messageUser);
                        // message with command stripped out
                        String computedMessage = null;
                        String command = null;

                        computedMessage = returnObject.get("message");
                        command = returnObject.get("command");
                        if (command != null && command.equals(";removeuser")) {
                            try {
                                dodexDatabase.deleteUser(ws, db, messageUser);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                ws.writeTextMessage("Your Previous handle did not delete: " + e.getMessage());
                            }
                        }

                        if (computedMessage.length() > 0) {
                            // private users to send message
                            selectedUsers = returnObject.get("selectedUsers");
                            Set<String> websockets = clients.keySet();
                            Map<String, String> query = null;

                            for (String websocket : websockets) {
                                ServerWebSocket webSocket = clients.get(websocket);
                                if (!webSocket.isClosed()) {
                                    if (!websocket.equals(ws.textHandlerID())) {
                                        // broadcast message
                                        query = ParseQuery.getQueryMap(wsChatSessions.get(webSocket.textHandlerID()));
                                        String handle = query.get("handle");
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
                            List<String> selected = Arrays.asList(selectedUsers.split(","));
                            List<String> disconnectedUsers = selected.stream().filter(user -> 
                                    !onlineUsers.contains(user)).collect(Collectors.toList());
                            // Save private message to send when to-user logs in
                            if (disconnectedUsers.size() > 0) {
                                long key = 0;
                                try {
                                    key = dodexDatabase.addMessage(ws, messageUser, computedMessage);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                dodexDatabase.addUndelivered(ws, disconnectedUsers, key);
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

                resultUser = dodexDatabase.selectUser(messageUser, ws);
                try {
                    userJson = dodexDatabase.buildUsersJson(messageUser);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                ws.writeTextMessage("connected:" + userJson); // Users for private messages
                /*
                 * Send undelivered messages and remove user related messages.
                 */
                dodexDatabase.processUserMessages(ws, resultUser);
            }
        });
    }
}
