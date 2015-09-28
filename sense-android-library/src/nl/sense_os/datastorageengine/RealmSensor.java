package nl.sense_os.datastorageengine;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import nl.sense_os.service.shared.SensorDataPoint;

public class RealmSensor implements Sensor {

    private Realm realm = null;

    private long id = -1;
    private String name = null;
    private String userId = null;
    private String source = null;
    private SensorDataPoint.DataType dataType = null;
    private SensorOptions options = new SensorOptions();
    private boolean synced = false;

    protected RealmSensor(Realm realm, long id, String name, String userId, String source, SensorDataPoint.DataType dataType, SensorOptions options, boolean synced) {
        this.realm = realm;

        this.id = id;
        this.name = name;
        this.userId = userId;
        this.source = source;
        this.dataType = dataType;
        this.options = options;
        this.synced = synced;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUserId() {
        return userId;
    }

    public String getSource() {
        return source;
    }

    public SensorDataPoint.DataType getDataType() {
        return dataType;
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
        this.synced = false; // mark as dirty    TODO: is it desired behavior to set synced to false?

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
    public void insertOrUpdateDataPoint(boolean value, long date) {
        insertOrUpdateDataPoint(new DataPoint(id, value, date));
    }

    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param date
     */
    public void insertOrUpdateDataPoint(float value, long date) {
        insertOrUpdateDataPoint(new DataPoint(id, value, date));
    }

    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param date
     */
    public void insertOrUpdateDataPoint(int value, long date) {
        insertOrUpdateDataPoint(new DataPoint(id, value, date));
    }

    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param date
     */
    public void insertOrUpdateDataPoint(JSONObject value, long date) {
        insertOrUpdateDataPoint(new DataPoint(id, value, date));
    }

    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param date
     */
    public void insertOrUpdateDataPoint(String value, long date) {
        insertOrUpdateDataPoint(new DataPoint(id, value, date));
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
    protected void insertOrUpdateDataPoint (DataPoint dataPoint) {
        RealmModelDataPoint realmDataPoint = RealmModelDataPoint.fromDataPoint(dataPoint);

        realm.beginTransaction();
        realm.copyToRealmOrUpdate(realmDataPoint);
        realm.commitTransaction();
    }

    /**
     * Get data points from this sensor from the local database
     * @param startDate: Start date of the query, included. Null means there is no check for the start Date.
     * @param endDate: End date of the query, excluded. Null means there is no check for the end Date.
     * @param limit: The maximum number of data points. Null means no limit.
     * @param sortOrder: Sort order, either ASC or DESC
     * @return Returns a List with data points
     */
    public List<DataPoint> getDataPoints(Long startDate, Long endDate, Integer limit, DatabaseHandler.SORT_ORDER sortOrder) throws JSONException, DatabaseHandlerException {
        if(limit != null && limit <= 0)
            throw new DatabaseHandlerException("Invalid input of limit value");

        // query results
        realm.beginTransaction();
        RealmResults<RealmModelDataPoint> results = queryFromRealm(startDate, endDate);
        realm.commitTransaction();

        // sort
        boolean resultsOrder = (sortOrder == DatabaseHandler.SORT_ORDER.DESC )
                ? RealmResults.SORT_ORDER_DESCENDING
                : RealmResults.SORT_ORDER_ASCENDING;
        results.sort("date", resultsOrder);

        // limit and convert to DataPoint
        List<DataPoint> dataPoints = setLimitToResult(results, limit);
        // TODO: figure out what is the most efficient way to loop over the results
        return dataPoints;
    };

    /**
     * Generate an auto incremented id for a new Sensor
     * @param realm  Realm instance. Only used the first time to determine the max id
     *               of the existing sensors.
     * @return Returns an auto incremented id
     */
    public static synchronized long generateId (Realm realm) {
        if (auto_increment == -1) {
            // find the max id of the existing sensors
            realm.beginTransaction();
            auto_increment = realm
                    .where(RealmModelSensor.class)
                    .findAll()
                    .max("id")
                    .longValue();
            realm.commitTransaction();
        }

        return ++auto_increment;
    }

    protected static long auto_increment = -1; // -1 means not yet loaded

    // Helper function for getDataPoints
    private RealmResults<RealmModelDataPoint> queryFromRealm(Long startDate, Long endDate){

        RealmQuery<RealmModelDataPoint> query = realm
                                                .where(RealmModelDataPoint.class)
                                                .equalTo("sensorId", this.id);
        if(startDate != null)
            query.greaterThanOrEqualTo("date", startDate.longValue());
        if(endDate != null)
            query.lessThan("date", endDate.longValue());

        RealmResults<RealmModelDataPoint> results = query.findAll();

        return results;
    }

    // Helper function for getDataPoints
    private  List<DataPoint> setLimitToResult(RealmResults<RealmModelDataPoint> results,Integer limit){
        List<DataPoint> dataPoints = new ArrayList<>();
        Iterator<RealmModelDataPoint> iterator = results.iterator();
        int count = 0;

        if(limit == null){
            while(iterator.hasNext())
                dataPoints.add(RealmModelDataPoint.toDataPoint(iterator.next()));
        }else{
            while(count < limit && iterator.hasNext()){
                dataPoints.add(RealmModelDataPoint.toDataPoint(iterator.next()));
                count++;
            }
        }
        return dataPoints;
    }
}
