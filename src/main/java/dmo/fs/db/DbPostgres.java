
package dmo.fs.db;

import java.sql.Clob;
import java.sql.Timestamp;

import org.davidmoten.rx.jdbc.annotations.Column;
import org.davidmoten.rx.jdbc.annotations.Query;

public abstract class DbPostgres extends DbDefinitionBase implements PostgresConstants {
	private final static String QUERYUSERS = "select * from users where password=?";
	private final static String QUERYMESSAGES = "select * from messages where id=?";
	private final static String QUERYUNDELIVERED = "Select message_id, name, message, from_handle, post_date from users, undelivered, messages where users.id = user_id and messages.id = message_id and users.id = :id";

	private enum CreateTable {
		CREATEUSERS(
			"CREATE SEQUENCE public.users_id_seq INCREMENT 1 START 19 MINVALUE 1 MAXVALUE 2147483647 CACHE 1; " +	
			"ALTER SEQUENCE public.users_id_seq OWNER TO daveo;" +
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
			"ALTER TABLE public.users OWNER to daveo;"),
		CREATEMESSAGES(
			"CREATE SEQUENCE public.messages_id_seq INCREMENT 1 START 4 MINVALUE 1 MAXVALUE 2147483647 CACHE 1;" +
			"ALTER SEQUENCE public.messages_id_seq OWNER TO daveo;" +
			"CREATE TABLE public.messages" + 
				"(id integer NOT NULL DEFAULT nextval('messages_id_seq'::regclass)," +
				"message text COLLATE pg_catalog.\"default\"," +
				"from_handle character varying(255) COLLATE pg_catalog.\"default\"," +
				"post_date timestamp with time zone," +
				"CONSTRAINT messages_pkey PRIMARY KEY (id))" +
				"WITH (OIDS = FALSE) TABLESPACE pg_default;" +
			"ALTER TABLE public.messages OWNER to daveo;"),
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
			"ALTER TABLE public.undelivered OWNER to daveo;");

        private String sql;

        private CreateTable(String sql) {
            this.sql = sql;
        }
    };

	private enum UpdateTable {
		INSERTUSER("insert into users(name, password, ip, last_login) values (:name, :password, :ip, :lastlogin )"),
		INSERTMESSAGE("insert into messages(message, from_handle, post_date) values ( :message, :fromHandle, :postdate)");
		private String sql;

		private UpdateTable(String sql) {
			this.sql = sql;
		}
	}

	public DbPostgres() {
		super();
	}

	public String getCreateTable(String table) {
		return CreateTable.valueOf("CREATE"+table.toUpperCase()).sql;
	}
	@Override
	public String getInsertUser() {
		return UpdateTable.valueOf("INSERTUSER").sql;
	}

	@Override
	public String getInsertMessage() {
		return UpdateTable.valueOf("INSERTMESSAGE").sql;
	}

	public <T> Class<Users> getUsersClass() {
		return Users.class;
	}

	public <T> Class<Undelivered> getUndeliveredClass() {
		return Undelivered.class;
	}

	public <T> Class<Messages> getMessagesClass() {
		return Messages.class;
	}

	@Query(QUERYUSERS)
	public interface Users {

		@Column("id")
		Long id();

		@Column("name")
		String name();

		@Column("password")
		String password();

		@Column("ip")
		String ip();

		@Column("last_login")
		Timestamp lastLogin();
	}

	@Query(QUERYMESSAGES)
	public interface Messages {

		@Column("id")
		Long id();

		@Column("message")
		Clob message();

		@Column("from_handle")
		String fromHandle();

		@Column("post_date")
		Timestamp postDate();
	}

	@Query(QUERYUNDELIVERED)
	public interface Undelivered {

		@Column("message_id")
		Long messageId();

		@Column("name")
		String name();

		@Column("message")
		String message();

		@Column("from_handle")
		String fromHandle();

		@Column("post_date")
		Timestamp postDate();
	}

}
