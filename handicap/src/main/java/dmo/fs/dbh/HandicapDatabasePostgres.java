package dmo.fs.dbh;

import com.fasterxml.jackson.databind.JsonNode;
import dmo.fs.utils.DodexUtil;
import golf.handicap.vertx.MainVerticle;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.vertx.core.Future;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.rxjava3.core.Promise;
import io.vertx.rxjava3.pgclient.PgPool;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.RowIterator;
import io.vertx.rxjava3.sqlclient.RowSet;
import io.vertx.sqlclient.PoolOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HandicapDatabasePostgres extends DbPostgres {
  private final static Logger logger =
      LoggerFactory.getLogger(HandicapDatabasePostgres.class.getName());
  protected Disposable disposable;
  protected PgPool pool4;
  protected Properties dbProperties = new Properties();
  protected Map<String, String> dbOverrideMap = new ConcurrentHashMap<>();
  protected Map<String, String> dbMap = new ConcurrentHashMap<>();
  protected JsonNode defaultNode;
  protected String webEnv = System.getenv("VERTXWEB_ENVIRONMENT");
  protected DodexUtil dodexUtil = new DodexUtil();
  protected Boolean isCreateTables = false;
  protected Promise<String> returnPromise = Promise.promise();

  public HandicapDatabasePostgres(Map<String, String> dbOverrideMap, Properties dbOverrideProps)
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

    assert dbOverrideMap != null;
    DbConfiguration.mapMerge(dbMap, dbOverrideMap);
    databaseSetup();
  }

  public HandicapDatabasePostgres() throws InterruptedException, IOException, SQLException {
    super();

    defaultNode = dodexUtil.getDefaultNode();
    webEnv = webEnv == null || "prod".equals(webEnv) ? "prod" : "dev";

    dbMap = dodexUtil.jsonNodeToMap(defaultNode, webEnv);
    dbProperties = dodexUtil.mapToProperties(dbMap);

    dbProperties.setProperty("foreign_keys", "true");

    databaseSetup();
  }

  public HandicapDatabasePostgres(Boolean isCreateTables)
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

    PgConnectOptions connectOptions;

    connectOptions = new PgConnectOptions().setHost(dbMap.get("host2"))
        .setPort(Integer.parseInt(dbMap.get("port")))
        .setUser(dbProperties.getProperty("user").toString())
        .setPassword(dbProperties.getProperty("password").toString())
        .setDatabase(dbMap.get("database")).setSsl(Boolean.parseBoolean(dbProperties.getProperty("ssl")))
        .setIdleTimeout(1)
    // .setCachePreparedStatements(true)
    ;

    PoolOptions poolOptions =
        new PoolOptions().setMaxSize(Runtime.getRuntime().availableProcessors() * 5);

    pool4 = PgPool.pool(DodexUtil.getVertx(), connectOptions, poolOptions);

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
