
package dmo.fs.spa.db;

import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.modellwerkstatt.javaxbus.ConsumerHandler;
import org.modellwerkstatt.javaxbus.EventBus;
import org.modellwerkstatt.javaxbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dmo.fs.spa.utils.SpaLogin;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.reactivex.core.Vertx;

public abstract class DbCassandraBase {
	private static final Logger logger = LoggerFactory.getLogger(DbCassandraBase.class.getName());
	private Map<String, Promise<SpaLogin>> mUserPromises = new ConcurrentHashMap<>();
	private static final String GETLOGIN = "getlogin";
	private static final String ADDLOGIN = "addlogin";
	private static final String REMOVELOGIN = "removelogin";

	private Vertx vertx;
	private String vertxConsumer = "";
	public Future<SpaLogin> getLogin(SpaLogin spaLogin, EventBus eb) throws InterruptedException {

		if ("".equals(vertxConsumer)) {
			eb.consumer("vertx", setEbConsumer());
			vertxConsumer = "vertx";
		}
		Promise<SpaLogin> promise = Promise.promise();

		mUserPromises.put(spaLogin.getPassword() + GETLOGIN, promise);
		mjson.Json mess = setMessage(GETLOGIN, spaLogin);
		mjson.Json jsonPayLoad = mjson.Json.object().set("msg", mess == null ? "" : mess.getValue()); 

		eb.send("akka", jsonPayLoad);

		return promise.future();
	}

	public Future<SpaLogin> addLogin(SpaLogin spaLogin, EventBus eb) throws InterruptedException {
		Promise<SpaLogin> promise = Promise.promise();

		mUserPromises.put(spaLogin.getPassword() + ADDLOGIN, promise);
		mjson.Json mess = setMessage(ADDLOGIN, spaLogin);
		mjson.Json jsonPayLoad = mjson.Json.object().set("msg", mess == null ? "" : mess.getValue());

		eb.send("akka", jsonPayLoad);

		return promise.future();
	}

	public Future<SpaLogin> removeLogin(SpaLogin spaLogin, EventBus eb) throws InterruptedException {

		Promise<SpaLogin> promise = Promise.promise();

		mUserPromises.put(spaLogin.getPassword() + REMOVELOGIN, promise);
		mjson.Json mess = setMessage(REMOVELOGIN, spaLogin);
		mjson.Json jsonPayLoad = mjson.Json.object().set("msg", mess == null ? "" : mess.getValue());

		eb.send("akka", jsonPayLoad);

		return promise.future();
	}

	private mjson.Json setMessage(String cmd, SpaLogin spaLogin) {
		mjson.Json mess = null;
		try {
			mess = mjson.Json.object()
				.set("cmd", cmd)
				.set("password", spaLogin.getPassword())
				.set("name", spaLogin.getName());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return mess;
	}

	public Vertx getVertx() {
		return vertx;
	}

	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}

	public abstract SpaLogin createSpaLogin();

	public ConsumerHandler setEbConsumer() {
		return new ConsumerHandler() {
			@Override
			public void handle(Message msg) {
				if (!msg.isErrorMsg()) {
					mjson.Json json = msg.getBodyAsMJson();

					switch (json.at("cmd").asString()) {
						case "string":
							String infoMsg = json.at("msg").asString();
							logger.info(infoMsg);
							break;
						case GETLOGIN:
						case ADDLOGIN:
						case REMOVELOGIN:
							mjson.Json cassJson = json.at("msg");
							String cmd = json.at("cmd").asString();
							SpaLogin spaLogin = createSpaLogin();
							spaLogin.setId(0L);
							spaLogin.setPassword(cassJson.at("password").asString());
							spaLogin.setName(cassJson.at("name").asString());
							spaLogin.setLastLogin(new Timestamp(cassJson.at("last_login").asLong()));
							spaLogin.setStatus(cassJson.at("status").asString());
							// Return Akka data to requester
							mUserPromises.get(spaLogin.getPassword() + cmd).tryComplete(spaLogin);
							mUserPromises.remove(spaLogin.getPassword() + cmd);
							break;
						default:
							break;
						}
				} else {
					logger.error("ERROR received {}", msg.getErrMessage());
				}
			}
		};
	}
}
