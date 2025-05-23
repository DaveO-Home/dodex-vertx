package dmo.fs.dbh;

import com.fasterxml.jackson.databind.JsonNode;
import dmo.fs.utils.DodexUtils;
import io.reactivex.rxjava3.disposables.Disposable;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.jdbcclient.JDBCConnectOptions;
import io.vertx.rxjava3.jdbcclient.JDBCPool;
import io.vertx.sqlclient.PoolOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class HandicapDatabaseSqlite3 extends DbSqlite3 {
  private final static Logger logger =
      LoggerFactory.getLogger(HandicapDatabaseSqlite3.class.getName());
  protected Disposable disposable;
  protected Properties dbProperties = new Properties();
  protected Map<String, String> dbOverrideMap = new ConcurrentHashMap<>();
  protected Map<String, String> dbMap = new ConcurrentHashMap<>();
  protected JsonNode defaultNode;
  protected String webEnv = System.getenv("VERTXWEB_ENVIRONMENT");
  protected DodexUtils dodexUtil = new DodexUtils();
  protected JDBCPool pool4;
  protected Boolean isCreateTables = false;
  protected Promise<String> returnPromise = Promise.promise();
  protected Future<String> returnFuture;
  protected Handler<Promise<String>> returnHandler = new Handler<>() {
    @Override
    public void handle(io.vertx.core.Promise<String> p) {
      p.complete(p.future().result());
    }
  };

  public HandicapDatabaseSqlite3(Map<String, String> dbOverrideMap, Properties dbOverrideProps)
      throws InterruptedException, IOException, SQLException {
    super();

    returnHandler.handle(returnPromise);
    returnFuture = Future.future(returnHandler);

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

  public HandicapDatabaseSqlite3() throws InterruptedException, IOException, SQLException {
    super();

    returnHandler.handle(returnPromise);
    returnFuture = Future.future(returnHandler);

    defaultNode = dodexUtil.getDefaultNode();
    webEnv = webEnv == null || "prod".equals(webEnv) ? "prod" : "dev";

    dbMap = dodexUtil.jsonNodeToMap(defaultNode, webEnv);
    dbProperties = dodexUtil.mapToProperties(dbMap);

    dbProperties.setProperty("foreign_keys", "true");

    databaseSetup();
  }

  public HandicapDatabaseSqlite3(Boolean isCreateTables) throws IOException {
    super();

    returnHandler.handle(returnPromise);
    returnFuture = Future.future(returnHandler);

    defaultNode = dodexUtil.getDefaultNode();
    webEnv = webEnv == null || "prod".equals(webEnv) ? "prod" : "dev";

    dbMap = dodexUtil.jsonNodeToMap(defaultNode, webEnv);
    dbProperties = dodexUtil.mapToProperties(dbMap);

    dbProperties.setProperty("foreign_keys", "true");
    this.isCreateTables = isCreateTables;
  }

  public Future<String> checkOnTables() throws InterruptedException, SQLException {
    databaseSetup();
    return returnFuture;
  }

  private void databaseSetup() {
    Promise<String> finalPromise = Promise.promise();
    if ("dev".equals(webEnv)) {
      DbConfiguration.configureTestDefaults(dbMap, dbProperties);
    } else {
      DbConfiguration.configureDefaults(dbMap, dbProperties); // Using prod (./dodex.db)
    }
    PoolOptions poolOptions =
        new PoolOptions().setMaxSize(Runtime.getRuntime().availableProcessors() * 5);

    JDBCConnectOptions connectOptions;

    connectOptions = new JDBCConnectOptions()
        .setJdbcUrl(dbMap.get("url") + dbMap.get("filename") + "?foreign_keys=on;")
        .setIdleTimeout(1)
    // .setCachePreparedStatements(true)
    ;
    pool = JDBCPool.pool(DodexUtils.getVertx(), connectOptions, poolOptions);

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
  public <T> T getPool() {
    return (T) pool;
  }
}
