
package dmo.fs.spa.db;

public abstract class DbH2 extends SqlBuilder implements SpaDatabase {
	public static final String CHECKLOGINSQL = "SELECT table_name FROM  INFORMATION_SCHEMA.TABLES where table_name = 'LOGIN' and table_type = 'BASE TABLE'";

	protected enum CreateTable {
		CREATELOGIN("create table login (id int auto_increment NOT NULL PRIMARY KEY, name varchar(255) not null unique, password varchar(255) not null, last_login TIMESTAMP not null)");

		final String sql;

        CreateTable(String sql) {
            this.sql = sql;
        }
    };

	protected DbH2() {
		super();
	}

	public String getCreateTable(String table) {
		return CreateTable.valueOf("CREATE"+table.toUpperCase()).sql;
	}
}
