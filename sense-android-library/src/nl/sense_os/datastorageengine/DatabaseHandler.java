package nl.sense_os.datastorageengine;

import android.content.Context;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.exceptions.RealmPrimaryKeyConstraintException;
import nl.sense_os.datastorageengine.realm.RealmSensor;


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

    private Context context = null;
    private String userId = null;

    public DatabaseHandler(Context context, String userId) {
        this.context = context;
        this.userId = userId;
    }
    public String getUserId() {
        return userId;
    }

    public Sensor createSensor(String source, String name, SensorOptions options) throws DatabaseHandlerException, SensorException {
        Realm realm = Realm.getInstance(context);
        try {
            final long id = Sensor.generateId(realm);
            final boolean synced = false;
            Sensor sensor = new Sensor(context, id, name, userId, source, options, synced);

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

    public Sensor getSensor(String source, String name) throws JSONException, DatabaseHandlerException, SensorException {
        Realm realm = Realm.getInstance(context);
        try {
            realm.beginTransaction();

            RealmSensor realmSensor = realm
                    .where(RealmSensor.class)
                    .equalTo("userId", userId)
                    .equalTo("source", source)
                    .equalTo("name", name)
                    .findFirst();

            realm.commitTransaction();

            if (realmSensor == null) {
                throw new DatabaseHandlerException("Sensor not found. Sensor with name " + name + " does not exist.");
            }

            return RealmSensor.toSensor(context, realmSensor);
        }
        finally {
            realm.close();
        }
    }

    public boolean hasSensor(String source, String name) {
        Realm realm = Realm.getInstance(context);
        try {
            realm.beginTransaction();
            RealmSensor realmSensor = realm
                    .where(RealmSensor.class)
                    .equalTo("userId", userId)
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

    public List<Sensor> getSensors(String source) throws JSONException, SensorException {
        Realm realm = Realm.getInstance(context);
        try {
            // query results
            realm.beginTransaction();
            RealmResults<RealmSensor> results = realm
                    .where(RealmSensor.class)
                    .equalTo("userId", userId)
                    .equalTo("source", source)
                    .findAll();
            realm.commitTransaction();

            // convert to Sensor
            List<Sensor> sensors = new ArrayList<>();
            Iterator<RealmSensor> iterator = results.iterator();
            while (iterator.hasNext()) {
                sensors.add(RealmSensor.toSensor(context, iterator.next()));
            }
            // TODO: figure out what is the most efficient way to loop over the results

            return sensors;
        }
        finally {
            realm.close();
        }
    }

    public List<String> getSources() {
        Realm realm = Realm.getInstance(context);
        try {
            // query results
            realm.beginTransaction();
            RealmResults<RealmSensor> results = realm
                    .where(RealmSensor.class)
                    .equalTo("userId", userId)
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

    public void createDataDeletionRequest(String sensorName, String source, Long startTime, Long endTime) throws DatabaseHandlerException{
        Realm realm = Realm.getInstance(context);
        try {
            if (startTime == null) {
                startTime = -1l;
            }
            if (endTime == null) {
                endTime = -1l;
            }
            DataDeletionRequest dataDeletionRequest = new DataDeletionRequest(userId, sensorName, source, startTime, endTime);

            realm.beginTransaction();
            try {
                realm.copyToRealm(dataDeletionRequest);
                realm.commitTransaction();
            } catch (RealmPrimaryKeyConstraintException err) {
                throw new DatabaseHandlerException("Cannot create sensor. A sensor with name \"" + sensorName + "\" and source \"" + source + "\" already exists.");
            }
        }
        finally {
            realm.close();
        }
    }

    public List<DataDeletionRequest> getDataDeletionRequests(){
        Realm realm = Realm.getInstance(context);
        try {
            // query results
            realm.beginTransaction();
            RealmResults<DataDeletionRequest> results = realm
                    .where(DataDeletionRequest.class)
                    .equalTo("userId", userId)
                    .findAll();
            realm.commitTransaction();

            List<DataDeletionRequest> dataDeletionRequests = new ArrayList<>();
            Iterator<DataDeletionRequest> iterator = results.iterator();
            while (iterator.hasNext()) {
                dataDeletionRequests.add(iterator.next());
            }
            return dataDeletionRequests;
        }
        finally {
            realm.close();
        }
    }

    public void deleteDataDeletionRequest(String uuid){
        Realm realm = Realm.getInstance(context);
        try {
            realm.beginTransaction();
            RealmResults<DataDeletionRequest> result = realm
                    .where(DataDeletionRequest.class)
                    .equalTo("userId", userId)
                    .equalTo("uuid", uuid)
                    .findAll();
            result.clear();
            realm.commitTransaction();
        }
        finally {
            realm.close();
        }
    }
}
