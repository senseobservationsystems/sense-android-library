package nl.sense_os.datastorageengine.realm;

import android.content.Context;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import nl.sense_os.datastorageengine.DataPoint;
import nl.sense_os.datastorageengine.DatabaseHandler;
import nl.sense_os.datastorageengine.DatabaseHandlerException;
import nl.sense_os.datastorageengine.Sensor;
import nl.sense_os.datastorageengine.Source;


/**
 * RealmDatabaseHandler handles local storage of DataPoints, Sensors, and Sources.
 * It stores the data in a local Realm database.
 * It needs to be instantiated with an Android Context.
 *
 * Example usage:
 *
 *     DatabaseHandler dh = RealmDatabaseHandler.getInstance(getContext());
 *
 *     DataPoint dataPoint = new DataPoint("mysensor", 1234, new Date().getTime());
 *     dh.insertDataPoint(dataPoint);
 *
 *     long startDate = 1388534400000; // 2014-01-01
 *     long startDate = 1420070400000; // 2015-01-01
 *     List<DataPoint> data = dh.getDataPoints("mysensor", startDate, endDate, 1000, SORT_ORDER.ASC);
 *
 *     dh.close();
 *
 */
public class RealmDatabaseHandler implements DatabaseHandler {

    private static RealmDatabaseHandler instance = null; // singleton instance

    private Realm realm = null;

    /**
     * Get a singleton instance of the RealmDatabaseHandler
     * @param context   Android application context
     * @return Returns a singleton instance
     */
    DatabaseHandler getInstance(Context context) {
        if (instance == null) {
            synchronized (RealmDatabaseHandler.class) {
                if (instance == null) {
                    instance = new RealmDatabaseHandler(context);
                }
            }
        }

        return instance;
    };

    private RealmDatabaseHandler(Context context) {
        realm = Realm.getInstance(context);
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

        // remove pointer to singleton instance
        instance = null;
    }

    /**
     * Close the DatabaseHandler. This will neatly close the database connection to Realm.
     * @throws Exception
     */
    public void close () throws Exception {
        finalize();
    }

    /**
     * This method inserts a DataPoint object into the local Realm database.
     * The typecasting to string should already be done at this point.
     * @param dataPoint	A DataPoint object that has a stringified value that will be copied
     * 			into a Realm object.
     */
    public void insertDataPoint (DataPoint dataPoint) {
        RealmDataPoint realmDataPoint = RealmDataPoint.fromDataPoint(dataPoint);

        realm.beginTransaction();
        realm.copyToRealmOrUpdate(realmDataPoint);
        realm.commitTransaction();
    }

    /**
     * Get data points from the local database with the given sensor id.
     * @param sensorId: String for the sensorID of the sensor that the data point belongs to.
     * @param startDate: Start date of the query, included.
     * @param endDate: End date of the query, excluded.
     * @param limit: The maximum number of data points.
     * @param sortOrder: Sort order, either ASC or DESC
     * @return Returns a List with data points
     */
    public List<DataPoint> getDataPoints(String sensorId, long startDate, long endDate, int limit, SORT_ORDER sortOrder) throws JSONException {
        // query results
        realm.beginTransaction();
        RealmResults<RealmDataPoint> results = realm
                .where(RealmDataPoint.class)
                .equalTo("sensorId", sensorId)
                .greaterThanOrEqualTo("date", startDate)
                .lessThan("date", endDate)
                .findAll();
        realm.commitTransaction();

        // sort
        boolean resultsOrder = (sortOrder == SORT_ORDER.DESC )
                ? RealmResults.SORT_ORDER_DESCENDING
                : RealmResults.SORT_ORDER_ASCENDING;
        results.sort("date", resultsOrder);

        // limit and convert to DataPoint
        int count = 0;
        List<DataPoint> dataPoints = new ArrayList<>();
        Iterator<RealmDataPoint> iterator = results.iterator();
        while (count < limit && iterator.hasNext()) {
            dataPoints.add(RealmDataPoint.toDataPoint(iterator.next()));
            count++;
        }
        // TODO: figure out what is the most efficient way to loop over the results

        return dataPoints;
    }

