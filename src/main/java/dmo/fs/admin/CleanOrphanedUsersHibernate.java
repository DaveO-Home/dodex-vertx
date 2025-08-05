package dmo.fs.admin;

import dmo.fs.db.MessageUser;
import dmo.fs.dbh.emf.DodexEntityManager;
import dmo.fs.entities.Messages;
import dmo.fs.entities.Users;
import dmo.fs.entities.Users_;
import dmo.fs.hib.DodexDatabaseBase;
import dmo.fs.utils.ColorUtilConstants;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Optional auto user cleanup - config in "application-conf.json". When client changes handle when
 * server is down, old users and undelivered messages will be orphaned.
 * ...
 * Defaults: off - when turned on 1. execute on start up and every 7 days thereafter. 2. remove
 * users who have not logged in for 90 days.
 */
public class CleanOrphanedUsersHibernate extends DodexDatabaseBase {
  private static final Logger logger = LoggerFactory.getLogger(CleanOrphanedUsersHibernate.class.getName());

  private SessionFactory sessionFactory;
  private static Integer age;
  private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private final Runnable clean = new Runnable() {
    @Override
    public void run() {
      runClean();
    }
  };

  public void startClean(JsonObject config)
      throws InterruptedException, IOException, SQLException {
    long delay = 0;
    long period = 0;

    sessionFactory = DodexEntityManager.getEmf();
    if (sessionFactory == null) {
      new DodexEntityManager();
      sessionFactory = DodexEntityManager.getEmf();
    }

    delay = config.getLong("clean.delay");
    period = config.getLong("clean.period");
    age = config.getInteger("clean.age");
    scheduler.scheduleAtFixedRate(clean, delay, period, TimeUnit.DAYS);
  }

  private void runClean() {
    Set<String> names = new HashSet<>();
    Disposable disposable = Flowable.fromCallable(() -> {
      Future<Set<MessageUser>> users = getUsers(sessionFactory);
      users.onSuccess(data -> {
        Set<Long> possibleUsers = getPossibleOrphanedUsers(data);

        cleanUsers(sessionFactory, possibleUsers);

        data.iterator().forEachRemaining(user -> {
          if (possibleUsers.contains(user.getId())) {
            names.add(user.getName());
          }
        });

        logger.info(String.join("", ColorUtilConstants.BLUE_BOLD_BRIGHT, "Cleaned users: ",
            names.toString(), ColorUtilConstants.RESET));
      });

      return String.join("", ColorUtilConstants.BLUE_BOLD_BRIGHT,
          "Starting User/Undelivered/Message Clean: ", ColorUtilConstants.RESET);

    }).subscribeOn(Schedulers.io()).observeOn(Schedulers.single()).subscribe(logger::info,
        Throwable::printStackTrace);
  }

  private Future<Set<MessageUser>> getUsers(SessionFactory sessionFactory) throws SQLException {
    EntityManager entityManager = sessionFactory.createEntityManager();
    Set<MessageUser> listOfUsers = new HashSet<>();
    Promise<Set<MessageUser>> promise = Promise.promise();

    MessageUser dummyUser = createMessageUser();
    dummyUser.setName("");

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Users> userQuery = builder.createQuery(Users.class);
    Root<Users> root = userQuery.from(Users.class);

    ParameterExpression<String> value = builder.parameter(String.class, Users_.NAME);
    userQuery.select(root).where(builder.notEqual(root.<String>get(Users_.NAME), value));

    TypedQuery<Users> query = entityManager.createQuery(userQuery);
    query.setParameter(Users_.NAME, "");
    List<Users> allUsers = query.getResultList();

    for (Users user : allUsers) {
      MessageUser messageUser = createMessageUser();
      messageUser.setId(user.getId());
      messageUser.setName(user.getName());
      messageUser.setPassword(user.getPassword());
      messageUser.setIp(user.getIp());
      messageUser.setLastLogin(user.getLastLogin());
      listOfUsers.add(messageUser);
    }
    promise.complete(listOfUsers);
    entityManager.close();
    return promise.future();
  }

