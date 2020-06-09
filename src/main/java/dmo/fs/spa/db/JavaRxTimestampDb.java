package dmo.fs.spa.db;

import java.sql.Clob;

import org.davidmoten.rx.jdbc.annotations.Column;
import org.davidmoten.rx.jdbc.annotations.Query;

public abstract class JavaRxTimestampDb extends SqlBuilder implements SpaDatabase {
	private Boolean isTimestamp = true;

	JavaRxTimestampDb() {
		setIsTimestamp(isTimestamp);
	}

	public Boolean isTimestamp() {
		return isTimestamp;
	}

    @Query(QUERYLOGIN)
	public interface Users {

		@Column("id")
		Long id();

		@Column("name")
		String name();

		@Column("password")
		String password();

		@Column("last_login")
		<T> T lastLogin();
	}

}

