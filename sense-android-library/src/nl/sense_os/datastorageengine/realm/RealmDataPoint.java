package nl.sense_os.datastorageengine.realm;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;
import nl.sense_os.service.shared.SensorDataPoint;

public class RealmDataPoint extends RealmObject {
    @PrimaryKey
    private String id = null;
    private String sensorId = null;
    private String type = null;  // String name of the enum SensorDataPoint.DataType
    private String value = null; // Stringified JSONObject of the value
    private long date = 0;
    private boolean synced = false;

    public RealmDataPoint () {}

    public RealmDataPoint(String id, String sensorId, boolean value, long date, boolean synced) {
        this.id = id;
        this.sensorId = sensorId;
        setValue(value);
        this.date = date;
        this.synced = synced;
    }

    public RealmDataPoint(String id, String sensorId, double value, long date, boolean synced) {
        this.id = id;
        this.sensorId = sensorId;
        setValue(value);
        this.date = date;
        this.synced = synced;
    }

    public RealmDataPoint(String id, String sensorId, float value, long date, boolean synced) {
        this.id = id;
        this.sensorId = sensorId;
        setValue(value);
        this.date = date;
        this.synced = synced;
    }

    public RealmDataPoint(String id, String sensorId, int value, long date, boolean synced) {
        this.id = id;
        this.sensorId = sensorId;
        setValue(value);
        this.date = date;
        this.synced = synced;
    }

    public RealmDataPoint(String id, String sensorId, JSONObject value, long date, boolean synced) {
        this.id = id;
        this.sensorId = sensorId;
        setValue(value);
        this.date = date;
        this.synced = synced;
    }

    public RealmDataPoint(String id, String sensorId, String value, long date, boolean synced) {
        this.id = id;
        this.sensorId = sensorId;
        setValue(value);
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

    public boolean getBooleanValue() {
        if (SensorDataPoint.DataType.BOOL.name().equals(this.type)) {
            return Boolean.parseBoolean(this.value);
        }
        else {
            throw new ClassCastException("DataPoint does not contain a boolean value.");
        }
    }

    public double getDoubleValue() {
        if (SensorDataPoint.DataType.DOUBLE.name().equals(this.type)) {
            return Double.parseDouble(this.value);
        }
        else {
            throw new ClassCastException("DataPoint does not contain a double value.");
        }
    }

    public float getFloatValue() {
        if (SensorDataPoint.DataType.FLOAT.name().equals(this.type)) {
            return Float.parseFloat(this.value);
        }
        else {
            throw new ClassCastException("DataPoint does not contain a float value.");
        }
    }

    public int getIntValue() {
        if (SensorDataPoint.DataType.INT.name().equals(this.type)) {
            return Integer.parseInt(this.value);
        }
        else {
            throw new ClassCastException("DataPoint does not contain an int value.");
        }
    }

    public JSONObject getJSONValue() throws JSONException {
        if (SensorDataPoint.DataType.JSON.name().equals(this.type)) {
            return new JSONObject(this.value);
        }
        else {
            throw new ClassCastException("DataPoint does not contain a JSON value.");
        }
    }

    public String getStringValue() {
        if (SensorDataPoint.DataType.STRING.name().equals(this.type)) {
            return this.value;
        }
        else {
            throw new ClassCastException("DataPoint does not contain a string value.");
        }
    }

    /**
     * Get a String representation of the value of the DataPoint
     * @return
     */
    public String getValue() {
        return this.value;
    }

    public void setValue(boolean value) {
        this.type = SensorDataPoint.DataType.BOOL.name();
        this.value = Boolean.toString(value);
    }

    public void setValue(double value) {
        this.type = SensorDataPoint.DataType.DOUBLE.name();
        this.value = Double.toString(value);
    }

    public void setValue(float value) {
        this.type = SensorDataPoint.DataType.FLOAT.name();
        this.value = Float.toString(value);
    }

    public void setValue(int value) {
        this.type = SensorDataPoint.DataType.INT.name();
        this.value = Integer.toString(value);
    }

    public void setValue(JSONObject value) {
        this.type = SensorDataPoint.DataType.JSON.name();
        this.value = value.toString();
    }

    public void setValue(String value) {
        this.type = SensorDataPoint.DataType.STRING.name();
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

    public SensorDataPoint.DataType getType () {
        return SensorDataPoint.DataType.valueOf(type);
    }
}
