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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dmo.fs.db.DbConfiguration;
import dmo.fs.db.DodexFirebase;
import dmo.fs.db.MessageUser;
import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import dmo.fs.utils.FirebaseMessage;
import dmo.fs.utils.FirebaseUser;
import dmo.fs.utils.ParseQueryUtilHelper;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.reactivex.core.Promise;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.http.ServerWebSocket;
import io.vertx.reactivex.core.shareddata.LocalMap;
import io.vertx.reactivex.core.shareddata.SharedData;

public class FirebaseRouter {
    private final static Logger logger = LoggerFactory.getLogger(FirebaseRouter.class.getName());
    protected Vertx vertx;
    private Map<String, ServerWebSocket> clients = new ConcurrentHashMap<>();
    private DodexFirebase dodexFirebase;
    private final static String DODEX_PROJECT_ID = "dodex-firebase";
    Firestore dbf;
    static FirestoreOptions firestoreOptions;
    static {
        try {
            firestoreOptions =
                FirestoreOptions.getDefaultInstance().toBuilder()
                    .setProjectId(DODEX_PROJECT_ID)
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .build();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public FirebaseRouter(final Vertx vertx) throws InterruptedException {
        this.vertx = vertx;
        dbf = firestoreOptions.getService();
        dbf.collection("users").get(); // dummy call to get firestore ready, helps on initial thread blockage
    }

    public void setWebSocket(final HttpServer server) throws InterruptedException, IOException, SQLException {
        /**
         * You can customize the db config here by: Map = db configuration, Properties =
         * credentials e.g. Map overrideMap = new Map(); Properties overrideProperties =
         * new Properties(); set override or additional values... dodexDatabase =
         * DbConfiguration.getDefaultDb(overrideMap, overrideProperties);
         */

        dodexFirebase = DbConfiguration.getDefaultDb();
        dodexFirebase.setVertx(vertx);
        dodexFirebase.setFirestore(dbf);

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
                    final MessageUser messageUser = dodexFirebase.createMessageUser();
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

                            Promise<MessageUser> promise = Promise.promise();
                            promise.complete(null);
                            Future<MessageUser> continued = null;

                            if (";removeuser".equals(command[0])) {
                                try {
                                    continued = dodexFirebase.deleteUser(ws, messageUser);
                                } catch (InterruptedException | ExecutionException e) {
                                    e.printStackTrace();
                                    ws.writeTextMessage("Your Previous handle did not delete: " + e.getMessage());
                                }
                            } else {
                                continued = promise.future();
                            }

                            continued.onSuccess(result -> {
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
                                        Future<FirebaseMessage> future = null;
                                        try {
                                        
                                            future = dodexFirebase.addMessage(ws, messageUser, computedMessage[0], disconnectedUsers);
                                        
                                        } catch (ExecutionException | InterruptedException e) {
                                            e.printStackTrace();
                                            ws.writeTextMessage("Message delivery failure: " + e.getMessage());
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
                        Future<FirebaseUser> future = dodexFirebase.selectUser(messageUser, ws);
                        future.onSuccess(firebaseUser -> {
                            try {
                                Future<StringBuilder> userJson = dodexFirebase.buildUsersJson(ws, messageUser);

                                userJson.onSuccess(json -> {
                                    ws.writeTextMessage("connected:" + json); // Users for private messages
                                    /*
                                     * Send undelivered messages and remove user related messages.
                                     */
                                    try {
                                        dodexFirebase.processUserMessages(ws, firebaseUser).onComplete(counts -> {
                                            final int messageCount = counts.result().get("messages");
                                            if (messageCount > 0) {
                                                logger.info(String.format("%sMessages Delivered: %d to %s%s",
                                                        ColorUtilConstants.BLUE_BOLD_BRIGHT, messageCount,
                                                        firebaseUser.getName(), ColorUtilConstants.RESET));
                                            }
                                        });
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                });
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        });

                    } catch (InterruptedException | ExecutionException | SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        server.webSocketHandler(handler);
    }
}