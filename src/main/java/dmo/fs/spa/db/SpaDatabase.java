package dmo.fs.spa.db;

import java.sql.SQLException;

import org.davidmoten.rx.jdbc.Database;
import org.davidmoten.rx.jdbc.pool.NonBlockingConnectionPool;

import dmo.fs.spa.utils.SpaLogin;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

public interface SpaDatabase {

	public Database getDatabase();

	public NonBlockingConnectionPool getPool();

	public SpaLogin createSpaLogin();

	public Future<SpaLogin> getLogin(SpaLogin spaLogin, Database db) throws InterruptedException, SQLException;

	public Future<SpaLogin> addLogin(SpaLogin spaLogin, Database db) throws InterruptedException, SQLException;
	
	public Future<SpaLogin> removeLogin(SpaLogin spaLogin, Database db) throws InterruptedException, SQLException;

}