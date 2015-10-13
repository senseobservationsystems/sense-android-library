package nl.sense_os.datastorageengine;

import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SensorProfiles {
    protected static final Map<String, String> sensorDataTypes;
    static
    {
        // TODO: put together a real list with the sensors and types
        // https://github.com/senseobservationsystems/aim/blob/master/docs/sensors.md

        // TODO: support for validation of the fields inside JSONObjects
        sensorDataTypes = new ConcurrentHashMap<>();
        sensorDataTypes.put("accelerometer", "JSONObject");
        sensorDataTypes.put("location", "JSONObject");
        sensorDataTypes.put("noise_sensor", "number");
        sensorDataTypes.put("light", "number");
        sensorDataTypes.put("time_active", "number");
        sensorDataTypes.put("sleep_time", "string");
        sensorDataTypes.put("visits", "number");
        sensorDataTypes.put("screen activity", "boolean");
    }

    /**
     * Add a new sensor type to the list.
     * @param sensorName
     * @param type
     */
    public static void addSensorType (String sensorName, String type) {
        sensorDataTypes.put(sensorName, type);
    }

    /**
     * Get the type of a sensor. Returns null if the sensor does not exist
     * @param sensorName
     * @return
     */
    public static String getSensorType(String sensorName) {
        return sensorDataTypes.get(sensorName);
    }

    /**
     * Validate whether a DataPoint has the correct type of value for the specified sensor.
     * Throws an exception when the type of value is not valid, else remains silent.
     * @param sensorName
     * @param dataPoint
     */
    public static void validate (String sensorName, DataPoint dataPoint) throws SensorException {
        Object value = dataPoint.getValue();
        String type = sensorDataTypes.get(sensorName);

        if (type == null) {
            // TODO: what type of exception do we want to throw here?
            throw new SensorException("Unknown sensor name \"" + sensorName + "\"");
        }

        if ("boolean".equals(type)) {
            if (value instanceof Boolean) {
                return; // valid
            }
            throw new SensorException("Invalid data type, boolean value expected for sensor '" + sensorName + "'");
        }

        if ("number".equals(type)) {
            if (value instanceof Number) {
                return; // valid
            }
            throw new SensorException("Invalid data type, numeric value expected for sensor '" + sensorName + "'");
        }

        if ("string".equals(type)) {
            if (value instanceof String) {
                return; // valid
            }
            throw new SensorException("Invalid data type, string value expected for sensor '" + sensorName + "'");
        }

        if ("JSONObject".equals(type)) {
            if (value instanceof JSONObject) {
                return; // valid
            }
            throw new SensorException("Invalid data type, JSONObject as value expected for sensor '" + sensorName + "'");
        }

        throw new SensorException("Sensor '" + sensorName + "' has unknown data type '" + type + "'");
    }
}
