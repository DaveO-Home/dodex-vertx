
package dmo.fs.spa.db;

public abstract class DbMariadb extends SqlBuilder implements SpaDatabase {
    protected final static String CHECKLOGINSQL = "select 1 from information_schema.tables where table_name='login';";
    
	private enum CreateTable {
		CREATELOGIN(
			"CREATE TABLE login (" +
				"id INT NOT NULL AUTO_INCREMENT," +
				"name VARCHAR(255) CHARACTER SET  utf8mb4 collate  utf8mb4_bin NOT NULL COMMENT 'Dodex Users'," +
				"password VARCHAR(255) NOT NULL," +
				"last_login DATETIME NOT NULL," +
				"PRIMARY KEY (id)," +
				"UNIQUE INDEX U_name_password_UNIQUE (name ASC, password ASC));");

        String sql;

        CreateTable(String sql) {
            this.sql = sql;
        }
    };

	public DbMariadb() {
		super();
	}

	public String getCreateTable(String table) {
		return CreateTable.valueOf("CREATE"+table.toUpperCase()).sql;
	}
}
