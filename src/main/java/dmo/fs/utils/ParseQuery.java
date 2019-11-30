
package dmo.fs.utils;

import java.util.Hashtable;
import java.util.Map;

public class ParseQuery {

    public static Map<String, String> getQueryMap(String query) {
        String[] params = query.substring(query.indexOf("?") + 1).split("&");
        Map<String, String> map = new Hashtable<String, String>();
        for (String param : params)
        {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            map.put(name, value);
        }
        return map;
    }
}