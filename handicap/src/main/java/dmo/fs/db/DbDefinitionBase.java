
package dmo.fs.db;

import static org.jooq.impl.DSL.deleteFrom;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.insertInto;
import static org.jooq.impl.DSL.notExists;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.table;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.plexus.component.configurator.converters.ConfigurationConverter;
import org.jooq.DSLContext;
import org.jooq.codegen.GenerationTool;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.jooq.meta.jaxb.Configuration;
import org.jooq.meta.jaxb.Database;
import org.jooq.meta.jaxb.Generate;
import org.jooq.meta.jaxb.Generator;
import org.jooq.meta.jaxb.Jdbc;
import org.jooq.meta.jaxb.Logging;
import org.jooq.meta.jaxb.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import javax.sql.*;

import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import dmo.fs.utils.JooqGenerate;
import io.reactivex.rxjava3.functions.Action;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.jdbcclient.JDBCPool;
import io.vertx.rxjava3.mysqlclient.MySQLPool;
import io.vertx.rxjava3.pgclient.PgPool;
import io.vertx.rxjava3.sqlclient.Pool;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.RowSet;
import io.vertx.rxjava3.sqlclient.SqlConnection;
import io.vertx.rxjava3.sqlclient.Tuple;
import golf.handicap.db.*;

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
