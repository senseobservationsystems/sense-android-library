package nl.sense_os.datastorageengine;

import java.util.Date;
import java.util.List;

public class DatabaseHandler {
    /**
     * Add a data point to the sensor with the given sensorId.
     * Throw exceptions if it fails to add the data point.
     * @param sensorId    The id of the sensor where to add the dataPoint
     * @param value        value for the data point
     * @param date         time for the data point
     */
    public void addDataPoint (String sensorId, Object value, Date date) {
        // TODO: implement
    }

    /**
     * Get data points from the sensor with the given sensor id.
     * @param sensorId: String for the sensorID of the sensor that the data point belongs to.
     * @param startDate: Start date of the query.
     * @param endDate: End date of the query.
     * @param limit: The maximum number of data points.
     * @param sortOrder: Sort order, either "ASC" or "DESC"
     * @return datapoints: An array of NSDictionary represents data points.
     */
    public List<DataPoint> getDataPoints(String sensorId, Date startDate, Date endDate, int limit, String sortOrder) {
        // TODO: implement
        return null;
    }

    /**
     * Set the meta data and the options for enabling data upload/download to Common Sense and local data persistence.
     *
     * @param sensorId: String for the sensorID of the sensor that the new setting should be applied to.
     * @param options: Sensor options.
     */
    public void setSensorOptions(String sensorId, SensorOptions options) {
        // TODO: implement
    }

    /**
     * Store a new sensor in the local database
     * @param sensor
     */
    public void createSensor(Sensor sensor) {
        // TODO: implement
    }

    /**
     * Retrieve a sensor by its name.
     * @param sourceId
     * @param sensorName
     * @return
     */
    public Sensor getSensor(String sourceId, String sensorName) {
        //TODO: implement
        return null;
    }

    /**
     * Retrieve all sensors for given source id.
     * @param sourceId
     * @return
     */
    public List<Sensor> getSensors(String sourceId) {
        //TODO: implement
        return null;
    }

    /**
     * Store a new source in the local database
     * @param source
     */
    public void createSource(Source source) {
        // TODO: implement
    }

    /**
     * Retrieve a source by it's id
     * @param sourceId
     * @return
     */
    public Source getSource (String sourceId) {
        // TODO: implement
        return null;
    }

    /**
     * Retrieve all sources
     * @return
     */
    public List<Source> getSources () {
        // TODO: implement
        return null;
    }
}
