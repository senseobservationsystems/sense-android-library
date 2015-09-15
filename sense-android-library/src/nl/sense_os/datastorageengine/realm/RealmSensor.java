package nl.sense_os.datastorageengine.realm;

import org.json.JSONObject;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import nl.sense_os.service.shared.SensorDataPoint;

public class RealmSensor extends RealmObject {

    @PrimaryKey
    private String id = null;

    private String name = null;
    private JSONObject meta = null;
    private boolean csUploadEnabled = false;
    private boolean csDownloadEnabled = false;
    private boolean persistLocally = true;
    private String userId = null;
    private String sourceId = null;
    private SensorDataPoint.DataType dataType = null;
    private String csId = null;
    private boolean synced = false;

    public RealmSensor(String id, String name, JSONObject meta, boolean csUploadEnabled, boolean csDownloadEnabled, boolean persistLocally, String userId, String sourceId, SensorDataPoint.DataType dataType, String csId, boolean synced) {
        this.id = id;
        this.name = name;
        this.meta = meta;
        this.csUploadEnabled = csUploadEnabled;
        this.csDownloadEnabled = csDownloadEnabled;
        this.persistLocally = persistLocally;
        this.userId = userId;
        this.sourceId = sourceId;
        this.dataType = dataType;
        this.csId = csId;
        this.synced = synced;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JSONObject getMeta() {
        return meta;
    }

    public void setMeta(JSONObject meta) {
        this.meta = meta;
    }

    public boolean isCsUploadEnabled() {
        return csUploadEnabled;
    }

    public void setCsUploadEnabled(boolean csUploadEnabled) {
        this.csUploadEnabled = csUploadEnabled;
    }

    public boolean isCsDownloadEnabled() {
        return csDownloadEnabled;
    }

    public void setCsDownloadEnabled(boolean csDownloadEnabled) {
        this.csDownloadEnabled = csDownloadEnabled;
    }

    public boolean isPersistLocally() {
        return persistLocally;
    }

    public void setPersistLocally(boolean persistLocally) {
        this.persistLocally = persistLocally;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public SensorDataPoint.DataType getdataType() {
        return dataType;
    }

    public void setDataType(SensorDataPoint.DataType dataType) {
        this.dataType = dataType;
    }

    public String getCsId() {
        return csId;
    }

    public void setCsId(String csId) {
        this.csId = csId;
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }
}
