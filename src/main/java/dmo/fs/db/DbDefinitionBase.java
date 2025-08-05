
package dmo.fs.db;

import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import golf.handicap.db.PopulateCourse;
import golf.handicap.db.PopulateGolfer;
import golf.handicap.db.PopulateGolferScores;
import golf.handicap.db.PopulateScore;
import golf.handicap.vertx.MainVerticle;
import io.reactivex.rxjava3.functions.Action;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.ServerWebSocket;
import io.vertx.rxjava3.jdbcclient.JDBCPool;
import io.vertx.rxjava3.mysqlclient.MySQLClient;
import io.vertx.rxjava3.sqlclient.Pool;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.SqlConnection;
import io.vertx.rxjava3.sqlclient.Tuple;
import org.jooq.DSLContext;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.jooq.impl.DSL.*;

public abstract class DbDefinitionBase {
  private final static Logger logger = LoggerFactory.getLogger(DbDefinitionBase.class.getName());
  protected final static String QUERYUSERS = "select * from users where password=$";
  protected final static String QUERYMESSAGES = "select * from messages where id=$";
  protected final static String QUERYUNDELIVERED = "Select message_id, name, message, from_handle, post_date from users, undelivered, messages where users.id = user_id and message.id = message_id and users.id = $1";

  protected static DSLContext create;

  private static String GETALLUSERS;
  private static String GETUSERBYNAME;
  private static String GETINSERTUSER;
  private static String GETUPDATEUSER;
  private static String GETREMOVEUNDELIVERED;
  private static String GETREMOVEMESSAGE;
  private static String GETUNDELIVEREDMESSAGE;
  private static String GETDELETEUSER;
  private static String GETADDMESSAGE;
  private static String GETADDUNDELIVERED;
  private static String GETUSERNAMES;
  private static String GETUSERBYID;
  private static String GETREMOVEUSERUNDELIVERED;
  private static String GETUSERUNDELIVERED;
  private static String GETDELETEUSERBYID;
  private static String GETSQLITEUPDATEUSER;
  private static String GETREMOVEUSERS;
  private static String GETCUSTOMDELETEMESSAGES;
  private static String GETCUSTOMDELETEUSERS;
  private static String GETMARIAINSERTUSER;
  private static String GETMARIAADDMESSAGE;
  private static String GETMARIADELETEUSER;
  private static String GETMESSAGEIDBYHANDLEDATE;
  private static String GETDELETEMEMBERS;

  private Boolean isTimestamp;
  protected Vertx vertx;
  protected static Pool pool;
  private static boolean qmark = true;

  public static <T> void setupSql(Pool pool5) throws SQLException {
    // Non-Blocking Drivers
    if (DbConfiguration.isUsingPostgres()) {
      qmark = false;
    }

    pool = pool5;

    Settings settings = new Settings().withRenderNamedParamPrefix("$"); // making compatible with Vertx4/Postgres

    create = DSL.using(DodexUtil.getSqlDialect(), settings);
    // Postges works with "$"(numbered) - Others work with "?"(un-numbered)
    GETALLUSERS = qmark ? setupAllUsers().replaceAll("\\$\\d", "?") : setupAllUsers();
    GETUSERBYNAME = qmark ? setupUserByName().replaceAll("\\$\\d", "?") : setupUserByName();
    GETINSERTUSER = qmark ? setupInsertUser().replaceAll("\\$\\d", "?") : setupInsertUser();
    GETMARIAINSERTUSER = qmark ? setupMariaInsertUser().replaceAll("\\$\\d", "?") : setupMariaInsertUser();
    GETUPDATEUSER = qmark ? setupUpdateUser().replaceAll("\\$\\d{1,2}", "?") : setupUpdateUser();
    GETREMOVEUNDELIVERED = qmark ? setupRemoveUndelivered().replaceAll("\\$\\d", "?") : setupRemoveUndelivered();
    GETREMOVEMESSAGE = qmark ? setupRemoveMessage().replaceAll("\\$\\d", "?") : setupRemoveMessage();
    GETUNDELIVEREDMESSAGE = qmark ? setupUndeliveredMessage().replaceAll("\\$\\d", "?") : setupUndeliveredMessage();
    GETDELETEUSER = qmark ? setupDeleteUser().replaceAll("\\$\\d", "?") : setupDeleteUser();
    GETMARIADELETEUSER = qmark ? setupMariaDeleteUser().replaceAll("\\$\\d", "?") : setupMariaDeleteUser();
    GETADDMESSAGE = qmark ? setupAddMessage().replaceAll("\\$\\d", "?") : setupAddMessage();
    GETMARIAADDMESSAGE = qmark ? setupMariaAddMessage().replaceAll("\\$\\d", "?") : setupMariaAddMessage();
    GETADDUNDELIVERED = qmark ? setupAddUndelivered().replaceAll("\\$\\d", "?") : setupAddUndelivered();
    GETUSERNAMES = qmark ? setupUserNames().replaceAll("\\$\\d", "?") : setupUserNames();
    GETUSERBYID = qmark ? setupUserById().replaceAll("\\$\\d", "?") : setupUserById();
    GETREMOVEUSERUNDELIVERED = qmark ? setupRemoveUserUndelivered().replaceAll("\\$\\d", "?")
        : setupRemoveUserUndelivered();
    GETUSERUNDELIVERED = qmark ? setupUserUndelivered().replaceAll("\\$\\d", "?") : setupUserUndelivered();
    GETDELETEUSERBYID = qmark ? setupDeleteUserById().replaceAll("\\$\\d", "?") : setupDeleteUserById();
    GETSQLITEUPDATEUSER = setupSqliteUpdateUser();
    GETREMOVEUSERS = qmark ? setupRemoveUsers().replaceAll("\\$\\d", "?") : setupRemoveUsers();
    GETCUSTOMDELETEMESSAGES = setupCustomDeleteMessage();
    GETCUSTOMDELETEUSERS = setupCustomDeleteUsers();
    GETMESSAGEIDBYHANDLEDATE = qmark ? setupMessageByHandleDate().replaceAll("\\$\\d", "?") : setupRemoveUsers();
    GETDELETEMEMBERS = qmark ? setupDeleteMembers().replaceAll("\\$\\d", "?") : setupDeleteMembers();

    GroupOpenApiSql.setPool(pool);
    GroupOpenApiSql.setCreate(create);
    GroupOpenApiSql.setQmark(qmark);
    GroupOpenApiSql.buildSql();

    if (Boolean.TRUE.equals(MainVerticle.getEnableHandicap())) {
      PopulateGolfer.setQMark(qmark);
      PopulateGolfer.setSqlPool(pool);
      PopulateGolfer.setDslContext(create);
      PopulateGolfer.buildSql();
      PopulateCourse.buildSql();
      PopulateScore.buildSql();
      PopulateGolferScores.buildSql();
    }
  }

