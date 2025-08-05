package dmo.fs.hib;

import dmo.fs.db.MessageUser;
import dmo.fs.hib.bld.DatabaseBuild;
import io.vertx.rxjava3.core.http.ServerWebSocket;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;


public interface DodexDatabase {

  DatabaseBuild getDatabaseBuild();

  void databaseSetup() throws InterruptedException;

  void configDatabase() throws InterruptedException;

  void entityManagerSetup();

  void entityManagerSetup(EntityManagerFactory emf);

  Long deleteUser(MessageUser messageUser)
      throws SQLException, InterruptedException;

  Long addMessage(MessageUser messageUser, String message)
      throws SQLException, InterruptedException;

  void addUndelivered(MessageUser messageUser, List<String> undelivered, Long messageId)
      throws SQLException, InterruptedException;

  Map<String, Integer> processUserMessages(ServerWebSocket ws, MessageUser messageUser)
      throws Exception;

  MessageUser createMessageUser();

  MessageUser selectUser(MessageUser messageUser)
      throws IOException;

  String buildUsersJson(MessageUser messageUser)
      throws Exception;

  EntityManager getEntityManager();

  SessionFactory getEmf();
}
