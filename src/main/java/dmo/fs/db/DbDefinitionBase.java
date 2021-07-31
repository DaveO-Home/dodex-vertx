
package dmo.fs.db;

import static org.jooq.impl.DSL.deleteFrom;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.insertInto;
import static org.jooq.impl.DSL.notExists;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.table;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jooq.DSLContext;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import io.reactivex.functions.Action;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.ServerWebSocket;
import io.vertx.reactivex.db2client.DB2Pool;
import io.vertx.reactivex.jdbcclient.JDBCPool;
import io.vertx.reactivex.mysqlclient.MySQLClient;
import io.vertx.reactivex.mysqlclient.MySQLPool;
import io.vertx.reactivex.pgclient.PgPool;
import io.vertx.reactivex.sqlclient.Pool;
import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.RowSet;
import io.vertx.reactivex.sqlclient.SqlConnection;
import io.vertx.reactivex.sqlclient.Tuple;

public abstract class DbDefinitionBase {
    private final static Logger logger = LoggerFactory.getLogger(DbDefinitionBase.class.getName());
    protected final static String QUERYUSERS = "select * from USERS where password=$";
    protected final static String QUERYMESSAGES = "select * from MESSAGES where id=$";
    protected final static String QUERYUNDELIVERED = "Select message_id, name, message, from_handle, post_date from USERS, UNDELIVERED, MESSAGES where USERS.id = user_id and MESSAGES.id = message_id and USERS.id = $1";

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
    private Boolean isTimestamp;
    protected Vertx vertx;
    protected static Pool pool;
    private static boolean qmark = true;

    public static <T> void setupSql(T pool4) throws SQLException {
        // Non-Blocking Drivers
        if (pool4 instanceof PgPool) {
            pool = (PgPool) pool4;
            qmark = false;
        } else if (pool4 instanceof MySQLPool) {
            pool = (MySQLPool) pool4;
        } else if (pool4 instanceof DB2Pool) {
            pool = (DB2Pool) pool4;
        } else if (pool4 instanceof JDBCPool) {
            pool = (JDBCPool) pool4;
        }

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
    }

    private static String setupAllUsers() {
        return create.renderNamedParams(
                select(field("ID"), field("NAME"), field("PASSWORD"), field("IP"), field("LAST_LOGIN"))
                        .from(table("USERS")).where(field("NAME").ne("$")));
    }

    public String getAllUsers() {
        return GETALLUSERS;
    }

    private static String setupUserByName() {
        return create.renderNamedParams(
                select(field("ID"), field("NAME"), field("PASSWORD"), field("IP"), field("LAST_LOGIN"))
                        .from(table("USERS")).where(field("NAME").eq("$")));
    }

    public String getUserByName() {
        return GETUSERBYNAME;
    }

    private static String setupUserById() {
        return create.renderNamedParams(
                select(field("ID"), field("NAME"), field("PASSWORD"), field("IP"), field("LAST_LOGIN"))
                        .from(table("USERS")).where(field("NAME").eq("$")).and(field("PASSWORD").eq("$")));
    }

    public String getUserById() {
        return GETUSERBYID;
    }

    private static String setupInsertUser() {
        return create.renderNamedParams(
                insertInto(table("USERS")).columns(field("NAME"), field("PASSWORD"), field("IP"), field("LAST_LOGIN"))
                        .values("$", "$", "$", "$").returning(field("ID")));
    }

    public String getInsertUser() {
        return GETINSERTUSER;
    }

    private static String setupMariaInsertUser() {
        return create.renderNamedParams(
                insertInto(table("USERS")).columns(field("NAME"), field("PASSWORD"), field("IP"), field("LAST_LOGIN"))
                        .values("$", "$", "$", "$"));
    }

    public String getMariaInsertUser() {
        return GETMARIAINSERTUSER;
    }

    private static String setupUpdateUser() {
        return create.renderNamedParams(insertInto(table("USERS"))
                .columns(field("ID"), field("NAME"), field("PASSWORD"), field("IP"), field("LAST_LOGIN"))
                .values("$1", "$2", "$3", "$4", "$5").onConflict(field("PASSWORD")).doUpdate()
                .set(field("LAST_LOGIN"), "$5").returning(field("ID")));
    }

