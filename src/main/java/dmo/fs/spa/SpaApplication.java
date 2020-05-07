package dmo.fs.spa;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.stream.Collectors;

import org.davidmoten.rx.jdbc.Database;

import dmo.fs.spa.db.SpaDatabase;
import dmo.fs.spa.db.SpaDbConfiguration;
import dmo.fs.spa.utils.SpaLogin;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class SpaApplication {
    private static Database db;
    private SpaDatabase spaDatabase;
    private SpaLogin spaLogin;

    public SpaApplication() throws InterruptedException, IOException, SQLException {
        spaDatabase = SpaDbConfiguration.getSpaDb();
        db = spaDatabase.getDatabase();
        spaLogin = spaDatabase.createSpaLogin();
    }

    public JsonObject getLogin(String queryData) throws InterruptedException, SQLException {
        JsonObject loginObject = new JsonObject(String.join("", "{\"data\":", queryData, "}"));
        JsonArray data = loginObject.getJsonArray("data");
        String name = data.getJsonObject(0).getString("value");
        String password = data.getJsonObject(1).getString("value");

        spaLogin.setName(name);
        spaLogin.setPassword(password);
        spaLogin = spaDatabase.getLogin(spaLogin, db);

        loginObject = new JsonObject(spaLogin.getMap());
        return loginObject;
    }

    public JsonObject addLogin(String bodyData) throws InterruptedException, SQLException {
        JsonObject loginObject = new JsonObject(String.join("", "{\"data\":", bodyData, "}"));
        JsonArray data = loginObject.getJsonArray("data");
        int size = loginObject.getJsonArray("data").getList().size();
        String userName = null;
        String password = null;

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

        spaLogin = spaDatabase.addLogin(spaLogin, db);

        loginObject = new JsonObject(spaLogin.getMap());

        return loginObject;
    }

    public JsonObject unregisterLogin(String queryData) throws InterruptedException, SQLException {
        String name = null;
        String password = null;
        Map<String, String> queryMap = mapQuery(queryData);

        name = queryMap.get("user");
        password = queryMap.get("password");

        spaLogin.setName(name);
        spaLogin.setPassword(password);
        spaLogin = spaDatabase.removeLogin(spaLogin, db);

        JsonObject loginObject = new JsonObject(spaLogin.getMap());

        return loginObject;
    }

    public Map<String, String> mapQuery(String queryString) {
        return Arrays.stream(queryString.split("&")).map(s -> s.split("="))
                .collect(Collectors.toMap(s -> s[0], s -> s[1]));
    }
}