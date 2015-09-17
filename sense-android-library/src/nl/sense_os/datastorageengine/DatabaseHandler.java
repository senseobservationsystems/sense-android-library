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


// TODO: comment
public class DatabaseHandler {

    private Realm realm = null;

    public enum SORT_ORDER {ASC, DESC};

    public DatabaseHandler (Context context) {
        realm = Realm.getInstance(context);
    }

    @Override
    protected void finalize() throws Throwable {
        if (realm != null) {
            realm.close();
            realm = null;
        }
    }

    /**
     * Add a data point to the sensor with the given sensorId.
     * Throw exceptions if it fails to add the data point.
     * @param dataPoint   A data point containing sensorId, value, and date
     */
    public void insertDataPoint (DataPoint dataPoint) {
        RealmDataPoint realmDataPoint = RealmDataPoint.fromDataPoint(dataPoint);

        realm.beginTransaction();
        realm.copyToRealm(realmDataPoint);
        realm.commitTransaction();
    }

    /**
     * Get data points from the sensor with the given sensor id.
     * @param sensorId: String for the sensorID of the sensor that the data point belongs to.
     * @param startDate: Start date of the query, included.
     * @param endDate: End date of the query, excluded.
     * @param limit: The maximum number of data points.
     * @param sortOrder: Sort order, either ASC or DESC
     * @return datapoints: An array of NSDictionary represents data points.
     */
    public List<DataPoint> getDataPoints(String sensorId, Date startDate, Date endDate, int limit, SORT_ORDER sortOrder) throws JSONException {
        // query results
        realm.beginTransaction();
        RealmResults<RealmDataPoint> results = realm
                .where(RealmDataPoint.class)
                .equalTo("sensorId", sensorId)
                .greaterThanOrEqualTo("date", startDate.getTime())
                .lessThan("date", endDate.getTime())
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
     * Set the meta data and the options for enabling data upload/download to Common Sense and local data persistence.
     *
     * @param sensorId: String for the sensorID of the sensor that the new setting should be applied to.
     * @param options: Sensor options.
     */
    public void setSensorOptions(String sensorId, SensorOptions options) {
        // TODO: implement
    }

    /**
     * Store a new sensor in the local database
     * @param sensor
     */
    public void createSensor(Sensor sensor) {
        // TODO: implement
    }

    /**
     * Retrieve a sensor by its name.
     * @param sourceId
     * @param sensorName
     * @return
     */
    public Sensor getSensor(String sourceId, String sensorName) {
        //TODO: implement
        return null;
    }

    /**
     * Retrieve all sensors for given source id.
     * @param sourceId
     * @return
     */
    public List<Sensor> getSensors(String sourceId) {
        //TODO: implement
        return null;
    }

    /**
     * Store a new source in the local database
     * @param source
     */
    public void createSource(Source source) {
        // TODO: implement
    }

    /**
     * Retrieve a source by it's id
     * @param sourceId
     * @return
     */
    public Source getSource (String sourceId) {
        // TODO: implement
        return null;
    }

    /**
     * Retrieve all sources
     * @return
     */
    public List<Source> getSources () {
        // TODO: implement
        return null;
    }
}
