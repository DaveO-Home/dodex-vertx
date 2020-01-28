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
import dmo.fs.utils.ConsoleColors;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Optional auto user cleanup - config in "application-conf.json". When client
 * changes handle when server is down, old users and undelivered messages will
 * be orphaned.
 * 
 * Defaults: off - when turned on 1. execute on start up and every 7 days
 * thereafter. 2. remove users who have not logged in for 90 days.
 */
public class CleanOrphanedUsers extends DbDefinitionBase {
    private static Logger logger = LoggerFactory.getLogger(CleanOrphanedUsers.class.getName());

    private static DodexDatabase dodexDatabase = null;
    private static Database db = null;
    private static Integer age = null;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Runnable clean = new Runnable() {
        public void run() {
            runClean();
        }
    };

    public CleanOrphanedUsers() {
    }

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
        Flowable.fromCallable(() -> {
            List<Tuple5<Integer, String, String, String, Object>> users = getUsers(db);
            List<Integer> possibleUsers = getPossibleOrphanedUsers(users);
            List<String> names = new ArrayList<>();

            cleanUsers(db, possibleUsers);

            users.iterator().forEachRemaining(user -> {
                if (possibleUsers.contains(user.value1())) {
                    names.add(user.value2());
                }
            });

            return ConsoleColors.BLUE_BOLD_BRIGHT + "Cleaned users: " + names.toString() + ConsoleColors.RESET;
        }).subscribeOn(Schedulers.io()).observeOn(Schedulers.single()).subscribe(logger::info,
                Throwable::printStackTrace);
    }

    private List<Tuple5<Integer, String, String, String, Object>> getUsers(Database db) throws SQLException {
        List<Tuple5<Integer, String, String, String, Object>> listOfUsers = new ArrayList<>();

        Disposable disposable = db.select(getAllUsers()).parameter("NAME", "DUMMY")
                .getAs(Integer.class, String.class, String.class, String.class, Object.class).doOnNext(result -> {
                    listOfUsers.add(result);
                }).subscribe(result -> {
                    //
                }, throwable -> {
                    logger.error("{0}Error building registered user list{1}",
                            new Object[] { ConsoleColors.RED_BOLD_BRIGHT, ConsoleColors.RESET });
                    throwable.printStackTrace();
                });

        await(disposable);
        return listOfUsers;
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
        diffInDays = (diff / (1000 * 60 * 60 * 24));
        return diffInDays;
    }

    private void cleanUsers(Database db, List<Integer> users) {
        List<Integer> messageIds = new ArrayList<>();

        users.iterator().forEachRemaining(userId -> {

            Disposable disposable = db.select(getUserUndelivered()).parameter("USERID", userId)
                    .getAs(Integer.class, Integer.class).doOnEach(result -> {
                        if (result.isOnNext()) {
                            Integer messageId = result.getValue().value2();
                            messageIds.add(messageId);
                        }
                    }).subscribe(result -> {
                        //
                    }, throwable -> {
                        logger.error("{0}Error cleaning user list{1}",
                                new Object[] { ConsoleColors.RED_BOLD_BRIGHT, ConsoleColors.RESET });
                        throwable.printStackTrace();
                    });

            await(disposable);
            cleanUndelivered(db, userId, messageIds);
            cleanMessage(db, messageIds);
            cleanRemainingUsers(db, users);
        });
    }

    private int cleanUndelivered(Database db, Integer userId, List<Integer> messageIds) {
        int count[] = { 0 };
        messageIds.iterator().forEachRemaining(messageId -> {
            Disposable disposable = db.update(getRemoveUndelivered()).parameter("USERID", userId)
                    .parameter("MESSAGEID", messageId).counts().doOnNext(c -> count[0] += c).subscribe(result -> {
                        //
                    }, throwable -> {
                        logger.error("{0}Error removing undelivered record{1}",
                                new Object[] { ConsoleColors.RED, ConsoleColors.RESET });
                        throwable.printStackTrace();
                    });

            await(disposable);
        });
        return count[0];
    }

    private int cleanMessage(Database db, List<Integer> messageIds) {
        int count[] = { 0 };
        messageIds.iterator().forEachRemaining(messageId -> {
            Disposable disposable = db.update(getRemoveMessage()).parameter("MESSAGEID", messageId).counts()
                    .doOnNext(c -> count[0] += c).subscribe(result -> {
                        //
                    }, throwable -> {
                        logger.error("{0}Error removing message: {2}{1}",
                                new Object[] { ConsoleColors.RED, ConsoleColors.RESET, messageId });
                        throwable.printStackTrace();
                    });

            await(disposable);
        });
        return count[0];
    }

    private int cleanUser(Database db, Integer userId) {
        int count[] = { 0 };

        Disposable disposable = db.update(getRemoveUsers()).parameter("USERID", userId).counts()
                .doOnNext(c -> count[0] += c).subscribe(result -> {
                    //
                }, throwable -> {
                    logger.error("{0}Error deleting user{1} {2}",
                            new Object[] { ConsoleColors.RED, ConsoleColors.RESET, userId });
                    throwable.printStackTrace();
                });

        await(disposable);
        return count[0];
    }

    private int cleanRemainingUsers(Database db, List<Integer> users) {
        int count = 0;

        users.iterator().forEachRemaining(userId -> {
            cleanUser(db, userId);
        });

        return count;
    }

    private static void await(Disposable disposable) {
        while (!disposable.isDisposed()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public MessageUser createMessageUser() {
        return null;
    }
}