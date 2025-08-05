package dmo.fs.db.mariadb;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import dmo.fs.db.DbConfiguration;
import dmo.fs.db.MessageUser;
import dmo.fs.db.MessageUserImpl;
import dmo.fs.vertx.Server;
import io.vertx.rxjava3.mysqlclient.MySQLBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import dmo.fs.utils.DodexUtil;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.RowIterator;
import io.vertx.rxjava3.sqlclient.RowSet;
import io.vertx.sqlclient.PoolOptions;
import golf.handicap.vertx.*;

public class DodexDatabaseMariadb extends DbMariadb {
  private final static Logger logger =
      LoggerFactory.getLogger(DodexDatabaseMariadb.class.getName());
  protected Disposable disposable;
  protected Properties dbProperties = new Properties();
  protected Map<String, String> dbOverrideMap = new ConcurrentHashMap<>();
  protected Map<String, String> dbMap = new ConcurrentHashMap<>();
  protected JsonNode defaultNode;
  protected String webEnv = System.getenv("VERTXWEB_ENVIRONMENT");
  protected DodexUtil dodexUtil = new DodexUtil();
  protected Boolean isCreateTables = false;
  protected Promise<String> returnPromise = Promise.promise();


  public DodexDatabaseMariadb(Map<String, String> dbOverrideMap, Properties dbOverrideProps)
      throws IOException {
    super();

    defaultNode = dodexUtil.getDefaultNode();

    webEnv = webEnv == null || "prod".equals(webEnv) ? "prod" : "dev";

    dbMap = dodexUtil.jsonNodeToMap(defaultNode, webEnv);
    dbProperties = dodexUtil.mapToProperties(dbMap);

    if (dbOverrideProps != null && !dbOverrideProps.isEmpty()) {
      this.dbProperties = dbOverrideProps;
    }
    if (dbOverrideMap != null) {
      this.dbOverrideMap = dbOverrideMap;
    }

    dbProperties.setProperty("foreign_keys", "true");

    DbConfiguration.mapMerge(dbMap, dbOverrideMap);
    databaseSetup();
  }

  public DodexDatabaseMariadb() throws IOException {
    super();

    defaultNode = dodexUtil.getDefaultNode();
    webEnv = webEnv == null || "prod".equals(webEnv) ? "prod" : "dev";

    dbMap = dodexUtil.jsonNodeToMap(defaultNode, webEnv);
    dbProperties = dodexUtil.mapToProperties(dbMap);

    dbProperties.setProperty("foreign_keys", "true");

    databaseSetup();
  }

  public DodexDatabaseMariadb(Boolean isCreateTables)
      throws IOException {
    super();
    defaultNode = dodexUtil.getDefaultNode();
    webEnv = webEnv == null || "prod".equals(webEnv) ? "prod" : "dev";

    dbMap = dodexUtil.jsonNodeToMap(defaultNode, webEnv);
    dbProperties = dodexUtil.mapToProperties(dbMap);

    dbProperties.setProperty("foreign_keys", "true");
    this.isCreateTables = isCreateTables;
  }

  public Future<String> checkOnTables() {
    databaseSetup();
    return returnPromise.future();
  }

