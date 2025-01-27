package dmo.fs.spa.db.postgres;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import dmo.fs.spa.db.SpaDbConfiguration;
import io.vertx.rxjava3.pgclient.PgBuilder;
import io.vertx.rxjava3.sqlclient.Pool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import dmo.fs.spa.utils.SpaLogin;
import dmo.fs.spa.utils.SpaLoginImpl;
import dmo.fs.utils.DodexUtil;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.vertx.core.Future;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.rxjava3.core.Promise;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.RowSet;
import io.vertx.sqlclient.PoolOptions;

public class SpaDatabasePostgres extends DbPostgres {
    private final static Logger logger =
            LoggerFactory.getLogger(SpaDatabasePostgres.class.getName());
    protected Disposable disposable;
    protected Properties dbProperties = new Properties();
    protected Map<String, String> dbOverrideMap = new ConcurrentHashMap<>();
    protected Map<String, String> dbMap = new ConcurrentHashMap<>();
    protected JsonNode defaultNode;
    protected String webEnv = System.getenv("VERTXWEB_ENVIRONMENT");
    protected DodexUtil dodexUtil = new DodexUtil();
    private Vertx vertx;

    public SpaDatabasePostgres(Map<String, String> dbOverrideMap, Properties dbOverrideProps)
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

        SpaDbConfiguration.mapMerge(dbMap, dbOverrideMap);
    }

    public SpaDatabasePostgres() throws InterruptedException, IOException, SQLException {
        super();

        defaultNode = dodexUtil.getDefaultNode();
        webEnv = webEnv == null || "prod".equals(webEnv) ? "prod" : "dev";

        dbMap = dodexUtil.jsonNodeToMap(defaultNode, webEnv);
        dbProperties = dodexUtil.mapToProperties(dbMap);
    }

    @Override
    public Future<Void> databaseSetup() throws InterruptedException, SQLException {
        // Override default credentials
        // dbProperties.setProperty("user", "myUser");
        // dbProperties.setProperty("password", "myPassword");
        // dbProperties.setProperty("ssl", "false");

        if ("dev".equals(webEnv)) {
            // dbMap.put("dbname", "/myDbname"); // this wiil be merged into the default map
            SpaDbConfiguration.configureTestDefaults(dbMap, dbProperties);
        } else {
            SpaDbConfiguration.configureDefaults(dbMap, dbProperties); // Prod
        }

        PoolOptions poolOptions =
                new PoolOptions().setMaxSize(Runtime.getRuntime().availableProcessors() * 5);

        PgConnectOptions connectOptions;

        connectOptions = new PgConnectOptions().setHost(dbMap.get("host2"))
                .setPort(Integer.valueOf(dbMap.get("port")))
                .setUser(dbProperties.getProperty("user"))
                .setPassword(dbProperties.getProperty("password"))
                .setDatabase(dbMap.get("database"))
                .setSsl(Boolean.valueOf(dbProperties.getProperty("ssl"))).setIdleTimeout(1)
        // .setCachePreparedStatements(true)
        ;

        vertx = DodexUtil.getVertx();

        Pool pool = PgBuilder
            .pool()
            .with(poolOptions)
            .connectingTo(connectOptions)
            .using(vertx)
            .build();

        Completable completable = pool.rxGetConnection()
                .flatMapCompletable(conn -> conn.rxBegin().flatMapCompletable(
                        tx -> conn.query(CHECKLOGINSQL).rxExecute().doOnSuccess(rows -> {
                            for (Row row : rows) {
                                if (row.getValue(0) == null) {
                                    final String usersSql = getCreateTable("LOGIN").replace("dummy",
                                            dbProperties.get("user").toString());

                                    Single<RowSet<Row>> crow =
                                            conn.query(usersSql).rxExecute().doOnError(err -> {
                                                logger.info(String.format("Login Table Error: %s",
                                                        err.getMessage()));
                                            }).doOnSuccess(result -> {
                                                logger.info("Login Table Added.");
                                            });

                                    crow.subscribe(result -> {
                                        //
                                    }, err -> {
                                        logger.info(String.format("Login Table Error: %s",
                                                err.getMessage()));
                                    });
                                }
                            }
                        }).doOnError(err -> {
                            logger.info(String.format("Login Table Error: %s", err.getMessage()));

                        }).flatMapCompletable(res -> tx.rxCommit())));

        Promise<Void> setupPromise = Promise.promise();
        completable.subscribe(() -> {
            try {
                setupSql(pool);
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
        return (T) pool;
    }

    @Override
    public Vertx getVertx() {
        return vertx;
    }

    @Override
    public void setVertx(Vertx vertx) {
        this.vertx = vertx;
    }
}
