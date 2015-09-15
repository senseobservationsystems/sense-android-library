package nl.sense_os.datastorageengine;

import java.util.UUID;

public class DataPoint {

    private String id = null;
    private String sensorId = null;
    private Object value = null;
    private long date = 0;
    private boolean synced = false;

    public DataPoint(String sensorId, Object value, long date) {
        this.id = UUID.randomUUID().toString();
        this.sensorId = sensorId;
        this.value = value;
        this.date = date;
    }

    public DataPoint(String id, String sensorId, Object value, long date) {
        this.id = id;
        this.sensorId = sensorId;
        this.value = value;
        this.date = date;
    }

    public DataPoint(String sensorId, Object value, long date, boolean synced) {
        this.sensorId = sensorId;
        this.value = value;
        this.date = date;
        this.synced = synced;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }

}
