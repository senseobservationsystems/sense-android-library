package nl.sense_os.datastorageengine;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import nl.sense_os.service.shared.SensorDataPoint;

public interface Sensor {

    long getId();

    String getName();

    String getUserId();

    String getSource();

    String getDataType();

    boolean isRemoteDataPointsDownloaded();

    /**
     * Apply options for the sensor.
     * The fields in `options` which are `null` will be ignored.
     * @param options
     * @return Returns the applied options.
     */
    SensorOptions setOptions (SensorOptions options) throws JSONException, DatabaseHandlerException;

    /**
     * Retrieve a clone of the current options of the sensor.
     * @return options
     */
    SensorOptions getOptions ();

    void setRemoteDataPointsDownloaded(boolean remoteDataPointsDownloaded) throws DatabaseHandlerException;
    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param date
     */
    void insertOrUpdateDataPoint(Object value, long date) throws SensorException;
    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param date
     * @param exsitedInRemote
     */
    void insertOrUpdateDataPoint(Object value, long date, boolean exsitedInRemote) throws SensorException;

    /**
     * Get data points from this sensor from the local database
     * @param queryOptions: options for the query of dataPoints.
     * queryOptions.startDate: Start date of the query, included.
     * queryOptions.endDate: End date of the query, excluded.
     * queryOptions.limit: The maximum number of data points.
     * queryOptions.sortOrder: Sort order, either ASC or DESC
     * queryOptions.existsInRemote: required existsInRemote status of the dataPoint
     * queryOptions.interval: one of  the INTERVAL {MINUTE, HOUR, DAY, WEEK}
     * @return Returns a List with data points
     */
    List<DataPoint> getDataPoints(QueryOptions queryOptions) throws JSONException, DatabaseHandlerException;

    /**
     * Delete DataPoints from this sensor,
     * @param queryOptions: options for the query of dataPoints that need to be deleted.
     * queryOptions.startDate: Start date of the query, included.
     * queryOptions.endDate: End date of the query, excluded.
     * queryOptions.limit: it is not used for this method.
     * queryOptions.sortOrder: it is not used for this method.
     * queryOptions.existsInRemote: required existsInRemote status of the dataPoint
     * queryOptions.requireDeletionInCS: required requireDeletionInCS status of the dataPoint
     */
    void deleteDataPoints(QueryOptions queryOptions) throws DatabaseHandlerException;
}
