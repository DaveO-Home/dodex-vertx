
package dmo.fs.db;

import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import dmo.fs.utils.FirebaseMessage;
import dmo.fs.utils.FirebaseUser;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.ServerWebSocket;

public abstract class DbFirebaseBase {
    private static final Logger logger = LoggerFactory.getLogger(DbFirebaseBase.class.getName());

    private Vertx vertx;
    private Firestore dbf;

    public Timestamp addUser(ServerWebSocket ws, FirebaseUser firebaseUser)
            throws InterruptedException, ExecutionException, SQLException {
        DocumentReference userDoc = dbf.collection("users").document(firebaseUser.getName());
        ApiFuture<WriteResult> apiFuture = userDoc.set(firebaseUser);

        WriteResult writeResult = null;
        writeResult = apiFuture.get();

        return writeResult == null ? null : writeResult.getUpdateTime();
    }

    public FirebaseUser updateUser(ServerWebSocket ws, FirebaseUser firebaseUser)
            throws InterruptedException, ExecutionException {
        Map<String, Object> lastLogin = new ConcurrentHashMap<>();
        DocumentReference userDoc = dbf.collection("users").document(firebaseUser.getName());

        lastLogin.put("lastLogin", Timestamp.now());
        lastLogin.put("ip", firebaseUser.getIp());
        ApiFuture<WriteResult> apiFuture =
                userDoc.update("lastLogin", Timestamp.now(), "ip", firebaseUser.getIp());

        WriteResult writeResult = null;
        writeResult = apiFuture.get();

        firebaseUser.setLastLogin(writeResult == null ? null : writeResult.getUpdateTime());
        return firebaseUser;
    }

    public Future<MessageUser> deleteUser(ServerWebSocket ws, MessageUser messageUser)
            throws InterruptedException, ExecutionException {
        Promise<MessageUser> promise = Promise.promise();

        DocumentReference userDoc = dbf.collection("users").document(messageUser.getName());
        ApiFuture<WriteResult> writeResult = userDoc.delete();

        if (writeResult.isDone()) {
            messageUser.setLastLogin(writeResult.get().getUpdateTime().toDate());
            promise.complete(messageUser);
        }

        return promise.future();
    }

    public Future<FirebaseMessage> addMessage(ServerWebSocket ws, MessageUser messageUser,
            String message, List<String> undelivered)
            throws InterruptedException, ExecutionException {
        Promise<FirebaseMessage> promise = Promise.promise();

        undelivered.forEach(user -> {
            FirebaseMessage firebaseMessage = setMessageFromUser(messageUser, message);
            firebaseMessage.setName(user);
            DocumentReference userDoc = dbf.collection("users").document(user);
            try {
                String id = (String) userDoc.get().get().get("id");
                firebaseMessage.setUser_id(id);
                CollectionReference messageDoc = dbf.collection("messages")
                        .document(firebaseMessage.getName()).collection("documents");
                ApiFuture<DocumentReference> apiFuture = messageDoc.add(firebaseMessage);
                if (apiFuture.isDone()) {
                    promise.complete(firebaseMessage);
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });

        return promise.future();
    }

    public abstract MessageUser createMessageUser();

    public Future<FirebaseUser> selectUser(MessageUser messageUser, ServerWebSocket ws)
            throws InterruptedException, SQLException, ExecutionException {
        Promise<FirebaseUser> promise = Promise.promise();

        FirebaseUser firebaseUser = new FirebaseUser();
        firebaseUser.setName(messageUser.getName());
        firebaseUser.setPassword(messageUser.getPassword());
        firebaseUser.setIp(messageUser.getIp());
        firebaseUser.setId(UUID.randomUUID().toString());
        firebaseUser.setLastLogin(Timestamp.now());

        DocumentReference documentReference =
                dbf.collection("users").document(messageUser.getName());

        ApiFuture<DocumentSnapshot> apiFuture = documentReference.get();
        Map<String, Object> user = apiFuture.get().getData();

        if (user == null || user.isEmpty()) {
            addUser(ws, firebaseUser);
            promise.complete(firebaseUser);
        } else {
            firebaseUser = setUserFromMap(user);
            firebaseUser.setLastLogin(Timestamp.now());
            updateUser(ws, firebaseUser);
            promise.complete(firebaseUser);
        }

        return promise.future();
    }

    private FirebaseUser setUserFromMap(Map<String, Object> user) {
        return new FirebaseUser((String) user.get("id"), (String) user.get("name"),
                (String) user.get("password"), (String) user.get("ip"),
                (Timestamp) user.get("lastLogin"));
    }

    private FirebaseMessage setMessageFromUser(MessageUser user, String message) {
        return new FirebaseMessage(null, null, UUID.randomUUID().toString(), null, message,
                user.getName(), Timestamp.now());
    }

    public Future<StringBuilder> buildUsersJson(ServerWebSocket ws, MessageUser messageUser)
            throws InterruptedException {
        Promise<StringBuilder> promise = Promise.promise();
        JsonArray ja = new JsonArray();

        CollectionReference usersQuery = dbf.collection("users");
        ApiFuture<QuerySnapshot> apiFuture = usersQuery.get();
        try {
            apiFuture.get().forEach(user -> {
                String name = user.getString("name");
                if (!name.equals(messageUser.getName())) {
                    ja.add(new JsonObject().put("name", name));
                }
            });
        } catch (ExecutionException e) {
            logger.error(String.format("%sError build user json: %s%s", ColorUtilConstants.RED,
                    e.getMessage(), ColorUtilConstants.RESET));
        }
        promise.complete(new StringBuilder(ja.toString()));

        return promise.future();
    }



    public Future<Map<String, Integer>> processUserMessages(ServerWebSocket ws,
            FirebaseUser firebaseUser) throws Exception {
        Promise<Map<String, Integer>> promise = Promise.promise();
        Map<String, Integer> counts = new ConcurrentHashMap<>();

        CollectionReference messageQuery =
                dbf.collection(String.format("messages/%s/documents", firebaseUser.getName()));
        ApiFuture<QuerySnapshot> documents = messageQuery.get();

        counts.put("messages", 0);
        if (DodexUtil.isNull(documents)) {
            promise.complete(counts);
        } else {
            documents.get().forEach(message -> {
                Date postDate = message.getTimestamp("post_date").toDate();
                DateFormat formatDate =
                        DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault());
                String handle = message.getString("name");

                ws.writeTextMessage(
                        handle + formatDate.format(postDate) + " " + message.getString("message"));

                messageQuery.document(message.getId()).delete();

                Integer count = counts.get("messages") + 1;
                counts.put("messages", count);
                promise.tryComplete(counts);
            });
        }

        return promise.future();
    }

    public Vertx getVertx() {
        return vertx;
    }

    public void setVertx(Vertx vertx) {
        this.vertx = vertx;
    }

    public void setFirestore(Firestore dbf) {
        this.dbf = dbf;
    }
}
