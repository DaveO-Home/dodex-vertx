
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
import dmo.fs.utils.ConsoleColors;
import dmo.fs.utils.DodexUtil;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class SqlBuilder {
	private final static Logger logger = LoggerFactory.getLogger(SqlBuilder.class.getName());
	protected final static String QUERYLOGIN = "select * from LOGIN where name=?";

	private static DSLContext create = null;

	private static String GETLOGINBYNP = null;
	private static String GETLOGINBYNAME = null;
	private static String GETINSERTLOGIN = null;
	private static String GETREMOVELOGIN = null;
	private static String GETUPDATELOGIN = null;
	private static String GETLOGINBYID = null;
	private static String GETSQLITEUPDATELOGIN = null;
	private Boolean isTimestamp = null;

	public static void setupSql(Database db) throws SQLException {
		Connection conn = db.connection().blockingGet();
		create = DSL.using(conn, DodexUtil.getSqlDialect());

		GETLOGINBYNP = setupLoginByNamePassword();
		GETLOGINBYNAME = setupLoginByName();
		GETINSERTLOGIN = setupInsertLogin();
		GETREMOVELOGIN = setupRemoveLogin();
		GETLOGINBYID = setupLoginById();
		GETUPDATELOGIN = setupUpdateLogin();
		GETSQLITEUPDATELOGIN = setupSqliteUpdateLogin();

		conn.close();
	}

	private static String setupLoginByNamePassword() {
		String sql = create.renderNamedParams(select(field("ID"), field("NAME"), field("PASSWORD"), field("LAST_LOGIN"))
				.from(table("LOGIN")).where(field("NAME").eq(param("NAME", ":name")))
				.and(field("PASSWORD").eq(param("PASSWARD", ":password"))));

		return sql;
	}

	public String getLoginByNamePassword() {
		return GETLOGINBYNP;
	}

	private static String setupLoginByName() {
		String sql = create.renderNamedParams(select(field("ID"), field("NAME"), field("PASSWORD"), field("LAST_LOGIN"))
				.from(table("LOGIN")).where(field("NAME").eq(param("NAME", ":name"))));

		return sql;
	}

	public String getUserByName() {
		return GETLOGINBYNAME;
	}

	private static String setupLoginById() {
		String sql = create.renderNamedParams(select(field("ID"), field("NAME"), field("PASSWORD"), field("LAST_LOGIN"))
				.from(table("LOGIN")).where(field("NAME").eq(param("NAME", ":name"))));

		return sql;
	}

	public String getUserById() {
		return GETLOGINBYID;
	}

	private static String setupInsertLogin() {
		String sql = create.renderNamedParams(
				insertInto(table("LOGIN")).columns(field("NAME"), field("PASSWORD"), field("LAST_LOGIN")).values(
						param("NAME", ":name"), param("PASSWORD", ":password"), param("LASTLOGIN", ":lastlogin")));

		return sql;
	}

	public String getInsertLogin() {
		return GETINSERTLOGIN;
	}

	private static String setupUpdateLogin() {
		String sql = create.renderNamedParams(
				mergeInto(table("LOGIN")).columns(field("ID"), field("NAME"), field("PASSWORD"), field("LAST_LOGIN"))
						.key(field("ID")).values(param("ID", ":id"), param("NAME", ":name"),
								param("PASSWORD", ":password"), param("LASTLOGIN", ":lastlogin")));
		return sql;
	}

	public String getUpdateLogin() {
		return GETUPDATELOGIN;
	}

	public static String setupSqliteUpdateLogin() {
		String sql = "update LOGIN set last_login = :LASTLOGIN where id = :LOGINID";

		return sql;
	}

	public String getSqliteUpdateLogin() {
		return GETSQLITEUPDATELOGIN;
	}

	public abstract SpaLogin createSpaLogin();

	public Future<SpaLogin> getLogin(SpaLogin spaLogin, Database db)
			throws InterruptedException, SQLException {
		Promise<SpaLogin> promise = Promise.promise();
		
		Future.future(prom -> {
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
					} else {
						resultLogin.setStatus("0");
					}
				}).subscribe(result -> {
					if(resultLogin.getStatus().equals("0")) {
						if(SpaDbConfiguration.isUsingSqlite3()) {
							Future<Integer> future = updateCustomLogin(db, resultLogin, "date");
							future.onSuccess(result2 -> {
								promise.complete(resultLogin);
							});

							future.onFailure(failed -> {
								logger.error("{0}Add Login Update failed...{1}{2} ", new Object[] {
										ConsoleColors.RED_BOLD_BRIGHT, failed.getMessage(), ConsoleColors.RESET });
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
					logger.error("{0}Error retrieving user {1}{2}",
							new Object[] { ConsoleColors.RED, spaLogin.getName(), ConsoleColors.RESET });
					throwable.printStackTrace();
				});
		});

		return promise.future();
	}

	public Future<SpaLogin> addLogin(SpaLogin spaLogin, Database db)
			throws InterruptedException, SQLException {
		Promise<SpaLogin> promise = Promise.promise();
	
		Future.future(prom -> {
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
					logger.error("{0}Error adding user - {1}{2}",
							new Object[] { ConsoleColors.RED, spaLogin.getName(), ConsoleColors.RESET });
					throwable.printStackTrace();
				});
		});
		
		return promise.future();
	}

	private static String setupRemoveLogin() {
		String sql = create.renderNamedParams(deleteFrom(table("LOGIN")).where(field("NAME").eq(param("NAME", ":name")),
				field("PASSWORD").eq(param("PASSWORD", ":password"))));

		return sql;
	}

	public String getRemoveLogin() {
		return GETREMOVELOGIN;
	}

	public Future<SpaLogin> removeLogin(SpaLogin spaLogin, Database db)
			throws InterruptedException, SQLException {
		Promise<SpaLogin> promise = Promise.promise();
		
		Future.future(prom -> {
			db.update(getRemoveLogin()).parameter("NAME", spaLogin.getName())
				.parameter("PASSWORD", spaLogin.getPassword())
				.counts()
				.subscribe(result -> {
					spaLogin.setStatus(result.toString()); // result = records deleted
					promise.complete(spaLogin);
				}, throwable -> {
					spaLogin.setStatus("-4");
					promise.complete(spaLogin);
					logger.error("{0}Error deleting user{1} {2}",
							new Object[] { ConsoleColors.RED, ConsoleColors.RESET, spaLogin.getName() });
					throwable.printStackTrace();
				});
		});

		return promise.future();
	}

	public Future<Integer> updateCustomLogin(Database db, SpaLogin spaLogin, String type)
			throws InterruptedException, SQLException {
		Promise<Integer> promise = Promise.promise();

		Future.future(prom -> {
			int count[] = { 0 };
			db.update(getSqliteUpdateLogin()).parameter("LOGINID", spaLogin.getId())
				.parameter("LASTLOGIN", type.equals("date")? new Date().getTime(): new Timestamp(new Date().getTime()))
				.counts()
				.doOnNext(c -> count[0] += c)
				.subscribe(result -> {
					promise.complete(count[0]);
				}, throwable -> {
					promise.complete(-99);
					logger.error("{0}Error Updating user{1}", new Object[] { ConsoleColors.RED, ConsoleColors.RESET });
					throwable.printStackTrace();
				});
		});

		return promise.future();
	}

	public void setIsTimestamp(Boolean isTimestamp) {
		this.isTimestamp = isTimestamp;
	}

	// private void await(Disposable disposable, Vertx vertx) {
	// 	while (!disposable.isDisposed()) {
	// 		vertx.setTimer(100, l -> {
	// 			if (disposable.isDisposed()) {
	// 				return;
	// 			}
	// 		});
	// 	}
	// }
}
