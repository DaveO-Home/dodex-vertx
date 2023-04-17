
package dmo.fs.vertx;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dmo.fs.db.DbConfiguration;
import dmo.fs.db.DbCubrid;
import dmo.fs.db.DbMariadb;
import dmo.fs.db.DbPostgres;
import dmo.fs.db.DbSqlite3;
import dmo.fs.db.DodexDatabase;
import dmo.fs.db.DodexDatabaseSqlite3;
import dmo.fs.db.MessageUser;
import dmo.fs.utils.DodexUtil;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.vertx.core.Future;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.sqlclient.Pool;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.SqlConnection;
import io.vertx.rxjava3.sqlclient.Tuple;

class AppTest {
    Logger logger = LoggerFactory.getLogger(AppTest.class.getName());

    @Test
    void getJavaResource() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/application-conf.json")) {
            assertNotNull(in, "should find resource file.");
        }
    }
}

class UtilTest {
    String messageOnly = "This is a message.";
    String messageWithCommand = "This is a message.;removeuser";
    String messageWithCommandAndData = "This is a message.;removeuser!!{[\"name\",\"user1\"]}";

    @Test
    void getMessageFromRequest() {
        String description = "should return correct message";
        String message = DodexUtil.ClientInfoUtilHelper.getMessage.apply(messageOnly);
        assertEquals(message, "This is a message.", description);
        message = DodexUtil.ClientInfoUtilHelper.getMessage.apply(messageWithCommand);
        assertEquals(message, "This is a message.", description);
        message = DodexUtil.ClientInfoUtilHelper.getMessage.apply(messageWithCommandAndData);
        assertEquals(message, "This is a message.", description);
    }

    @Test
    void getCommandFromRequest() {
        String description = "should return a command";
        String command = DodexUtil.ClientInfoUtilHelper.getCommand.apply(messageOnly);
        assertEquals(command, null, "command should be nul");
        command = DodexUtil.ClientInfoUtilHelper.getCommand.apply(messageWithCommand);
        assertEquals(command, ";removeuser", description);
        command = DodexUtil.ClientInfoUtilHelper.getCommand.apply(messageWithCommandAndData);
        assertEquals(command, ";removeuser", description);
    }

    @Test
    void getDataFromRequest() {
        String data = DodexUtil.ClientInfoUtilHelper.getData.apply(messageOnly);
        assertEquals(data, null, "should be null");
        data = DodexUtil.ClientInfoUtilHelper.getData.apply(messageWithCommand);
        assertEquals(data, null, "should be null");
        data = DodexUtil.ClientInfoUtilHelper.getData.apply(messageWithCommandAndData);
        assertEquals(data, "{[\"name\",\"user1\"]}", "should return data");
    }

    @Test
    void testUsersCommand() {
        String command = DodexUtil.ClientInfoUtilHelper.getCommand.apply(";users!!{[\"name\",\"user1\"]}");
        assertEquals(command, ";users", "should return the users command");
    }
}

@TestInstance(Lifecycle.PER_CLASS)
class DbTest /* extends DbDefinitionBase */ {
    Logger logger = LoggerFactory.getLogger(DbTest.class.getName());

    MessageUser messageUser;
    MessageUser resultUser;
    DodexDatabase dodexDatabase;
    Pool pool;
    String checkSql = DbSqlite3.CHECKUSERSQL;

