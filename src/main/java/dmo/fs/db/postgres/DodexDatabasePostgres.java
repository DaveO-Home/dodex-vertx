package dmo.fs.db.postgres;

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
import io.vertx.rxjava3.pgclient.PgBuilder;
import io.vertx.rxjava3.sqlclient.Pool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import dmo.fs.utils.DodexUtil;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.RowIterator;
import io.vertx.rxjava3.sqlclient.RowSet;
import io.vertx.sqlclient.PoolOptions;
import golf.handicap.vertx.*;

public class DodexDatabasePostgres extends DbPostgres {
  private final static Logger logger =
      LoggerFactory.getLogger(DodexDatabasePostgres.class.getName());
  protected Disposable disposable;
  protected Properties dbProperties = new Properties();
  protected Map<String, String> dbOverrideMap = new ConcurrentHashMap<>();
  protected Map<String, String> dbMap = new ConcurrentHashMap<>();
  protected JsonNode defaultNode;
  protected String webEnv = System.getenv("VERTXWEB_ENVIRONMENT");
  protected DodexUtil dodexUtil = new DodexUtil();
  protected Boolean isCreateTables = false;
  protected Promise<String> returnPromise = Promise.promise();

  public DodexDatabasePostgres(Map<String, String> dbOverrideMap, Properties dbOverrideProps)
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

