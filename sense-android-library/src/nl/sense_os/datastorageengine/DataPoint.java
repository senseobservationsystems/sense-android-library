package nl.sense_os.datastorageengine;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A DataPoint can hold a single data point of for a sensor: a time and a value.
 */
public class DataPoint {

    private long sensorId = -1;
    private Object value = null;
    private long time = 0;
    private boolean existsInRemote = false;

    public DataPoint(long sensorId, Object value, long time, boolean existsInRemote) {
        this.sensorId = sensorId;
        this.value = value;
        this.time = time;
        this.existsInRemote = existsInRemote;
    }

    /**
     * Construct a DataPoint from a JSONObject.
     * @param obj
     */
    public DataPoint(JSONObject obj) throws JSONException {
        this.sensorId   = obj.getLong("sensorId");
        this.value      = obj.opt("value");
        this.time = obj.getLong("time");
        this.existsInRemote = obj.optBoolean("existsInRemote", false);
    }

    public long getSensorId() {
        return sensorId;
    }

    public void setSensorId(long sensorId) {
        this.sensorId = sensorId;
    }

    public Object getValue() {
        return value;
    }

    public boolean getValueAsBoolean() throws ClassCastException {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            String stringValue = (String) value;
            if ("true".equalsIgnoreCase(stringValue)) {
                return true;
            } else if ("false".equalsIgnoreCase(stringValue)) {
                return false;
            }
        }
        throw new ClassCastException("Cannot cast value to Boolean");
    }

    public float getValueAsFloat() throws ClassCastException {
        if (value instanceof Float) {
            return (float) value;
        } else if (value instanceof Number) {
            return ((Number) value).floatValue();
        } else if (value instanceof String) {
            try {
                return Float.valueOf((String) value);
            } catch (NumberFormatException ignored) {
            }
        }
        throw new ClassCastException("Cannot cast value to Float");
    }

    public double getValueAsDouble() throws ClassCastException {
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.valueOf((String) value);
            } catch (NumberFormatException ignored) {
            }
        }
        throw new ClassCastException("Cannot cast value to Double");
    }

    public int getValueAsInteger() throws ClassCastException {
        if (value instanceof Integer) {
            return (int) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {
            }
        }
        throw new ClassCastException("Cannot cast value to Integer");
    }

    public long getValueAsLong() throws ClassCastException {
        if (value instanceof Long) {
            return (long) value;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException ignored) {
            }
        }
        throw new ClassCastException("Cannot cast value to Long");
    }

    public JSONObject getValueAsJSONObject() throws ClassCastException {
        if (value instanceof JSONObject) {
            return (JSONObject) value;
        } else if (value instanceof String) {
            try {
                return new JSONObject((String) value);
            } catch (JSONException ignored) {
            }
        }
        throw new ClassCastException("Cannot cast value to JSONObject");
    }

    public String getValueAsString() {
        if (value instanceof String) {
            return (String) value;
        } else {
            return String.valueOf(value);
        }
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public boolean existsInRemote() {
        return existsInRemote;
    }

    public void setExistsInRemote(boolean existsInRemote) {
        this.existsInRemote = existsInRemote;
    }

    /**
     * @return Returns a JSON representation of the DataPoint.
     */
    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();

        try {
            obj.put("sensorId", sensorId);
            obj.put("value",    value);
            obj.put("time", time);
            obj.put("existsInRemote", existsInRemote);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj;
    }

    /**
     * @return Returns a string representation of the DataPoint, a stringified JSON object.
     */
    public String toString() {
      return toJSON().toString();
    }
}