  private static String setupAllUsers() {
    return create.renderNamedParams(
        select(field("ID"), field("NAME"), field("PASSWORD"), field("IP"), field("LAST_LOGIN"))
            .from(table("users")).where(field("NAME").ne("$")));
  }

  public String getAllUsers() {
    return GETALLUSERS;
  }

  private static String setupMessageByHandleDate() {
    return create.renderNamedParams(
        select(field("ID"))
            .from(table("messages")).where(field("FROM_HANDLE").eq("$").and(field("POST_DATE").eq("$"))));
  }

  public String getMessageIdByHandleDate() {
    return GETMESSAGEIDBYHANDLEDATE;
  }

  private static String setupUserByName() {
    return create.renderNamedParams(
        select(field("ID"), field("NAME"), field("PASSWORD"), field("IP"), field("LAST_LOGIN"))
            .from(table("users")).where(field("NAME").eq("$")));
  }

  public String getUserByName() {
    return GETUSERBYNAME;
  }

  private static String setupUserById() {
    return create.renderNamedParams(
        select(field("ID"), field("NAME"), field("PASSWORD"), field("IP"), field("LAST_LOGIN"))
            .from(table("users")).where(field("NAME").eq("$")).and(field("PASSWORD").eq("$")));
  }

  public String getUserById() {
    return GETUSERBYID;
  }

  private static String setupInsertUser() {
    return create.renderNamedParams(
        insertInto(table("users")).columns(field("NAME"), field("PASSWORD"), field("IP"), field("LAST_LOGIN"))
            .values("$", "$", "$", "$").returning(field("ID")));
  }

  public String getInsertUser() {
    return GETINSERTUSER;
  }

  private static String setupMariaInsertUser() {
    return create.renderNamedParams(
        insertInto(table("users")).columns(field("NAME"), field("PASSWORD"), field("IP"), field("LAST_LOGIN"))
            .values("$", "$", "$", "$"));
  }

  public String getMariaInsertUser() {
    return GETMARIAINSERTUSER;
  }

  private static String setupUpdateUser() {
    return create.renderNamedParams(insertInto(table("users"))
        .columns(field("ID"), field("NAME"), field("PASSWORD"), field("IP"), field("LAST_LOGIN"))
        .values("$1", "$2", "$3", "$4", "$5").onConflict(field("PASSWORD")).doUpdate()
        .set(field("LAST_LOGIN"), "$5").returning(field("ID")));
  }

  public String getUpdateUser() {
    return GETUPDATEUSER;
  }

  public static String setupSqliteUpdateUser() {
    return "update users set last_login = ? where id = ?";
  }

