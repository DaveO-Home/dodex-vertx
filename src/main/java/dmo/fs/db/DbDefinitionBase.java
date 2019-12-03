
package dmo.fs.db;

import java.util.ArrayList;
import java.util.List;

import org.davidmoten.rx.jdbc.Database;

import dmo.fs.utils.ConsoleColors;
import io.reactivex.disposables.Disposable;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class DbDefinitionBase {
	private final static Logger logger = LoggerFactory.getLogger(DbDefinitionBase.class.getName());
	
	private enum SelectTable {
		GETALLUSERS("select id, name, password, ip, last_login from users where name <> :name"),
		GETUSER("select  id, name, password, ip, last_login from users where name=:name"),
		GETUSERMESSAGES("Select name, message, from_handle, post_date from users, undelivered, messages where users.id = user_id and messages.id = message_id and user.id = :id"),
		GETUNDELIVEREDMESSAGE("Select user_id, message_id from messages, undelivered where user_id = :userid and message_id = id and id = :messageid");
		private String sql;

		private SelectTable(String sql) {
			this.sql = sql;
		}
	}
	
	private enum UpdateTable {
		INSERTUSER("insert into users values (null, :name, :password, :ip, " + new java.util.Date().getTime() + ")"),
		DELETEUSER("delete from users where name = :name and password = :password"),
		INSERTMESSAGE("insert into messages values (null, :message, :fromHandle, " + new java.util.Date().getTime() + ")"),
		INSERTUNDELIVERED("insert into undelivered values (:userid, :messageid)"),
		DELETEUNDELIVERED("delete from undelivered where user_id = :userid and message_id = :messageid"),
		DELETEMESSAGE("delete from messages where id = :messageid and (select count(id) from messages, undelivered where id = message_id and id = :messageid) = 0");
		private String sql;

		private UpdateTable(String sql) {
			this.sql = sql;
		}
	}

	public String getAllUsers() {
		return SelectTable.valueOf("GETALLUSERS").sql;
	}

	public String getUserByName() {
		return SelectTable.valueOf("GETUSER").sql;
	}

	public String getInsertUser() {
		return UpdateTable.valueOf("INSERTUSER").sql;
	}

	public String getDeleteUser() {
		return UpdateTable.valueOf("DELETEUSER").sql;
	}

	public String getInsertMessage() {
		return UpdateTable.valueOf("INSERTMESSAGE").sql;
	}

	public String getInsertUndelivered() {
		return UpdateTable.valueOf("INSERTUNDELIVERED").sql;
	}

	public String getRemoveUndelivered() {
		return UpdateTable.valueOf("DELETEUNDELIVERED").sql;
	}

	public String getRemoveMessage() {
		return UpdateTable.valueOf("DELETEMESSAGE").sql;
	}

	public String getUndeliveredMessage() {
		return SelectTable.valueOf("GETUNDELIVEREDMESSAGE").sql;
    }

	public long deleteUser(ServerWebSocket ws, Database db, MessageUser messageUser) throws InterruptedException {
		Disposable disposable = db.update(getDeleteUser())
				.parameter("name", messageUser.getName())
				.parameter("password", messageUser.getPassword())
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

	public long addMessage(ServerWebSocket ws, MessageUser messageUser, String message, Database db) throws InterruptedException {
		List<Long> messageId = new ArrayList<>();

		Disposable disposable = db.update(getInsertMessage()).parameter("message", message)
				.parameter("fromHandle", messageUser.getName()).returnGeneratedKeys().getAs(Long.class)
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

	public void addUndelivered(Long userId, Long messageId, Database db) throws InterruptedException {
		// Database db = getDatabase();
		Disposable disposable = db.update(getInsertUndelivered())
				.parameter("userid", userId)
				.parameter("messageid", messageId)
				.complete()
				.doOnError(error -> logger.error("{0}Remove Undelivered Error: {1}{2}", new Object[] { ConsoleColors.RED, error.getMessage(), ConsoleColors.RESET }))
				.subscribe();
		await(disposable);
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
