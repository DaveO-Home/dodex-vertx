package dmo.fs.admin;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import dmo.fs.db.*;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dmo.fs.utils.ColorUtilConstants;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.sqlclient.Pool;
import io.vertx.rxjava3.sqlclient.Row;

/**
 * Optional auto user cleanup - config in "application-conf.json". When client changes handle when
 * server is down, old users and undelivered messages will be orphaned.
 * 
 * Defaults: off - when turned on 1. execute on start up and every 7 days thereafter. 2. remove
 * users who have not logged in for 90 days.
 */
public class CleanOrphanedUsers extends DbDefinitionBase {
    private static Logger logger = LoggerFactory.getLogger(CleanOrphanedUsers.class.getName());

    private static DodexDatabase dodexDatabase;
    private DSLContext create;
    private Pool pool;
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
        dodexDatabase = DbConfiguration.getDefaultDb();

        pool = dodexDatabase.getPool4();
        setupSql(dodexDatabase.getPool4());
        create = DbDefinitionBase.getCreate();

        delay = config.getLong("clean.delay");
        period = config.getLong("clean.period");
        age = config.getInteger("clean.age");
        scheduler.scheduleAtFixedRate(clean, delay, period, TimeUnit.DAYS);
    }

    private void runClean() {
        Set<String> names = new HashSet<>();
        Flowable.fromCallable(() -> {
            Future<Set<MessageUser>> users = getUsers(pool);
            users.onSuccess(data -> {
                Set<Long> possibleUsers = getPossibleOrphanedUsers(data);

                cleanUsers(pool, possibleUsers);

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

    private Future<Set<MessageUser>> getUsers(Pool pool) throws SQLException {
        Set<MessageUser> listOfUsers = new HashSet<>();
        Promise<Set<MessageUser>> promise = Promise.promise();
        GotUsers gotUsers = new GotUsers();

        gotUsers.setPromise(promise);
        gotUsers.setListOfUsers(listOfUsers);

        String query = create.query(getAllUsers().replaceAll("\\$\\d", "?"), "DUMMY").toString();

        pool.query(query).rxExecute().doOnSuccess(rows -> {
            for (Row row : rows) {
                MessageUser messageUser = createMessageUser();
                messageUser.setId(row.getLong(0));
                messageUser.setName(row.getString(1));
                messageUser.setPassword(row.getString(2));
                messageUser.setIp(row.getString(3));
                messageUser.setLastLogin(row.getValue(4));
                listOfUsers.add(messageUser);
            }
        }).doFinally(gotUsers).subscribe(rows -> {
            //
        }, err -> {
            logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT,
                    "Error building registered user list", ColorUtilConstants.RESET));
            err.printStackTrace();
        });

        return gotUsers.getPromise().future();
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
        Long currentDate = new Date().getTime();
        Long diffInDays = 0L;
        Long loginDate = 0L;

        if (lastLogin instanceof Date) {
            loginDate = ((Date) lastLogin).getTime();
        } else {
            loginDate = ((Timestamp) lastLogin).getTime();
        }

        long diff = currentDate - loginDate;
        diffInDays = diff / (1000 * 60 * 60 * 24);
        return diffInDays;
    }

    Object value;

    private void cleanUsers(Pool pool, Set<Long> users) {
        Set<Long> messageIds = new HashSet<>();

        users.iterator().forEachRemaining(userId -> {
            CleanObjects cleanObjects = new CleanObjects();

            Future.future(prom -> {
                cleanObjects.setPromise(prom);
                String query = create.query(getUserUndelivered().replaceAll("\\$\\d", "?"), userId)
                        .toString();

                pool.query(query).rxExecute().doOnSuccess(rows -> {
                    for (Row row : rows) {
                        messageIds.add(row.getLong(1));
                    }
                }).doFinally(cleanObjects).subscribe(rows -> {
                    //
                }, err -> {
                    logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT,
                            "Error cleaning user list: ", err.getMessage(),
                            ColorUtilConstants.RESET));
                    err.printStackTrace();
                });

                prom.future().onSuccess(result -> {
                    cleanUndelivered(pool, userId, messageIds, users);
                    value = result;
                });
                if (value == null) {
                    cleanRemainingUsers(pool, users);
                }
            });
        });
    }

    private int cleanUndelivered(Pool pool, Long userId, Set<Long> messageIds, Set<Long> users) {
        int count[] = {0};
        messageIds.iterator().forEachRemaining(messageId -> {
            Future.future(prom -> {
                CleanObjects cleanObjects = new CleanObjects();
                cleanObjects.setPromise(prom);
                String query = create
                        .query(getRemoveUndelivered().replaceAll("\\$\\d", "?"), userId, messageId)
                        .toString();

                pool.query(query).rxExecute().doFinally(cleanObjects).subscribe(rows -> {
                    count[0] = count[0] += rows.rowCount();
                    cleanObjects.setCount(rows.rowCount());
                }, throwable -> {
                    logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT,
                            "Error removing undelivered record: ", throwable.getMessage(),
                            ColorUtilConstants.RESET));
                });

                prom.future().onSuccess(result -> {
                    if (result != null) {
                        count[0] += (Integer) result;
                    }
                    cleanMessage(pool, messageIds, users);
                });
            });
        });

        return count[0];
    }

    private int cleanMessage(Pool pool, Set<Long> messageIds, Set<Long> users) {
        int count[] = {0};
        messageIds.iterator().forEachRemaining(messageId -> {
            CleanObjects cleanObjects = new CleanObjects();

            Future.future(prom -> {
                cleanObjects.setPromise(prom);
                String sql = null;
                if (DbConfiguration.isUsingIbmDB2() || DbConfiguration.isUsingMariadb()
                        || DbConfiguration.isUsingSqlite3()) {
                    sql = getCustomDeleteMessages();
                } else {
                    sql = getRemoveMessage();
                }
                String query = create.query(sql.replaceAll("\\$\\d", "?"), messageId, messageId)
                        .toString();

                pool.query(query).rxExecute().doFinally(cleanObjects).subscribe(rows -> {
                    count[0] = count[0] += rows.rowCount();
                    cleanObjects.setCount(rows.rowCount());
                }, throwable -> {
                    logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT,
                            "Error removing message record: ", throwable.getMessage(),
                            ColorUtilConstants.RESET));
                });

                prom.future().onSuccess(result -> {
                    if (result != null) {
                        count[0] += (Integer) result;
                    }
                    cleanRemainingUsers(pool, users);
                });
            });

        });

        return count[0];
    }

    private Future<Object> cleanUser(Pool pool, Long userId) {
        CleanObjects cleanObjects = new CleanObjects();

        return Future.future(prom -> {
            cleanObjects.setPromise(prom);
            String sql = null;

            if (DbConfiguration.isUsingIbmDB2() || DbConfiguration.isUsingMariadb()
                    || DbConfiguration.isUsingSqlite3()) {
                sql = getCustomDeleteUsers();
            } else {
                sql = getRemoveUsers();
            }
            String query = create.query(sql.replaceAll("\\$\\d", "?"), userId, userId).toString();

            pool.query(query).rxExecute().doOnSuccess(rows -> {
                cleanObjects.setCount(cleanObjects.getCount() + rows.rowCount());
            }).doFinally(cleanObjects).subscribe(result -> {
                //
            }, throwable -> {
                logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT,
                        ":Error deleting user: ", userId.toString(), " : ", throwable.getMessage(),
                        ColorUtilConstants.RESET));
            });
        });
    }

    private int cleanRemainingUsers(Pool pool, Set<Long> users) {
        int count[] = {0};

        users.iterator().forEachRemaining(userId -> {
            cleanUser(pool, userId).onSuccess(result -> {
                if (result != null) {
                    count[0] += (Integer) result;
                }
            });
        });

        return count[0];
    }

    @Override
    public MessageUser createMessageUser() {
        return new MessageUserImpl();
    }

    class GotUsers implements Action {
        Set<MessageUser> listOfUsers;
        Promise<Set<MessageUser>> promise;

        @Override
        public void run() throws Exception {
            promise.complete(listOfUsers);
        }

        public void setPromise(Promise<Set<MessageUser>> promise) {
            this.promise = promise;
        }

        public Promise<Set<MessageUser>> getPromise() {
            return promise;
        }

        public void setListOfUsers(Set<MessageUser> listOfUsers) {
            this.listOfUsers = listOfUsers;
        }
    }

    class CleanObjects implements Action {
        Object object;
        Promise<Object> promise;
        Integer count = 0;

        @Override
        public void run() throws Exception {
            promise.complete(count);
        }

        public void setPromise(Promise<Object> promise) {
            this.promise = promise;
        }

        public Promise<Object> getPromise() {
            return promise;
        }

        public void setObject(Object object) {
            this.object = object;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

        public Integer getCount() {
            return count;
        }
    }
}
