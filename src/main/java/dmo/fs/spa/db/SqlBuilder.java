
package dmo.fs.spa.db;

import static org.jooq.impl.DSL.deleteFrom;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.insertInto;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.table;
import static org.jooq.impl.DSL.update;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Date;

import dmo.fs.db.DbConfiguration;
import org.jooq.DSLContext;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dmo.fs.spa.utils.SpaLogin;
import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.rxjava3.db2client.DB2Pool;
import io.vertx.rxjava3.jdbcclient.JDBCPool;
import io.vertx.rxjava3.mysqlclient.MySQLPool;
import io.vertx.rxjava3.pgclient.PgPool;
import io.vertx.rxjava3.sqlclient.Pool;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.Tuple;

public abstract class SqlBuilder {
    private static final Logger logger = LoggerFactory.getLogger(SqlBuilder.class.getName());
    protected static final String QUERYLOGIN = "select * from LOGIN where name=?";

    private static DSLContext create;

    private static String GETLOGINBYNP;
    private static String GETLOGINBYNAME;
    private static String GETINSERTLOGIN;
    private static String GETREMOVELOGIN;
    private static String GETUPDATELOGIN;
    private static String GETLOGINBYID;
    private static String GETSQLITEUPDATELOGIN;
    private Boolean isTimestamp;
    private static Pool pool;
    private static Boolean qmark = true;

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

        Settings settings = new Settings().withRenderNamedParamPrefix("$"); // making compatible
                                                                            // with Vertx4/Postgres

