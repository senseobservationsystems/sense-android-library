package nl.sense_os.datastorageengine;

import org.json.JSONException;

import java.util.List;

import nl.sense_os.service.shared.SensorDataPoint;


/**
 * DatabaseHandler handles local storage of DataPoints, Sensors, and Sources.
 *
 * Example usage:
 *
 *     DatabaseHandler databaseHandler = new RealmDatabaseHandler(getContext(), userId);
 *     Sensor sensor = source.getSensor(sensorName);
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

    enum SORT_ORDER {ASC, DESC};

    /**
     * Close the DatabaseHandler. This will neatly close the database connection to Realm.
     * @throws Throwable
     */
    void close () throws Exception;

    /**
     * Create a new Sensor and store it in the local database.
     */
    Sensor createSensor(String source, String name, SensorDataPoint.DataType dataType, SensorOptions options) throws DatabaseHandlerException;

    /**
     * Get a sensor
     * @param source  Name of the source
     * @param name	  The name of the sensor
     * @return sensor: sensor with the given sensor name and source.
     **/
    Sensor getSensor(String source, String name) throws JSONException, DatabaseHandlerException;

    /**
     * Retrieve all sensors of the current user
     * @return
     */
    List<Sensor> getSensors() throws JSONException;

    /**
     * Retrieve a list with all sources of the current user
     * @return
     */
    List<String> getSources();
}
