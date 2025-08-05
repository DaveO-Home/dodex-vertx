package dmo.fs.hib;

import dmo.fs.db.MessageUser;
import dmo.fs.db.MessageUserImpl;
import dmo.fs.hib.bld.Database;
import dmo.fs.hib.bld.DatabaseBuild;
import dmo.fs.hib.srv.DodexService;
import dmo.fs.utils.DodexUtil;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;

public abstract class DodexDatabaseBase extends DodexService implements DodexDatabase, Serializable {
  @Serial
  private static final long serialVersionUID = 1L;
  private final static Logger logger = LoggerFactory.getLogger(DodexDatabaseBase.class.getName());

  protected static EntityManager entityManager;
  static protected String dbName;
  protected String persistenceUnitName;

  private DatabaseBuild databaseBuild;

  public DodexDatabaseBase() {
  }

  public void databaseSetup() throws InterruptedException {

    persistenceUnitName =
        entityManager.getEntityManagerFactory().getProperties().get("hibernate.persistenceUnitName").toString();

    Database database = new Database.DatabaseBuilder(
        DodexUtil.getMode(), dbName, persistenceUnitName).build();

    databaseBuild = database.getDatabaseBuild();

    if (databaseBuild == null) {
      throw new InterruptedException("DatabaseBuild is null: Is the Persistence Unit set properly in " +
          "'DbConfiguration.java'? mode/dbName/pu " +
          DodexUtil.getMode() + " -- " + dbName + " -- " + persistenceUnitName + " -- " + database);
    }
  }

  public abstract void configDatabase();

  @Override
  public MessageUser createMessageUser() {
    return new MessageUserImpl();
  }

  public EntityManager getEntityManager() {
    return entityManager;
  }

  public DatabaseBuild
  getDatabaseBuild() {
    return databaseBuild;
  }
}
