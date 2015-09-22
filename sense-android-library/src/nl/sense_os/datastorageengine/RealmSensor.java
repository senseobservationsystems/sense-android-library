package nl.sense_os.datastorageengine;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import nl.sense_os.service.shared.SensorDataPoint;

public class RealmSensor implements Sensor {

    private Realm realm = null;

    private String id = null;
    private String name = null;
    private String userId = null;
    private String sourceId = null;   // corresponds Source.id
    private SensorDataPoint.DataType dataType = null;
    private String csId = null;
    private SensorOptions options = new SensorOptions();
    private boolean synced = false;

    protected RealmSensor(Realm realm, String id, String name, String userId, String sourceId, SensorDataPoint.DataType dataType, String csId, SensorOptions options, boolean synced) {
        this.realm = realm;

        this.id = id;
        this.name = name;
        this.userId = userId;
        this.sourceId = sourceId;
        this.dataType = dataType;
        this.csId = csId;
        this.options = options;
        this.synced = synced;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUserId() {
        return userId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public SensorDataPoint.DataType getdataType() {
        return dataType;
    }

    public String getCsId() {
        return csId;
    }

    public boolean isSynced() {
        return synced;
    }

    /**
     * Apply options for the sensor.
     * The fields in `options` which are `null` will be ignored.
     * @param options
     * @return Returns the applied options.
     */
    public SensorOptions setOptions (SensorOptions options) throws JSONException, DatabaseHandlerException {
        this.options = SensorOptions.merge(this.options, options);
        this.synced = false; // mark as dirty

        // store changes in the local database
        saveChanges();

        return getOptions();
    }

    /**
     * Retrieve a clone of the current options of the sensor.
     * @return options
     */
    public SensorOptions getOptions () {
        return options.clone();
    }

    public void setSynced(boolean synced) throws DatabaseHandlerException {
        this.synced = synced;

        // store changes in the local database
        saveChanges();
    }

    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param date
     */
    public void insertDataPoint(boolean value, long date) {
        insertDataPoint(new DataPoint(id, value, date));
    }

    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param date
     */
    public void insertDataPoint(float value, long date) {
        insertDataPoint(new DataPoint(id, value, date));
    }

    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param date
     */
    public void insertDataPoint(int value, long date) {
        insertDataPoint(new DataPoint(id, value, date));
    }

    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param date
     */
    public void insertDataPoint(JSONObject value, long date) {
        insertDataPoint(new DataPoint(id, value, date));
    }

    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param date
     */
    public void insertDataPoint(String value, long date) {
        insertDataPoint(new DataPoint(id, value, date));
    }

    /**
     * Update RealmSensor in local database with the info of the given Sensor object. Throws an exception if it fails to updated.
     */
    protected void saveChanges() throws DatabaseHandlerException {
        realm.beginTransaction();

        // get the existing sensor (just to throw an error if it doesn't exist)
        RealmModelSensor realmSensor = realm
                .where(RealmModelSensor.class)
                .equalTo("id", this.id)
                .findFirst();

        if (realmSensor == null) {
            throw new DatabaseHandlerException("Cannot update sensor: sensor doesn't yet exist.");
        }

        // update the sensor
        realm.copyToRealmOrUpdate(RealmModelSensor.fromSensor(this));

        realm.commitTransaction();
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
     * Get data points from this sensor from the local database
     * @param startDate: Start date of the query, included.
     * @param endDate: End date of the query, excluded.
     * @param limit: The maximum number of data points.
     * @param sortOrder: Sort order, either ASC or DESC
     * @return Returns a List with data points
     */
    public List<DataPoint> getDataPoints(long startDate, long endDate, int limit, DatabaseHandler.SORT_ORDER sortOrder) throws JSONException {
        // query results
        realm.beginTransaction();
        RealmResults<RealmModelDataPoint> results = realm
                .where(RealmModelDataPoint.class)
                .equalTo("sensorId", this.id)
                .greaterThanOrEqualTo("date", startDate)
                .lessThan("date", endDate)
                .findAll();
        realm.commitTransaction();

        // sort
        boolean resultsOrder = (sortOrder == DatabaseHandler.SORT_ORDER.DESC )
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
    };
}
