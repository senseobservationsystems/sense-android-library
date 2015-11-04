package nl.sense_os.datastorageengine;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;
import io.realm.exceptions.RealmPrimaryKeyConstraintException;
import nl.sense_os.datastorageengine.realm.RealmSensorProfile;

public class SensorProfiles {
    private Context context = null;

    public SensorProfiles(Context context) {
        this.context = context;
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
}
