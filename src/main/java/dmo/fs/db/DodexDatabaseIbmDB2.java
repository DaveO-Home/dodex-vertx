package dmo.fs.db;

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

import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import io.reactivex.disposables.Disposable;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class DodexDatabaseIbmDB2 extends DbIbmDB2 {
	private final static Logger logger = LoggerFactory.getLogger(DodexDatabaseIbmDB2.class.getName());
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

	public DodexDatabaseIbmDB2(Map<String, String> dbOverrideMap, Properties dbOverrideProps)
			throws InterruptedException, IOException, SQLException {
		super();

		defaultNode = dodexUtil.getDefaultNode();

		webEnv = webEnv == null || "prod".equals(webEnv)? "prod": "dev";

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
		webEnv = webEnv == null || "prod".equals(webEnv)? "prod": "dev";

		dbMap = dodexUtil.jsonNodeToMap(defaultNode, webEnv);
		dbProperties = dodexUtil.mapToProperties(dbMap);

		databaseSetup();
	}

	private void databaseSetup() throws InterruptedException, SQLException {
		// Override default credentials
		// dbProperties.setProperty("user", "myUser");
		// dbProperties.setProperty("password", "myPassword");
		// dbProperties.setProperty("ssl", "false");
		
		if("dev".equals(webEnv)) {
			// dbMap.put("dbname", "/myDbname"); // this wiil be merged into the default map
			DbConfiguration.configureTestDefaults(dbMap, dbProperties);
		} else {
			DbConfiguration.configureDefaults(dbMap, dbProperties); // Prod
		}
		cp = DbConfiguration.getIbmDb2ConnectionProvider();

		pool = Pools.nonBlocking()
				.maxPoolSize(Runtime.getRuntime().availableProcessors() * 5).connectionProvider(cp)
				.build();
		
		db = Database.from(pool);
				
		Future.future(prom -> {
			db.member().doOnSuccess(c -> {
				Statement stat = c.value().createStatement();
				
				// stat.executeUpdate("drop table undelivered");
				// stat.executeUpdate("drop table users");
				// stat.executeUpdate("drop table messages");
				
				String sql = getCreateTable("USERS");
				// Set defined user
				if (!tableExist(c.value(), "users")) {
					stat.executeUpdate(sql);
					sql = getUsersIndex("USERS");
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
	public MessageUser createMessageUser() {
		return new MessageUserImpl();
	}

	private static boolean tableExist(Connection conn, String tableName) throws SQLException {
		boolean exists = false;
		try(Statement stat = conn.createStatement()) {		
			try(ResultSet rs = stat.executeQuery("select 1 from " + tableName + " where 0 = 1")) {
				exists = true;
			} catch(Exception e) {
				logger.info("{0}Creating table {1}{2}",
						new Object[] { ColorUtilConstants.BLUE, tableName, ColorUtilConstants.RESET });
			}
		}
		
		return exists;
	}
}