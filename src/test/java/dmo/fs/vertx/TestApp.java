
package dmo.fs.vertx;

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
import dmo.fs.db.DodexDatabasePostgres;
import dmo.fs.db.DodexDatabaseSqlite3;
import dmo.fs.db.MessageUser;
import dmo.fs.db.DbPostgres.Users;
import dmo.fs.utils.ConsoleColors;
import dmo.fs.utils.DodexUtil;
import io.reactivex.disposables.Disposable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

class AppTest {
    Logger logger = LoggerFactory.getLogger(AppTest.class.getName());

    @Test
    void getJavaResource() {
        InputStream in = getClass().getResourceAsStream("/application-conf.json");
        assertNotNull(in, "should find resource file.");
    }
}

class UtilTest {
    String messageOnly = "This is a message.";
    String messageWithCommand = "This is a message.;removeuser";
    String messageWithCommandAndData = "This is a message.;removeuser!!{[\"name\",\"user1\"]}";

    @Test
    void getMessageFromRequest() {
        String description = "should return correct message";
        String message = DodexUtil.ClientInfo.getMessage(messageOnly);
        assertEquals(message, "This is a message.", description);
        message = DodexUtil.ClientInfo.getMessage(messageWithCommand);
        assertEquals(message, "This is a message.", description);
        message = DodexUtil.ClientInfo.getMessage(messageWithCommandAndData);
        assertEquals(message, "This is a message.", description);
    }

    @Test
    void getCommandFromRequest() {
        String description = "should return a command";
        String command = DodexUtil.ClientInfo.getCommand(messageOnly);
        assertEquals(command, null, "command should be nul");
        command = DodexUtil.ClientInfo.getCommand(messageWithCommand);
        assertEquals(command, ";removeuser", description);
        command = DodexUtil.ClientInfo.getCommand(messageWithCommandAndData);
        assertEquals(command, ";removeuser", description);
    }

    @Test
    void getDataFromRequest() {
        String data = DodexUtil.ClientInfo.getData(messageOnly);
        assertEquals(data, null, "should be null");
        data = DodexUtil.ClientInfo.getData(messageWithCommand);
        assertEquals(data, null, "should be null");
        data = DodexUtil.ClientInfo.getData(messageWithCommandAndData);
        assertEquals(data, "{[\"name\",\"user1\"]}", "should return data");
    }

    @Test
    void testUsersCommand() {
        String command = DodexUtil.ClientInfo.getCommand(";users!!{[\"name\",\"user1\"]}");
        assertEquals(command, ";users", "should return the users command");
    }
}

@TestInstance(Lifecycle.PER_CLASS)
class DbTest extends DbDefinitionBase {
    Logger logger = LoggerFactory.getLogger(DbTest.class.getName());

    MessageUser messageUser = null;
    MessageUser resultUser = null;
    ConnectionProvider cp = null;
    Database db = null;
    DodexDatabase dodexDatabase = null;

    @BeforeAll
    @Test
    void testDatabaseSetup() throws InterruptedException, IOException {
        String whichDb = "sqlite3"; // postgres || sqlite3
        if (System.getenv("DEFAULT_DB") != null) {
            whichDb = System.getenv("DEFAULT_DB");
        }
        Properties props = new Properties();
        props.setProperty("user", "daveo");
        props.setProperty("password", "albatross");
        props.setProperty("ssl", "false");    

        if (whichDb.equals("sqlite3")) {
            dodexDatabase = new DodexDatabaseSqlite3();
            cp = DbConfiguration.getSqlite3ConnectionProvider();
        } else {
            Map<String,String>overrideMap = new HashMap<>();
            overrideMap.put("dbname", "/daveo"); // this wiil be merged into the default map
            dodexDatabase = new DodexDatabasePostgres(overrideMap, props);
            cp = DbConfiguration.getPostgresConnectionProvider();
        }

        NonBlockingConnectionPool pool = Pools.nonBlocking()
                .maxPoolSize(Runtime.getRuntime().availableProcessors() * 5)
                .connectionProvider(cp).build();

        db = Database.from(pool);

        assertNotEquals(dodexDatabase, null, "dodexDatabase should be created");
        assertNotEquals(db.member(), null, "database should should exist");
        messageUser = dodexDatabase.createMessageUser();
        resultUser = dodexDatabase.createMessageUser();

        messageUser.setName("User1");
        messageUser.setPassword("Password");
    }

