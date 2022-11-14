
package dmo.fs.db;

public abstract class DbPostgres extends DbDefinitionBase implements DodexDatabase, HandicapDatabase {
	public final static String CHECKUSERSQL = "SELECT to_regclass('public.users')";
    protected final static String CHECKMESSAGESSQL = "SELECT to_regclass('public.messages')";
    protected final static String CHECKUNDELIVEREDSQL = "SELECT to_regclass('public.undelivered')";
	protected final static String CHECKHANDICAPSQL = "SELECT table_name FROM information_schema.tables WHERE table_name in ('golfer', 'course', 'scores', 'ratings');";
	protected final static String SELECTONE = "SELECT 1;";

	private enum CreateTable {
		CREATEUSERS(
			"CREATE SEQUENCE public.users_id_seq INCREMENT 1 START 19 MINVALUE 1 MAXVALUE 2147483647 CACHE 1; " +	
			"ALTER SEQUENCE public.users_id_seq OWNER TO dummy;" +
			"CREATE TABLE public.users" +
				"(id integer NOT NULL DEFAULT nextval('users_id_seq'::regclass)," +
				"name character varying(255) COLLATE pg_catalog.\"default\"," +
				"password character varying(255) COLLATE pg_catalog.\"default\"," +
				"ip character varying(255) COLLATE pg_catalog.\"default\"," +
				"last_login timestamp with time zone," +
				"CONSTRAINT users_pkey PRIMARY KEY (id)," +
				"CONSTRAINT users_name_unique UNIQUE (name)," +
				"CONSTRAINT users_password_unique UNIQUE (password))" +
				"WITH (OIDS = FALSE) TABLESPACE pg_default;" +		
			"ALTER TABLE public.users OWNER to dummy;"),
		CREATEMESSAGES(
			"CREATE SEQUENCE public.messages_id_seq INCREMENT 1 START 4 MINVALUE 1 MAXVALUE 2147483647 CACHE 1;" +
			"ALTER SEQUENCE public.messages_id_seq OWNER TO dummy;" +
			"CREATE TABLE public.messages" + 
				"(id integer NOT NULL DEFAULT nextval('messages_id_seq'::regclass)," +
				"message text COLLATE pg_catalog.\"default\"," +
				"from_handle character varying(255) COLLATE pg_catalog.\"default\"," +
				"post_date timestamp with time zone," +
				"CONSTRAINT messages_pkey PRIMARY KEY (id))" +
				"WITH (OIDS = FALSE) TABLESPACE pg_default;" +
			"ALTER TABLE public.messages OWNER to dummy;"),
		CREATEUNDELIVERED(
			"CREATE TABLE public.undelivered" +
				"(user_id integer, message_id integer," +
				"CONSTRAINT undelivered_message_id_foreign FOREIGN KEY (message_id)" +
				"REFERENCES public.messages (id) MATCH SIMPLE " +
				"ON UPDATE NO ACTION ON DELETE NO ACTION NOT VALID," +
				"CONSTRAINT undelivered_user_id_foreign FOREIGN KEY (user_id)" +
				"REFERENCES public.users (id) MATCH SIMPLE " +
				"ON UPDATE NO ACTION ON DELETE NO ACTION NOT VALID)" +
				"WITH (OIDS = FALSE) TABLESPACE pg_default;" +
			"ALTER TABLE public.undelivered OWNER to dummy;"),
		CREATEGOLFER(
			"CREATE TABLE IF NOT EXISTS public.GOLFER (" +
					"PIN VARCHAR(8) primary key NOT NULL," +
					"FIRST_NAME VARCHAR(32) NOT NULL," +
					"LAST_NAME VARCHAR(32) NOT NULL," +
					"HANDICAP NUMERIC(3,1) DEFAULT 0.0," +
					"COUNTRY CHARACTER(2) DEFAULT 'US' NOT NULL," +
					"STATE CHARACTER(2) DEFAULT 'NV' NOT NULL," +
					"OVERLAP_YEARS INTEGER DEFAULT 1," +
					"PUBLIC INTEGER DEFAULT 0," +
					"LAST_LOGIN BIGINT," +
					"CONSTRAINT golfer_pkey UNIQUE (PIN)," +
					"CONSTRAINT golfer_names_unique UNIQUE (LAST_NAME, FIRST_NAME))" +
					"WITH (OIDS = FALSE) TABLESPACE pg_default;" +
			"ALTER TABLE public.golfer OWNER to dummy;"),
		CREATECOURSE(
			"CREATE SEQUENCE IF NOT EXISTS public.course_id_seq INCREMENT 1 START 1 MINVALUE 1 MAXVALUE 2147483647 CACHE 1;" +
					"ALTER SEQUENCE public.course_id_seq OWNER TO dummy;" +
			"CREATE TABLE IF NOT EXISTS public.COURSE (" +
					"COURSE_SEQ INTEGER primary key DEFAULT nextval('course_id_seq'::regclass) NOT NULL," +
					"COURSE_NAME VARCHAR(128) NOT NULL," +
					"COURSE_COUNTRY VARCHAR(128) NOT NULL," +
					"COURSE_STATE CHARACTER(2) NOT NULL," +
					"CONSTRAINT course_pkey UNIQUE (COURSE_SEQ))" +
					"WITH (OIDS = FALSE) TABLESPACE pg_default;" +
			"ALTER TABLE public.COURSE OWNER to dummy;"),
		CREATERATINGS(
			"CREATE TABLE IF NOT EXISTS public.RATINGS (" +
					"COURSE_SEQ INTEGER NOT NULL," +
					"TEE INTEGER NOT NULL," +
					"TEE_COLOR VARCHAR(16)," +
					"TEE_RATING NUMERIC(3,1) NOT NULL," +
					"TEE_SLOPE INTEGER NOT NULL," +
					"TEE_PAR INTEGER DEFAULT '72' NOT NULL, PRIMARY KEY (COURSE_SEQ, TEE)," +
					"CONSTRAINT fk_course_ratings " +
					"FOREIGN KEY (COURSE_SEQ) " +
					"REFERENCES public.COURSE (COURSE_SEQ) " +
					"ON DELETE NO ACTION " +
					"ON UPDATE NO ACTION NOT VALID)" +
					"WITH (OIDS = FALSE) TABLESPACE pg_default;" +
			"ALTER TABLE public.RATINGS OWNER to dummy;"),
		CREATESCORES(
			"CREATE TABLE IF NOT EXISTS public.SCORES (" +
					"PIN VARCHAR(8) NOT NULL," +
					"GROSS_SCORE INTEGER NOT NULL," +
					"NET_SCORE NUMERIC(3,1)," +
					"ADJUSTED_SCORE INTEGER NOT NULL," +
					"TEE_TIME TEXT NOT NULL," +
					"HANDICAP NUMERIC(3,1)," +
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
					"ON UPDATE NO ACTION NOT VALID)" +
					"WITH (OIDS = FALSE) TABLESPACE pg_default;" +
			"ALTER TABLE public.SCORES OWNER to dummy;");

        String sql;

        CreateTable(String sql) {
            this.sql = sql;
        }
    };

	public DbPostgres() {
		super();
	}

	public String getCreateTable(String table) {
		return CreateTable.valueOf("CREATE"+table.toUpperCase()).sql;
	}
}
