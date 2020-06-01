
package dmo.fs.spa.db;

import static org.jooq.impl.DSL.deleteFrom;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.insertInto;
import static org.jooq.impl.DSL.mergeInto;
import static org.jooq.impl.DSL.param;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.table;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import org.davidmoten.rx.jdbc.Database;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import dmo.fs.spa.db.RxJavaDateDb.Login;
import dmo.fs.spa.utils.SpaLogin;
import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class SqlBuilder {
	private final static Logger logger = LoggerFactory.getLogger(SqlBuilder.class.getName());
	protected final static String QUERYLOGIN = "select * from LOGIN where name=?";

	private static DSLContext create;

	private static String GETLOGINBYNP;
	private static String GETLOGINBYNAME;
	private static String GETINSERTLOGIN;
	private static String GETREMOVELOGIN;
	private static String GETUPDATELOGIN;
	private static String GETLOGINBYID;
	private static String GETSQLITEUPDATELOGIN;
	private Boolean isTimestamp;

	public static void setupSql(Database db) throws SQLException {
		try(Connection conn = db.connection().blockingGet()) {
			create = DSL.using(conn, DodexUtil.getSqlDialect());

			GETLOGINBYNP = setupLoginByNamePassword();
			GETLOGINBYNAME = setupLoginByName();
			GETINSERTLOGIN = setupInsertLogin();
			GETREMOVELOGIN = setupRemoveLogin();
			GETLOGINBYID = setupLoginById();
			GETUPDATELOGIN = setupUpdateLogin();
			GETSQLITEUPDATELOGIN = setupSqliteUpdateLogin();
		}
	}

	private static String setupLoginByNamePassword() {
		return create.renderNamedParams(select(field("ID"), field("NAME"), field("PASSWORD"), field("LAST_LOGIN"))
				.from(table("LOGIN")).where(field("NAME").eq(param("NAME", ":name")))
				.and(field("PASSWORD").eq(param("PASSWARD", ":password"))));
	}

	public String getLoginByNamePassword() {
		return GETLOGINBYNP;
	}

	private static String setupLoginByName() {
		return create.renderNamedParams(select(field("ID"), field("NAME"), field("PASSWORD"), field("LAST_LOGIN"))
				.from(table("LOGIN")).where(field("NAME").eq(param("NAME", ":name"))));
	}

	public String getUserByName() {
		return GETLOGINBYNAME;
	}

	private static String setupLoginById() {
		return create.renderNamedParams(select(field("ID"), field("NAME"), field("PASSWORD"), field("LAST_LOGIN"))
				.from(table("LOGIN")).where(field("NAME").eq(param("NAME", ":name"))));
	}

	public String getUserById() {
		return GETLOGINBYID;
	}

	private static String setupInsertLogin() {
		return create.renderNamedParams(
				insertInto(table("LOGIN")).columns(field("NAME"), field("PASSWORD"), field("LAST_LOGIN")).values(
						param("NAME", ":name"), param("PASSWORD", ":password"), param("LASTLOGIN", ":lastlogin")));
	}

	public String getInsertLogin() {
		return GETINSERTLOGIN;
	}

	private static String setupUpdateLogin() {
		return create.renderNamedParams(
				mergeInto(table("LOGIN")).columns(field("ID"), field("NAME"), field("PASSWORD"), field("LAST_LOGIN"))
						.key(field("ID")).values(param("ID", ":id"), param("NAME", ":name"),
								param("PASSWORD", ":password"), param("LASTLOGIN", ":lastlogin")));
	}

	public String getUpdateLogin() {
		return GETUPDATELOGIN;
	}

	public static String setupSqliteUpdateLogin() {
		return "update LOGIN set last_login = :LASTLOGIN where id = :LOGINID";
	}

	public String getSqliteUpdateLogin() {
		return GETSQLITEUPDATELOGIN;
	}

	public abstract SpaLogin createSpaLogin();

	public Future<SpaLogin> getLogin(SpaLogin spaLogin, Database db)
			throws InterruptedException, SQLException {
		Promise<SpaLogin> promise = Promise.promise();
		
		SpaLogin resultLogin = createSpaLogin();
		db.select(Login.class)
			.parameter(spaLogin.getName())
			.parameters(spaLogin.getPassword())
			.get()
			.doOnNext(result -> {
				resultLogin.setId(result.id());
				resultLogin.setName(result.name());
				resultLogin.setPassword(result.password());
				resultLogin.setLastLogin(result.lastLogin());
			}).isEmpty().doOnSuccess(empty -> {
				if (empty || !(resultLogin.getPassword().equals(spaLogin.getPassword()))) {
					resultLogin.setStatus("-1");
					resultLogin.setId(0l);
					resultLogin.setName(spaLogin.getName());
					resultLogin.setPassword(spaLogin.getPassword());
					resultLogin.setLastLogin(new Date());
				} else {
					resultLogin.setStatus("0");
				}
			}).subscribe(result -> {
				if("0".equals(resultLogin.getStatus())) {
					if(SpaDbConfiguration.isUsingSqlite3()) {
						Future<Integer> future = updateCustomLogin(db, resultLogin, "date");
						future.onSuccess(result2 -> {
							promise.complete(resultLogin);
						});

						future.onFailure(failed -> {
							logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT, "Add Login Update failed...: ", failed.getMessage(), ColorUtilConstants.RESET ));
							resultLogin.setStatus("-99");
							promise.complete(resultLogin);
						});
					} 
				}
				else {
					promise.complete(resultLogin);
				}
			}, throwable -> {
				resultLogin.setStatus("-99");
				promise.complete(resultLogin);
				logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT, "Error retrieving user: ", spaLogin.getName(), " : ", throwable.getMessage(), ColorUtilConstants.RESET ));
				throwable.printStackTrace();
			});

		return promise.future();
	}

	public Future<SpaLogin> addLogin(SpaLogin spaLogin, Database db)
			throws InterruptedException, SQLException {
		Promise<SpaLogin> promise = Promise.promise();
	
		spaLogin.setStatus("0");
		db.update(getInsertLogin()).parameter("NAME", spaLogin.getName())
			.parameter("PASSWORD", spaLogin.getPassword())
			.parameter("LASTLOGIN", new Timestamp(new Date().getTime()))
			.returnGeneratedKeys()
			.getAs(Long.class)
			.doOnNext(k -> spaLogin.setId(k))
			.subscribe(result -> {
				promise.complete(spaLogin);
			},throwable -> {
				spaLogin.setStatus("-4");
				promise.complete(spaLogin);
				logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT, "Error adding user -  ", spaLogin.getName(), " : ", throwable.getMessage(), ColorUtilConstants.RESET ));
				throwable.printStackTrace();
			});
		
		return promise.future();
	}

	private static String setupRemoveLogin() {
		return create.renderNamedParams(deleteFrom(table("LOGIN")).where(field("NAME").eq(param("NAME", ":name")),
				field("PASSWORD").eq(param("PASSWORD", ":password"))));
	}

	public String getRemoveLogin() {
		return GETREMOVELOGIN;
	}

	public Future<SpaLogin> removeLogin(SpaLogin spaLogin, Database db)
			throws InterruptedException, SQLException {
		Promise<SpaLogin> promise = Promise.promise();
		
		db.update(getRemoveLogin()).parameter("NAME", spaLogin.getName())
			.parameter("PASSWORD", spaLogin.getPassword())
			.counts()
			.subscribe(result -> {
				spaLogin.setStatus(result.toString()); // result = records deleted
				if(spaLogin.getId() == null) {
					spaLogin.setId(-1l);
				}
				if(spaLogin.getLastLogin() == null) {
					spaLogin.setLastLogin(new Date());
				}
				promise.complete(spaLogin);
			}, throwable -> {
				spaLogin.setStatus("-4");
				promise.complete(spaLogin);
				logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT, "Error deleting user: ", spaLogin.getName(), " : ", throwable.getMessage(), ColorUtilConstants.RESET ));
				throwable.printStackTrace();
			});

		return promise.future();
	}

	public Future<Integer> updateCustomLogin(Database db, SpaLogin spaLogin, String type)
			throws InterruptedException, SQLException {
		Promise<Integer> promise = Promise.promise();

		int count[] = { 0 };
		db.update(getSqliteUpdateLogin()).parameter("LOGINID", spaLogin.getId())
			.parameter("LASTLOGIN", "date".equals(type)? new Date().getTime(): new Timestamp(new Date().getTime()))
			.counts()
			.doOnNext(c -> count[0] += c)
			.subscribe(result -> {
				promise.complete(count[0]);
			}, throwable -> {
				promise.complete(-99);
				logger.error(String.join("", ColorUtilConstants.RED_BOLD_BRIGHT, "Error Updating user: ", spaLogin.getName(), " : ", throwable.getMessage(), ColorUtilConstants.RESET ));
				throwable.printStackTrace();
			});

		return promise.future();
	}

	public void setIsTimestamp(Boolean isTimestamp) {
		this.isTimestamp = isTimestamp;
	}

	public Boolean getIsTimestamp() {
		return isTimestamp;
	}
}
