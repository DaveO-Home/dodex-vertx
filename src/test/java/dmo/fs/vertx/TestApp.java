
package dmo.fs.vertx;

import dmo.fs.db.DbConfiguration;
import dmo.fs.db.DodexDatabase;
import dmo.fs.db.MessageUser;
import dmo.fs.db.cubrid.DbCubrid;
import dmo.fs.db.h2.DbH2;
import dmo.fs.db.mariadb.DbMariadb;
import dmo.fs.db.postgres.DbPostgres;
import dmo.fs.db.sqlite3.DbSqlite3;
import dmo.fs.utils.DodexUtil;
import golf.handicap.vertx.HandicapGrpcServer;
import golf.handicap.vertx.MainVerticle;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.vertx.core.Future;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.sqlclient.Pool;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.SqlConnection;
import io.vertx.rxjava3.sqlclient.Tuple;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

@Execution(ExecutionMode.SAME_THREAD)
class AppTest {
  Logger logger = LoggerFactory.getLogger(AppTest.class);

  @Test
  void getJavaResource() throws IOException {
    try (InputStream in = getClass().getResourceAsStream("/application-conf.json")) {
      assertNotNull(in, "should find resource file.");
    }
  }

  @Test
  void testDataFormatter() {
    Locale.setDefault(Locale.forLanguageTag("US"));
    DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault());
    LocalDate localDate = LocalDate.now();
    LocalTime localTime = LocalTime.of(
        LocalTime.now().getHour(),
        LocalTime.now().getMinute(),
        LocalTime.now().getSecond());
    ZonedDateTime zonedDateTime = ZonedDateTime.of(localDate, localTime, ZoneId.systemDefault());
    String openApiDate = zonedDateTime.format(formatter);
    Assertions.assertSame(Boolean.TRUE, openApiDate.length() > 8, "Date should be formatted to openapi format");
  }
}

@Execution(ExecutionMode.SAME_THREAD)
class UtilTest {
  String messageOnly = "This is a message.";
  String messageWithCommand = "This is a message.;removeuser";
  String messageWithCommandAndData = "This is a message.;removeuser!!{[\"name\",\"user1\"]}";

  @Test
  void getMessageFromRequest() {
    String description = "should return correct message";
    String message = DodexUtil.ClientInfoUtilHelper.getMessage.apply(messageOnly);
    assertEquals("This is a message.", message, description);
    message = DodexUtil.ClientInfoUtilHelper.getMessage.apply(messageWithCommand);
    assertEquals("This is a message.", message, description);
    message = DodexUtil.ClientInfoUtilHelper.getMessage.apply(messageWithCommandAndData);
    assertEquals("This is a message.", message, description);
  }

  @Test
  void getCommandFromRequest() {
    String description = "should return a command";
    String command = DodexUtil.ClientInfoUtilHelper.getCommand.apply(messageOnly);
    assertNull(command, "command should be nul");
    command = DodexUtil.ClientInfoUtilHelper.getCommand.apply(messageWithCommand);
    assertEquals(";removeuser", command, description);
    command = DodexUtil.ClientInfoUtilHelper.getCommand.apply(messageWithCommandAndData);
    assertEquals(";removeuser", command, description);
  }

  @Test
//  @Disabled
  void getDataFromRequest() {
    String data = DodexUtil.ClientInfoUtilHelper.getData.apply(messageOnly);
    assertNull(data, "should be null");
    data = DodexUtil.ClientInfoUtilHelper.getData.apply(messageWithCommand);
    assertNull(data, "should be null");
    data = DodexUtil.ClientInfoUtilHelper.getData.apply(messageWithCommandAndData);
    assertEquals("{[\"name\",\"user1\"]}", data, "should return data");
  }

  @Test
  void testUsersCommand() {
    String command = DodexUtil.ClientInfoUtilHelper.getCommand.apply(";users!!{[\"name\",\"user1\"]}");
    assertEquals(";users", command, "should return the users command");
  }
}

@Execution(ExecutionMode.SAME_THREAD)
@TestInstance(Lifecycle.PER_CLASS)
//@TestMethodOrder(MethodOrderer.MethodName.class)
class DbTest /* extends DbDefinitionBase */ {
  Logger logger = LoggerFactory.getLogger(DbTest.class);

  MessageUser messageUser;
  MessageUser resultUser;
  DodexDatabase dodexDatabase;
  Pool pool;
  String checkSql;

