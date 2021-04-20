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
import io.vertx.jdbcclient.JDBCConnectOptions;
import io.vertx.reactivex.jdbcclient.JDBCPool;
import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.RowIterator;
import io.vertx.reactivex.sqlclient.RowSet;
import io.vertx.sqlclient.PoolOptions;

public class DodexDatabaseCubrid extends DbCubrid {
    private final static Logger logger = LoggerFactory.getLogger(DodexDatabaseCubrid.class.getName());
    protected Disposable disposable;
    protected Properties dbProperties = new Properties();
    protected Map<String, String> dbOverrideMap = new ConcurrentHashMap<>();
    protected Map<String, String> dbMap = new ConcurrentHashMap<>();
    protected JsonNode defaultNode;
    protected String webEnv = System.getenv("VERTXWEB_ENVIRONMENT");
    protected DodexUtil dodexUtil = new DodexUtil();
    protected JDBCPool pool4;

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

        PoolOptions poolOptions = new PoolOptions().setMaxSize(Runtime.getRuntime().availableProcessors() * 5);
       
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

        pool4 = JDBCPool.pool(DodexUtil.getVertx(), connectOptions, poolOptions);

        Completable completable = pool4.rxGetConnection().flatMapCompletable(conn -> conn.rxBegin().flatMapCompletable(
            tx -> conn.query(CHECKUSERSQL).rxExecute().doOnSuccess(row -> {
                RowIterator<Row> ri = row.iterator();
                String val = null;

                while (ri.hasNext()) {
					val = ri.next().getString(0);
				}

                if (val == null) {
                    final String usersSql = getCreateTable("USERS");

                    Single<RowSet<Row>> crow = conn.query(usersSql).rxExecute()
                        .doOnError(err -> {
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

			}).flatMap(
				result -> conn.query(CHECKMESSAGESQL).rxExecute().doOnSuccess(row -> {
					RowIterator<Row> ri = row.iterator();
					String val = null;
					while (ri.hasNext()) {
						val = ri.next().getString(0);
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
					.doOnSuccess(row -> {
						RowIterator<Row> ri = row.iterator();
						String val = null;
						while (ri.hasNext()) {
							val = ri.next().getString(0);
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
                .flatMapCompletable(res -> tx.rxCommit())
		));

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
	public MessageUser createMessageUser() {
		return new MessageUserImpl();
	}

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getPool4() {
        return (T) pool4;
    }
}