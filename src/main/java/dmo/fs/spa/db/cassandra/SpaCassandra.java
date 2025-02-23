package dmo.fs.spa.db.cassandra;

import org.modellwerkstatt.javaxbus.EventBus;
import dmo.fs.spa.utils.SpaLogin;
import io.vertx.core.Future;
import io.vertx.rxjava3.core.Vertx;

public interface SpaCassandra {

	SpaLogin createSpaLogin();

	Future<SpaLogin> getLogin(SpaLogin spaLogin, EventBus eb) throws InterruptedException;

	Future<SpaLogin> addLogin(SpaLogin spaLogin, EventBus eb) throws InterruptedException;

	Future<SpaLogin> removeLogin(SpaLogin spaLogin, EventBus eb) throws InterruptedException;

	void setVertx(Vertx vertx);

	Vertx getVertx();

}
