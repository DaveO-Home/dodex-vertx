
package dmo.fs.db.db2;

import dmo.fs.db.DbDefinitionBase;
import dmo.fs.db.DodexDatabase;

public abstract class DbIbmDB2 extends DbDefinitionBase implements DodexDatabase {
    /*
        Modify "tabschema=" appropriately
     */
    protected final static String CHECKUSERSQL = "select tabname from syscat.tables where tabschema='DB2INST1' and tabname='USERS'";
    protected final static String CHECKMESSAGESQL = "select tabname from syscat.tables where tabschema='DB2INST1' and tabname='MESSAGES'";
    protected final static String CHECKUNDELIVEREDSQL = "select tabname from syscat.tables where tabschema='DB2INST1' and tabname='UNDELIVERED'";
    
    private enum CreateTable {
		CREATEUSERS(
			"CREATE TABLE USERS (" +
				"id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY, " +
				"name VARCHAR(255) NOT NULL, " +
				"password VARCHAR(255) NOT NULL, " +
				"ip VARCHAR(255) NOT NULL, " +
				"last_login TIMESTAMP(12) NOT NULL, " +
				"PRIMARY KEY (id))"),
		CREATEUSERSINDEX(
			"CREATE UNIQUE INDEX XUSERS " +
			"ON USERS " +
		  "(name ASC, password ASC)"),
		CREATEMESSAGES(
			"CREATE TABLE MESSAGES (" +
				"id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY," +
				"message VARCHAR(10240) NOT NULL," +
				"from_handle VARCHAR(255) NOT NULL," +
				"post_date TIMESTAMP(12) NOT NULL, " +
				"PRIMARY KEY (id))"),
		CREATEUNDELIVERED(
			"CREATE TABLE UNDELIVERED (" +
				"user_id INTEGER NOT NULL," +
				"message_id INTEGER NOT NULL, " +
				"FOREIGN KEY (USER_ID) " +
				"REFERENCES USERS (ID) ON DELETE RESTRICT, " +
				"FOREIGN KEY (MESSAGE_ID) " +
				"REFERENCES MESSAGES (ID) ON DELETE RESTRICT)"
				);

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

	public String getUsersIndex(String table) {
		return CreateTable.valueOf("CREATE"+table.toUpperCase()+"INDEX").sql;
	}
}