  private void databaseSetup() {
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
//        .setSsl(Boolean.valueOf(dbProperties.getProperty("ssl"))).setIdleTimeout(1)
        .setCharset("utf8mb4");

    // Pool options
    PoolOptions poolOptions =
        new PoolOptions().setMaxSize(Runtime.getRuntime().availableProcessors() * 5);

    pool = MySQLBuilder
        .pool()
        .with(poolOptions)
        .connectingTo(connectOptions)
        .using(Server.getRxVertx())
        .build();

    Completable completable =
        pool.rxGetConnection().cache().flatMapCompletable(conn -> conn.rxBegin()
            .flatMapCompletable(tx ->
                conn.query(CHECKUSERSQL.replace("$db_name", dbMap.get("database")))
                    .rxExecute().doOnSuccess(rows -> {
              RowIterator<Row> ri = rows.iterator();
              Long val = null;
              while (ri.hasNext()) {
                val = ri.next().getLong(0);
              }

              if (val == null) {
                final String usersSql = getCreateTable("USERS");

                Single<RowSet<Row>> crow = conn.query(usersSql).rxExecute().doOnError(err -> {
                  logger.info("Users Create Error: {}", err.getMessage());
                }).doOnSuccess(result -> {
                  logger.info("Users Table Added.");
                });

                crow.subscribe(result -> {
                  //
                }, err -> {
                  logger.info("Users Table Error: {}", err.getMessage());
                });
              }
            }).doOnError(err -> {
              logger.info("Users Table Check Error: {}", err.getMessage());

            }).flatMap(result -> conn.query(CHECKMESSAGESSQL.replace("$db_name", dbMap.get("database"))).rxExecute().doOnSuccess(rows -> {
              RowIterator<Row> ri = rows.iterator();
              Long val = null;
              while (ri.hasNext()) {
                val = ri.next().getLong(0);
              }

              if (val == null) {
                final String sql = getCreateTable("MESSAGES");

                Single<RowSet<Row>> crow = conn.query(sql).rxExecute().doOnError(err -> {
                  logger.info("Messages Create Table Error: {}", err.getMessage());
                }).doOnSuccess(row2 -> {
                  logger.info("Messages Table Added.");
                });

                crow.subscribe(res -> {
                  //
                }, err -> {
                  logger.info("Messages Error: {}", err.getMessage());
                });
              }
            }).doOnError(err -> {
              logger.info("Messages Table Error: {}", err.getMessage());

            })).flatMap(result -> conn.query(CHECKUNDELIVEREDSQL.replace("$db_name", dbMap.get("database")))
                        .rxExecute().doOnSuccess(rows -> {
              RowIterator<Row> ri = rows.iterator();
              Long val = null;
              while (ri.hasNext()) {
                val = ri.next().getLong(0);
              }
              if (val == null) {
                final String sql = getCreateTable("UNDELIVERED");

                Single<RowSet<Row>> crow = conn.query(sql).rxExecute().doOnError(err -> {
                  logger.info("Undelivered Table Error: {}", err.getMessage());
                }).doOnSuccess(row2 -> {
                  logger.info("Undelivered Table Added.");
                });

                crow.subscribe(result2 -> {
                  //
                }, err -> {
                  logger.info("Undelivered Error: {}", err.getMessage());
                });
              }
            }).doOnError(err -> {
              logger.info("Undelivered Create Error: {}", err.getMessage());
            })).flatMap(result -> conn.query(CHECKHANDICAPSQL.replace("$db_name", dbMap.get("database")))
                        .rxExecute().doOnError(err -> {
              logger.error("Golfer Table Check Error: {}", err.getMessage());
            }).doOnSuccess(rows -> {
              if (Boolean.TRUE.equals(MainVerticle.getEnableHandicap())) {
                Set<String> names = new HashSet<>();

                for (Row row : rows) {
                  names.add(row.getString(0));
                }
                conn.query(getCreateTable("GOLFER")).rxExecute().doOnError(err -> {
                  logger.error("Golfer Table Error: {}", err.getMessage());
                }).doOnSuccess(row1 -> {
                  if (!names.contains("golfer")) {
                    logger.warn("Golfer Table Added.");
                  }

                  conn.query(getCreateTable("COURSE")).rxExecute().doOnError(err -> {
                    logger.warn("Course Table Error: {}", err.getMessage());
                  }).doOnSuccess(row2 -> {
                    if (!names.contains("course")) {
                      logger.warn("Course Table Added.");
                    }
                    conn.query(getCreateTable("RATINGS")).rxExecute().doOnError(err -> {
                      logger.warn("Ratings Table Error: {}", err.getMessage());
                    }).doOnSuccess(row3 -> {
                      if (!names.contains("ratings")) {
                        logger.warn("Ratings Table Added.");
                      }
                      conn.query(getCreateTable("SCORES")).rxExecute().doOnError(err -> {
                        logger.error("Scores Table Error: {}", err.getMessage());
                      }).doOnSuccess(row4 -> {
                        if (!names.contains("scores")) {
                          logger.warn("Scores Table Added.");
                          tx.commit();
                        }
                        conn.query(getCreateTable("GROUPS")).rxExecute().doOnError(err -> {
                          logger.error("Groups Table Error: {}", err.getMessage());
                        }).doOnSuccess(row5 -> {
                          if (!names.contains("groups")) {
                            logger.warn("Groups Table Added.");
                          }
                          conn.query(getCreateTable("MEMBER")).rxExecute().doOnError(err -> {
                            logger.error("Member Table Error: {}", err.getMessage());
                          }).doOnSuccess(row6 -> {
                            if (!names.contains("member")) {
                              logger.warn("Member Table Added.");
                            }
                            tx.commit();
                            if (isCreateTables) {
                              returnPromise.complete(isCreateTables.toString());
                            }
                            finalPromise.complete(isCreateTables.toString());
                          }).subscribe(res -> conn.close().subscribe());
                        }).subscribe();
                      }).subscribe();
                    }).subscribe();
                  }).subscribe();
                }).subscribe();
              } else {
                tx.commit();
                conn.close().subscribe();
                finalPromise.complete("");
              }
            })).flatMapCompletable(res -> Completable.complete())));

    completable.subscribe(() -> {
      finalPromise.future().onComplete(c -> {
        if (!isCreateTables) {
          try {
            setupSql(pool);
          } catch (SQLException e) {
            e.printStackTrace();
          }
        }
      });
    }, err -> {
      logger.info("Tables Create Error: {}", err.getMessage());
      err.printStackTrace();
    });
  }

  @Override
  public MessageUser createMessageUser() {
    return new MessageUserImpl();
  }


  @Override
  @SuppressWarnings("unchecked")
  public <R> R getPool() {
    return (R) pool;
  }
}
