package nl.sense_os.datastorageengine;

import android.content.Context;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import io.realm.exceptions.RealmPrimaryKeyConstraintException;
import nl.sense_os.datastorageengine.realm.RealmDataDeletionRequest;
import nl.sense_os.datastorageengine.realm.RealmSensor;
import nl.sense_os.util.json.SchemaException;


/**
 * DatabaseHandler handles local storage of DataPoints and Sensors.
 * It stores the data in a local Realm database.
 * It needs to be instantiated with an Android Context.
 *
 * Example usage:
 *
 *     DatabaseHandler databaseHandler = new DatabaseHandler(getContext(), userId);
 *     Sensor sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);
 *
 *     sensor.insertDataPoint(1234, new Date().getTime());
 *
 *     long startTime = 1388534400000; // 2014-01-01
 *     long endTime = 1420070400000; // 2015-01-01
 *     List<DataPoint> data = sensor.getDataPoints(startTime, endTime, 1000, SORT_ORDER.ASC);
 *
 *     Sensor returnedSensor = databaseHandler.getSensor(sourceName,sensorName);
 *
 *     databaseHandler.close();
 *
 */
public class DatabaseHandler {

    private Context mContext = null;
    private String mUserId = null;

    private byte[] mEncryptionKey = null;

    public DatabaseHandler(Context context, String userId) {
        this.mContext = context;
        this.mUserId = userId;
        this.mEncryptionKey = null;
    }

    public DatabaseHandler(Context context, byte[] encryptionKey, String userId) {
        this.mContext = context;
        this.mEncryptionKey = encryptionKey;
        this.mUserId = userId;
    }

    public byte[] getEncryptionKey() {
        return mEncryptionKey;
    }

    public String getUserId() {
        return mUserId;
    }

    public Sensor createSensor(String source, String name, SensorOptions options) throws SensorProfileException, SchemaException, JSONException, SensorException, DatabaseHandlerException {
        Realm realm = getRealmInstance();
        try {
            final long id = Sensor.generateId(realm);
            final boolean synced = false;
            Sensor sensor = new Sensor(mContext, mEncryptionKey, id, name, mUserId, source, options, synced);

            RealmSensor realmSensor = RealmSensor.fromSensor(sensor);

            realm.beginTransaction();
            try {
                realm.copyToRealm(realmSensor);
                realm.commitTransaction();
            } catch (RealmPrimaryKeyConstraintException err) {
                throw new DatabaseHandlerException("Cannot create sensor. A sensor with name \"" + name + "\" and source \"" + source + "\" already exists.");
            }

            return sensor;
        } finally {
            realm.close();
        }
    }

    public Sensor getSensor(String source, String name) throws SensorProfileException, SchemaException, SensorException, JSONException, DatabaseHandlerException {
        Realm realm = getRealmInstance();
        try {
            realm.beginTransaction();

            RealmSensor realmSensor = realm
                    .where(RealmSensor.class)
                    .equalTo("userId", mUserId)
                    .equalTo("source", source)
                    .equalTo("name", name)
                    .findFirst();

            realm.commitTransaction();

            if (realmSensor == null) {
                throw new DatabaseHandlerException("Sensor not found. Sensor with name " + name + " does not exist.");
            }

            return RealmSensor.toSensor(mContext, mEncryptionKey, realmSensor);
        }
        finally {
            realm.close();
        }
    }

    public boolean hasSensor(String source, String name) {
        Realm realm = getRealmInstance();
        try {
            realm.beginTransaction();
            RealmSensor realmSensor = realm
                    .where(RealmSensor.class)
                    .equalTo("userId", mUserId)
                    .equalTo("source", source)
                    .equalTo("name", name)
                    .findFirst();
            realm.commitTransaction();

            return (realmSensor != null);
        }
        finally {
            realm.close();
        }
    }

    public List<Sensor> getSensors(String source) throws JSONException, SensorException, SensorProfileException, SchemaException {
        Realm realm = getRealmInstance();
        try {
            // query results
            realm.beginTransaction();
            RealmResults<RealmSensor> results = realm
                    .where(RealmSensor.class)
                    .equalTo("userId", mUserId)
                    .equalTo("source", source)
                    .findAll();
            realm.commitTransaction();

            // convert to Sensor
            List<Sensor> sensors = new ArrayList<>();
            Iterator<RealmSensor> iterator = results.iterator();
            while (iterator.hasNext()) {
                sensors.add(RealmSensor.toSensor(mContext, mEncryptionKey, iterator.next()));
            }
            // TODO: figure out what is the most efficient way to loop over the results

            return sensors;
        }
        finally {
            realm.close();
        }
    }

    public List<String> getSources() {
        Realm realm = getRealmInstance();
        try {
            // query results
            realm.beginTransaction();
            RealmResults<RealmSensor> results = realm
                    .where(RealmSensor.class)
                    .equalTo("userId", mUserId)
                    .findAll();
            realm.commitTransaction();

            // extract the list with sensors
            Set<String> sources = new HashSet<>();
            Iterator<RealmSensor> iterator = results.iterator();
            while (iterator.hasNext()) {
                sources.add(iterator.next().getSource());
            }

            return new ArrayList<>(sources);
        }
        finally {
            realm.close();
        }
    }

    public void createDataDeletionRequest(String sourceName, String sensorName, Long startTime, Long endTime) throws DatabaseHandlerException {
        Realm realm = getRealmInstance();
        try {
            RealmDataDeletionRequest dataDeletionRequest
                    = RealmDataDeletionRequest.fromDataDeletionRequest(new DataDeletionRequest(mUserId, sourceName, sensorName, startTime, endTime));

            realm.beginTransaction();
            try {
                realm.copyToRealm(dataDeletionRequest);
                realm.commitTransaction();
            } catch (RealmPrimaryKeyConstraintException err) {
                throw new DatabaseHandlerException("Error adding delete data request for \"" + sensorName + "\" and source \"" + sourceName + "\".");
            }
        }
        finally {
            realm.close();
        }
    }

    public List<DataDeletionRequest> getDataDeletionRequests() {
        Realm realm = getRealmInstance();
        try {
            // query results
            realm.beginTransaction();
            RealmResults<RealmDataDeletionRequest> results = realm
                    .where(RealmDataDeletionRequest.class)
                    .equalTo("userId", mUserId)
                    .findAll();
            realm.commitTransaction();

            List<DataDeletionRequest> dataDeletionRequests = new ArrayList<>();
            Iterator<RealmDataDeletionRequest> iterator = results.iterator();
            while (iterator.hasNext()) {
                dataDeletionRequests.add(RealmDataDeletionRequest.toDataDeletionRequest(iterator.next()));
            }
            return dataDeletionRequests;
        }
        finally {
            realm.close();
        }
    }

    public void deleteDataDeletionRequest(String id) {
        Realm realm = getRealmInstance();
        try {
            realm.beginTransaction();
            RealmResults<RealmDataDeletionRequest> result = realm
                    .where(RealmDataDeletionRequest.class)
                    .equalTo("userId", mUserId)
                    .equalTo("id", id)
                    .findAll();
            result.clear();
            realm.commitTransaction();
        }
        finally {
            realm.close();
        }
    }

    /**
     * Helper function to create a new Realm instance
     * TODO: come up with a smarter way to use Realm, this is a lot of redundant code right now
     * @return Returns a new Realm instance
     */
    protected Realm getRealmInstance () {
        RealmConfiguration.Builder config = new RealmConfiguration.Builder(mContext);

        if (mEncryptionKey != null) {
            config = config.encryptionKey(mEncryptionKey);
        }

        return Realm.getInstance(config.build());
    }
}
