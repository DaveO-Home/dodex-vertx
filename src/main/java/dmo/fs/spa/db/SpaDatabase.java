package dmo.fs.spa.db;

import java.sql.SQLException;

import org.davidmoten.rx.jdbc.Database;
import org.davidmoten.rx.jdbc.pool.NonBlockingConnectionPool;

import dmo.fs.spa.utils.SpaLogin;

public interface SpaDatabase {

	public Database getDatabase();

	public NonBlockingConnectionPool getPool();

	public SpaLogin createSpaLogin();

	public SpaLogin getLogin(SpaLogin spaLogin, Database db) throws InterruptedException, SQLException;

	public SpaLogin addLogin(SpaLogin spaLogin, Database db) throws InterruptedException, SQLException;
	
	public SpaLogin removeLogin(SpaLogin spaLogin, Database db) throws InterruptedException, SQLException;

}