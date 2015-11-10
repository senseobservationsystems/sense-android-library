package nl.sense_os.datastorageengine;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A DataPoint can hold a single data point of for a sensor: a mTime and a mValue.
 */
public class DataPoint {

    private long sensorId = -1;
    private Object mValue = null;
    private long mTime = 0;
    private boolean mExistsInRemote = false;

    public DataPoint(long sensorId, Object value, long time, boolean existsInRemote) {
        this.sensorId = sensorId;
        this.mValue = value;
        this.mTime = time;
        this.mExistsInRemote = existsInRemote;
    }

    /**
     * Construct a DataPoint from a JSONObject.
     * @param obj
     */
    public DataPoint(JSONObject obj) throws JSONException {
        this.sensorId = obj.getLong("sensorId");
        this.mValue = obj.opt("value");
        this.mTime = obj.getLong("time");
        this.mExistsInRemote = obj.optBoolean("existsInRemote", false);
    }

    public long getSensorId() {
        return sensorId;
    }

    public void setSensorId(long sensorId) {
        this.sensorId = sensorId;
    }

    public Object getValue() {
        return mValue;
    }

    public boolean getValueAsBoolean() throws ClassCastException {
        if (mValue instanceof Boolean) {
            return (Boolean) mValue;
        } else if (mValue instanceof String) {
            String stringValue = (String) mValue;
            if ("true".equalsIgnoreCase(stringValue)) {
                return true;
            } else if ("false".equalsIgnoreCase(stringValue)) {
                return false;
            }
        }
        throw new ClassCastException("Cannot cast value to Boolean");
    }

    public float getValueAsFloat() throws ClassCastException {
        if (mValue instanceof Float) {
            return (float) mValue;
        } else if (mValue instanceof Number) {
            return ((Number) mValue).floatValue();
        } else if (mValue instanceof String) {
            try {
                return Float.valueOf((String) mValue);
            } catch (NumberFormatException ignored) {
            }
        }
        throw new ClassCastException("Cannot cast value to Float");
    }

    public double getValueAsDouble() throws ClassCastException {
        if (mValue instanceof Double) {
            return (Double) mValue;
        } else if (mValue instanceof Number) {
            return ((Number) mValue).doubleValue();
        } else if (mValue instanceof String) {
            try {
                return Double.valueOf((String) mValue);
            } catch (NumberFormatException ignored) {
            }
        }
        throw new ClassCastException("Cannot cast value to Double");
    }

    public int getValueAsInteger() throws ClassCastException {
        if (mValue instanceof Integer) {
            return (int) mValue;
        } else if (mValue instanceof Number) {
            return ((Number) mValue).intValue();
        } else if (mValue instanceof String) {
            try {
                return Integer.parseInt((String) mValue);
            } catch (NumberFormatException ignored) {
            }
        }
        throw new ClassCastException("Cannot cast value to Integer");
    }

    public long getValueAsLong() throws ClassCastException {
        if (mValue instanceof Long) {
            return (long) mValue;
        } else if (mValue instanceof Number) {
            return ((Number) mValue).longValue();
        } else if (mValue instanceof String) {
            try {
                return Long.parseLong((String) mValue);
            } catch (NumberFormatException ignored) {
            }
        }
        throw new ClassCastException("Cannot cast value to Long");
    }

    public JSONObject getValueAsJSONObject() throws ClassCastException {
        if (mValue instanceof JSONObject) {
            return (JSONObject) mValue;
        } else if (mValue instanceof String) {
            try {
                return new JSONObject((String) mValue);
            } catch (JSONException ignored) {
            }
        }
        throw new ClassCastException("Cannot cast value to JSONObject");
    }

    public String getValueAsString() {
        if (mValue instanceof String) {
            return (String) mValue;
        } else {
            return String.valueOf(mValue);
        }
    }

    public void setValue(Object value) {
        this.mValue = value;
    }

    public long getTime() {
        return mTime;
    }

    public void setTime(long time) {
        this.mTime = time;
    }

    public boolean existsInRemote() {
        return mExistsInRemote;
    }

    public void setExistsInRemote(boolean existsInRemote) {
        this.mExistsInRemote = existsInRemote;
    }

    /**
     * @return Returns a JSON representation of the DataPoint.
     */
    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();

        try {
            obj.put("sensorId", sensorId);
            obj.put("value", mValue);
            obj.put("time", mTime);
            obj.put("existsInRemote", mExistsInRemote);
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
