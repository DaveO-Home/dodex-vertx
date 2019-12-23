
package dmo.fs.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.davidmoten.rx.jdbc.Database;
import org.jooq.DSLContext;

import dmo.fs.utils.ConsoleColors;
import dmo.fs.utils.DodexUtil;
import io.reactivex.disposables.Disposable;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import static org.jooq.impl.DSL.*;
import org.jooq.impl.DSL;

public abstract class DbDefinitionBase {
	private final static Logger logger = LoggerFactory.getLogger(DbDefinitionBase.class.getName());
	protected final static String QUERYUSERS = "select * from USERS where password=?";
	protected final static String QUERYMESSAGES = "select * from MESSAGES where id=?";
	protected final static String QUERYUNDELIVERED = "Select message_id, name, message, from_handle, post_date from USERS, UNDELIVERED, MESSAGES where USERS.id = user_id and MESSAGES.id = message_id and USERS.id = :id";
	
	private static DSLContext create = null;

	private static String GETALLUSERS = null;
	private static String GETUSERBYNAME = null;
	private static String GETINSERTUSER = null;
	private static String GETUPDATEUSER = null;
	private static String GETREMOVEUNDELIVERED = null;
	private static String GETREMOVEMESSAGE = null;
	private static String GETUNDELIVEREDMESSAGE = null;
	private static String GETDELETEUSER = null;
	private static String GETADDMESSAGE = null;
	private static String GETADDUNDELIVERED = null;
	private static String GETUSERNAMES = null;
	private static String GETUSERBYID = null;
	private static String GETREMOVEUSERUNDELIVERED = null;
	private static String GETUSERUNDELIVERED = null;
	private static String GETDELETEUSERBYID = null;
	private static String GETSQLITEUPDATEUSER = null;
	private static String GETREMOVEUSERS = null;

	public static void setupSql(Database db) throws SQLException {
		Connection conn = db.connection().blockingGet();
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

		conn.close();
	}

	private static String setupAllUsers() {
		String sql = create.renderNamedParams(
				select(field("ID"), field("NAME"), field("PASSWORD"), field("IP"), field("LAST_LOGIN"))
                   .from(table("USERS"))
                   .where(field("NAME").ne(param("NAME",":name"))));

		return sql;
	}

	public String getAllUsers() {
		return GETALLUSERS;
	}

	private static String setupUserByName() {
		String sql = create.renderNamedParams(
				select(field("ID"), field("NAME"), field("PASSWORD"), field("IP"), field("LAST_LOGIN"))
                   .from(table("USERS"))
                   .where(field("NAME").eq(param("NAME",":name"))));

	   return sql;
	}

	public String getUserByName() {
		return GETUSERBYNAME;	
	}

	private static String setupUserById() {
		String sql = create.renderNamedParams(
				select(field("ID"), field("NAME"), field("PASSWORD"), field("IP"), field("LAST_LOGIN"))
                   .from(table("USERS"))
                   .where(field("NAME").eq(param("NAME",":name"))));

	   return sql;
	}

	public String getUserById() {
		return GETUSERBYID;	
	}

	private static String setupInsertUser() {
		String sql = create.renderNamedParams(insertInto(table("USERS"))
			.columns(field("NAME"), field("PASSWORD"), field("IP"), field("LAST_LOGIN"))
			.values(
				param("NAME", ":name"), 
				param("PASSWORD", ":password"), 
				param("IP", ":ip"), 
				param("LASTLOGIN", ":lastlogin")));

		return sql;
	}

	public String getInsertUser() {
		return GETINSERTUSER;	
	}

	private static String setupUpdateUser() {
		String sql = create.renderNamedParams(mergeInto(table("USERS"))
		.columns(field("ID"), field("NAME"), field("PASSWORD"), field("IP"), field("LAST_LOGIN"))
		.key(field("ID"))
		.values(
			param("ID", ":id"), 
			param("NAME", ":name"), 
			param("PASSWORD", ":password"), 
			param("IP", ":ip"), 
			param("LASTLOGIN", ":lastlogin"))
		);
		return sql;
	}

	public String getUpdateUser() {
		return GETUPDATEUSER;	
	}

	public static String setupSqliteUpdateUser() {
		String sql = "update USERS set last_login = :LASTLOGIN where id = :USERID";

		return sql;
	}
	public String getSqliteUpdateUser() {
		return GETSQLITEUPDATEUSER;	
	}

