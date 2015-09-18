package nl.sense_os.datastorageengine.realm;

import org.json.JSONException;
import org.json.JSONObject;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import nl.sense_os.datastorageengine.DataPoint;
import nl.sense_os.service.shared.SensorDataPoint.DataType;

/**
 * The class RealmDataPoint contains a serializable form of DataPoint, suited for storing
 * a DataPoint in Realm.
 *
 * Data values are stored in a string representation, together with a field `type` to be able
 * to restore the data type.
 *
 * The class contains two static helper functions to convert from and to DataPoint:
 *     RealmDataPoint.fromDataPoint()
 *     RealmDataPoint.toDataPoint()
 */
public class RealmDataPoint extends RealmObject {
    @PrimaryKey
    private String id = null; // purely used to identify the data point in Realm

    @Index
    private String sensorId = null;

    private String type = null;  // String name of the enum SensorDataPoint.DataType
    private String value = null; // Stringified JSONObject of the value

    @Index
    private long date = 0;

    private boolean synced = false;

    public RealmDataPoint () {}

    public RealmDataPoint(String id, String sensorId, String type, String value, long date, boolean synced) {
        this.sensorId = sensorId;
        this.type = type;
        this.value = value;
        this.date = date;
        this.synced = synced;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    /**
     * Get a String representation of the value of the RealmDataPoint
     * @return Returns a string representation of the value
     */
    public String getValue() {
        return this.value;
    }

    /**
     * Set the string representation of a value. The type of the data point should be set accordingly
     * @param value String representation of a value
     */
    public void setValue(String value) {
        this.value = value;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }

    public void setType(DataType type) {
        this.type = type.name();
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType () {
        return this.type;
    }

    /**
     * Build up a unique id for a DataPoint, consisting of the sensorId and the date.
     * @param sensorId
     * @param date
     * @return Returns a string "<sensorId>:<date>"
     */
    public static String buildId (String sensorId, long date) {
        return sensorId + ":" + date;
    }

    /**
     * Convert a RealmDataPoint into a DataPoint
     * @param realmDataPoint
     * @return Returns a DataPoint
     */
    public static DataPoint toDataPoint (RealmDataPoint realmDataPoint) {
        return new DataPoint(
                realmDataPoint.getSensorId(),
                realmDataPoint.getType(),
                realmDataPoint.getValue(),
                realmDataPoint.getDate(),
                realmDataPoint.isSynced());
    }

    /**
     * Create a RealmDataPoint from a DataPoint
     * @param dataPoint  A DataPoint
     * @return Returns a RealmDataPoint
     */
    public static RealmDataPoint fromDataPoint (DataPoint dataPoint) {
        String id = RealmDataPoint.buildId(dataPoint.getSensorId(), dataPoint.getDate());

        return new RealmDataPoint(
                id,
                dataPoint.getSensorId(),
                dataPoint.getType().name(),
                dataPoint.getStringifiedValue(),
                dataPoint.getDate(),
                dataPoint.isSynced());
    }

}
