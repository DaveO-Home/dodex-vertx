package dmo.fs.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.sql.Clob;
import java.sql.Timestamp;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.davidmoten.rx.jdbc.Database;
import org.davidmoten.rx.jdbc.annotations.Column;
import org.davidmoten.rx.jdbc.annotations.Query;

import dmo.fs.utils.ConsoleColors;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.sql.SQLException;

public abstract class JavaRxDateDb extends DbDefinitionBase implements DodexDatabase {
    private final static Logger logger = LoggerFactory.getLogger(JavaRxDateDb.class.getName());
    private Database db = null;

    public void setDatabase(Database db) {
        this.db = db;
    }

    @Override
	public int addUndelivered(ServerWebSocket ws, List<String> undelivered, Long messageId, Database db) {
		int count = 0;
		try {
			for (String name : undelivered) {
				Long userId = getUserIdByName(name, db);
				addUndelivered(userId, messageId, db);
				count++;
			}
		} catch (Exception e) {
			ws.writeTextMessage(e.getMessage());
		}
		return count;
	}

	@Override
	public Long getUserIdByName(String name, Database db) throws InterruptedException {
		List<Long> userKey = new ArrayList<>();

		Disposable disposable = db.select(getUserByName())
				.parameter("NAME", name)
				.autoMap(Users.class)
				.doOnNext(result -> {
					userKey.add(result.id());
				}).subscribe(result -> {
					//
				}, throwable -> {
					logger.error("{0}Error finding user by name: {2}{1}",
							new Object[] { ConsoleColors.RED, ConsoleColors.RESET, name });
					throwable.printStackTrace();
				});
		await(disposable);
		return userKey.get(0);
	}

	@Override
	public MessageUser selectUser(MessageUser messageUser, ServerWebSocket ws, Database db)
			throws InterruptedException, SQLException {
		MessageUser resultUser = createMessageUser();

		Disposable disposable = db.select(Users.class).parameter(messageUser.getPassword())
			.get()
			.doOnNext(result -> {
				resultUser.setId(result.id());
				resultUser.setName(result.name());
				resultUser.setPassword(result.password());
				resultUser.setIp(result.ip());
				resultUser.setLastLogin(new Timestamp(result.lastLogin().getTime()));
			})
			.isEmpty()
			.doOnSuccess(empty -> {
				if (empty) {
					addUser(ws, db, messageUser);
				}
		}).doAfterSuccess(record -> {
			db.select(Users.class).parameter(messageUser.getPassword()).get().doOnNext(result -> {
				resultUser.setId(result.id());
				resultUser.setName(result.name());
				resultUser.setPassword(result.password());
				resultUser.setIp(result.ip());
				resultUser.setLastLogin(new Timestamp(result.lastLogin().getTime()));
			}).subscribe(result -> {
				//
			}, throwable -> {
				logger.error("{0}Error finding user {1}{2}",
						new Object[] { ConsoleColors.RED, messageUser.getName(), ConsoleColors.RESET });
				throwable.printStackTrace();
			});
		}).subscribe(result -> {
			//
		}, throwable -> {
			logger.error("{0}Error adding user {1}{2}",
					new Object[] { ConsoleColors.RED, messageUser.getName(), ConsoleColors.RESET });
			throwable.printStackTrace();
		});

		await(disposable);
		//Changing last login datetime
		if(DbConfiguration.isUsingSqlite3()) {
			updateSqliteUser(ws, db, resultUser);
		}
		else {
			updateUser(ws, db, resultUser);
		}
		return resultUser;
	}

