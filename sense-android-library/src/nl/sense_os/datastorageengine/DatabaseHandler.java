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
import io.realm.exceptions.RealmException;
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

    private Realm realm = null;
    private String userId = null;

    public DatabaseHandler(Context context, String userId) {
        this.realm = Realm.getInstance(context);
        this.userId = userId;
    }

    public Realm getRealm() {
        return realm;
    }

    public String getUserId() {
        return userId;
    }

    /**
     * Close the database connection.
     * @throws Exception
     */
    @Override
    protected void finalize() throws Exception {
        // close realm
        if (realm != null) {
            realm.close();
            realm = null;
        }
    }

    /**
     * Close the DatabaseHandler. This will neatly close the database connection to Realm.
     * @throws Exception
     */
    public void close () throws Exception {
        finalize();
    }

    public Sensor createSensor(String source, String name, SensorOptions options) throws DatabaseHandlerException, SensorException {
        final long id = Sensor.generateId(realm);
        final boolean synced = false;
        Sensor sensor = new Sensor(realm, id, name, userId, source, options, synced);

        RealmSensor realmSensor = RealmSensor.fromSensor(sensor);

        realm.beginTransaction();
        try {
            realm.copyToRealm(realmSensor);
        }
        catch (RealmException err) {
            if (err.toString().contains("Primary key constraint broken")) {
                throw new DatabaseHandlerException("Cannot create sensor. A sensor with name \"" + name + "\" and source \"" + source + "\" already exists.");
            }
            else {
                throw err;
            }
        }
        realm.commitTransaction();

        return sensor;
    }

    public Sensor getSensor(String source, String name) throws JSONException, DatabaseHandlerException, SensorException {
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

        return RealmSensor.toSensor(realm, realmSensor);
    }

    public boolean hasSensor(String source, String name) {
        realm.beginTransaction();
        RealmSensor realmSensor = realm
                .where(RealmSensor.class)
                .equalTo("userId", userId)
                .equalTo("source", source)
                .equalTo("name", name)
                .findFirst();
        realm.commitTransaction();

        if(realmSensor != null){
            return true;
        }else{
            return false;
        }
    }

    public List<Sensor> getSensors(String source) throws JSONException, SensorException {
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
            sensors.add(RealmSensor.toSensor(realm, iterator.next()));
        }
        // TODO: figure out what is the most efficient way to loop over the results

        return sensors;
    }

    public List<String> getSources() {
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

    public void createDataDeletionRequest(String sensorName, String source, Long startTime, Long endTime) throws DatabaseHandlerException{
        if(startTime == null){
            startTime = -1l;
        }
        if(endTime == null){
            endTime = -1l;
        }
        DataDeletionRequest dataDeletionRequest = new DataDeletionRequest(userId, sensorName, source, startTime,endTime);
        realm.beginTransaction();
        try {
            realm.copyToRealm(dataDeletionRequest);
        }
        catch (RealmException err) {
            if (err.toString().contains("Primary key constraint broken")) {
                throw new DatabaseHandlerException("Cannot create DataDeletionRequest. uuid already exists.");
            }
            else {
                throw err;
            }
        }
        realm.commitTransaction();
    }

    public List<DataDeletionRequest> getDataDeletionRequests(){
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

    public void deleteDataDeletionRequest(String uuid){
        realm.beginTransaction();
        RealmResults<DataDeletionRequest> result = realm
                .where(DataDeletionRequest.class)
                .equalTo("userId", userId)
                .equalTo("uuid", uuid)
                .findAll();
        result.clear();
        realm.commitTransaction();
    }
}
