package nl.sense_os.datastorageengine;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.exceptions.RealmException;
import nl.sense_os.service.shared.SensorDataPoint;


/**
 * RealmDatabaseHandler handles local storage of DataPoints, Sensors, and Sources.
 * It stores the data in a local Realm database.
 * It needs to be instantiated with an Android Context.
 *
 * Example usage:
 *
 *     DatabaseHandler databaseHandler = new RealmDatabaseHandler(getContext(), userId);
 *     Sensor sensor = source.getSensor(sensorName);
 *
 *     sensor.insertDataPoint(1234, new Date().getTime());
 *
 *     long startDate = 1388534400000; // 2014-01-01
 *     long startDate = 1420070400000; // 2015-01-01
 *     List<DataPoint> data = sensor.getDataPoints(startDate, endDate, 1000, SORT_ORDER.ASC);
 *
 *     databaseHandler.close();
 *
 */
public class RealmDatabaseHandler implements DatabaseHandler {

    private Realm realm = null;
    private String userId = null;
    private String source = android.os.Build.MODEL; // for example "HTC Desire"

    public RealmDatabaseHandler(Context context, String userId) {
        realm = Realm.getInstance(context);
        this.userId = userId;

        // TODO: determine source, like "Nexus 5"
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

    @Override
    public Sensor createSensor(String name, SensorDataPoint.DataType dataType, SensorOptions options) throws DatabaseHandlerException {
        String id = UUID.randomUUID().toString();  // TODO: change to auto increment
        String csId = null;  // must be filled out by the database syncer
        boolean synced = false;
        Sensor sensor = new RealmSensor(realm, id, name, userId, this.source, dataType, csId, options, synced);

        RealmModelSensor realmSensor = RealmModelSensor.fromSensor(sensor);

        realm.beginTransaction();
        try {
            realm.copyToRealm(realmSensor);
        }
        catch (RealmException err) {
            if (err.toString().indexOf("Primary key constraint broken") != -1) {
                throw new DatabaseHandlerException("Cannot create sensor. A sensor with id " + id + " already exists.");
            }
            else {
                throw err;
            }
        }
        realm.commitTransaction();

        return sensor;
    }

    @Override
    public Sensor getSensor(String name) throws JSONException, DatabaseHandlerException {
        realm.beginTransaction();

        RealmModelSensor realmSensor = realm
                .where(RealmModelSensor.class)
                .equalTo("source", this.source)
                .equalTo("name", name)
                .findFirst();

        realm.commitTransaction();

        if (realmSensor == null) {
            throw new DatabaseHandlerException("Sensor not found. Sensor with name " + name + " does not exist.");
        }

        return RealmModelSensor.toSensor(realm, realmSensor);
    }

    @Override
    public List<Sensor> getSensors() throws JSONException {
        // query results
        realm.beginTransaction();
        RealmResults<RealmModelSensor> results = realm
                .where(RealmModelSensor.class)
                .equalTo("source", this.source)
                .findAll();
        realm.commitTransaction();

        // convert to Sensor
        List<Sensor> sensors = new ArrayList<>();
        Iterator<RealmModelSensor> iterator = results.iterator();
        while (iterator.hasNext()) {
            sensors.add(RealmModelSensor.toSensor(realm, iterator.next()));
        }
        // TODO: figure out what is the most efficient way to loop over the results

        return sensors;
    }

}
