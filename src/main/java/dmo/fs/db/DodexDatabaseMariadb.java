package dmo.fs.db;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dmo.fs.utils.DodexUtil;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.reactivex.mysqlclient.MySQLPool;
import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.RowIterator;
import io.vertx.reactivex.sqlclient.RowSet;
import io.vertx.sqlclient.PoolOptions;

public class DodexDatabaseMariadb extends DbMariadb {
    private final static Logger logger = LoggerFactory.getLogger(DodexDatabaseMariadb.class.getName());
    protected Disposable disposable;
    protected MySQLPool pool4;
    protected Properties dbProperties = new Properties();
    protected Map<String, String> dbOverrideMap = new ConcurrentHashMap<>();
    protected Map<String, String> dbMap = new ConcurrentHashMap<>();
    protected JsonNode defaultNode;
    protected String webEnv = System.getenv("VERTXWEB_ENVIRONMENT");
    protected DodexUtil dodexUtil = new DodexUtil();

    public DodexDatabaseMariadb(Map<String, String> dbOverrideMap, Properties dbOverrideProps)
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

    public DodexDatabaseMariadb() throws InterruptedException, IOException, SQLException {
        super();

        defaultNode = dodexUtil.getDefaultNode();
        webEnv = webEnv == null || "prod".equals(webEnv) ? "prod" : "dev";

        dbMap = dodexUtil.jsonNodeToMap(defaultNode, webEnv);
        dbProperties = dodexUtil.mapToProperties(dbMap);

        dbProperties.setProperty("foreign_keys", "true");

        databaseSetup();
    }

    private void databaseSetup() throws InterruptedException, SQLException {
        if ("dev".equals(webEnv)) {
            DbConfiguration.configureTestDefaults(dbMap, dbProperties);
        } else {
            DbConfiguration.configureDefaults(dbMap, dbProperties);
        }

        MySQLConnectOptions connectOptions = new MySQLConnectOptions()
            .setPort(Integer.valueOf(dbMap.get("port")))
            .setHost(dbMap.get("host2"))
            .setDatabase(dbMap.get("database"))
            .setUser(dbProperties.getProperty("user").toString())
            .setPassword(dbProperties.getProperty("password").toString())
            .setSsl(Boolean.valueOf(dbProperties.getProperty("ssl")))
            .setIdleTimeout(1)
            .setCharset("utf8mb4");

        // Pool options
        PoolOptions poolOptions = new PoolOptions().setMaxSize(Runtime.getRuntime().availableProcessors() * 5);

        // Create the client pool
        pool4 = MySQLPool.pool(DodexUtil.getVertx(), connectOptions, poolOptions);
        
        Completable completable = pool4.rxGetConnection().cache().flatMapCompletable(conn -> conn.rxBegin()
            .flatMapCompletable(
            tx -> conn.query(CHECKUSERSQL)
            .rxExecute().doOnSuccess(rows -> {
                RowIterator<Row> ri = rows.iterator();
                Long val = null;
				while (ri.hasNext()) {
                    val = ri.next().getLong(0);
				}
                
                if (val == null) {
                    final String usersSql = getCreateTable("USERS");

                    Single<RowSet<Row>> crow = conn.query(usersSql).rxExecute()
                        .doOnError(err -> {
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

			}).flatMap(
				result -> conn.query(CHECKMESSAGESSQL).rxExecute().doOnSuccess(rows -> {
					RowIterator<Row> ri = rows.iterator();
					Long val = null;
					while (ri.hasNext()) {
						val = ri.next().getLong(0);
					}

					if (val == null) {
						final String sql = getCreateTable("MESSAGES");

						Single<RowSet<Row>> crow = conn.query(sql).rxExecute()
							.doOnError(err -> {
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

				})).flatMap(result -> conn.query(CHECKUNDELIVEREDSQL).rxExecute()
					.doOnSuccess(rows -> {
                    RowIterator<Row> ri = rows.iterator();
						Long val = null;
						while (ri.hasNext()) {
							val = ri.next().getLong(0);
						}
						if (val == null) {
							final String sql = getCreateTable("UNDELIVERED");
						
								Single<RowSet<Row>> crow = conn.query(sql).rxExecute()
									.doOnError(err -> {
										logger.info(String.format("Undelivered Table Error: %s", err.getMessage()));;
									}).doOnSuccess(row2 -> {
										logger.info("Undelivered Table Added.");
									});

							crow.subscribe(result2 -> {
								//
							}, err -> {
								logger.info(String.format("Messages Table Error: %s", err.getMessage()));
							});
						}
					}).doOnError(err -> {
						logger.info(String.format("Messages Table Error: %s", err.getMessage()));
					}))
				.flatMapCompletable(res -> tx.rxCommit()).doOnSubscribe(sub -> {
                    conn.rxClose();
                })
		));

		completable.subscribe(() -> {
			try {
                pool4.rxClose();
				setupSql(pool4);
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
    public <R> R getPool4() {
        return (R) pool4;
    }
}