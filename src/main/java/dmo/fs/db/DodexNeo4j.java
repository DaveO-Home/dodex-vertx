package dmo.fs.db;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import io.vertx.rxjava3.core.http.ServerWebSocket;

import org.neo4j.driver.Driver;

import io.vertx.mutiny.core.Promise;

public interface DodexNeo4j {

	Promise<MessageUser> deleteUser(MessageUser messageUser) throws InterruptedException, ExecutionException;

    MessageUser updateUser(MessageUser messageUser);

	Promise<MessageUser> addMessage(MessageUser messageUser, String message, List<String> undelivered) throws InterruptedException, ExecutionException;

	Promise<Map<String, Integer>> processUserMessages(ServerWebSocket ws, MessageUser messageUser) throws Exception;

	MessageUser createMessageUser();

	Promise<MessageUser> selectUser(MessageUser messageUser) throws InterruptedException, SQLException, ExecutionException;

	Promise<StringBuilder> buildUsersJson(MessageUser messageUser) throws InterruptedException;

    Promise<Driver> databaseSetup();

	void setDriver(Driver driver);

}
