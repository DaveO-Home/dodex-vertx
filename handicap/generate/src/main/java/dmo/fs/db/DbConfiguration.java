package dmo.fs.db;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import dmo.fs.utils.DodexUtil;

public abstract class DbConfiguration {

    private static Map<String, String> map = new ConcurrentHashMap<>();
    protected static Properties properties = new Properties();

    private static Boolean isUsingSqlite3 = false;
    private static Boolean isUsingPostgres = false;
    private static Boolean isUsingCubrid = false;
    private static Boolean isUsingMariadb = false;
    private static Boolean isUsingIbmDB2 = false;
    private static Boolean isUsingCassandra = false;
    private static Boolean isUsingFirebase = false;
    private static String defaultDb = "sqlite3";
    private static DodexUtil dodexUtil = new DodexUtil();
    private static HandicapDatabase handicapDatabase;
    // private static DodexCassandra dodexCassandra;
    // private static DodexFirebase dodexFirebase;

    private enum DbTypes {
        POSTGRES("postgres"),
        SQLITE3("sqlite3"),
        CUBRID("cubrid"),
        MARIADB("mariadb"),
        IBMDB2("ibmdb2"),
        CASSANDRA("cassandra"),
        FIREBASE("firebase");

        String db;

        DbTypes(String db) {
            this.db = db;
        }
    };

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

    public static boolean isUsingIbmDB2() {
        return isUsingIbmDB2;
    }

    public static boolean isUsingCassandra() {
        return isUsingCassandra;
    }

    public static boolean isUsingFirebase() {
        return isUsingFirebase;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getDefaultDb() throws InterruptedException, IOException, SQLException {
        defaultDb = dodexUtil.getDefaultDb().toLowerCase();
        try {
            if(defaultDb.equals(DbTypes.POSTGRES.db) && handicapDatabase == null) {
                handicapDatabase = new HandicapDatabasePostgres();
                isUsingPostgres = true;
            } else 
            if(defaultDb.equals(DbTypes.SQLITE3.db) && handicapDatabase == null) {
                handicapDatabase = new HandicapDatabaseSqlite3();
                isUsingSqlite3 = true;
            } 
            // else if(defaultDb.equals(DbTypes.CUBRID.db) && handicapDatabase == null) {
            //     handicapDatabase = new handicapDatabaseCubrid();
            //     isUsingCubrid = true;
            // } else if(defaultDb.equals(DbTypes.MARIADB.db) && handicapDatabase == null) {
            //     handicapDatabase = new handicapDatabaseMariadb();
            //     isUsingMariadb = true;
            // } else if(defaultDb.equals(DbTypes.IBMDB2.db) && handicapDatabase == null) {
            //     handicapDatabase = new handicapDatabaseIbmDB2();
            //     isUsingIbmDB2 = true;
            // } else if(defaultDb.equals(DbTypes.CASSANDRA.db)) {
            //     dodexCassandra = dodexCassandra == null? new handicapDatabaseCassandra(): dodexCassandra;
            //     isUsingCassandra = true;
            //     return (T) dodexCassandra;
            // } else if(defaultDb.equals(DbTypes.FIREBASE.db)) {
            //     dodexFirebase = dodexFirebase == null? new handicapDatabaseFirebase(): dodexFirebase;
            //     isUsingFirebase = true;
            //     return (T) dodexFirebase;
            // }
        } catch (Exception exception) { 
            throw exception;
        }
        return (T) handicapDatabase;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getDefaultDb(Boolean isCreateTables) throws InterruptedException, IOException, SQLException {
        defaultDb = dodexUtil.getDefaultDb().toLowerCase();
        try {
            if(defaultDb.equals(DbTypes.POSTGRES.db) /*&& handicapDatabase == null*/ && isCreateTables) {
                handicapDatabase = new HandicapDatabasePostgres(isCreateTables);
                isUsingPostgres = true;
            } else 
            if(defaultDb.equals(DbTypes.SQLITE3.db) /*&& handicapDatabase == null*/ && isCreateTables) {
                handicapDatabase = new HandicapDatabaseSqlite3(isCreateTables);
                isUsingSqlite3 = true;
            }
            // else if(defaultDb.equals(DbTypes.CUBRID.db) && handicapDatabase == null) {
            //     handicapDatabase = new handicapDatabaseCubrid();
            //     isUsingCubrid = true;
            else if(defaultDb.equals(DbTypes.MARIADB.db) /* && handicapDatabase == null*/ && isCreateTables) {
                handicapDatabase = new HandicapDatabaseMariadb(isCreateTables);
                isUsingMariadb = true;
            } 
            // else if(defaultDb.equals(DbTypes.IBMDB2.db) && handicapDatabase == null) {
            //     handicapDatabase = new handicapDatabaseIbmDB2();
            //     isUsingIbmDB2 = true;
            // } else if(defaultDb.equals(DbTypes.CASSANDRA.db)) {
            //     dodexCassandra = dodexCassandra == null? new handicapDatabaseCassandra(): dodexCassandra;
            //     isUsingCassandra = true;
            //     return (T) dodexCassandra;
            // } else if(defaultDb.equals(DbTypes.FIREBASE.db)) {
            //     dodexFirebase = dodexFirebase == null? new handicapDatabaseFirebase(): dodexFirebase;
            //     isUsingFirebase = true;
            //     return (T) dodexFirebase;
            // }
        } catch (Exception exception) { 
            throw exception;
        }
        return (T) handicapDatabase;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getDefaultDb(Map<String, String> overrideMap, Properties overrideProps)
            throws InterruptedException, IOException, SQLException {
        defaultDb = dodexUtil.getDefaultDb();
        
        try {
            if(defaultDb.equals(DbTypes.POSTGRES.db) && handicapDatabase == null) {
                handicapDatabase = new HandicapDatabasePostgres(overrideMap, overrideProps);
                isUsingPostgres = true;
            } else 
            if(defaultDb.equals(DbTypes.SQLITE3.db) && handicapDatabase == null) {
                handicapDatabase = new HandicapDatabaseSqlite3(overrideMap, overrideProps);
                isUsingSqlite3 = true;
            } 
            // else if(defaultDb.equals(DbTypes.CUBRID.db) && handicapDatabase == null) {
            //     handicapDatabase = new handicapDatabaseCubrid(overrideMap, overrideProps);
            //     isUsingCubrid = true;
            // } 
            else if(defaultDb.equals(DbTypes.MARIADB.db) && handicapDatabase == null) {
                handicapDatabase = new HandicapDatabaseMariadb(overrideMap, overrideProps);
                isUsingMariadb = true;
            } 
            // else if(defaultDb.equals(DbTypes.IBMDB2.db) && handicapDatabase == null) {
            //     handicapDatabase = new handicapDatabaseIbmDB2(overrideMap, overrideProps);
            //     isUsingIbmDB2 = true;
            // } else if(defaultDb.equals(DbTypes.CASSANDRA.db)) {
            //     dodexCassandra = dodexCassandra == null? 
            //         new handicapDatabaseCassandra(overrideMap, overrideProps): dodexCassandra;
            //         isUsingCassandra = true;
            //     return (T) dodexCassandra;
            // } else if(defaultDb.equals(DbTypes.FIREBASE.db)) {
            //     dodexFirebase = dodexFirebase == null? 
            //         new handicapDatabaseFirebase(overrideMap, overrideProps): dodexFirebase;
            //         isUsingFirebase = true;
            //     return (T) dodexFirebase;
            // }
        } catch (Exception exception) { 
            throw exception;
        }
        return (T) handicapDatabase;
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