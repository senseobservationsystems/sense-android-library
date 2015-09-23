package nl.sense_os.datastorageengine;

import org.json.JSONException;
import org.json.JSONObject;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import nl.sense_os.service.shared.SensorDataPoint.DataType;

public class RealmModelSensor extends RealmObject {

    @PrimaryKey
    private String id = null;

    @Index
    private String name = null;
    private String meta = null;  // Stringified JSON object
    private boolean csUploadEnabled = false;
    private boolean csDownloadEnabled = false;
    private boolean persistLocally = true;
    private String userId = null;

    @Index
    private String sourceId = null;
    private String dataType = null;  // String value of the enum SensorDataPoint.DataType
    private String csId = null;
    private boolean synced = false;

    public RealmModelSensor() {}

    public RealmModelSensor(String id, String name, String meta, boolean csUploadEnabled, boolean csDownloadEnabled, boolean persistLocally, String userId, String sourceId, String dataType, String csId, boolean synced) {
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

    /**
     * Returns the meta information of a RealmSensor as JSONObject
     * @param realmSensor
     * @return Returns JSONObject with meta information
     */
    public static JSONObject getMeta (RealmModelSensor realmSensor) throws JSONException {
        String meta = realmSensor.getMeta();
        return meta != null ? new JSONObject(meta) : null;
    }

    /**
     * Returns meta information as a stringified JSON object. Deserialize the string like:
     *
     *     RealmSensor.getMeta(sensor);
     *
     * @return stringified JSON object
     */
    public String getMeta() {
        return meta;
    }

    /**
     * Set meta information for this sensor
     * @param meta Must contain a stringified JSON object.
     */
    public void setMeta(String meta) {
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

    /**
     * Returns the string name of the data type.
     * This is an entry from the enum SensorDataPoint.DataType
     * @return
     */
    public String getDataType() {
        return dataType;
    }

    /**
     * Set the sensor data type.
     * @param dataType Must be a the name of one of the enums SensorDataPoint.DataType
     */
    public void setDataType(String dataType) {
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

    /**
     * Convert a RealmSensor into a Sensor
     * @param realm
     * @param realmSensor
     * @return Returns a Sensor
     */
    public static Sensor toSensor (Realm realm, RealmModelSensor realmSensor) throws JSONException {
        String meta = realmSensor.getMeta();
        String dataType = realmSensor.getDataType();

        return new RealmSensor(
                realm,
                realmSensor.getId(),
                realmSensor.getName(),
                realmSensor.getUserId(),
                realmSensor.getSourceId(),
                dataType != null ? DataType.valueOf(dataType) : null,
                realmSensor.getCsId(),
                new SensorOptions(
                        meta != null ? new JSONObject(meta) : null,
                        realmSensor.isCsUploadEnabled(),
                        realmSensor.isCsDownloadEnabled(),
                        realmSensor.isPersistLocally()
                ),
                realmSensor.isSynced()
        );
    }

    /**
     * Create a RealmSensor from a Sensor
     * @param sensor  A Sensor
     * @return Returns a RealmSensor
     */
    public static RealmModelSensor fromSensor (Sensor sensor) {
        DataType dataType = sensor.getDataType();
        SensorOptions options = sensor.getOptions();
        JSONObject meta = options != null ? options.getMeta() : null;

        return new RealmModelSensor(
                sensor.getId(),
                sensor.getName(),
                meta != null ? meta.toString() : null,
                options != null ? options.isUploadEnabled() : null,
                options != null ? options.isDownloadEnabled() : null,
                options != null ? options.isPersist() : null,
                sensor.getUserId(),
                sensor.getSourceId(),
                dataType != null ? dataType.name() : null,
                sensor.getCsId(),
                sensor.isSynced()
        );
    }

}