    /**
     * Store a new sensor in the local database
     * @param sensor
     */
    public void insertSensor(Sensor sensor) {
        RealmSensor realmSensor = RealmSensor.fromSensor(sensor);

        realm.beginTransaction();
        realm.copyToRealm(realmSensor);
        realm.commitTransaction();
    }

    /**
     * Update RealmSensor in local database with the info of the given Sensor object. Throws an exception if it fails to updated.
     * @param sensor: Sensor object containing the updated info.
     */
    public void updateSensor(Sensor sensor) throws DatabaseHandlerException {
        realm.beginTransaction();

        // get the existing sensor (just to throw an error if it doesn't exist)
        RealmSensor realmSensor = realm
                .where(RealmSensor.class)
                .equalTo("id", sensor.getId())
                .findFirst();
        
        if (realmSensor == null) {
            throw new DatabaseHandlerException("Cannot update sensor: sensor doesn't yet exist.");
        }

        // update the sensor
        realm.copyToRealmOrUpdate(RealmSensor.fromSensor(sensor));

        realm.commitTransaction();
    }

    /**
     * Get a sensor
     * @param sourceId	The ID of the source object or Null
     * @param sensorName	The name of the sensor or Null
     * @return sensor: sensor with the given sensor name and sourceId.
     **/
    public Sensor getSensor(String sourceId, String sensorName) throws JSONException {
        realm.beginTransaction();

        RealmSensor realmSensor = realm
                .where(RealmSensor.class)
                .equalTo("sourceId", sourceId)
                .equalTo("name", sensorName)
                .findFirst();

        realm.commitTransaction();

        return RealmSensor.toSensor(realmSensor);
    }

    /**
     * Retrieve all sensors for given source id.
     * @param sourceId
     * @return
     */
    public List<Sensor> getSensors(String sourceId) throws JSONException {
        // query results
        realm.beginTransaction();
        RealmResults<RealmSensor> results = realm
                .where(RealmSensor.class)
                .equalTo("sourceId", sourceId)
                .findAll();
        realm.commitTransaction();

        // convert to Sensor
        List<Sensor> sensors = new ArrayList<>();
        Iterator<RealmSensor> iterator = results.iterator();
        while (iterator.hasNext()) {
            sensors.add(RealmSensor.toSensor(iterator.next()));
        }
        // TODO: figure out what is the most efficient way to loop over the results

        return sensors;
    }


    /**
     * Store a new source in the local database
     * @param source
     */
    public void insertSource(Source source) {
        RealmSource realmSource = RealmSource.fromSource(source);

        realm.beginTransaction();
        realm.copyToRealm(realmSource);
        realm.commitTransaction();
    }

    /**
     * Update Source in database with the info of the given Source object.
     * Throws an exception if it fails to updated.
     * @param source: Source object containing the updated info.
     */
    public void updateSource(Source source) throws DatabaseHandlerException {
        realm.beginTransaction();

        // get the existing source (just to throw an error if it doesn't exist)
        RealmSource realmSource = realm
                .where(RealmSource.class)
                .equalTo("id", source.getId())
                .findFirst();

        if (realmSource == null) {
            throw new DatabaseHandlerException("Cannot update source: source doesn't yet exist.");
        }

        // update the source
        realm.copyToRealmOrUpdate(RealmSource.fromSource(source));

        realm.commitTransaction();
    }

    /**
     * Returns a list of sources based on the specified criteria.
     * @param sourceName    Name of the source
     * @param uuid          Device identifier
     * @return list of source objects that correspond to the specified criteria.
     */
    public List<Source> getSources (String sourceName, String uuid) throws JSONException {
        // query results
        realm.beginTransaction();
        RealmResults<RealmSource> results = realm
                .where(RealmSource.class)
                .equalTo("name", sourceName)
                .equalTo("uuid", uuid)
                .findAll();
        realm.commitTransaction();

        // convert to Source
        List<Source> sources = new ArrayList<>();
        Iterator<RealmSource> iterator = results.iterator();
        while (iterator.hasNext()) {
            sources.add(RealmSource.toSource(iterator.next()));
        }
        // TODO: figure out what is the most efficient way to loop over the results

        return sources;
    }

}
