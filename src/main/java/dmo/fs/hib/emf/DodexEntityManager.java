package dmo.fs.hib.emf;

import com.fasterxml.jackson.databind.JsonNode;
import dmo.fs.dbh.DbConfiguration;
import dmo.fs.utils.DodexUtil;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import org.hibernate.SessionFactory;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import static org.hibernate.cfg.AgroalSettings.*;

public class DodexEntityManager {
  protected static final Logger logger = LoggerFactory.getLogger(DodexEntityManager.class.getName());
  private final DodexUtil dodexUtil = new DodexUtil();
  private String webEnv = System.getenv("VERTXWEB_ENVIRONMENT");
  private static SessionFactory emf;

  public DodexEntityManager() throws IOException {
    if(emf == null) {
      HibernatePersistenceConfiguration config = configSetup();

      emf = Objects.requireNonNull(config).createEntityManagerFactory();
    }
  }

  private HibernatePersistenceConfiguration configSetup() throws IOException {
    JsonNode defaultNode = dodexUtil.getDefaultNode();
    webEnv = (webEnv == null || "prod".equals(webEnv)) ? "prod" : "dev";

    Map<String, String> dbMap = dodexUtil.jsonNodeToMap(defaultNode, webEnv);
    Properties dbProperties = dodexUtil.mapToProperties(dbMap);

    if ("dev".equals(webEnv)) {
      DbConfiguration.configureTestDefaults(dbMap, dbProperties);
    } else {
      DbConfiguration.configureDefaults(dbMap, dbProperties);
    }
    String defaultDb = dodexUtil.getDefaultDb();
    String pu = defaultDb + "." + webEnv;
    String url = dbMap.get("url").concat(dbMap.get("host")).concat(dbMap.get("port")).concat(dbMap.get("dbname"));
    if("mssql".equals(defaultDb)) {
      url = url.concat(dbMap.get("encrypt")).concat(dbMap.get("security"));
    }
    ValidationMode validationMode = ValidationMode.AUTO;
    SharedCacheMode sharedCacheMode = SharedCacheMode.NONE;

    return new HibernatePersistenceConfiguration(pu)
        .provider("org.hibernate.jpa.HibernatePersistenceProvider")
        .mappingFile("/dodex.xml")
        .property(AGROAL_MIN_SIZE, 5)
        .property(AGROAL_MAX_SIZE, 10)
        .property(AGROAL_ACQUISITION_TIMEOUT, Duration.ofMillis(2000))
        .property(AGROAL_LEAK_TIMEOUT, Duration.ofMillis(2000))
        .property(AGROAL_FLUSH_ON_CLOSE, true)
        .jdbcCredentials(
            dbProperties.getProperty("user"),
            dbProperties.getProperty("password")
        )
        .jdbcUrl(url)
        .sharedCacheMode(sharedCacheMode)
        .validationMode(validationMode)
        .showSql(false, false, false);
  }

  public static SessionFactory getEmf() {
    return emf;
  }
}