	private static String setupRemoveUndelivered() {
		String sql = create.renderNamedParams(deleteFrom(table("UNDELIVERED"))
			.where(field("USER_ID")
				.eq(param("USERID",":userid")), field("MESSAGE_ID")
				.eq(param("MESSAGEID",":messageid"))));
		
		return sql;
	}

	public String getRemoveUndelivered() {
		return GETREMOVEUNDELIVERED;	
	}

	private static String setupRemoveUserUndelivered() {
		String sql = create.renderNamedParams(deleteFrom(table("UNDELIVERED"))
			.where(field("USER_ID")
				.eq(param("USERID",":userid"))));
		
		return sql;
	}

	public String getRemoveUserUndelivered() {
		return GETREMOVEUSERUNDELIVERED;	
	}

	private static String setupRemoveMessage() {
		String sql = create.render(deleteFrom(table("MESSAGES"))
			.where(
				create.renderNamedParams(field("ID").eq(param("MESSAGEID", ":messageid"))
					.and(create.renderNamedParams(notExists(select().from(table("MESSAGES"))
						.join(table("UNDELIVERED"))
						.on(field("ID").eq(field("MESSAGE_ID")))
						.and(field("ID").eq(param("MESSAGEID", ":messageid")))
					))))
			));

		return sql;
	}

	public String getRemoveMessage() {
		return GETREMOVEMESSAGE;	
	}


	private static String setupRemoveUsers() {
		String sql = create.render(deleteFrom(table("USERS"))
			.where(
				create.renderNamedParams(field("ID").eq(param("USERID", ":userid"))
					.and(create.renderNamedParams(notExists(select().from(table("USERS"))
						.join(table("UNDELIVERED"))
						.on(field("ID").eq(field("USER_ID")))
						.and(field("ID").eq(param("USERID", ":userid")))
					))))
			));

		return sql;
	}

	public String getRemoveUsers() {
		return GETREMOVEUSERS;	
	}

	private static String setupUndeliveredMessage() {
		String sql = create.renderNamedParams(select(field("USER_ID"), field("MESSAGE_ID"))
					.from(table("MESSAGES"))
					.join(table("UNDELIVERED"))
					.on(field("ID").eq(field("MESSAGE_ID")))
					.and(field("ID").eq(param("MESSAGEID", ":messageid")))
					.and(field("USER_ID").eq(param("USERID", ":userid")))
				);

		return sql;
    }

	public String getUndeliveredMessage() {
		return GETUNDELIVEREDMESSAGE;	
	}

	private static String setupUserUndelivered() {
		String sql = create.renderNamedParams(select(field("USER_ID"), field("MESSAGE_ID"))
					.from(table("USERS"))
					.join(table("UNDELIVERED"))
					.on(field("ID").eq(field("USER_ID")))
					.and(field("ID").eq(param("USERID", ":userid")))
				);

		return sql;
    }

	public String getUserUndelivered() {
		return GETUSERUNDELIVERED;	
	}

