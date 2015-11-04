package nl.sense_os.datastorageengine;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import io.realm.Realm;
import io.realm.exceptions.RealmPrimaryKeyConstraintException;
import nl.sense_os.datastorageengine.realm.RealmSensorProfile;

public class SensorProfiles {
    private static HashMap<Context, SensorProfiles> singletons = new HashMap<>(); // a singleton for every context (just 1 in practice)

    private Context context = null;

    public static synchronized SensorProfiles getInstance(Context context) {
        if (!singletons.containsKey(context)) {
            singletons.put(context, new SensorProfiles(context));
        }
        return singletons.get(context);
    }

    private SensorProfiles(Context context) {
        this.context = context;
        // TODO: load all profiles from Realm, keep them in memory
    }

    public void createSensorProfile(String sensorName, JSONObject profile) throws SensorProfileException {
        Realm realm = Realm.getInstance(context);
        try {
            RealmSensorProfile realmSensorProfile = new RealmSensorProfile(sensorName, profile.toString());

            realm.beginTransaction();
            try {
                realm.copyToRealm(realmSensorProfile);
                realm.commitTransaction();
            } catch (RealmPrimaryKeyConstraintException err) {
                throw new SensorProfileException("Cannot create sensorPorfile. A sensor with name " + sensorName + " already exists.");
            }
        }
        finally {
            realm.close();
        }
    }

    public void createOrUpdateSensorProfile(String sensorName, JSONObject profile) throws SensorProfileException {
        Realm realm = Realm.getInstance(context);
        try {
            RealmSensorProfile realmSensorProfile = new RealmSensorProfile(sensorName, profile.toString());

            realm.beginTransaction();
            realm.copyToRealmOrUpdate(realmSensorProfile);
            realm.commitTransaction();
        }
        finally {
            realm.close();
        }
    }

    public boolean hasSensorProfile(String sensorName) {
        Realm realm = Realm.getInstance(context);
        try {
            realm.beginTransaction();
            RealmSensorProfile realmSensorProfile = realm
                    .where(RealmSensorProfile.class)
                    .equalTo("sensorName", sensorName)
                    .findFirst();
            realm.commitTransaction();

            return (realmSensorProfile != null);
        }
        finally {
            realm.close();
        }
    }

    public JSONObject getSensorProfile(String sensorName) throws SensorProfileException, JSONException {
        Realm realm = Realm.getInstance(context);
        try {
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
        finally {
            realm.close();
        }
    }

    /**
     * Validate whether a value has the correct type for the specified sensor.
     * Throws an exception when the type of value is not valid, else remains silent.
     * @param sensorName
     * @param value
     */
    public void validate (String sensorName, Object value) throws JSONException, SensorProfileException {
        // TODO: implement validate
//        JSONObject rawSchema = getSensorProfile(sensorName);
//        Schema schema = SchemaLoader.load(rawSchema);
//        schema.validate(value);
    }
}
