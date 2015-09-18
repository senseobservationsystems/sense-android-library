package nl.sense_os.datastorageengine;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import nl.sense_os.service.shared.SensorDataPoint.DataType;

/**
 * A DataPoint can hold a single data point of for a sensor: a date and a value.
 */
public class DataPoint {

    private String sensorId = null;
    private String type;  // string name of enum SensorDataPoint.DataType
    private String value;
    private long date = 0;
    private boolean synced = false;

    public DataPoint(String sensorId, String type, String value, long date, boolean synced) {
        this.sensorId = sensorId;
        this.type = type;
        this.value = value;
        this.date = date;
        this.synced = synced;
    }

    /**
     * Construct a DataPoint from a JSONObject.
     * @param obj
     */
    public DataPoint(JSONObject obj) throws JSONException {
        this.sensorId   = obj.getString("sensorId");
        this.type       = obj.getString("type");
        this.value      = stringifyValue(obj.opt("value"));
        this.date       = obj.getLong("date");
        this.synced     = obj.optBoolean("synced", false);
    }

    public DataPoint(String sensorId, Integer value, long date) {
        setSensorId(sensorId);
        setValue(value);
        setDate(date);
    }

    public DataPoint(String sensorId, Float value, long date) {
        setSensorId(sensorId);
        setValue(value);
        setDate(date);
    }

    public DataPoint(String sensorId, String value, long date) {
        setSensorId(sensorId);
        setValue(value);
        setDate(date);
    }

    public DataPoint(String sensorId, Boolean value, long date) {
        setSensorId(sensorId);
        setValue(value);
        setDate(date);
    }

    public DataPoint(String sensorId, JSONObject value, long date) {
        setSensorId(sensorId);
        setValue(value);
        setDate(date);
    }

    public DataPoint(String sensorId, Integer value, long date, boolean synced) {
        this(sensorId, value, date);
        setSynced(synced);
    }

    public DataPoint(String sensorId, Float value, long date, boolean synced) {
        this(sensorId, value, date);
        setSynced(synced);
    }

    public DataPoint(String sensorId, String value, long date, boolean synced) {
        this(sensorId, value, date);
        setSynced(synced);
    }

    public DataPoint(String sensorId, Boolean value, long date, boolean synced) {
        this(sensorId, value, date);
        setSynced(synced);
    }

    public DataPoint(String sensorId, JSONObject value, long date, boolean synced) {
        this(sensorId, value, date);
        setSynced(synced);
    }

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public Boolean getBooleanValue() {
        if (DataType.BOOL.name().equals(this.type)) {
            return Boolean.parseBoolean(this.value);
        }
        else {
            throw new ClassCastException("DataPoint does not contain a boolean value.");
        }
    }

    public Float getFloatValue() {
        if (DataType.FLOAT.name().equals(this.type)) {
            return Float.parseFloat(this.value);
        }
        else {
            throw new ClassCastException("DataPoint does not contain a float value.");
        }
    }

    public Integer getIntegerValue() {
        if (DataType.INT.name().equals(this.type)) {
            return Integer.parseInt(this.value);
        }
        else {
            throw new ClassCastException("DataPoint does not contain an int value.");
        }
    }

    public JSONObject getJSONValue() throws JSONException {
        if (DataType.JSON.name().equals(this.type)) {
            return new JSONObject(this.value);
        }
        else {
            throw new ClassCastException("DataPoint does not contain a JSON value.");
        }
    }

    public String getStringValue() {
        if (DataType.STRING.name().equals(this.type)) {
            try {
                return unquote(this.value);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }
        else {
            throw new ClassCastException("DataPoint does not contain a string value.");
        }
    }

    public DataType getType() {
        return this.type != null ? DataType.valueOf(this.type) : null;
    }

    /**
     * Get a String representation of the value of the DataPoint
     * In order to cast the value of the data point into the correct type, use the static
     * functions getIntValue, getStringValue, etc:
     *
     *     DataPoint dataPoint = new DataPoint("sensor-xyz", "the value", new Date().getTime());
     *     DataType type = dataPoint.getType();                     // "STRING"
     *     String value = dataPoint.getStringValue();               // "\"the value\""
     *
     * @return Returns a string representation of the value
     */
    public String getStringifiedValue() {
        return this.value;
    }

    public void setValue(boolean value) {
        this.type = DataType.BOOL.name();
        this.value = Boolean.toString(value);
    }

    public void setValue(float value) {
        this.type = DataType.FLOAT.name();
        this.value = Float.toString(value);
    }

    public void setValue(int value) {
        this.type = DataType.INT.name();
        this.value = Integer.toString(value);
    }

    public void setValue(JSONObject value) {
        this.type = DataType.JSON.name();
        this.value = value.toString();
    }

    public void setValue(String value) {
        this.type = DataType.STRING.name();
        this.value = quote(value);
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

    /**
     * @return Returns a JSON representation of the DataPoint.
     */
    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();

        try {
            obj.put("sensorId", sensorId);
            obj.put("type",     type);
            obj.put("value",    parseValue(value));
            obj.put("date",     date);
            obj.put("synced",   synced);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj;
    };

    /**
     * @return Returns a string representation of the DataPoint, a stringified JSON object.
     */
    public String toString() {
      return toJSON().toString();
    };

    /**
     * Quote a string and escape quotes.
     * @param str
     * @return Quoted and escaped string.
     */
    public static String quote(String str) {
        return JSONObject.quote(str);
    };

    /**
     * Unquote a quoted string, and unescape escaped characters.
     * @param quotedStr
     * @return
     */
    public static String unquote(String quotedStr) throws JSONException {
        return (String) parseValue(quotedStr);
    };

    /**
     * Stringify a value, like a Float, String, Int, etc.
     * @param obj
     * @return
     */
    public static String stringifyValue(Object obj) {
        if (obj == null) {
            return null;
        }
        else if (obj instanceof String) {
            return quote((String)obj);
        }
        else {
            return String.valueOf(obj);
        }
    };

    /**
     * Parse a stringified value (String, Float, Int, ...)
     * @param str
     * @return
     */
    public static Object parseValue(String str) throws JSONException {
        return new JSONTokener(str).nextValue();
    };

}
