package nl.sense_os.datastorageengine;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import nl.sense_os.service.shared.SensorDataPoint;


/**
 * RealmDatabaseHandler handles local storage of DataPoints, Sensors, and Sources.
 * It stores the data in a local Realm database.
 * It needs to be instantiated with an Android Context.
 *
 * Example usage:
 *
 *     DatabaseHandler databaseHandler = new RealmDatabaseHandler(getContext());
 *
 *     Sensor sensor = databaseHandler.getSensor(sourceId, sensorName);
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

    public RealmDatabaseHandler(Context context) {
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
    protected void insertDataPoint (DataPoint dataPoint) {
        RealmModelDataPoint realmDataPoint = RealmModelDataPoint.fromDataPoint(dataPoint);

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
    protected List<DataPoint> getDataPoints(String sensorId, long startDate, long endDate, int limit, SORT_ORDER sortOrder) throws JSONException {
        // query results
        realm.beginTransaction();
        RealmResults<RealmModelDataPoint> results = realm
                .where(RealmModelDataPoint.class)
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
        Iterator<RealmModelDataPoint> iterator = results.iterator();
        while (count < limit && iterator.hasNext()) {
            dataPoints.add(RealmModelDataPoint.toDataPoint(iterator.next()));
            count++;
        }
        // TODO: figure out what is the most efficient way to loop over the results

        return dataPoints;
    }

    /**
     * Create a new Sensor and store it in the local database
     */
    protected Sensor createSensor(String id, String name, String userId, String sourceId, SensorDataPoint.DataType dataType, String csId, SensorOptions options, boolean synced) {
        Sensor sensor = new RealmSensor(this, id, name, userId, sourceId, dataType, csId, options, synced);

        RealmModelSensor realmSensor = RealmModelSensor.fromSensor(sensor);

        realm.beginTransaction();
        realm.copyToRealm(realmSensor);
        realm.commitTransaction();

        return sensor;
    }

    /**
     * Update RealmSensor in local database with the info of the given Sensor object. Throws an exception if it fails to updated.
     * @param sensor: Sensor object containing the updated info.
     */
    protected void updateSensor(Sensor sensor) throws DatabaseHandlerException {
        realm.beginTransaction();

        // get the existing sensor (just to throw an error if it doesn't exist)
        RealmModelSensor realmSensor = realm
                .where(RealmModelSensor.class)
                .equalTo("id", sensor.getId())
                .findFirst();
        
        if (realmSensor == null) {
            throw new DatabaseHandlerException("Cannot update sensor: sensor doesn't yet exist.");
        }

        // update the sensor
        realm.copyToRealmOrUpdate(RealmModelSensor.fromSensor(sensor));

        realm.commitTransaction();
    }

    /**
     * Get a sensor
     * @param sourceId	The ID of the source object or Null
     * @param sensorName	The name of the sensor or Null
     * @return sensor: sensor with the given sensor name and sourceId.
     **/
    protected Sensor getSensor(String sourceId, String sensorName) throws JSONException {
        realm.beginTransaction();

        RealmModelSensor realmSensor = realm
                .where(RealmModelSensor.class)
                .equalTo("sourceId", sourceId)
                .equalTo("name", sensorName)
                .findFirst();

        realm.commitTransaction();

        return RealmModelSensor.toSensor(this, realmSensor);
    }

    /**
     * Retrieve all sensors for given source id.
     * @param sourceId
     * @return
     */
    protected List<Sensor> getSensors(String sourceId) throws JSONException {
        // query results
        realm.beginTransaction();
        RealmResults<RealmModelSensor> results = realm
                .where(RealmModelSensor.class)
                .equalTo("sourceId", sourceId)
                .findAll();
        realm.commitTransaction();

        // convert to Sensor
        List<Sensor> sensors = new ArrayList<>();
        Iterator<RealmModelSensor> iterator = results.iterator();
        while (iterator.hasNext()) {
            sensors.add(RealmModelSensor.toSensor(this, iterator.next()));
        }
        // TODO: figure out what is the most efficient way to loop over the results

        return sensors;
    }


    /**
     * Create a new source and store it in the local database
     */
    public Source createSource(String id, String name, JSONObject meta, String deviceId, String userId, String csId, boolean synced) {
        Source source = new RealmSource(this, id, name, meta, deviceId, userId, csId, synced);

        RealmModelSource realmSource = RealmModelSource.fromSource(source);

        realm.beginTransaction();
        realm.copyToRealm(realmSource);
        realm.commitTransaction();

        return source;
    }

    /**
     * Update Source in database with the info of the given Source object.
     * Throws an exception if it fails to updated.
     * @param source: Source object containing the updated info.
     */
    protected void updateSource(Source source) throws DatabaseHandlerException {
        realm.beginTransaction();

        // get the existing source (just to throw an error if it doesn't exist)
        RealmModelSource realmSource = realm
                .where(RealmModelSource.class)
                .equalTo("id", source.getId())
                .findFirst();

        if (realmSource == null) {
            throw new DatabaseHandlerException("Cannot update source: source doesn't yet exist.");
        }

        // update the source
        realm.copyToRealmOrUpdate(RealmModelSource.fromSource(source));

        realm.commitTransaction();
    }

    /**
     * Returns a list of sources based on the specified criteria.
     * @param sourceName    Name of the source
     * @param deviceId      Device identifier
     * @return list of source objects that correspond to the specified criteria.
     */
    public List<Source> getSources (String sourceName, String deviceId) throws JSONException {
        // query results
        realm.beginTransaction();
        RealmResults<RealmModelSource> results = realm
                .where(RealmModelSource.class)
                .equalTo("name", sourceName)
                .equalTo("deviceId", deviceId)
                .findAll();
        realm.commitTransaction();

        // convert to Source
        List<Source> sources = new ArrayList<>();
        Iterator<RealmModelSource> iterator = results.iterator();
        while (iterator.hasNext()) {
            sources.add(RealmModelSource.toSource(this, iterator.next()));
        }
        // TODO: figure out what is the most efficient way to loop over the results

        return sources;
    }

}