    @Test
    void testDatabaseAndJavaRx() throws InterruptedException {
        messageUser.setIp("0.0.0.0");
        boolean emptyTable[] = { false, false, false };
        messageUser.setId(-1l);

        Disposable disposable = db.select(Users.class)
                .parameter(messageUser.getPassword())
                .get()
                .isEmpty()
                .doOnSuccess(empty -> {
                    if (empty) {
                        emptyTable[0] = empty;
                        dodexDatabase.addUser(null, db, messageUser);
                        emptyTable[1] = messageUser.getId() > 0l;
                    } else {
                        emptyTable[1] = empty == false;
                        emptyTable[0] = empty == false;
                    }
                }).doAfterSuccess(record -> {
                    db.select(Users.class)
                        .parameter(messageUser.getPassword())
                        .get()
                        .doOnNext(result -> {
                            resultUser.setId(result.id());
                            resultUser.setName(result.name());
                            resultUser.setPassword(result.password());
                            resultUser.setIp(result.ip());
                            emptyTable[1] = true;
                    }).subscribe(result -> {
                        //
                    }, throwable -> {
                        logger.error("{0}Error finding user {1}{2}",
                                new Object[] { ConsoleColors.RED, messageUser.getName(), ConsoleColors.RESET });
                        throwable.printStackTrace();
                    });
                }).subscribe(result -> {
                    emptyTable[2] = true;
                }, throwable -> {
                    assertEquals(throwable, null, "throwable should should not happen");
                    logger.error("{0}Error adding user {1}{2}",
                            new Object[] { ConsoleColors.RED, messageUser.getName(), ConsoleColors.RESET });
                    throwable.printStackTrace();
                });
        assertEquals(emptyTable[0], false, "javaRx should be running asynchronously");
        assertEquals(emptyTable[1], false, "javaRx should be running asynchronously");
        await(disposable);
        assertEquals(emptyTable[0], true, "user should be not found");
        assertEquals(emptyTable[1], true, "user should be added to table Users");
        assertEquals(resultUser.getId() > 0, true, "user id should be generated");
        assertEquals(resultUser.getName(), "User1", "user should be retrieved");
        assertEquals(emptyTable[2], true, "subscribe should finish");
    }

    @Test
    void doesUsersTableExist() {
        boolean tableExists[] = {false};
        Disposable disposable = db.member().doOnSuccess(c -> {
            tableExists[0] = tableExist(c.value(), "users");
        }).subscribe();
        
        assertEquals(tableExists[0], false, "verify that javaRx is asynchronous");
        await(disposable);
        assertEquals(tableExists[0], true, "table Users should exist");
    }

    @Test
    void deleteUserFromDatabase() {
        messageUser.setId(-1l);
        Disposable disposable = db.update(getDeleteUser())
                .parameter("name", messageUser.getName())
                .parameter("password", messageUser.getPassword())
                .counts()
                .subscribe(result -> {
                    messageUser.setId(Long.parseLong(result.toString()));
				}, throwable -> {
					logger.error("{0}Error deleting user{1} {2}", new Object[] { ConsoleColors.RED, ConsoleColors.RESET, messageUser.getName() });
					throwable.printStackTrace();
                });
        assertEquals(messageUser.getId() ==  -1l, true, "user deletion should not start yet");
        await(disposable);
        assertEquals((messageUser.getId() != -1l), true, "user deleted or not in database");
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
}