  public String getSqliteUpdateUser() {
    return GETSQLITEUPDATEUSER;
  }

  public static String setupCustomDeleteUsers() {
    return "DELETE FROM users WHERE id = ? and NOT EXISTS (SELECT mid FROM (SELECT DISTINCT users.id AS mid FROM users INNER JOIN undelivered ON user_id = users.id) AS u )";
  }

  public String getCustomDeleteUsers() {
    return GETCUSTOMDELETEUSERS;
  }

  public static String setupCustomDeleteMessage() {
    return "DELETE FROM messages WHERE id = ? and NOT EXISTS (SELECT mid FROM (SELECT DISTINCT messages.id AS mid FROM messages INNER JOIN undelivered ON message_id = messages.id and messages.id = ?) AS m )";
  }

  public String getCustomDeleteMessages() {
    return GETCUSTOMDELETEMESSAGES;
  }

  private static String setupRemoveUndelivered() {
    return create.renderNamedParams(
        deleteFrom(table("undelivered")).where(field("USER_ID").eq("$1"), field("MESSAGE_ID").eq("$2")));
  }

  public String getRemoveUndelivered() {
    return GETREMOVEUNDELIVERED;
  }

  private static String setupRemoveUserUndelivered() {
    return create.renderNamedParams(deleteFrom(table("undelivered")).where(field("USER_ID").eq("$")));
  }

  public String getRemoveUserUndelivered() {
    return GETREMOVEUSERUNDELIVERED;
  }

  private static String setupRemoveMessage() {
    return create
        .renderNamedParams(
            deleteFrom(table("messages")).where(create.renderNamedParams(field("ID").eq("$1")
                .and(create.renderNamedParams(notExists(select().from(table("messages"))
                    .join(table("undelivered")).on(field("ID").eq(field("MESSAGE_ID")))
                    .and(field("ID").eq("$2"))))))));
  }

  public String getRemoveMessage() {
    return GETREMOVEMESSAGE;
  }

  private static String setupRemoveUsers() {
    return create.renderNamedParams(deleteFrom(table("users")).where(create.renderNamedParams(
        field("ID").eq("$").and(create.renderNamedParams(notExists(select().from(table("users"))
            .join(table("undelivered")).on(field("ID").eq(field("USER_ID"))).and(field("ID").eq("$"))))))));
  }

  public String getRemoveUsers() {
    return GETREMOVEUSERS;
  }

  private static String setupUndeliveredMessage() {
    return create.renderNamedParams(select(field("USER_ID"), field("MESSAGE_ID")).from(table("messages"))
        .join(table("undelivered")).on(field("ID").eq(field("MESSAGE_ID"))).and(field("ID").eq("$"))
        .and(field("USER_ID").eq("$")));
  }

  public String getUndeliveredMessage() {
    return GETUNDELIVEREDMESSAGE;
  }

  private static String setupUserUndelivered() {
    return create.renderNamedParams(select(field("USER_ID"), field("MESSAGE_ID"), field("MESSAGE"),
        field("POST_DATE"), field("FROM_HANDLE")).from(table("users")).join(table("undelivered"))
        .on(field("users.ID").eq(field("USER_ID")).and(field("users.ID").eq("$")))
        .join(table("messages")).on(field("messages.ID").eq(field("MESSAGE_ID"))));
  }

  public String getUserUndelivered() {
    return GETUSERUNDELIVERED;
  }

  private static String setupDeleteUser() {
    return create.renderNamedParams(deleteFrom(table("users"))
        .where(field("NAME").eq("$1"), field("PASSWORD").eq("$2")).returning(field("ID")));
  }

  public String getDeleteUser() {
    return GETDELETEUSER;
  }

  private static String setupMariaDeleteUser() {
    return create.renderNamedParams(
        deleteFrom(table("users")).where(field("NAME").eq("$1"), field("PASSWORD").eq("$2")));
  }

  public String getMariaDeleteUser() {
    return GETMARIADELETEUSER;
  }

  private static String setupDeleteUserById() {
    return create.renderNamedParams(deleteFrom(table("users")).where(field("ID").eq("$1")).returning(field("ID")));
  }

  public String getDeleteUserById() {
    return GETDELETEUSERBYID;
  }

  private static String setupDeleteMembers() {
    return create.renderNamedParams(
        deleteFrom(table("member")).where(field("user_id").eq("$1")));
  }

  public String getDeleteMember() {
    return GETDELETEMEMBERS;
  }

  private static String setupAddMessage() {
    return create.renderNamedParams(
        insertInto(table("messages")).columns(field("MESSAGE"), field("FROM_HANDLE"), field("POST_DATE"))
            .values("$", "$", "$").returning(field("ID")));
  }

