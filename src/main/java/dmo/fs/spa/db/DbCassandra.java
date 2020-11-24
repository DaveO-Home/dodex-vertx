
package dmo.fs.spa.db;

public abstract class DbCassandra extends RxJavaTimestampDb {
	
	private enum CreateTable {
		CREATELOGIN(
			""
		);

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
