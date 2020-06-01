package dmo.fs.db;

import java.sql.Clob;

import org.davidmoten.rx.jdbc.annotations.Column;
import org.davidmoten.rx.jdbc.annotations.Query;

public abstract class JavaRxDateDb extends DbDefinitionBase implements DodexDatabase {
	private Boolean isTimestamp = false;

	JavaRxDateDb() {
		setIsTimestamp(isTimestamp);
	}

	public Boolean isTimestamp() {
		return isTimestamp;
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
		<T> T lastLogin();
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
		<T> T postDate();
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
		<T> T postDate();
	}

}

