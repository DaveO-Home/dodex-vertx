
package dmo.fs.spa.db.cubrid;

import dmo.fs.spa.db.SpaDatabase;
import dmo.fs.spa.db.SqlBuilder;

public abstract class DbCubrid extends SqlBuilder implements SpaDatabase {
    protected final static String CHECKLOGINSQL = "SELECT class_name FROM _db_class WHERE class_name = 'login'";

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
