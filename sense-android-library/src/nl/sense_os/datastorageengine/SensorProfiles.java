package nl.sense_os.datastorageengine;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.realm.Realm;
import io.realm.exceptions.RealmException;
import nl.sense_os.datastorageengine.realm.RealmSensorProfile;

public class SensorProfiles {
    private static HashMap<Context, SensorProfiles> singletons = new HashMap<>(); // a singleton for every context (just 1 in practice)

    private Realm realm = null;

    // TODO: cleanup
    protected static final Map<String, String> sensorDataTypes;
    static
    {
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

    public static synchronized SensorProfiles getInstance(Context context) {
        if (!singletons.containsKey(context)) {
            singletons.put(context, new SensorProfiles(context));
        }
        return singletons.get(context);
    }

    private SensorProfiles(Context context) {
        this.realm = Realm.getInstance(context);

        // TODO: load all profiles from Realm, keep them in memory
    }

    /**
     * Close the database connection.
     */
    @Override
    protected void finalize() throws IllegalStateException {
        // close realm
        if (realm != null) {
            realm.close();
            realm = null;
        }
    }

    public void createSensorProfile(String sensorName, JSONObject profile) throws SensorProfileException {
        RealmSensorProfile realmSensorProfile = new RealmSensorProfile(sensorName, profile.toString());
        realm.beginTransaction();

        try {
            realm.copyToRealm(realmSensorProfile);
        }
        catch (RealmException err) {
            if (err.toString().contains("Primary key constraint broken")) {
                throw new SensorProfileException("Cannot create sensorPorfile. A sensor with name " + sensorName  + " already exists.");
            }
            else {
                throw err;
            }
        }

        realm.commitTransaction();
    }

    public void createOrUpdateSensorProfile(String sensorName, JSONObject profile) throws SensorProfileException {
        RealmSensorProfile realmSensorProfile = new RealmSensorProfile(sensorName, profile.toString());
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(realmSensorProfile);
        realm.commitTransaction();
    }

    public boolean hasSensorProfile(String sensorName) {
        realm.beginTransaction();
        RealmSensorProfile realmSensorProfile = realm
                .where(RealmSensorProfile.class)
                .equalTo("sensorName", sensorName)
                .findFirst();
        realm.commitTransaction();

        return (realmSensorProfile != null);
    }

    public JSONObject getSensorProfile(String sensorName) throws SensorProfileException, JSONException {
        realm.beginTransaction();
        RealmSensorProfile realmSensorProfile = realm
                .where(RealmSensorProfile.class)
                .equalTo("sensorName", sensorName)
                .findFirst();
        realm.commitTransaction();

        if (realmSensorProfile != null) {
            return RealmSensorProfile.getProfileAsJSONObject(realmSensorProfile);
        }
        else {
            throw new SensorProfileException("Sensor profile not found. Sensor name: '" + sensorName  + "'");
        }
    }

    /**
     * Add a new sensor type to the list.
     * @param sensorName
     * @param type
     * @deprecated
     */
    // TODO: cleanup
    public static void addSensorType (String sensorName, String type) {
        sensorDataTypes.put(sensorName, type);
    }

    /**
     * Get the type of a sensor. Returns null if the sensor does not exist
     * @param sensorName
     * @return
     * @deprecated
     */
    // TODO: cleanup
    public static String getSensorType(String sensorName) {
        return sensorDataTypes.get(sensorName);
    }

    /**
     * Validate whether a DataPoint has the correct type of value for the specified sensor.
     * Throws an exception when the type of value is not valid, else remains silent.
     * @param sensorName
     * @param dataPoint
     */
    public static void validate (String sensorName, DataPoint dataPoint) throws SensorProfileException {
        // TODO: refactor (not static anymore)

        Object value = dataPoint.getValue();
        String type = sensorDataTypes.get(sensorName);

        if (type == null) {
            // TODO: what type of exception do we want to throw here?
            throw new SensorProfileException("Unknown sensor name \"" + sensorName + "\"");
        }

        if ("boolean".equals(type)) {
            if (value instanceof Boolean) {
                return; // valid
            }
            throw new SensorProfileException("Invalid data type, boolean value expected for sensor '" + sensorName + "'");
        }

        if ("number".equals(type)) {
            if (value instanceof Number) {
                return; // valid
            }
            throw new SensorProfileException("Invalid data type, numeric value expected for sensor '" + sensorName + "'");
        }

        if ("string".equals(type)) {
            if (value instanceof String) {
                return; // valid
            }
            throw new SensorProfileException("Invalid data type, string value expected for sensor '" + sensorName + "'");
        }

        if ("JSONObject".equals(type)) {
            if (value instanceof JSONObject) {
                return; // valid
            }
            throw new SensorProfileException("Invalid data type, JSONObject as value expected for sensor '" + sensorName + "'");
        }

        throw new SensorProfileException("Sensor '" + sensorName + "' has unknown data type '" + type + "'");
    }
}
