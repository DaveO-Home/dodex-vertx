
package dmo.fs.spa.db;

public abstract class DbIbmDB2 extends SqlBuilder implements SpaDatabase {
    protected final static String CHECKLOGINSQL = "select tabname from syscat.tables where tabschema='DB2INST1' and tabname='LOGIN'";
    
    private enum CreateTable {
		CREATELOGIN(
			"CREATE TABLE LOGIN (" +
				"id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY, " +
				"name VARCHAR(255) NOT NULL, " +
				"password VARCHAR(255) NOT NULL, " +
				"last_login TIMESTAMP(12) NOT NULL, " +
				"PRIMARY KEY (id))"),
		CREATELOGININDEX(
			"CREATE UNIQUE INDEX XLOGIN " +
			"ON LOGIN " +
		  "(name ASC, password ASC)");

        String sql;

        CreateTable(String sql) {
            this.sql = sql;
        }
    };

	public DbIbmDB2() {
		super();
	}

	public String getCreateTable(String table) {
		return CreateTable.valueOf("CREATE"+table.toUpperCase()).sql;
	}

	public String getLoginIndex(String table) {
		return CreateTable.valueOf("CREATE"+table.toUpperCase()+"INDEX").sql;
	}
}
