package dmo.fs.db;

import java.io.IOException;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.davidmoten.rx.jdbc.ConnectionProvider;

import dmo.fs.utils.DodexUtil;

public class DbConfiguration {

    private static Map<String, String> map = new HashMap<>();
    private static Properties properties = new Properties();

    private static Boolean isUsingSqlite3 = false;
    private static Boolean isUsingPostgres = false;
    private static Boolean isUsingCubrid = false;
    private static Boolean isUsingMariadb = false;
    private static String defaultDb = "sqlite3";
    private static DodexUtil dodexUtil = new DodexUtil();

    DbConfiguration() {
    }

    public static ConnectionProvider getSqlite3ConnectionProvider() {
        isUsingSqlite3 = true;
        return ConnectionProvider.from(map.get("url") + map.get("filename"), properties);
    }

    public static ConnectionProvider getPostgresConnectionProvider() {
        isUsingPostgres = true;
        return ConnectionProvider.from(map.get("url") + map.get("host") + map.get("dbname"), properties);
    }

    public static ConnectionProvider getCubridConnectionProvider() throws SQLException {
        isUsingCubrid = true;
        
        Driver driver = new cubrid.jdbc.driver.CUBRIDDriver();
        DriverManager.registerDriver(driver);

        return ConnectionProvider.from(map.get("url") + map.get("host") + map.get("dbname") + "?charSet=utf8",
                properties.get("user").toString(), properties.get("password").toString());
    }

    public static ConnectionProvider getMariadbConnectionProvider() throws SQLException {
        isUsingMariadb = true;

        return ConnectionProvider.from(map.get("url") + map.get("host") + map.get("dbname") + "?charSet=utf8",
                properties.get("user").toString(), properties.get("password").toString());
    }

    public static boolean isUsingSqlite3() {
        return isUsingSqlite3;
    }

    public static boolean isUsingPostgres() {
        return isUsingPostgres;
    }

    public static boolean isUsingCubrid() {
        return isUsingCubrid;
    }

    public static boolean isUsingMariadb() {
        return isUsingMariadb;
    }

    public static DodexDatabase getDefaultDb() throws InterruptedException, IOException, SQLException {
        defaultDb = dodexUtil.getDefaultDb().toLowerCase();

        if(defaultDb.equals("postgres")) {
            return new DodexDatabasePostgres();
        } else if(defaultDb.equals("sqlite3")) {
            return new DodexDatabaseSqlite3();
        } else if(defaultDb.equals("cubrid")) {
            return new DodexDatabaseCubrid();
        } else if(defaultDb.equals("mariadb")) {
            return new DodexDatabaseMariadb();
        }
        throw new InterruptedException("No Database set");
    }

    public static DodexDatabase getDefaultDb(Map<String, String>overrideMap, Properties overrideProps) throws InterruptedException, IOException, SQLException {
        defaultDb = dodexUtil.getDefaultDb();
        
        if(defaultDb.equals("postgres")) {
            return new DodexDatabasePostgres(overrideMap, overrideProps);
        } else if(defaultDb.equals("sqlite3")) {
            return new DodexDatabaseSqlite3(overrideMap, overrideProps);
        } else if(defaultDb.equals("cubrid")) {
            return new DodexDatabaseCubrid(overrideMap, overrideProps);
        } else if(defaultDb.equals("mariadb")) {
            return new DodexDatabaseMariadb(overrideMap, overrideProps);
        }
        throw new InterruptedException("No Database set");
    }

    public static void configureDefaults(Map<String, String>overrideMap, Properties overrideProps) {
        if(overrideProps != null && overrideProps.size() > 0) {
            properties = overrideProps;
        }
        mapMerge(map, overrideMap);
    }

    public static void configureTestDefaults(Map<String, String>overrideMap, Properties overrideProps) {
        if(overrideProps != null && overrideProps.size() > 0) {
            properties = overrideProps;
        }
        mapMerge(map, overrideMap);

    }

    public static void mapMerge(Map<String,String> map1, Map<String, String> map2) {
        map2.forEach((key, value) -> map1
            .merge( key, value, (v1, v2) -> v2));  // let duplicate key in map2 win
    }
}