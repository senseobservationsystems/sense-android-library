package nl.sense_os.datastorageengine.realm.model;

import org.json.JSONException;
import org.json.JSONObject;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import nl.sense_os.datastorageengine.Sensor;
import nl.sense_os.datastorageengine.SensorException;
import nl.sense_os.datastorageengine.SensorOptions;
import nl.sense_os.datastorageengine.realm.RealmSensor;
import nl.sense_os.service.shared.SensorDataPoint.DataType;

public class RealmModelSensor extends RealmObject {

    // compoundKey is used purely for Realm, to prevent duplicate sensors.
    // it's built up from the sensors `name`, `source` and `userId`.
    @PrimaryKey
    private String compoundKey = null;

    @Index
    private long id = -1; // only used to locally keep a relation between Sensor and DataPoints

    @Index
    private String name = null;
    private String meta = null;  // Stringified JSON object
    private boolean csUploadEnabled = false;
    private boolean csDownloadEnabled = false;
    private boolean persistLocally = true;
    private String userId = null;

    @Index
    private String source = null;
    private boolean csDataPointsDownloaded = false;

    public RealmModelSensor() {}

    public RealmModelSensor(long id, String name, String meta, boolean csUploadEnabled, boolean csDownloadEnabled, boolean persistLocally, String userId, String source, boolean csDataPointsDownloaded) {
        this.compoundKey = getCompoundKey(name, source, userId);

        this.id = id;
        this.name = name;
        this.meta = meta;
        this.csUploadEnabled = csUploadEnabled;
        this.csDownloadEnabled = csDownloadEnabled;
        this.persistLocally = persistLocally;
        this.userId = userId;
        this.source = source;
        this.csDataPointsDownloaded = csDataPointsDownloaded;
    }

    public String getCompoundKey() {
        return compoundKey;
    }

    public void setCompoundKey(String compoundKey) {
        this.compoundKey = compoundKey;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.compoundKey = getCompoundKey(name, source, userId);
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
        this.compoundKey = getCompoundKey(name, source, userId);
    }

    public boolean isCsDataPointsDownloaded() {
        return csDataPointsDownloaded;
    }

    public void setCsDataPointsDownloaded(boolean csDataPointsDownloaded) {
        this.csDataPointsDownloaded = csDataPointsDownloaded;
    }

    /**
     * Create a unique id for a Sensor, consisting of the source and name.
     * @param source
     * @param name
     * @return Returns a string "<name>:<source>:<userId>"
     */
    public static String getCompoundKey (String name, String source, String userId) {
        return name + ":" + source + ":" + userId;
    }

    /**
     * Convert a RealmSensor into a Sensor
     * @param realm
     * @param realmSensor
     * @return Returns a Sensor
     */
    public static Sensor toSensor (Realm realm, RealmModelSensor realmSensor) throws JSONException, SensorException {
        String meta = realmSensor.getMeta();

        return new RealmSensor(
                realm,
                realmSensor.getId(),
                realmSensor.getName(),
                realmSensor.getUserId(),
                realmSensor.getSource(),
                new SensorOptions(
                        meta != null ? new JSONObject(meta) : null,
                        realmSensor.isCsUploadEnabled(),
                        realmSensor.isCsDownloadEnabled(),
                        realmSensor.isPersistLocally()
                ),
                realmSensor.isCsDataPointsDownloaded()
        );
    }

    /**
     * Create a RealmSensor from a Sensor
     * @param sensor  A Sensor
     * @return Returns a RealmSensor
     */
    public static RealmModelSensor fromSensor (Sensor sensor) {
        SensorOptions options = sensor.getOptions();
        JSONObject meta = options != null ? options.getMeta() : null;

        return new RealmModelSensor(
                sensor.getId(),
                sensor.getName(),
                meta != null ? meta.toString() : null,
                options != null ? options.isUploadEnabled() : null,
                options != null ? options.isDownloadEnabled() : null,
                options != null ? options.isPersistLocally() : null,
                sensor.getUserId(),
                sensor.getSource(),
                sensor.isCsDataPointsDownloaded()
        );
    }

}
