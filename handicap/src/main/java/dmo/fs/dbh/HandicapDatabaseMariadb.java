package dmo.fs.dbh;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import dmo.fs.utils.DodexUtil;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.vertx.core.Future;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.rxjava3.core.Promise;
import io.vertx.rxjava3.mysqlclient.MySQLPool;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.RowIterator;
import io.vertx.rxjava3.sqlclient.RowSet;
import io.vertx.sqlclient.PoolOptions;

public class HandicapDatabaseMariadb extends DbMariadb {
  private final static Logger logger =
      LoggerFactory.getLogger(HandicapDatabaseMariadb.class.getName());
  protected Disposable disposable;
  protected MySQLPool pool4;
  protected Properties dbProperties = new Properties();
  protected Map<String, String> dbOverrideMap = new ConcurrentHashMap<>();
  protected Map<String, String> dbMap = new ConcurrentHashMap<>();
  protected JsonNode defaultNode;
  protected String webEnv = System.getenv("VERTXWEB_ENVIRONMENT");
  protected DodexUtil dodexUtil = new DodexUtil();
  protected Boolean isCreateTables = false;
  protected Promise<String> returnPromise = Promise.promise();

  public HandicapDatabaseMariadb(Map<String, String> dbOverrideMap, Properties dbOverrideProps)
      throws InterruptedException, IOException, SQLException {
    super();

    defaultNode = dodexUtil.getDefaultNode();

    webEnv = webEnv == null || "prod".equals(webEnv) ? "prod" : "dev";

    dbMap = dodexUtil.jsonNodeToMap(defaultNode, webEnv);
    dbProperties = dodexUtil.mapToProperties(dbMap);

    if (dbOverrideProps != null && dbOverrideProps.size() > 0) {
      this.dbProperties = dbOverrideProps;
    }
    if (dbOverrideMap != null) {
      this.dbOverrideMap = dbOverrideMap;
    }

    dbProperties.setProperty("foreign_keys", "true");

    DbConfiguration.mapMerge(dbMap, dbOverrideMap);
    databaseSetup();
  }

  public HandicapDatabaseMariadb() throws InterruptedException, IOException, SQLException {
    super();

    defaultNode = dodexUtil.getDefaultNode();
    webEnv = webEnv == null || "prod".equals(webEnv) ? "prod" : "dev";

    dbMap = dodexUtil.jsonNodeToMap(defaultNode, webEnv);
    dbProperties = dodexUtil.mapToProperties(dbMap);

    dbProperties.setProperty("foreign_keys", "true");

    databaseSetup();
  }

  public HandicapDatabaseMariadb(Boolean isCreateTables)
      throws InterruptedException, IOException, SQLException {
    super();
    defaultNode = dodexUtil.getDefaultNode();
    webEnv = webEnv == null || "prod".equals(webEnv) ? "prod" : "dev";

    dbMap = dodexUtil.jsonNodeToMap(defaultNode, webEnv);
    dbProperties = dodexUtil.mapToProperties(dbMap);

    dbProperties.setProperty("foreign_keys", "true");
    this.isCreateTables = isCreateTables;
  }

  public Future<String> checkOnTables() throws InterruptedException, SQLException {
    databaseSetup();
    return returnPromise.future();
  }

  private void databaseSetup() throws InterruptedException, SQLException {
    Promise<String> finalPromise = Promise.promise();
    if ("dev".equals(webEnv)) {
      DbConfiguration.configureTestDefaults(dbMap, dbProperties);
    } else {
      DbConfiguration.configureDefaults(dbMap, dbProperties);
    }

    MySQLConnectOptions connectOptions = new MySQLConnectOptions()
        .setPort(Integer.parseInt(dbMap.get("port"))).setHost(dbMap.get("host2"))
        .setDatabase(dbMap.get("database")).setUser(dbProperties.getProperty("user").toString())
        .setPassword(dbProperties.getProperty("password").toString())
        .setSsl(Boolean.parseBoolean(dbProperties.getProperty("ssl"))).setIdleTimeout(1)
        .setCharset("utf8mb4");

    // Pool options
    PoolOptions poolOptions =
        new PoolOptions().setMaxSize(Runtime.getRuntime().availableProcessors() * 5);

    // Create the client pool
    pool4 = MySQLPool.pool(DodexUtil.getVertx(), connectOptions, poolOptions);

    if (!isCreateTables) {
      try {
        setupSql(pool4);
      } catch (SQLException | IOException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <R> R getPool4() {
    return (R) pool4;
  }
}
