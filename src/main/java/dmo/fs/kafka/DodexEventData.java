package dmo.fs.kafka;

import java.sql.Timestamp;

public class DodexEventData {
    String key;
    String topic;
    Integer payload;
    Timestamp timestamp;
    Integer partition;
    Long offset;
    

    public DodexEventData() {
        //
    }

    public DodexEventData(String key, String topic, Integer payload, Timestamp timestamp, Integer partition,
            Long offset) {
        this.key = key;
        this.topic = topic;
        this.payload = payload;
        this.timestamp = timestamp;
        this.partition = partition;
        this.offset = offset;
    }

    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }
    public String getTopic() {
        return topic;
    }
    public void setTopic(String topic) {
        this.topic = topic;
    }
    public Integer getPayload() {
        return payload;
    }
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
    public Timestamp getTimestamp() {
        return timestamp;
    }
    public void setPayload(Integer payload) {
        this.payload = payload;
    }

    public Integer getPartition() {
        return partition;
    }

    public void setPartition(Integer partition) {
        this.partition = partition;
    }

    public Long getOffset() {
        return offset;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
    }

    @Override
    public String toString() {
        return "DodexEventData [key=" + key + ", offset=" + offset + ", partition=" + partition + ", payload=" + payload
                + ", timestamp=" + timestamp + ", topic=" + topic + "]";
    }
}
    