package dmo.fs.dbh;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dmo.fs.utils.DodexUtils;
import dmo.fs.dbh.ora.DodexDatabaseOracle;
import dmo.fs.dbh.mssql.DodexDatabaseMssql;

public abstract class DbConfiguration {
    static Logger logger = LoggerFactory.getLogger(DbConfiguration.class.getName());
    private static final Map<String, String> map = new ConcurrentHashMap<>();
    protected static Properties properties = new Properties();

    private static Boolean isUsingSqlite3 = false;
    private static Boolean isUsingPostgres = false;
    private static Boolean isUsingCubrid = false;
    private static Boolean isUsingMariadb = false;
    private static Boolean isUsingIbmDB2 = false;
    private static Boolean isUsingCassandra = false;
    private static Boolean isUsingFirebase = false;
    private static Boolean isUsingH2 = false;
    private static Boolean isUsingOracle = false;
    private static Boolean isUsingMssql = false;
    private static String defaultDb = "h2";
    private static final DodexUtils dodexUtil = new DodexUtils();
    private static HandicapDatabase handicapDatabase;

    private enum DbTypes {
        POSTGRES("postgres"),
        SQLITE3("sqlite3"),
        CUBRID("cubrid"),
        MARIADB("mariadb"),
        IBMDB2("ibmdb2"),
        CASSANDRA("cassandra"),
        FIREBASE("firebase"),
        ORACLE("oracle"),
        MSSQL("mssql"),
        H2("h2");

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

    public static boolean isUsingOracle() {
        return isUsingOracle;
    }

    public static boolean isUsingMssql() {
        return isUsingMssql;
    }

    public static boolean isUsingH2() {
        return isUsingH2;
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
            else
            if(defaultDb.equals(DbTypes.H2.db) && handicapDatabase == null) {
                handicapDatabase = new HandicapDatabaseH2();
                isUsingH2 = true;
            }
            else if(defaultDb.equals(DbTypes.MARIADB.db) && handicapDatabase == null) {
                handicapDatabase = new HandicapDatabaseMariadb();
                isUsingMariadb = true;
            } else if(defaultDb.equals(DbTypes.ORACLE.db) && handicapDatabase == null) {
                DodexDatabaseOracle handicapDatabase = new DodexDatabaseOracle();
                isUsingOracle = true;
                return (T) handicapDatabase;
            } else if(defaultDb.equals(DbTypes.MSSQL.db) && handicapDatabase == null) {
                DodexDatabaseMssql handicapDatabase = new DodexDatabaseMssql();
                isUsingMssql = true;
                return (T) handicapDatabase;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            throw exception;
        }

        return (T) handicapDatabase;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getDefaultDb(Boolean isCreateTables) throws InterruptedException, IOException, SQLException {
        defaultDb = dodexUtil.getDefaultDb().toLowerCase();
        try {
            if(defaultDb.equals(DbTypes.POSTGRES.db) && isCreateTables) {
                handicapDatabase = new HandicapDatabasePostgres();
                isUsingPostgres = true;
            } else 
            if(defaultDb.equals(DbTypes.SQLITE3.db)  && isCreateTables) {
                handicapDatabase = new HandicapDatabaseSqlite3(isCreateTables);
                isUsingSqlite3 = true;
            } else
            if(defaultDb.equals(DbTypes.H2.db) /*&& handicapDatabase == null*/ && isCreateTables) {
                handicapDatabase = new HandicapDatabaseH2(isCreateTables);
                isUsingH2= true;
            }
            else if(defaultDb.equals(DbTypes.MARIADB.db) && isCreateTables) {
                handicapDatabase = new HandicapDatabaseMariadb(isCreateTables);
                isUsingMariadb = true;
            } 
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
            } else
            if(defaultDb.equals(DbTypes.H2.db) && handicapDatabase == null) {
                handicapDatabase = new HandicapDatabaseH2(overrideMap, overrideProps);
                isUsingH2 = true;
            }
            else if(defaultDb.equals(DbTypes.MARIADB.db) && handicapDatabase == null) {
                handicapDatabase = new HandicapDatabaseMariadb(overrideMap, overrideProps);
                isUsingMariadb = true;
            }
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