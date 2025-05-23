package dmo.fs.db.db2;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import dmo.fs.db.DbConfiguration;
import dmo.fs.db.MessageUser;
import dmo.fs.db.MessageUserImpl;
import dmo.fs.vertx.Server;
import io.vertx.rxjava3.db2client.DB2Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import dmo.fs.utils.DodexUtil;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.vertx.db2client.DB2ConnectOptions;
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

//    pool4 = DB2Pool.pool(DodexUtil.getVertx(), connectOptions, poolOptions);
    pool = DB2Builder
        .pool()
        .with(poolOptions)
        .connectingTo(connectOptions)
        .using(Server.getRxVertx())
        .build();

    Completable completable = pool.rxGetConnection()
        .flatMapCompletable(conn -> conn.rxBegin().flatMapCompletable(
            tx -> conn.query(CHECKUSERSQL.replace("DB2INST1", dbMap.get("tabschema"))).rxExecute()
                .doOnSuccess(rows -> {
                  if (rows.size() == 0) {
                    final String usersSql = getCreateTable("USERS");

                    Single<RowSet<Row>> crow = conn.query(usersSql).rxExecute().doOnError(err -> {
                      logger.info("Users Table Error: {}", err.getMessage());
                    }).doOnSuccess(result -> {
                      logger.info("Users Table Added.");
                    });

                    crow.subscribe(result -> {
                      //
                    }, err -> {
                      logger.info("Users Table Error1: {}", err.getMessage());
                    });
                  }
                }).doOnError(err -> {
                  logger.info("Users Table Error2: {}", err.getMessage());

                })
                .flatMap(result -> conn
                    .query(CHECKMESSAGESQL.replace("DB2INST1", dbMap.get("tabschema"))).rxExecute()
                    .doOnSuccess(rows -> {
                      if (rows.size() == 0) {
                        final String sql = getCreateTable("MESSAGES");

                        Single<RowSet<Row>> crow = conn.query(sql).rxExecute().doOnError(err -> {
                          logger.info("Messages Table Error: {}", err.getMessage());
                        }).doOnSuccess(row2 -> {
                          logger.info("Messages Table Added.");
                        });

                        crow.subscribe(res -> {
                          //
                        }, err -> {
                          logger.info("Messages Table Error2: {}", err.getMessage());
                        });
                      }
                    }).doOnError(err -> {
                      logger.info("Messages Table Error3: {}", err.getMessage());

                    }))
                .flatMap(result -> conn
                    .query(CHECKUNDELIVEREDSQL.replace("DB2INST1", dbMap.get("tabschema")))
                    .rxExecute().doOnSuccess(rows -> {
                      if (rows.size() == 0) {
                        final String sql = getCreateTable("UNDELIVERED");

                        Single<RowSet<Row>> crow = conn.query(sql).rxExecute().doOnError(err -> {
                          logger
                              .info("Undelivered Table Error: {}", err.getMessage());
                        }).doOnSuccess(row2 -> {
                          logger.info("Undelivered Table Added.");
                        });

                        crow.subscribe(result2 -> {
                          //
                        }, err -> {
                          logger.info("Table create Error: {}", err.getMessage());
                        });
                      tx.commit();
                        conn.close().subscribe();
                      }
                    }).doOnError(err -> {
                      logger.info("Table create Error2: {}", err.getMessage());
                    }))
                .flatMapCompletable(res -> Completable.complete())));

    completable.subscribe(() -> {
      try {
        setupSql(pool);
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }, err -> {
      logger.info("Tables Create Error: {}", err.getMessage());
    });
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getPool() {
    return (T) pool;
  }

  @Override
  public MessageUser createMessageUser() {
    return new MessageUserImpl();
  }
}
