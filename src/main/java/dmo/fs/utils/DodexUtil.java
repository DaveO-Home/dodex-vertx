
package dmo.fs.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dmo.fs.db.MessageUser;
import io.reactivex.disposables.Disposable;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class DodexUtil {
    private final static Logger logger = LoggerFactory.getLogger(DodexUtil.class.getName());
    private final static String REMOVEUSER = ";removeuser";
    private final static String USERS = ";users";
    private static String env = "dev";
    String defaultDb = "sqlite3";

    public DodexUtil() {
    }

    public void await(Disposable disposable) {
        while (!disposable.isDisposed()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public Map<String, String> commandMessage(ServerWebSocket ws, String clientData, MessageUser messageUser) {
        Map<String, String> returnObject = new HashMap<>();
    
        try {
            String message = ClientInfo.getMessage(clientData);
            String command = ClientInfo.getCommand(clientData);
            String data = ClientInfo.getData(clientData);
            
            returnObject = processCommand(ws, command, data, messageUser);
            returnObject.put("message", message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnObject;
    }

    private Map<String, String> processCommand(ServerWebSocket ws, String command, String data, MessageUser messageUser)
            throws InterruptedException {
        String selectedUsers = null;
        Map<String, String> returnObject = new HashMap<>();

        switch (command == null? "": command) {
        // Remove user from db when client changes handle.
        // There should not be any undelivered messages against this user if using the dodex client.
        // Howerver, if the browser cache is cleared or server is down, the user may remain in db(obsoleted).
        case REMOVEUSER:
            // DodexDatabase dodexDatabase = new DodexDatabase();
            // Database db = dodexDatabase.getDatabase();
            // dodexDatabase.deleteUser(ws, db, messageUser);
            break;
        // Set users for private messaging.
        case USERS:
            selectedUsers = data.substring(1, data.lastIndexOf("]")).replace("\"", "");
            if(selectedUsers != null && selectedUsers.length() == 0) {
                selectedUsers = null;
            }
            break;
        default:
            command = null;
            break;
        }
        returnObject.put("selectedUsers", selectedUsers);
        returnObject.put("command", command);
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
    public static class ClientInfo {
        private final static String[] commands = { REMOVEUSER, USERS };

        private static String command(String  clientData) {
            for (String clientCommand : commands) {
                if (clientData.contains(clientCommand)) {
                    return clientCommand;
                }
            }
            return null;
        }

        public static String getCommand(String clientData) {
            return command(clientData);
        }

        public static String getMessage(String clientData) {
            if (getCommand(clientData) == null) {
                return clientData;
            }
            return clientData.substring(0, clientData.indexOf(getCommand(clientData)));
        }

        public static String getData(String clientData) {
            String command = getCommand(clientData);
            Integer indexOf = command == null? -1: clientData.indexOf("!!");
            if (indexOf == -1) {
                return null;
            }
            return clientData.substring(clientData.lastIndexOf("!!") + 2);
        }
    }

    public JsonNode getDefaultNode() throws IOException {
		InputStream in = getClass().getResourceAsStream("/database_config.json");
		ObjectMapper jsonMapper = new ObjectMapper();

		JsonNode node = jsonMapper.readTree(in);
		in.close();
        String defaultdbProp = System.getProperty("DEFAULT_DB");
        String defaultdbEnv = System.getenv("DEFAULT_DB");
        defaultDb = node.get("defaultdb").textValue();

        /* use environment variable first, if set, than properties and then from config json */
        defaultDb = defaultdbEnv != null? defaultdbEnv: defaultdbProp != null? defaultdbProp: defaultDb;

        JsonNode defaultNode = node.get(defaultDb);
		return defaultNode;
	}
    
    public String getDefaultDb() throws IOException {
        getDefaultNode();
        return defaultDb;
    }

	public Map<String,String> jsonNodeToMap(JsonNode jsonNode, String env) {
        Map<String, String>defaultMap = new HashMap<>();
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
}