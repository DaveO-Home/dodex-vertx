package dmo.fs.db.cassandra;

import java.sql.SQLException;
import java.util.List;

import dmo.fs.db.MessageUser;
import io.vertx.core.Future;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.ServerWebSocket;

public interface DodexCassandra<T> {

	Future<mjson.Json> deleteUser(ServerWebSocket ws, T mqttServer, MessageUser messageUser)
			throws SQLException, InterruptedException;

	Future<mjson.Json> addMessage(ServerWebSocket ws, MessageUser messageUser, String message,
			List<String> undelivered, T mqttServer) throws SQLException, InterruptedException;

	Future<mjson.Json> deleteDelivered(ServerWebSocket ws, T mqttServer, MessageUser messageUser);

	Future<mjson.Json> processUserMessages(ServerWebSocket ws, T mqttServer, MessageUser messageUser)
			throws Exception;

	MessageUser createMessageUser();

	Future<MessageUser> selectUser(MessageUser messageUser, ServerWebSocket ws, T mqttServer)
			throws InterruptedException, SQLException;

	Future<mjson.Json> buildUsersJson(ServerWebSocket ws, T mqttServer, MessageUser messageUser)
			throws InterruptedException, SQLException;

	void setVertx(Vertx vertx);

	Vertx getVertx();
}
