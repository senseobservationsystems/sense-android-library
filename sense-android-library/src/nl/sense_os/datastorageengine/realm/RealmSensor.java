package nl.sense_os.datastorageengine.realm;

import org.json.JSONException;
import org.json.JSONObject;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import nl.sense_os.service.shared.SensorDataPoint;

public class RealmSensor extends RealmObject {

    @PrimaryKey
    private String id = null;

    private String name = null;
    private String meta = null;  // Stringified JSON object
    private boolean csUploadEnabled = false;
    private boolean csDownloadEnabled = false;
    private boolean persistLocally = true;
    private String userId = null;
    private String sourceId = null;
    private String dataType = null;  // String value of the enum SensorDataPoint.DataType
    private String csId = null;
    private boolean synced = false;

    public RealmSensor () {}

    public RealmSensor(String id, String name, JSONObject meta, boolean csUploadEnabled, boolean csDownloadEnabled, boolean persistLocally, String userId, String sourceId, SensorDataPoint.DataType dataType, String csId, boolean synced) {
        this.id = id;
        this.name = name;
        setMeta(meta);
        this.csUploadEnabled = csUploadEnabled;
        this.csDownloadEnabled = csDownloadEnabled;
        this.persistLocally = persistLocally;
        this.userId = userId;
        this.sourceId = sourceId;
        setDataType(dataType);
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

    public JSONObject getMeta() throws JSONException {
        return meta != null ? new JSONObject(meta) : null;
    }

    public void setMeta(JSONObject meta) {
        this.meta = meta != null ? meta.toString() : null;
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
        return SensorDataPoint.DataType.valueOf(dataType);
    }

    public void setDataType(SensorDataPoint.DataType dataType) {
        this.dataType = dataType.name();
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
