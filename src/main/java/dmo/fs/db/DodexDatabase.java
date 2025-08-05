package dmo.fs.db;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import io.vertx.core.Future;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.ServerWebSocket;

public interface DodexDatabase {

	String getAllUsers();

	String getUserByName();

	String getUserById();

	String getRemoveMessage();

	String getDeleteUser();

	Future<MessageUser> addUser(ServerWebSocket ws, MessageUser messageUser)
			throws SQLException, InterruptedException;

	Future<Long> deleteUser(ServerWebSocket ws, MessageUser messageUser)
			throws SQLException, InterruptedException;

	Future<Long> addMessage(ServerWebSocket ws, MessageUser messageUser, String message)
			throws SQLException, InterruptedException;

	void addUndelivered(ServerWebSocket ws, List<String> undelivered, Long messageId)
			throws SQLException;

	Future<Map<String, Integer>> processUserMessages(ServerWebSocket ws, MessageUser messageUser)
			throws Exception;

	<T> T getPool();

	MessageUser createMessageUser();

	Future<MessageUser> selectUser(MessageUser messageUser, ServerWebSocket ws)
			throws InterruptedException, SQLException;

	Future<StringBuilder> buildUsersJson(MessageUser messageUser)
			throws InterruptedException, SQLException;

	void setVertx(Vertx vertx);

	Vertx getVertx();
}
