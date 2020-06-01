package dmo.fs.spa.db;

import java.sql.SQLException;

import org.davidmoten.rx.jdbc.Database;
import org.davidmoten.rx.jdbc.pool.NonBlockingConnectionPool;

import dmo.fs.spa.utils.SpaLogin;
import io.vertx.core.Future;

public interface SpaDatabase {

	Database getDatabase();

	NonBlockingConnectionPool getPool();

	SpaLogin createSpaLogin();

	Future<SpaLogin> getLogin(SpaLogin spaLogin, Database db) throws InterruptedException, SQLException;

	Future<SpaLogin> addLogin(SpaLogin spaLogin, Database db) throws InterruptedException, SQLException;
	
	Future<SpaLogin> removeLogin(SpaLogin spaLogin, Database db) throws InterruptedException, SQLException;

}