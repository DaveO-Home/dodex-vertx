package dmo.fs.spa.db;

import java.sql.SQLException;

import dmo.fs.spa.utils.SpaLogin;
import io.vertx.core.Future;
import io.vertx.reactivex.core.Vertx;

public interface SpaDatabase {

	SpaLogin createSpaLogin();

	Future<SpaLogin> getLogin(SpaLogin spaLogin) throws InterruptedException, SQLException;

	Future<SpaLogin> addLogin(SpaLogin spaLogin) throws InterruptedException, SQLException;
	
	Future<SpaLogin> removeLogin(SpaLogin spaLogin) throws InterruptedException, SQLException;

    Future<Void> databaseSetup() throws InterruptedException, SQLException;

    <T> T getPool4();

    void setVertx(Vertx vertx);

	Vertx getVertx();
}