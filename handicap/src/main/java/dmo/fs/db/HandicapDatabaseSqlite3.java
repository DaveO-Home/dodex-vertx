package dmo.fs.db;

import java.io.IOException;
// import java.lang.reflect.InvocationTargetException;
// import java.sql.Connection;
// import java.sql.Driver;
// import java.sql.DriverManager;
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
import io.vertx.core.Promise;
import io.vertx.jdbcclient.JDBCConnectOptions;
import io.vertx.rxjava3.jdbcclient.JDBCPool;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.RowIterator;
import io.vertx.rxjava3.sqlclient.RowSet;
import io.vertx.sqlclient.PoolOptions;

public class HandicapDatabaseSqlite3 extends DbSqlite3 {
  private final static Logger logger =
      LoggerFactory.getLogger(HandicapDatabaseSqlite3.class.getName());
  protected Disposable disposable;
  protected Properties dbProperties = new Properties();
  protected Map<String, String> dbOverrideMap = new ConcurrentHashMap<>();
  protected Map<String, String> dbMap = new ConcurrentHashMap<>();
  protected JsonNode defaultNode;
  protected String webEnv = System.getenv("VERTXWEB_ENVIRONMENT");
  protected DodexUtil dodexUtil = new DodexUtil();
  protected JDBCPool pool4;
  protected Boolean isCreateTables = false;
  protected Promise<String> returnPromise = Promise.promise();

  public HandicapDatabaseSqlite3(Map<String, String> dbOverrideMap, Properties dbOverrideProps)
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

  public HandicapDatabaseSqlite3() throws InterruptedException, IOException, SQLException {
    super();
    defaultNode = dodexUtil.getDefaultNode();
    webEnv = webEnv == null || "prod".equals(webEnv) ? "prod" : "dev";

    dbMap = dodexUtil.jsonNodeToMap(defaultNode, webEnv);
    dbProperties = dodexUtil.mapToProperties(dbMap);

    dbProperties.setProperty("foreign_keys", "true");

    databaseSetup();
  }

  public HandicapDatabaseSqlite3(Boolean isCreateTables)
      throws IOException {
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
    pool4 = JDBCPool.pool(DodexUtil.getVertx(), connectOptions, poolOptions);

    Completable completable = pool4.rxGetConnection().flatMapCompletable(conn -> conn.rxBegin()
        .flatMapCompletable(tx -> conn.query(CHECKUSERSQL).rxExecute().doOnSuccess(row -> {
          RowIterator<Row> ri = row.iterator();
          String val = null;
          while (ri.hasNext()) {
            val = ri.next().getString(0);
          }

          if (val == null) {
            final String usersSql = getCreateTable("USERS");

            Single<RowSet<Row>> crow = conn.query(usersSql).rxExecute().doOnError(err -> {
              logger.error(String.format("Users Table Error: %s", err.getCause().getMessage()));
            }).doOnSuccess(result -> {
              logger.warn("Users Table Added.");
            });

            crow.subscribe(result -> {
              //
            }, err -> {
              logger.warn(String.format("Users Table Error: %s", err.getMessage()));
            });
          }
        }).doOnError(err -> {
          logger.warn(String.format("Users Table Error: %s", err.getMessage()));

        }).flatMap(result -> conn.query(CHECKMESSAGESSQL).rxExecute().doOnSuccess(row -> {
          RowIterator<Row> ri = row.iterator();
          String val = null;
          while (ri.hasNext()) {
            val = ri.next().getString(0);
          }

          if (val == null) {
            final String sql = getCreateTable("MESSAGES");

            Single<RowSet<Row>> crow = conn.query(sql).rxExecute().doOnError(err -> {
              logger.error(String.format("Messages Table Error: %s", err.getMessage()));
            }).doOnSuccess(row2 -> {
              logger.warn("Messages Table Added.");
            });

            crow.subscribe(res -> {
              //
            }, err -> {
              logger.error(String.format("Messages Table Error: %s", err.getMessage()));
            });
          }
        }).doOnError(err -> {
          logger.error(String.format("Messages Table Error: %s", err.getMessage()));

        })).flatMap(result -> conn.query(CHECKUNDELIVEREDSQL).rxExecute().doOnSuccess(row -> {
          RowIterator<Row> ri = row.iterator();
          String val = null;
          while (ri.hasNext()) {
            val = ri.next().getString(0);
          }

          if (val == null) {
            final String sql = getCreateTable("UNDELIVERED");

            Single<RowSet<Row>> crow = conn.query(sql).rxExecute().doOnError(err -> {
              logger.error(String.format("Undelivered Table Error: %s", err.getMessage()));
            }).doOnSuccess(row2 -> {
              logger.warn("Undelivered Table Added.");
            });

            crow.subscribe(result2 -> {
              //
            }, err -> {
              logger.error(String.format("Messages Table Error: %s", err.getMessage()));
            });
          }
        }).doOnError(err -> {
          logger.error(String.format("Messages Table Error: %s", err.getMessage()));
        })).flatMap(result -> conn.query(CHECKHANDICAPSQL).rxExecute().doOnError(err -> {
          logger.error(String.format("Golfer Table Error: %s", err.getMessage()));
        }).doOnSuccess(rows -> {
          Set<String> names = new HashSet<>();

          for (Row row : rows) {
            names.add(row.getString(0));
          }
          conn.query(getCreateTable("GOLFER")).rxExecute().doOnError(err -> {
            logger.error(String.format("Golfer Table Error: %s", err.getMessage()));
          }).doOnSuccess(row1 -> {
            if (!names.contains("GOLFER")) {
              logger.warn("Golfer Table Added.");
            }

            conn.query(getCreateTable("COURSE")).rxExecute().doOnError(err -> {
              logger.warn(String.format("Course Table Error: %s", err.getMessage()));
            }).doOnSuccess(row2 -> {
              if (!names.contains("COURSE")) {
                logger.warn("Course Table Added.");
              }
              conn.query(getCreateTable("RATINGS")).rxExecute().doOnError(err -> {
                logger.warn(String.format("Ratings Table Error: %s", err.getMessage()));
              }).doOnSuccess(row3 -> {
                if (!names.contains("RATINGS")) {
                  logger.warn("Ratings Table Added.");
                }
                conn.query(getCreateTable("SCORES")).rxExecute().doOnError(err -> {
                  logger.error(String.format("Scores Table Error: %s", err.getMessage()));
                }).doOnSuccess(row4 -> {
                  if (!names.contains("SCORES")) {
                    logger.warn("Scores Table Added.");
                  }
                  tx.commit();
                  conn.close();
                  if (isCreateTables) {
                    returnPromise.complete(isCreateTables.toString());
                  }
                  finalPromise.complete(isCreateTables.toString());
                }).subscribe();
              }).subscribe();
            }).subscribe();
          }).subscribe();
        })).flatMapCompletable(res -> Completable.complete())));

    completable.subscribe(() -> {
      finalPromise.future().onComplete(c -> {
        if (!isCreateTables) {
          try {
            setupSql(pool4);
          } catch (SQLException | IOException e) {
            e.printStackTrace();
          }
        }
      });
    }, err -> {
      logger.error(String.format("Tables Create Error: %s", err.getMessage()));
      err.printStackTrace();
    });
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getPool4() {
    return (T) pool4;
  }
}