  @BeforeAll
  void testDatabaseSetup() throws InterruptedException, IOException, SQLException {
    String whichDb = "h2"; // postgres || sqlite3
    DodexUtil.setVertx(Vertx.vertx());
//    System.setProperty("kotlinTest", "true");

    if (System.getenv("DEFAULT_DB") != null) {
      whichDb = System.getenv("DEFAULT_DB");
    }
    Properties props = new Properties();
    props.setProperty("user", "sa"); // <------- change to use
    props.setProperty("password", "sa"); // <------- change to use
    props.setProperty("ssl", "false");
    if ("sqlite3".equals(whichDb)) {
      dodexDatabase = DbConfiguration.getDefaultDb();
      pool = dodexDatabase.getPool();
      checkSql = DbSqlite3.CHECKUSERSQL;
      DbSqlite3.setupSql(pool);
    } else if ("h2".equals(whichDb)) {
      dodexDatabase = DbConfiguration.getDefaultDb();
      pool = dodexDatabase.getPool();
      checkSql = DbH2.CHECKUSERSQL;
//      HandicapGrpcServer.Companion.setEnableHandicap(true);
      DbH2.setupSql(pool);
    } else if ("postgres".equals(whichDb)) {
      Map<String, String> overrideMap = new ConcurrentHashMap<>();
      overrideMap.put("database", "dodex"); // <------- should match test/dev db
      dodexDatabase = DbConfiguration.getDefaultDb(overrideMap, props);
      pool = dodexDatabase.getPool();
      checkSql = DbPostgres.CHECKUSERSQL;
      DbPostgres.setupSql(pool);
    } else if ("mariadb".equals(whichDb)) {
      Map<String, String> overrideMap = new ConcurrentHashMap<>();
      overrideMap.put("database", "test"); // <------- should match test/dev db
      dodexDatabase = DbConfiguration.getDefaultDb(overrideMap, props);
      pool = dodexDatabase.getPool();
      checkSql = DbMariadb.CHECKUSERSQL;
      DbMariadb.setupSql(pool);
    } else if ("cubrid".equals(whichDb)) {
      Map<String, String> overrideMap = new ConcurrentHashMap<>();
      overrideMap.put("database", "dodex"); // <------- should match test/dev db
      dodexDatabase = DbConfiguration.getDefaultDb(overrideMap, props);
      pool = dodexDatabase.getPool();
      checkSql = DbCubrid.CHECKUSERSQL;
      DbCubrid.setupSql(pool);
    }

    messageUser = dodexDatabase.createMessageUser();
    resultUser = dodexDatabase.createMessageUser();

    messageUser.setName("User1");
    messageUser.setPassword("Password");
    // Make sure test user is removed from database
//    Single<SqlConnection> connection = pool.getConnection();
//    Tuple parameters = Tuple.of(messageUser.getName(), messageUser.getPassword());
//
//    Disposable disposable = connection
//        .doOnSuccess(conn -> conn.preparedQuery(dodexDatabase.getDeleteUser())
//        .execute(parameters).subscribe(r -> {
//          conn.close().subscribe();
//        }, err -> {
//          conn.close().subscribe();
//          logger.info("Error First delete: {}", err.getMessage());
//          throw new Exception(err);
//        })).subscribe();
//
//    await(disposable);
//    assertTrue(disposable.isDisposed(), "Delete action is disposed");
  }

  @Test
  @Order(1)
  void t1databaseSetup() {
    assertNotEquals(null, dodexDatabase, "dodexDatabase should be created");
    pool.rxGetConnection().doOnSuccess(value -> {
      assertNotNull(value, "database pool should exist");
    }).doOnError(Throwable::printStackTrace).subscribe();
  }

  @Test
  @Order(2)
  void t2doesUsersTableExist() throws InterruptedException {
    String[] table = {null};

    Single<SqlConnection> con = pool.rxGetConnection();
    Disposable testDisposable = con.subscribe(c -> {
      c.query(checkSql).rxExecute().doOnSuccess(rows -> {
            for (Row row : rows) {
              table[0] = row.getString(0).toLowerCase();
            }
          }).doOnError(Throwable::printStackTrace)
          .subscribe(r -> {
            c.close().subscribe();
          });
    });

    assertFalse(testDisposable.isDisposed(), "Query not yet complete");
    await(testDisposable);
    assertEquals("users", table[0], "Users Table Exists");
  }