	@Override
	public StringBuilder buildUsersJson(MessageUser messageUser) throws InterruptedException, SQLException {
		StringBuilder userJson = new StringBuilder();
		ObjectMapper mapper = new ObjectMapper();

		class User {
			String name = null;

			public void setName(String name) {
				this.name = name;
			}

			public String getName() {
				return name;
			}
		}
		class AllUsers implements Action {
			@Override
			public void run() throws Exception {
				userJson.append("]");
			}
		}

		AllUsers allUsers = new AllUsers();
		List<String> delimiter = new ArrayList<>();
		User user = new User();
		userJson.append("[");
		delimiter.add("");

		Disposable disposable = db.select(getAllUsers())
				.parameter("NAME", messageUser.getName())
				.autoMap(Users.class)
				.doOnNext(result -> {
					// build json = ["name": "user1", "name": "user2", etc...]
					user.setName(result.name());
					userJson.append(delimiter.get(0) + mapper.writeValueAsString(user));
					delimiter.set(0, ",");
				}).doOnComplete(allUsers).subscribe(result -> {
					//
				}, throwable -> {
					logger.error("{0}Error building registered user list{1}",
							new Object[] { ConsoleColors.RED_BOLD_BRIGHT, ConsoleColors.RESET });
					throwable.printStackTrace();
				});
		// wait for user json before sending back to newly connected user
		await(disposable);
		return userJson;
	}

	List<Long> messageIds = new ArrayList<>();
	Long userId = null;

	class RemoveUndelivered implements Action {
		int count = 0;

		@Override
		public void run() throws Exception {
			for (Long messageId : messageIds) {
				Disposable disposable = db.update(getRemoveUndelivered())
					.parameter("USERID", userId)
					.parameter("MESSAGEID", messageId)
					.counts()
					.doOnNext(c -> count += c)
					.subscribe(result -> {
						//
					}, throwable -> {
						logger.error("{0}Error removing undelivered record{1}",
								new Object[] { ConsoleColors.RED, ConsoleColors.RESET });
						throwable.printStackTrace();
					});
				await(disposable);
			}
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}
	}

	RemoveUndelivered removeUndelivered = new RemoveUndelivered();

	class RemoveMessage implements Action {
		int count = 0;

		@Override
		public void run() throws Exception {
			for (Long messageId : messageIds) {
				Disposable disposable = db.select(getUndeliveredMessage())
						.parameter("USERID", userId)
						.parameter("MESSAGEID", messageId)
						.getAs(Undelivered.class)
						.isEmpty()
						.doOnSuccess(empty -> {
							if (empty) {
								Disposable disposable2 = db.update(getRemoveMessage())
										.parameter("MESSAGEID", messageId)
										.counts()
										.doOnNext(c -> count += c)
										.subscribe(result -> {
											//
										}, throwable -> {
											logger.error("{0}Error removing undelivered message{1}",
													new Object[] { ConsoleColors.RED, ConsoleColors.RESET });
											throwable.printStackTrace();
										});
								await(disposable2);
							}
						}).subscribe(result -> {
							//
						}, throwable -> {
							logger.error("{0}Error finding undelivered message{1}",
									new Object[] { ConsoleColors.RED, ConsoleColors.RESET });
							throwable.printStackTrace();
						});
				await(disposable);
			}
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}
	}

	RemoveMessage removeMessage = new RemoveMessage();

	@Override
	public int processUserMessages(ServerWebSocket ws, MessageUser messageUser) {
		userId = messageUser.getId();
		removeUndelivered.setCount(0);
		removeMessage.setCount(0);
		db = getDatabase();
		/*
		 * Get all undelivered messages for current user
		 */
		db.select(Undelivered.class).parameter("id", messageUser.getId()).get().doOnNext(result -> {
			Date postDate = result.postDate();
			String handle = result.fromHandle();
			String message = result.message();
			// Send messages back to client
			ws.writeTextMessage(handle + postDate + " " + message);
			messageIds.add(result.messageId());
		})
				// Remove undelivered for user
				.doOnComplete(removeUndelivered)
				.doOnError(error -> logger.error("{0}Remove Undelivered Error: {1}{2}",
						new Object[] { ConsoleColors.RED, error.getMessage(), ConsoleColors.RESET }))
				// Remove message if no other users are attached
				.doFinally(removeMessage).doOnError(error -> logger.error("{0}Remove Message Error: {1}{2}",
						new Object[] { ConsoleColors.RED, error.getMessage(), ConsoleColors.RESET }))
				.subscribe();
		return removeUndelivered.getCount();
	}

	private static void await(Disposable disposable) throws InterruptedException {
		while (!disposable.isDisposed()) {
			Thread.sleep(100);
		}
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

