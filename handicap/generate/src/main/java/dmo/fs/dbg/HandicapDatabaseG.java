package dmo.fs.dbg;

import io.vertx.core.Future;
import io.vertx.rxjava3.core.Vertx;

import java.sql.SQLException;

public interface HandicapDatabaseG {
	Future<String> checkOnTables() throws InterruptedException, SQLException;

	<T> T getClient();

	void setVertx(Vertx vertx);

	Vertx getVertx();
}