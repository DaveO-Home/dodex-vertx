package dmo.fs.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dmo.fs.vertx.Server;
import io.vertx.kafka.admin.Config;
import io.vertx.kafka.admin.ConfigEntry;
import io.vertx.kafka.admin.KafkaAdminClient;
import io.vertx.kafka.admin.TopicDescription;
import io.vertx.kafka.client.common.ConfigResource;
import io.vertx.kafka.client.common.TopicPartitionInfo;
import io.vertx.rxjava3.kafka.client.consumer.KafkaConsumer;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.*;

public class KafkaConsumerDodex {
  private static final Logger logger =
      LoggerFactory.getLogger(KafkaConsumerDodex.class.getName());
  private static final String topic = "dodex-events";
  private static final Set<DodexEventData> dodexEventData =
      Collections.newSetFromMap(Collections.synchronizedMap(new LinkedHashMap<>()));
  private final ObjectMapper objectMapper = new ObjectMapper();

  public KafkaConsumerDodex() {
    config();
    setRetention("15000");
  }

  private void config() {
    Map<String, String> config = new HashMap<>();
    config.put("bootstrap.servers", "localhost:9092");
    config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    config.put("value.deserializer",
        "org.apache.kafka.common.serialization.IntegerDeserializer");
    config.put("group.id", "dodex_events");
    config.put("auto.offset.reset", "earliest");
    config.put("enable.auto.commit", "false");


    KafkaConsumer<String, Integer> consumer =
        KafkaConsumer.create(Server.getRxVertx(), config);

    consumer.handler(record -> {
      if (logger.isDebugEnabled()) {
        logger.info("Processing key={}, value={}, partition={}, offset={}", record.key(),
            record.value(), record.partition(), record.offset());
      }
      Timestamp timestamp = new Timestamp(record.timestamp());
      dodexEventData.add(new DodexEventData(record.key(), topic, record.value(), timestamp,
          record.partition(), record.offset()));
    });
    consumer.subscribe(topic);
  }

  public String list(String command, String init) throws JsonProcessingException {
    String json = objectMapper.writeValueAsString(dodexEventData);

    switch (command) {
      case "list":
        return json;
      default:
        break;
    }
    return json;
  }

  private void setRetention(String value) {
    Map<String, String> props = new HashMap<>();
    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

    KafkaAdminClient adminClient = KafkaAdminClient.create(Server.getRxVertx().getDelegate(), props);
    ConfigResource resource =
        new ConfigResource(org.apache.kafka.common.config.ConfigResource.Type.TOPIC, topic);

    adminClient.describeTopics(Collections.singletonList(topic)).onSuccess(topics -> {
      if (logger.isDebugEnabled()) {
        TopicDescription topicDescription = topics.get(topic);

        logger.info(String.format("Topic name=%s isInternal=%s partitions= %s",
            topicDescription.getName(), topicDescription.isInternal(),
            topicDescription.getPartitions().size()));

        for (TopicPartitionInfo topicPartitionInfo : topicDescription.getPartitions()) {
          logger.info(String.format("Partition id=%s leaderId= %s replicas= %s isr= %s",
              topicPartitionInfo.getPartition(),
              topicPartitionInfo.getLeader().getId(),
              topicPartitionInfo.getReplicas(), topicPartitionInfo.getIsr()));
        }
      }
    }).onFailure(err -> {
      err.printStackTrace();
    }).succeeded();


    ConfigEntry retentionEntry = new ConfigEntry(TopicConfig.RETENTION_MS_CONFIG, value);
    Map<ConfigResource, Config> updateConfig = new HashMap<>();
    Config newConfig = new Config(Collections.singletonList(retentionEntry));
    updateConfig.put(resource, newConfig);
    adminClient.alterConfigs(updateConfig).onSuccess(v -> {
      if (logger.isDebugEnabled()) {
        logger.debug(String.format("Kafka retention configured to: %s millisconds", value));
      }
    }).onFailure(Throwable::printStackTrace);
  }
}
