package nl.sense_os.datastorageengine;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import nl.sense_os.service.shared.SensorDataPoint;


/**
 * DatabaseHandler handles local storage of DataPoints, Sensors, and Sources.
 *
 * Example usage:
 *
 *     DatabaseHandler databaseHandler = new RealmDatabaseHandler(getContext());
 *
 *     Sensor sensor = databaseHandler.getSensor(sourceId, sensorName);
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
     * Create a new source and store it in the local database
     */
    Source createSource(String id, String name, JSONObject meta, String deviceId, String userId, String csId, boolean synced);

    /**
     * Returns a list of sources based on the specified criteria.
     * @param sourceName    Name of the source
     * @param deviceId      Device identifier
     * @return list of source objects that correspond to the specified criteria.
     */
    List<Source> getSources (String sourceName, String deviceId) throws JSONException;
}
