
package dmo.fs.db.h2;

import dmo.fs.db.DbDefinitionBase;
import dmo.fs.db.DodexDatabase;
import dmo.fs.dbh.HandicapDatabase;

public abstract class DbH2 extends DbDefinitionBase implements DodexDatabase, HandicapDatabase {
	public static final String CHECKUSERSQL = "SELECT table_name FROM  INFORMATION_SCHEMA.TABLES where table_name = 'users' and table_type='BASE TABLE' and table_schema = 'PUBLIC'";
  protected static final String CHECKMESSAGESSQL = "SELECT table_name FROM  INFORMATION_SCHEMA.TABLES where table_name = 'messages'";
  protected static final String CHECKUNDELIVEREDSQL = "SELECT table_name FROM  INFORMATION_SCHEMA.TABLES where table_name = 'undelivered'";
	protected static final String CHECKGROUPSSQL = "SELECT table_name FROM INFORMATION_SCHEMA.TABLES WHERE table_type='BASE TABLE' AND table_name = 'groups'";
	protected static final String CHECKMEMBERSQL = "SELECT table_name FROM INFORMATION_SCHEMA.TABLES WHERE table_type='BASE TABLE' AND table_name = 'member'";
	protected final static String CHECKHANDICAPSQL = "SELECT table_name FROM INFORMATION_SCHEMA.TABLES WHERE table_type='BASE TABLE' AND table_name in ('golfer', 'course', 'scores', 'ratings', 'groups', 'member')";
	protected enum CreateTable {
		CREATEUSERS("create table public.users (id int auto_increment NOT NULL PRIMARY KEY, name varchar(255) not null unique, password varchar(255) not null unique, ip varchar(255) not null, last_login TIMESTAMP not null)"),
		CREATEMESSAGES("create table messages (id int auto_increment NOT NULL PRIMARY KEY, message clob not null, from_handle varchar(255) not null, post_date TIMESTAMP not null)"),
		CREATEUNDELIVERED("create table undelivered (user_id int, message_id int, CONSTRAINT undelivered_user_id_foreign FOREIGN KEY (user_id) REFERENCES users (id), CONSTRAINT undelivered_message_id_foreign FOREIGN KEY (message_id) REFERENCES messages (id))"),
		CREATEGOLFER("CREATE TABLE IF NOT EXISTS golfer (" +
				"PIN CHARACTER(8) primary key NOT NULL," +
				"FIRST_NAME VARCHAR(32) NOT NULL," +
				"LAST_NAME VARCHAR(32) NOT NULL," +
				"HANDICAP REAL DEFAULT 0.0," +
				"COUNTRY CHARACTER(2) DEFAULT 'US' NOT NULL," +
				"STATE CHARACTER(2) DEFAULT 'NV' NOT NULL," +
				"OVERLAP_YEARS BOOLEAN," +
				"PUBLIC BOOLEAN," +
				"LAST_LOGIN NUMERIC)"),
		CREATECOURSE("CREATE TABLE IF NOT EXISTS course (" +
				"COURSE_SEQ INTEGER primary key auto_increment NOT NULL," +
				"COURSE_NAME VARCHAR(128) NOT NULL," +
				"COURSE_COUNTRY VARCHAR(128) NOT NULL," +
				"COURSE_STATE CHARACTER(2) NOT NULL )"),
		CREATERATINGS("CREATE TABLE IF NOT EXISTS ratings (" +
				"COURSE_SEQ INTEGER NOT NULL," +
				"TEE INTEGER NOT NULL," +
				"TEE_COLOR VARCHAR(16)," +
				"TEE_RATING REAL NOT NULL," +
				"TEE_SLOPE INTEGER NOT NULL," +
				"TEE_PAR INTEGER DEFAULT '72' NOT NULL, PRIMARY KEY (COURSE_SEQ, TEE)," +
				"CONSTRAINT FK_COURSE_SEQ_RATING FOREIGN KEY ( COURSE_SEQ ) " +
				"REFERENCES COURSE ( COURSE_SEQ ))"),
		CREATESCORES("CREATE TABLE IF NOT EXISTS scores (" +
				"PIN CHARACTER(8) NOT NULL," +
				"GROSS_SCORE INTEGER NOT NULL," +
				"NET_SCORE REAL," +
				"ADJUSTED_SCORE INTEGER NOT NULL," +
				"TEE_TIME TEXT NOT NULL," +
				"HANDICAP REAL," +
				"COURSE_SEQ INTEGER," +
				"COURSE_TEES INTEGER," +
				"USED CHARACTER(1), CONSTRAINT FK_SCORES_SEQ_COURSE FOREIGN KEY ( COURSE_SEQ )" +
				"REFERENCES COURSE ( COURSE_SEQ ), CONSTRAINT FK_GOLFER_PIN_SCORES FOREIGN KEY ( PIN ) " +
				"REFERENCES GOLFER ( PIN ))"),
		CREATEGROUPS("CREATE TABLE IF NOT EXISTS groups (" +
				"ID INTEGER primary key auto_increment NOT NULL," +
				"NAME CHARACTER(24) UNIQUE NOT NULL," +
				"OWNER INTEGER NOT NULL," +
				"CREATED TIMESTAMP NOT NULL," +
				"UPDATED TIMESTAMP)"),
		CREATEMEMBER("CREATE TABLE IF NOT EXISTS member (" +
				"GROUP_ID INTEGER NOT NULL DEFAULT 0," +
				"USER_ID INTEGER NOT NULL DEFAULT 0," +
				"PRIMARY KEY (GROUP_ID,USER_ID)," +
				"CONSTRAINT U_fk_MEMBER_GROUP FOREIGN KEY (GROUP_ID) REFERENCES groups (ID)," +
				"CONSTRAINT U_fk_MEMBER_USER FOREIGN KEY (USER_ID) REFERENCES users (ID))");

		final String sql;

        CreateTable(String sql) {
            this.sql = sql;
        }
    }

	protected DbH2() {
		super();
	}

	public String getCreateTable(String table) {
		return CreateTable.valueOf("CREATE"+table.toUpperCase()).sql;
	}
}