	public Long addUser(ServerWebSocket ws, Database db, MessageUser messageUser) throws InterruptedException, SQLException {
		Disposable disposable = db.update(getInsertUser())
				.parameter("NAME", messageUser.getName())
				.parameter("PASSWORD", messageUser.getPassword())
				.parameter("IP", messageUser.getIp())
				.parameter("LASTLOGIN", new Timestamp(new Date().getTime()))
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

	public int updateUser(ServerWebSocket ws, Database db, MessageUser messageUser) throws InterruptedException, SQLException {
		int count[] = {0};

		Disposable disposable = db.update(getUpdateUser())
				.parameter("ID", messageUser.getId())
				.parameter("NAME", messageUser.getName())
				.parameter("PASSWORD", messageUser.getPassword())
				.parameter("IP", messageUser.getIp())
				.parameter("LASTLOGIN", new Timestamp(new Date().getTime()))
				.counts()
				.doOnNext(c -> count[0] += c)
				.subscribe(result -> {
					//
				}, throwable -> {
					logger.error("{0}Error Updating user{1}", new Object[] { ConsoleColors.RED, ConsoleColors.RESET });
					throwable.printStackTrace();
					ws.writeTextMessage(throwable.getMessage());
				});
		await(disposable);

		return count[0];
	}

	public int updateSqliteUser(ServerWebSocket ws, Database db, MessageUser messageUser) throws InterruptedException, SQLException {
		int count[] = {0};

		Disposable disposable = db.update(getSqliteUpdateUser())
				.parameter("USERID", messageUser.getId())
				.parameter("LASTLOGIN", new Date().getTime())
				.counts()
				.doOnNext(c -> count[0] += c)
				.subscribe(result -> {
					//
				}, throwable -> {
					logger.error("{0}Error Updating user{1}", new Object[] { ConsoleColors.RED, ConsoleColors.RESET });
					throwable.printStackTrace();
					ws.writeTextMessage(throwable.getMessage());
				});
		await(disposable);

		return count[0];
	}

	private static String setupDeleteUser() {
		String sql = create.renderNamedParams(deleteFrom(table("USERS"))
			.where(field("NAME")
				.eq(param("NAME",":name")), field("PASSWORD")
				.eq(param("PASSWORD",":password"))));

		return sql;
	}

	public String getDeleteUser() {
		return GETDELETEUSER;
	}

	private static String setupDeleteUserById() {
		String sql = create.renderNamedParams(deleteFrom(table("USERS"))
			.where(field("ID")
				.eq(param("USERID",":userid"))));

		return sql;
	}

	public String getDeleteUserById() {
		return GETDELETEUSERBYID;
	}

	public long deleteUser(ServerWebSocket ws, Database db, MessageUser messageUser) throws InterruptedException, SQLException {
		Disposable disposable = db.update(getDeleteUser())
				.parameter("NAME", messageUser.getName())
				.parameter("PASSWORD", messageUser.getPassword())
				.counts()
				.subscribe(result -> {
					messageUser.setId(Long.parseLong(result.toString()));
				}, throwable -> {
					logger.error("{0}Error deleting user{1} {2}", new Object[] { ConsoleColors.RED, ConsoleColors.RESET, messageUser.getName() });
					throwable.printStackTrace();
					ws.writeTextMessage(throwable.getMessage());
				});
		await(disposable);
		
		return messageUser.getId();
	}

	private static String setupAddMessage() {
		String sql = create.renderNamedParams(insertInto(table("MESSAGES"))
			.columns(field("MESSAGE"), field("FROM_HANDLE"), field("POST_DATE"))
			.values(param("MESSAGE", ":message"), param("FROMHANDLE", ":fromHandle"), param("POSTDATE", ":postdate")));

		return sql;
	}

	public String getAddMessage() {
		return GETADDMESSAGE;
	}

	public long addMessage(ServerWebSocket ws, MessageUser messageUser, String message, Database db) throws InterruptedException, SQLException {
		List<Long> messageId = new ArrayList<>();
		
		Disposable disposable = db.update(getAddMessage())
				.parameter("MESSAGE", message)
				.parameter("FROMHANDLE", messageUser.getName())
				.parameter("POSTDATE", new Timestamp(new Date().getTime()))
				.returnGeneratedKeys()
				.getAs(Long.class)
				.doOnNext(k -> messageId.add(k)).subscribe(result -> {
					//
				}, throwable -> {
					logger.error("{0}Error adding message:{1} {2}",
							new Object[] { ConsoleColors.RED, ConsoleColors.RESET, message });
					throwable.printStackTrace();
					ws.writeTextMessage(throwable.getMessage());
				});
		await(disposable);

		return messageId.get(0);
	}

	private static String setupAddUndelivered() {
		String sql = create.renderNamedParams(insertInto(table("UNDELIVERED"))
			.columns(field("USER_ID"), field("MESSAGE_ID"))
			.values(param("USERID", ":userid"), param("MESSAGEID", ":messageid")));
		
		return sql;
	}

	public String getAddUndelivered() {
		return GETADDUNDELIVERED;
	}

	public void addUndelivered(Long userId, Long messageId, Database db) throws SQLException, InterruptedException {
		Disposable disposable = db.update(getAddUndelivered())
				.parameter("USERID", userId)
				.parameter("MESSAGEID", messageId)
				.complete()
				.doOnError(error -> logger.error("{0}Remove Undelivered Error: {1}{2}", new Object[] { ConsoleColors.RED, error.getMessage(), ConsoleColors.RESET }))
				.subscribe();
		await(disposable);
	}

	private static String setupUserNames() {
		String sql = create.renderNamedParams(
				select(field("ID"), field("NAME"), field("PASSWORD"), field("IP"), field("LAST_LOGIN"))
                   .from(table("USERS"))
                   .where(field("NAME").ne(param("NAME",":name"))));

		return sql;
	}

	public String getUserNames() {
		return GETUSERNAMES;
	}

	private void await(Disposable disposable) {
		while (!disposable.isDisposed()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
