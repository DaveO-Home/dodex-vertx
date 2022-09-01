package dmo.fs.spa.db;

import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import com.google.cloud.firestore.Firestore;
import dmo.fs.spa.utils.SpaLogin;
import io.vertx.core.Future;
import io.vertx.rxjava3.core.Vertx;

public interface SpaFirebase {

	SpaLogin createSpaLogin();

	Future<SpaLogin> getLogin(SpaLogin spaLogin)
			throws ExecutionException, InterruptedException, SQLException;

	Future<SpaLogin> addLogin(SpaLogin spaLogin) throws InterruptedException, ExecutionException;

	Future<SpaLogin> removeLogin(SpaLogin spaLogin) throws InterruptedException, ExecutionException;

	void setVertx(Vertx vertx);

	Vertx getVertx();

	void setDbf(Firestore dbf);
}
