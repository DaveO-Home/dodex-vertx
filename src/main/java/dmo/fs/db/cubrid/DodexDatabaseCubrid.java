package dmo.fs.db.cubrid;

import com.fasterxml.jackson.databind.JsonNode;
import dmo.fs.db.DbConfiguration;
import dmo.fs.db.MessageUser;
import dmo.fs.db.MessageUserImpl;
import dmo.fs.utils.DodexUtil;
import dmo.fs.vertx.Server;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.vertx.jdbcclient.JDBCConnectOptions;
import io.vertx.rxjava3.jdbcclient.JDBCPool;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.RowIterator;
import io.vertx.rxjava3.sqlclient.RowSet;
import io.vertx.sqlclient.PoolOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class DodexDatabaseCubrid extends DbCubrid {
  private final static Logger logger = LoggerFactory.getLogger(DodexDatabaseCubrid.class.getName());
  protected Disposable disposable;
  protected Properties dbProperties = new Properties();
  protected Map<String, String> dbOverrideMap = new ConcurrentHashMap<>();
  protected Map<String, String> dbMap = new ConcurrentHashMap<>();
  protected JsonNode defaultNode;
  protected String webEnv = System.getenv("VERTXWEB_ENVIRONMENT");
  protected DodexUtil dodexUtil = new DodexUtil();

  public DodexDatabaseCubrid(Map<String, String> dbOverrideMap, Properties dbOverrideProps)
      throws InterruptedException, IOException, SQLException {
    super();

    defaultNode = dodexUtil.getDefaultNode();

    webEnv = webEnv == null || "prod".equals(webEnv) ? "prod" : "dev";

    dbMap = dodexUtil.jsonNodeToMap(defaultNode, webEnv);
    dbProperties = dodexUtil.mapToProperties(dbMap);

    if (dbOverrideProps != null) {
      this.dbProperties = dbOverrideProps;
    }
    if (dbOverrideMap != null) {
      this.dbOverrideMap = dbOverrideMap;
    }

    DbConfiguration.mapMerge(dbMap, dbOverrideMap);
    databaseSetup();
  }

  public DodexDatabaseCubrid() throws InterruptedException, IOException, SQLException {
    super();

    defaultNode = dodexUtil.getDefaultNode();
    webEnv = webEnv == null || "prod".equals(webEnv) ? "prod" : "dev";

    dbMap = dodexUtil.jsonNodeToMap(defaultNode, webEnv);
    dbProperties = dodexUtil.mapToProperties(dbMap);

    databaseSetup();
  }

  private void databaseSetup() throws InterruptedException, SQLException {
    // Override default credentials
    // dbProperties.setProperty("user", "myUser");
    // dbProperties.setProperty("password", "myPassword");
    // dbProperties.setProperty("ssl", "false");

    if ("dev".equals(webEnv)) {
      // dbMap.put("dbname", "/myDbname"); // this wiil be merged into the default map
      DbConfiguration.configureTestDefaults(dbMap, dbProperties);
    } else {
      DbConfiguration.configureDefaults(dbMap, dbProperties); // Prod
    }

    // CUBRIDDriver driver = new cubrid.jdbc.driver.CUBRIDDriver();
    // DriverManager.registerDriver(driver);

    PoolOptions poolOptions =
        new PoolOptions().setMaxSize(Runtime.getRuntime().availableProcessors() * 5);

    JDBCConnectOptions connectOptions;

    connectOptions = new JDBCConnectOptions()
        .setJdbcUrl(dbMap.get("url") + dbMap.get("host") + dbMap.get("dbname") + "?charSet=UTF-8")
        .setUser(dbProperties.getProperty("user").toString())
        .setPassword(dbProperties.getProperty("password").toString())
        // .setDatabase(dbMap.get("dbname")+"?charSet=utf8")
        // .setSsl(Boolean.valueOf(dbProperties.getProperty("ssl")))
        .setIdleTimeout(1)
    // .setCachePreparedStatements(true)
    ;

    pool = JDBCPool.pool(Server.getRxVertx(), connectOptions, poolOptions);

    Completable completable = pool.rxGetConnection().flatMapCompletable(conn -> conn.rxBegin()
        .flatMapCompletable(tx -> conn.query(CHECKUSERSQL).rxExecute().doOnSuccess(row -> {
          RowIterator<Row> ri = row.iterator();
          String val = null;

          while (ri.hasNext()) {
            val = ri.next().getString(0);
          }

          if (val == null) {
            final String usersSql = getCreateTable("USERS");

            Single<RowSet<Row>> crow = conn.query(usersSql).rxExecute().doOnError(err -> {
              logger.info(String.format("Users Table Error: %s", err.getCause().getMessage()));
            }).doOnSuccess(result -> {
              logger.info("Users Table Added.");
            });

            crow.subscribe(result -> {
              //
            }, err -> {
              logger.info(String.format("Users Table Error: %s", err.getMessage()));
            });
          }
        }).doOnError(err -> {
          logger.info(String.format("Users Table Error: %s", err.getMessage()));

        }).flatMap(result -> conn.query(CHECKMESSAGESQL).rxExecute().doOnSuccess(row -> {
          RowIterator<Row> ri = row.iterator();
          String val = null;
          while (ri.hasNext()) {
            val = ri.next().getString(0);
          }

          if (val == null) {
            final String sql = getCreateTable("MESSAGES");

            Single<RowSet<Row>> crow = conn.query(sql).rxExecute().doOnError(err -> {
              logger.info(String.format("Messages Table Error: %s", err.getMessage()));
            }).doOnSuccess(row2 -> {
              logger.info("Messages Table Added.");
            });

            crow.subscribe(res -> {
              //
            }, err -> {
              logger.info(String.format("Messages Table Error: %s", err.getMessage()));
            });
          }
        }).doOnError(err -> {
          logger.info(String.format("Messages Table Error: %s", err.getMessage()));

        })).flatMap(result -> conn.query(CHECKUNDELIVEREDSQL).rxExecute().doOnSuccess(row -> {
          RowIterator<Row> ri = row.iterator();
          String val = null;
          while (ri.hasNext()) {
            val = ri.next().getString(0);
          }

          if (val == null) {
            final String sql = getCreateTable("UNDELIVERED");

            Single<RowSet<Row>> crow = conn.query(sql).rxExecute().doOnError(err -> {
              logger.info(String.format("Undelivered Table Error: %s", err.getMessage()));
            }).doOnSuccess(row2 -> {
              logger.info("Undelivered Table Added.");
              tx.commit();
              conn.close().subscribe();
            });

            crow.subscribe(result2 -> {
              //
            }, err -> {
              logger.info(String.format("Messages Table Error: %s", err.getMessage()));
            });
          } else {
            tx.commit();
            conn.close().subscribe();
          }
        }).doOnError(err -> {
          logger.info(String.format("Messages Table Error: %s", err.getMessage()));
        })).flatMapCompletable(res -> Completable.complete())));

    completable.subscribe(() -> {
      try {
        setupSql(pool);
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }, err -> {
      logger.info(String.format("Tables Create Error: %s", err.getMessage()));
    });
  }

  @Override
  public MessageUser createMessageUser() {
    return new MessageUserImpl();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getPool() {
    return (T) pool;
  }
}