        create = DSL.using(DodexUtil.getSqlDialect(), settings);
        // qmark? setupAllUsers().replaceAll("\\$\\d", "?"): setupAllUsers();
        GETLOGINBYNP = setupLoginByNamePassword().replaceAll("\\$\\d", "?");
        GETLOGINBYNAME = qmark ? setupLoginByName().replaceAll("\\$\\d", "?") : setupLoginByName();
        GETINSERTLOGIN = qmark ? setupInsertLogin().replaceAll("\\$\\d", "?") : setupInsertLogin();
        GETREMOVELOGIN = setupRemoveLogin().replaceAll("\\$\\d", "?");
        GETLOGINBYID = qmark ? setupLoginById().replaceAll("\\$\\d", "?") : setupLoginById();
        GETUPDATELOGIN = qmark ? setupUpdateLogin().replaceAll("\\$\\d*", "?") : setupUpdateLogin();
        GETSQLITEUPDATELOGIN = qmark ? setupSqliteUpdateLogin().replaceAll("\\$\\d*", "?")
                : setupSqliteUpdateLogin();
    }

    private static String setupLoginByNamePassword() {
        return create.renderNamedParams(
                select(field("ID"), field("NAME"), field("PASSWORD"), field("LAST_LOGIN"))
                        .from(table("login")).where(field("NAME").eq("$"))
                        .and(field("PASSWORD").eq("$")));
    }

    public String getLoginByNamePassword() {
        return GETLOGINBYNP;
    }

    private static String setupLoginByName() {
        return create.renderNamedParams(
                select(field("ID"), field("NAME"), field("PASSWORD"), field("LAST_LOGIN"))
                        .from(table("login")).where(field("NAME").eq("$")));
    }

    public String getUserByName() {
        return GETLOGINBYNAME;
    }

    private static String setupLoginById() {
        return create.renderNamedParams(
                select(field("ID"), field("NAME"), field("PASSWORD"), field("LAST_LOGIN"))
                        .from(table("login")).where(field("NAME").eq("$")));
    }

    public String getUserById() {
        return GETLOGINBYID;
    }

    private static String setupInsertLogin() {
        return create.renderNamedParams(insertInto(table("login"))
                .columns(field("NAME"), field("PASSWORD"), field("LAST_LOGIN"))
                .values("$", "$", "$").returning(field("ID")));
    }

    public String getInsertLogin() {
        return GETINSERTLOGIN;
    }

    private static String setupUpdateLogin() {
        return create.renderNamedParams(update(table("login")).set(field("LAST_LOGIN"), "$")
                .where(field("ID").eq("$")).returning());
    }

    public String getUpdateLogin() {
        return GETUPDATELOGIN;
    }

    public static String setupSqliteUpdateLogin() {
        return "update login set last_login = $ where id = $";
    }

    public String getSqliteUpdateLogin() {
        return GETSQLITEUPDATELOGIN;
    }

    private static String setupRemoveLogin() {
        return create.renderNamedParams(
                deleteFrom(table("login")).where(field("NAME").eq("$"), field("PASSWORD").eq("$")));
    }

    public String getRemoveLogin() {
        return GETREMOVELOGIN;
    }

    public abstract SpaLogin createSpaLogin();

    public Future<SpaLogin> getLogin(SpaLogin spaLogin) {
        Promise<SpaLogin> promise = Promise.promise();

        SpaLogin resultLogin = createSpaLogin();
        resultLogin.setStatus("0");

        pool.rxGetConnection().doOnSuccess(conn -> {

            conn.query(create
                    .query(getLoginByNamePassword(), spaLogin.getName(), spaLogin.getPassword())
                    .toString()).rxExecute().doOnSuccess(rows -> {
                        if (rows.size() == 0) {
                            if (!(spaLogin.getPassword().equals(resultLogin.getPassword()))) {
                                resultLogin.setStatus("-1");
                                resultLogin.setId(0l);
                                resultLogin.setName(spaLogin.getName());
                                resultLogin.setPassword(spaLogin.getPassword());
                                resultLogin.setLastLogin(new Date());
                            } else {
                                resultLogin.setStatus("0");
                            }
                        } else {
                            for (Row row : rows) {
                                try {
                                    resultLogin.setId(row.getLong(0));
                                    resultLogin.setName(row.getString(1));
                                    resultLogin.setPassword(row.getString(2));
                                    resultLogin.setLastLogin(row.getValue(3));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        if (rows.size() > 0 && "0".equals(resultLogin.getStatus())) {
                            conn.close();
                            Future<Integer> future = null;
                            try {
                                future = updateCustomLogin(resultLogin, "date");
                            } catch (/* InterruptedException | SQLException */ Exception e) {
                                e.printStackTrace();
                            }
                            future.onSuccess(result2 -> {
                                resultLogin.setStatus("0");
                                promise.complete(resultLogin);
                            });

                            future.onFailure(failed -> {
                                logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT,
                                        "Add Login Update failed...: ", failed.getMessage(),
                                        ColorUtilConstants.RESET));
                                resultLogin.setStatus("-99");
                                promise.complete(resultLogin);
                            });
                        } else {
                            conn.close();
                            promise.complete(resultLogin);
                        }
                    }).doOnError(err -> {
                        conn.close();
                        resultLogin.setStatus("-99");
                        promise.complete(resultLogin);
                        logger.error(String.format("%sError retrieving user: %s -- %s%s",
                                ColorUtilConstants.RED, spaLogin.getName(),
                                err.getCause().getMessage(), ColorUtilConstants.RESET));
                    }).subscribe();
        }).subscribe();

        return promise.future();
    }

    public Future<SpaLogin> addLogin(SpaLogin spaLogin) {
        Promise<SpaLogin> promise = Promise.promise();
        Timestamp current = new Timestamp(new Date().getTime());
        OffsetDateTime time = OffsetDateTime.now();

        Object lastLogin = DbConfiguration.isUsingPostgres() ? time : current;

        spaLogin.setStatus("0");

        pool.rxGetConnection().doOnSuccess(conn -> {
            Tuple parameters = Tuple.of(spaLogin.getName(), spaLogin.getPassword(), lastLogin);

            conn.preparedQuery(getInsertLogin()).rxExecute(parameters).doOnSuccess(rows -> {
                for (Row row : rows) {
                    spaLogin.setId(row.getLong(0));
                    spaLogin.setLastLogin(current);
                }

                conn.close();
                promise.complete(spaLogin);
            }).doOnError(err -> {
                conn.close();
                spaLogin.setStatus("-4");
                logger.error(String.format("%sError adding login: %s%s", ColorUtilConstants.RED,
                        err, ColorUtilConstants.RESET));
            }).subscribe(rows -> {
                //
            }, err -> {
                conn.close();
                spaLogin.setStatus("-4");
                promise.complete(spaLogin);
                logger.error(String.format("%sError adding login: %s%s", ColorUtilConstants.RED,
                        err, ColorUtilConstants.RESET));
            });
        }).subscribe();

        return promise.future();
    }

    public Future<SpaLogin> removeLogin(SpaLogin spaLogin) {
        Promise<SpaLogin> promise = Promise.promise();


        pool.rxGetConnection().doOnSuccess(conn -> {
            String query = create
                    .query(getRemoveLogin(), spaLogin.getName(), spaLogin.getPassword()).toString();

            conn.query(query).rxExecute().doOnSuccess(rows -> {
                for (Row row : rows) {
                    spaLogin.setId(row.getLong(0));
                }
                Integer count = Integer.valueOf(rows.rowCount());

                spaLogin.setStatus(count.toString());
                if (spaLogin.getId() == null) {
                    spaLogin.setId(-1l);
                }
                if (spaLogin.getLastLogin() == null) {
                    spaLogin.setLastLogin(new Date());
                }
                promise.complete(spaLogin);

                conn.close();
            }).doOnError(err -> {
                logger.error(String.format("%sError deleting login: %s%s", ColorUtilConstants.RED,
                        err, ColorUtilConstants.RESET));
                spaLogin.setStatus("-4");
                promise.complete(spaLogin);
            }).subscribe(rows -> {

            }, err -> {
                spaLogin.setStatus("-4");
                promise.complete(spaLogin);
                err.printStackTrace();
            });
        }).subscribe();

        return promise.future();
    }

    public Future<Integer> updateCustomLogin(SpaLogin spaLogin, String type) {
        Promise<Integer> promise = Promise.promise();

        pool.rxGetConnection().doOnSuccess(conn -> {
            Timestamp timeStamp = new Timestamp(new Date().getTime());
            OffsetDateTime time = OffsetDateTime.now();
            long date = new Date().getTime();
            String sql = getUpdateLogin();
            LocalDateTime lTime = LocalDateTime.now();

            Object dateTime = DbConfiguration.isUsingIbmDB2() || DbConfiguration.isUsingMariadb() ? lTime
                    : DbConfiguration.isUsingSqlite3() ? date
                            : DbConfiguration.isUsingCubrid() ? timeStamp : time;

            spaLogin.setLastLogin(timeStamp);

            if (DbConfiguration.isUsingCubrid()) {
                // Cubrid fails with NullPointer using "preparedQuery"
                String query =
                        create.query(getSqliteUpdateLogin(), dateTime, spaLogin.getId()).toString();

                conn.query(query).rxExecute().doOnSuccess(rows -> {
                    conn.close();
                    promise.complete(rows.rowCount());
                }).doOnError(err -> {
                    logger.error(String.format("%sError Spa Updating login: %s%s",
                            ColorUtilConstants.RED, err, ColorUtilConstants.RESET));
                }).subscribe(rows -> {
                    //
                }, err -> {
                    logger.error(String.format("%sError Spa Updating login: %s%s",
                            ColorUtilConstants.RED, err, ColorUtilConstants.RESET));
                    err.printStackTrace();
                    conn.close();
                    promise.complete(-99);
                    err.printStackTrace();
                });
            } else {
                conn.preparedQuery(sql).rxExecute(Tuple.of(dateTime, spaLogin.getId()))
                        .doOnSuccess(rows -> {
                            conn.close();
                            promise.complete(rows.rowCount());

                        }).doOnError(err -> {
                            logger.error(String.format("%sError Updating login: %s%s",
                                    ColorUtilConstants.RED, err, ColorUtilConstants.RESET));
                            // conn.close();
                        }).subscribe(rows -> {
                            //
                        }, err -> {
                            logger.error(String.format("%sError Updating login: %s%s",
                                    ColorUtilConstants.RED, err, ColorUtilConstants.RESET));
                            err.printStackTrace();
                            conn.close();
                            promise.complete(-99);
                            err.printStackTrace();
                        });
            }
        }).subscribe();

        return promise.future();
    }

    public void setIsTimestamp(Boolean isTimestamp) {
        this.isTimestamp = isTimestamp;
    }

    public Boolean getIsTimestamp() {
        return isTimestamp;
    }
}
