package dmo.fs.spa.db;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import dmo.fs.db.DbConfiguration;
import dmo.fs.spa.utils.SpaUtil;

public class SpaDbConfiguration extends DbConfiguration {
    private static String defaultDb = "sqlite3";
    private static SpaDatabase spaDatabase;
    
    private enum DbTypes {
        POSTGRES("postgres"),
        SQLITE3("sqlite3"),
        CUBRID("cubrid"),
        MARIADB("mariadb"),
        IBMDB2("ibmdb2");

        String db;

        DbTypes(String db) {
            this.db = db;
        }
    };

    SpaDbConfiguration() {
        super();
    }

    public static SpaDatabase getSpaDb() throws InterruptedException , IOException , SQLException {
        defaultDb = SpaUtil.getDefaultDb().toLowerCase();
        try {
            if(defaultDb.equals(DbTypes.POSTGRES.db) && spaDatabase == null) {
                spaDatabase = new SpaDatabasePostgres();
            } else if(defaultDb.equals(DbTypes.SQLITE3.db) && spaDatabase == null) {
                spaDatabase = new SpaDatabaseSqlite3();
            } else if(defaultDb.equals(DbTypes.CUBRID.db) && spaDatabase == null) {
                spaDatabase = new SpaDatabaseCubrid();
            } else if(defaultDb.equals(DbTypes.MARIADB.db) && spaDatabase == null) {
                spaDatabase = new SpaDatabaseMariadb();
            } else if(defaultDb.equals(DbTypes.IBMDB2.db) && spaDatabase == null) {
                spaDatabase = new SpaDatabaseIbmDB2();
            }
        } catch (Exception exception) { 
            throw exception;
        }
        return spaDatabase;
    }

    public static SpaDatabase getSpaDb(Map<String, String>overrideMap, Properties overrideProps) 
            throws InterruptedException, IOException, SQLException {
        defaultDb = SpaUtil.getDefaultDb().toLowerCase();
        
        try {
            if(defaultDb.equals(DbTypes.POSTGRES.db) && spaDatabase == null) {
                spaDatabase = new SpaDatabasePostgres(overrideMap, overrideProps);
            } else if(defaultDb.equals(DbTypes.SQLITE3.db) && spaDatabase == null) {
                spaDatabase = new SpaDatabaseSqlite3(overrideMap, overrideProps);
            } else if(defaultDb.equals(DbTypes.CUBRID.db) && spaDatabase == null) {
                spaDatabase = new SpaDatabaseCubrid(overrideMap, overrideProps);
            } else if(defaultDb.equals(DbTypes.MARIADB.db) && spaDatabase == null) {
                spaDatabase = new SpaDatabaseMariadb(overrideMap, overrideProps);
            } else if(defaultDb.equals(DbTypes.IBMDB2.db) && spaDatabase == null) {
                spaDatabase = new SpaDatabaseIbmDB2(overrideMap, overrideProps);
            }
        } catch (Exception exception) { 
            throw exception;
        }
        
        return spaDatabase;
    }
}