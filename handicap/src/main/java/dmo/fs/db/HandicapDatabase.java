package dmo.fs.db;

import java.sql.SQLException;

import io.vertx.core.Future;
import io.vertx.rxjava3.core.Vertx;

public interface HandicapDatabase {
	Future<String> checkOnTables() throws InterruptedException, SQLException;

	<T> T getPool4();

	void setVertx(Vertx vertx);

	Vertx getVertx();
}