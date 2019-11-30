
package dmo.fs.db;

public abstract class DbDefinitionBase {
    
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

}
