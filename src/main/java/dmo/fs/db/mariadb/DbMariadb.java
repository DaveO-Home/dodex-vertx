
package dmo.fs.db.mariadb;

import dmo.fs.db.DbDefinitionBase;
import dmo.fs.db.DodexDatabase;
import dmo.fs.dbh.HandicapDatabase;

public abstract class DbMariadb extends DbDefinitionBase implements DodexDatabase, HandicapDatabase {
	protected final static String CHECKLOGINSQL = "select 1 from information_schema.tables where table_name='login' and table_schema = '$db_name';";
	public final static String CHECKUSERSQL = "select 1 from information_schema.tables where table_name='users' and table_schema = '$db_name';";
	protected final static String CHECKMESSAGESSQL = "select 1 from information_schema.tables where table_name='messages' and table_schema = '$db_name';";
	protected final static String CHECKUNDELIVEREDSQL = "select 1 from information_schema.tables where table_name='undelivered' and table_schema = '$db_name';";
	protected final static String CHECKHANDICAPSQL = "SELECT table_name FROM information_schema.tables WHERE table_name in ('golfer', 'course', 'scores', 'ratings', 'groups', 'member') and table_schema = '$db_name'";

	private enum CreateTable {
		CREATEUSERS(
				"CREATE TABLE users (" +
						"id INT NOT NULL AUTO_INCREMENT," +
						"name VARCHAR(255) CHARACTER SET utf8mb4 collate  utf8mb4_bin NOT NULL," +
						"password VARCHAR(255) NOT NULL," +
						"ip VARCHAR(255) NOT NULL," +
						"last_login DATETIME NOT NULL," +
						"PRIMARY KEY (id)," +
						"UNIQUE INDEX U_name_password_UNIQUE (name ASC, password ASC));"),
		CREATEMESSAGES(
				"CREATE TABLE messages (" +
						"id INT NOT NULL AUTO_INCREMENT," +
						"message MEDIUMBLOB NOT NULL," +
						"from_handle VARCHAR(255) CHARACTER SET utf8mb4 collate  utf8mb4_bin NOT NULL," +
						"post_date DATETIME NOT NULL," +
						"PRIMARY KEY (id));"),
		CREATEUNDELIVERED(
				"CREATE TABLE undelivered (" +
						"user_id INT NOT NULL," +
						"message_id INT NOT NULL," +
						"INDEX U_fk_undelivered_users_idx (user_id ASC)," +
						"INDEX U_fk_undelivered_messages_idx (message_id ASC)," +
						"CONSTRAINT U_fk_undelivered_users " +
						"FOREIGN KEY (user_id) " +
						"REFERENCES users (id) " +
						"ON DELETE NO ACTION " +
						"ON UPDATE NO ACTION," +
						"CONSTRAINT U_fk_undelivered_messages " +
						"FOREIGN KEY (message_id) " +
						"REFERENCES messages (id) " +
						"ON DELETE NO ACTION " +
						"ON UPDATE NO ACTION);"),
		CREATEGOLFER(
				"CREATE TABLE IF NOT EXISTS golfer (" +
						"PIN CHARACTER(8) primary key NOT NULL," +
						"FIRST_NAME VARCHAR(32) NOT NULL," +
						"LAST_NAME VARCHAR(32) NOT NULL," +
						"HANDICAP FLOAT(4,1) DEFAULT 0.0," +
						"COUNTRY CHARACTER(2) DEFAULT 'US' NOT NULL," +
						"STATE CHARACTER(2) DEFAULT 'NV' NOT NULL," +
						"OVERLAP_YEARS BOOLEAN DEFAULT true," +
						"PUBLIC BOOLEAN DEFAULT false," +
						"LAST_LOGIN TIMESTAMP NOT NULL)"),
		CREATECOURSE(
				"CREATE TABLE IF NOT EXISTS course (" +
						"COURSE_SEQ INTEGER primary key auto_increment NOT NULL," +
						"COURSE_NAME VARCHAR(128) NOT NULL," +
						"COURSE_COUNTRY VARCHAR(128) NOT NULL," +
						"COURSE_STATE CHARACTER(2) NOT NULL )"),
		CREATERATINGS(
				"CREATE TABLE IF NOT EXISTS ratings (" +
						"COURSE_SEQ INTEGER NOT NULL," +
						"TEE INTEGER NOT NULL," +
						"TEE_COLOR VARCHAR(16)," +
						"TEE_RATING FLOAT(4,1) NOT NULL," +
						"TEE_SLOPE INTEGER NOT NULL," +
						"TEE_PAR INTEGER DEFAULT '72' NOT NULL, PRIMARY KEY (COURSE_SEQ, TEE)," +
						"INDEX idx_rating_course (course_seq ASC)," +
						"CONSTRAINT U_fk_course_ratings " +
						"FOREIGN KEY (COURSE_SEQ) " +
						"REFERENCES course (COURSE_SEQ) " +
						"ON DELETE NO ACTION " +
						"ON UPDATE NO ACTION)"),
		CREATESCORES(
				"CREATE TABLE IF NOT EXISTS scores (" +
						"PIN CHARACTER(8) NOT NULL," +
						"GROSS_SCORE INTEGER NOT NULL," +
						"NET_SCORE FLOAT(4,1)," +
						"ADJUSTED_SCORE INTEGER NOT NULL," +
						"TEE_TIME TEXT NOT NULL," +
						"HANDICAP FLOAT(32)," +
						"COURSE_SEQ INTEGER," +
						"COURSE_TEES INTEGER," +
						"USED CHARACTER(1)," +
						"CONSTRAINT U_fk_course_scores " +
						"FOREIGN KEY (COURSE_SEQ) " +
						"REFERENCES course (COURSE_SEQ) " +
						"ON DELETE NO ACTION " +
						"ON UPDATE NO ACTION," +
						"CONSTRAINT U_fk_golfer_scores " +
						"FOREIGN KEY (PIN) " +
						"REFERENCES golfer (PIN) " +
						"ON DELETE NO ACTION " +
						"ON UPDATE NO ACTION)"),
		CREATEGROUPS(
				"CREATE TABLE IF NOT EXISTS groups (" +
						"ID INTEGER primary key auto_increment NOT NULL," +
						"NAME VARCHAR(24) NOT NULL," +
						"OWNER INTEGER NOT NULL DEFAULT 0," +
						"CREATED DATETIME NOT NULL DEFAULT current_timestamp()," +
						"UPDATED DATETIME DEFAULT NULL ON UPDATE current_timestamp()," +
						"UNIQUE KEY unique_on_name (NAME))"
		),
		CREATEMEMBER(
				"CREATE TABLE IF NOT EXISTS member (" +
						"GROUP_ID INTEGER NOT NULL DEFAULT 0," +
						"USER_ID INTEGER NOT NULL DEFAULT 0," +
						"PRIMARY KEY (GROUP_ID,USER_ID)," +
						"KEY U_fk_MEMBER_USER_idx (USER_ID)," +
						"CONSTRAINT U_fk_MEMBER_GROUP FOREIGN KEY (GROUP_ID) REFERENCES groups (ID) ON DELETE NO ACTION ON UPDATE NO ACTION," +
						"CONSTRAINT U_fk_MEMBER_USER FOREIGN KEY (USER_ID) REFERENCES users (ID) ON DELETE NO ACTION ON UPDATE NO ACTION)"
		);

        final String sql;

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
