
package dmo.fs.db;

import java.sql.Clob;
import java.util.Date;

import org.davidmoten.rx.jdbc.annotations.Column;
import org.davidmoten.rx.jdbc.annotations.Query;

public abstract class DbSqlite3 extends DbDefinitionBase {
	private final static String QUERYUSERS = "select * from users where password=?";
	private final static String QUERYMESSAGES = "select * from messages where id=?";
	private final static String QUERYUNDELIVERED = "Select message_id, name, message, from_handle, post_date from users, undelivered, messages where users.id = user_id and messages.id = message_id and users.id = :id";

	private enum CreateTable {
		CREATEUSERS("create table users (id integer primary key, name text not null unique, password text not null unique, ip text not null, last_login DATETIME not null)"),
		CREATEMESSAGES("create table messages (id integer primary key, message text not null, from_handle text not null, post_date DATETIME not null)"),
		CREATEUNDELIVERED("create table undelivered (user_id integer, message_id integer, CONSTRAINT undelivered_user_id_foreign FOREIGN KEY (user_id) REFERENCES users (id), CONSTRAINT undelivered_message_id_foreign FOREIGN KEY (message_id) REFERENCES messages (id))");
        private String sql;

        private CreateTable(String sql) {
            this.sql = sql;
        }
    };

	public DbSqlite3() {
		super();
	}

	public String getCreateTable(String table) {
		return CreateTable.valueOf("CREATE"+table.toUpperCase()).sql;
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
		Date lastLogin();
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
		Date postDate();
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
		Date postDate();
	}

}
