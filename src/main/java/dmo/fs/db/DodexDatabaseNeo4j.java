package dmo.fs.db;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.async.AsyncSession;
import org.neo4j.driver.exceptions.NoSuchRecordException;
import org.neo4j.driver.reactive.RxResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Promise;

public class DodexDatabaseNeo4j extends DbNeo4j {
	private final static Logger logger = LoggerFactory.getLogger(DodexDatabaseNeo4j.class.getName());
	protected Properties dbProperties = new Properties();
	protected Map<String, String> dbOverrideMap = new ConcurrentHashMap<>();
	protected Map<String, String> dbMap = new ConcurrentHashMap<>();
	protected JsonNode defaultNode;
	protected String webEnv = System.getenv("VERTXWEB_ENVIRONMENT");
	protected DodexUtil dodexUtil = new DodexUtil();

	public DodexDatabaseNeo4j(Map<String, String> dbOverrideMap, Properties dbOverrideProps)
			throws InterruptedException, IOException, SQLException {
		super();

		defaultNode = dodexUtil.getDefaultNode();

		webEnv = webEnv == null || "prod".equals(webEnv) ? "prod" : "dev";

		dbMap = dodexUtil.jsonNodeToMap(defaultNode, webEnv);
		dbProperties = dodexUtil.mapToProperties(dbMap);

		if (dbOverrideProps != null && dbOverrideProps.size() > 0) {
			this.dbProperties = dbOverrideProps;
		}
		if (dbOverrideMap != null) {
			this.dbOverrideMap = dbOverrideMap;
		}

		DbConfiguration.mapMerge(dbMap, dbOverrideMap);
	}

	public DodexDatabaseNeo4j() throws InterruptedException, IOException, SQLException {
		super();

		defaultNode = dodexUtil.getDefaultNode();
		webEnv = webEnv == null || "prod".equals(webEnv) ? "prod" : "dev";

		dbMap = dodexUtil.jsonNodeToMap(defaultNode, webEnv);
		dbProperties = dodexUtil.mapToProperties(dbMap);
	}

	@Override
	public MessageUser createMessageUser() {
		return new MessageUserImpl();
	}

	@Override
	public Promise<Driver> databaseSetup() {
		if ("dev".equals(webEnv)) {
			// dbMap.put("dbname", "myDbname"); // this wiil be merged into the default map
			DbConfiguration.configureTestDefaults(dbMap, dbProperties);
		} else {
			DbConfiguration.configureDefaults(dbMap, dbProperties); // Prod
		}

		Promise<Driver> promise = Promise.promise();
		Driver driver = getDriver(dbMap, dbProperties);

		AsyncSession session = driver.asyncSession();

		session.runAsync(getCheckConstraints())
			.thenCompose(fn -> fn.singleAsync())
			.handle((record, exception) -> {
				if (exception != null) {
					Throwable source = exception;
					if (exception instanceof CompletionException) {
						source = ((CompletionException) exception).getCause();
					}
					Status status = Status.INTERNAL_SERVER_ERROR;
					if (source instanceof NoSuchRecordException) {
						status = Status.NOT_FOUND;
					}
					logger.error("Error Checking Constraints: {}{}{}", ColorUtilConstants.RED,
							status.getReasonPhrase(), ColorUtilConstants.RESET);
					source.printStackTrace();
					return -1;
				} else {
					return record.get(0).asInt();
				}
			}).whenCompleteAsync((count, exception) -> {
				if (count < 4) {
					session
					.runAsync(getCheckApoc())
					.thenCompose(fn -> fn.singleAsync())
					.handle((record, exception2) -> {
						if (exception2 != null) {
							Throwable source = exception2;
							if (exception2 instanceof CompletionException) {
								source = ((CompletionException) exception2).getCause();
							}
							Status status = Status.INTERNAL_SERVER_ERROR;
							if (source instanceof NoSuchRecordException) {
								status = Status.NOT_FOUND;
							}
							logger.error("Error Checking Apoc procedures: {}{}{}", ColorUtilConstants.RED,
									status.getReasonPhrase(), ColorUtilConstants.RESET);
							source.printStackTrace();
							return -1;
						} else {
							session.closeAsync();
							return record.get(0).asInt();
						}
					}).whenCompleteAsync((apocCount, exception3) -> {
						if(apocCount != -1) {
							if(apocCount == 1) {
								apocConstraints(driver, promise);
							} else {
								createConstraints(driver, promise);
							}
						}
					});
				} else {
					promise.complete(driver);
				}
			});

		return promise;
	}
	// If apoc plugin installed
	private void apocConstraints(Driver driver, Promise<Driver> promise) {
		Multi.createFrom().resource(driver::rxSession,
			session -> session.writeTransaction(tx -> {
				RxResult resultConstraints = tx.run(getCreateConstraints());
				return Multi.createFrom().publisher(resultConstraints.records())
						.map(record -> "constraints");
			}))
			.withFinalizer(session -> {
				promise.complete(driver);
				return Uni.createFrom().publisher(session.close());
			})
			.onFailure().invoke(Throwable::printStackTrace)
			.subscribe().asStream();
	}
	// If minimum install
	private void createConstraints(Driver driver, Promise<Driver> promise) {
		Session session = driver.session();
		if (session != null) {
			try (Transaction tx = session.beginTransaction()) {
				tx.run(getUserNameConstraint());
				tx.run(getUserPasswordConstraint());
				tx.run(getMessageNameConstraint());
				tx.run(getMessagePasswordConstraint());
				tx.commit();
			} catch(Exception ex) {
				ex.printStackTrace();
			}
			session.close();
			promise.complete(driver);
		}
	}

	private Driver getDriver(Map<String, String> dbMap, Properties dbProperties) {
		String uri = String.join(":", dbMap.get("protocol"),
				String.format("//%s", dbMap.get("host")),
				dbMap.get("port"));
		Driver neo4jDriver = GraphDatabase.driver(uri,
				AuthTokens.basic(dbProperties.getProperty("user"), dbProperties.getProperty("password")));

		return neo4jDriver;
	}
}