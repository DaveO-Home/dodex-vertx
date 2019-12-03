package dmo.fs.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.davidmoten.rx.jdbc.ConnectionProvider;
import org.davidmoten.rx.jdbc.Database;
import org.davidmoten.rx.jdbc.pool.NonBlockingConnectionPool;
import org.davidmoten.rx.jdbc.pool.Pools;

import dmo.fs.utils.ConsoleColors;
import dmo.fs.utils.DodexUtil;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZonedDateTime;

public class DodexDatabasePostgres extends DbPostgres implements DodexDatabase {
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

	public DodexDatabasePostgres(Map<String, String> dbOverrideMap, Properties dbOverrideProps)
			throws InterruptedException, IOException {
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
	}

	public DodexDatabasePostgres() throws InterruptedException, IOException {
		super();

		defaultNode = dodexUtil.getDefaultNode();
		webEnv = webEnv == null || webEnv.equals("prod")? "prod": "dev";

		dbMap = dodexUtil.jsonNodeToMap(defaultNode, webEnv);
		dbProperties = dodexUtil.mapToProperties(dbMap);

		databaseSetup();
	}

	@Override
	public String getAllUsers() {
		return super.getAllUsers();
	}

	private void databaseSetup() throws InterruptedException {
		// dbProperties.setProperty("user", "myUser");
		// dbProperties.setProperty("password", "myPassword");
		// dbProperties.setProperty("ssl", "false")

		if(webEnv.equals("dev")) {
			// dbMap.put("dbname", "/myDbname"); // this wiil be merged into the default map
			DbConfiguration.configurePostgresTestDefaults(dbMap, dbProperties);
		} else {
			DbConfiguration.configurePostgresDefaults(dbMap, dbProperties); // Prod
		}
		cp = DbConfiguration.getPostgresConnectionProvider();

		pool = Pools.nonBlocking()
				.maxPoolSize(Runtime.getRuntime().availableProcessors() * 5).connectionProvider(cp)
				.build();
		
		db = Database.from(pool);
		
		Disposable disposable = db.member().doOnSuccess(c -> {
			Statement stat = c.value().createStatement();

			// stat.executeUpdate("drop table undelivered");
			// stat.executeUpdate("drop table users");
			// stat.executeUpdate("drop table messages");
			// stat.executeUpdate("drop sequence messages_id_seq;");
			// stat.executeUpdate("drop sequence users_id_seq;");
			
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
		}).subscribe(result -> {
			//
		}, throwable -> {
			logger.error("{0}Error creating database tables{1}",
					new Object[] { ConsoleColors.RED, ConsoleColors.RESET });
			throwable.printStackTrace();
		});
		await(disposable);
	}

	@Override
	public Long addUser(ServerWebSocket ws, Database db, MessageUser messageUser) throws InterruptedException {
		Disposable disposable = db.update(getInsertUser())
				.parameter("name", messageUser.getName())
				.parameter("password", messageUser.getPassword())
				.parameter("ip", messageUser.getIp())
				.parameter("lastlogin", ZonedDateTime.now())
				.returnGeneratedKeys()
				.getAs(Long.class)
				.doOnNext(k -> messageUser.setId(k))
				.subscribe(result -> {
					//
				}, throwable -> {
					logger.error("{0}Error adding user{1}", new Object[] { ConsoleColors.RED, ConsoleColors.RESET });
					throwable.printStackTrace();
					ws.writeTextMessage(throwable.getMessage());
				});
		await(disposable);

		return messageUser.getId();
	}

	@Override
	public int addUndelivered(ServerWebSocket ws, List<String> undelivered, Long messageId, Database db) {
		int count = 0;
		try {
			for (String name : undelivered) {
				Long userId = getUserIdByName(name, db);
				addUndelivered(userId, messageId, db);
				count++;
			}
		} catch (Exception e) {
			ws.writeTextMessage(e.getMessage());
		}
		return count;
	}

	@Override
	public Long getUserIdByName(String name, Database db) throws InterruptedException {
		List<Long> userKey = new ArrayList<>();
		Disposable disposable = db.select(getUserByName()).parameter("name", name)
				.autoMap(Users.class).doOnNext(result -> {
					userKey.add(result.id());
				}).subscribe(result -> {
					//
				}, throwable -> {
					logger.error("{0}Error finding user by name: {2}{1}", new Object[] { ConsoleColors.RED, ConsoleColors.RESET, name });
					throwable.printStackTrace();
				});
		await(disposable);
		return userKey.get(0);
	}

	@Override
	public MessageUser selectUser(MessageUser messageUser, ServerWebSocket ws, Database db) {
		MessageUser resultUser = createMessageUser();
		// db = getDatabase();
		db.select(Users.class).parameter(messageUser.getPassword()).get().isEmpty().doOnSuccess(empty -> {
			if (empty) {
				addUser(ws, db, messageUser);
			}
		}).doAfterSuccess(record -> {
			db.select(Users.class).parameter(messageUser.getPassword()).get().doOnNext(result -> {
				resultUser.setId(result.id());
				resultUser.setName(result.name());
				resultUser.setPassword(result.password());
				resultUser.setIp(result.ip());
				resultUser.setLastLogin(result.lastLogin());
			}).subscribe(result -> {
				//
			}, throwable -> {
				logger.error("{0}Error finding user {1}{2}",
						new Object[] { ConsoleColors.RED, messageUser.getName(), ConsoleColors.RESET });
				throwable.printStackTrace();
			});
		}).subscribe(result -> {
			//
		}, throwable -> {
			logger.error("{0}Error adding user {1}{2}",
					new Object[] { ConsoleColors.RED, messageUser.getName(), ConsoleColors.RESET });
			throwable.printStackTrace();
		});
		return resultUser;
	}

