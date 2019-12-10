
package dmo.fs.db;

public abstract class DbSqlite3 extends JavaRxDateDb {
	
	private enum CreateTable {
		CREATEUSERS("create table users (id integer primary key, name text not null unique, password text not null unique, ip text not null, last_login DATETIME not null)"),
		CREATEMESSAGES("create table messages (id integer primary key, message text not null, from_handle text not null, post_date DATETIME not null)"),
		CREATEUNDELIVERED("create table undelivered (user_id integer, message_id integer, CONSTRAINT undelivered_user_id_foreign FOREIGN KEY (user_id) REFERENCES users (id), CONSTRAINT undelivered_message_id_foreign FOREIGN KEY (message_id) REFERENCES messages (id))");
        private String sql;

        private CreateTable(String sql) {
            this.sql = sql;
        }
    };

	public DbSqlite3() {
		super();
	}

	public String getCreateTable(String table) {
		return CreateTable.valueOf("CREATE"+table.toUpperCase()).sql;
	}
}
