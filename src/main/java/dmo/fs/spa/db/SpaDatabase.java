package dmo.fs.spa.db;

import java.sql.SQLException;
import dmo.fs.spa.utils.SpaLogin;
import io.vertx.core.Future;
import io.vertx.rxjava3.core.Vertx;

public interface SpaDatabase {

	SpaLogin createSpaLogin();

	Future<SpaLogin> getLogin(SpaLogin spaLogin) throws InterruptedException, SQLException;

	Future<SpaLogin> addLogin(SpaLogin spaLogin) throws InterruptedException, SQLException;

	Future<SpaLogin> removeLogin(SpaLogin spaLogin) throws InterruptedException, SQLException;

	Future<Void> databaseSetup() throws InterruptedException, SQLException;

	<T> T getPool();

	void setVertx(Vertx vertx);

	Vertx getVertx();
}
