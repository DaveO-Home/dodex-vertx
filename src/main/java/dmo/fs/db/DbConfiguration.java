package dmo.fs.db;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import dmo.fs.db.cassandra.DodexCassandra;
import dmo.fs.db.cassandra.DodexDbCassandra;
import dmo.fs.db.cubrid.DodexDatabaseCubrid;
import dmo.fs.db.db2.DodexDatabaseIbmDB2;
import dmo.fs.db.firebase.DodexDatabaseFirebase;
import dmo.fs.db.firebase.DodexFirebase;
import dmo.fs.db.h2.DodexDatabaseH2;
import dmo.fs.db.mariadb.DodexDatabaseMariadb;
import dmo.fs.db.mongodb.DodexDatabaseMongo;
import dmo.fs.db.mongodb.DodexMongo;
import dmo.fs.db.neo4j.DodexDatabaseNeo4j;
import dmo.fs.db.neo4j.DodexNeo4j;
import dmo.fs.db.postgres.DodexDatabasePostgres;
import dmo.fs.db.sqlite3.DodexDatabaseSqlite3;
import dmo.fs.utils.DodexUtil;

public abstract class DbConfiguration {

    private static final Map<String, String> map = new ConcurrentHashMap<>();
    protected static Properties properties = new Properties();

    private static Boolean isUsingSqlite3 = false;
    private static Boolean isUsingPostgres = false;
    private static Boolean isUsingCubrid = false;
    private static Boolean isUsingMariadb = false;
    private static Boolean isUsingIbmDB2 = false;
    private static Boolean isUsingCassandra = false;
    private static Boolean isUsingFirebase = false;
    private static Boolean isUsingNeo4j = false;
    private static Boolean isUsingH2 = false;
    private static Boolean isUsingMongo = false;
    private static String defaultDb = "h2";
    private static final DodexUtil dodexUtil = new DodexUtil();
    private static DodexDatabase dodexDatabase;
    private static DodexCassandra dodexCassandra;
    private static DodexFirebase dodexFirebase;
    private static DodexNeo4j dodexNeo4j;
    private static DodexMongo dodexMongo;

    private enum DbTypes {
        POSTGRES("postgres"), SQLITE3("sqlite3"), CUBRID("cubrid"), MARIADB("mariadb"),
                IBMDB2("ibmdb2"), CASSANDRA("cassandra"), FIREBASE("firebase"),
                NEO4J("neo4j"), H2("h2"), MONGO("mongo");

        final String db;

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

    public static boolean isUsingNeo4j() {
        return isUsingNeo4j;
    }

    public static boolean isUsingH2() {
        return isUsingH2;
    }

