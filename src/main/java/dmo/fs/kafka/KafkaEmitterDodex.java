package dmo.fs.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dmo.fs.router.Routes;
import dmo.fs.utils.ColorUtilConstants;
import io.reactivex.rxjava3.core.Emitter;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.kafka.client.producer.KafkaProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.TimeZone;

public class KafkaEmitterDodex {
  private static final Logger logger = LoggerFactory.getLogger(KafkaEmitterDodex.class.getName());
  private static int toggle;
  private Integer dodexEventsPartitions = 2;
  private String dodexEventsTopic = "dodex-events";
  private static Boolean removeMessages = false;
  private static Integer messageLimit = 25;

  Emitter<Integer> countEmitter;

  {
    setConfig();
  }

  public void setValue(Integer value) {
    setValue("broadcast", value);
  }

  public void setValue(String key, Integer value) {
    if (value == null) {
      value = 0;
    }
    int partition = toggle++ % dodexEventsPartitions;
    if (toggle == dodexEventsPartitions) {
      toggle = 0;
    }
    int offset = TimeZone.getDefault().getOffset(Instant.now().toEpochMilli());
    Long currentMilli = Instant.ofEpochMilli(System.currentTimeMillis() - -offset).toEpochMilli();

    if (logger.isDebugEnabled()) {
      logger.info("Emitting data: {} -- {}-- {} -- {} -- {}", currentMilli, key, partition, dodexEventsTopic, value);
    }

    KafkaProducerRecord<String, Integer> record =
        KafkaProducerRecord.create(dodexEventsTopic, key, value, currentMilli, partition);

    Routes.getProducer().rxSend(record).doOnSuccess(recordMetadata -> {
      if (logger.isDebugEnabled()) {
        logger.info("Message {} written on topic={}, partition={}, offset={}", record.value(), recordMetadata.getTopic(), recordMetadata.getPartition(), recordMetadata.getOffset());
      }
    }).doOnError(Throwable::printStackTrace).subscribe();
  }

  private void setConfig() {
    JsonObject jsonObject = null;
    ObjectMapper jsonMapper = new ObjectMapper();
    JsonNode node;
    try {
      try (InputStream in = getClass().getResourceAsStream("/application-conf.json")) {
        node = jsonMapper.readTree(in);
      }
      jsonObject = JsonObject.mapFrom(node);

      final Optional<Integer> optionalEventsPartitions = Optional.ofNullable(jsonObject.getInteger("dodex.events.partitions"));
      optionalEventsPartitions.ifPresent(integer -> dodexEventsPartitions = integer);
      final Optional<String> optionalEventsTopic = Optional.ofNullable(jsonObject.getString("dodex.events.topic"));
      optionalEventsTopic.ifPresent(s -> dodexEventsTopic = s);
      final Optional<Integer> optionalMessageLimit = Optional.ofNullable(jsonObject.getInteger("dodex.events.limit"));
      optionalMessageLimit.ifPresent(integer -> messageLimit = integer);
      final Optional<Boolean> optionalRemoveMessages = Optional.ofNullable(jsonObject.getBoolean("dodex.events.remove"));
      optionalRemoveMessages.ifPresent(aBoolean -> removeMessages = aBoolean);
      if (logger.isDebugEnabled()) {
        logger.info("Dodex Config Setup(parts/top/limit/remove): {} -- {} -- {} -- {}", dodexEventsPartitions, dodexEventsTopic, messageLimit, removeMessages);
      }
    } catch (final Exception e) {
      logger.info("{}Context Configuration failed...{}{}", ColorUtilConstants.RED_BOLD_BRIGHT,
          e.getMessage(), ColorUtilConstants.RESET);
      throw new RuntimeException(e);
    }
  }

  public static Integer getMessageLimit() {
    return messageLimit;
  }

  public static Boolean getRemoveMessages() {
    return removeMessages;
  }
}
