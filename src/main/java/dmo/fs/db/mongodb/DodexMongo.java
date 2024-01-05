package dmo.fs.db.mongodb;

import dmo.fs.db.MessageUser;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Promise;
import io.vertx.rxjava3.core.http.ServerWebSocket;
import io.vertx.rxjava3.ext.mongo.MongoClient;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public interface DodexMongo {

  Promise<MessageUser> deleteUser(MessageUser messageUser) throws InterruptedException, ExecutionException;

  Promise<MessageUser> addMessage(MessageUser messageUser, String message, List<String> undelivered) throws InterruptedException, ExecutionException;

  Promise<Map<String, Integer>> processUserMessages(ServerWebSocket ws, MessageUser messageUser) throws Exception;

  MessageUser createMessageUser();

  Promise<MessageUser> selectUser(MessageUser messageUser) throws InterruptedException, SQLException, ExecutionException;

  Promise<StringBuilder> buildUsersJson(MessageUser messageUser) throws InterruptedException;

  Promise<MongoClient> databaseSetup();
}