  public String getAddMessage() {
    return GETADDMESSAGE;
  }

  private static String setupMariaAddMessage() {
    return create.renderNamedParams(insertInto(table("messages"))
        .columns(field("MESSAGE"), field("FROM_HANDLE"), field("POST_DATE")).values("$", "$", "$"));
  }

  public String getMariaAddMessage() {
    return GETMARIAADDMESSAGE;
  }

  private static String setupAddUndelivered() {
    return create.renderNamedParams(
        insertInto(table("undelivered")).columns(field("USER_ID"), field("MESSAGE_ID")).values("$", "$"));
  }

  public String getAddUndelivered() {
    return GETADDUNDELIVERED;
  }

  private static String setupUserNames() {
    return create.renderNamedParams(
        select(field("ID"), field("NAME"), field("PASSWORD"), field("IP"), field("LAST_LOGIN"))
            .from(table("users")).where(field("NAME").ne("$")));
  }

  public String getUserNames() {
    return GETUSERNAMES;
  }

  public Future<MessageUser> addUser(ServerWebSocket ws, MessageUser messageUser)
      throws InterruptedException, SQLException {
    Promise<MessageUser> promise = Promise.promise();
    Timestamp current = new Timestamp(new Date().getTime());
    OffsetDateTime time = OffsetDateTime.now();

    Object lastLogin = DbConfiguration.isUsingPostgres() ? time : current;

    pool.rxGetConnection().doOnSuccess(conn -> {
      Tuple parameters = Tuple.of(messageUser.getName(), messageUser.getPassword(), messageUser.getIp(),
          lastLogin);
      String sql = getInsertUser();

      if (DbConfiguration.isUsingMariadb()) {
        sql = getMariaInsertUser();
      }

      conn.preparedQuery(sql).rxExecute(parameters).doOnSuccess(rows -> {
        messageUser.setId(0L);
        for (Row row : rows) {
          messageUser.setId(row.getLong(0));
        }
        if (DbConfiguration.isUsingMariadb()) {
          messageUser.setId(rows.property(MySQLClient.LAST_INSERTED_ID));
        } else if (messageUser.getId() == 0) {
          if (DbConfiguration.isUsingSqlite3()) {
            getSqliteLastId(conn, "users", promise, messageUser);
            conn.close().subscribe();
          } else {
            messageUser.setId(rows.property(JDBCPool.GENERATED_KEYS).getLong(0));
          }
        }

        messageUser.setLastLogin(current);
        if (!DbConfiguration.isUsingSqlite3()) {
          conn.close().subscribe();
          promise.tryComplete(messageUser);
        }
      }).doOnError(err -> logger.error("{}Error adding user: {}{}",
          ColorUtilConstants.RED, err, ColorUtilConstants.RESET)).subscribe(rows -> {
        //
      }, err -> {
        logger.error("{}Error Adding user: {}{}", ColorUtilConstants.RED, err, ColorUtilConstants.RESET);
        throw new RuntimeException(err);
      });
    }).doOnError(err -> {
      logger.error("{}Database Error: {}{}",
          ColorUtilConstants.RED, err.getMessage(), ColorUtilConstants.RESET);
      throw new RuntimeException(err);
    }).subscribe();

    return promise.future();
  }

  public Future<Integer> updateUser(ServerWebSocket ws, MessageUser messageUser) {
    Promise<Integer> promise = Promise.promise();

    pool.rxGetConnection().doOnSuccess(conn -> {
      Tuple parameters = getTupleParameters(messageUser);

      String sql = DbConfiguration.isUsingIbmDB2()
          || DbConfiguration.isUsingSqlite3()
          || DbConfiguration.isUsingMariadb()
          || DbConfiguration.isUsingH2() ? getSqliteUpdateUser() : getUpdateUser();

      conn.preparedQuery(sql).rxExecute(parameters).doOnSuccess(rows -> {
        conn.close().subscribe();
        promise.complete(rows.rowCount());
      }).doOnError(err -> {
        logger.error("{}Error Updating user: {}{}", ColorUtilConstants.RED, err, ColorUtilConstants.RESET);
        ws.writeTextMessage(err.toString());
        conn.close().subscribe();
      }).subscribe(rows -> {
        //
      }, err -> {
        logger.error("{}Error Updating user2: {}{}", ColorUtilConstants.RED, err, ColorUtilConstants.RESET);
        throw new RuntimeException(err);
      });
    }).subscribe();

    return promise.future();
  }