    public String getUpdateUser() {
        return GETUPDATEUSER;
    }

    public static String setupSqliteUpdateUser() {
        return "update USERS set last_login = ? where id = ?";
    }

    public String getSqliteUpdateUser() {
        return GETSQLITEUPDATEUSER;
    }

    public static String setupCustomDeleteUsers() {
        return "DELETE FROM USERS WHERE id = ? and NOT EXISTS (SELECT mid FROM (SELECT DISTINCT USERS.id AS mid FROM USERS INNER JOIN UNDELIVERED ON user_id = USERS.id) AS u )";
    }

    public String getCustomDeleteUsers() {
        return GETCUSTOMDELETEUSERS;
    }

    public static String setupCustomDeleteMessage() {
        return "DELETE FROM MESSAGES WHERE id = ? and NOT EXISTS (SELECT mid FROM (SELECT DISTINCT MESSAGES.id AS mid FROM MESSAGES INNER JOIN UNDELIVERED ON message_id = MESSAGES.id and MESSAGES.id = ?) AS m )";
    }

    public String getCustomDeleteMessages() {
        return GETCUSTOMDELETEMESSAGES;
    }

    private static String setupRemoveUndelivered() {
        return create.renderNamedParams(
                deleteFrom(table("UNDELIVERED")).where(field("USER_ID").eq("$1"), field("MESSAGE_ID").eq("$2")));
    }

    public String getRemoveUndelivered() {
        return GETREMOVEUNDELIVERED;
    }

    private static String setupRemoveUserUndelivered() {
        return create.renderNamedParams(deleteFrom(table("UNDELIVERED")).where(field("USER_ID").eq("$")));
    }

    public String getRemoveUserUndelivered() {
        return GETREMOVEUSERUNDELIVERED;
    }

    private static String setupRemoveMessage() {
        return create
                .renderNamedParams(
                        deleteFrom(table("MESSAGES")).where(create.renderNamedParams(field("ID").eq("$1")
                                .and(create.renderNamedParams(notExists(select().from(table("MESSAGES"))
                                        .join(table("UNDELIVERED")).on(field("ID").eq(field("MESSAGE_ID")))
                                        .and(field("ID").eq("$2"))))))));
    }

    public String getRemoveMessage() {
        return GETREMOVEMESSAGE;
    }

    private static String setupRemoveUsers() {
        return create.renderNamedParams(deleteFrom(table("USERS")).where(create.renderNamedParams(
                field("ID").eq("$").and(create.renderNamedParams(notExists(select().from(table("USERS"))
                        .join(table("UNDELIVERED")).on(field("ID").eq(field("USER_ID"))).and(field("ID").eq("$"))))))));
    }

    public String getRemoveUsers() {
        return GETREMOVEUSERS;
    }

    private static String setupUndeliveredMessage() {
        return create.renderNamedParams(select(field("USER_ID"), field("MESSAGE_ID")).from(table("MESSAGES"))
                .join(table("UNDELIVERED")).on(field("ID").eq(field("MESSAGE_ID"))).and(field("ID").eq("$"))
                .and(field("USER_ID").eq("$")));
    }

    public String getUndeliveredMessage() {
        return GETUNDELIVEREDMESSAGE;
    }

    private static String setupUserUndelivered() {
        return create.renderNamedParams(select(field("USER_ID"), field("MESSAGE_ID"), field("MESSAGE"),
                field("POST_DATE"), field("FROM_HANDLE")).from(table("USERS")).join(table("UNDELIVERED"))
                        .on(field("USERS.ID").eq(field("USER_ID")).and(field("USERS.ID").eq("$")))
                        .join(table("MESSAGES")).on(field("MESSAGES.ID").eq(field("MESSAGE_ID"))));
    }

    public String getUserUndelivered() {
        return GETUSERUNDELIVERED;
    }