	@Override
	public StringBuilder buildUsersJson(MessageUser messageUser) throws InterruptedException {
		StringBuilder userJson = new StringBuilder();
		ObjectMapper mapper = new ObjectMapper();
		
		class User {
			String name = null;

			public void setName(String name) {
				this.name = name;
			}

			public String getName() {
				return name;
			}
		}
		class AllUsers implements Action {
			@Override
			public void run() throws Exception {
				userJson.append("]");
			}
		}

		AllUsers allUsers = new AllUsers();
		List<String> delimiter = new ArrayList<>();
		User user = new User();
		userJson.append("[");
		delimiter.add("");

		Disposable disposable = db.select(getAllUsers())
				.parameter("name", messageUser.getName())
				.autoMap(Users.class)
				.doOnNext(result -> {
					// build json = ["name": "user1", "name": "user2", etc...]
					user.setName(result.name());
					userJson.append(delimiter.get(0) + mapper.writeValueAsString(user));
					delimiter.set(0, ",");
				}).doOnComplete(allUsers).subscribe(result -> {
					//
				}, throwable -> {
					logger.error("{0}Error building registered user list{1}",
							new Object[] { ConsoleColors.RED_BOLD_BRIGHT, ConsoleColors.RESET });
					throwable.printStackTrace();
				});
		// wait for user json before sending back to newly connected user
		await(disposable);
		return userJson;
	}

	List<Long> messageIds = new ArrayList<>();
	Long userId = null;
	
	class RemoveUndelivered implements Action {
		int count = 0;

		@Override
		public void run() throws Exception {
			for (Long messageId : messageIds) {
				Disposable disposable = db.update(getRemoveUndelivered())
						.parameter("userid", userId)
						.parameter("messageid", messageId)
						.counts()
						.doOnNext(c -> count += c)
						.subscribe(result -> {
							//
						}, throwable -> {
							logger.error("{0}Error removing undelivered record2{1}", new Object[]{ ConsoleColors.RED, ConsoleColors.RESET });
							throwable.printStackTrace();
						});
				await(disposable);
			}
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}
	}

	RemoveUndelivered removeUndelivered = new RemoveUndelivered();

	class RemoveMessage implements Action {
		int count = 0;

		@Override
		public void run() throws Exception {
			for (Long messageId : messageIds) {
				Disposable disposable = db.select(getUndeliveredMessage())
						.parameter("userid", userId)
						.parameter("messageid", messageId)
						.getAs(Undelivered.class)
						.isEmpty()
						.doOnSuccess(empty -> {
							if (empty) {
								Disposable disposable2 = db.update(getRemoveMessage())
										.parameter("messageid", messageId).
										counts()
										.doOnNext(c -> count += c)
										.subscribe(result -> {
											//
										}, throwable -> {
											logger.error("{0}Error removing message: {2}{1}", new Object[] { ConsoleColors.RED, ConsoleColors.RESET, messageId });
											throwable.printStackTrace();
										});
								await(disposable2);
							}
						}).subscribe(result -> {
							//
						}, throwable -> {
							logger.error("{0}Error finding undelivered message{1}", new Object[] { ConsoleColors.RED, ConsoleColors.RESET });
							throwable.printStackTrace();
						});
				await(disposable);
			}
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}
	}

	RemoveMessage removeMessage = new RemoveMessage();

	@Override
	public int processUserMessages(ServerWebSocket ws, MessageUser messageUser) {
		userId = messageUser.getId();
		removeUndelivered.setCount(0);
		removeMessage.setCount(0);
		db = getDatabase();
		/*
		 * Get all undelivered messages for current user
		 */
		db.select(Undelivered.class)
			.parameter("id", messageUser.getId())
			.get()
			.doOnNext(result -> {
				Date postDate = result.postDate();
				String handle = result.fromHandle();
				String message = result.message();
				// Send messages back to client
				ws.writeTextMessage(handle + postDate + " " + message);
				messageIds.add(result.messageId());
			})
			// Remove undelivered for user
			.doOnComplete(removeUndelivered)
			.doOnError(error -> logger.error("{0}Remove Undelivered Error: {1}{2}", new Object[] { ConsoleColors.RED, error.getMessage(), ConsoleColors.RESET }))
			// Remove message if no other users are attached
			.doFinally(removeMessage)
			.doOnError(error -> logger.error("{0}Remove Message Error: {1}{2}", new Object[] { ConsoleColors.RED, error.getMessage(), ConsoleColors.RESET }))
			.subscribe();
		return removeUndelivered.getCount();
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