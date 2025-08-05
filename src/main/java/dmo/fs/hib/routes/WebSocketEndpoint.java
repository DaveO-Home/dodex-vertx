package dmo.fs.hib.routes;

import dmo.fs.kafka.KafkaEmitterDodex;
import dmo.fs.utils.DodexUtil;
import dmo.fs.utils.ParseQueryUtilHelper;
import dmo.fs.vertx.Server;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpServer;
import io.vertx.rxjava3.core.http.ServerWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Locale;

public class WebSocketEndpoint extends WebSocketEndpointBase implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;
  protected static final Logger logger = LoggerFactory.getLogger(WebSocketEndpoint.class.getName());


  public WebSocketEndpoint() throws IOException {
    System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$s] %5$s %3$s %n");
    System.setProperty("dmo.fs.level", "INFO");
    System.setProperty("org.jooq.no-logo", "true");
    String value = DodexUtil.getMode();

    Locale.setDefault(Locale.US);
    if (isProduction) {
      DodexUtil.setEnv("prod");
    } else {
      DodexUtil.setEnv(value);
    }
    if (Server.getUseKafka()) {
      ke = new KafkaEmitterDodex();
    }
  }

  public void setWebSocket(final HttpServer server) {

    server.webSocketHandler(ws -> {
      try {
        setup();
        onOpen(ws);
      } catch (InterruptedException | SQLException | IOException e) {
        onError(ws, e);
        throw new RuntimeException(e);
      }
      ws.closeHandler(ch -> {
        onClose(ws);
      });
      ws.handler(new Handler<Buffer>() {
        @Override
        public void handle(final Buffer data) {
          try {
            if (data.getByte(0) == 0x9) {
              byte b = Byte.parseByte("0x9", 16);
              ws.write(data.setByte(0, b));
            } else {
              onMessage(ws, data.toString());
            }
          } catch (SQLException | InterruptedException | IOException e) {
            throw new RuntimeException(e);
          }
        }
      });
      ws.exceptionHandler(err -> {
        onError(ws, err);
        throw new RuntimeException(err);
      });
    });

  }

  @Override
  public void onOpen(ServerWebSocket session) throws InterruptedException, IOException, SQLException {
    sessions.put(URLDecoder.decode(ParseQueryUtilHelper
        .getQueryMap(session.query()).get("id"), StandardCharsets.UTF_8), session);

    doConnection(session);
    broadcast(session, "User " + URLDecoder.decode(ParseQueryUtilHelper
        .getQueryMap(session.query()).get("handle"), StandardCharsets.UTF_8) + " joined");
    if (ke != null) {
      ke.setValue("sessions", sessions.size());
    }
  }

  public void onMessage(ServerWebSocket session, String message) throws SQLException, IOException, InterruptedException {
    doMessage(session, message);
  }

  @Override
  public void onClose(ServerWebSocket session) {
    sessions.remove(URLDecoder.decode(ParseQueryUtilHelper
        .getQueryMap(session.query()).get("id"), StandardCharsets.UTF_8));

    if (logger.isInfoEnabled()) {
      logger.info("Closing ws-connection to client: {}",
          URLDecoder.decode(ParseQueryUtilHelper
              .getQueryMap(session.query()).get("handle"), StandardCharsets.UTF_8));
    }
    broadcast(session, "User " + URLDecoder.decode(ParseQueryUtilHelper
        .getQueryMap(session.query()).get("handle"), StandardCharsets.UTF_8) + " left");
    if (ke != null) {
      ke.setValue("sessions", sessions.size());
    }
  }

  @Override
  public void onError(ServerWebSocket session, Throwable throwable) {
    sessions.remove(URLDecoder.decode(ParseQueryUtilHelper
        .getQueryMap(session.query()).get("id"), StandardCharsets.UTF_8));
    if (logger.isInfoEnabled()) {
      logger.info("Websocket-failure...User {} {} {}", URLDecoder.decode(ParseQueryUtilHelper
              .getQueryMap(session.query()).get("handle"), StandardCharsets.UTF_8), "left on error:",
          throwable.getMessage());
    }
  }
}
