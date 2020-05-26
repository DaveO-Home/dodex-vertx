package dmo.fs.db;

import java.sql.SQLException;
import java.util.List;

import org.davidmoten.rx.jdbc.Database;
import org.davidmoten.rx.jdbc.pool.NonBlockingConnectionPool;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;

public interface DodexDatabase {

	public String getAllUsers();

	public String getUserByName();

    public String getInsertUser();
    
	public String getRemoveUndelivered();

	public String getRemoveMessage();

    public String getUndeliveredMessage();
	
	public String getDeleteUser();

	public Future<MessageUser> addUser(ServerWebSocket ws, Database db, MessageUser messageUser) throws SQLException, InterruptedException;

	public Future<Long> deleteUser(ServerWebSocket ws, Database db, MessageUser messageUser) throws SQLException, InterruptedException;

	public Future<Long> addMessage(ServerWebSocket ws, MessageUser messageUser, String message, Database db) throws SQLException, InterruptedException;

	public Future<Void> addUndelivered(ServerWebSocket ws, List<String> undelivered, Long messageId, Database db) throws SQLException;

	public Future<Long> getUserIdByName(String name, Database db) throws InterruptedException, SQLException;

	public Future<Void> addUndelivered(Long userId, Long messageId, Database db) throws SQLException, InterruptedException;

	public int processUserMessages(ServerWebSocket ws, Database db, MessageUser messageUser);

	public Database getDatabase();

	public NonBlockingConnectionPool getPool();

	public MessageUser createMessageUser();

	public Future<MessageUser> selectUser(MessageUser messageUser, ServerWebSocket ws, Database db) throws InterruptedException, SQLException;

	public Future<StringBuilder> buildUsersJson(Database db, MessageUser messageUser) throws InterruptedException, SQLException;

	public void setVertx(Vertx vertx);

	public Vertx getVertx();
}