package nl.sense_os.datastorageengine.realm;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;


public class RealmDataDeletionRequest extends RealmObject {
    @PrimaryKey
    private String uuid = null;
    private String userId = null;
    private String sensorName = null;
    @Index
    private String sourceName = null;
    private Long startTime = null;
    private Long endTime = null;

    public RealmDataDeletionRequest() {}

    public RealmDataDeletionRequest(String userId, String sourceName, String sensorName, long startTime, long endTime) {
        this.uuid = UUID.randomUUID().toString();
        this.userId = userId;
        this.sourceName = sourceName;
        this.sensorName = sensorName;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getUuid() { return uuid; }

    public void setUuid(String uuid) { this.uuid = uuid; }

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