  @Test
  @Order(3)
  void t3testDatabaseAndReactiveVertx() throws InterruptedException, SQLException {
    Disposable[] testDisposable = {null, null};
    boolean[] emptyTable = {false, false, false};

    messageUser.setIp("0");
    messageUser.setId(-1L);
    resultUser.setId(0L);
    resultUser.setName(null);

    Single<SqlConnection> conn = pool.rxGetConnection();

    testDisposable[0] = conn.subscribe(c -> {
      c.preparedQuery(dodexDatabase.getUserById())
          .rxExecute(Tuple.of(messageUser.getName(), messageUser.getPassword()))
          .doOnSuccess(rows -> {
            if (rows.size() == 0) {
              emptyTable[0] = true;
              Future<MessageUser> future2 = dodexDatabase.addUser(null, messageUser);

              future2.onComplete(handler -> {
                emptyTable[1] = messageUser.getId() > 0l;
                MessageUser result = future2.result();
                resultUser.setId(result.getId());
                resultUser.setName(result.getName());
                resultUser.setPassword(result.getPassword());
                resultUser.setIp(result.getIp());
                resultUser.setLastLogin(result.getLastLogin());
              });
            } else {
              emptyTable[1] = false;
              emptyTable[0] = false;
              for (Row row : rows) {
                resultUser.setId(row.getLong(0));
                resultUser.setName(row.getString(1));
                resultUser.setPassword(row.getString(2));
                resultUser.setIp(row.getString(3));
                resultUser.setLastLogin(row.getValue(4));
              }
            }
          }).doOnError(Throwable::printStackTrace)
          .subscribe(result -> {
            if (result.size() > 0) {
              emptyTable[1] = true;
            }
            emptyTable[2] = true;
            c.getDelegate().close(); //.subscribe();
          });
    });

    Assertions.assertSame(false, emptyTable[0], "reactive vertx should be running asynchronously");

    await(testDisposable[0]);

    Assertions.assertSame(true, emptyTable[1], "user should not be found & added to table");
    Assertions.assertTrue(resultUser.getId() > 0, "user id should be generated");
    assertEquals("User1", resultUser.getName(), "user should be retrieved");
    Assertions.assertTrue(emptyTable[2], "subscribe should finish");
  }

  @Test
  @Order(4)
  void t4deleteUserFromDatabase() {
    int[] deleted = {0};
    Single<SqlConnection> connection = pool.rxGetConnection();

    Disposable testDisposable = connection.subscribe(conn -> {
      conn.preparedQuery(dodexDatabase.getDeleteUser())
          .rxExecute(Tuple.of(messageUser.getName(), messageUser.getPassword()))
          .doOnSuccess(rows -> {
            deleted[0] = 1;
            conn.rxClose().subscribe();
          }).doOnError(err -> {
            conn.rxClose().subscribe();
          }).subscribe();
    });


    Assertions.assertSame(Boolean.TRUE, deleted[0] == 0, "user deletion should not start yet");

    await(testDisposable);

    Assertions.assertSame(Boolean.TRUE, deleted[0] == 1, "user deleted or not in database");
  }

  @Test
  @Order(5)
  void t5batachQueryDatabase() {
    Integer[] rowSize = {0};

    Single<SqlConnection> conn = pool.getConnection();

    Disposable testDisposable = conn.subscribe(connection -> {
      List<Tuple> userList = new ArrayList<Tuple>();
      String x = "daveo🎱";
      String y = "daveo🌵";

      connection.preparedQuery("select ID, NAME, PASSWORD, IP, LAST_LOGIN from USERS where NAME = $1")
          .executeBatch(Arrays.asList(
              Tuple.of(x),
              Tuple.of(y)
          ))
          .doOnSuccess(rows -> {
            rowSize[0] = rows.size();
            for (Row row : rows) {
              // addGroupJson.put("id", row.getLong(0));
            }

            Assertions.assertSame(Boolean.TRUE, rowSize[0] == 0, "Batch Size should be");
          }).subscribe(c -> {
            connection.close().subscribe();
          }, err -> {
            logger.info("Error: " + err.getMessage());
            connection.close().subscribe();
          });
    });
  }

  public void await(Disposable disposable) {
    int count = 0;
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    while ((disposable == null || !disposable.isDisposed()) && count++ < 50) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
