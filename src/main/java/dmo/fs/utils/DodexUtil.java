
package dmo.fs.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jooq.SQLDialect;

import io.reactivex.disposables.Disposable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class DodexUtil {
    private final static Logger logger = LoggerFactory.getLogger(DodexUtil.class.getName());
    private final static String REMOVEUSER = ";removeuser";
    private final static String USERS = ";users";
    private static String env = "dev";
    String defaultDb = "sqlite3";

    public void await(Disposable disposable) {
        while (!disposable.isDisposed()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                logger.error(String.join("", "Await: ", e.getMessage()));
            }
        }
    }

    public Map<String, String> commandMessage(String clientData) {
        Map<String, String> returnObject = new ConcurrentHashMap<>();
    
        try {
            String message = ClientInfoUtilHelper.getMessage.apply(clientData);
            String command = ClientInfoUtilHelper.getCommand.apply(clientData);
            String data = ClientInfoUtilHelper.getData.apply(clientData);
            
            returnObject = processCommand(command, data);
            returnObject.put("message", message);
        } catch (Exception e) {
            logger.error(String.join("", "Command Message: ", e.getMessage()));
            e.printStackTrace();
        }
        return returnObject;
    }

    private Map<String, String> processCommand(String command, String data)
            throws InterruptedException {
        String selectedUsers = "";
        Map<String, String> returnObject = new ConcurrentHashMap<>();
        String switchValue = command == null ? "" : command;

        switch (switchValue) {
        // Remove user from db when client changes handle.
        // There should not be any undelivered messages against this user if using the dodex client.
        // Howerver, if the browser cache is cleared or server is down, the user may remain in db(obsoleted).
        case REMOVEUSER:
            // Happening in DodexRouter
            break;
        // Set users for private messaging.
        case USERS:
            selectedUsers = data.substring(1, data.lastIndexOf(']')).replace("\"", "");
            break;
        default:
            break;
        }
        returnObject.put("selectedUsers", selectedUsers);
        returnObject.put("command", switchValue);
        return returnObject;
    }

    public static void setEnv(String envMode) {
        env = envMode;
    }

    public static String getEnv() {
        return env;
    }
    /*
        Split out command and data from client message.
    */
    public static class ClientInfoUtilHelper {
        private final static String[] commands = { REMOVEUSER, USERS };

        private static Function<String, String> command = (clientData) -> {
            for (String clientCommand : commands) {
                if (clientData.contains(clientCommand)) {
                    return clientCommand;
                }
            }
            return null;
        };

        public static Function<String, String> getCommand = (clientData) -> {
            return command.apply(clientData);
        };

        public static Function<String, String> getMessage = (clientData) -> {
            if (getCommand.apply(clientData) == null) {
                return clientData;
            }
            return clientData.substring(0, clientData.indexOf(getCommand.apply(clientData)));
        };

        public static Function<String, String> getData = (clientData) -> {
            String command = getCommand.apply(clientData);
            Integer indexOf = command == null? -1: clientData.indexOf("!!");
            if (indexOf == -1) {
                return null;
            }
            return clientData.substring(clientData.lastIndexOf("!!") + 2);
        };

        private ClientInfoUtilHelper() {
        }
    }

    public JsonNode getDefaultNode() throws IOException {
        ObjectMapper jsonMapper = new ObjectMapper();
        JsonNode node;

        try(InputStream in = getClass().getResourceAsStream("/database_config.json")) {
            node = jsonMapper.readTree(in);
        }

        String defaultdbProp = System.getProperty("DEFAULT_DB");
        String defaultdbEnv = System.getenv("DEFAULT_DB");
        defaultDb = node.get("defaultdb").textValue();

        /* use environment variable first, if set, than properties and then from config json */
        defaultDb = defaultdbEnv != null? defaultdbEnv: defaultdbProp != null? defaultdbProp: defaultDb;

		return node.get(defaultDb);
	}
    
    public String getDefaultDb() throws IOException {
        getDefaultNode();
        return defaultDb;
    }

	public Map<String,String> jsonNodeToMap(JsonNode jsonNode, String env) {
        Map<String, String>defaultMap = new ConcurrentHashMap<>();
        JsonNode credentials = jsonNode.get(env).get("credentials");
        JsonNode config = jsonNode.get(env).get("config");
		Iterator<String> fields = config.fieldNames();

        while(fields.hasNext()) {
            String field = fields.next();
            if(config.get(field) != null) {
                defaultMap.put(field, config.get(field).textValue());
            }
		}
        
        fields = credentials.fieldNames();
		while(fields.hasNext()) {
            String field = fields.next();
            if(credentials.get(field) != null) {
                defaultMap.put("CRED:" + field, credentials.get(field).textValue());
            }
		}
		return defaultMap;
    }
    
    public Properties mapToProperties(Map<String, String> map) {
		Properties properties = new Properties();
		Set<Map.Entry<String,String>> set = map.entrySet();
		for (Map.Entry<String,String> entry : set) {
		  if(entry.getKey().startsWith("CRED:")) {
              properties.put(entry.getKey().substring(5), entry.getValue());
		  }
        }

        Set<Object> keys = properties.keySet();
        for(Object key :keys) {
            map.remove("CRED:"+key.toString());
        }

		return properties;
      }

      public static SQLDialect getSqlDialect() {
        DodexUtil dodexUtil = new DodexUtil();
        String database = null;
        try {
            database = dodexUtil.getDefaultDb();
            database = "SQLITE".equals(database)? "SQLITE": database.toUpperCase();
            for (SQLDialect sqlDialect : SQLDialect.values()) {
                if(database.equals(sqlDialect.name())) {
                    return sqlDialect;
                }
            }
        } catch (IOException e) {
            logger.error(String.join("", "SqlDialect: ", e.getMessage()));
        }

        return SQLDialect.DEFAULT;
      }
}