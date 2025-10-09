package dmo.fs.db.ora;

import dmo.fs.hib.DodexDatabaseBase;
import dmo.fs.hib.emf.DodexEntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

public class DodexDatabaseOracle extends DodexDatabaseBase implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;
  private final static Logger logger = LoggerFactory.getLogger(DodexDatabaseOracle.class.getName());

  private static SessionFactory emf;
  static protected String dbName;

  public DodexDatabaseOracle() {
    super();
  }

  @Override
  public void entityManagerSetup() {
    SessionFactory sessionFactory;
    try {
      sessionFactory = DodexEntityManager.getEmf();
      if(sessionFactory == null) {
        new DodexEntityManager();
        sessionFactory = DodexEntityManager.getEmf();
        emf = sessionFactory;
      }
    } catch (IOException ioe) {
      throw new RuntimeException(ioe.getMessage());
    }
    entityManager = Objects.requireNonNull(sessionFactory).createEntityManager();

    Map<String, Object> properties = sessionFactory.getProperties();
    Object sessionDbname = properties.get("dbname");
    dbName = sessionDbname != null ? sessionDbname.toString() : "dodex";
    Object mode = properties.get("mode");
  }

  @Override
  public void entityManagerSetup(EntityManagerFactory emf) {
  }

  @Override
  public void configDatabase() {
    String[] types = {"TABLE"};

    Session session = entityManager.unwrap(Session.class);

    session.doWork(connection -> {
      try {
        connection.setAutoCommit(false);
        DatabaseMetaData databaseMetaData = connection.getMetaData();
        boolean didCreateATable = false;

        ResultSet rsDbname = databaseMetaData.getTables(dbName, null, "USERS", types);

        if (!rsDbname.next()) {
          connection.createStatement().execute(getDatabaseBuild().getCreateTable("USERS"));
          connection.createStatement().execute(getDatabaseBuild().getCreateTable("NAMEIDX"));
          connection.createStatement().execute(getDatabaseBuild().getCreateTable("PASSWORDIDX"));
          didCreateATable = true;
          logger.info("Users Table Created.");
        }
        rsDbname = databaseMetaData.getTables(dbName, null, "MESSAGES", types);
        if (!rsDbname.next()) {
          connection.createStatement().execute(getDatabaseBuild().getCreateTable("MESSAGES"));
          didCreateATable = true;
          logger.info("Messages Table Created.");
        }
        rsDbname = databaseMetaData.getTables(dbName, null, "UNDELIVERED", types);
        if (!rsDbname.next()) {
          connection.createStatement().execute(getDatabaseBuild().getCreateTable("UNDELIVERED"));
          didCreateATable = true;
          logger.info("Undelivered Table Created.");
        }
        rsDbname = databaseMetaData.getTables(dbName, null, "GROUPS", types);
        if (!rsDbname.next()) {
          connection.createStatement().execute(getDatabaseBuild().getCreateTable("GROUPS"));
          didCreateATable = true;
          logger.info("Groups Table Created.");
        }
        rsDbname = databaseMetaData.getTables(dbName, null, "MEMBER", types);
        if (!rsDbname.next()) {
          connection.createStatement().execute(getDatabaseBuild().getCreateTable("MEMBER"));
          didCreateATable = true;
          logger.info("Member Table Created.");
        }
        rsDbname = databaseMetaData.getTables(dbName, null, "GOLFER", types);
        if (!rsDbname.next()) {
          connection.createStatement().execute(getDatabaseBuild().getCreateTable("GOLFER"));
          didCreateATable = true;
          logger.info("Golfer Table Created.");
        }
        rsDbname = databaseMetaData.getTables(dbName, null, "COURSE", types);
        if (!rsDbname.next()) {
          connection.createStatement().execute(getDatabaseBuild().getCreateTable("COURSE"));
          didCreateATable = true;
          logger.info("Course Table Created.");
        }
        rsDbname = databaseMetaData.getTables(dbName, null, "RATINGS", types);
        if (!rsDbname.next()) {
          connection.createStatement().execute(getDatabaseBuild().getCreateTable("RATINGS"));
          connection.createStatement().execute(getDatabaseBuild().getCreateTable("RATINGSIDX"));
          didCreateATable = true;
          logger.info("Ratings Table Created.");
        }
        rsDbname = databaseMetaData.getTables(dbName, null, "SCORES", types);
        if (!rsDbname.next()) {
          connection.createStatement().execute(getDatabaseBuild().getCreateTable("SCORES"));
          didCreateATable = true;
          logger.info("Scores Table Created.");
        }
        if (didCreateATable) {
          ResultSet rsSchemas = databaseMetaData.getCatalogs();
          while (rsSchemas.next()) {
            if (dbName.equals(rsSchemas.getString(1))) {
              logger.warn("Used database: '{}' to Create Tables.", dbName);
            }
          }
        }
        rsDbname.close();
        connection.commit();
        connection.close();
      } catch (SQLException se) {
        throw new RuntimeException(se.getMessage());
      }
    });
    setupSql(entityManager);
  }

  public SessionFactory getEmf() {
    return emf;
  }
}