  private Tuple getTupleParameters(MessageUser messageUser) {
    Timestamp timeStamp = new Timestamp(new Date().getTime());
    long date = new Date().getTime();
    OffsetDateTime time = OffsetDateTime.now();
    Tuple parameters;

    if (DbConfiguration.isUsingIbmDB2() || DbConfiguration.isUsingSqlite3()
        || DbConfiguration.isUsingMariadb()
        || DbConfiguration.isUsingH2()) {
      parameters = Tuple.of(
          DbConfiguration.isUsingIbmDB2()
              || DbConfiguration.isUsingMariadb()
              || DbConfiguration.isUsingH2() ? timeStamp : date,
          messageUser.getId());
      return parameters;
    }

    parameters = Tuple.of(messageUser.getId(), messageUser.getName(), messageUser.getPassword(),
        messageUser.getIp(), DbConfiguration.isUsingCubrid() ? timeStamp : time,
        DbConfiguration.isUsingCubrid() ? timeStamp : time);

    return parameters;
  }

  public Future<Long> deleteUser(ServerWebSocket ws, MessageUser messageUser)
      throws InterruptedException, SQLException {
    Promise<Long> promise = Promise.promise();

    selectUser(messageUser, ws).onSuccess(mUser -> {
      Long userId = mUser.getId();
      pool.getConnection().doOnSuccess(conn -> {
        String deleteMembersSql = getDeleteMember();
        Tuple deleteMembersParameter = Tuple.of(userId);

        conn.preparedQuery(deleteMembersSql).rxExecute(deleteMembersParameter).doFinally(() -> {
          Tuple parameters = Tuple.of(messageUser.getName(), messageUser.getPassword());

          String sql = getDeleteUser();
          if (DbConfiguration.isUsingMariadb()) {
            sql = getMariaDeleteUser();
          }

          conn.preparedQuery(sql).rxExecute(parameters)
              .doOnSuccess(rows -> {
                Long id = 0L;
                for (Row row : rows) {
                  id = row.getLong(0);
                }
                Long count = (long) rows.rowCount();
                messageUser.setId(id > 0L ? id : count);
                conn.close().subscribe();
                promise.complete(count);
              }).doOnError(err -> {
                String errMessage;
                if (err != null && err.getMessage() == null) {
                  errMessage = String.format("%s%s", "User deleted - but returned: ", err.getMessage());
                } else {
                  errMessage = String.format("%s%s", "Error deleting user: ", err);
                }
                logger.error("{}{}{}", ColorUtilConstants.RED, errMessage, ColorUtilConstants.RESET);
                promise.complete(-1L);
                conn.close().subscribe();
              }).subscribe(rows -> {
                //
              }, err -> {
                  throw new RuntimeException(err);
              });
        }).doOnError(Throwable::printStackTrace).subscribe(v -> {
        }, err -> {
          promise.tryComplete(-1L);
          conn.close().subscribe();
          String errMessage = String.format("%s%s", "Error deleting Group Members: ", err);
          logger.error("{}{}{}", ColorUtilConstants.RED, errMessage, ColorUtilConstants.RESET);
        });
      }).subscribe();
    }).onFailure(err -> {
      promise.tryComplete(-1L);
      String errMessage = String.format("%s%s", "Error Finding User: ", err);
      logger.error("{}{}{}", ColorUtilConstants.RED, errMessage, ColorUtilConstants.RESET);
    });
    return promise.future();
  }

  public Future<Long> addMessage(ServerWebSocket ws, MessageUser messageUser, String message)
      throws InterruptedException, SQLException {
    Promise<Long> promise = Promise.promise();

    OffsetDateTime time = OffsetDateTime.now();
    Timestamp current = new Timestamp(new Date().getTime());

    Object postDate = DbConfiguration.isUsingPostgres() ? time : current;
    Tuple parameters = Tuple.of(message, messageUser.getName(), postDate);

    pool.rxGetConnection().doOnSuccess(conn -> {
      conn.rxBegin().doOnSuccess(tx -> {
        String sql = getAddMessage();
        if (DbConfiguration.isUsingIbmDB2()) {
          sql = String.format("%s%s%s", "SELECT id FROM FINAL TABLE (", getAddMessage(), ")");
        } else if (DbConfiguration.isUsingMariadb()) {
          sql = getMariaAddMessage();
        }

        conn.preparedQuery(sql).rxExecute(parameters).doOnSuccess(rows -> {
          Long id = 0L;
          for (Row row : rows) {
            id = row.getLong(0);
          }

          if (DbConfiguration.isUsingMariadb()) {
            id = rows.property(MySQLClient.LAST_INSERTED_ID);
          } else if (id == 0) {
            if (DbConfiguration.isUsingSqlite3()) {
              tx.commit().doOnSubscribe(v -> {
                getSqliteLastId(conn, "messages", promise);
                conn.close().subscribe();
              }).subscribe();
            } else {
              id = rows.property(JDBCPool.GENERATED_KEYS).getLong(0);
            }
          }

          if (!DbConfiguration.isUsingSqlite3()) {
            tx.commit().doOnComplete(() -> conn.close().subscribe()).subscribe();
            promise.complete(id);
          }
        }).doOnError(err -> {
          logger.error("{}Error adding message: {}{}", ColorUtilConstants.RED, err, ColorUtilConstants.RESET);
          ws.writeTextMessage(err.toString());
          conn.close().subscribe();
        }).subscribe(rows -> {
          //
        }, err -> {
          if (err != null && err.getMessage() != null) {
            throw new RuntimeException(err);
          }
        });
      }).doOnError(err -> logger.error(String.format("%sError Adding Message: - %s%s", ColorUtilConstants.RED,
          err.getMessage(), ColorUtilConstants.RESET))).subscribe();
    }).subscribe();
    return promise.future();
  }

