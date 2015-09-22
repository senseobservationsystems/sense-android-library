package nl.sense_os.datastorageengine;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import nl.sense_os.service.shared.SensorDataPoint;

public interface Source {

    String getUserId();

    void setUserId(String userId) throws DatabaseHandlerException;

    String getCsId();

    void setCsId(String csId) throws DatabaseHandlerException;

    String getDeviceId();

    void setDeviceId(String deviceId) throws DatabaseHandlerException;

    JSONObject getMeta();

    void setMeta(JSONObject meta) throws DatabaseHandlerException;

    String getName();

    void setName(String name) throws DatabaseHandlerException;

    String getId();

    void setId(String id) throws DatabaseHandlerException;

    boolean isSynced();

    void setSynced(boolean synced) throws DatabaseHandlerException;

    /**
     * Create a new Sensor and store it in the local database
     */
    Sensor createSensor(String id, String name, String userId, SensorDataPoint.DataType dataType, String csId, SensorOptions options) throws DatabaseHandlerException;

    /**
     * Get a sensor
     * @param sensorName	The name of the sensor or Null
     * @return sensor: sensor with the given sensor name and sourceId.
     **/
    Sensor getSensor(String sensorName) throws JSONException, DatabaseHandlerException;

    /**
     * Retrieve all sensors for given source id.
     * @return
     */
    List<Sensor> getSensors() throws JSONException;

}
