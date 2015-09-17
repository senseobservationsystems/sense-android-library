package nl.sense_os.datastorageengine.realm;

import org.json.JSONException;
import org.json.JSONObject;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import nl.sense_os.service.shared.SensorDataPoint;

public class RealmDataPoint extends RealmObject {
    @Index
    private String sensorId = null;

    private String type = null;  // String name of the enum SensorDataPoint.DataType
    private String value = null; // Stringified JSONObject of the value

    @PrimaryKey
    private long date = 0;

    private boolean synced = false;

    public RealmDataPoint () {}

    public RealmDataPoint(String sensorId, boolean value, long date) {
        this.sensorId = sensorId;
        this.type = SensorDataPoint.DataType.BOOL.name();
        this.value = Boolean.toString(value);
        this.date = date;
    }

    public RealmDataPoint(String sensorId, float value, long date) {
        this.sensorId = sensorId;
        this.type = SensorDataPoint.DataType.FLOAT.name();
        this.value = Float.toString(value);
        this.date = date;
    }

    public RealmDataPoint(String sensorId, int value, long date) {
        this.sensorId = sensorId;
        this.type = SensorDataPoint.DataType.INT.name();
        this.value = Integer.toString(value);
        this.date = date;
    }

    public RealmDataPoint(String sensorId, JSONObject value, long date) {
        this.sensorId = sensorId;
        this.type = SensorDataPoint.DataType.JSON.name();
        this.value = value.toString();
        this.date = date;
    }

    public RealmDataPoint(String sensorId, String value, long date) {
        this.sensorId = sensorId;
        this.type = SensorDataPoint.DataType.STRING.name();
        this.value = value;
        this.date = date;
    }

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public static boolean getBooleanValue(RealmDataPoint dataPoint) {
        if (SensorDataPoint.DataType.BOOL.name().equals(dataPoint.getType())) {
            return Boolean.parseBoolean(dataPoint.getValue());
        }
        else {
            throw new ClassCastException("DataPoint does not contain a boolean value.");
        }
    }

    public static float getFloatValue(RealmDataPoint dataPoint) {
        if (SensorDataPoint.DataType.FLOAT.name().equals(dataPoint.getType())) {
            return Float.parseFloat(dataPoint.getValue());
        }
        else {
            throw new ClassCastException("DataPoint does not contain a float value.");
        }
    }

    public static int getIntValue(RealmDataPoint dataPoint) {
        if (SensorDataPoint.DataType.INT.name().equals(dataPoint.getType())) {
            return Integer.parseInt(dataPoint.getValue());
        }
        else {
            throw new ClassCastException("DataPoint does not contain an int value.");
        }
    }

    public static JSONObject getJSONValue(RealmDataPoint dataPoint) throws JSONException {
        if (SensorDataPoint.DataType.JSON.name().equals(dataPoint.getType())) {
            return new JSONObject(dataPoint.getValue());
        }
        else {
            throw new ClassCastException("DataPoint does not contain a JSON value.");
        }
    }

    public static String getStringValue(RealmDataPoint dataPoint) {
        if (SensorDataPoint.DataType.STRING.name().equals(dataPoint.getType())) {
            return dataPoint.getValue();
        }
        else {
            throw new ClassCastException("DataPoint does not contain a string value.");
        }
    }

    /**
     * Get a String representation of the value of the DataPoint
     * In order to cast the value of the data point into the correct type, use the static
     * functions getIntValue, getStringValue, etc:
     *
     *     RealmDataPoint dataPoint = new RealmDataPoint("1", "123", "the value", new Date().getTime());
     *     String type = dataPoint.getType();                       // "STRING"
     *     String value = RealmDataPoint.getStringValue(dataPoint); // "the value"
     *
     * @return Returns a string representation of the value
     */
    public String getValue() {
        return this.value;
    }

    /**
     * Set the string representation of a value. The type of the datapoint should be set accordingly
     * @param value
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

    public void setType(SensorDataPoint.DataType type) {
        this.type = type.name();
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType () {
        return this.type;
    }
}