  public Future<Void> addUndelivered(Long userId, Long messageId) throws SQLException, InterruptedException {
    Promise<Void> promise = Promise.promise();

    Tuple parameters = Tuple.of(userId, messageId);
    pool.rxGetConnection().doOnSuccess(conn -> conn.preparedQuery(getAddUndelivered())
        .rxExecute(parameters)
        .doOnSuccess(rows -> {
          conn.close().subscribe();
          promise.complete();
        }).doOnError(err -> {
          logger.error(String.format("%sAdd Undelivered Error: %s%s", ColorUtilConstants.RED,
              err.getMessage(), ColorUtilConstants.RESET));
          conn.close().subscribe();
        }).subscribe()).subscribe();

    return promise.future();
  }

  public void addUndelivered(ServerWebSocket ws, List<String> undelivered, Long messageId) {
    Promise<Void> promise = Promise.promise();
    try {
      for (String name : undelivered) {
        Future<Long> future = getUserIdByName(name);
        future.onSuccess(userId -> {
          try {
            Future<Void> future2 = addUndelivered(userId, messageId);
            future2.onSuccess(handler -> promise.tryComplete());
          } catch (SQLException | InterruptedException e) {
            logger.error(String.join("", "AddUndelivered: ", e.getMessage()));
          }
        });
      }
    } catch (Exception e) {
      ws.writeTextMessage(e.getMessage());
    }
    promise.future();
  }

  public Future<Long> getUserIdByName(String name) throws InterruptedException {
    Promise<Long> promise = Promise.promise();

    pool.rxGetConnection().doOnSuccess(conn -> conn.preparedQuery(getUserByName())
        .rxExecute(Tuple.of(name))
        .doOnSuccess(rows -> {
          Long id = 0L;
          for (Row row : rows) {
            id = row.getLong(0);
          }
          conn.close().subscribe();
          promise.complete(id);
        }).doOnError(err -> {
          logger.error(String.format("%sError finding user by name: %s - %s%s", ColorUtilConstants.RED,
              name, err.getMessage(), ColorUtilConstants.RESET));
          conn.close().subscribe();
        }).subscribe()).subscribe();
    return promise.future();
  }

  public abstract MessageUser createMessageUser();

