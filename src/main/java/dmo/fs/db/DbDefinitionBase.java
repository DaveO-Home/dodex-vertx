
package dmo.fs.db;

import static org.jooq.impl.DSL.deleteFrom;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.insertInto;
import static org.jooq.impl.DSL.mergeInto;
import static org.jooq.impl.DSL.notExists;
import static org.jooq.impl.DSL.param;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.table;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.davidmoten.rx.jdbc.Database;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import dmo.fs.db.JavaRxDateDb.Undelivered;
import dmo.fs.db.JavaRxDateDb.Users;
import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import io.reactivex.functions.Action;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class DbDefinitionBase {
	private final static Logger logger = LoggerFactory.getLogger(DbDefinitionBase.class.getName());
	protected final static String QUERYUSERS = "select * from USERS where password=?";
	protected final static String QUERYMESSAGES = "select * from MESSAGES where id=?";
	protected final static String QUERYUNDELIVERED = "Select message_id, name, message, from_handle, post_date from USERS, UNDELIVERED, MESSAGES where USERS.id = user_id and MESSAGES.id = message_id and USERS.id = :id";

	private static DSLContext create;

	private static String GETALLUSERS;
	private static String GETUSERBYNAME;
	private static String GETINSERTUSER;
	private static String GETUPDATEUSER;
	private static String GETREMOVEUNDELIVERED;
	private static String GETREMOVEMESSAGE;
	private static String GETUNDELIVEREDMESSAGE;
	private static String GETDELETEUSER;
	private static String GETADDMESSAGE;
	private static String GETADDUNDELIVERED;
	private static String GETUSERNAMES;
	private static String GETUSERBYID;
	private static String GETREMOVEUSERUNDELIVERED;
	private static String GETUSERUNDELIVERED;
	private static String GETDELETEUSERBYID;
	private static String GETSQLITEUPDATEUSER;
	private static String GETREMOVEUSERS;
	private Boolean isTimestamp;
	private Vertx vertx;

	public static void setupSql(Database db) throws SQLException {
		try (Connection conn = db.connection().blockingGet()) {
			create = DSL.using(conn, DodexUtil.getSqlDialect());

			GETALLUSERS = setupAllUsers();
			GETUSERBYNAME = setupUserByName();
			GETINSERTUSER = setupInsertUser();
			GETUPDATEUSER = setupUpdateUser();
			GETREMOVEUNDELIVERED = setupRemoveUndelivered();
			GETREMOVEMESSAGE = setupRemoveMessage();
			GETUNDELIVEREDMESSAGE = setupUndeliveredMessage();
			GETDELETEUSER = setupDeleteUser();
			GETADDMESSAGE = setupAddMessage();
			GETADDUNDELIVERED = setupAddUndelivered();
			GETUSERNAMES = setupUserNames();
			GETUSERBYID = setupUserById();
			GETREMOVEUSERUNDELIVERED = setupRemoveUserUndelivered();
			GETUSERUNDELIVERED = setupUserUndelivered();
			GETDELETEUSERBYID = setupDeleteUserById();
			GETSQLITEUPDATEUSER = setupSqliteUpdateUser();
			GETREMOVEUSERS = setupRemoveUsers();
		} 
	}

	private static String setupAllUsers() {
		return create.renderNamedParams(
				select(field("ID"), field("NAME"), field("PASSWORD"), field("IP"), field("LAST_LOGIN"))
						.from(table("USERS")).where(field("NAME").ne(param("NAME", ":name"))));
	}

	public String getAllUsers() {
		return GETALLUSERS;
	}

	private static String setupUserByName() {
		return create.renderNamedParams(
				select(field("ID"), field("NAME"), field("PASSWORD"), field("IP"), field("LAST_LOGIN"))
						.from(table("USERS")).where(field("NAME").eq(param("NAME", ":name"))));
	}

	public String getUserByName() {
		return GETUSERBYNAME;
	}

	private static String setupUserById() {
		return create.renderNamedParams(
				select(field("ID"), field("NAME"), field("PASSWORD"), field("IP"), field("LAST_LOGIN"))
						.from(table("USERS")).where(field("NAME").eq(param("NAME", ":name"))));
	}

	public String getUserById() {
		return GETUSERBYID;
	}

	private static String setupInsertUser() {
		return create.renderNamedParams(
				insertInto(table("USERS")).columns(field("NAME"), field("PASSWORD"), field("IP"), field("LAST_LOGIN"))
						.values(param("NAME", ":name"), param("PASSWORD", ":password"), param("IP", ":ip"),
								param("LASTLOGIN", ":lastlogin")));
	}

	public String getInsertUser() {
		return GETINSERTUSER;
	}

	private static String setupUpdateUser() {
		return create.renderNamedParams(mergeInto(table("USERS"))
				.columns(field("ID"), field("NAME"), field("PASSWORD"), field("IP"), field("LAST_LOGIN"))
				.key(field("ID")).values(param("ID", ":id"), param("NAME", ":name"), param("PASSWORD", ":password"),
						param("IP", ":ip"), param("LASTLOGIN", ":lastlogin")));
	}

	public String getUpdateUser() {
		return GETUPDATEUSER;
	}

	public static String setupSqliteUpdateUser() {
		return "update USERS set last_login = :LASTLOGIN where id = :USERID";
	}

	public String getSqliteUpdateUser() {
		return GETSQLITEUPDATEUSER;
	}

	private static String setupRemoveUndelivered() {
		return create.renderNamedParams(
				deleteFrom(table("UNDELIVERED")).where(field("USER_ID").eq(param("USERID", ":userid")),
						field("MESSAGE_ID").eq(param("MESSAGEID", ":messageid"))));
	}

	public String getRemoveUndelivered() {
		return GETREMOVEUNDELIVERED;
	}

	private static String setupRemoveUserUndelivered() {
		return create.renderNamedParams(
				deleteFrom(table("UNDELIVERED")).where(field("USER_ID").eq(param("USERID", ":userid"))));
	}

	public String getRemoveUserUndelivered() {
		return GETREMOVEUSERUNDELIVERED;
	}

	private static String setupRemoveMessage() {
		return create.render(deleteFrom(table("MESSAGES"))
				.where(create.renderNamedParams(field("ID").eq(param("MESSAGEID", ":messageid"))
						.and(create.renderNamedParams(notExists(select().from(table("MESSAGES"))
								.join(table("UNDELIVERED")).on(field("ID").eq(field("MESSAGE_ID")))
								.and(field("ID").eq(param("MESSAGEID", ":messageid")))))))));
	}

	public String getRemoveMessage() {
		return GETREMOVEMESSAGE;
	}

	private static String setupRemoveUsers() {
		return create.render(
				deleteFrom(table("USERS")).where(create.renderNamedParams(field("ID").eq(param("USERID", ":userid"))
						.and(create.renderNamedParams(notExists(select().from(table("USERS")).join(table("UNDELIVERED"))
								.on(field("ID").eq(field("USER_ID")))
								.and(field("ID").eq(param("USERID", ":userid")))))))));
	}

	public String getRemoveUsers() {
		return GETREMOVEUSERS;
	}

	private static String setupUndeliveredMessage() {
		return create.renderNamedParams(
				select(field("USER_ID"), field("MESSAGE_ID")).from(table("MESSAGES")).join(table("UNDELIVERED"))
						.on(field("ID").eq(field("MESSAGE_ID"))).and(field("ID").eq(param("MESSAGEID", ":messageid")))
						.and(field("USER_ID").eq(param("USERID", ":userid"))));
	}

	public String getUndeliveredMessage() {
		return GETUNDELIVEREDMESSAGE;
	}

	private static String setupUserUndelivered() {
		return create.renderNamedParams(
				select(field("USER_ID"), field("MESSAGE_ID")).from(table("USERS")).join(table("UNDELIVERED"))
						.on(field("ID").eq(field("USER_ID"))).and(field("ID").eq(param("USERID", ":userid"))));
	}

	public String getUserUndelivered() {
		return GETUSERUNDELIVERED;
	}

	public Future<MessageUser> addUser(ServerWebSocket ws, Database db, MessageUser messageUser)
			throws InterruptedException, SQLException {
		Promise<MessageUser> promise = Promise.promise();

		Timestamp current = new Timestamp(new Date().getTime());
		db.update(getInsertUser()).parameter("NAME", messageUser.getName())
			.parameter("PASSWORD", messageUser.getPassword())
			.parameter("IP", messageUser.getIp())
			.parameter("LASTLOGIN", current)
			.returnGeneratedKeys()
			.getAs(Long.class)
			.subscribe(key -> {
				messageUser.setId(key);
				messageUser.setLastLogin(current);
				promise.tryComplete(messageUser);
			}, throwable -> {
				logger.error(String.join(ColorUtilConstants.RED, "Error adding user: ", throwable.getMessage(),
						ColorUtilConstants.RESET));
				throwable.printStackTrace();
				ws.writeTextMessage(throwable.getMessage());
			});

		return promise.future();
	}

	public Future<Integer> updateUser(ServerWebSocket ws, Database db, MessageUser messageUser)
			throws InterruptedException, SQLException {
		int count[] = { 0 };
		Timestamp dateTime = new Timestamp(new Date().getTime());

		Promise<Integer> promise = Promise.promise();

		db.update(getUpdateUser()).parameter("ID", messageUser.getId()).parameter("NAME", messageUser.getName())
			.parameter("PASSWORD", messageUser.getPassword())
			.parameter("IP", messageUser.getIp())
			.parameter("LASTLOGIN", dateTime)
			.counts()
			.doOnNext(c -> count[0] += c)
			.subscribe(result -> {
				promise.complete(count[0]);
			}, throwable -> {
				logger.error(String.join(ColorUtilConstants.RED, "Error Updating user: ", throwable.getMessage(),
						ColorUtilConstants.RESET));
				throwable.printStackTrace();
				ws.writeTextMessage(throwable.getMessage());
			});

		return promise.future();
	}

	public Future<Integer> updateCustomUser(ServerWebSocket ws, Database db, MessageUser messageUser, String type)
			throws InterruptedException, SQLException {
		int count[] = { 0 };

		Promise<Integer> promise = Promise.promise();

		db.update(getSqliteUpdateUser()).parameter("USERID", messageUser.getId())
			.parameter("LASTLOGIN",
					"date".equals(type) ? new Date().getTime() : new Timestamp(new Date().getTime()))
			.counts().doOnNext(c -> count[0] += c)
			.subscribe(result -> {
				promise.complete(count[0]);
			}, throwable -> {
				logger.error(String.join(ColorUtilConstants.RED, "Error Updating user: ", throwable.getMessage(),
						ColorUtilConstants.RESET));
				throwable.printStackTrace();
				ws.writeTextMessage(throwable.getMessage());
			});

		return promise.future();
	}

	private static String setupDeleteUser() {
		return create.renderNamedParams(deleteFrom(table("USERS")).where(field("NAME").eq(param("NAME", ":name")),
				field("PASSWORD").eq(param("PASSWORD", ":password"))));
	}

	public String getDeleteUser() {
		return GETDELETEUSER;
	}

	private static String setupDeleteUserById() {
		return create
				.renderNamedParams(deleteFrom(table("USERS")).where(field("ID").eq(param("USERID", ":userid"))));
	}

	public String getDeleteUserById() {
		return GETDELETEUSERBYID;
	}

	public Future<Long> deleteUser(ServerWebSocket ws, Database db, MessageUser messageUser)
			throws InterruptedException, SQLException {
		Promise<Long> promise = Promise.promise();

		db.update(getDeleteUser())
			.parameter("NAME", messageUser.getName())
			.parameter("PASSWORD", messageUser.getPassword())
			.counts()
			.subscribe(result -> {
				messageUser.setId(Long.parseLong(result.toString()));
				promise.complete(Long.parseLong(result.toString()));
			}, throwable -> {
				logger.error(String.join(ColorUtilConstants.RED, "Error deleting user: ", messageUser.getName(), " : ",
						throwable.getMessage(), ColorUtilConstants.RESET));
				throwable.printStackTrace();
				ws.writeTextMessage(throwable.getMessage());
			});
		
		return promise.future();
	}

	private static String setupAddMessage() {
		return create.renderNamedParams(
				insertInto(table("MESSAGES")).columns(field("MESSAGE"), field("FROM_HANDLE"), field("POST_DATE"))
						.values(param("MESSAGE", ":message"), param("FROMHANDLE", ":fromHandle"),
								param("POSTDATE", ":postdate")));
	}

	public String getAddMessage() {
		return GETADDMESSAGE;
	}

	public Future<Long> addMessage(ServerWebSocket ws, MessageUser messageUser, String message, Database db)
			throws InterruptedException, SQLException {
		Promise<Long> promise = Promise.promise();
		
		db.update(getAddMessage())
			.parameter("MESSAGE", message)
			.parameter("FROMHANDLE", messageUser.getName())
			.parameter("POSTDATE", new Timestamp(new Date().getTime()))
			.returnGeneratedKeys()
			.getAs(Long.class)
			.subscribe(result -> {
				promise.complete(result);
			}, throwable -> {
				logger.error(String.join(ColorUtilConstants.RED, "Error adding messaage: ", message, " : ",
						throwable.getMessage(), ColorUtilConstants.RESET));
				throwable.printStackTrace();
				ws.writeTextMessage(throwable.getMessage());
			});
		
		return promise.future();
	}

	private static String setupAddUndelivered() {
		return create
				.renderNamedParams(insertInto(table("UNDELIVERED")).columns(field("USER_ID"), field("MESSAGE_ID"))
						.values(param("USERID", ":userid"), param("MESSAGEID", ":messageid")));
	}

	public String getAddUndelivered() {
		return GETADDUNDELIVERED;
	}

	public Future<Void> addUndelivered(Long userId, Long messageId, Database db) throws SQLException, InterruptedException {
		Promise<Void> promise = Promise.promise();
		
		db.update(getAddUndelivered())
			.parameter("USERID", userId)
			.parameter("MESSAGEID", messageId)
			.counts()
			.subscribe(result -> {
				promise.complete(); 
			}, throwable -> {
				logger.error(String.join(ColorUtilConstants.RED, "Add Undelivered Error: ", throwable.getMessage(), ColorUtilConstants.RESET));
				throwable.printStackTrace();
			});

		return promise.future();
	}

	private static String setupUserNames() {
		return create.renderNamedParams(
				select(field("ID"), field("NAME"), field("PASSWORD"), field("IP"), field("LAST_LOGIN"))
						.from(table("USERS")).where(field("NAME").ne(param("NAME", ":name"))));
	}

	public String getUserNames() {
		return GETUSERNAMES;
	}

	public Future<Void> addUndelivered(ServerWebSocket ws, List<String> undelivered, Long messageId, Database db) {
		Promise<Void> promise = Promise.promise();
		try {
			for (String name : undelivered) {
				Future<Long> future = getUserIdByName(name, db);
				future.onSuccess(userId -> {
					try {
						Future<Void> future2 = addUndelivered(userId, messageId, db);
						future2.onSuccess(handler -> {
							promise.complete();
						});
					} catch (SQLException | InterruptedException e) {
						logger.error(String.join("", "AddUndelivered: ", e.getMessage()));
					}
				});
			}
		} catch (Exception e) {
			ws.writeTextMessage(e.getMessage());
		}
		return promise.future();
	}

	public Future<Long> getUserIdByName(String name, Database db) throws InterruptedException {
		Promise<Long> promise = Promise.promise();

		db.select(getUserByName())
			.parameter("NAME", name)
			.autoMap(Users.class)
			.doOnNext(result -> {
				promise.complete(result.id());
			})
			.subscribe(result -> {
			//
		}, throwable -> {
			logger.error(String.join(ColorUtilConstants.RED, "Error finding user by name: ", name, " : ",
					throwable.getMessage(), ColorUtilConstants.RESET));
			throwable.printStackTrace();
		});

		return promise.future();
	}

	public abstract MessageUser createMessageUser();

	public Future<MessageUser> selectUser(MessageUser messageUser, ServerWebSocket ws, Database db)
			throws InterruptedException, SQLException {
		MessageUser resultUser = createMessageUser();
		Promise<MessageUser> promise = Promise.promise();

		db.select(Users.class)
			.parameter(messageUser.getPassword())
			.get()
			.doOnNext(result -> {
				resultUser.setId(result.id());
				resultUser.setName(result.name());
				resultUser.setPassword(result.password());
				resultUser.setIp(result.ip());
				resultUser.setLastLogin(result.lastLogin());
			})
			.isEmpty()
			.doOnSuccess(empty -> {
				Future<Integer> future1 = null;
				if (empty) {
					Future<MessageUser> future2 = addUser(ws, db, messageUser);

					future2.onComplete(handler -> {
						MessageUser result = future2.result();
						resultUser.setId(result.getId());
						resultUser.setName(result.getName());
						resultUser.setPassword(result.getPassword());
						resultUser.setIp(result.getIp());
						resultUser.setLastLogin(result.getLastLogin());
						promise.complete(resultUser);
					});
				}
						
				if (DbConfiguration.isUsingSqlite3() && !empty) {
					try {
						future1 = updateCustomUser(ws, db, resultUser, "date");
						promise.complete(resultUser);
					} catch (InterruptedException | SQLException e) {
						e.printStackTrace();
					}
				} else if (DbConfiguration.isUsingIbmDB2() && !empty) {
					try {
						future1 = updateCustomUser(ws, db, resultUser, "timestamp");
						promise.complete(resultUser);
					} catch (InterruptedException | SQLException e) {
						e.printStackTrace();
					}
				} else if (!empty) {
					try {
						future1 = updateUser(ws, db, resultUser);
						promise.complete(resultUser);
					} catch (InterruptedException | SQLException e) {
						e.printStackTrace();
					}
				}
				if (!empty) {
					future1.onSuccess(v -> {
						// logger.info(String.join("", "Login Time Changed:", resultUser.getName()));
					});
				}		
			})
			.subscribe(result -> {
				//
			}, throwable -> {
				logger.error(String.join(ColorUtilConstants.RED, "Error adding user: ", messageUser.getName(), " : ", throwable.getMessage(), ColorUtilConstants.RESET));
				throwable.printStackTrace();
			});

		return promise.future();
	}

	public Future<StringBuilder> buildUsersJson(Database db, MessageUser messageUser)
			throws InterruptedException, SQLException {
		StringBuilder userJson = new StringBuilder();
		ObjectMapper mapper = new ObjectMapper();

		class User {
			String name;

			public void setName(String name) {
				this.name = name;
			}

			public String getName() {
				return name;
			}
		}
		class AllUsers implements Action {
			private Promise<StringBuilder> promise;
			@Override
			public void run() throws Exception {
				userJson.append(']');
				promise.complete(userJson);
			}
			public void setPromise(Promise<StringBuilder>promise) {
				this.promise = promise;
			}
			public Promise<StringBuilder> getPromise() {
				return promise;
			}
		}

		Promise<StringBuilder> promise = Promise.promise();
		AllUsers allUsers = new AllUsers();
		allUsers.setPromise(promise);
		List<String> delimiter = new ArrayList<>();
		User user = new User();
		userJson.append('[');
		delimiter.add("");

		db.select(getAllUsers())
			.parameter("NAME", messageUser.getName())
			.autoMap(Users.class)
			.doOnNext(result -> {
				// build json = ["name": "user1", "name": "user2", etc...]
				user.setName(result.name());
				userJson.append(delimiter.get(0) + mapper.writeValueAsString(user));
				delimiter.set(0, ",");
			})
			.doOnComplete(allUsers)
			.subscribe(result -> {
				//				
			}, throwable -> {
				logger.error(String.join(ColorUtilConstants.RED, "Error building registered user list: ", throwable.getMessage(), ColorUtilConstants.RESET));
				throwable.printStackTrace();
			});

		// wait for user json before sending back to newly connected user
		return allUsers.getPromise().future();
	}

	class RemoveUndelivered implements Action {
		List<Long> messageIds = new ArrayList<>();
		Long userId;
		int count;
		Database db;

		@Override
		public void run() throws Exception {
			for (Long messageId : messageIds) {
				db.update(getRemoveUndelivered())
					.parameter("USERID", userId)
					.parameter("MESSAGEID", messageId)
					.counts()
					.doOnNext(c -> count += c)
					.subscribe(result -> {
						//
					}, throwable -> {
						logger.error(String.join(ColorUtilConstants.RED, "Error removing undelivered record: ", throwable.getMessage(), ColorUtilConstants.RESET));
						throwable.printStackTrace();
					});
			}
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		public void setDatabase(Database db) {
			this.db = db;
		}

		public List<Long> getMessageIds() {
			return messageIds;
		}

		public void setMessageIds(List<Long> messageIds) {
			this.messageIds = messageIds;
		}

		public Long getUserId() {
			return userId;
		}

		public void setUserId(Long userId) {
			this.userId = userId;
		}
	}

	RemoveUndelivered removeUndelivered = new RemoveUndelivered();

	class RemoveMessage implements Action {
		int count;
		Database db;

		@Override
		public void run() throws Exception {
			for (Long messageId : removeUndelivered.getMessageIds()) {
				db.select(getUndeliveredMessage())
					.parameter("USERID", removeUndelivered.getUserId())
					.parameter("MESSAGEID", messageId)
					.getAs(Undelivered.class)
					.isEmpty()
					.doOnSuccess(empty -> {
						if (empty) {
							db.update(getRemoveMessage())
								.parameter("MESSAGEID", messageId)
								.counts()
								.doOnNext(c -> count += c)
								.subscribe(result -> {
									//
								}, throwable -> {
									logger.error(String.join(ColorUtilConstants.RED, "Error removing undelivered message: ", throwable.getMessage(), ColorUtilConstants.RESET));
									throwable.printStackTrace();
								});
						}
					}).subscribe(result -> {
						//
					}, throwable -> {
						logger.error(String.join(ColorUtilConstants.RED, "Error finding undelivered message: ", throwable.getMessage(), ColorUtilConstants.RESET));
						throwable.printStackTrace();
					});
			}
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		public void setDatabase(Database db) {
			this.db = db;
		}
	}

	RemoveMessage removeMessage = new RemoveMessage();

	public int processUserMessages(ServerWebSocket ws, Database db, MessageUser messageUser) {
		removeUndelivered.setUserId(messageUser.getId());
		removeUndelivered.setCount(0);
		removeUndelivered.setDatabase(db);
		removeMessage.setCount(0);
		removeMessage.setDatabase(db);
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
				removeUndelivered.getMessageIds().add(result.messageId());
			})
			// Remove undelivered for user
			.doOnComplete(removeUndelivered)
			.doOnError(error -> logger.error(String.join(ColorUtilConstants.RED, "Remove Undelivered Error: ", error.getMessage(), ColorUtilConstants.RESET)))
			// Remove message if no other users are attached
			.doFinally(removeMessage)
			.doOnError(error -> logger.error(String.join(ColorUtilConstants.RED, "Remove Message Error: ", error.getMessage(), ColorUtilConstants.RESET)))
			.subscribe();

		return removeUndelivered.getCount();
	}

	public void setIsTimestamp(Boolean isTimestamp) {
		this.isTimestamp = isTimestamp;
	}

	public boolean getisTimestamp() {
		return this.isTimestamp;
	}

	public Vertx getVertx() {
		return vertx;
	}

	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}
	
}
