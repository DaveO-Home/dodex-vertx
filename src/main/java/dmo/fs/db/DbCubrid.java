
package dmo.fs.db;

public abstract class DbCubrid extends JavaRxTimestampDb {

	private enum CreateTable {

		CREATEUSERS(
			"CREATE TABLE users" +
				"(id INTEGER AUTO_INCREMENT(1, 1) NOT NULL," +
				"[name] CHARACTER VARYING (255) COLLATE utf8_en_cs NOT NULL," +
				"[password] CHARACTER VARYING (255) NOT NULL," +
				"ip CHARACTER VARYING (255) NOT NULL," +
				"last_login TIMESTAMP," +
				"CONSTRAINT pk_users_id PRIMARY KEY(id)) " +
				"COLLATE iso88591_bin " +
				"REUSE_OID;"),
		CREATEMESSAGES(
			"CREATE TABLE messages" + 
				"(id INTEGER AUTO_INCREMENT(1, 1) NOT NULL," +
				"message CLOB," +
				"from_handle CHARACTER VARYING (255)," +
				"post_date TIMESTAMP," +
				"CONSTRAINT pk PRIMARY KEY(id)) " +
				"COLLATE iso88591_bin " +
				"REUSE_OID; "),
		CREATEUNDELIVERED(
			"CREATE TABLE undelivered " +
				"(user_id integer, message_id integer," +
				"FOREIGN KEY(message_id) REFERENCES messages(id) ON DELETE RESTRICT ON UPDATE RESTRICT," +
				"FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT) " +
				"COLLATE iso88591_bin " +
				"REUSE_OID; "
				);

        String sql;

        CreateTable(String sql) {
            this.sql = sql;
        }
    };

	public DbCubrid() {
		super();
	}

	public String getCreateTable(String table) {
		return CreateTable.valueOf("CREATE"+table.toUpperCase()).sql;
	}
}
