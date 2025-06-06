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

import dmo.fs.db.DbConfiguration;
import io.vertx.rxjava3.core.http.ServerWebSocketHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import dmo.fs.db.firebase.DodexFirebase;
import dmo.fs.db.MessageUser;
import dmo.fs.kafka.KafkaEmitterDodex;
import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import dmo.fs.utils.FirebaseUser;
import dmo.fs.utils.ParseQueryUtilHelper;
import dmo.fs.vertx.Server;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpServer;
import io.vertx.rxjava3.core.http.ServerWebSocket;
import io.vertx.rxjava3.core.shareddata.LocalMap;
import io.vertx.rxjava3.core.shareddata.SharedData;

public class FirebaseRouter {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseRouter.class.getName());
    protected Vertx vertx;
    private final Map<String, ServerWebSocket> clients = new ConcurrentHashMap<>();
    private DodexFirebase dodexFirebase;
    private static final String DODEX_PROJECT_ID = "dodex-firebase";
    Firestore dbf;
    static FirestoreOptions firestoreOptions;
    private static final String LOGFORMAT = "{}{}{}";
    private KafkaEmitterDodex ke;
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
        vertx.exceptionHandler(e -> {
            e.printStackTrace();
        });

        dbf = firestoreOptions.getService();
        dbf.collection("users").get(); // dummy call to get firestore ready, helps on initial thread blockage

        if(Server.getUseKafka()) {
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
        dodexFirebase = DbConfiguration.getDefaultDb();
        dodexFirebase.setVertx(DodexUtil.getVertx().getDelegate());
        dodexFirebase.setFirestore(dbf);

        final SharedData sd = vertx.sharedData();
        String startupMessage = "In Production";

        startupMessage = "dev".equals(DodexUtil.getEnv()) ? "In Development" : startupMessage;
        logger.info(LOGFORMAT, ColorUtilConstants.BLUE_BOLD_BRIGHT, startupMessage, ColorUtilConstants.RESET);

        Handler<ServerWebSocket> handler = new Handler<ServerWebSocket>() {

            @Override
            public void handle(ServerWebSocket ws) {

              String handle = URLDecoder.decode(ParseQueryUtilHelper.getQueryMap(ws.query())
                  .get("handle"), StandardCharsets.UTF_8);
              logger.info(LOGFORMAT, ColorUtilConstants.BLUE_BOLD_BRIGHT, handle, ColorUtilConstants.RESET);

              final DodexUtil dodexUtil = new DodexUtil();

                if (!"/dodex".equals(ws.path())) {
                    server.webSocketHandshakeHandler(ServerWebSocketHandshake::reject);
                } else {
                    server.webSocketHandshakeHandler(ServerWebSocketHandshake::accept);
                    final LocalMap<String, String> wsChatSessions = sd.getLocalMap("ws.dodex.sessions");
                    final MessageUser messageUser = dodexFirebase.createMessageUser();
                  wsChatSessions.put(ws.remoteAddress().toString(),
                          URLDecoder.decode(ws.uri(), StandardCharsets.UTF_8));
                  clients.put(ws.remoteAddress().toString(), ws);
                    if(ke != null) {
                        ke.setValue("sessions", wsChatSessions.size());
                    }
                    ws.closeHandler(ch -> {
                        if (logger.isInfoEnabled()) {
                            logger.info(String.join("", ColorUtilConstants.BLUE_BOLD_BRIGHT,
                                    "Closing ws-connection to client: ", messageUser.getName(), ColorUtilConstants.RESET));
                        }
                        wsChatSessions.remove(ws.remoteAddress().toString());
                        clients.remove(ws.remoteAddress().toString());
                        if(ke != null) {
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
                            if(continued != null) {
                                continued.onSuccess(result -> {
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
                                                        .getQueryMap(URLDecoder.decode(webSocket.query(), StandardCharsets.UTF_8));
                                                        //.getQueryMap(wsChatSessions.get(webSocket.textHandlerID()));
                                                    final String handle = query.get("handle");
                                                    if (selectedUsers.isEmpty() && command[0].isEmpty()) {
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
                                                    if (selectedUsers.isEmpty() && !command[0].isEmpty()) {
                                                        ws.writeTextMessage("Private user not selected");
                                                    } else {
                                                        ws.writeTextMessage("ok");
                                                        if(ke != null) {
                                                            if(!selectedUsers.isEmpty()) {
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
                                            try {
                                            
                                                dodexFirebase.addMessage(ws, messageUser, computedMessage[0], disconnectedUsers);
                                                if (ke != null) {
                                                    ke.setValue("undelivered", disconnectedUsers.size());
                                                }
                                                
                                            } catch (ExecutionException | InterruptedException e) {
                                                e.printStackTrace();
                                                ws.writeTextMessage("Message delivery failure: " + e.getMessage());
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
                    String userHandle;
                    String id;
                    Map<String, String> query;

                    query = ParseQueryUtilHelper.getQueryMap(wsChatSessions.get(ws.remoteAddress().toString()));

                    userHandle = query.get("handle");
                    id = query.get("id");

                    messageUser.setName(userHandle);
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
                                                if (ke != null) {
                                                    ke.setValue("delivered", messageCount);
                                                }
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

    public Firestore getDbf() {
        return dbf;
    }
}
