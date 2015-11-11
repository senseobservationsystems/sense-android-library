package nl.sense_os.datastorageengine.realm;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import nl.sense_os.datastorageengine.DataDeletionRequest;


public class RealmDataDeletionRequest extends RealmObject {
    @PrimaryKey
    private String id = null; // A UUID
    private String userId = null;
    private String sensorName = null;
    @Index
    private String sourceName = null;
    private Long startTime = null;
    private Long endTime = null;

    public RealmDataDeletionRequest() {}

    public RealmDataDeletionRequest(String id, String userId, String sourceName, String sensorName, Long startTime, Long endTime) {
        this.setId(id);
        this.setUserId(userId);
        this.setSourceName(sourceName);
        this.setSensorName(sensorName);
        this.setStartTime(startTime);
        this.setEndTime(endTime);
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

    public static RealmDataDeletionRequest fromDataDeletionRequest(DataDeletionRequest request) {
        return new RealmDataDeletionRequest(
                request.getId(),
                request.getUserId(),
                request.getSourceName(),
                request.getSensorName(),
                request.getStartTime(),
                request.getEndTime());
    }

    public static DataDeletionRequest toDataDeletionRequest(RealmDataDeletionRequest realmRequest) {
        return new DataDeletionRequest(
                realmRequest.getId(),
                realmRequest.getUserId(),
                realmRequest.getSourceName(),
                realmRequest.getSensorName(),
                realmRequest.getStartTime(),
                realmRequest.getEndTime());
    }
}
