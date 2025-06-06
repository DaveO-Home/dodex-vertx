package dmo.fs.utils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import dmo.fs.dbg.HandicapDatabaseG;

import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.*;

import com.fasterxml.jackson.databind.JsonNode;

import dmo.fs.dbg.DbConfiguration;
import org.jooq.meta.jaxb.Generator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JooqGenerate {
  public static void main(String[] args) throws IOException {
    final Logger logger = LoggerFactory.getLogger(JooqGenerate.class.getName());
    DodexUtil dodexUtil = new DodexUtil();
    JsonNode defaultNode = dodexUtil.getDefaultNode();
    Map<String, String> dbMap = dodexUtil.jsonNodeToMap(defaultNode, "dev");
    System.setProperty("org.jooq.no-logo", "true");
    System.setProperty("org.jooq.no-tips", "true");
    final String defaultDb = dodexUtil.getDefaultDb();

    try {
      HandicapDatabaseG handicapDatabase = DbConfiguration.getDefaultDb(true);
      handicapDatabase.checkOnTables().onComplete(c -> {
        String dbUrl = null;
        String jooqMetaName = "org.jooq.meta.sqlite.SQLiteDatabase";
        String databaseDbname = "";
        if ("sqlite3".equals(defaultDb)) {
          dbUrl = dbMap.get("url") + dbMap.get("filename");
        } else if ("mariadb".equals(defaultDb)) {
          dbUrl = dbMap.get("url") + dbMap.get("host") + dbMap.get("dbname") + "?user=" + dbMap.get("CRED:user")
              + "&password=" + dbMap.get("CRED:password");
          jooqMetaName = "org.jooq.meta.mariadb.MariaDBDatabase";
          databaseDbname = dbMap.get("database");
        } else if ("postgres".equals(defaultDb)) {
          dbUrl = dbMap.get("url") + dbMap.get("host") + dbMap.get("dbname") + "?user=" + dbMap.get("CRED:user")
              + "&password=" + dbMap.get("CRED:password");
          jooqMetaName = "org.jooq.meta.postgres.PostgresDatabase";
          databaseDbname = "public"; // dbMap.get("database");
        } else if ("h2".equals(defaultDb)) {
          databaseDbname = "public";
          jooqMetaName = "org.jooq.meta.h2.H2Database";
          dbUrl = dbMap.get("url") + dbMap.get("filename") + ";DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE";
        }
        logger.info("Generate: {} -- {} -- {}", dbUrl, jooqMetaName, databaseDbname);
        generateJooqObjects(dbUrl, jooqMetaName, databaseDbname);
        System.exit(0);
      });
    } catch (SQLException | IOException | InterruptedException e) {
      e.printStackTrace();
    }

    return;
  }

  private static void generateJooqObjects(String jdbcUrl, String jooqMetaName, String databaseDbname) {
    try {
      // ForcedType ft = new ForcedType();
      // ft.setName("BLOB");
      // ft.withIncludeExpression("SEQ|NAME");
      boolean generateSequences = "org.jooq.meta.postgres.PostgresDatabase".equals(jooqMetaName);
      Configuration configuration = new Configuration()
          .withJdbc(new Jdbc()
//               .withDriver("org.sqlite.JDBC")
              .withUrl(jdbcUrl))
          .withGenerator(new Generator()
              .withName("org.jooq.codegen.KotlinGenerator")
              .withDatabase(new Database()
                  // .withForcedTypes(ft)
                  .withName(jooqMetaName)
                  .withOutputSchemaToDefault(true)
                  .withIncludeTables(true)
                  .withIncludePrimaryKeys(true)
                  .withInputSchema(databaseDbname)
                  .withExcludes(
                      "USERS|UNDELIVERED|MESSAGES|SQLITE_SEQUENCE"))
              .withGenerate(new Generate()
                      .withDeprecated(false)
                      .withSequences(generateSequences)
                      .withDaos(false)
                  // .withEmptyCatalogs(true)
              )
              .withTarget(new Target()
                  .withPackageName("golf.handicap.generated")
                  .withClean(true)
                  .withDirectory("./src/main/kotlin")))
          .withLogging(Logging.WARN);
      GenerationTool.generate(configuration);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
