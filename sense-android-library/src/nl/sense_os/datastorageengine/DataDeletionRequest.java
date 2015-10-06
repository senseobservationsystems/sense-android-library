package nl.sense_os.datastorageengine;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;


public class DataDeletionRequest extends RealmObject {
    @PrimaryKey
    private String uuid = null;
    private String userId = null;
    private String sensorName = null;
    @Index
    private String sourceName = null;
    private long startDate =  -1;
    private long endDate = -1;

    public DataDeletionRequest() {}

    public DataDeletionRequest(String userId, String sensorName, String sourceName, long startDate, long endDate) {
        this.uuid = UUID.randomUUID().toString();
        this.userId = userId;
        this.sensorName = sensorName;
        this.sourceName = sourceName;
        this.startDate = startDate;
        this.endDate = endDate;
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

    public long getStartDate() { return startDate; }

    public void setStartDate(long startDate) { this.startDate = startDate; }

    public long getEndDate() { return endDate; }

    public void setEndDate(long endDate) { this.endDate = endDate; }
}
