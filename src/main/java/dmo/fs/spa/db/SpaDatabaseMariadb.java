package dmo.fs.spa.db;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dmo.fs.spa.utils.SpaLogin;
import dmo.fs.spa.utils.SpaLoginImpl;
import dmo.fs.utils.DodexUtil;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.vertx.core.Future;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.reactivex.core.Promise;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.mysqlclient.MySQLPool;
import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.RowSet;
import io.vertx.sqlclient.PoolOptions;

public class SpaDatabaseMariadb extends DbMariadb {
    private final static Logger logger = LoggerFactory.getLogger(SpaDatabaseMariadb.class.getName());
    protected Disposable disposable;
    protected Properties dbProperties = new Properties();
    protected Map<String, String> dbOverrideMap = new ConcurrentHashMap<>();
    protected Map<String, String> dbMap = new ConcurrentHashMap<>();
    protected JsonNode defaultNode;
    protected String webEnv = System.getenv("VERTXWEB_ENVIRONMENT");
    protected DodexUtil dodexUtil = new DodexUtil();
    protected MySQLPool pool4;
    private Vertx vertx;

	public SpaDatabaseMariadb(Map<String, String> dbOverrideMap, Properties dbOverrideProps)
			throws InterruptedException, IOException, SQLException {
		super();

		defaultNode = dodexUtil.getDefaultNode();

		webEnv = webEnv == null || "prod".equals(webEnv)? "prod": "dev";

		dbMap = dodexUtil.jsonNodeToMap(defaultNode, webEnv);
		dbProperties = dodexUtil.mapToProperties(dbMap);
		
		if (dbOverrideProps != null && dbOverrideProps.size() > 0) {
			this.dbProperties = dbOverrideProps;
		}
		if (dbOverrideMap != null) {
			this.dbOverrideMap = dbOverrideMap;
		}

		dbProperties.setProperty("foreign_keys", "true");

		SpaDbConfiguration.mapMerge(dbMap, dbOverrideMap);
	}

	public SpaDatabaseMariadb() throws InterruptedException, IOException, SQLException {
		super();

		defaultNode = dodexUtil.getDefaultNode();
		webEnv = webEnv == null || "prod".equals(webEnv)? "prod": "dev";

		dbMap = dodexUtil.jsonNodeToMap(defaultNode, webEnv);
		dbProperties = dodexUtil.mapToProperties(dbMap);
		
		dbProperties.setProperty("foreign_keys", "true");
	}

	public Future<Void> databaseSetup() throws InterruptedException, SQLException {
		if ("dev".equals(webEnv)) {
			SpaDbConfiguration.configureTestDefaults(dbMap, dbProperties);
		} else {
			SpaDbConfiguration.configureDefaults(dbMap, dbProperties); // Using prod (./dodex.db)
		}
        
        PoolOptions poolOptions = new PoolOptions().setMaxSize(Runtime.getRuntime().availableProcessors() * 5);

        MySQLConnectOptions connectOptions;

        connectOptions = new MySQLConnectOptions()
            .setPort(Integer.valueOf(dbMap.get("port")))
            .setHost(dbMap.get("host2"))
            .setDatabase(dbMap.get("database"))
            .setUser(dbProperties.getProperty("user").toString())
            .setPassword(dbProperties.getProperty("password").toString())
            .setSsl(Boolean.valueOf(dbProperties.getProperty("ssl")))
            .setIdleTimeout(1)
            .setCharset("utf8mb4");

        vertx = DodexUtil.getVertx();

        pool4 = MySQLPool.pool(vertx, connectOptions, poolOptions);

        Completable completable = pool4.rxGetConnection().flatMapCompletable(conn -> 
            conn.rxBegin().flatMapCompletable(
			tx -> conn.query(CHECKLOGINSQL).rxExecute().doOnSuccess(rows -> {
                if (rows.size() == 0) {
                    final String usersSql = getCreateTable("LOGIN");

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

            }).flatMapCompletable(res -> tx.rxCommit())
        ));
        
        Promise<Void> setupPromise = Promise.promise();
        completable.subscribe(() -> {
			try {
                setupSql(pool4);
                setupPromise.complete();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}, err -> {
            logger.info(String.format("Tables Create Error: %s", err.getMessage()));
        });

        return setupPromise.future();
	}

	@Override
	public SpaLogin createSpaLogin() {
		return new SpaLoginImpl();
	}

    @Override
	@SuppressWarnings("unchecked")
    public <T> T getPool4() {
        return (T) pool4;
    }

    public Vertx getVertx() {
        return vertx;
    }

    public void setVertx(Vertx vertx) {
        this.vertx = vertx;
    }
}