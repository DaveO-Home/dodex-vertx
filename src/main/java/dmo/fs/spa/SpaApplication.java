package dmo.fs.spa;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

import org.modellwerkstatt.javaxbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dmo.fs.spa.db.DbCassandraBase;
import dmo.fs.spa.db.SpaCassandra;
import dmo.fs.spa.db.SpaDatabase;
import dmo.fs.spa.db.SpaDbConfiguration;
import dmo.fs.spa.utils.SpaLogin;
import dmo.fs.spa.utils.SpaLoginImpl;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class SpaApplication extends DbCassandraBase {
    private final static Logger logger = LoggerFactory.getLogger(SpaApplication.class.getName());
    private SpaDatabase spaDatabase;
    private SpaCassandra spaCassandra;
    private SpaLogin spaLogin;
    private static EventBus eb;
    private Boolean isCassandra = false;

    public SpaApplication() throws InterruptedException, IOException, SQLException {
        if(SpaDbConfiguration.getSpaDb() instanceof SpaDatabase) {
            spaDatabase = SpaDbConfiguration.getSpaDb();
        } else {
            spaCassandra = SpaDbConfiguration.getSpaDb();
            isCassandra = true;
        }

        spaLogin = createSpaLogin();
    }

    public Future<Void> setupDatabase() throws InterruptedException, SQLException {
        return spaDatabase.databaseSetup();
    }

    public Future<SpaLogin> getLogin(String queryData) throws InterruptedException, SQLException {
        JsonObject loginObject = new JsonObject(String.join("", "{\"data\":", queryData, "}"));
        String name;
        String password;
        if(!queryData.contains("[")) {
            name = loginObject.getJsonObject("data").getString("name");
            password = loginObject.getJsonObject("data").getString("password");
        } else {
            JsonArray data = loginObject.getJsonArray("data");
            name = data.getJsonObject(0).getString("value");
            password = data.getJsonObject(1).getString("value");
        }

        spaLogin.setName(name);
        spaLogin.setPassword(password);
        
        if (isCassandra) {
            return spaCassandra.getLogin(spaLogin, eb);
        }
        return spaDatabase.getLogin(spaLogin);
    }

    public Future<SpaLogin> addLogin(String bodyData) throws InterruptedException, SQLException {
        JsonObject loginObject = new JsonObject(String.join("", "{\"data\":", bodyData, "}"));
        String userName = null;
        String password = null;
        JsonArray data = loginObject.getJsonArray("data");
        int size = loginObject.getJsonArray("data").getList().size();

        for (int i = 0; i < size; i++) {
            String name = data.getJsonObject(i).getString("name");
            switch (name) {
                case "username":
                    userName = data.getJsonObject(i).getString("value");
                    break;
                case "password":
                    password = data.getJsonObject(i).getString("value");
                    break;
                default:
                    break;
            }
        }

        spaLogin.setName(userName);
        spaLogin.setPassword(password);
        spaLogin.setId(0l);
        spaLogin.setLastLogin(new Date());
        spaLogin.setStatus("0");

        if (isCassandra) {
            return spaCassandra.addLogin(spaLogin, eb);
        }
        return spaDatabase.addLogin(spaLogin);
    }

    public Future<SpaLogin> unregisterLogin(String queryData) throws InterruptedException, SQLException {
        Map<String, String> queryMap = mapQuery(queryData);

        String name = queryMap.get("user");
        String password = queryMap.get("password");

        spaLogin.setName(name);
        spaLogin.setPassword(password);

        if (isCassandra) {
            return spaCassandra.removeLogin(spaLogin, eb);
        }
        return spaDatabase.removeLogin(spaLogin);
    }

    public Map<String, String> mapQuery(String queryString) {
        return Arrays.stream(queryString.split("&")).map(s -> s.split("="))
                .collect(Collectors.toMap(s -> s[0], s -> s[1]));
    }

    @Override
	public SpaLogin createSpaLogin() {
		return new SpaLoginImpl();
	}

    public static EventBus getEb() {
        return eb;
    }

    public static void setEb(EventBus eb) {
        SpaApplication.eb = eb;
    }
}