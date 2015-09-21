package nl.sense_os.datastorageengine;

import org.json.JSONException;

import java.util.List;


/**
 * DatabaseHandler handles local storage of DataPoints, Sensors, and Sources.
 *
 * Example usage:
 *
 *     DatabaseHandler dh = new RealmDatabaseHandler(getContext());
 *
 *     DataPoint dataPoint = new DataPoint("mysensor", 1234, new Date().getTime());
 *     dh.insertDataPoint(dataPoint);
 *
 *     long startDate = 1388534400000; // 2014-01-01
 *     long startDate = 1420070400000; // 2015-01-01
 *     List<DataPoint> data = dh.getDataPoints("mysensor", startDate, endDate, 1000, SORT_ORDER.ASC);
 *
 *     dh.close();
 *
 */
public interface DatabaseHandler {

    public enum SORT_ORDER {ASC, DESC};

    /**
     * Close the DatabaseHandler. This will neatly close the database connection to Realm.
     * @throws Throwable
     */
    public void close () throws Exception;

    /**
     * This method inserts a DataPoint object into the local Realm database.
     * The typecasting to string should already be done at this point.
     * @param dataPoint	A DataPoint object that has a stringified value that will be copied
     * 			into a Realm object.
     */
    public void insertDataPoint (DataPoint dataPoint);

    /**
     * Get data points from the local database with the given sensor id.
     * @param sensorId: String for the sensorID of the sensor that the data point belongs to.
     * @param startDate: Start date of the query, included.
     * @param endDate: End date of the query, excluded.
     * @param limit: The maximum number of data points.
     * @param sortOrder: Sort order, either ASC or DESC
     * @return Returns a List with data points
     */
    public List<DataPoint> getDataPoints(String sensorId, long startDate, long endDate, int limit, SORT_ORDER sortOrder) throws JSONException;

    /**
     * Store a new sensor in the local database
     * @param sensor
     */
    public void insertSensor(Sensor sensor);

    /**
     * Update RealmSensor in local database with the info of the given Sensor object. Throws an exception if it fails to updated.
     * @param sensor: Sensor object containing the updated info.
     */
    public void updateSensor(Sensor sensor) throws DatabaseHandlerException;

    /**
     * Get a sensor
     * @param sourceId	The ID of the source object or Null
     * @param sensorName	The name of the sensor or Null
     * @return sensor: sensor with the given sensor name and sourceId.
     **/
    public Sensor getSensor(String sourceId, String sensorName) throws JSONException;

    /**
     * Retrieve all sensors for given source id.
     * @param sourceId
     * @return
     */
    public List<Sensor> getSensors(String sourceId) throws JSONException;


    /**
     * Store a new source in the local database
     * @param source
     */
    public void insertSource(Source source);

    /**
     * Update Source in database with the info of the given Source object.
     * Throws an exception if it fails to updated.
     * @param source: Source object containing the updated info.
     */
    public void updateSource(Source source) throws DatabaseHandlerException;

    /**
     * Returns a list of sources based on the specified criteria.
     * @param sourceName    Name of the source
     * @param uuid          Device identifier
     * @return list of source objects that correspond to the specified criteria.
     */
    public List<Source> getSources (String sourceName, String uuid) throws JSONException;
}
