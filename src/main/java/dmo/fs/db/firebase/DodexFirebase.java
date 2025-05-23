package dmo.fs.db.firebase;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import com.google.cloud.firestore.Firestore;
import dmo.fs.db.MessageUser;
import dmo.fs.utils.FirebaseMessage;
import dmo.fs.utils.FirebaseUser;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.rxjava3.core.http.ServerWebSocket;

public interface DodexFirebase {

	Future<MessageUser> deleteUser(ServerWebSocket ws, MessageUser messageUser)
			throws InterruptedException, ExecutionException;

	FirebaseUser updateUser(ServerWebSocket ws, FirebaseUser firebaseUser)
			throws InterruptedException, ExecutionException;

	Future<FirebaseMessage> addMessage(ServerWebSocket ws, MessageUser messageUser, String message,
			List<String> undelivered) throws InterruptedException, ExecutionException;

	Future<Map<String, Integer>> processUserMessages(ServerWebSocket ws, FirebaseUser firebaseUser)
			throws Exception;

	MessageUser createMessageUser();

	Future<FirebaseUser> selectUser(MessageUser messageUser, ServerWebSocket ws)
			throws InterruptedException, SQLException, ExecutionException;

	Future<StringBuilder> buildUsersJson(ServerWebSocket ws, MessageUser messageUser)
			throws InterruptedException;

	void setVertx(Vertx vertx);

	Vertx getVertx();

	void setFirestore(Firestore firestore);
}
