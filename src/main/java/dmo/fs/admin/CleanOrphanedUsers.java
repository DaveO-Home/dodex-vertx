package dmo.fs.admin;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.davidmoten.rx.jdbc.Database;
import org.davidmoten.rx.jdbc.tuple.Tuple5;

import dmo.fs.db.DbConfiguration;
import dmo.fs.db.DbDefinitionBase;
import dmo.fs.db.DodexDatabase;
import dmo.fs.db.MessageUser;
import dmo.fs.utils.ColorUtilConstants;
import io.reactivex.Flowable;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Optional auto user cleanup - config in "application-conf.json". When client
 * changes handle when server is down, old users and undelivered messages will
 * be orphaned.
 * 
 * Defaults: off - when turned on 
 * 1. execute on start up and every 7 days thereafter. 
 * 2. remove users who have not logged in for 90 days.
 */
public class CleanOrphanedUsers extends DbDefinitionBase {
    private static Logger logger = LoggerFactory.getLogger(CleanOrphanedUsers.class.getName());

    private static DodexDatabase dodexDatabase;
    private static Database db;
    private static Integer age;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Runnable clean = new Runnable() {
        @Override
        public void run() {
            runClean();
        }
    };

    public void startClean(JsonObject config) throws InterruptedException, IOException, SQLException {
        long delay = 0;
        long period = 0;
        dodexDatabase = DbConfiguration.getDefaultDb();
        db = dodexDatabase.getDatabase();
        setupSql(db);
        delay = config.getLong("clean.delay");
        period = config.getLong("clean.period");
        age = config.getInteger("clean.age");
        scheduler.scheduleAtFixedRate(clean, delay, period, TimeUnit.DAYS);
    }

    private void runClean() {
        List<String> names = new ArrayList<>();
        Flowable.fromCallable(() -> {
            Future<List<Tuple5<Integer, String, String, String, Object>>> users = getUsers(db);
            users.onSuccess(data -> {
                List<Integer> possibleUsers = getPossibleOrphanedUsers(data);

                cleanUsers(db, possibleUsers);

                data.iterator().forEachRemaining(user -> {
                    if (possibleUsers.contains(user.value1())) {
                        names.add(user.value2());
                    }
                });

                logger.info(String.join("", ColorUtilConstants.BLUE_BOLD_BRIGHT, "Cleaned users: ", names.toString(), ColorUtilConstants.RESET));
            });

            return String.join("", ColorUtilConstants.BLUE_BOLD_BRIGHT, "Starting User/Undelivered/Message Clean: ", ColorUtilConstants.RESET);
        
        }).subscribeOn(Schedulers.io()).observeOn(Schedulers.single()).subscribe(logger::info,
                Throwable::printStackTrace);
    }

