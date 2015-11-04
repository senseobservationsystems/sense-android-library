package nl.sense_os.datastorageengine;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.exceptions.RealmPrimaryKeyConstraintException;
import nl.sense_os.datastorageengine.realm.RealmDataPoint;
import nl.sense_os.datastorageengine.realm.RealmSensor;

public class Sensor {

    private Context context = null;
    private SensorProfiles profiles = null;

    private long id = -1;
    private String name = null;
    private String userId = null;
    private String source = null;
    private SensorOptions options = new SensorOptions();
    private boolean remoteDataPointsDownloaded = false;

    public Sensor(Context context, long id, String name, String userId, String source, SensorOptions options, boolean remoteDataPointsDownloaded) throws SensorException {
        this.context = context;
        this.profiles = SensorProfiles.getInstance(context);

        // validate if the sensor name is valid
        if (!profiles.hasSensorProfile(name)) {
            throw new SensorException("Unknown sensor name '" + name + "'.");
        }

        this.id = id;
        this.name = name;
        this.userId = userId;
        this.source = source;
        this.options = options;
        this.remoteDataPointsDownloaded = remoteDataPointsDownloaded;
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

    public JSONObject getProfile() throws SensorProfileException, JSONException {
        return profiles.getSensorProfile(name);
    }

    public boolean isRemoteDataPointsDownloaded() {
        return remoteDataPointsDownloaded;
    }

    /**
     * Apply options for the sensor.
     * The fields in `options` which are `null` will be ignored.
     * @param options
     * @return Returns the applied options.
     */
    public SensorOptions setOptions (SensorOptions options) throws JSONException, DatabaseHandlerException {
        this.options = SensorOptions.merge(this.options, options);

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

    public void setRemoteDataPointsDownloaded(boolean remoteDataPointsDownloaded) throws DatabaseHandlerException {
        this.remoteDataPointsDownloaded = remoteDataPointsDownloaded;

        // store changes in the local database
        saveChanges();
    }

    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param time
     */
    public void insertOrUpdateDataPoint(Object value, long time) throws SensorProfileException, JSONException {
        insertOrUpdateDataPoint(new DataPoint(id, value, time, false));
    }

    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param time
     * @param existsInRemote
     */
    public void insertOrUpdateDataPoint(Object value, long time, boolean existsInRemote) throws SensorProfileException, JSONException {
        insertOrUpdateDataPoint(new DataPoint(id, value, time, existsInRemote));
    }

    /**
     * Update RealmSensor in local database with the info of the given Sensor object. Throws an exception if it fails to updated.
     */
    protected void saveChanges() throws DatabaseHandlerException {
        Realm realm = Realm.getInstance(context);
        try {
            realm.beginTransaction();

            // get the existing sensor (just to throw an error if it doesn't exist)
            RealmSensor realmSensor = realm
                    .where(RealmSensor.class)
                    .equalTo("id", this.id)
                    .findFirst();

            if (realmSensor == null) {
                throw new DatabaseHandlerException("Cannot update sensor: sensor doesn't yet exist.");
            }

            // update the sensor
            realm.copyToRealmOrUpdate(RealmSensor.fromSensor(this));

            realm.commitTransaction();
        }
        finally {
            realm.close();
        }
    }

    /**
     * This method inserts a DataPoint object into the local Realm database.
     * The typecasting to string should already be done at this point.
     * @param dataPoint	A DataPoint object that has a stringified value that will be copied
     * 			into a Realm object.
     */
    protected void insertOrUpdateDataPoint (DataPoint dataPoint) throws SensorProfileException, JSONException {
        Realm realm = Realm.getInstance(context);
        try {
            // validate whether the value type of dataPoint matches the data type of the sensor
            profiles.validate(name, dataPoint);

            RealmDataPoint realmDataPoint = RealmDataPoint.fromDataPoint(dataPoint);

            realm.beginTransaction();
            realm.copyToRealmOrUpdate(realmDataPoint);
            realm.commitTransaction();
        }
        finally {
            realm.close();
        }
    }

    /**
     * Get data points from this sensor from the local database
     * startTime: Start date of the query, included. Null means there is no check for the start Date.
     * endTime: End date of the query, excluded. Null means there is no check for the end Date.
     * limit: The maximum number of data points. Null means no limit.
     * sortOrder: Sort order, either ASC or DESC
     * existsInRemote: the field status to query. Null means the not query this field
     * interval: one of  the INTERVAL {MINUTE, HOUR, DAY, WEEK}
     * @return Returns a List with data points
     */
    public List<DataPoint> getDataPoints(QueryOptions queryOptions) throws JSONException, DatabaseHandlerException {
        // query results
        Realm realm = Realm.getInstance(context);
        try {
            if(queryOptions.getLimit() != null) {
                if(queryOptions.getLimit() <= 0) {
                    throw new DatabaseHandlerException("Invalid input of limit value");
                }
            }

            realm.beginTransaction();
            RealmResults<RealmDataPoint> results = queryFromRealm(queryOptions.getStartTime(), queryOptions.getEndTime(), queryOptions.getExistsInRemote());
            realm.commitTransaction();

            // sort
            boolean resultsOrder = (queryOptions.getSortOrder() == QueryOptions.SORT_ORDER.DESC )
                    ? RealmResults.SORT_ORDER_DESCENDING
                    : RealmResults.SORT_ORDER_ASCENDING;
            results.sort("date", resultsOrder);

            // limit and convert to DataPoint
            List<DataPoint> dataPoints = setLimitToResult(results, queryOptions.getLimit());
            // TODO: figure out what is the most efficient way to loop over the results

            return dataPoints;
        }
        finally {
            realm.close();
        }
    };

    /**
     * Delete data from the Local Storage and Common Sense for this sensor
     * DataPoints will be immediately removed locally, and an event (class DataDeletionRequest)
     * is scheduled for the next synchronization round to delete them from Common Sense.
     * @param startTime The start time in epoch milliseconds
     * @param endTime The end time in epoch milliseconds
     * @trhows DatabaseHandlerException when the local deletion of sensor data for a sensor fails
     **/
    public void deleteDataPoints(Long startTime, Long endTime) throws DatabaseHandlerException {
        Realm realm = Realm.getInstance(context);
        try {
            Long startTimeRequest = startTime == null? -1l: startTime;
            Long endTimeRequest  = endTime == null? -1l: startTime;
            // Add the delete data request for deleting data from the backend
            DataDeletionRequest dataDeletionRequest = new DataDeletionRequest(userId, name, source, startTimeRequest, endTimeRequest);

            realm.beginTransaction();
            realm.copyToRealm(dataDeletionRequest);
            realm.commitTransaction();

            // Delete the local data
            RealmResults<RealmDataPoint> results = queryFromRealm(startTime, endTime, null);

            realm.beginTransaction();
            results.clear();
            realm.commitTransaction();

        } catch (RealmPrimaryKeyConstraintException err) {
            throw new DatabaseHandlerException("Error adding delete data request for \"" + name + "\" and source \"" + source + "\".");
        }
        finally {
            realm.close();
        }
    }

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
            Number max = realm
                    .where(RealmSensor.class)
                    .findAll()
                    .max("id");
            realm.commitTransaction();

            auto_increment = max != null ? max.longValue() : 0;
        }

        return ++auto_increment;
    }