  private static Set<Long> getPossibleOrphanedUsers(Set<MessageUser> users) {
    Set<Long> orphaned = new HashSet<>();

    users.iterator().forEachRemaining(user -> {
      Long days = getLastLogin(user.getLastLogin());

      if (days >= age) {
        orphaned.add(user.getId());
      }
    });

    return orphaned;
  }

  private static Long getLastLogin(Object lastLogin) {
    long currentDate = new Date().getTime();
    long diffInDays;
    long loginDate = 0L;

    if (lastLogin instanceof Date) {
      loginDate = ((Date) lastLogin).getTime();
    }
    if (lastLogin instanceof Timestamp) {
      loginDate = ((Timestamp) lastLogin).getTime();
    }


    long diff = currentDate - loginDate;
    diffInDays = diff / (1000 * 60 * 60 * 24);
    return diffInDays;
  }

  Object value;

  private void cleanUsers(SessionFactory sessionFactory, Set<Long> users) {
    Set<Long> messageIds = new HashSet<>();
    MessageUser messageUser = createMessageUser();
    users.iterator().forEachRemaining(userId -> {

      Future.future(prom -> {
        messageUser.setId(userId);
        EntityManager entityManagerRead = sessionFactory.createEntityManager();
        List<Messages> messages = getUserUndelivered(messageUser, entityManagerRead);
        entityManagerRead.close();

        for (Messages row : messages) {
          messageIds.add(row.getId());
        }
        prom.complete(messageIds);
        entityManager.close();
        EntityManager entityManagerWrite = sessionFactory.createEntityManager();
        entityManagerWrite.getTransaction().begin();
        prom.future().onSuccess(result -> {
          int undelivered = cleanUndelivered(entityManagerWrite, userId, messageIds, users);
          value = result;
        });
        if (value == null) {
          int cleaned = cleanRemainingUsers(entityManagerWrite, users);
        }
        entityManagerWrite.flush();
        entityManagerWrite.getTransaction().commit();
      });
    });
  }

  private int cleanUndelivered(EntityManager entityManager, Long userId, Set<Long> messageIds, Set<Long> users) {
    int[] count = {0};
    Iterator<Long> messageIdIterator = messageIds.iterator();
    messageIdIterator.forEachRemaining(messageId -> {
      Future.future(prom -> {
        removeUndelivered(userId, messageId, entityManager);
        entityManager.flush();
        count[0] += 1;

        if (!messageIdIterator.hasNext()) {
          prom.complete(count[0]);
        }
        prom.future().onSuccess(result -> {
          int messages = cleanMessage(entityManager, messageIds, users);
        });
      });
    });

    return count[0];
  }

  private int cleanMessage(EntityManager entityManager, Set<Long> messageIds, Set<Long> users) {
    int[] count = {0};
    Iterator<Long> messageIdsIterator = messageIds.iterator();
    messageIdsIterator.forEachRemaining(messageId -> {
      Future.future(prom -> {

        Messages currentMessage = entityManager.find(Messages.class, messageId);

        if (currentMessage.getUndelivered().isEmpty()) {
          entityManager.remove(currentMessage);
        }
        count[0]++;
        if (!messageIdsIterator.hasNext()) {
          prom.complete(count[0]);
        }

        prom.future().onSuccess(result -> {
          count[0] += cleanRemainingUsers(entityManager, users);
        });
      });

    });

    return count[0];
  }

  private Future<Object> cleanUser(EntityManager entityManager, Long userId) {

    return Future.future(prom -> {
      Users user = entityManager.find(Users.class, userId);
      entityManager.remove(user);
      prom.complete(1);
    });
  }

  private int cleanRemainingUsers(EntityManager entityManager, Set<Long> users) {
    int[] count = {0};

    users.iterator().forEachRemaining(userId -> {
      cleanUser(entityManager, userId).onSuccess(result -> {
        if (result != null) {
          count[0] += (Integer) result;
        }
      });
    });

    return count[0];
  }

  @Override
  public void configDatabase() {
  }

  @Override
  public void entityManagerSetup() {
  }

  @Override
  public void entityManagerSetup(EntityManagerFactory emf) {
  }

  @Override
  public SessionFactory getEmf() {
    return null;
  }
}
