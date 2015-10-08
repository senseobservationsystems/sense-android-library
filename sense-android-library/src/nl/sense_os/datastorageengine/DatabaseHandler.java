package nl.sense_os.datastorageengine;

import org.json.JSONException;

import java.util.List;


/**
 * DatabaseHandler handles local storage of DataPoints and Sensors.
 *
 * Example usage:
 *
 *     DatabaseHandler databaseHandler = new RealmDatabaseHandler(getContext(), userId);
 *     Sensor sensor = databaseHandler.createSensor(sourceName,sensorName,dataType,sensorOptions);
 *
 *     sensor.insertDataPoint(1234, new Date().getTime());
 *
 *     long startDate = 1388534400000; // 2014-01-01
 *     long startDate = 1420070400000; // 2015-01-01
 *     List<DataPoint> data = sensor.getDataPoints(startDate, endDate, 1000, SORT_ORDER.ASC);
 *
 *     databaseHandler.close();
 *
 */
public interface DatabaseHandler {
    /**
     * Close the DatabaseHandler. This will neatly close the database connection to Realm.
     * @throws Throwable
     */
    void close () throws Exception;

    /**
     * Create a new Sensor and store it in the local database.
     */
    Sensor createSensor(String source, String name, SensorOptions options) throws DatabaseHandlerException, SensorException;

    /**
     * Get a sensor
     * @param source  Name of the source
     * @param name	  The name of the sensor
     * @return sensor: sensor with the given sensor name and source.
     **/
    Sensor getSensor(String source, String name) throws JSONException, DatabaseHandlerException, SensorException;

    /**
     * Retrieve all sensors connected to the given source of the current user
     * @param source the name of the source
     * @return a list of sensor attached to the given source
     */
    List<Sensor> getSensors(String source) throws JSONException, SensorException;

    /**
     * Retrieve a list with all sources of the current user
     * @return a list of source name in String
     */
    List<String> getSources();

    /**
     * Store the content of deletion request for data points in the local storage
     * @param sensorName the name of the sensor
     * @param source the source name of the sensor
     * @param startDate the start date to delete the data points
     * @param endDate the end date to delete the data points
     */
    void createDataDeletionRequest(String sensorName, String source, Long startDate, Long endDate) throws DatabaseHandlerException;

    /**
     * Get the list of data deletion requests from local storage
     * @return the list of data deletion requests
     */
    List<DataDeletionRequest> getDataDeletionRequests();

    /**
     * Delete the DataDeletionRequest from local storage by querying uuid.
     * @param uuid
     */
    void deleteDataDeletionRequest(String uuid);
}
