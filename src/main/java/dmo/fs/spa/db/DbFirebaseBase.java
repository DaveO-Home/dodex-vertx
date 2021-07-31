
package dmo.fs.spa.db;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dmo.fs.spa.utils.SpaLogin;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.reactivex.core.Vertx;

public abstract class DbFirebaseBase {
	private static final Logger logger = LoggerFactory.getLogger(DbFirebaseBase.class.getName());

	private Vertx vertx;
    private Firestore dbf;


	public Future<SpaLogin> getLogin(SpaLogin spaLogin) throws InterruptedException, ExecutionException {
        Promise<SpaLogin> promise = Promise.promise();
        spaLogin.setStatus("0");

        DocumentReference docRef = dbf.collection("login").document(String.format("%s%s",spaLogin.getName(), spaLogin.getPassword()));

        ApiFuture<DocumentSnapshot> apiFuture = docRef.get();
        DocumentSnapshot document = apiFuture.get();

        if (document.exists()) {
            spaLogin.setId(document.get("id"));
            updateLogin(spaLogin, docRef);
            promise.complete(spaLogin);
            return promise.future();
        } else {
            spaLogin.setLastLogin(Timestamp.now().toDate());
            spaLogin.setId("0");
            spaLogin.setStatus("-1");
            promise.complete(spaLogin);
            return promise.future();
        }
	}

	public Future<SpaLogin> addLogin(SpaLogin spaLogin) throws InterruptedException, ExecutionException {
        Promise<SpaLogin> promise = Promise.promise();
        Map<String, Object> spaLoginMap = new ConcurrentHashMap<>();
		
        spaLogin.setId(UUID.randomUUID());
        spaLogin.setLastLogin(Timestamp.now().toDate());
        spaLogin.setStatus("0");

        spaLoginMap.put("id", spaLogin.getId());
        spaLoginMap.put("name", spaLogin.getName());
        spaLoginMap.put("password", spaLogin.getPassword());
        spaLoginMap.put("lastlogin", spaLogin.getLastLogin());
        spaLoginMap.put("status", spaLogin.getStatus());


        CollectionReference logins = dbf.collection("login");

        ApiFuture<WriteResult> loginDoc = logins
            .document(String.format("%s%s", spaLogin.getName(), spaLogin.getPassword())).set(spaLoginMap);
        
        loginDoc.get(); // blocking
        
        if(loginDoc.isDone()) {
            promise.complete(spaLogin);
        } else if(loginDoc.isCancelled()) {
            spaLogin.setStatus("-1");
            spaLogin.setId("");
            promise.complete(spaLogin);
        }
		return promise.future();
	}

    public SpaLogin updateLogin(SpaLogin spaLogin, DocumentReference docRef)
			throws InterruptedException, ExecutionException {
        spaLogin.setLastLogin(Timestamp.now().toDate());
        
        ApiFuture<WriteResult> apiFuture = docRef.update("lastlogin", spaLogin.getLastLogin());

        apiFuture.get();  // blocking

        return spaLogin;
	}

	public Future<SpaLogin> removeLogin(SpaLogin spaLogin) throws InterruptedException, ExecutionException {
		// app does not remove logins - used for testing
        Promise<SpaLogin> promise = Promise.promise();
        if(spaLogin.getId() == null) {
            spaLogin.setId("");
        }
        spaLogin.setLastLogin(Timestamp.now().toDate());
        spaLogin.setStatus("1");
        try {
            ApiFuture<WriteResult> writeResult = dbf.collection("login").document(String.format("%s%s", spaLogin.getName(), spaLogin.getPassword())).delete();
            writeResult.get();

            promise.complete(spaLogin);
        } catch(Exception ex) {
            // deleting a non-existing document throws a NULL exception
            logger.error("Error deleting login: {} - {}{}", ex.getMessage(), spaLogin.getName(), spaLogin.getPassword());
            promise.complete(spaLogin);
        }
		return promise.future();
	}

    public void setDbf(Firestore dbf) {
        this.dbf = dbf;
    }

	public Vertx getVertx() {
		return vertx;
	}

	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}
}
