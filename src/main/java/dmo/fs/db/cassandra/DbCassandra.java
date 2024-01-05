
package dmo.fs.db;

public abstract class DbCassandra extends DbCassandraBase implements DodexCassandra {
	
	private enum CreateTable {
		CreateDummy("");

        String sql;

        CreateTable(String sql) {
            this.sql = sql;
        }
    };

	public DbCassandra() {
		super();
	}

	public String getCreateTable(String table) {
		return CreateTable.valueOf("CREATE"+table.toUpperCase()).sql;
	}
}
