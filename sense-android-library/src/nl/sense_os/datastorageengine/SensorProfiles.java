package nl.sense_os.datastorageengine;

import android.content.Context;

//import org.everit.json.schema.Schema;
//import org.everit.json.schema.ValidationException;
//import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import io.realm.Realm;
import io.realm.exceptions.RealmException;
import nl.sense_os.datastorageengine.realm.RealmSensorProfile;

public class SensorProfiles {
    private static HashMap<Context, SensorProfiles> singletons = new HashMap<>(); // a singleton for every context (just 1 in practice)

    private Realm realm = null;

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