    assert dbOverrideMap != null;
    DbConfiguration.mapMerge(dbMap, dbOverrideMap);
    databaseSetup();
  }

  public DodexDatabasePostgres() throws InterruptedException, IOException, SQLException {
    super();

    defaultNode = dodexUtil.getDefaultNode();
    webEnv = webEnv == null || "prod".equals(webEnv) ? "prod" : "dev";
    dbMap = dodexUtil.jsonNodeToMap(defaultNode, webEnv);
    dbProperties = dodexUtil.mapToProperties(dbMap);

    databaseSetup();
  }

  public DodexDatabasePostgres(Boolean isCreateTables)
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

  protected void databaseSetup() throws InterruptedException, SQLException {
    Promise<String> finalPromise = Promise.promise();
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

    PoolOptions poolOptions =
        new PoolOptions().setMaxSize(Runtime.getRuntime().availableProcessors() * 5);

    PgConnectOptions connectOptions;

    connectOptions = new PgConnectOptions().setHost(dbMap.get("host2"))
        .setPort(Integer.parseInt(dbMap.get("port")))
        .setUser(dbProperties.getProperty("user").toString())
        .setPassword(dbProperties.getProperty("password").toString())
        .setDatabase(dbMap.get("database"))
//        .setSsl(Boolean.parseBoolean(dbProperties.getProperty("ssl")))
//        .setIdleTimeout(1)
    // .setCachePreparedStatements(true)
    ;
    Pool pool = PgBuilder
        .pool()
        .with(poolOptions)
        .connectingTo(connectOptions)
        .using(Server.getRxVertx())
        .build();

    Completable completable = pool.rxGetConnection().cache().flatMapCompletable(conn -> conn.rxBegin()
        .flatMapCompletable(tx -> conn.query(CHECKUSERSQL).rxExecute().doOnSuccess(row -> {
          RowIterator<Row> ri = row.iterator();
          String val = null;
          while (ri.hasNext()) {
            val = ri.next().getString(0);
          }

          if (val == null) {
            final String usersSql =
                getCreateTable("USERS").replaceAll("dummy", dbProperties.get("user").toString());

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

        }).flatMap(result -> conn.query(CHECKMESSAGESSQL).rxExecute().doOnSuccess(row -> {
          RowIterator<Row> ri = row.iterator();
          String val = null;
          while (ri.hasNext()) {
            val = ri.next().getString(0);
          }

          if (val == null) {
            final String sql =
                getCreateTable("MESSAGES").replaceAll("dummy", dbProperties.get("user").toString());

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
            final String sql = getCreateTable("UNDELIVERED").replaceAll("dummy",
                dbProperties.get("user").toString());

            Single<RowSet<Row>> crow = conn.query(sql).rxExecute().doOnError(err -> {
              logger.info(String.format("Undelivered Table Error: %s", err.getMessage()));
            }).doOnSuccess(row2 -> {
              tx.commit();
              logger.info("Undelivered Table Added.");
            });

            crow.subscribe(result2 -> {
              //
            }, err -> {
              logger.info(String.format("Undelivered Table Error: %s", err.getMessage()));
            });
          }
        }).doOnError(err -> {
          logger.info(String.format("Undelivered Table Error: %s", err.getMessage()));
        })).flatMap(result -> conn.query(CHECKHANDICAPSQL).rxExecute().doOnError(err -> {
          logger.error(String.format("Golfer Table Error: %s", err.getMessage()));
          conn.close().subscribe();
        }).doOnSuccess(rows -> {
          if (Boolean.TRUE.equals(MainVerticle.getEnableHandicap())) {
            Set<String> names = new HashSet<>();

            for (Row row : rows) {
              names.add(row.getString(0));
            }
            String sql =
                getCreateTable("GOLFER").replaceAll("dummy", dbProperties.get("user").toString());
            if ((names.contains("golfer"))) {
              sql = SELECTONE;
            }
            conn.query(sql).rxExecute().doOnError(err -> {
              logger.error(String.format("Golfer Table Error: %s", err.getMessage()));
              conn.close().subscribe();
              throw new SQLException(err.getMessage());
            }).doOnSuccess(row1 -> {
              if (!names.contains("golfer")) {
                logger.warn("Golfer Table Added.");
              }
              String sql2 =
                  getCreateTable("COURSE").replaceAll("dummy", dbProperties.get("user").toString());
              if ((names.contains("course"))) {
                sql2 = SELECTONE;
              }
              conn.query(sql2).rxExecute().doOnError(err -> {
                logger.warn(String.format("Course Table Error: %s", err.getMessage()));
                conn.close().subscribe();
                throw new SQLException(err.getMessage());
              }).doOnSuccess(row2 -> {
                if (!names.contains("course")) {
                  logger.warn("Course Table Added.");
                }
                String sql3 = getCreateTable("RATINGS").replaceAll("dummy",
                    dbProperties.get("user").toString());
                if ((names.contains("ratings"))) {
                  sql3 = SELECTONE;
                }
                conn.query(sql3).rxExecute().doOnError(err -> {
                  logger.warn(String.format("Ratings Table Error: %s", err.getMessage()));
                  conn.close().subscribe();
                  throw new SQLException(err.getMessage());
                }).doOnSuccess(row3 -> {
                  if (!names.contains("ratings")) {
                    logger.warn("Ratings Table Added.");
                  }
                  String sql4 = getCreateTable("SCORES").replaceAll("dummy",
                      dbProperties.get("user").toString());
                  if ((names.contains("scores"))) {
                    sql4 = SELECTONE;
                  }
                  conn.query(sql4).rxExecute().doOnError(err -> {
                    logger.error(String.format("Scores Table Error: %s", err.getMessage()));
                    conn.close().subscribe();
                    throw new SQLException(err.getMessage());
                  }).doOnSuccess(row4 -> {
                    if (!names.contains("scores")) {
                      logger.warn("Scores Table Added.");
                    }
                    String sql5 = getCreateTable("GROUPS").replaceAll("dummy",
                        dbProperties.get("user").toString());
                    if ((names.contains("groups"))) {
                      sql5 = SELECTONE;
                    }
                    conn.query(sql5).rxExecute().doOnError(err -> {
                      logger.error(String.format("Groups Table Error: %s", err.getMessage()));
                      conn.close().subscribe();
                      throw new SQLException(err.getMessage());
                    }).doOnSuccess(row5 -> {
                      if (!names.contains("groups")) {
                        logger.warn("Groups Table Added.");
                      }
                      String sql6 = getCreateTable("MEMBER").replaceAll("dummy",
                          dbProperties.get("user").toString());
                      if ((names.contains("member"))) {
                        sql6 = SELECTONE;
                      }
                      conn.query(sql6).rxExecute().doOnError(err -> {
                        logger.error(String.format("Member Table Error: %s", err.getMessage()));
                        conn.close().subscribe();
                        throw new SQLException(err.getMessage());
                      }).doOnSuccess(row6 -> {
                        if (!names.contains("member")) {
                          logger.warn("Member Table Added.");
                        }
                        tx.commit();
                        conn.close().subscribe();
                        if (isCreateTables) {
                          returnPromise.complete(isCreateTables.toString());
                        }
                        finalPromise.complete(isCreateTables.toString());
                      }).subscribe();
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

    completable.toCompletionStage(pool).whenComplete((pool5, err) -> {
      finalPromise.future().onComplete(c -> {
        if (!isCreateTables) {
          try {
            setupSql(pool5);
          } catch (SQLException e) {
            e.printStackTrace();
          }
        }
      });
      if(err != null) {
        logger.error("Tables Create Error: {}", err.getMessage());
      }
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
