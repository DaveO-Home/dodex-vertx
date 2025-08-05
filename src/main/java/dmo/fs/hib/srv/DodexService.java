
package dmo.fs.hib.srv;

import dmo.fs.db.MessageUser;
import dmo.fs.entities.Messages;
import dmo.fs.entities.Undelivered;
import dmo.fs.entities.Users;
import dmo.fs.entities.Users_;
import io.vertx.core.json.JsonArray;
import io.vertx.rxjava3.core.http.ServerWebSocket;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DodexService {
  private final static Logger logger = LoggerFactory.getLogger(DodexService.class.getName());

  private static EntityManager entityManager;

  public static void setupSql(EntityManager entityManager) {
    DodexService.entityManager = entityManager;
  }

  protected static List<Map<String, String>> getAllUsers(MessageUser messageUser, EntityManager entityManager) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Users> userQuery = builder.createQuery(Users.class);
    Root<Users> root = userQuery.from(Users.class);

    ParameterExpression<String> value = builder.parameter(String.class, Users_.NAME);
    userQuery.select(root).where(builder.notEqual(root.<String>get(Users_.NAME), value));

    TypedQuery<Users> query = entityManager.createQuery(userQuery);
    query.setParameter(Users_.NAME, messageUser.getName());
    List<Users> results = query.getResultList();
    List<Map<String, String>> userList = new ArrayList<>();

    results.forEach(user -> {
      Map<String, String> userMap = new HashMap<>();
      userMap.put(Users_.NAME, user.getName());
      userList.add(userMap);
    });
    return userList;
  }

  public static Users getUserByName(String name, EntityManager entityManager) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Users> userQuery = builder.createQuery(Users.class);
    Root<Users> root = userQuery.from(Users.class);

    userQuery.select(root).where(builder.equal(root.get(Users_.NAME), name));
    TypedQuery<Users> query = entityManager.createQuery(userQuery);

    return query.getSingleResult();
  }

  public Users getUserById(MessageUser messageUser, EntityManager entityManager) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Users> userQuery = builder.createQuery(Users.class);
    Root<Users> root = userQuery.from(Users.class);

    Predicate equalName = builder.equal(root.get(Users_.NAME), messageUser.getName());
    Predicate equalPassword = builder.equal(root.get(Users_.PASSWORD), messageUser.getPassword());
    userQuery.select(root).where(builder.and(equalName, equalPassword));

    TypedQuery<Users> query = entityManager.createQuery(userQuery);

    return query.getSingleResult();
  }

  public List<Messages> getUserUndelivered(MessageUser messageUser, EntityManager entityManager) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Messages> criteriaQuery = builder.createQuery(Messages.class);

    Root<Users> userRoot = criteriaQuery.from(Users.class);
    Join<Users, Messages> users = userRoot.join("messages");
    criteriaQuery.where(builder.equal(userRoot.get("id"), messageUser.getId()));

    return entityManager
        .createQuery(criteriaQuery.select(users))
        .getResultList();
  }

  public Long addMessage(MessageUser messageUser, String mess) {
    EntityManager em = entityManager.getEntityManagerFactory().createEntityManager();

    Messages message = new Messages();

    message.setPostDate(LocalDateTime.now());
    message.setMessage(mess);
    message.setFromHandle(messageUser.getName());

    em.getTransaction().begin();
    em.persist(message);
    messageUser.setEntityManager(em);

    return message.getId();
  }

  private void persistUndelivered(Long userId, Long messageId, EntityManager entityManager) {
    Undelivered undelivered = new Undelivered();
    Undelivered.UndeliveredId undeliveredId = new Undelivered.UndeliveredId(userId, messageId);
    undelivered.setUndeliveredId(undeliveredId);

    entityManager.persist(undelivered);
  }

  public void addUndelivered(MessageUser messageUser, List<String> destination, Long messageId) {
    EntityManager em = messageUser.getEntityManager();

    for (String name : destination) {
      Users user = getUserByName(name, em);
      persistUndelivered(user.getId(), messageId, em);
    }
    em.getTransaction().commit();
    em.clear(); // detach all entities
    em.close();
  }

  @Transactional
  public MessageUser selectUser(MessageUser messageUser)
      throws IOException {
    EntityManager em = entityManager.getEntityManagerFactory().createEntityManager(); // get a connection
    messageUser.setEntityManager(em);

    try {
      Users user = getUserById(messageUser, em);
      user.setLastLogin(LocalDateTime.now());

      em.getTransaction().begin();
      em.merge(user);
      em.getTransaction().commit();

      messageUser.setLastLogin(user.getLastLogin());
      messageUser.setId(user.getId());
    } catch (NoResultException nre) { // add user on not found exception
      messageUser.setLastLogin(LocalDateTime.now());
      if (messageUser.getIp() == null) {
        messageUser.setIp("Unknown");
      }

      Users user = new Users();
      user.setLastLogin(messageUser.getLastLogin().toLocalDateTime());
      user.setName(messageUser.getName());
      user.setPassword(messageUser.getPassword());
      user.setIp(messageUser.getIp());
      if (messageUser.getIp() == null) {
        user.setIp("Unknown");
      }
      em.getTransaction().begin();
      em.persist(user);
      em.getTransaction().commit();
      messageUser.setId(user.getId());
      messageUser.setLastLogin(user.getLastLogin());
    }
    return messageUser;
  }

  //
  public String buildUsersJson(MessageUser messageUser) throws Exception {
    EntityManager em = messageUser.getEntityManager();

    List<Map<String, String>> userList = getAllUsers(messageUser, em);
    JsonArray jsonArray = JsonArray.of(userList);

    return jsonArray.toString().replace("[[", "[").replace("]]", "]");
  }

  private void removeMessage(Messages message, EntityManager em) {
    Messages currentMessage = em.find(Messages.class, message.getId());

    if (currentMessage.getUndelivered().isEmpty()) {
      em.remove(currentMessage);
    }
  }

  protected void removeUndelivered(Long userId, Long messageId, EntityManager entityManager) {
    Undelivered undelivered = entityManager.find(Undelivered.class, new Undelivered.UndeliveredId(userId, messageId));
    entityManager.remove(undelivered);
  }

  public Map<String, Integer> processUserMessages(ServerWebSocket ws, MessageUser messageUser) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd@HH");
    Map<String, Integer> counts = new ConcurrentHashMap<>();

    EntityManager em = messageUser.getEntityManager();

    List<Messages> messageList = getUserUndelivered(messageUser, em);
    em.getTransaction().begin();
    for (Messages message : messageList) {
      ws.writeTextMessage(
          message.getFromHandle() + message.getPostDate().format(formatter) + " " + message.getMessage());
      removeUndelivered(messageUser.getId(), message.getId(), em);
    }

    em.flush();
    em.clear();

    for (Messages message : messageList) {
      removeMessage(message, em);
    }
    em.getTransaction().commit();
    em.close();
    counts.put("messages", messageList.size());

    return counts;
  }

  public Long deleteUser(MessageUser messageUser) throws SQLException, InterruptedException {
    EntityManager em = entityManager.getEntityManagerFactory().createEntityManager();
    Users user = getUserById(messageUser, em);
    em.getTransaction().begin();
    em.remove(user);
    em.getTransaction().commit();
    em.close();
    return user.getId();
  }
}
