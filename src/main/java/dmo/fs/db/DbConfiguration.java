package dmo.fs.db;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.davidmoten.rx.jdbc.ConnectionProvider;

import dmo.fs.utils.DodexUtil;

public class DbConfiguration {
    private static Map<String, String> sqlite3 = new HashMap<>();
    private static Map<String, String> postgres = new HashMap<>();
    private static Properties sqlite3Properties = new Properties();
    private static Properties postgresProperties = new Properties();

    private static Boolean isUsingSqlite3 = false;
    private static Boolean isUsingPostgres = false;
    private static String defaultDb = "sqlite3";
    private static DodexUtil dodexUtil = new DodexUtil();

    DbConfiguration() {
    }

    public static ConnectionProvider getSqlite3ConnectionProvider() {
        isUsingSqlite3 = true;
        isUsingPostgres = false;
        return ConnectionProvider.from(sqlite3.get("url") + sqlite3.get("filename"), sqlite3Properties);
    }

    public static ConnectionProvider getPostgresConnectionProvider() {
        isUsingSqlite3 = false;
        isUsingPostgres = true;
        return ConnectionProvider.from(postgres.get("url") + postgres.get("host") + postgres.get("dbname"),
                postgresProperties);
    }

    public static boolean isUsingSqlite3() {
        return isUsingSqlite3;
    }

    public static boolean isUsingPostgres() {
        return isUsingPostgres;
    }

    public static DodexDatabase getDefaultDb() throws InterruptedException, IOException {
        defaultDb = dodexUtil.getDefaultDb();

        if(defaultDb.equals("postgres")) {
            return new DodexDatabasePostgres();
        } else if(defaultDb.equals("sqlite3")) {
            return new DodexDatabaseSqlite3();
        }
        throw new InterruptedException("No Database set");
    }

    public static DodexDatabase getDefaultDb(Map<String, String>overrideMap, Properties overrideProps) throws InterruptedException, IOException {
        defaultDb = dodexUtil.getDefaultDb();
        
        if(defaultDb.equals("postgres")) {
            return new DodexDatabasePostgres(overrideMap, overrideProps);
        } else if(defaultDb.equals("sqlite3")) {
            return new DodexDatabaseSqlite3(overrideMap, overrideProps);
        }
        throw new InterruptedException("No Database set");
    }

    public static void configureSqlite3Defaults(Map<String, String>overrideMap, Properties overrideProps) {
        if(overrideProps != null && overrideProps.size() > 0) {
            sqlite3Properties = overrideProps;
        }
        mapMerge(sqlite3, overrideMap);
    }

    public static void configureSqlite3TestDefaults(Map<String, String>overrideMap,  Properties overrideProps) {
        if(overrideProps != null && overrideProps.size() > 0) {
            sqlite3Properties = overrideProps;
        }
        mapMerge(sqlite3, overrideMap);
    }

        public static void configurePostgresDefaults(Map<String, String>overrideMap, Properties overrideProps) {
        if(overrideProps != null && overrideProps.size() > 0) {
            postgresProperties = overrideProps;
        }
        mapMerge(postgres, overrideMap);
    }

    public static void configurePostgresTestDefaults(Map<String, String>overrideMap, Properties overrideProps) {
        if(overrideProps != null && overrideProps.size() > 0) {
            postgresProperties = overrideProps;
        }
        mapMerge(postgres, overrideMap);

    }

    public static void mapMerge(Map<String,String> map1, Map<String, String> map2) {
        map2.forEach((key, value) -> map1
            .merge( key, value, (v1, v2) -> v2));  // let duplicate key in map2 win
    }
}