package nl.sense_os.datastorageengine;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import nl.sense_os.service.shared.SensorDataPoint;

public class RealmSource implements Source {

    private RealmDatabaseHandler databaseHandler = null;

    private String id = null;
    private String name = null;
    private JSONObject meta = null;
    private String deviceId = null; // device UUID or some other UUID. TODO: rename to deviceId?
    private String userId = null;
    private String csId = null;
    private boolean synced = false;

    protected RealmSource(RealmDatabaseHandler databaseHandler, String id, String name, JSONObject meta, String deviceId, String userId, String csId, boolean synced) {
        this.databaseHandler = databaseHandler;

        this.id = id;
        this.name = name;
        this.meta = meta;
        this.deviceId = deviceId;
        this.userId = userId;
        this.csId = csId;
        this.synced = synced;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) throws DatabaseHandlerException {
        this.userId = userId;
        this.synced = false; // mark as dirty

        databaseHandler.updateSource(this);
    }

    public String getCsId() {
        return csId;
    }

    public void setCsId(String csId) throws DatabaseHandlerException {
        this.csId = csId;
        this.synced = false; // mark as dirty

        databaseHandler.updateSource(this);
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) throws DatabaseHandlerException {
        this.deviceId = deviceId;
        this.synced = false; // mark as dirty

        databaseHandler.updateSource(this);
    }

    public JSONObject getMeta() {
        return meta;
    }

    public void setMeta(JSONObject meta) throws DatabaseHandlerException {
        this.meta = meta;
        this.synced = false; // mark as dirty

        databaseHandler.updateSource(this);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) throws DatabaseHandlerException {
        this.name = name;
        this.synced = false; // mark as dirty

        databaseHandler.updateSource(this);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) throws DatabaseHandlerException {
        this.id = id;
        this.synced = false; // mark as dirty

        databaseHandler.updateSource(this);
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) throws DatabaseHandlerException {
        this.synced = synced;

        databaseHandler.updateSource(this);
    }

    @Override
    public Sensor createSensor(String id, String name, String userId, SensorDataPoint.DataType dataType, String csId, SensorOptions options) {
        boolean synced = false;
        return databaseHandler.createSensor(id, name, userId, this.id, dataType, csId, options, synced);
    }

    @Override
    public Sensor getSensor(String sensorName) throws JSONException {
        return databaseHandler.getSensor(this.id, sensorName);
    }

    @Override
    public List<Sensor> getSensors() throws JSONException {
        return databaseHandler.getSensors(this.id);
    }

}
