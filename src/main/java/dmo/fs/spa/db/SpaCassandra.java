package dmo.fs.spa.db;

import org.modellwerkstatt.javaxbus.EventBus;

import dmo.fs.spa.utils.SpaLogin;
import io.vertx.core.Future;

public interface SpaCassandra {

	SpaLogin createSpaLogin();

	Future<SpaLogin> getLogin(SpaLogin spaLogin, EventBus eb) throws InterruptedException;

	Future<SpaLogin> addLogin(SpaLogin spaLogin, EventBus eb) throws InterruptedException;
	
	Future<SpaLogin> removeLogin(SpaLogin spaLogin, EventBus eb) throws InterruptedException;

}