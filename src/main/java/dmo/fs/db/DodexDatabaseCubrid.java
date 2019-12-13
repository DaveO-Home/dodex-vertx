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
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DodexDatabaseCubrid extends DbCubrid implements DodexDatabase {
	private final static Logger logger = LoggerFactory.getLogger(DodexDatabasePostgres.class.getName());
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

	public DodexDatabaseCubrid(Map<String, String> dbOverrideMap, Properties dbOverrideProps)
			throws InterruptedException, IOException, SQLException {
		super();

		defaultNode = dodexUtil.getDefaultNode();

		webEnv = webEnv == null || webEnv.equals("prod")? "prod": "dev";

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
		setDatabase(db);
	}

	public DodexDatabaseCubrid() throws InterruptedException, IOException, SQLException {
		super();

		defaultNode = dodexUtil.getDefaultNode();
		webEnv = webEnv == null || webEnv.equals("prod")? "prod": "dev";

		dbMap = dodexUtil.jsonNodeToMap(defaultNode, webEnv);
		dbProperties = dodexUtil.mapToProperties(dbMap);

		databaseSetup();
		setDatabase(db);
	}

	@Override
	public String getAllUsers() {
		return super.getAllUsers();
	}

	private void databaseSetup() throws InterruptedException, SQLException {
		// Override default credentials
		// dbProperties.setProperty("user", "myUser");
		// dbProperties.setProperty("password", "myPassword");
		// dbProperties.setProperty("ssl", "false");
		
		if(webEnv.equals("dev")) {
			// dbMap.put("dbname", "/myDbname"); // this wiil be merged into the default map
			DbConfiguration.configureTestDefaults(dbMap, dbProperties);
		} else {
			DbConfiguration.configureDefaults(dbMap, dbProperties); // Prod
		}
		cp = DbConfiguration.getCubridConnectionProvider();

		pool = Pools.nonBlocking()
				.maxPoolSize(Runtime.getRuntime().availableProcessors() * 5).connectionProvider(cp)
				.build();
		
		db = Database.from(pool);
		
		Disposable disposable = db.member().doOnSuccess(c -> {
			Statement stat = c.value().createStatement();
			// try {
			// 	stat.executeUpdate("drop table undelivered");
			// 	stat.executeUpdate("drop table users");
			// 	stat.executeUpdate("drop table messages");
			// } catch(Exception e) {
			// 	e.printStackTrace();
			// }

			String sql = getCreateTable("USERS");
			// Set defined user
			if (!tableExist(c.value(), "users")) {
				stat.executeUpdate(sql);
				stat.executeUpdate("CREATE UNIQUE INDEX u_users_name ON users ([name], [password]);");
			}
			sql = getCreateTable("MESSAGES");
			if (!tableExist(c.value(), "messages")) {
				stat.executeUpdate(sql);
			}
			sql = getCreateTable("UNDELIVERED");
			if (!tableExist(c.value(), "undelivered")) {
				stat.executeUpdate(sql);
			}
		}).subscribe(result -> {
			//
		}, throwable -> {
			logger.error("{0}Error creating database tables{1}",
					new Object[] { ConsoleColors.RED, ConsoleColors.RESET });
			throwable.printStackTrace();
		});
		await(disposable);
		// generate all jooq sql only once.
		setupSql(db);
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

	private static void await(Disposable disposable) throws InterruptedException {
		while (!disposable.isDisposed()) {
			Thread.sleep(100);
		}
	}
}