    private static String setupDeleteUser() {
        return create.renderNamedParams(deleteFrom(table("USERS"))
                .where(field("NAME").eq("$1"), field("PASSWORD").eq("$2")).returning(field("ID")));
    }

    public String getDeleteUser() {
        return GETDELETEUSER;
    }

    private static String setupMariaDeleteUser() {
        return create.renderNamedParams(
                deleteFrom(table("USERS")).where(field("NAME").eq("$1"), field("PASSWORD").eq("$2")));
    }

    public String getMariaDeleteUser() {
        return GETMARIADELETEUSER;
    }

    private static String setupDeleteUserById() {
        return create.renderNamedParams(deleteFrom(table("USERS")).where(field("ID").eq("$1")).returning(field("ID")));
    }

    public String getDeleteUserById() {
        return GETDELETEUSERBYID;
    }

    private static String setupAddMessage() {
        return create.renderNamedParams(
                insertInto(table("MESSAGES")).columns(field("MESSAGE"), field("FROM_HANDLE"), field("POST_DATE"))
                        .values("$", "$", "$").returning(field("ID")));
    }

    public String getAddMessage() {
        return GETADDMESSAGE;
    }

    private static String setupMariaAddMessage() {
        return create.renderNamedParams(insertInto(table("MESSAGES"))
                .columns(field("MESSAGE"), field("FROM_HANDLE"), field("POST_DATE")).values("$", "$", "$"));
    }

    public String getMariaAddMessage() {
        return GETMARIAADDMESSAGE;
    }

    private static String setupAddUndelivered() {
        return create.renderNamedParams(
                insertInto(table("UNDELIVERED")).columns(field("USER_ID"), field("MESSAGE_ID")).values("$", "$"));
    }

    public String getAddUndelivered() {
        return GETADDUNDELIVERED;
    }

    private static String setupUserNames() {
        return create.renderNamedParams(
                select(field("ID"), field("NAME"), field("PASSWORD"), field("IP"), field("LAST_LOGIN"))
                        .from(table("USERS")).where(field("NAME").ne("$")));
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

        pool.getConnection(c -> {
            Tuple parameters = Tuple.of(messageUser.getName(), messageUser.getPassword(), messageUser.getIp(),
                    lastLogin);
            SqlConnection conn = c.result();
            String sql = getInsertUser();

            if (DbConfiguration.isUsingMariadb()) {
                sql = getMariaInsertUser();
            }

            conn.preparedQuery(sql).rxExecute(parameters).doOnSuccess(rows -> {
                for (Row row : rows) {
                    messageUser.setId(row.getLong(0));
                }
                if (DbConfiguration.isUsingMariadb()) {
                    messageUser.setId(rows.property(MySQLClient.LAST_INSERTED_ID));
                } else if (DbConfiguration.isUsingSqlite3() || DbConfiguration.isUsingCubrid()) {
                    messageUser.setId(rows.property(JDBCPool.GENERATED_KEYS).getLong(0));
                }
                messageUser.setLastLogin(current);

                conn.close();
                promise.tryComplete(messageUser);
            }).doOnError(err -> {
                logger.error(String.format("%sError adding user: %s%s", ColorUtilConstants.RED, err,
                        ColorUtilConstants.RESET));
            }).subscribe(rows -> {
                //
            }, err -> {
                logger.error(String.format("%sError Adding user: %s%s", ColorUtilConstants.RED, err,
                        ColorUtilConstants.RESET));
                err.printStackTrace();
            });
        });

        return promise.future();
    }

    public Future<Integer> updateUser(ServerWebSocket ws, MessageUser messageUser)
            throws InterruptedException, SQLException {
        Promise<Integer> promise = Promise.promise();

        pool.getConnection(c -> {
            SqlConnection conn = c.result();

            Tuple parameters = getTupleParameters(messageUser);

            String sql = DbConfiguration.isUsingIbmDB2() || DbConfiguration.isUsingSqlite3()
                    || DbConfiguration.isUsingMariadb() ? getSqliteUpdateUser() : getUpdateUser();

            conn.preparedQuery(sql).rxExecute(parameters).doOnSuccess(rows -> {
                conn.close();
                promise.complete(rows.rowCount());
            }).doOnError(err -> {
                logger.error(String.format("%sError Updating user: %s%s", ColorUtilConstants.RED, err,
                        ColorUtilConstants.RESET));
                ws.writeTextMessage(err.toString());
                conn.close();
            }).subscribe(rows -> {
                //
            }, err -> {
                logger.error(String.format("%sError Updating user: %s%s", ColorUtilConstants.RED, err,
                        ColorUtilConstants.RESET));
                err.printStackTrace();
            });
        });

        return promise.future();
    }

