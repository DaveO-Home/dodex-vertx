package dmo.fs.dbh.bld;

import dmo.fs.dbh.fac.DbConfiguration.DbTypes;
import dmo.fs.dbh.ora.DbOracle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Database {
  private final static Logger logger = LoggerFactory.getLogger(Database.class.getName());
  private final String mode;
  private final String dbName;
  private final String persistenceUnitName;
  private final DatabaseBuild databaseBuild;

  public String getdbName() {
    return dbName;
  }

  public String getMode() {
    return mode;
  }

  public String getPersistenceUnitName() {
    return persistenceUnitName;
  }

  public DatabaseBuild getDatabaseBuild() {
    return databaseBuild;
  }

  private Database(DatabaseBuilder builder) {
    this.dbName = builder.dbName;
    this.mode = builder.mode;
    this.persistenceUnitName = builder.persistenceUnitName;
    this.databaseBuild = builder.databaseBuild;
  }

  public static class DatabaseBuilder {
    private final String mode;
    private final String dbName;
    private final String persistenceUnitName;
    private DatabaseBuild databaseBuild;

    public DatabaseBuilder(String mode, String dbName, String persistenceUnitName) {
      this.mode = mode;
      this.dbName = dbName;
      this.persistenceUnitName = parseUnitName(persistenceUnitName);
    }

    public Database build() {
      if ((DbTypes.ORACLE.db).equals(persistenceUnitName)) {
        databaseBuild = new DbOracle();
      }
      return new Database(this);
    }

    private static String parseUnitName(String persistenceUnit) {
      return persistenceUnit.replaceFirst("(\\.dev|\\.prod)$", "");
    }
  }
}
