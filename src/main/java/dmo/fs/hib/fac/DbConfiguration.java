package dmo.fs.hib.fac;

import dmo.fs.db.mssql.DodexDatabaseMssql;
import dmo.fs.db.ora.DodexDatabaseOracle;
import dmo.fs.hib.DodexDatabase;
import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public abstract class DbConfiguration {
  private final static Logger logger = LoggerFactory.getLogger(DbConfiguration.class.getName());

  public final static String pu = "oracle.dev";

  private static final Map<String, String> map = new ConcurrentHashMap<>();
  protected static Properties properties = new Properties();
  private static Boolean isUsingOracle = false;
  private static Boolean isUsingMssql = false;
  private static String defaultDb = "oracle";
  private static final DodexUtil dodexUtil = new DodexUtil();
  private static DodexDatabase dodexDatabase;

  public enum DbTypes {
    ORACLE("oracle"), MSSQL("mssql"), DEFAULT("");

    public final String db;

    DbTypes(String db) {
      this.db = db;
    }
  }

  public static boolean isUsingOracle() {
    return isUsingOracle;
  }

  public static boolean isUsingMssql() {
    return isUsingMssql;
  }

  public DbConfiguration() {
  }

  @SuppressWarnings("unchecked")
  public static <T> T getDefaultDb() throws InterruptedException, IOException, SQLException {
    defaultDb = dodexUtil.getDefaultDb().toLowerCase();
    String mode = DodexUtil.getMode();
    String dev = "dev";

    if (dodexDatabase == null) {
      logger.info("{}Persistence Unit: {}.{}{}", ColorUtilConstants.BLUE_BRIGHT, defaultDb, mode, ColorUtilConstants.RESET);
    }

    if (defaultDb.equals(DbTypes.ORACLE.db)) {
      dodexDatabase = new DodexDatabaseOracle();
      dodexDatabase.entityManagerSetup();
      isUsingOracle = true;
    } else if (defaultDb.equals(DbTypes.MSSQL.db)) {
      dodexDatabase = new DodexDatabaseMssql();
      isUsingMssql = true;
    }


    return (T) dodexDatabase;
  }

  public static String getDb() {
    return defaultDb;
  }
}