    private Future<List<Tuple5<Integer, String, String, String, Object>>> getUsers(Database db) throws SQLException {
        List<Tuple5<Integer, String, String, String, Object>> listOfUsers = new ArrayList<>();
        Promise<List<Tuple5<Integer, String, String, String, Object>>> promise = Promise.promise();
        GotUsers gotUsers = new GotUsers();
		
        gotUsers.setPromise(promise);
        gotUsers.setListOfUsers(listOfUsers);
        
        db.select(getAllUsers()).parameter("NAME", "DUMMY")
            .getAs(Integer.class, String.class, String.class, String.class, Object.class)
            .doOnNext(result -> {
                listOfUsers.add(result);
            })
            .doOnComplete(gotUsers)
            .subscribe(result -> {
                //
            }, throwable -> {
                logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT, "Error building registered user list",  ColorUtilConstants.RESET));
                throwable.printStackTrace();
            });
            
        return gotUsers.getPromise().future();
    }

    private static List<Integer> getPossibleOrphanedUsers(List<Tuple5<Integer, String, String, String, Object>> users) {
        List<Integer> orphaned = new ArrayList<>();

        users.iterator().forEachRemaining(user -> {
            Long days = getLastLogin(user.value5());

            if (days >= age) {
                orphaned.add(user.value1());
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
    private void cleanUsers(Database db, List<Integer> users) {
        List<Integer> messageIds = new ArrayList<>();        

        users.iterator().forEachRemaining(userId -> {
            CleanObjects cleanObjects = new CleanObjects();

            Future.future(prom -> {
                cleanObjects.setPromise(prom);
                db.select(getUserUndelivered())
                    .parameter("USERID", userId)
                    .getAs(Integer.class, Integer.class)
                    .doOnEach(result -> {
                        if (result.isOnNext()) {
                            Integer messageId = result.getValue().value2();
                            messageIds.add(messageId);
                        }
                    })
                    .doOnComplete(cleanObjects)
                    .subscribe(result -> {
                    }, throwable -> {
                        logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT, "Error cleaning user list: ", throwable.getMessage(), ColorUtilConstants.RESET ));
                    });
                
                prom.future().onSuccess(result -> {
                    cleanUndelivered(db, userId, messageIds, users);
                    value = result;
                });
                
            });
            
        });
        if(value == null) {
            cleanRemainingUsers(db, users);
        }
    }

    private int cleanUndelivered(Database db, Integer userId, List<Integer> messageIds, List<Integer> users) {
        int count[] = { 0 };
        messageIds.iterator().forEachRemaining(messageId -> {
            CleanObjects cleanObjects = new CleanObjects();

            Future.future(prom -> {
                cleanObjects.setPromise(prom);
                db.update(getRemoveUndelivered())
                    .parameter("USERID", userId)
                    .parameter("MESSAGEID", messageId)
                    .counts()
                    .doOnNext(c -> { 
                        count[0] += c;
                    })
                    .doOnComplete(cleanObjects)
                    .subscribe(result -> {
                        cleanObjects.setCount(count[0]);
                    }, throwable -> {
                        logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT, "Error removing undelivered record: ", throwable.getMessage(), ColorUtilConstants.RESET ));
                    });
                
                prom.future().onSuccess(result -> {
                    if(result != null) {
                        count[0] += (Integer)result;
                    }
                    cleanMessage(db, messageIds, users);
                });
            });
        });
        return count[0];
    }

    private int cleanMessage(Database db, List<Integer> messageIds, List<Integer> users) {
        int count[] = { 0 };
        messageIds.iterator().forEachRemaining(messageId -> {
            CleanObjects cleanObjects = new CleanObjects();

            Future.future(prom -> {
                cleanObjects.setPromise(prom);
                db.update(getRemoveMessage())
                    .parameter("MESSAGEID", messageId)
                    .counts()
                    .doOnNext(c -> count[0] += c)
                    .doOnComplete(cleanObjects)
                    .subscribe(result -> {
                        cleanObjects.setCount(count[0]);
                    }, throwable -> {
                        logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT, " Error removing message:", throwable.getMessage(), ColorUtilConstants.RESET ));
                    });
                prom.future().onSuccess(result -> {
                    if(result != null) {
                        count[0] += (Integer)result;
                    }
                    cleanRemainingUsers(db, users);
                });
            });
        });
        return count[0];
    }

    private Future<Object> cleanUser(Database db, Integer userId) {
        CleanObjects cleanObjects = new CleanObjects();

        return Future.future(prom -> {
            cleanObjects.setPromise(prom);
            db.update(getRemoveUsers())
                .parameter("USERID", userId)
                .counts()
                .doOnNext(c -> cleanObjects.setCount(cleanObjects.getCount() + c))
                .doOnComplete(cleanObjects)
                .subscribe(result -> {
                    //
                }, throwable -> {
                    logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT, ":Error deleting user: ",  userId.toString(), " : ", throwable.getMessage(), ColorUtilConstants.RESET ));
                });
        });
    }

    private int cleanRemainingUsers(Database db, List<Integer> users) {
        int count[] = { 0 };

        users.iterator().forEachRemaining(userId -> {
            cleanUser(db, userId).onSuccess(result -> {
                if(result != null) {
                    count[0] += (Integer)result;
                }
            });
        });

        return count[0];
    }

    @Override
    public MessageUser createMessageUser() {
        return null;
    }

    class GotUsers implements Action {
		List<Tuple5<Integer, String, String, String, Object>> listOfUsers;
		Promise<List<Tuple5<Integer, String, String, String, Object>>> promise;

		@Override
		public void run() throws Exception {
            promise.complete(listOfUsers);
        }
        
        public void setPromise(Promise<List<Tuple5<Integer, String, String, String, Object>>> promise) {
            this.promise = promise;
        }

        public Promise<List<Tuple5<Integer, String, String, String, Object>>> getPromise() {
            return promise;
        }

        public void setListOfUsers(List<Tuple5<Integer, String, String, String, Object>> listOfUsers) {
            this.listOfUsers = listOfUsers;
        }
    }

    class CleanObjects implements Action {
		Object object;
        Promise <Object> promise;
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
