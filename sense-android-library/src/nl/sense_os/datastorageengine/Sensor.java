package nl.sense_os.datastorageengine;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.exceptions.RealmPrimaryKeyConstraintException;
import nl.sense_os.datastorageengine.realm.RealmDataDeletionRequest;
import nl.sense_os.datastorageengine.realm.RealmDataPoint;
import nl.sense_os.datastorageengine.realm.RealmSensor;
import nl.sense_os.util.json.JSONSchemaValidator;
import nl.sense_os.util.json.SchemaException;
import nl.sense_os.util.json.ValidationException;

public class Sensor {

    private Context mContext = null;
    private byte[] mEncryptionKey = null;
    private JSONObject mProfile = null;
    private JSONSchemaValidator mValidator = null;

    private long mId = -1;
    private String mName = null;
    private String mUserId = null;
    private String mSource = null;
    private SensorOptions mOptions = new SensorOptions();
    private boolean mRemoteDataPointsDownloaded = false;

    public Sensor(Context context, byte[] encryptionKey, long id, String name, String userId, String source, SensorOptions options, boolean remoteDataPointsDownloaded) throws SensorException, SensorProfileException, JSONException, SchemaException {
        this.mContext = context;
        this.mEncryptionKey = encryptionKey;
        this.mProfile = new SensorProfiles(context, encryptionKey).get(name);
        this.mValidator = new JSONSchemaValidator(this.mProfile);

        this.mId = id;
        this.mName = name;
        this.mUserId = userId;
        this.mSource = source;
        this.mOptions = options;
        this.mRemoteDataPointsDownloaded = remoteDataPointsDownloaded;
    }

    public long getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public String getUserId() {
        return mUserId;
    }

    public String getSource() {
        return mSource;
    }

    public JSONObject getProfile() throws SensorProfileException, JSONException {
        return mProfile;
    }

    public boolean isRemoteDataPointsDownloaded() {
        return mRemoteDataPointsDownloaded;
    }

    /**
     * Apply options for the sensor.
     * The fields in `options` which are `null` will be ignored.
     * @param options
     * @return Returns the applied options.
     */
    public SensorOptions setOptions (SensorOptions options) throws JSONException, DatabaseHandlerException {
        this.mOptions = SensorOptions.merge(this.mOptions, options);

        // store changes in the local database
        saveChanges();

        return getOptions();
    }

    /**
     * Retrieve a clone of the current options of the sensor.
     * @return options
     */
    public SensorOptions getOptions () {
        return mOptions.clone();
    }

    public void setRemoteDataPointsDownloaded(boolean remoteDataPointsDownloaded) throws DatabaseHandlerException {
        this.mRemoteDataPointsDownloaded = remoteDataPointsDownloaded;

        // store changes in the local database
        saveChanges();
    }

    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param time
     */
    public void insertOrUpdateDataPoint(Object value, long time) throws ValidationException, JSONException {
        insertOrUpdateDataPoint(new DataPoint(mId, value, time, false));
    }

    /**
     * Update RealmSensor in local database with the info of the given Sensor object. Throws an exception if it fails to updated.
     */
    protected void saveChanges() throws DatabaseHandlerException {
        Realm realm = getRealmInstance();
        try {
            realm.beginTransaction();

            // get the existing sensor (just to throw an error if it doesn't exist)
            RealmSensor realmSensor = realm
                    .where(RealmSensor.class)
                    .equalTo("id", this.mId)
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
     * @param dataPoint	A DataPoint object
     */
    protected void insertOrUpdateDataPoint (DataPoint dataPoint) throws ValidationException, JSONException {
        Realm realm = getRealmInstance();
        try {
            mValidator.validate(dataPoint.getValue());

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
        Realm realm = getRealmInstance();
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
     * DataPoints will be immediately removed locally, and an event (class RealmDataDeletionRequest)
     * is scheduled for the next synchronization round to delete them from Common Sense.
     * @param startTime The start time in epoch milliseconds
     * @param endTime The end time in epoch milliseconds
     * @trhows DatabaseHandlerException when the local deletion of sensor data for a sensor fails
     **/
    public void deleteDataPoints(Long startTime, Long endTime) throws DatabaseHandlerException {
        // create a deletion request, will be put in a queue and in the next synchronization
        // the remote data will be deleted
        DatabaseHandler databaseHandler = new DatabaseHandler(mContext, mUserId);
        databaseHandler.createDataDeletionRequest(mSource, mName, startTime, endTime);

        // Delete the data locally
        Realm realm = getRealmInstance();
        try {
            RealmResults<RealmDataPoint> results = queryFromRealm(startTime, endTime, null);
            realm.beginTransaction();
            results.clear();
            realm.commitTransaction();

        } catch (RealmPrimaryKeyConstraintException err) {
            throw new DatabaseHandlerException("Error adding delete data request for \"" + mName + "\" and mSource \"" + mSource + "\".");
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
            // find the max mId of the existing sensors
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
        Realm realm = getRealmInstance();
        try {
            RealmQuery<RealmDataPoint> query = realm
                                                    .where(RealmDataPoint.class)
                                                    .equalTo("sensorId", this.mId);
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

    /**
     * Helper function to create a new Realm instance
     * TODO: come up with a smarter way to use Realm, this is a lot of redundant code right now
     * @return Returns a new Realm instance
     */
    protected Realm getRealmInstance () {
        RealmConfiguration.Builder config = new RealmConfiguration.Builder(mContext);

        if (mEncryptionKey != null) {
            config = config.encryptionKey(mEncryptionKey);
        }

        return Realm.getInstance(config.build());
    }
}
