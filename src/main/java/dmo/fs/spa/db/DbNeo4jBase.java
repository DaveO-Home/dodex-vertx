
package dmo.fs.spa.db;

import java.sql.Timestamp;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.neo4j.driver.Driver;
import org.neo4j.driver.reactive.RxResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dmo.fs.spa.utils.SpaLogin;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Future;
import io.vertx.rxjava3.core.Promise;

public abstract class DbNeo4jBase {
    private static final Logger logger = LoggerFactory.getLogger(DbNeo4jBase.class.getName());
    private Driver driver;

    public Future<SpaLogin> getLogin(SpaLogin spaLogin) throws InterruptedException, ExecutionException {
        Promise<SpaLogin> promise = Promise.promise();
        spaLogin.setStatus("0");

        Map<String, Object> params = new ConcurrentHashMap<>();
        params.put("name", spaLogin.getName());
        params.put("password", spaLogin.getPassword());
        spaLogin.setLastLogin(new Timestamp(System.currentTimeMillis()));

        Uni.createFrom().item(driver::rxSession).call(session -> {
            RxResult result = session.run(
                    "MATCH (l:Login) where l.name = $name and l.password = $password RETURN count(*) as count;",
                    params);
            Uni.createFrom().publisher(result.records()).call(record -> {
                int count = record.get("count").asInt();
                session.close();
                if (count == 0) {
                    spaLogin.setStatus("-1");
                } else {
                    try {
                        updateLogin(spaLogin);
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
                promise.complete(spaLogin);
                return Uni.createFrom().publisher(session.close());
            }).onFailure().invoke(Throwable::printStackTrace).subscribeAsCompletionStage();
            return Uni.createFrom().item(session);
        }).onFailure().invoke(Throwable::printStackTrace).subscribeAsCompletionStage();

        return promise.future();
    }

    public Future<SpaLogin> addLogin(SpaLogin spaLogin) throws InterruptedException, ExecutionException {
        Promise<SpaLogin> promise = Promise.promise();
        Map<String, Object> params = new ConcurrentHashMap<>();

        params.put("name", spaLogin.getName());
        params.put("password", spaLogin.getPassword());
        params.put("zone", TimeZone.getDefault().getID());
        spaLogin.setLastLogin(new Timestamp(System.currentTimeMillis()));
        spaLogin.setStatus("0");

        Multi.createFrom().resource(driver::rxSession,
                session -> session.writeTransaction(tx -> {
                    RxResult results = tx.run(
                            "create (l:Login {name: $name, password: $password, lastLogin: datetime({timezone:$zone})});",
                            params);
                    tx.commit();
                    return Multi.createFrom().publisher(results.records()).onItem().transform(record -> "add");
                }))
                .withFinalizer(session -> {
                    promise.complete(spaLogin);
                    return Uni.createFrom().publisher(session.close());
                })
                .onFailure().invoke(t -> {
                    spaLogin.setStatus("-4");
                    promise.complete(spaLogin);
                    t.printStackTrace();
                })
                .subscribe().asStream();

        return promise.future();
    }

    private SpaLogin updateLogin(SpaLogin spaLogin) // DocumentReference docRef)
            throws InterruptedException, ExecutionException {
        Map<String, Object> params = new ConcurrentHashMap<>();
        params.put("name", spaLogin.getName());
        params.put("password", spaLogin.getPassword());
        params.put("zone", TimeZone.getDefault().getID());
        spaLogin.setLastLogin(new Timestamp(System.currentTimeMillis()));

        Multi.createFrom().resource(driver::rxSession,
                session -> session.writeTransaction(tx -> {
                    RxResult results = tx.run(
                            "MATCH (l:Login {name: $name, password: $password}) SET l.lastLogin = dateTime({timezone:$zone});",
                            params);
                    tx.commit();
                    return Multi.createFrom().publisher(results.records()).onItem().transform(record -> "update");
                }))
                .withFinalizer(session -> {
                    return Uni.createFrom().publisher(session.close());
                })
                .onFailure().invoke(Throwable::printStackTrace)
                .subscribe().asStream();

        return spaLogin;
    }

    public Future<SpaLogin> removeLogin(SpaLogin spaLogin) throws InterruptedException, ExecutionException {
        // app does not remove logins - used for testing
        Promise<SpaLogin> promise = Promise.promise();
        if (spaLogin.getId() == null) {
            spaLogin.setId("");
        }
        spaLogin.setLastLogin(new Timestamp(System.currentTimeMillis()));
        spaLogin.setStatus("1");
        Map<String, Object> params = new ConcurrentHashMap<>();
        params.put("name", spaLogin.getName());

        Multi.createFrom().resource(driver::rxSession,
                session -> session.writeTransaction(tx -> {
                    RxResult results = tx.run("MATCH (l:Login) where l.name = $name delete l;", params);
                    tx.commit();
                    return Multi.createFrom().publisher(results.records()).onItem().transform(record -> "delete");
                }))
                .withFinalizer(session -> {
                    promise.complete(spaLogin);
                    return Uni.createFrom().publisher(session.close());
                })
                .onFailure().invoke(Throwable::printStackTrace)
                .subscribe().asStream();

        return promise.future();
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }
}
