
package dmo.fs.vertx;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.davidmoten.rx.jdbc.ConnectionProvider;
import org.davidmoten.rx.jdbc.Database;
import org.davidmoten.rx.jdbc.pool.NonBlockingConnectionPool;
import org.davidmoten.rx.jdbc.pool.Pools;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import dmo.fs.db.DbConfiguration;
import dmo.fs.db.DbDefinitionBase;
import dmo.fs.db.DodexDatabase;
import dmo.fs.db.DodexDatabaseCubrid;
import dmo.fs.db.DodexDatabaseMariadb;
import dmo.fs.db.DodexDatabasePostgres;
import dmo.fs.db.DodexDatabaseSqlite3;
import dmo.fs.db.JavaRxDateDb.Users;
import dmo.fs.db.MessageUser;
import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import io.reactivex.disposables.Disposable;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

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
    ConnectionProvider cp;
    NonBlockingConnectionPool pool;
    Database db;
    DodexDatabase dodexDatabase;

    @BeforeAll
    void testDatabaseSetup() throws InterruptedException, IOException, SQLException {
        String whichDb = "sqlite3"; // postgres || sqlite3

        if (System.getenv("DEFAULT_DB") != null) {
            whichDb = System.getenv("DEFAULT_DB");
        }
        Properties props = new Properties();
        props.setProperty("user", "user"); // <------- change to use
        props.setProperty("password", "password"); // <------- change to use
        props.setProperty("ssl", "false");

        if ("sqlite3".equals(whichDb)) {
            dodexDatabase = new DodexDatabaseSqlite3();
            cp = DbConfiguration.getSqlite3ConnectionProvider();
        } else if ("postgres".equals(whichDb)) {
            Map<String, String> overrideMap = new ConcurrentHashMap<>();
            overrideMap.put("dbname", "/test"); // <------- should match test/dev db
            try {
                dodexDatabase = new DodexDatabasePostgres(overrideMap, props);
            } catch (Exception e) {
                e.printStackTrace();
            }
            cp = DbConfiguration.getPostgresConnectionProvider();
        } else if ("mariadb".equals(whichDb)) {
            Map<String, String> overrideMap = new ConcurrentHashMap<>();
            overrideMap.put("dbname", "/test"); // <------- should match test/dev db
            try {
                dodexDatabase = new DodexDatabaseMariadb(overrideMap, props);
                cp = DbConfiguration.getMariadbConnectionProvider();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("cubrid".equals(whichDb)) {
            Map<String, String> overrideMap = new ConcurrentHashMap<>();
            overrideMap.put("dbname", "test:public::"); // <------- should match test/dev db
            try {
                dodexDatabase = new DodexDatabaseCubrid(overrideMap, props);
            } catch (Exception e) {
                e.printStackTrace();
            }
            cp = DbConfiguration.getCubridConnectionProvider();
        }

        pool = Pools.nonBlocking().maxPoolSize(Runtime.getRuntime().availableProcessors() * 5)
                .connectionProvider(cp).build();
        
        db = Database.from(pool);
                
        DbDefinitionBase.setupSql(db);

        assertNotEquals(dodexDatabase, null, "dodexDatabase should be created");
        assertNotEquals(db.member(), null, "database should should exist");
        messageUser = dodexDatabase.createMessageUser();
        resultUser = dodexDatabase.createMessageUser();

        messageUser.setName("User1");
        messageUser.setPassword("Password");
    }

    @Test
    void doesUsersTableExist() {
        boolean tableExists[] = { false };

        Disposable disposable = db.member()
            .doOnSuccess(c -> {
                tableExists[0] = tableExist(c.value(), "USERS");
                c.checkin();
            })
            .subscribe();

        assertSame("verify that javaRx is asynchronous", tableExists[0], Boolean.FALSE);
        await(disposable);
        assertSame("table Users should exist", tableExists[0], Boolean.TRUE);
    }

    @Test
    void testDatabaseAndJavaRx() throws InterruptedException {
        messageUser.setIp("0");
        boolean emptyTable[] = { false, false, false };
        messageUser.setId(-1l);

        Promise<MessageUser> promise = Promise.promise();

        Disposable disposable = db.select(Users.class)
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
                if (empty) {
                    emptyTable[0] = empty;
                    
                    Future<MessageUser> future2 = dodexDatabase.addUser(null, db, messageUser);
                    
                    future2.onComplete(handler -> {
                        emptyTable[1] = messageUser.getId() > 0l;
                        MessageUser result = future2.result();
                        resultUser.setId(result.getId());
                        resultUser.setName(result.getName());
                        resultUser.setPassword(result.getPassword());
                        resultUser.setIp(result.getIp());
                        resultUser.setLastLogin(result.getLastLogin());
                        promise.complete(resultUser);
                    });
                } else {
                    emptyTable[1] = empty == false;
                    emptyTable[0] = empty == false;
                }
            })
            .subscribe(result -> {
                emptyTable[2] = true;
            }, throwable -> {
                logger.error(String.join(ColorUtilConstants.RED, "Error adding user: ", messageUser.getName(),
                        " : ", throwable.getMessage(), ColorUtilConstants.RESET));
                throwable.printStackTrace();
            });
        
        assertSame("javaRx should be running asynchronously", emptyTable[0], false);
        assertSame("javaRx should be running asynchronously", emptyTable[1], false);
        promise.future().onSuccess(result -> {
            assertSame("user should be not found", emptyTable[0], true);
            assertSame("user should be added to table Users", emptyTable[0], true);
            assertTrue("user id should be generated", result.getId() > 0);
            assertEquals(result.getName(), "User1", "user should be retrieved");
            assertTrue("subscribe should finish", emptyTable[2]);
        });
        // Making sure we don't execute the next test before finishing this test
        await(disposable);
    }

    @Test
    void deleteUserFromDatabase() {
        messageUser.setId(-1l);
        Promise<Integer> promise = Promise.promise();

        db.update(dodexDatabase.getDeleteUser())
            .parameter("NAME", messageUser.getName())
            .parameter("PASSWORD", messageUser.getPassword())
            .counts()
            .subscribe(result -> {
                messageUser.setId(Long.parseLong(result.toString()));
                promise.complete(result);
            }, throwable -> {
                logger.error(String.join(ColorUtilConstants.RED, "Error deleting user: ",
                        messageUser.getName(), " : ", throwable.getMessage(), ColorUtilConstants.RESET));
                throwable.printStackTrace();
            });
            
        assertSame("user deletion should not start yet", messageUser.getId() == -1l, Boolean.TRUE);
        promise.future().onSuccess(result -> {
            assertSame("user deleted or not in database", messageUser.getId() == 1, Boolean.TRUE);
            assertSame("user deleted or not in database", result == 1, Boolean.TRUE);
        });
    }

    public void await(Disposable disposable) {
        while (!disposable.isDisposed()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean tableExist(Connection conn, String tableName) throws SQLException {
        boolean exists = false;
        try (ResultSet rs = conn.getMetaData().getTables(null, null, tableName.toLowerCase(), null)) {
            while (rs.next()) {
                String name = rs.getString("TABLE_NAME");
                if (name != null && name.equalsIgnoreCase(tableName)) {
                    exists = true;
                    break;
                }
            }
        }
        return exists;
    }
}