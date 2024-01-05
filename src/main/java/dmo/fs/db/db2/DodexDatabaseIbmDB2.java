package dmo.fs.db;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import dmo.fs.utils.DodexUtil;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.vertx.db2client.DB2ConnectOptions;
import io.vertx.rxjava3.db2client.DB2Pool;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.RowSet;
import io.vertx.sqlclient.PoolOptions;

public class DodexDatabaseIbmDB2 extends DbIbmDB2 {
  private final static Logger logger = LoggerFactory.getLogger(DodexDatabaseIbmDB2.class.getName());
  protected Disposable disposable;
  protected Properties dbProperties = new Properties();
  protected Map<String, String> dbOverrideMap = new ConcurrentHashMap<>();
  protected Map<String, String> dbMap = new ConcurrentHashMap<>();
  protected JsonNode defaultNode;
  protected String webEnv = System.getenv("VERTXWEB_ENVIRONMENT");
  protected DodexUtil dodexUtil = new DodexUtil();
  protected DB2Pool pool4;

  public DodexDatabaseIbmDB2(Map<String, String> dbOverrideMap, Properties dbOverrideProps)
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

  public DodexDatabaseIbmDB2() throws InterruptedException, IOException, SQLException {
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

    PoolOptions poolOptions =
        new PoolOptions().setMaxSize(Runtime.getRuntime().availableProcessors() * 5);

    DB2ConnectOptions connectOptions;

    connectOptions = new DB2ConnectOptions().setHost(dbMap.get("host2"))
        .setPort(Integer.valueOf(dbMap.get("port")))
        .setUser(dbProperties.getProperty("user").toString())
        .setPassword(dbProperties.getProperty("password").toString())
        .setDatabase(dbMap.get("database")).setSsl(Boolean.valueOf(dbProperties.getProperty("ssl")))
    // .setIdleTimeout(1) // You might need this to release connections
    // "db2 force applications all"
    // .setCachePreparedStatements(true)
    ;

    pool4 = DB2Pool.pool(DodexUtil.getVertx(), connectOptions, poolOptions);

    Completable completable = pool4.rxGetConnection()
        .flatMapCompletable(conn -> conn.rxBegin().flatMapCompletable(
            tx -> conn.query(CHECKUSERSQL.replace("DB2INST1", dbMap.get("tabschema"))).rxExecute()
                .doOnSuccess(rows -> {
                  if (rows.size() == 0) {
                    final String usersSql = getCreateTable("USERS");

                    Single<RowSet<Row>> crow = conn.query(usersSql).rxExecute().doOnError(err -> {
                      logger.info(String.format("Users Table Error: %s", err.getMessage()));
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

                })
                .flatMap(result -> conn
                    .query(CHECKMESSAGESQL.replace("DB2INST1", dbMap.get("tabschema"))).rxExecute()
                    .doOnSuccess(rows -> {
                      if (rows.size() == 0) {
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

                    }))
                .flatMap(result -> conn
                    .query(CHECKUNDELIVEREDSQL.replace("DB2INST1", dbMap.get("tabschema")))
                    .rxExecute().doOnSuccess(rows -> {
                      if (rows.size() == 0) {
                        final String sql = getCreateTable("UNDELIVERED");

                        Single<RowSet<Row>> crow = conn.query(sql).rxExecute().doOnError(err -> {
                          logger
                              .info(String.format("Undelivered Table Error: %s", err.getMessage()));
                        }).doOnSuccess(row2 -> {
                          logger.info("Undelivered Table Added.");
                        });

                        crow.subscribe(result2 -> {
                          //
                        }, err -> {
                          logger.info(String.format("Messages Table Error: %s", err.getMessage()));
                        });
                        tx.commit();
                        conn.close();
                      }
                    }).doOnError(err -> {
                      logger.info(String.format("Messages Table Error: %s", err));
                    }))
                .flatMapCompletable(res -> Completable.complete())));

    completable.subscribe(() -> {
      try {
        setupSql(pool4);
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }, err -> {
      logger.info(String.format("Tables Create Error: %s", err.getMessage()));
    });
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getPool4() {
    return (T) pool4;
  }

  @Override
  public MessageUser createMessageUser() {
    return new MessageUserImpl();
  }
}