  public Future<MessageUser> selectUser(MessageUser messageUser, ServerWebSocket ws)
      throws InterruptedException, SQLException {
    MessageUser resultUser = createMessageUser();
    Promise<MessageUser> promise = Promise.promise();

    Tuple parameters = Tuple.of(messageUser.getName(), messageUser.getPassword());

    pool.rxGetConnection().doOnSuccess(conn -> conn.preparedQuery(getUserById())
        .rxExecute(parameters)
        .doOnSuccess(rows -> {
          Future<Integer> future1 = null;

          if (rows.size() == 0) {
            try {
              Future<MessageUser> future2 = addUser(ws, messageUser);

              future2.onComplete(handler -> {
                MessageUser result = future2.result();
                resultUser.setId(result.getId());
                resultUser.setName(result.getName());
                resultUser.setPassword(result.getPassword());
                resultUser.setIp(result.getIp());
                resultUser.setLastLogin(result.getLastLogin() == null ? new Date().getTime()
                    : result.getLastLogin());
                promise.complete(resultUser);
              });
            } catch (InterruptedException | SQLException e) {
              throw new RuntimeException(e);
            } catch (Exception ex) {
              logger.info("Unexpected Error: {}", ex.getMessage());
            }
          } else {
            for (Row row : rows) {
              resultUser.setId(row.getLong(0));
              resultUser.setName(row.getString(1));
              resultUser.setPassword(row.getString(2));
              resultUser.setIp(row.getString(3));
              resultUser.setLastLogin(row.getValue(4));
            }
          }

          if (rows.size() > 0) {
            try {
              conn.close().subscribe();
              future1 = updateUser(ws, resultUser);
              promise.complete(resultUser);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        }).doOnError(err -> {
          logger.error("{}Error selecting user: {}{}",
              ColorUtilConstants.RED, err.getMessage(), ColorUtilConstants.RESET);
          conn.close().subscribe();
        }).subscribe()).subscribe();
    return promise.future();
  }

  public Future<StringBuilder> buildUsersJson(MessageUser messageUser)
      throws InterruptedException, SQLException {
    Promise<StringBuilder> promise = Promise.promise();

    pool.rxGetConnection().doOnSuccess(conn -> conn.preparedQuery(getAllUsers())
        .rxExecute(Tuple.of(messageUser.getName()))
        .doOnSuccess(rows -> {
          JsonArray ja = new JsonArray();

          for (Row row : rows) {
            ja.add(new JsonObject().put("name", row.getString(1)));
          }
          conn.close().subscribe();
          promise.complete(new StringBuilder(ja.toString()));
        }).doOnError(err -> {
          logger.error("{}Error build user json: {}{}",
              ColorUtilConstants.RED, err.getMessage(), ColorUtilConstants.RESET);
          conn.close().subscribe();
        }).subscribe()).subscribe();

    return promise.future();
  }

  public Future<Map<String, Integer>> processUserMessages(ServerWebSocket ws, MessageUser messageUser)
      throws Exception {
    RemoveUndelivered removeUndelivered = new RemoveUndelivered();
    RemoveMessage removeMessage = new RemoveMessage();
    CompletePromise completePromise = new CompletePromise();

    removeUndelivered.setUserId(messageUser.getId());
    /*
     * Get all undelivered messages for current user
     */
    Future<Void> future = Future.future(promise -> {
      completePromise.setPromise(promise);

      Tuple parameters = Tuple.of(messageUser.getId());

      pool.rxGetConnection().flatMapCompletable(conn -> conn.rxBegin().flatMapCompletable(
              tx -> conn.preparedQuery(getUserUndelivered()).rxExecute(parameters)
                  .doOnSuccess(rows -> {
                    for (Row row : rows) {
                      DateFormat formatDate = DateFormat.getDateInstance(DateFormat.DEFAULT,
                          Locale.getDefault());

                      String message = null;
                      Object mariadbMessage = null;
                      mariadbMessage = row.getValue(2);

                      String handle = row.getString(4);
                      messageUser.setLastLogin(row.getValue(3));

                      // Send messages back to client
                      ws.writeTextMessage(
                          handle + formatDate.format(messageUser.getLastLogin()) + " " + mariadbMessage);
                      removeUndelivered.getMessageIds().add(row.getLong(1));
                      removeMessage.getMessageIds().add(row.getLong(1));
                    }
                  }).doOnError(err -> {
                    logger.info("{}Retrieving Messages Error: {}{}", ColorUtilConstants.RED,
                        err.getMessage(), ColorUtilConstants.RESET);
                    throw new RuntimeException(err);
                  }).flatMapCompletable(
                      res -> tx.rxCommit().doFinally(completePromise)
                          .doOnSubscribe(onSubscribe -> tx.rxCompletion()
                              .doOnError(err -> {
                                tx.rollback();
                                logger.error("{}Messages Transaction Error: {}{}",
                                    ColorUtilConstants.RED, err.getMessage(), ColorUtilConstants.RESET);
                              }))))
          .doOnError(err -> {
            logger.info("{}Database for Messages Error: {}{}", ColorUtilConstants.RED,
                err.getMessage(), ColorUtilConstants.RESET);
            conn.close().subscribe();
            throw new RuntimeException(err);
          })).subscribe();
    });

    future.compose(v -> Future.<Void>future(promise -> {
      removeUndelivered.setPromise(promise);
      try {
        removeUndelivered.run();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      if (removeUndelivered.getCount() > 0) {
        logger.info(String.join(ColorUtilConstants.BLUE_BOLD_BRIGHT,
            Integer.toString(removeUndelivered.getCount()), " Messages Delivered", " to ",
            messageUser.getName(), ColorUtilConstants.RESET));
      }
    })).compose(v -> Future.<Void>future(promise -> {
      removeMessage.setPromise(promise);
      try {
        removeMessage.run();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }));

    return removeUndelivered.getPromise2().future();
  }

  private void getSqliteLastId(SqlConnection conn, String table, Promise<Long> promise) {
    conn.query("SELECT rowid from " + table + " order by ROWID DESC limit 1").execute()
        .doOnSuccess(rows -> {
          long id = 0L;
          for (Row row : rows) {
            id = row.getLong(0);
          }
          promise.complete(id);
        }).subscribe();
  }

  private void getSqliteLastId(SqlConnection conn, String table, Promise<MessageUser> promise, MessageUser messageUser) {
    conn.query("SELECT rowid from " + table + " order by ROWID DESC limit 1").execute()
        .doOnSuccess(rows -> {
          for (Row row : rows) {
            messageUser.setId(row.getLong(0));
          }
          promise.complete(messageUser);
        }).subscribe();
  }

  static class CompletePromise implements Action {
    Promise<Void> promise;

    @Override
    public void run() throws Exception {
      promise.tryComplete();
    }

    public Promise<Void> getPromise() {
      return promise;
    }

    public void setPromise(Promise<Void> promise) {
      this.promise = promise;
    }
  }

  class RemoveUndelivered implements Action {
    List<Long> messageIds = new ArrayList<>();
    CompletePromise completePromise = new CompletePromise();
    Long userId;
    int count;
    Promise<Void> promise;
    Promise<Map<String, Integer>> promise2 = Promise.promise();
    Map<String, Integer> counts = new ConcurrentHashMap<>();

    @Override
    public void run() throws Exception {
      completePromise.setPromise(promise);

      for (Long messageId : messageIds) {
        Tuple parameters = Tuple.of(userId, messageId);
        pool.rxGetConnection().doOnSuccess(conn -> conn.preparedQuery(getRemoveUndelivered()).rxExecute(parameters).doOnSuccess(rows -> {
          for (Row row : rows) {
            System.out.println(row.toJson().toString());
          }
          count += rows.rowCount() == 0 ? 1 : rows.rowCount();

          if (messageIds.size() == count) {
            try {
              conn.close().subscribe();
              completePromise.run();
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
            counts.put("messages", count);
            promise2.complete(counts);
          }
        }).doOnError(err -> logger.error("Deleting Undelivered: {}",
            err.getMessage())).subscribe()).subscribe();
      }
    }

    public int getCount() {
      return count;
    }

    public void setCount(int count) {
      this.count = count;
    }

    public List<Long> getMessageIds() {
      return messageIds;
    }

    public void setMessageIds(List<Long> messageIds) {
      this.messageIds = messageIds;
    }

    public Long getUserId() {
      return userId;
    }

    public void setUserId(Long userId) {
      this.userId = userId;
    }

    public Promise<Void> getPromise() {
      return promise;
    }

    public void setPromise(Promise<Void> promise) {
      this.promise = promise;
    }

    public Promise<Map<String, Integer>> getPromise2() {
      return promise2;
    }

    public void setPromise2(Promise<Map<String, Integer>> promise2) {
      this.promise2 = promise2;
    }
  }

  class RemoveMessage implements Action {
    int count;
    List<Long> messageIds = new ArrayList<>();
    CompletePromise completePromise = new CompletePromise();
    Promise<Void> promise;

    @Override
    public void run() throws Exception {
      completePromise.setPromise(promise);

      for (Long messageId : messageIds) {
        if (DbConfiguration.isUsingSqlite3()) {
          try { // Sqlite3 needs a delay???
            Thread.sleep(100);
          } catch (InterruptedException e) {
            logger.error(String.join("", "Await: ", e.getMessage()));
          }
        }

        pool.rxGetConnection().doOnSuccess(conn -> {
          Tuple parameters = Tuple.of(messageId, messageId);
          String sql;
          if (DbConfiguration.isUsingIbmDB2() || DbConfiguration.isUsingMariadb()
              || DbConfiguration.isUsingSqlite3()) {
            sql = getCustomDeleteMessages();
          } else {
            if (DbConfiguration.isUsingPostgres()) {
              parameters = Tuple.of(messageId);
            }
            sql = getRemoveMessage();
          }

          conn.preparedQuery(sql).rxExecute(parameters).doOnSuccess(rows -> {
                count += rows.rowCount() == 0 ? 1 : rows.rowCount();

                if (messageIds.size() == count) {
                  conn.close().subscribe();
                }
              }).doOnError(err -> logger.error("{}Deleting Message2: {}{}", ColorUtilConstants.RED, err,
                  ColorUtilConstants.RESET))
              .doFinally(completePromise)
              .subscribe(rows -> {
              }, Throwable::printStackTrace);
        }).subscribe();
      }
    }

    public List<Long> getMessageIds() {
      return messageIds;
    }

    public void setMessageIds(List<Long> messageIds) {
      this.messageIds = messageIds;
    }

    public Promise<Void> getPromise() {
      return promise;
    }

    public void setPromise(Promise<Void> promise) {
      this.promise = promise;
    }
  }

  public void setIsTimestamp(Boolean isTimestamp) {
    this.isTimestamp = isTimestamp;
  }

  public boolean getIsTimestamp() {
    return this.isTimestamp;
  }

  public Vertx getVertx() {
    return vertx;
  }

  public void setVertx(Vertx vertx) {
    this.vertx = vertx;
  }

  public static DSLContext getCreate() {
    return create;
  }
}
