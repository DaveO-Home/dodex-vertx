package dmo.fs.hib.routes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dmo.fs.admin.CleanOrphanedUsersHibernate;
import dmo.fs.db.MessageUser;
import dmo.fs.hib.DodexDatabase;
import dmo.fs.hib.fac.DbConfiguration;
import dmo.fs.kafka.KafkaEmitterDodex;
import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import dmo.fs.utils.ParseQueryUtilHelper;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.http.ServerWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public abstract class WebSocketEndpointBase {
  protected static final Logger logger = LoggerFactory.getLogger(WebSocketEndpointBase.class.getName());
  protected final boolean isProduction = DodexUtil.getMode().equals("prod");
  protected boolean isSetupDone;
  protected DodexDatabase dodexDatabase;
  protected KafkaEmitterDodex ke;
  private Boolean isCleaning = false;

  protected Map<String, ServerWebSocket> sessions = new ConcurrentHashMap<>();

  public void setRemoteAddress(String remoteAddress) {
    this.remoteAddress = remoteAddress;
  }

  protected String remoteAddress;

  protected WebSocketEndpointBase() throws IOException {
  }

  public abstract void onOpen(ServerWebSocket session) throws InterruptedException, IOException, SQLException;

  public abstract void onClose(ServerWebSocket session);

  public abstract void onError(ServerWebSocket session, Throwable throwable);

  protected void broadcast(ServerWebSocket session, String message) {
    MessageUser mu = setMessageUser(session);
    sessions.values().stream().filter(s -> !URLDecoder.decode(ParseQueryUtilHelper
        .getQueryMap(s.query()).get("id"), StandardCharsets.UTF_8).equals(mu.getPassword())).forEach(s -> {
      if (!s.isClosed()) {
        s.rxWriteTextMessage(message).doOnError(err -> {
          logger.warn("Unable to send message: {}{} {}",
              URLDecoder.decode(ParseQueryUtilHelper
                  .getQueryMap(s.query()).get("handle"), StandardCharsets.UTF_8), ":", err.getMessage());
        }).subscribe();
      }
    });
  }

  protected void doConnection(ServerWebSocket session) {
    final MessageUser messageUser = setMessageUser(session);

    if (!isSetupDone) {
      isSetupDone = true;
    }
    try {
      MessageUser resultUser = dodexDatabase.selectUser(messageUser);

      try {
        String userJson = dodexDatabase.buildUsersJson(resultUser);
        /*
         * Send list of registered users with connected notification
         */
        session.writeTextMessage("connected:" + userJson); // Users for private messages
        if (ke != null) {
          ke.setValue("sessions", sessions.size());
        }
        /*
         * Send undelivered messages and remove user related messages.
         */
        Map<String, Integer> map = dodexDatabase.processUserMessages(session, resultUser);

        int messageCount = map.get("messages");
        if (messageCount > 0) {
          logger.info("Messages Delivered: {} to {}", messageCount, resultUser.getName());
          if (ke != null) {
            ke.setValue("delivered", messageCount);
          }
        }
      } catch (InterruptedException | SQLException e) {
        session.writeTextMessage(e.getMessage());
        session.close();
        throw new RuntimeException(e);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected void doMessage(ServerWebSocket session, String message) {
    final MessageUser messageUser = setMessageUser(session);
    final ArrayList<String> onlineUsers = new ArrayList<>();
    // Checking if message or command
    final Map<String, String> returnObject = new DodexUtil().commandMessage(message);
    final String selectedUsers = returnObject.get("selectedUsers");
    // message with command stripped out
    final String computedMessage = returnObject.get("message");
    final String command = returnObject.get("command");

    if (";removeuser".equals(command)) {
      try {
        dodexDatabase.deleteUser(messageUser);
      } catch (InterruptedException | SQLException e) {
        session.writeTextMessage("Your Previous handle did not delete: " + e.getMessage());
        throw new RuntimeException(e);
      }
    }
    if (!computedMessage.isEmpty()) {
      sessions.values().stream().filter(s ->
              !URLDecoder.decode(ParseQueryUtilHelper
                  .getQueryMap(s.query()).get("id"), StandardCharsets.UTF_8).equals(messageUser.getPassword()) &&
                  !session.isClosed())
          .forEach(s -> {
            final String handle = URLDecoder.decode(ParseQueryUtilHelper
                .getQueryMap(s.query()).get("handle"), StandardCharsets.UTF_8);
            // broadcast
            if ("".equals(selectedUsers) && "".equals(command)) {
              s.writeTextMessage(messageUser.getName() + ": " + computedMessage);

              // private message
            } else if (Arrays.stream(selectedUsers.split(",")).anyMatch(h -> {
              boolean isMatched = false;
              isMatched = h.contains(handle);
              return isMatched;
            })) {
              s.rxWriteTextMessage(messageUser.getName() + ": " + computedMessage)
                  .doOnError(err -> {
                    logger.info("Websocket-connection...Unable to send message: {}{} {}",
                        handle, ": ", err.getMessage());
                  }).subscribe();
              // keep track of delivered messages
              onlineUsers.add(handle);
            }
          });

      if ("".equals(selectedUsers) && !"".equals(command)) {
        session.rxWriteTextMessage("Private user not selected").subscribe();
      } else {
        session.rxWriteTextMessage("ok").subscribe();
        if (ke != null) {
          if (!selectedUsers.isEmpty()) {
            ke.setValue("private", 1);
          } else {
            ke.setValue(1); // broadcast
          }
        }
      }
    }

    // calculate difference between selected and online users
    if (!"".equals(selectedUsers)) {
      final List<String> selected = Arrays.asList(selectedUsers.split(","));
      final List<String> disconnectedUsers = selected.stream().filter(user -> !onlineUsers.contains(user))
          .collect(Collectors.toList());
      // Save undelivered message to send when to-user logs in
      if (!disconnectedUsers.isEmpty()) {
        try {
          Long id = dodexDatabase.addMessage(messageUser, computedMessage);
          if (ke != null) {
            ke.setValue("undelivered", disconnectedUsers.size());
          }
          try {
            dodexDatabase.addUndelivered(messageUser, disconnectedUsers, id);
          } catch (SQLException e) {
            throw new RuntimeException(e);
          }
        } catch (InterruptedException | SQLException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  protected MessageUser setMessageUser(ServerWebSocket session) {
    final MessageUser messageUser = dodexDatabase.createMessageUser();
    ServerWebSocket sess = sessions.get(URLDecoder.decode(ParseQueryUtilHelper
        .getQueryMap(session.query()).get("id"), StandardCharsets.UTF_8));
    if (sess == null) {
      sess = session;
    }
    String handle;
    String id;

    handle = URLDecoder.decode(ParseQueryUtilHelper
        .getQueryMap(sess.query()).get("handle"), StandardCharsets.UTF_8);
    id = URLDecoder.decode(ParseQueryUtilHelper
        .getQueryMap(sess.query()).get("id"), StandardCharsets.UTF_8);

    messageUser.setName(handle);
    messageUser.setPassword(id);
    messageUser.setIp(session.remoteAddress() == null ? "Unknown" : remoteAddress);

    return messageUser;
  }

  protected void setup() throws InterruptedException, IOException, SQLException {
    dodexDatabase = DbConfiguration.getDefaultDb();
    dodexDatabase.entityManagerSetup();
    dodexDatabase.databaseSetup();
    dodexDatabase.configDatabase();

    /*
     * Optional auto user cleanup - config in "application-conf.json". When client
     * changes handle when server is down, old users and undelivered messages will
     * be orphaned.
     *
     * Defaults: off - when turned on 1. execute on start up and every 7 days
     * thereafter. 2. remove users who have not logged in for 90 days.
     */
    if(!isCleaning) {
      try {
        JsonObject config;
        ObjectMapper jsonMapper = new ObjectMapper();
        JsonNode node;

        try (InputStream in = getClass().getResourceAsStream("/application-conf.json")) {
          node = jsonMapper.readTree(in);
        }
        config = JsonObject.mapFrom(node);

        final Optional<Boolean> runClean = Optional.ofNullable(config.getBoolean("clean.run"));
        if (runClean.isPresent() && runClean.get().equals(true)) {
          final CleanOrphanedUsersHibernate clean = new CleanOrphanedUsersHibernate();
          isCleaning = true;
          clean.startClean(config);
        }
      } catch (final Exception e) {
        logger.info("{}Context Configuration failed...{}{}",
            ColorUtilConstants.RED_BOLD_BRIGHT, e.getMessage(), ColorUtilConstants.RESET);
        throw new RuntimeException(e);
      }
    }
    String db = DbConfiguration.getDb();
  }
}
