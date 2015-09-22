package nl.sense_os.datastorageengine;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import nl.sense_os.service.shared.SensorDataPoint;

public interface Sensor {

    String getId();

    String getName();

    String getUserId();

    String getSourceId();

    SensorDataPoint.DataType getdataType();

    String getCsId();

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
    void insertDataPoint(boolean value, long date);

    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param date
     */
    void insertDataPoint(float value, long date);

    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param date
     */
    void insertDataPoint(int value, long date);

    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param date
     */
    void insertDataPoint(JSONObject value, long date);

    /**
     * Insert a new DataPoint to this sensor
     * @param value
     * @param date
     */
    void insertDataPoint(String value, long date);

    /**
     * Get data points from this sensor from the local database
     * @param startDate: Start date of the query, included.
     * @param endDate: End date of the query, excluded.
     * @param limit: The maximum number of data points.
     * @param sortOrder: Sort order, either ASC or DESC
     * @return Returns a List with data points
     */
    List<DataPoint> getDataPoints(long startDate, long endDate, int limit, DatabaseHandler.SORT_ORDER sortOrder) throws JSONException;

}
