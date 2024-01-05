package dmo.fs.dbg;

import dmo.fs.utils.DodexUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public abstract class DbConfiguration {
    private static final Map<String, String> map = new ConcurrentHashMap<>();
    protected static Properties properties = new Properties();
    private static Boolean isUsingSqlite3 = false;
    private static Boolean isUsingPostgres = false;
    private static Boolean isUsingMariadb = false;
    private static Boolean isUsingH2 = false;
    private static String defaultDb = "h2";
    private static final DodexUtil dodexUtil = new DodexUtil();
    private static HandicapDatabaseG handicapDatabase;

    private enum DbTypes {
        POSTGRES("postgres"),
        SQLITE3("sqlite3"),
        MARIADB("mariadb"),
        H2("h2");

        final String db;

        DbTypes(String db) {
            this.db = db;
        }
    }

    public static boolean isUsingSqlite3() {
        return isUsingSqlite3;
    }

    public static boolean isUsingPostgres() {
        return isUsingPostgres;
    }

    public static boolean isUsingMariadb() {
        return isUsingMariadb;
    }

    public static boolean isUsingH2() {
        return isUsingH2;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getDefaultDb() throws InterruptedException, IOException, SQLException {
        defaultDb = dodexUtil.getDefaultDb().toLowerCase();
      if(defaultDb.equals(DbTypes.POSTGRES.db) && handicapDatabase == null) {
          handicapDatabase = new HandicapDatabasePostgresG();
          isUsingPostgres = true;
      } else
      if(defaultDb.equals(DbTypes.SQLITE3.db) && handicapDatabase == null) {
          handicapDatabase = new HandicapDatabaseSqlite3G();
          isUsingSqlite3 = true;
      } else if(defaultDb.equals(DbTypes.MARIADB.db) && handicapDatabase == null) {
           handicapDatabase = new HandicapDatabaseMariadbG();
           isUsingMariadb = true;
       } else if(defaultDb.equals(DbTypes.H2.db) && handicapDatabase == null) {
          handicapDatabase = new HandicapDatabaseH2G();
          isUsingH2 = true;
      }
      return (T) handicapDatabase;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getDefaultDb(Boolean isCreateTables) throws InterruptedException, IOException, SQLException {
        defaultDb = dodexUtil.getDefaultDb().toLowerCase();
      if(defaultDb.equals(DbTypes.POSTGRES.db) /*&& handicapDatabase == null*/ && isCreateTables) {
          handicapDatabase = new HandicapDatabasePostgresG(isCreateTables);
          isUsingPostgres = true;
      } else
      if(defaultDb.equals(DbTypes.SQLITE3.db) /*&& handicapDatabase == null*/ && isCreateTables) {
          handicapDatabase = new HandicapDatabaseSqlite3G(isCreateTables);
          isUsingSqlite3 = true;
      }
      else if(defaultDb.equals(DbTypes.MARIADB.db) /* && handicapDatabase == null*/ && isCreateTables) {
          handicapDatabase = new HandicapDatabaseMariadbG(isCreateTables);
          isUsingMariadb = true;
      } else if(defaultDb.equals(DbTypes.H2.db) /* && handicapDatabase == null*/ && isCreateTables) {
          handicapDatabase = new HandicapDatabaseH2G(isCreateTables);
          isUsingH2 = true;
      }
      return (T) handicapDatabase;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getDefaultDb(Map<String, String> overrideMap, Properties overrideProps)
            throws InterruptedException, IOException, SQLException {
        defaultDb = dodexUtil.getDefaultDb();

      if(defaultDb.equals(DbTypes.POSTGRES.db) && handicapDatabase == null) {
          handicapDatabase = new HandicapDatabasePostgresG(overrideMap, overrideProps);
          isUsingPostgres = true;
      } else
      if(defaultDb.equals(DbTypes.SQLITE3.db) && handicapDatabase == null) {
          handicapDatabase = new HandicapDatabaseSqlite3G(overrideMap, overrideProps);
          isUsingSqlite3 = true;
      }
      else if(defaultDb.equals(DbTypes.MARIADB.db) && handicapDatabase == null) {
          handicapDatabase = new HandicapDatabaseMariadbG(overrideMap, overrideProps);
          isUsingMariadb = true;
      } else if(defaultDb.equals(DbTypes.H2.db) && handicapDatabase == null) {
          handicapDatabase = new HandicapDatabaseH2G(overrideMap, overrideProps);
          isUsingH2 = true;
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