    public static boolean isUsingMongo() {
        return isUsingMongo;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getDefaultDb() throws InterruptedException, IOException, SQLException {
        defaultDb = dodexUtil.getDefaultDb().toLowerCase();
      if (defaultDb.equals(DbTypes.POSTGRES.db) && dodexDatabase == null) {
          dodexDatabase = new DodexDatabasePostgres();
          isUsingPostgres = true;
      } else if (defaultDb.equals(DbTypes.SQLITE3.db) && dodexDatabase == null) {
          dodexDatabase = new DodexDatabaseSqlite3();
          isUsingSqlite3 = true;
      } else if (defaultDb.equals(DbTypes.CUBRID.db) && dodexDatabase == null) {
          dodexDatabase = new DodexDatabaseCubrid();
          isUsingCubrid = true;
      } else if (defaultDb.equals(DbTypes.MARIADB.db) && dodexDatabase == null) {
          dodexDatabase = new DodexDatabaseMariadb();
          isUsingMariadb = true;
      } else if (defaultDb.equals(DbTypes.IBMDB2.db) && dodexDatabase == null) {
          dodexDatabase = new DodexDatabaseIbmDB2();
          isUsingIbmDB2 = true;
      } else if (defaultDb.equals(DbTypes.H2.db) && dodexDatabase == null) {
          dodexDatabase = new DodexDatabaseH2();
          isUsingH2 = true;
      } else if (defaultDb.equals(DbTypes.CASSANDRA.db)) {
          dodexCassandra =
                  dodexCassandra == null ? new DodexDbCassandra() : dodexCassandra;
          isUsingCassandra = true;
          return (T) dodexCassandra;
      } else if (defaultDb.equals(DbTypes.FIREBASE.db)) {
          dodexFirebase = dodexFirebase == null ? new DodexDatabaseFirebase() : dodexFirebase;
          isUsingFirebase = true;
          return (T) dodexFirebase;
      }  else if(defaultDb.equals(DbTypes.NEO4J.db)) {
          dodexNeo4j = new DodexDatabaseNeo4j();
          isUsingNeo4j = true;
          return (T) dodexNeo4j;
      }  else if(defaultDb.equals(DbTypes.MONGO.db)) {
          dodexMongo = new DodexDatabaseMongo();
          isUsingMongo = true;
          return (T) dodexMongo;
      }

      return (T) dodexDatabase;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getDefaultDb(Map<String, String> overrideMap, Properties overrideProps)
            throws InterruptedException, IOException, SQLException {
        defaultDb = dodexUtil.getDefaultDb();

      if (defaultDb.equals(DbTypes.POSTGRES.db) && dodexDatabase == null) {
          dodexDatabase = new DodexDatabasePostgres(overrideMap, overrideProps);
          isUsingPostgres = true;
      } else if (defaultDb.equals(DbTypes.SQLITE3.db) && dodexDatabase == null) {
          dodexDatabase = new DodexDatabaseSqlite3(overrideMap, overrideProps);
          isUsingSqlite3 = true;
      } else if (defaultDb.equals(DbTypes.CUBRID.db) && dodexDatabase == null) {
          dodexDatabase = new DodexDatabaseCubrid(overrideMap, overrideProps);
          isUsingCubrid = true;
      } else if (defaultDb.equals(DbTypes.MARIADB.db) && dodexDatabase == null) {
          dodexDatabase = new DodexDatabaseMariadb(overrideMap, overrideProps);
          isUsingMariadb = true;
      } else if (defaultDb.equals(DbTypes.IBMDB2.db) && dodexDatabase == null) {
          dodexDatabase = new DodexDatabaseIbmDB2(overrideMap, overrideProps);
          isUsingIbmDB2 = true;
      } else if (defaultDb.equals(DbTypes.H2.db) && dodexDatabase == null) {
          dodexDatabase = new DodexDatabaseH2();
          isUsingH2 = true;
      } else if (defaultDb.equals(DbTypes.CASSANDRA.db)) {
          dodexCassandra = dodexCassandra == null
                  ? new DodexDbCassandra(overrideMap, overrideProps)
                  : dodexCassandra;
          isUsingCassandra = true;
          return (T) dodexCassandra;
      } else if (defaultDb.equals(DbTypes.FIREBASE.db)) {
          dodexFirebase = dodexFirebase == null
                  ? new DodexDatabaseFirebase(overrideMap, overrideProps)
                  : dodexFirebase;
          isUsingFirebase = true;
          return (T) dodexFirebase;
      } else if(defaultDb.equals(DbTypes.NEO4J.db)) {
          dodexNeo4j = new DodexDatabaseNeo4j(overrideMap, overrideProps);
          isUsingNeo4j = true;
          return (T) dodexNeo4j;
      } else if(defaultDb.equals(DbTypes.MONGO.db)) {
          dodexMongo = new DodexDatabaseMongo(overrideMap, overrideProps);
          isUsingMongo = true;
          return (T) dodexMongo;
      }
      return (T) dodexDatabase;
    }

    public static void configureDefaults(Map<String, String> overrideMap,
            Properties overrideProps) {
        if (overrideProps != null && !overrideProps.isEmpty()) {
            properties = overrideProps;
        }
        mapMerge(map, overrideMap);
    }

    public static void configureTestDefaults(Map<String, String> overrideMap,
            Properties overrideProps) {
        if (overrideProps != null && !overrideProps.isEmpty()) {
            properties = overrideProps;
        }
        mapMerge(map, overrideMap);

    }

    public static void mapMerge(Map<String, String> map1, Map<String, String> map2) {
        map2.forEach((key, value) -> map1.merge(key, value, (v1, v2) -> v2)); // let duplicate key
                                                                              // in map2 win
    }

}
