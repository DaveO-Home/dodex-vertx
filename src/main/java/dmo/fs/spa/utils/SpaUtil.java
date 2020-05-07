
package dmo.fs.spa.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jooq.SQLDialect;

import io.reactivex.disposables.Disposable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class SpaUtil {
    private final static Logger logger = LoggerFactory.getLogger(SpaUtil.class.getName());
    private static String env = "dev";
    private static String defaultDb = "sqlite3";

    public SpaUtil() {
    }

    public void await(final Disposable disposable) {
        while (!disposable.isDisposed()) {
            try {
                Thread.sleep(100);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void setEnv(final String envMode) {
        env = envMode;
    }

    public static String getEnv() {
        return env;
    }

    public static JsonNode getDefaultNode() throws IOException {
        final InputStream in = SpaUtil.class.getResourceAsStream("/database_spa_config.json");
        final ObjectMapper jsonMapper = new ObjectMapper();

        final JsonNode node = jsonMapper.readTree(in);
        in.close();
        final String defaultdbProp = System.getProperty("DEFAULT_DB");
        final String defaultdbEnv = System.getenv("DEFAULT_DB");
        defaultDb = node.get("defaultdb").textValue();

        /*
         * use environment variable first, if set, than properties and then from config
         * json
         */
        defaultDb = defaultdbEnv != null ? defaultdbEnv : defaultdbProp != null ? defaultdbProp : defaultDb;

        final JsonNode defaultNode = node.get(defaultDb);
        return defaultNode;
    }

    public static String getDefaultDb() throws IOException {
        getDefaultNode();
        return defaultDb;
    }

    public static Map<String, String> jsonNodeToMap(final JsonNode jsonNode, final String env) {
        final Map<String, String> defaultMap = new HashMap<>();
        final JsonNode credentials = jsonNode.get(env).get("credentials");
        final JsonNode config = jsonNode.get(env).get("config");
        Iterator<String> fields = config.fieldNames();

        while (fields.hasNext()) {
            final String field = fields.next();
            if (config.get(field) != null) {
                defaultMap.put(field, config.get(field).textValue());
            }
        }

        fields = credentials.fieldNames();
        while (fields.hasNext()) {
            final String field = fields.next();
            if (credentials.get(field) != null) {
                defaultMap.put("CRED:" + field, credentials.get(field).textValue());
            }
        }
        return defaultMap;
    }

    public Properties mapToProperties(final Map<String, String> map) {
        final Properties properties = new Properties();
        final Set<Map.Entry<String, String>> set = map.entrySet();
        for (final Map.Entry<String, String> entry : set) {
            if (entry.getKey().startsWith("CRED:")) {
                properties.put(entry.getKey().substring(5), entry.getValue());
            }
        }

        final Set<Object> keys = properties.keySet();
        for (final Object key : keys) {
            map.remove("CRED:" + key.toString());
        }

        return properties;
    }

    public static SQLDialect getSqlDialect() {
        String database = null;
        try {
            database = getDefaultDb();
            database = database.equals("sqlite3") ? "SQLITE" : database.toUpperCase();
            for (final SQLDialect sqlDialect : SQLDialect.values()) {
                if (database.equals(sqlDialect.name())) {
                    return sqlDialect;
                }
            }
            ;
        } catch (final IOException e) {
            e.printStackTrace();
        }

        return SQLDialect.DEFAULT;
      }
}