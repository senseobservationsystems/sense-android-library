package nl.sense_os.datastorageengine;

import java.util.UUID;


public class DataDeletionRequest {
    private String id = null;
    private String userId = null;
    private String sensorName = null;
    private String sourceName = null;
    private Long startTime = null;
    private Long endTime = null;

    public DataDeletionRequest(String userId, String sourceName, String sensorName, Long startTime, Long endTime) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.sourceName = sourceName;
        this.sensorName = sensorName;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public DataDeletionRequest(String uuid, String userId, String sourceName, String sensorName, Long startTime, Long endTime) {
        this.id = uuid;
        this.userId = userId;
        this.sourceName = sourceName;
        this.sensorName = sensorName;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSensorName() { return sensorName; }

    public void setSensorName(String sensorName) { this.sensorName = sensorName; }

    public String getSourceName() { return sourceName; }

    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    public Long getStartTime() { return startTime; }

    public void setStartTime(Long startTime) { this.startTime = startTime; }

    public Long getEndTime() { return endTime; }

    public void setEndTime(Long endTime) { this.endTime = endTime; }
}
