
package dmo.fs.db.cassandra;

import dmo.fs.db.MessageUser;
import dmo.fs.mqtt.DodexMqttServer;
import dmo.fs.vertx.Server;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.messages.MqttPublishMessage;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.ServerWebSocket;
import org.modellwerkstatt.javaxbus.ConsumerHandler;
import org.modellwerkstatt.javaxbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class DbCassandraBase<T> {
  private final static Logger logger = LoggerFactory.getLogger(DbCassandraBase.class.getName());
  private final Map<String, Promise<MessageUser>> mUserPromises = new ConcurrentHashMap<>();
  private final Map<String, Promise<mjson.Json>> mJsonPromises = new ConcurrentHashMap<>();
  private String vertxConsumer = "";

  private Vertx vertx;

  public Future<mjson.Json> deleteUser(ServerWebSocket ws, T transport, MessageUser messageUser) {
    Promise<mjson.Json> promise = Promise.promise();

    mJsonPromises.put(ws.remoteAddress().toString() + "deleteuser", promise);
    mjson.Json mess = setMessage("deleteuser", messageUser, ws);
    mjson.Json jsonPayLoad = mjson.Json.object().set("msg", mess.getValue());
    if (Server.getUseMqtt()) {
      DodexMqttServer mqttServer = (DodexMqttServer) transport;
      logger.info("Starting Endpoint: {}", ws.remoteAddress().toString() + "deleteuser");
      try {
        mqttServer.publish("dodex-topic", jsonPayLoad);
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    } else {
      EventBus eb = (EventBus) transport;
      eb.send("akka", jsonPayLoad);
    }

    return promise.future();
  }

  public Future<mjson.Json> addMessage(ServerWebSocket ws, MessageUser messageUser,
                                       String message, List<String> undelivered, T transport)
      throws InterruptedException, SQLException {
    Promise<mjson.Json> promise = Promise.promise();

    mJsonPromises.put(ws.remoteAddress().toString() + "addmessage", promise);
    mjson.Json mess = setMessage("addmessage", messageUser, ws);

    if (mess != null) {
      mess.set("users", undelivered).set("message", message);
      mjson.Json jsonPayLoad = mjson.Json.object().set("msg", mess.getValue());
      if (Server.getUseMqtt()) {
        DodexMqttServer mqttServer = (DodexMqttServer) transport;

        mqttServer.publish("dodex-topic", jsonPayLoad);

      } else {
        EventBus eb = (EventBus) transport;
        eb.send("akka", jsonPayLoad);
      }
    }

    return promise.future();
  }

  public abstract MessageUser createMessageUser();

  public Future<MessageUser> selectUser(MessageUser messageUser, ServerWebSocket ws,
                                        T transport) {
    Promise<MessageUser> promise = Promise.promise();
    // This promise will be completed in the eb.consumer/mqtt listener - see
    // setEbConsumer
    mUserPromises.put(ws.remoteAddress().toString() + "selectuser", promise);

    mjson.Json mess = setMessage("selectuser", messageUser, ws);
    mjson.Json jsonPayLoad = mjson.Json.object().set("msg", mess.getValue());
    if (Server.getUseMqtt()) {
      DodexMqttServer mqttServer = (DodexMqttServer) transport;

      mqttServer.publish("dodex-topic", jsonPayLoad);

      MqttEndpoint endpoint = mqttServer.getMqttEndpoint();
      endpoint.publishHandler(message -> {

        logger.debug("Just received message [{}] with QoS [{}]",
            message.payload().toString(Charset.defaultCharset()), message.qosLevel());

        mqttMessageConsumer(message);

        if (message.qosLevel() == MqttQoS.AT_LEAST_ONCE) {
          endpoint.publishAcknowledge(message.messageId());
        } else if (message.qosLevel() == MqttQoS.EXACTLY_ONCE) {
          endpoint.publishReceived(message.messageId());
        }

      }).publishReleaseHandler(endpoint::publishComplete);
    } else {
      EventBus eb = (EventBus) transport;
      if ("".equals(vertxConsumer)) {
        eb.consumer("vertx", setEbConsumer());
        vertxConsumer = "vertx";
      }
      // Send database request to the Akka client microservice -
      // the response is passed back to the requester via the promise completed in the
      // consumer handler
      eb.send("akka", jsonPayLoad);
    }
    // Only one handler for all event bridge sends - see setEbConsumer

    return promise.future();
  }

  public Future<mjson.Json> buildUsersJson(ServerWebSocket ws, T transport,
                                           MessageUser messageUser) {
    Promise<mjson.Json> promise = Promise.promise();

    mJsonPromises.put(ws.remoteAddress().toString() + "allusers", promise);
    mjson.Json mess = setMessage("allusers", messageUser, ws);
    mjson.Json jsonPayLoad = mjson.Json.object().set("msg", mess.getValue());
    if (Server.getUseMqtt()) {
      DodexMqttServer mqttServer = (DodexMqttServer) transport;
      mqttServer.publish("dodex-topic", jsonPayLoad);
    } else {
      EventBus eb = (EventBus) transport;
      eb.send("akka", jsonPayLoad);
    }

    // wait for user json before sending back to newly connected user
    return promise.future();
  }

  public Future<mjson.Json> deleteDelivered(ServerWebSocket ws, T transport,
                                            MessageUser messageUser) {
    Promise<mjson.Json> promise = Promise.promise();

    mJsonPromises.put(ws.remoteAddress().toString() + "deletedelivered", promise);
    mjson.Json mess = setMessage("deletedelivered", messageUser, ws);
    mjson.Json jsonPayLoad = mjson.Json.object().set("msg", mess.getValue());

    if (Server.getUseMqtt()) {
      DodexMqttServer mqttServer = (DodexMqttServer) transport;
      mqttServer.publish("dodex-topic", jsonPayLoad);
    } else {
      EventBus eb = (EventBus) transport;
      eb.send("akka", jsonPayLoad);
    }

    // wait for user json before sending back to newly connected user
    return promise.future();
  }

  public Future<mjson.Json> processUserMessages(ServerWebSocket ws, T transport,
                                                MessageUser messageUser) {
    Promise<mjson.Json> promise = Promise.promise();

    mJsonPromises.put(ws.remoteAddress().toString() + "delivermess", promise);
    mjson.Json mess = setMessage("delivermess", messageUser, ws);
    mjson.Json jsonPayLoad = mjson.Json.object().set("msg", mess.getValue());
    if (Server.getUseMqtt()) {
      DodexMqttServer mqttServer = (DodexMqttServer) transport;
      mqttServer.publish("dodex-topic", jsonPayLoad);
    } else {
      EventBus eb = (EventBus) transport;
      eb.send("akka", jsonPayLoad);
    }

    return promise.future();
  }

  private mjson.Json setMessage(String cmd, MessageUser messageUser, ServerWebSocket ws) {
    mjson.Json mess = null;
    try {
      mess = mjson.Json.object().set("cmd", cmd).set("ip", messageUser.getIp())
          .set("password", messageUser.getPassword()).set("name", messageUser.getName())
          .set("ws", ws.remoteAddress().toString() + cmd);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    return mess;
  }

  public Vertx getVertx() {
    return vertx;
  }

  public void setVertx(Vertx vertx) {
    this.vertx = vertx;
  }

  public ConsumerHandler setEbConsumer() {
    return msg -> {
      if (!msg.isErrorMsg()) {
        mjson.Json json = msg.getBodyAsMJson();

        switch (json.at("cmd").asString()) {
          case "string":
            logger.warn(json.at("msg").asString());
            break;
          case "selectuser":
            mjson.Json cassJson = json.at("msg");
            MessageUser resultUser = createMessageUser();
            resultUser.setId(-1L);
            resultUser.setIp(cassJson.at("ip").asString());
            resultUser.setPassword(cassJson.at("password").asString());
            resultUser.setName(cassJson.at("name").asString());
            resultUser.setLastLogin(
                new Timestamp(cassJson.at("last_login").asLong()));

            mUserPromises.get(json.at("ws").asString()).tryComplete(resultUser);
            mUserPromises.remove(json.at("ws").asString());
            break;
          case "allusers":
          case "delivermess":
          case "addmessage":
          case "deletedelivered":
          case "deleteuser":
            // Passing Akka response back to the requester
            mJsonPromises.get(json.at("ws").asString()).tryComplete(json.at("msg"));
            mJsonPromises.remove(json.at("ws").asString());
            break;
          default:
            break;
        }
      } else {
        logger.error("ERROR received {}", msg.getErrMessage());
      }
    };
  }

  private void mqttMessageConsumer(MqttPublishMessage message) {
    try {
      JsonObject json = message.payload().toJsonObject();

      switch (json.getString("cmd")) {
        case "string":
          logger.warn(json.getString("msg"));
          break;
        case "selectuser":
          JsonObject cassJson = json.getJsonObject("msg");
          MessageUser resultUser = createMessageUser();
          resultUser.setId(-1L);
          resultUser.setIp(cassJson.getString("ip"));
          resultUser.setPassword(cassJson.getString("password"));
          resultUser.setName(cassJson.getString("name"));
          resultUser.setLastLogin(
              new Timestamp(cassJson.getLong("last_login")));

          mUserPromises.get(json.getString("ws")).tryComplete(resultUser);
          mUserPromises.remove(json.getString("ws"));
          break;
        case "allusers":
        case "delivermess":
        case "addmessage":
        case "deletedelivered":
        case "deleteuser":
          // Passing Akka response back to the requester
          mjson.Json payload = mjson.Json.read(message.payload().toString(StandardCharsets.UTF_8));
          mJsonPromises.get(json.getString("ws")).tryComplete(payload.at("msg"));
          mJsonPromises.remove(json.getString("ws"));
          break;
        default:
          break;
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
