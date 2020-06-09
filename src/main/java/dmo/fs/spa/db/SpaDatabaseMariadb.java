package dmo.fs.spa.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;

import org.davidmoten.rx.jdbc.ConnectionProvider;
import org.davidmoten.rx.jdbc.Database;
import org.davidmoten.rx.jdbc.pool.NonBlockingConnectionPool;
import org.davidmoten.rx.jdbc.pool.Pools;

import dmo.fs.spa.utils.SpaLogin;
import dmo.fs.spa.utils.SpaLoginImpl;
import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import io.reactivex.disposables.Disposable;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class SpaDatabaseMariadb extends DbMariadb {
	private final static Logger logger = LoggerFactory.getLogger(SpaDatabaseMariadb.class.getName());
	protected Disposable disposable;
	protected ConnectionProvider cp;
	protected NonBlockingConnectionPool pool;
	protected Database db;
	protected Properties dbProperties = new Properties();
	protected Map<String, String> dbOverrideMap = new ConcurrentHashMap<>();
	protected Map<String, String> dbMap = new ConcurrentHashMap<>();
	protected JsonNode defaultNode;
	protected String webEnv = System.getenv("VERTXWEB_ENVIRONMENT");
	protected DodexUtil dodexUtil = new DodexUtil();

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
		databaseSetup();
	}

	public SpaDatabaseMariadb() throws InterruptedException, IOException, SQLException {
		super();

		defaultNode = dodexUtil.getDefaultNode();
		webEnv = webEnv == null || "prod".equals(webEnv)? "prod": "dev";

		dbMap = dodexUtil.jsonNodeToMap(defaultNode, webEnv);
		dbProperties = dodexUtil.mapToProperties(dbMap);
		
		dbProperties.setProperty("foreign_keys", "true");

		databaseSetup();
	}

	private void databaseSetup() throws InterruptedException, SQLException {
		if ("dev".equals(webEnv)) {
			SpaDbConfiguration.configureTestDefaults(dbMap, dbProperties);
		} else {
			SpaDbConfiguration.configureDefaults(dbMap, dbProperties); // Using prod (./dodex.db)
		}
		cp = SpaDbConfiguration.getMariadbConnectionProvider();

		pool = Pools.nonBlocking().maxPoolSize(Runtime.getRuntime().availableProcessors() * 5).connectionProvider(cp)
				.build();
		
		db = Database.from(pool);

		Future.future(prom -> {
			db.member().doOnSuccess(c -> {
				Statement stat = c.value().createStatement();

				// stat.executeUpdate("drop table LOGIN");

				String sql = getCreateTable("LOGIN");
				if (!tableExist(c.value(), "LOGIN")) {
					stat.executeUpdate(sql);
				}
				
				stat.close();
				c.value().close();
			}).subscribe(result -> {
				prom.complete();
			}, throwable -> {
				logger.error(String.join(ColorUtilConstants.RED, "Error creating database tables: ", throwable.getMessage(), ColorUtilConstants.RESET));
				throwable.printStackTrace();
			});
			// generate all jooq sql only once.
			prom.future().onSuccess(result -> {
				try {
					setupSql(db);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			});
		});
	}
	
	@Override
	public Database getDatabase() {
		return Database.from(pool);
	}

	@Override
	public NonBlockingConnectionPool getPool() {
		return pool;
	}

	@Override
	public SpaLogin createSpaLogin() {
		return new SpaLoginImpl();
	}

	// per stack overflow
	private static boolean tableExist(Connection conn, String tableName) throws SQLException {
		boolean exists = false;
		try (ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null)) {
			while (rs.next()) {
				String name = rs.getString("TABLE_NAME");
				if (name != null && name.equalsIgnoreCase(tableName)) {
					exists = true;
					break;
				}
			}
		}
		return exists;
	}
}