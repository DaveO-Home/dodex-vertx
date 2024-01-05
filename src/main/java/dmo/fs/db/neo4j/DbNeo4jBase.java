
package dmo.fs.db.neo4j;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import dmo.fs.db.MessageUser;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.reactive.RxResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Promise;
import io.vertx.rxjava3.core.http.ServerWebSocket;

public abstract class DbNeo4jBase {
	private static final Logger logger = LoggerFactory.getLogger(DbNeo4jBase.class.getName());
    private Driver driver;

	public abstract MessageUser createMessageUser();

    public Timestamp addUser(MessageUser messageUser) {
        Map<String, Object> params = new ConcurrentHashMap<>();
        params.put("name", messageUser.getName());
        params.put("password", messageUser.getPassword());
        params.put("ip", messageUser.getIp());
        params.put("zone", TimeZone.getDefault().getID());
        messageUser.setLastLogin(new Timestamp(System.currentTimeMillis()));

        Multi.createFrom().resource(driver::rxSession,
            session -> session.writeTransaction(tx -> {
                RxResult results = tx.run("create (u:User {name: $name, password: $password, ip: $ip, lastLogin: datetime({timezone:$zone})});", params);
                tx.commit();
                return Multi.createFrom().publisher(results.records()).onItem().transform(record -> "add");
            }))
            .withFinalizer(session -> {
                return Uni.createFrom().publisher(session.close());
            })
            .onFailure().invoke(Throwable::printStackTrace)
            .subscribe().asStream();

        return messageUser.getLastLogin();
    }

	public MessageUser updateUser(MessageUser messageUser) {
        Map<String,Object> params = new ConcurrentHashMap<>();
        params.put("name", messageUser.getName());
        params.put("password", messageUser.getPassword());
        params.put("zone", TimeZone.getDefault().getID());
        messageUser.setLastLogin(new Timestamp(System.currentTimeMillis()));
        
        Multi.createFrom().resource(driver::rxSession,
            session -> session.writeTransaction(tx -> {
                RxResult results = tx.run("MATCH (u:User {name: $name, password: $password}) SET u.lastLogin = dateTime({timezone:$zone});", params);
                tx.commit();
                return Multi.createFrom().publisher(results.records()).onItem().transform(record -> "update");
            }))
            .withFinalizer(session -> {
                return Uni.createFrom().publisher(session.close());
            })
            .onFailure().invoke(Throwable::printStackTrace)
            .subscribe().asStream();
	    
        return messageUser;
	}
    
	public Promise<Map<String, Integer>> processUserMessages(ServerWebSocket ws, MessageUser messageUser)
			throws Exception {
    	Promise<Map<String, Integer>> promise = Promise.promise();
        Map<String, Integer> counts = new ConcurrentHashMap<>();
        Map<String, Object> params = new HashMap<>();
        params.put("user", messageUser.getName());
        messageUser.setLastLogin(new Timestamp(System.currentTimeMillis()));
        counts.put("messages", 0);

        Multi.createFrom().resource(driver::rxSession,
			session -> session.readTransaction(tx -> {
				RxResult results = tx.run("match (m:Message), (u:User) where m.user = $user and u.name = $user RETURN m, u;", params);

                return Multi.createFrom().publisher(results.records()).onItem().call(record -> {
                    String fromUser = record.get("m").get("fromHandle").asString();
                    String message = record.get("m").get("message").asString();
                    ZonedDateTime postDate = record.get("m").get("postDate").asZonedDateTime();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd-HH:mm:ss z");
                    ws.writeTextMessage(fromUser +  postDate.format(formatter) + " " + message);
                    Integer count = counts.get("messages") + 1;
                    counts.put("messages", count);
                    return Uni.createFrom().item(session);
                })
                .map(record -> "messages");
			}))
			.withFinalizer(session -> {
				promise.complete(counts);
                removeMessages(messageUser.getName());
				return Uni.createFrom().publisher(session.close());
			})
			.onFailure().invoke(Throwable::printStackTrace)
			.subscribe().asStream();

		return promise;
	}

    public void removeMessages(String user) {
        Map<String, Object> params = new ConcurrentHashMap<>();
        params.put("user", user);

        Multi.createFrom().resource(driver::rxSession,
			session -> session.writeTransaction(tx -> {
                RxResult results = tx.run("MATCH (m:Message {user: $user}) DETACH DELETE m;", params);
                return Multi.createFrom().publisher(results.records()).onItem().call(record -> {
                    return Uni.createFrom().item(session);
                })
                .map(record -> "removed");
			}))
			.withFinalizer(session -> {
				return Uni.createFrom().publisher(session.close());
			})
			.onFailure().invoke(Throwable::printStackTrace)
			.subscribe().asStream();
    }

