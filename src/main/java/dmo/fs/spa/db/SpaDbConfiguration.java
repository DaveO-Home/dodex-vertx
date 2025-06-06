package dmo.fs.spa.db;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

//import dmo.fs.dbh.DbHandicapConfiguration;
import dmo.fs.db.DbConfiguration;
import dmo.fs.spa.db.cassandra.SpaCassandra;
import dmo.fs.spa.db.cassandra.SpaDatabaseCassandra;
import dmo.fs.spa.db.cubrid.SpaDatabaseCubrid;
import dmo.fs.spa.db.db2.SpaDatabaseIbmDB2;
import dmo.fs.spa.db.firebase.SpaDatabaseFirebase;
import dmo.fs.spa.db.firebase.SpaFirebase;
import dmo.fs.spa.db.h2.SpaDatabaseH2;
import dmo.fs.spa.db.mariadb.SpaDatabaseMariadb;
import dmo.fs.spa.db.neo4j.SpaDatabaseNeo4j;
import dmo.fs.spa.db.neo4j.SpaNeo4j;
import dmo.fs.spa.db.postgres.SpaDatabasePostgres;
import dmo.fs.spa.db.sqlite3.SpaDatabaseSqlite3;
import dmo.fs.spa.utils.SpaUtil;

public class SpaDbConfiguration extends DbConfiguration {
    private static String defaultDb = "h2";
    private static SpaDatabase spaDatabase;
    private static SpaCassandra spaCassandra;
    private static SpaFirebase spaFirebase;
    private static SpaNeo4j spaNeo4j;
    
    private enum DbTypes {
        POSTGRES("postgres"),
        SQLITE3("sqlite3"),
        CUBRID("cubrid"),
        MARIADB("mariadb"),
        IBMDB2("ibmdb2"),
        CASSANDRA("cassandra"),
        FIREBASE("firebase"),
        NEO4J("neo4j"),
        H2("h2");

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
            if(defaultDb.equals(DbTypes.POSTGRES.db) && spaDatabase == null) {
                spaDatabase = new SpaDatabasePostgres();
            } else if(defaultDb.equals(DbTypes.SQLITE3.db) && spaDatabase == null) {
                spaDatabase = new SpaDatabaseSqlite3();
            } else if(defaultDb.equals(DbTypes.H2.db) && spaDatabase == null) {
                spaDatabase = new SpaDatabaseH2();
            } else if(defaultDb.equals(DbTypes.CUBRID.db) && spaDatabase == null) {
                spaDatabase = new SpaDatabaseCubrid();
            } else if(defaultDb.equals(DbTypes.MARIADB.db) && spaDatabase == null) {
                spaDatabase = new SpaDatabaseMariadb();
            } else if(defaultDb.equals(DbTypes.IBMDB2.db) && spaDatabase == null) {
                spaDatabase = new SpaDatabaseIbmDB2();
            }  else if(defaultDb.equals(DbTypes.CASSANDRA.db)) {
                spaCassandra = spaCassandra == null? new SpaDatabaseCassandra(): spaCassandra;
                return (T) spaCassandra;
            } else if(defaultDb.equals(DbTypes.FIREBASE.db)) {
                spaFirebase = spaFirebase == null? new SpaDatabaseFirebase(): spaFirebase;
                return (T) spaFirebase;
            } else if(defaultDb.equals(DbTypes.NEO4J.db)) {
                spaNeo4j = spaNeo4j == null? new SpaDatabaseNeo4j(): spaNeo4j;
                return (T) spaNeo4j;
            }
        } catch (Exception exception) { 
            throw exception;
        }
        return (T) spaDatabase;
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T getSpaDb(Map<String, String>overrideMap, Properties overrideProps) 
            throws InterruptedException, IOException, SQLException {
        defaultDb = SpaUtil.getDefaultDb().toLowerCase();
        
        try {
            if(defaultDb.equals(DbTypes.POSTGRES.db) && spaDatabase == null) {
                spaDatabase = new SpaDatabasePostgres(overrideMap, overrideProps);
            } else if(defaultDb.equals(DbTypes.SQLITE3.db) && spaDatabase == null) {
                spaDatabase = new SpaDatabaseSqlite3(overrideMap, overrideProps);
            } else if(defaultDb.equals(DbTypes.H2.db) && spaDatabase == null) {
                spaDatabase = new SpaDatabaseH2(overrideMap, overrideProps);
            } else if(defaultDb.equals(DbTypes.CUBRID.db) && spaDatabase == null) {
                spaDatabase = new SpaDatabaseCubrid(overrideMap, overrideProps);
            } else if(defaultDb.equals(DbTypes.MARIADB.db) && spaDatabase == null) {
                spaDatabase = new SpaDatabaseMariadb(overrideMap, overrideProps);
            } else if(defaultDb.equals(DbTypes.IBMDB2.db) && spaDatabase == null) {
                spaDatabase = new SpaDatabaseIbmDB2(overrideMap, overrideProps);
            } else if(defaultDb.equals(DbTypes.CASSANDRA.db)) {
                spaCassandra = spaCassandra == null? new SpaDatabaseCassandra(overrideMap, overrideProps): spaCassandra;
                return (T) spaCassandra;
            } else if(defaultDb.equals(DbTypes.FIREBASE.db)) {
                spaFirebase = spaFirebase == null? new SpaDatabaseFirebase(overrideMap, overrideProps): spaFirebase;
                return (T) spaFirebase;
            } else if(defaultDb.equals(DbTypes.NEO4J.db)) {
                spaNeo4j = spaNeo4j == null? new SpaDatabaseNeo4j(overrideMap, overrideProps): spaNeo4j;
                return (T) spaFirebase;
            }
        } catch (Exception exception) { 
            throw exception;
        }
        
        return (T) spaDatabase;
    }
}