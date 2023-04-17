
package dmo.fs.db;

public abstract class DbMariadb extends DbDefinitionBase implements DodexDatabase, HandicapDatabase {
    protected final static String CHECKLOGINSQL = "select 1 from information_schema.tables where table_name='LOGIN';";
    public final static String CHECKUSERSQL = "select 1 from information_schema.tables where table_name='USERS';";
    protected final static String CHECKMESSAGESSQL = "select 1 from information_schema.tables where table_name='MESSAGES';";
    protected final static String CHECKUNDELIVEREDSQL = "select 1 from information_schema.tables where table_name='UNDELIVERED';";
	protected final static String CHECKHANDICAPSQL = "SELECT table_name FROM information_schema.tables WHERE table_name in ('GOLFER', 'COURSE', 'SCORES', 'RATINGS')";

    private enum CreateTable {
		CREATEUSERS(
			"CREATE TABLE USERS (" +
				"id INT NOT NULL AUTO_INCREMENT," +
				"name VARCHAR(255) CHARACTER SET utf8mb4 collate  utf8mb4_bin NOT NULL," +
				"password VARCHAR(255) NOT NULL," +
				"ip VARCHAR(255) NOT NULL," +
				"last_login DATETIME NOT NULL," +
				"PRIMARY KEY (id)," +
				"UNIQUE INDEX name_password_UNIQUE (name ASC, password ASC));"),
		CREATEMESSAGES(
			"CREATE TABLE MESSAGES (" +
				"id INT NOT NULL AUTO_INCREMENT," +
				"message MEDIUMTEXT NOT NULL," +
				"from_handle VARCHAR(255) CHARACTER SET utf8mb4 collate  utf8mb4_bin NOT NULL," +
				"post_date DATETIME NOT NULL," +
				"PRIMARY KEY (id));"),
		CREATEUNDELIVERED(
			"CREATE TABLE UNDELIVERED (" +
				"user_id INT NOT NULL," +
				"message_id INT NOT NULL," +
				"INDEX fk_undelivered_users_idx (user_id ASC)," +
				"INDEX fk_undelivered_messages_idx (message_id ASC)," +
				"CONSTRAINT fk_undelivered_users " +
					"FOREIGN KEY (user_id) " +
					"REFERENCES USERS (id) " +
					"ON DELETE NO ACTION " +
					"ON UPDATE NO ACTION," +
				"CONSTRAINT fk_undelivered_messages " +
					"FOREIGN KEY (message_id) " +
					"REFERENCES MESSAGES (id) " +
					"ON DELETE NO ACTION " +
					"ON UPDATE NO ACTION);"),
		CREATEGOLFER(
			"CREATE TABLE IF NOT EXISTS GOLFER (" +
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
			"CREATE TABLE IF NOT EXISTS COURSE (" +
				"COURSE_SEQ INTEGER primary key auto_increment NOT NULL," +
				"COURSE_NAME VARCHAR(128) NOT NULL," +
				"COURSE_COUNTRY VARCHAR(128) NOT NULL," +
				"COURSE_STATE CHARACTER(2) NOT NULL )"),
		CREATERATINGS(
			"CREATE TABLE IF NOT EXISTS RATINGS (" +
				"COURSE_SEQ INTEGER NOT NULL," +
				"TEE INTEGER NOT NULL," +
				"TEE_COLOR VARCHAR(16)," +
				"TEE_RATING FLOAT(4,1) NOT NULL," +
				"TEE_SLOPE INTEGER NOT NULL," +
				"TEE_PAR INTEGER DEFAULT '72' NOT NULL, PRIMARY KEY (COURSE_SEQ, TEE)," +
				"INDEX idx_rating_course (course_seq ASC)," +
				"CONSTRAINT fk_course_ratings " +
				"FOREIGN KEY (COURSE_SEQ) " +
				"REFERENCES COURSE (COURSE_SEQ) " +
				"ON DELETE NO ACTION " +
				"ON UPDATE NO ACTION)"),
		CREATESCORES(
			"CREATE TABLE IF NOT EXISTS SCORES (" +
				"PIN CHARACTER(8) NOT NULL," +
				"GROSS_SCORE INTEGER NOT NULL," +
				"NET_SCORE FLOAT(4,1)," +
				"ADJUSTED_SCORE INTEGER NOT NULL," +
				"TEE_TIME TEXT NOT NULL," +
				"HANDICAP FLOAT(32)," +
				"COURSE_SEQ INTEGER," +
				"COURSE_TEES INTEGER," +
				"USED CHARACTER(1)," +
				"CONSTRAINT fk_course_scores " +
				"FOREIGN KEY (COURSE_SEQ) " +
				"REFERENCES COURSE (COURSE_SEQ) " +
				"ON DELETE NO ACTION " +
				"ON UPDATE NO ACTION," +
				"CONSTRAINT fk_golfer_scores " +
				"FOREIGN KEY (PIN) " +
				"REFERENCES GOLFER (PIN) " +
				"ON DELETE NO ACTION " +
				"ON UPDATE NO ACTION)");

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
