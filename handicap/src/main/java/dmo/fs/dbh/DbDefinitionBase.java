
package dmo.fs.dbh;

import dmo.fs.utils.DodexUtil;
import golf.handicap.db.PopulateCourse;
import golf.handicap.db.PopulateGolfer;
import golf.handicap.db.PopulateGolferScores;
import golf.handicap.db.PopulateScore;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.jdbcclient.JDBCPool;
import io.vertx.rxjava3.mysqlclient.MySQLPool;
import io.vertx.rxjava3.pgclient.PgPool;
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

  public static <T> void setupSql(T pool4) throws IOException, SQLException {
    // Non-Blocking Drivers
    if (pool4 instanceof PgPool) {
    pool = (PgPool) pool4;
      qmark = false;
    } else 
    if (pool4 instanceof MySQLPool) {
      pool = (MySQLPool) pool4;
    } 
    // else if (pool4 instanceof DB2Pool) {
    // pool = (DB2Pool) pool4;
    // } 
    else if (pool4 instanceof JDBCPool) {
      pool = (JDBCPool) pool4;
    }

    Settings settings = new Settings().withRenderNamedParamPrefix("$"); // making compatible with Vertx4/Postgres

    create = DSL.using(DodexUtil.getSqlDialect(), settings);
    // PgClient client = PgClient.pool(vertx, config);

    PopulateGolfer.setQMark(qmark);
    PopulateGolfer.setSqlPool(pool);
    PopulateGolfer.setDslContext(create);
    PopulateGolfer.buildSql();
    PopulateCourse.buildSql();
    PopulateScore.buildSql();
    PopulateGolferScores.buildSql();
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
