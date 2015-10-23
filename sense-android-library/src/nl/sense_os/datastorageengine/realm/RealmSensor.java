package nl.sense_os.datastorageengine.realm;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import nl.sense_os.datastorageengine.DataPoint;
import nl.sense_os.datastorageengine.DatabaseHandlerException;
import nl.sense_os.datastorageengine.QueryOptions;
import nl.sense_os.datastorageengine.Sensor;
import nl.sense_os.datastorageengine.SensorException;
import nl.sense_os.datastorageengine.SensorOptions;
import nl.sense_os.datastorageengine.SensorProfiles;
import nl.sense_os.datastorageengine.realm.model.RealmModelDataPoint;
import nl.sense_os.datastorageengine.realm.model.RealmModelSensor;

public class RealmSensor implements Sensor {

    private Realm realm = null;

    private long id = -1;
    private String name = null;
    private String userId = null;
    private String source = null;
    private SensorOptions options = new SensorOptions();
    private boolean remoteDataPointsDownloaded = false;

    public RealmSensor(Realm realm, long id, String name, String userId, String source, SensorOptions options, boolean remoteDataPointsDownloaded) throws SensorException {
        // validate if the sensor name is valid
        if (SensorProfiles.getSensorType(name) == null) {
            throw new SensorException("Unknown sensor name '" + name + "'.");
        }

        this.realm = realm;

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

    public String getDataType() {
        return SensorProfiles.getSensorType(name);
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
     * @param date
     */
    public void insertOrUpdateDataPoint(Object value, long date) throws SensorException {
        insertOrUpdateDataPoint(new DataPoint(id, value, date,false));
    }

    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param date
     * @param existsInRemote
     */
    public void insertOrUpdateDataPoint(Object value, long date, boolean existsInRemote) throws SensorException {
        insertOrUpdateDataPoint(new DataPoint(id, value, date, existsInRemote));
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
    protected void insertOrUpdateDataPoint (DataPoint dataPoint) throws SensorException {
        // validate whether the value type of dataPoint matches the data type of the sensor
        SensorProfiles.validate(name, dataPoint);

        RealmModelDataPoint realmDataPoint = RealmModelDataPoint.fromDataPoint(dataPoint);

        realm.beginTransaction();
        realm.copyToRealmOrUpdate(realmDataPoint);
        realm.commitTransaction();
    }

    /**
     * Get data points from this sensor from the local database
     * startDate: Start date of the query, included. Null means there is no check for the start Date.
     * endDate: End date of the query, excluded. Null means there is no check for the end Date.
     * limit: The maximum number of data points. Null means no limit.
     * sortOrder: Sort order, either ASC or DESC
     * existsInRemote: the field status to query. Null means the not query this field
     * interval: one of  the INTERVAL {MINUTE, HOUR, DAY, WEEK}
     * @return Returns a List with data points
     */
      public List<DataPoint> getDataPoints(QueryOptions queryOptions) throws JSONException, DatabaseHandlerException {
        if(queryOptions.getLimit() != null) {
            if(queryOptions.getLimit() <= 0) {
                throw new DatabaseHandlerException("Invalid input of limit value");
            }
        }
        // query results
        realm.beginTransaction();
        RealmResults<RealmModelDataPoint> results = queryFromRealm(queryOptions.getStartDate(), queryOptions.getEndDate(), queryOptions.getExistsInRemote());
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
    };

    /**
     * This method deletes DataPoints from the local Realm database.
     * QueryOptions.limit and QueryOptions.sortOrder are not considered during the query.
     * @param queryOptions: options for the query of dataPoints that need to be deleted.
     */
    public void deleteDataPoints(QueryOptions queryOptions) throws DatabaseHandlerException{
        RealmResults<RealmModelDataPoint> results = queryFromRealm(queryOptions.getStartDate(), queryOptions.getEndDate(), queryOptions.getExistsInRemote());
        realm.beginTransaction();
        results.clear();
        realm.commitTransaction();
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
                    .where(RealmModelSensor.class)
                    .findAll()
                    .max("id");
            realm.commitTransaction();

            auto_increment = max != null ? max.longValue() : 0;
        }

        return ++auto_increment;
    }

    protected static long auto_increment = -1; // -1 means not yet loaded

    // Helper function for getDataPoints
    private RealmResults<RealmModelDataPoint> queryFromRealm(Long startDate, Long endDate, Boolean existsInRemote) throws DatabaseHandlerException{

        RealmQuery<RealmModelDataPoint> query = realm
                                                .where(RealmModelDataPoint.class)
                                                .equalTo("sensorId", this.id);
        if(startDate != null) {
            query.greaterThanOrEqualTo("date", startDate.longValue());
        }
        if(endDate != null) {
            query.lessThan("date", endDate.longValue());
        }
        if(startDate != null && endDate != null && startDate >= endDate) {
            throw new DatabaseHandlerException("startDate is the same as or later than the endDate");
        }
        if(existsInRemote != null) {
            query.equalTo("existsInRemote", existsInRemote);
        }

        RealmResults<RealmModelDataPoint> results = query.findAll();

        return results;
    }

    // Helper function for getDataPoints
    private  List<DataPoint>  setLimitToResult(RealmResults<RealmModelDataPoint> results,Integer limit) throws JSONException {
        List<DataPoint> dataPoints = new ArrayList<>();
        Iterator<RealmModelDataPoint> iterator = results.iterator();
        int count = 0;

        if(limit == null){
            while(iterator.hasNext()) {
                dataPoints.add(RealmModelDataPoint.toDataPoint(iterator.next()));
            }
        }else{
            while(count < limit && iterator.hasNext()){
                dataPoints.add(RealmModelDataPoint.toDataPoint(iterator.next()));
                count++;
            }
        }
        return dataPoints;
    }
}