    private Tuple getTupleParameters(MessageUser messageUser) {
        Timestamp timeStamp = new Timestamp(new Date().getTime());
        Long date = new Date().getTime();
        OffsetDateTime time = OffsetDateTime.now();
        Tuple parameters;

        if (DbConfiguration.isUsingIbmDB2() || DbConfiguration.isUsingSqlite3() || DbConfiguration.isUsingMariadb()) {
            parameters = Tuple.of(
                    DbConfiguration.isUsingIbmDB2() || DbConfiguration.isUsingMariadb() ? timeStamp : date,
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

        pool.getConnection(c -> {
            Tuple parameters = Tuple.of(messageUser.getName(), messageUser.getPassword());
            SqlConnection conn = c.result();

            String sql = getDeleteUser();
            if (DbConfiguration.isUsingMariadb()) {
                sql = getMariaDeleteUser();
            }

            conn.preparedQuery(sql).rxExecute(parameters).doOnSuccess(rows -> {
                Long id = 0L;
                for (Row row : rows) {
                    id = row.getLong(0);
                }
                Long count = Long.valueOf(rows.rowCount());
                messageUser.setId(id > 0L ? id : count);
                conn.close();
                promise.complete(count);
            }).doOnError(err -> {
                String errMessage = null;
                if (err != null && err.getMessage() == null) {
                    errMessage = String.format("%s%s", "User deleted - but returned: ", err.getMessage());
                } else {
                    errMessage = String.format("%s%s", "Error deleting user: ", err);
                }
                logger.error(String.format("%s%s%s", ColorUtilConstants.RED, errMessage, ColorUtilConstants.RESET));
                promise.complete(-1L);
                conn.close();
            }).subscribe(rows -> {
                //
            }, err -> {
                if (err != null && err.getMessage() != null) {
                    err.printStackTrace();
                }
            });
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

        pool.getConnection(ar -> {
            if (ar.succeeded()) {
                SqlConnection conn = ar.result();

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
                    } else if (DbConfiguration.isUsingSqlite3() || DbConfiguration.isUsingCubrid()) {
                        id = rows.property(JDBCPool.GENERATED_KEYS).getLong(0);
                    }
                    conn.close();
                    promise.complete(id);
                }).doOnError(err -> {
                    logger.error(String.format("%sError adding messaage: %s%s", ColorUtilConstants.RED, err,
                            ColorUtilConstants.RESET));
                    ws.writeTextMessage(err.toString());
                    conn.close();
                }).subscribe(rows -> {
                    //
                }, err -> {
                    if (err != null && err.getMessage() != null) {
                        err.printStackTrace();
                    }
                });
            } 
            
            if(ar.failed()) {
                logger.error(String.format("%sError Adding Message: - %s%s", ColorUtilConstants.RED,
                        ar.cause().getMessage(), ColorUtilConstants.RESET));
            }
        });

        return promise.future();
    }

    public Future<Void> addUndelivered(Long userId, Long messageId) throws SQLException, InterruptedException {
        Promise<Void> promise = Promise.promise();

        Tuple parameters = Tuple.of(userId, messageId);
        pool.getConnection(c -> {
            SqlConnection conn = c.result();

            conn.preparedQuery(getAddUndelivered()).execute(parameters, ar -> {
                if (ar.succeeded()) {
                    conn.close();
                    promise.complete();
                } else {
                    logger.error(String.format("%sAdd Undelivered Error: %s%s", ColorUtilConstants.RED,
                            ar.cause().getMessage(), ColorUtilConstants.RESET));
                    conn.close();
                }
            });
        });

        return promise.future();
    }

    public Future<Void> addUndelivered(ServerWebSocket ws, List<String> undelivered, Long messageId) {
        Promise<Void> promise = Promise.promise();
        try {
            for (String name : undelivered) {
                Future<Long> future = getUserIdByName(name);
                future.onSuccess(userId -> {
                    try {
                        Future<Void> future2 = addUndelivered(userId, messageId);
                        future2.onSuccess(handler -> {
                            promise.tryComplete();
                        });
                    } catch (SQLException | InterruptedException e) {
                        logger.error(String.join("", "AddUndelivered: ", e.getMessage()));
                    }
                });
            }
        } catch (Exception e) {
            ws.writeTextMessage(e.getMessage());
        }
        return promise.future();
    }

    public Future<Long> getUserIdByName(String name) throws InterruptedException {
        Promise<Long> promise = Promise.promise();

        pool.getConnection(c -> {
            SqlConnection conn = c.result();
        
            conn.preparedQuery(getUserByName()).execute(Tuple.of(name), ar -> {
                if (ar.succeeded()) {
                    RowSet<Row> rows = ar.result();
                    Long id = 0L;
                    for (Row row : rows) {
                        id = row.getLong(0);
                    }
                    conn.close();
                    promise.complete(id);
                } else {
                    logger.error(String.format("%sError finding user by name: %s - %s%s", ColorUtilConstants.RED,
                            name, ar.cause().getMessage(), ColorUtilConstants.RESET));
                    conn.close();
                }
            });
        });
        return promise.future();
    }

    public abstract MessageUser createMessageUser();

    public Future<MessageUser> selectUser(MessageUser messageUser, ServerWebSocket ws)
            throws InterruptedException, SQLException {
        MessageUser resultUser = createMessageUser();
        Promise<MessageUser> promise = Promise.promise();

        Tuple parameters = Tuple.of(messageUser.getName(), messageUser.getPassword());

        pool.getConnection(c -> {
            SqlConnection conn = c.result();

            
            conn.preparedQuery(getUserById()).execute(parameters, ar -> {
                if (ar.succeeded()) {
                    Future<Integer> future1 = null;
                    RowSet<Row> rows = ar.result();

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
                            e.printStackTrace();
                        } catch (Exception ex) {
                            ex.getCause().getMessage();
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
                            conn.close();
                            future1 = updateUser(ws, resultUser);
                            promise.complete(resultUser);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (rows.size() > 0) {
                        future1.onComplete(v -> {
                            // logger.info(String.format("%sLogin Time Changed: %s%s",
                            // ColorUtilConstants.BLUE,
                            // resultUser.getName(), ColorUtilConstants.RESET));
                        });
                    }
                } else {
                    logger.error(String.format("%sError selecting user: %s%s", ColorUtilConstants.RED,
                            ar.cause().getMessage(), ColorUtilConstants.RESET));
                    conn.close();
                }
            });
        });
        return promise.future();
    }

    public Future<StringBuilder> buildUsersJson(MessageUser messageUser) throws InterruptedException, SQLException {
        Promise<StringBuilder> promise = Promise.promise();

        pool.getConnection(c -> {
            SqlConnection conn = c.result();

            conn.preparedQuery(getAllUsers()).execute(Tuple.of(messageUser.getName()), ar -> {
                if (ar.succeeded()) {
                    RowSet<Row> rows = ar.result();
                    JsonArray ja = new JsonArray();

                    for (Row row : rows) {
                        ja.add(new JsonObject().put("name", row.getString(1)));
                    }
                    conn.close();
                    promise.complete(new StringBuilder(ja.toString()));
                } else {
                    logger.error(String.format("%sError build user json: %s%s", ColorUtilConstants.RED,
                            ar.cause().getMessage(), ColorUtilConstants.RESET));
                    conn.close();
                }
            });
        });

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
                tx -> conn.preparedQuery(getUserUndelivered()).rxExecute(parameters).doOnSuccess(rows -> {
                    for (Row row : rows) {
                        DateFormat formatDate = DateFormat.getDateInstance(DateFormat.DEFAULT,
                                Locale.getDefault());

                        String message = row.getString(2);
                        String handle = row.getString(4);
                        messageUser.setLastLogin(row.getValue(3));

                        // Send messages back to client
                        ws.writeTextMessage(
                                handle + formatDate.format(messageUser.getLastLogin()) + " " + message);
                        removeUndelivered.getMessageIds().add(row.getLong(1));
                        removeMessage.getMessageIds().add(row.getLong(1));
                    }
                }).doOnError(err -> {
                    logger.info(String.format("%sRetriveing Messages Error: %s%s", ColorUtilConstants.RED,
                            err.getMessage(), ColorUtilConstants.RESET));
                    err.printStackTrace();
                }).flatMapCompletable(
                        res -> tx.rxCommit().doFinally(completePromise).doOnSubscribe(onSubscribe -> {
                            tx.completion(x -> {
                                if (x.failed()) {
                                    tx.rollback();
                                    logger.error(
                                        String.format("%sMessages Transaction Error: %s%s",
                                            ColorUtilConstants.RED, x.cause(), ColorUtilConstants.RESET));
                                }
                                // conn.close();
                            });
                        })))
                .doOnError(err -> {
                    logger.info(String.format("%sDatabase for Messages Error: %s%s", ColorUtilConstants.RED,
                            err.getMessage(), ColorUtilConstants.RESET));
                    err.printStackTrace();
                    conn.close();
                })).subscribe();
        });

        future.compose(v -> {
            return Future.<Void>future(promise -> {
                removeUndelivered.setPromise(promise);
                try {
                    removeUndelivered.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (removeUndelivered.getCount() > 0) {
                    logger.info(String.join(ColorUtilConstants.BLUE_BOLD_BRIGHT,
                            Integer.toString(removeUndelivered.getCount()), " Messages Delivered", " to ",
                            messageUser.getName(), ColorUtilConstants.RESET));
                }
            });
        }).compose(v -> {
            return Future.<Void>future(promise -> {
                removeMessage.setPromise(promise);
                try {
                    removeMessage.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });

        return removeUndelivered.getPromise2().future();
    }

    class CompletePromise implements Action {
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
                pool.getConnection(c -> {
                    SqlConnection conn = c.result();

                    conn.preparedQuery(getRemoveUndelivered()).execute(parameters, ar -> {
                        if (ar.succeeded()) {
                            RowSet<Row> rows = ar.result();
                            for (Row row : rows) {
                                System.out.println(row.toJson().toString());
                            }
                            count += rows.rowCount() == 0 ? 1 : rows.rowCount();
                        } else {
                            logger.error("Deleting Undelivered: " + ar.cause().getMessage());
                        }
                        if (messageIds.size() == count) {
                            try {
                                conn.close();
                                completePromise.run();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            counts.put("messages", count);
                            promise2.complete(counts);
                        }
                    });
                });
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

                pool.getConnection(c -> {
                    Tuple parameters = Tuple.of(messageId, messageId);
                    String sql = null;
                    if (DbConfiguration.isUsingIbmDB2() || DbConfiguration.isUsingMariadb()
                            || DbConfiguration.isUsingSqlite3()) {
                        sql = getCustomDeleteMessages();
                    } else {
                        parameters = Tuple.of(messageId);
                        sql = getRemoveMessage();
                    }
                    SqlConnection conn = c.result();

                    conn.preparedQuery(sql).rxExecute(parameters).doOnSuccess(rows -> {
                        count += rows.rowCount() == 0 ? 1 : rows.rowCount();

                        if (messageIds.size() == count) {
                            conn.close();
                        }
                    }).doOnError(err -> {
                        logger.error(String.format("%sDeleting Message2: %s%s", ColorUtilConstants.RED, err,
                                ColorUtilConstants.RESET));
                    }).doFinally(completePromise).subscribe(rows -> {
                    }, err -> {
                        err.printStackTrace();
                    });
                });
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

    public boolean getisTimestamp() {
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
