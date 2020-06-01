package dmo.fs.spa.db;

import org.davidmoten.rx.jdbc.annotations.Column;
import org.davidmoten.rx.jdbc.annotations.Query;

public abstract class RxJavaDateDb extends SqlBuilder implements SpaDatabase {
	private Boolean isTimestamp = false;

	RxJavaDateDb() {
		setIsTimestamp(isTimestamp);
	}

	public Boolean isTimestamp() {
		return isTimestamp;
	}

	@Query(QUERYLOGIN)
	public interface Login {

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