    protected static long auto_increment = -1; // -1 means not yet loaded

    // Helper function for getDataPoints
    private RealmResults<RealmDataPoint> queryFromRealm(Long startTime, Long endTime, Boolean existsInRemote) throws DatabaseHandlerException{
        Realm realm = Realm.getInstance(context);
        try {
            RealmQuery<RealmDataPoint> query = realm
                                                    .where(RealmDataPoint.class)
                                                    .equalTo("sensorId", this.id);
            if(startTime != null) {
                query.greaterThanOrEqualTo("date", startTime.longValue());
            }
            if(endTime != null) {
                query.lessThan("date", endTime.longValue());
            }
            if(startTime != null && endTime != null && startTime >= endTime) {
                throw new DatabaseHandlerException("startTime is the same as or later than the endTime");
            }
            if(existsInRemote != null) {
                query.equalTo("existsInRemote", existsInRemote);
            }

            RealmResults<RealmDataPoint> results = query.findAll();

            return results;
        }
        finally {
            realm.close();
        }
    }

    // Helper function for getDataPoints
    private  List<DataPoint>  setLimitToResult(RealmResults<RealmDataPoint> results,Integer limit) throws JSONException {
        List<DataPoint> dataPoints = new ArrayList<DataPoint>();
        Iterator<RealmDataPoint> iterator = results.iterator();
        int count = 0;

        if(limit == null){
            while(iterator.hasNext()) {
                dataPoints.add(RealmDataPoint.toDataPoint(iterator.next()));
            }
        }else{
            while(count < limit && iterator.hasNext()){
                dataPoints.add(RealmDataPoint.toDataPoint(iterator.next()));
                count++;
            }
        }
        return dataPoints;
    }
}
