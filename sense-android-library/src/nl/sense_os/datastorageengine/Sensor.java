package nl.sense_os.datastorageengine;

import org.json.JSONException;
import org.json.JSONObject;

import nl.sense_os.service.shared.SensorDataPoint;

public class Sensor {

    private String id = null;
    private String name = null;
    private String userId = null;
    private String sourceId = null;
    private SensorDataPoint.DataType dataType = null;
    private String csId = null;
    private SensorOptions options = new SensorOptions();
    private Boolean synced = null;

    public Sensor(String id, String name, String userId, String sourceId, SensorDataPoint.DataType dataType, String csId, SensorOptions options, boolean synced) {
        this.id = id;
        this.name = name;
        this.userId = userId;
        this.sourceId = sourceId;
        this.dataType = dataType;
        this.csId = csId;
        this.options = options;
        this.synced = synced;
    }

    // TODO: implement method to add a DataPoint

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUserId() {
        return userId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public SensorDataPoint.DataType getdataType() {
        return dataType;
    }

    public String getCsId() {
        return csId;
    }

    public Boolean isSynced() {
        return synced;
    }

    /**
     * Apply options for the sensor.
     * The fields in `options` which are `null` will be ignored.
     * @param options
     */
    public void setOptions (SensorOptions options) throws JSONException {
        this.options = SensorOptions.merge(this.options, options);
    }

    /**
     * Retrieve a clone of the current options of the sensor.
     * @return options
     */
    public SensorOptions getOptions () {
        return options.clone();
    }

    public void setSynced(Boolean synced) {
        this.synced = synced;
    }
}
