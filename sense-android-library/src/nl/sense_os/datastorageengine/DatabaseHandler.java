package nl.sense_os.datastorageengine;

import android.content.Context;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import nl.sense_os.datastorageengine.realm.RealmDataPoint;
import nl.sense_os.datastorageengine.realm.RealmSensor;
import nl.sense_os.datastorageengine.realm.RealmSource;


// TODO: comment
public class DatabaseHandler {

    private Realm realm = null;

    public enum SORT_ORDER {ASC, DESC};

    public DatabaseHandler (Context context) {
        realm = Realm.getInstance(context);
    }

    /**
     * Close the database connection.
     * @throws Throwable
     */
    @Override
    protected void finalize() throws Throwable {
        if (realm != null) {
            realm.close();
            realm = null;
        }
    }

    /**
     * Close the DatabaseHandler. This will neatly close the database connection to Realm.
     * @throws Throwable
     */
    public void close () throws Throwable {
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
    public void updateSensor(Sensor sensor) {
        // TODO: implement
        // TODO: I think this method is not needed, insertSensor does this already. Jos
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
    public void updateSource(Source source ) {
        // TODO
        // TODO: I think this method is not needed, insertSource does this already. Jos
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
                .equalTo("sourceName", sourceName)
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
