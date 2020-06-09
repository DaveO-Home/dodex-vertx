
package dmo.fs.spa.db;

public abstract class DbCubrid extends RxJavaTimestampDb {

	private enum CreateTable {

		CREATELOGIN(
			"CREATE TABLE login" +
				"(id INTEGER AUTO_INCREMENT(1, 1) NOT NULL," +
				"[name] CHARACTER VARYING (255) COLLATE utf8_en_cs NOT NULL," +
				"[password] CHARACTER VARYING (255) NOT NULL," +
				"last_login TIMESTAMP," +
				"CONSTRAINT pk_login_id PRIMARY KEY(id)) " +
				"COLLATE iso88591_bin " +
				"REUSE_OID;");

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
