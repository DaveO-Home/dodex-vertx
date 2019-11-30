package dmo.fs.db;

import dmo.fs.db.DbSqlite3.Messages;
import dmo.fs.db.DbSqlite3.Undelivered;
import dmo.fs.db.DbSqlite3.Users;

public interface Sqlite3Constants {
    final static String QUERYUSERS = "select * from users where password=?";
	final static String QUERYMESSAGES = "select * from messages where id=?";
	final static String QUERYUNDELIVERED = "Select message_id, name, message, from_handle, post_date from users, undelivered, messages where users.id = user_id and messages.id = message_id and users.id = :id";

    enum SelectTable {};
    enum UpdateTable {};
    enum CreateTable {};

    public String getCreateTable(String table);
    public String getAllUsers();
	public String getUserByName();
    public String getInsertUser();
    public String getDeleteUser();
	public String getInsertMessage();
    public String getInsertUndelivered();
	public String getRemoveUndelivered();
	public String getRemoveMessage();
    public String getUndeliveredMessage();

    public <T> Class<Users> getUsersClass();
    public <T> Class<Undelivered> getUndeliveredClass();
    public <T> Class<Messages> getMessagesClass();
}