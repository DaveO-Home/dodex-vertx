package dmo.fs.db;

import java.sql.SQLException;
import java.util.List;

import org.davidmoten.rx.jdbc.Database;
import org.davidmoten.rx.jdbc.pool.NonBlockingConnectionPool;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;

public interface DodexDatabase {

	String getAllUsers();

	String getUserByName();

    String getInsertUser();
    
	String getRemoveUndelivered();

	String getRemoveMessage();

    String getUndeliveredMessage();
	
	String getDeleteUser();

	Future<MessageUser> addUser(ServerWebSocket ws, Database db, MessageUser messageUser) throws SQLException, InterruptedException;

	Future<Long> deleteUser(ServerWebSocket ws, Database db, MessageUser messageUser) throws SQLException, InterruptedException;

	Future<Long> addMessage(ServerWebSocket ws, MessageUser messageUser, String message, Database db) throws SQLException, InterruptedException;

	Future<Void> addUndelivered(ServerWebSocket ws, List<String> undelivered, Long messageId, Database db) throws SQLException;

	Future<Long> getUserIdByName(String name, Database db) throws InterruptedException, SQLException;

	Future<Void> addUndelivered(Long userId, Long messageId, Database db) throws SQLException, InterruptedException;

	int processUserMessages(ServerWebSocket ws, Database db, MessageUser messageUser) throws Exception;

	Database getDatabase();

	NonBlockingConnectionPool getPool();

	MessageUser createMessageUser();

	Future<MessageUser> selectUser(MessageUser messageUser, ServerWebSocket ws, Database db) throws InterruptedException, SQLException;

	Future<StringBuilder> buildUsersJson(Database db, MessageUser messageUser) throws InterruptedException, SQLException;

	void setVertx(Vertx vertx);

	Vertx getVertx();
}