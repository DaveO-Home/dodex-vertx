package dmo.fs.spa.db;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import dmo.fs.db.DbConfiguration;
import dmo.fs.spa.utils.SpaUtil;

public class SpaDbConfiguration extends DbConfiguration {
    private static String defaultDb = "sqlite3";
    private static SpaDatabase spaDatabase4;
    private static SpaCassandra spaCassandra;
    
    private enum DbTypes {
        POSTGRES("postgres"),
        SQLITE3("sqlite3"),
        CUBRID("cubrid"),
        MARIADB("mariadb"),
        IBMDB2("ibmdb2"),
        CASSANDRA("cassandra");

        String db;

        DbTypes(String db) {
            this.db = db;
        }
    };

    SpaDbConfiguration() {
        super();
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T getSpaDb() throws InterruptedException , IOException , SQLException {
        defaultDb = SpaUtil.getDefaultDb().toLowerCase();

        try {
            if(defaultDb.equals(DbTypes.POSTGRES.db) && spaDatabase4 == null) {
                spaDatabase4 = new SpaDatabasePostgres();
            } else if(defaultDb.equals(DbTypes.SQLITE3.db) && spaDatabase4 == null) {
                spaDatabase4 = new SpaDatabaseSqlite3();
            } else if(defaultDb.equals(DbTypes.CUBRID.db) && spaDatabase4 == null) {
                spaDatabase4 = new SpaDatabaseCubrid();
            } else if(defaultDb.equals(DbTypes.MARIADB.db) && spaDatabase4 == null) {
                spaDatabase4 = new SpaDatabaseMariadb();
            } else if(defaultDb.equals(DbTypes.IBMDB2.db) && spaDatabase4 == null) {
                spaDatabase4 = new SpaDatabaseIbmDB2();
            }  else if(defaultDb.equals(DbTypes.CASSANDRA.db)) {
                spaCassandra = spaCassandra == null? new SpaDatabaseCassandra(): spaCassandra;
                return (T) spaCassandra;
            }
        } catch (Exception exception) { 
            throw exception;
        }
        return (T) spaDatabase4;
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T getSpaDb(Map<String, String>overrideMap, Properties overrideProps) 
            throws InterruptedException, IOException, SQLException {
        defaultDb = SpaUtil.getDefaultDb().toLowerCase();
        
        try {
            if(defaultDb.equals(DbTypes.POSTGRES.db) && spaDatabase4 == null) {
                spaDatabase4 = new SpaDatabasePostgres(overrideMap, overrideProps);
            } else if(defaultDb.equals(DbTypes.SQLITE3.db) && spaDatabase4 == null) {
                spaDatabase4 = new SpaDatabaseSqlite3(overrideMap, overrideProps);
            } else if(defaultDb.equals(DbTypes.CUBRID.db) && spaDatabase4 == null) {
                spaDatabase4 = new SpaDatabaseCubrid(overrideMap, overrideProps);
            } else if(defaultDb.equals(DbTypes.MARIADB.db) && spaDatabase4 == null) {
                spaDatabase4 = new SpaDatabaseMariadb(overrideMap, overrideProps);
            } else if(defaultDb.equals(DbTypes.IBMDB2.db) && spaDatabase4 == null) {
                spaDatabase4 = new SpaDatabaseIbmDB2(overrideMap, overrideProps);
            } else if(defaultDb.equals(DbTypes.CASSANDRA.db)) {
                spaCassandra = spaCassandra == null? new SpaDatabaseCassandra(overrideMap, overrideProps): spaCassandra;
                return (T) spaCassandra;
            }
        } catch (Exception exception) { 
            throw exception;
        }
        
        return (T) spaDatabase4;
    }
}