package dmo.fs.mqtt;

import dmo.fs.router.CassandraRouter;
import dmo.fs.utils.DodexUtil;
import io.netty.handler.codec.mqtt.MqttProperties;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttTopicSubscription;
import io.vertx.mqtt.MqttWill;
import io.vertx.mqtt.messages.codes.MqttSubAckReasonCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DodexMqttServer {
  private static final Logger logger = LoggerFactory.getLogger(DodexMqttServer.class.getName());
  private MqttEndpoint mqttEndpoint;
  private String subscriptionTopic;
  private Boolean firstTime = true;
  public DodexMqttServer() {
    Vertx vertx = DodexUtil.getVertx().getDelegate();
    mqttServer(vertx);
  }

  void mqttServer(Vertx vertx) {
    MqttServer mqttServer = MqttServer.create(vertx);
    mqttServer.endpointHandler(endpoint -> {
          mqttEndpoint = endpoint;

          logger.debug("MQTT client [{}] request to connect, clean session = {}",
              endpoint.clientIdentifier(), endpoint.isCleanSession());

          if (endpoint.auth() != null) {
            logger.debug("[username = {}, password = {}]", endpoint.auth().getUsername(), endpoint.auth().getPassword());
          }
          logger.debug("[properties = {}]", endpoint.connectProperties().listAll().toArray());

          MqttWill will = endpoint.will();
          if (will != null) {
            logger.debug("[will topic = {} msg = {} QoS = {} isRetain = {}]",
                will.getWillTopic() == null ? false : will.getWillTopic(),
                will.getWillMessageBytes() == null ? 0x01 : Arrays.toString(will.getWillMessageBytes()),
                will.getWillQos(), will.isWillRetain());
          }

          logger.debug("[keep alive timeout = {}]", endpoint.keepAliveTimeSeconds());

          // accept connection from the remote client
          endpoint.accept(true);

          endpoint.disconnectMessageHandler(disconnectMessage -> {
            logger.info("Received disconnect from client, reason code = {}", disconnectMessage.code());
          });

          endpoint.subscribeHandler(subscribe -> {
            logger.debug("Subscribe: {}", subscribe.properties().listAll().toArray());
            List<MqttSubAckReasonCode> reasonCodes = new ArrayList<>();
            for (MqttTopicSubscription s : subscribe.topicSubscriptions()) {
              logger.debug("Subscription for {} with QoS {}", s.topicName(), s.qualityOfService());
              reasonCodes.add(MqttSubAckReasonCode.qosGranted(s.qualityOfService()));
              subscriptionTopic = s.topicName();
            }

            endpoint.subscribeAcknowledge(subscribe.messageId(), reasonCodes, MqttProperties.NO_PROPERTIES);
          });

          endpoint.publishAcknowledgeHandler(messageId ->
                  logger.debug("Received ack for message from acknowledge handler = {}", messageId))
              .publishReceivedHandler(endpoint::publishRelease).publishCompletionHandler(
                  messageId -> logger.debug("Received ack for message = {}", messageId));

          endpoint.unsubscribeHandler(unsubscribe -> {

            for (String t : unsubscribe.topics()) {
              logger.debug("Subscription for {}", t);
            }

            endpoint.unsubscribeAcknowledge(unsubscribe.messageId());
          });

          endpoint.pingHandler(v -> {
          });
          if(firstTime) {
            endpoint.publishHandler(message -> {
              firstTime = false;
              logger.info(message.payload().toJsonObject().getString("msg")); //toString(Charset.defaultCharset()));

              if (message.qosLevel() == MqttQoS.AT_LEAST_ONCE) {
                endpoint.publishAcknowledge(message.messageId());
              } else if (message.qosLevel() == MqttQoS.EXACTLY_ONCE) {
                endpoint.publishReceived(message.messageId());
              }

            }).publishReleaseHandler(endpoint::publishComplete);
          }
        })
        .listen()
        .onComplete(ar -> {

          if (ar.succeeded()) {

            logger.info("MQTT server is listening on port {}", ar.result().actualPort());

          } else {
            logger.info("Error on starting the server");
            ar.cause().printStackTrace();
          }
        }).onSuccess(mqServer -> CassandraRouter.setMqttServer(this));
  }

  public void publish(String topic, Object message) {
    publish(mqttEndpoint, topic, message);
  }

  public void publish(MqttEndpoint endpoint, String topic, Object message) {
    try {
      endpoint.publish(subscriptionTopic,
          Buffer.buffer(message.toString().getBytes(StandardCharsets.UTF_8)),
          MqttQoS.EXACTLY_ONCE,
          false,
          false);
    } catch (IllegalStateException | NullPointerException isx) {
      logger.error("No clients connected: {}", isx.getMessage());
    }
  }

  public MqttEndpoint getMqttEndpoint() {
    return mqttEndpoint;
  }
}
