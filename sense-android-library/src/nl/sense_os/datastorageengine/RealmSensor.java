package nl.sense_os.datastorageengine;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import nl.sense_os.service.shared.SensorDataPoint;

public class RealmSensor implements Sensor {

    private RealmDatabaseHandler databaseHandler = null; // used to insert/get datapoints, and to update itself

    private String id = null;
    private String name = null;
    private String userId = null;
    private String sourceId = null;
    private SensorDataPoint.DataType dataType = null;
    private String csId = null;
    private SensorOptions options = new SensorOptions();
    private boolean synced = false;

    public RealmSensor(RealmDatabaseHandler databaseHandler, String id, String name, String userId, String sourceId, SensorDataPoint.DataType dataType, String csId, SensorOptions options, boolean synced) {
        this.databaseHandler = databaseHandler;

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
     */
    public void setOptions (SensorOptions options) throws JSONException, DatabaseHandlerException {
        this.options = SensorOptions.merge(this.options, options);
        this.synced = false; // mark as dirty

        // store changes in the local database
        databaseHandler.updateSensor(this);
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
        databaseHandler.updateSensor(this);
    }

    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param date
     */
    public void insertDataPoint(boolean value, long date) {
        databaseHandler.insertDataPoint(new DataPoint(id, value, date));
    }

    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param date
     */
    public void insertDataPoint(float value, long date) {
        databaseHandler.insertDataPoint(new DataPoint(id, value, date));
    }

    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param date
     */
    public void insertDataPoint(int value, long date) {
        databaseHandler.insertDataPoint(new DataPoint(id, value, date));
    }

    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param date
     */
    public void insertDataPoint(JSONObject value, long date) {
        databaseHandler.insertDataPoint(new DataPoint(id, value, date));
    }

    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param date
     */
    public void insertDataPoint(String value, long date) {
        databaseHandler.insertDataPoint(new DataPoint(id, value, date));
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
        return databaseHandler.getDataPoints(id, startDate, endDate, limit, sortOrder);
    };

}
