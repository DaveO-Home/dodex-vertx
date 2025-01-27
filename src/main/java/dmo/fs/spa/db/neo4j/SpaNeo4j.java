package dmo.fs.spa.db.neo4j;

import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

import org.neo4j.driver.Driver;

import dmo.fs.spa.utils.SpaLogin;
import io.vertx.core.Future;

public interface SpaNeo4j {

	SpaLogin createSpaLogin();

	Future<SpaLogin> getLogin(SpaLogin spaLogin) throws ExecutionException, InterruptedException, SQLException;

	Future<SpaLogin> addLogin(SpaLogin spaLogin) throws InterruptedException, ExecutionException;
	
	Future<SpaLogin> removeLogin(SpaLogin spaLogin) throws InterruptedException, ExecutionException;

	Future<Void> databaseSetup();

	void setDriver(Driver driver);

}
