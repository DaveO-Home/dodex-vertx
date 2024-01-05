package dmo.fs.db.cassandra;

import java.sql.SQLException;
import java.util.List;

import dmo.fs.db.MessageUser;
import org.modellwerkstatt.javaxbus.EventBus;
import io.vertx.core.Future;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.ServerWebSocket;

public interface DodexCassandra {

	Future<mjson.Json> deleteUser(ServerWebSocket ws, EventBus eb, MessageUser messageUser)
			throws SQLException, InterruptedException;

	Future<mjson.Json> addMessage(ServerWebSocket ws, MessageUser messageUser, String message,
			List<String> undelivered, EventBus eb) throws SQLException, InterruptedException;

	Future<mjson.Json> deleteDelivered(ServerWebSocket ws, EventBus eb, MessageUser messageUser);

	Future<mjson.Json> processUserMessages(ServerWebSocket ws, EventBus eb, MessageUser messageUser)
			throws Exception;

	MessageUser createMessageUser();

	Future<MessageUser> selectUser(MessageUser messageUser, ServerWebSocket ws, EventBus eb)
			throws InterruptedException, SQLException;

	Future<mjson.Json> buildUsersJson(ServerWebSocket ws, EventBus eb, MessageUser messageUser)
			throws InterruptedException, SQLException;

	void setVertx(Vertx vertx);

	Vertx getVertx();
}
