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

    SensorDataPoint.DataType getDataType();

    boolean isSynced();

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

    void setSynced(boolean synced) throws DatabaseHandlerException;

    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param date
     */
    void insertOrUpdateDataPoint(boolean value, long date);

    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param date
     */
    void insertOrUpdateDataPoint(float value, long date);

    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param date
     */
    void insertOrUpdateDataPoint(int value, long date);

    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param date
     */
    void insertOrUpdateDataPoint(JSONObject value, long date);

    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param date
     */
    void insertOrUpdateDataPoint(String value, long date);

    /**
     * Get data points from this sensor from the local database
     * @param startDate: Start date of the query, included.
     * @param endDate: End date of the query, excluded.
     * @param limit: The maximum number of data points.
     * @param sortOrder: Sort order, either ASC or DESC
     * @return Returns a List with data points
     */
    List<DataPoint> getDataPoints(Long startDate, Long endDate, Integer limit, DatabaseHandler.SORT_ORDER sortOrder) throws JSONException, DatabaseHandlerException;

}
