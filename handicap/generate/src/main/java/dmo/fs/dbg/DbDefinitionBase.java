
package dmo.fs.dbg;

import dmo.fs.utils.DodexUtil;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.sqlclient.Pool;
import org.jooq.DSLContext;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
public abstract class DbDefinitionBase {
  private final static Logger logger = LoggerFactory.getLogger(DbDefinitionBase.class.getName());

  protected static DSLContext create;
  private Boolean isTimestamp;
  protected Vertx vertx;
  protected static Pool pool;
  private static boolean qmark = true;

  public static <T> void setupSql(Pool client) throws IOException, SQLException {
    // Non-Blocking Drivers
    if (DbConfiguration.isUsingPostgres()) {
        qmark = false;
    }

    pool = client;
    Settings settings = new Settings().withRenderNamedParamPrefix("$"); // making compatible with Vertx4/Postgres

    create = DSL.using(DodexUtil.getSqlDialect(), settings);

  }

  public void setIsTimestamp(Boolean isTimestamp) {
    this.isTimestamp = isTimestamp;
  }

  public boolean getisTimestamp() {
    return this.isTimestamp;
  }

  public Vertx getVertx() {
    return vertx;
  }

  public void setVertx(Vertx vertx) {
    this.vertx = vertx;
  }

  public static DSLContext getCreate() {
    return create;
  }
}
