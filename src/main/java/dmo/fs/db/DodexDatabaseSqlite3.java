package dmo.fs.db;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.databind.JsonNode;

import org.davidmoten.rx.jdbc.ConnectionProvider;
import org.davidmoten.rx.jdbc.Database;
import org.davidmoten.rx.jdbc.pool.NonBlockingConnectionPool;
import org.davidmoten.rx.jdbc.pool.Pools;

import dmo.fs.utils.ConsoleColors;
import dmo.fs.utils.DodexUtil;
import io.reactivex.disposables.Disposable;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DodexDatabaseSqlite3 extends DbSqlite3 {
	private final static Logger logger = LoggerFactory.getLogger(DodexDatabaseSqlite3.class.getName());
	protected Disposable disposable;
	protected ConnectionProvider cp;
	protected NonBlockingConnectionPool pool;
	protected Database db;
	protected Properties dbProperties = new Properties();
	protected Map<String, String> dbOverrideMap = new HashMap<>();
	protected Map<String, String> dbMap = new HashMap<>();
	protected JsonNode defaultNode = null;
	protected String webEnv = System.getenv("VERTXWEB_ENVIRONMENT");
	protected DodexUtil dodexUtil = new DodexUtil();

	public DodexDatabaseSqlite3(Map<String, String> dbOverrideMap, Properties dbOverrideProps)
			throws InterruptedException, IOException, SQLException {
		super();

		defaultNode = dodexUtil.getDefaultNode();

		webEnv = webEnv == null || webEnv.equals("prod") ? "prod" : "dev";

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

	public DodexDatabaseSqlite3() throws InterruptedException, IOException, SQLException {
		super();

		defaultNode = dodexUtil.getDefaultNode();
		webEnv = webEnv == null || webEnv.equals("prod") ? "prod" : "dev";

		dbMap = dodexUtil.jsonNodeToMap(defaultNode, webEnv);
		dbProperties = dodexUtil.mapToProperties(dbMap);

		dbProperties.setProperty("foreign_keys", "true");

		databaseSetup();
	}

	@Override
	public String getAllUsers() {
		return super.getAllUsers();
	}

	private void databaseSetup() throws InterruptedException, SQLException {
		if (webEnv.equals("dev")) {
			DbConfiguration.configureTestDefaults(dbMap, dbProperties);
		} else {
			DbConfiguration.configureDefaults(dbMap, dbProperties); // Using prod (./dodex.db)
		}
		cp = DbConfiguration.getSqlite3ConnectionProvider();

		pool = Pools.nonBlocking().maxPoolSize(Runtime.getRuntime().availableProcessors() * 5).connectionProvider(cp)
				.build();

		db = Database.from(pool);

		Future.future(prom -> {
			db.member().doOnSuccess(c -> {
				Statement stat = c.value().createStatement();

				// stat.executeUpdate("drop table undelivered");
				// stat.executeUpdate("drop table users");
				// stat.executeUpdate("drop table messages");

				String sql = getCreateTable("USERS");
				if (!tableExist(c.value(), "users")) {
					stat.executeUpdate(sql);
				}
				sql = getCreateTable("MESSAGES");
				if (!tableExist(c.value(), "messages")) {
					stat.executeUpdate(sql);
				}
				sql = getCreateTable("UNDELIVERED");
				if (!tableExist(c.value(), "undelivered")) {
					stat.executeUpdate(sql);
				}
				stat.close();
				c.value().close();
			}).subscribe(result -> {
				prom.complete();
			}, throwable -> {
				logger.error(String.join(ConsoleColors.RED, "Error creating database tables: ", throwable.getMessage(), ConsoleColors.RESET));
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
	public MessageUser createMessageUser() {
		return new MessageUserImpl();
	}

	// per stack overflow
	private static boolean tableExist(Connection conn, String tableName) throws SQLException {
		boolean exists = false;
		try (ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null)) {
			while (rs.next()) {
				String name = rs.getString("TABLE_NAME");
				if (name != null && name.toLowerCase().equals(tableName.toLowerCase())) {
					exists = true;
					break;
				}
			}
		}
		return exists;
	}
}