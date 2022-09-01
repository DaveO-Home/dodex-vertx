package dmo.fs.kafka;

import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.TimeZone;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dmo.fs.router.Routes;
import dmo.fs.utils.ColorUtilConstants;
import io.reactivex.rxjava3.core.Emitter;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.kafka.client.producer.KafkaProducerRecord;

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
            logger.info(String.format("Emitting data: %s--%s--%s--%s--%s", currentMilli, key, partition, dodexEventsTopic, value));
        }

        KafkaProducerRecord<String, Integer> record =
            KafkaProducerRecord.create(dodexEventsTopic, key, value, currentMilli, partition);

        Routes.getProducer().rxSend(record).doOnSuccess(recordMetadata -> {
            if (logger.isDebugEnabled()) {
                logger.info(String.format(
                    "Message %s written on topic=%s, partition=%s, offset=%s", record.value(), recordMetadata.getTopic(),
                        recordMetadata.getPartition(), recordMetadata.getOffset())
                );
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
                in.close();
            }
            jsonObject = JsonObject.mapFrom(node);

            final Optional<Integer> optionalEventsPartitions = Optional.ofNullable(jsonObject.getInteger("dodex.events.partitions"));
            if (optionalEventsPartitions.isPresent()) {
                dodexEventsPartitions = optionalEventsPartitions.get();
            }
            final Optional<String> optionalEventsTopic = Optional.ofNullable(jsonObject.getString("dodex.events.topic"));
            if (optionalEventsTopic.isPresent()) {
                dodexEventsTopic = optionalEventsTopic.get();
            }
            final Optional<Integer> optionalMessageLimit = Optional.ofNullable(jsonObject.getInteger("dodex.events.limit"));
            if (optionalMessageLimit.isPresent()) {
                messageLimit = optionalMessageLimit.get();
            }
            final Optional<Boolean> optionalRemoveMessages = Optional.ofNullable(jsonObject.getBoolean("dodex.events.remove"));
            if (optionalRemoveMessages.isPresent()) {
                removeMessages = optionalRemoveMessages.get();
            }
        } catch (final Exception exception) {
            logger.info(String.format("%sContext Configuration failed...%s%s", ColorUtilConstants.RED_BOLD_BRIGHT,
                    exception.getMessage(), ColorUtilConstants.RESET));
            exception.printStackTrace();
        }
    }

    public static Integer getMessageLimit() {
        return messageLimit;
    }

    public static Boolean getRemoveMessages() {
        return removeMessages;
    }
}
