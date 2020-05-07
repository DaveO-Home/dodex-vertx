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
    
    SpaDbConfiguration() {
        super();
    }

    public static SpaDatabase getSpaDb() throws InterruptedException , IOException , SQLException {
        defaultDb = SpaUtil.getDefaultDb().toLowerCase();
        try {
            // if(defaultDb.equals("postgres") && dodexDatabase == null) {
            //     spaDatabase = new SpaDatabasePostgres();
            // } else 
            if(defaultDb.equals("sqlite3") && spaDatabase == null) {
                spaDatabase = new SpaDatabaseSqlite3();
            } 
            // else if(defaultDb.equals("cubrid") && dodexDatabase == null) {
            //     dodexDatabase = new DodexDatabaseCubrid();
            // } else if(defaultDb.equals("mariadb") && dodexDatabase == null) {
            //     dodexDatabase = new DodexDatabaseMariadb();
            // } else if(defaultDb.equals("ibmdb2") && dodexDatabase == null) {
            //     dodexDatabase = new DodexDatabaseIbmDB2();
            // }
        } catch (Exception exception) { 
            throw exception;
        }
        return spaDatabase;
    }

    public static SpaDatabase getSpaDb(Map<String, String>overrideMap, Properties overrideProps) 
            throws InterruptedException, IOException, SQLException {
        defaultDb = SpaUtil.getDefaultDb().toLowerCase();
        
        try {
            // if(defaultDb.equals("postgres") && spaDatabase == null) {
            //     spaDatabase = new SpaDatabasePostgres(overrideMap, overrideProps);
            // } else 
            if(defaultDb.equals("sqlite3") && spaDatabase == null) {
                spaDatabase = new SpaDatabaseSqlite3(overrideMap, overrideProps);
            } 
            // else if(defaultDb.equals("cubrid") && spaDatabase == null) {
            //     spaDatabase = new SpaDatabaseCubrid(overrideMap, overrideProps);
            // } else if(defaultDb.equals("mariadb") && spaDatabase == null) {
            //     spaDatabase = new SpaDatabaseMariadb(overrideMap, overrideProps);
            // } else if(defaultDb.equals("ibmdb2") && spaDatabase == null) {
            //     spaDatabase = new SpaDatabaseIbmDB2(overrideMap, overrideProps);
            // }
        } catch (Exception exception) { 
            throw exception;
        }
        
        return spaDatabase;
    }
}