	public Promise<MessageUser> deleteUser(MessageUser messageUser)
			throws InterruptedException, ExecutionException {
        Promise<MessageUser> promise = Promise.promise();
        Map<String,Object> params = new ConcurrentHashMap<>();
        params.put("name", messageUser.getName());
        
        Multi.createFrom().resource(driver::rxSession,
            session -> session.writeTransaction(tx -> {
                RxResult results = tx.run("MATCH (u:User) where u.name = $name delete u;", params);
                tx.commit();
                return Multi.createFrom().publisher(results.records()).onItem().transform(record -> "delete");
            }))
            .withFinalizer(session -> {
                promise.complete(messageUser);
                return Uni.createFrom().publisher(session.close());
            })
            .onFailure().invoke(Throwable::printStackTrace)
            .subscribe().asStream();

		return promise;
	}

    public Promise<MessageUser> addMessage(MessageUser messageUser, String message,
			List<String> undelivered) throws InterruptedException, ExecutionException {
        Promise<MessageUser> promise = Promise.promise();
        Map<String, Object> params = new ConcurrentHashMap<>();
        params.put("message", message);
        params.put("fromHandle", messageUser.getName());
        params.put("zone", TimeZone.getDefault().getID());
        messageUser.setLastLogin(new Timestamp(System.currentTimeMillis()));    
        Uni.createFrom().item(driver::session).call(session -> {
            for(String user : undelivered) {
                params.put("user", user);
                session.run("create (m:Message {user: $user, message: $message, fromHandle: $fromHandle, postDate: datetime({timezone:$zone})});", params);
            }
            session.run("MATCH (u:User) MATCH (m:Message) WHERE m.user IN u.name MERGE (u)-[:Undelivered]->(m);");
            session.close();
            promise.complete(messageUser);
            return Uni.createFrom().item(session);
        })
        .onFailure().invoke(Throwable::printStackTrace).subscribeAsCompletionStage();
        
		return promise;
	}

	public Promise<MessageUser> selectUser(MessageUser messageUser)
			throws InterruptedException, SQLException, ExecutionException {
		Promise<MessageUser> promise = Promise.promise();
        Map<String,Object> params = new ConcurrentHashMap<>();
        
        params.put("name", messageUser.getName());
        params.put("password", messageUser.getPassword());
        Uni.createFrom().item(driver::rxSession).call(session -> {  
            RxResult result = session.run("MATCH (u:User) where u.name = $name and u.password = $password RETURN count(*) as count;", params);
            CompletableFuture<Record> cf = Uni.createFrom().publisher(result.records()).call(record  -> {
                int count = record.get("count").asInt();

                if(count == 0) {
                    addUser(messageUser);
                } else {
                    updateUser(messageUser);
                }
                return Uni.createFrom().item(record);
            }).onFailure().invoke(Throwable::getMessage).subscribeAsCompletionStage();
            // Blocking until finished - just another way
            try {
                cf.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            return Uni.createFrom().item(session);
        }).onFailure().invoke(Throwable::printStackTrace).subscribeAsCompletionStage();

        promise.complete(messageUser);
		return promise;
	}

	public Promise<StringBuilder> buildUsersJson(MessageUser messageUser)
			throws InterruptedException {
        Promise<StringBuilder> promise = Promise.promise();
        JsonArray ja = new JsonArray();
        Map<String,Object> params = new ConcurrentHashMap<>();
        params.put("name", messageUser.getName());
        
        Multi.createFrom().resource(driver::rxSession,
			session -> session.readTransaction(tx -> {
				RxResult results = tx.run("MATCH (u:User) where u.name <> $name RETURN u.name as name;", params);
                return Multi.createFrom().publisher(results.records()).onItem().call(record -> {
                    ja.add(new JsonObject().put("name", record.get("name").asString()));
                    return Uni.createFrom().item(record);
                })
                .map(record -> "name");
			}))
			.withFinalizer(session -> {
				promise.complete(new StringBuilder(ja.toString()));
				return Uni.createFrom().publisher(session.close());
			})
			.onFailure().invoke(Throwable::printStackTrace)
			.subscribe().asStream();
    
        return promise;
	}

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }
}
