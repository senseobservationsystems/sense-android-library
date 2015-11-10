package nl.sense_os.datastorageengine;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.exceptions.RealmPrimaryKeyConstraintException;
import nl.sense_os.datastorageengine.realm.RealmSensor;
import nl.sense_os.datastorageengine.realm.RealmSensorProfile;

public class SensorProfiles {
    private Context mContext = null;

    public SensorProfiles(Context context) {
        this.mContext = context;
    }

    public void create(String sensorName, JSONObject profile) throws SensorProfileException {
        Realm realm = Realm.getInstance(mContext);
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

    public void createOrUpdate(String sensorName, JSONObject profile) throws SensorProfileException {
        Realm realm = Realm.getInstance(mContext);
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

    public boolean has(String sensorName) {
        Realm realm = Realm.getInstance(mContext);
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

    public JSONObject get(String sensorName) throws SensorProfileException, JSONException {
        Realm realm = Realm.getInstance(mContext);
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

    public Set<String> getSensorNames() throws SensorProfileException, JSONException {
        Realm realm = Realm.getInstance(mContext);
        try {
            realm.beginTransaction();

            RealmResults<RealmSensorProfile> results = realm.where(RealmSensorProfile.class).findAll();
            realm.commitTransaction();

            Set<String> sensorNames = new HashSet<>();
            Iterator<RealmSensorProfile> iterator = results.iterator();
            while (iterator.hasNext()) {
                sensorNames.add(iterator.next().getSensorName());
            }

            return sensorNames;
        }
        finally {
            realm.close();
        }
    }

    /**
     * Get the number of sensor profiles
     * @return Returns the number of profiles
     */
    public long size() {
        Realm realm = Realm.getInstance(mContext);
        try {
            realm.beginTransaction();
            long count = realm.where(RealmSensorProfile.class).count();
            realm.commitTransaction();

            return count;
        }
        finally {
            realm.close();
        }
    }
}
