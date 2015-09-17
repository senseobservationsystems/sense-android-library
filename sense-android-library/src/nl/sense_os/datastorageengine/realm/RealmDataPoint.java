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
    @Index
    private String sensorId = null;

    private String type = null;  // String name of the enum SensorDataPoint.DataType
    private String value = null; // Stringified JSONObject of the value

    @PrimaryKey
    private long date = 0;

    private boolean synced = false;

    public RealmDataPoint () {}

    public RealmDataPoint(String sensorId, String type, String value, long date, boolean synced) {
        this.sensorId = sensorId;
        this.type = type;
        this.value = value;
        this.date = date;
        this.synced = synced;
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
     * Convert a RealmDataPoint into a DataPoint
     * @param realmDataPoint
     * @return Returns a DataPoint
     */
    public static DataPoint toDataPoint (RealmDataPoint realmDataPoint) throws ClassCastException, JSONException {
        // string
        if (DataType.STRING.name().equals(realmDataPoint.getType())) {
            return new DataPoint<>(
                    realmDataPoint.getSensorId(),
                    realmDataPoint.getValue(),
                    realmDataPoint.getDate(),
                    realmDataPoint.isSynced());
        }

        // integer
        if (DataType.INT.name().equals(realmDataPoint.getType())) {
            return new DataPoint<>(
                    realmDataPoint.getSensorId(),
                    Integer.parseInt(realmDataPoint.getValue()),
                    realmDataPoint.getDate());
        }

        // float
        if (DataType.FLOAT.name().equals(realmDataPoint.getType())) {
            return new DataPoint<>(
                    realmDataPoint.getSensorId(),
                    Float.parseFloat(realmDataPoint.getValue()),
                    realmDataPoint.getDate());
        }

        // boolean
        if (DataType.BOOL.name().equals(realmDataPoint.getType())) {
            return new DataPoint<> (
                    realmDataPoint.getSensorId(),
                    Boolean.parseBoolean(realmDataPoint.getValue()),
                    realmDataPoint.getDate());
        }

        // JSON object
        if (DataType.JSON.name().equals(realmDataPoint.getType())) {
            return new DataPoint<> (
                    realmDataPoint.getSensorId(),
                    new Object(),
                    realmDataPoint.getDate());
        }

        throw new ClassCastException("Unknown type of DataPoint");
    }

    /**
     * Create a RealmDataPoint from a DataPoint
     * @param dataPoint  A DataPoint
     * @return Returns a RealmDataPoint
     */
    public static <T> RealmDataPoint fromDataPoint (DataPoint<T> dataPoint) throws ClassCastException {

        Object value = dataPoint.getValue();

        // null
        if (value == null) {
            return new RealmDataPoint(
                    dataPoint.getSensorId(),
                    null,
                    null,
                    dataPoint.getDate(),
                    dataPoint.isSynced());
        }

        // string
        if (value instanceof String) {
            return new RealmDataPoint(
                    dataPoint.getSensorId(),
                    DataType.STRING.name(),
                    value.toString(),
                    dataPoint.getDate(),
                    dataPoint.isSynced());
        }

        // float
        if (value instanceof Float) {
            return new RealmDataPoint(
                    dataPoint.getSensorId(),
                    DataType.FLOAT.name(),
                    value.toString(),
                    dataPoint.getDate(),
                    dataPoint.isSynced());
        }

        // integer
        if (value instanceof Integer) {
            return new RealmDataPoint(
                    dataPoint.getSensorId(),
                    DataType.INT.name(),
                    value.toString(),
                    dataPoint.getDate(),
                    dataPoint.isSynced());
        }

        // boolean
        if (value instanceof Boolean) {
            return new RealmDataPoint(
                    dataPoint.getSensorId(),
                    DataType.BOOL.name(),
                    value.toString(),
                    dataPoint.getDate(),
                    dataPoint.isSynced());
        }

        // JSON object
        if (value instanceof JSONObject) {
            return new RealmDataPoint(
                    dataPoint.getSensorId(),
                    DataType.BOOL.name(),
                    value.toString(),
                    dataPoint.getDate(),
                    dataPoint.isSynced());
        }

        throw new ClassCastException("Unknown type of value in DataPoint");
    }

}
