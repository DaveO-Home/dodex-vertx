package dmo.fs.dbh;

import com.fasterxml.jackson.databind.JsonNode;
import dmo.fs.utils.DodexUtils;
import io.reactivex.rxjava3.disposables.Disposable;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.jdbcclient.JDBCConnectOptions;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.jdbcclient.JDBCPool;
import io.vertx.sqlclient.PoolOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class HandicapDatabaseH2 extends DbH2 {
  private final static Logger logger =
      LoggerFactory.getLogger(HandicapDatabaseH2.class.getName());
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

  public HandicapDatabaseH2(Map<String, String> dbOverrideMap, Properties dbOverrideProps)
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

  public HandicapDatabaseH2() throws InterruptedException, IOException, SQLException {
    super();

    returnHandler.handle(returnPromise);
    returnFuture = Future.future(returnHandler);

    defaultNode = dodexUtil.getDefaultNode();
    webEnv = webEnv == null || "prod".equals(webEnv) ? "prod" : "dev";

    dbMap = dodexUtil.jsonNodeToMap(defaultNode, webEnv);
    dbProperties = dodexUtil.mapToProperties(dbMap);

    databaseSetup();
  }

  public HandicapDatabaseH2(Boolean isCreateTables) throws IOException {
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
      DbConfiguration.configureDefaults(dbMap, dbProperties);
    }
    PoolOptions poolOptions =
        new PoolOptions().setMaxSize(Runtime.getRuntime().availableProcessors() * 5);

    JDBCConnectOptions connectOptions;

    connectOptions = new JDBCConnectOptions()
        .setJdbcUrl(dbMap.get("url") + dbMap.get("filename") + ";DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE")
        .setUser(dbProperties.getProperty("user"))
        .setPassword(dbProperties.getProperty("password"))
        .setIdleTimeout(1)
    // .setCachePreparedStatements(true)
    ;
    Vertx vertx = DodexUtils.getVertx();
    if(vertx == null) { // for testing
      vertx = Vertx.vertx();
    }
    pool = JDBCPool.pool(vertx, connectOptions, poolOptions);
    if (!isCreateTables) {
      try {
        setupSql(pool);
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
