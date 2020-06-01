
package dmo.fs.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ParseQueryUtilHelper {
    
    public static Map<String, String> getQueryMap(String query) {
        Map<String, String> map = new ConcurrentHashMap<String, String>();
        if(query == null) {
            return map;
        }
        String[] params = query.substring(query.indexOf('?') + 1).split("&");
                
        for (String param : params)
        {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            map.put(name, value);
        }
        return map;
    }

    private ParseQueryUtilHelper() {
    }

}