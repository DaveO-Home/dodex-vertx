package dmo.fs.admin;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Value;
import org.neo4j.driver.internal.shaded.reactor.core.publisher.Flux;
import org.neo4j.driver.internal.shaded.reactor.core.publisher.Mono;
import org.neo4j.driver.reactive.RxResult;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.reactive.RxTransaction;
import org.neo4j.driver.summary.ResultSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dmo.fs.db.DbNeo4jBase;
import dmo.fs.db.MessageUser;
import dmo.fs.db.MessageUserImpl;
import dmo.fs.router.Neo4jRouter;
import dmo.fs.utils.ColorUtilConstants;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

/**
 * Optional auto user cleanup - config in "application-conf.json". When client changes handle when
 * server is down, old users and undelivered messages will be orphaned.
 * 
 * Defaults: off - when turned on 1. execute on start up and every 7 days thereafter. 2. remove
 * users who have not logged in for 90 days.
 */
public class CleanOrphanedUsersNeo4j extends DbNeo4jBase {
    private static Logger logger = LoggerFactory.getLogger(CleanOrphanedUsers.class.getName());

    private Driver driver;
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

        driver = Neo4jRouter.getDriver();

        delay = config.getLong("clean.delay");
        period = config.getLong("clean.period");
        age = config.getInteger("clean.age");

        scheduler.scheduleAtFixedRate(clean, delay, period, TimeUnit.DAYS);
    }

    private void runClean() {
        Set<String> names = new HashSet<>();
        Flowable.fromCallable(() -> {
            Future<Set<MessageUser>> users = getUsers();

            users.onSuccess(data -> {
                Set<String> possibleUsers = getPossibleOrphanedUsers(data);

                if(logger.isDebugEnabled()) {
                    logger.info("{}", possibleUsers);
                }

                cleanUsers(possibleUsers);

                data.iterator().forEachRemaining(user -> {
                    if (possibleUsers.contains(user.getName())) {
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

    private Future<Set<MessageUser>> getUsers() throws SQLException {
        Set<MessageUser> listOfUsers = new HashSet<>();
        Promise<Set<MessageUser>> promise = Promise.promise();
        GotUsers gotUsers = new GotUsers();
        
        gotUsers.setPromise(promise);
        gotUsers.setListOfUsers(listOfUsers);
        String query = "MATCH (u:User) RETURN u";

        Flux.usingWhen(
            Mono.fromSupplier(driver::rxSession),
            rxSession -> rxSession.readTransaction(tx -> {
                RxResult result = tx.run(query);
                Mono<ResultSummary> mono = Flux.from(result.records())
                    .doOnNext(record -> {
                        MessageUser messageUser = createMessageUser();
                        Value value = record.get("u");
                        messageUser.setId(0l);
                        messageUser.setName(value.get("name").asString());
                        messageUser.setPassword(value.get("password").asString());
                        messageUser.setIp(value.get("ip").asString());
                        messageUser.setLastLogin(value.get("lastLogin").asZonedDateTime());
                        listOfUsers.add(messageUser);
                        if(logger.isDebugEnabled()) {
                            logger.info("{}--{}", record.get("u").get("lastLogin").asZonedDateTime(), messageUser.toString());
                        }
                    }).doFinally(onFinally -> {
                        try {
                            gotUsers.run();
                            tx.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    })
                    .then(Mono.from(result.consume()));
                return mono;
            }),
            RxSession::close).subscribe();
       
        return gotUsers.getPromise().future();
    }

    private static Set<String> getPossibleOrphanedUsers(Set<MessageUser> users) {
        Set<String> orphaned = new HashSet<>();

        users.iterator().forEachRemaining(user -> {
            Long days = getLastLogin(user.getLastLogin());

            if (days >= age) {
                orphaned.add(user.getName());
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

    private void cleanUsers(Set<String> users) {
        String query = "MATCH (m:Message {user: $user}) DETACH DELETE m;";

        users.iterator().forEachRemaining(userId -> {
            Map<String, Object> parameter = Collections.singletonMap("user", userId);
                
            Flux.usingWhen(
                Mono.fromSupplier(driver::rxSession),
                rxSession -> rxSession.writeTransaction(tx -> {
                    RxResult result = tx.run(query, parameter);
                    Mono<ResultSummary> mono = Flux.from(result.records())
                        .doOnEach(record -> {
                            cleanUser(userId, tx);
                            tx.commit();
                        })
                        .doOnError(Throwable::printStackTrace)
                        .then(Mono.from(result.consume()));
                    return mono;
                }),
                RxSession::close).subscribe();
            });
    }

    private int cleanUser(String userId, RxTransaction tx) {
        String query = "MATCH (u:User) where u.name = $name delete u;";

        Map<String, Object> parameter = Collections.singletonMap("name", userId);
                    
        RxResult result = tx.run(query, parameter);
        Flux.from(result.records())
            .doOnError(Throwable::printStackTrace)
            .then(Mono.from(result.consume())).subscribe();
        return 1;
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