    @BeforeAll
    void testDatabaseSetup() throws InterruptedException, IOException, SQLException {
        String whichDb = "sqlite3"; // postgres || sqlite3
        DodexUtil.setVertx(Vertx.vertx());

        if (System.getenv("DEFAULT_DB") != null) {
            whichDb = System.getenv("DEFAULT_DB");
        }
        Properties props = new Properties();
        props.setProperty("user", "dodex"); // <------- change to use
        props.setProperty("password", "dodex"); // <------- change to use
        props.setProperty("ssl", "false");

        if ("sqlite3".equals(whichDb)) {
            dodexDatabase = DbConfiguration.getDefaultDb();
            pool = dodexDatabase.getPool4();
        } else if ("postgres".equals(whichDb)) {
            Map<String, String> overrideMap = new ConcurrentHashMap<>();
            overrideMap.put("database", "dodex"); // <------- should match test/dev db
            dodexDatabase = DbConfiguration.getDefaultDb(overrideMap, props);
            pool = dodexDatabase.getPool4();
            checkSql = DbPostgres.CHECKUSERSQL;
        } else if ("mariadb".equals(whichDb)) {
            Map<String, String> overrideMap = new ConcurrentHashMap<>();
            overrideMap.put("database", "dodex"); // <------- should match test/dev db
            dodexDatabase = DbConfiguration.getDefaultDb(overrideMap, props);
            pool = dodexDatabase.getPool4();
            checkSql = DbMariadb.CHECKUSERSQL;
        } else if ("cubrid".equals(whichDb)) {
            Map<String, String> overrideMap = new ConcurrentHashMap<>();
            overrideMap.put("database", "dodex"); // <------- should match test/dev db
            dodexDatabase = DbConfiguration.getDefaultDb(overrideMap, props);
            pool = dodexDatabase.getPool4();
            checkSql = DbCubrid.CHECKUSERSQL;
        }

        // DodexDatabaseSqlite3.setupSql(pool);

        messageUser = dodexDatabase.createMessageUser();
        resultUser = dodexDatabase.createMessageUser();

        messageUser.setName("User1");
        messageUser.setPassword("Password");

        // Make sure test user is removed from database
        SqlConnection conn = pool.rxGetConnection().blockingGet();
        Tuple parameters = Tuple.of(messageUser.getName(), messageUser.getPassword());
        conn.preparedQuery(dodexDatabase.getDeleteUser()).rxExecute(parameters).subscribe(r -> {
            conn.close();
        }, err -> {
            throw new Exception(err);
        });
    }

    @Test
    void databaseSetup() {
        assertNotEquals(dodexDatabase, null, "dodexDatabase should be created");
        pool.rxGetConnection().doOnSuccess(value -> {
            assertNotNull(value, "database pool should exist");
        });
    }

    @Test
    void doesUsersTableExist() throws InterruptedException {
        String table[] = { null };        
        Disposable testDisposable[] = { null };
        
        Single<SqlConnection> con = pool.rxGetConnection();
        testDisposable[0] = con.subscribe(c -> {
            testDisposable[0] = c.query(checkSql).rxExecute().doOnSuccess(rows -> {
                for (Row row : rows) {
                    table[0] = row.getString(0).toLowerCase();
                }
            }).subscribe(r -> {
                c.close();
            });
        });

        assertTrue("Query not yet complete",  !testDisposable[0].isDisposed());
        await(testDisposable[0]);
        assertTrue("Users Table Exists",  "users".equals(table[0]));
    }
    @Disabled("Disabled until it works")
    @Test
    void testDatabaseAndReactiveVertx() throws InterruptedException, SQLException {
        Disposable testDisposable[] = { null };
        boolean emptyTable[] = { false, false, false };

        messageUser.setIp("0");
        messageUser.setId(-1l);
        resultUser.setId(0L);
        resultUser.setName(null);

        Single<SqlConnection> conn = pool.rxGetConnection();

        testDisposable[0] = conn.subscribe(c -> {
            testDisposable[0] = c.preparedQuery(dodexDatabase.getUserById())
                .rxExecute(Tuple.of(messageUser.getName(), messageUser.getPassword()))
                .doOnSuccess(rows -> {
                    if(rows.rowCount() == 0) {
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
                })
                .subscribe(result -> {
                    c.close();
                    emptyTable[2] = true;
                });
            });
        
        assertSame("reactive vertx should be running asynchronously", emptyTable[0], false);
        
        await(testDisposable[0]);
       
        assertSame("user should not be found & added to table", emptyTable[0], true);
        assertTrue("user id should be generated", resultUser.getId() > 0);
        assertEquals(resultUser.getName(), "User1", "user should be retrieved");
        assertTrue("subscribe should finish", emptyTable[2]);
    }
    @Disabled("Disabled until it works")
    @Test
    void deleteUserFromDatabase() {
        Disposable testDisposable[] = { null };
        int deleted[] = { 0 };
        Single<SqlConnection> conn = pool.rxGetConnection();

        testDisposable[0] = conn.subscribe(c -> {
            testDisposable[0] = c.preparedQuery(dodexDatabase.getDeleteUser())
            .rxExecute(Tuple.of(messageUser.getName(), messageUser.getPassword()))
            .doOnSuccess(rows -> {
                deleted[0] = 1;  // can't use rowCount because of execution order
                c.close();
            }).subscribe();
        });
            
        assertSame("user deletion should not start yet", deleted[0] == 0, Boolean.TRUE);

        await(testDisposable[0]);
        
        assertSame("user deleted or not in database", deleted[0] == 1, Boolean.TRUE);
    }

    public void await(Disposable disposable) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        while (!disposable.isDisposed()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}