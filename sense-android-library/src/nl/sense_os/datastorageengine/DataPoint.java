package nl.sense_os.datastorageengine;

/**
 * A DataPoint can hold a value and date of a specific sensor
 *
 * Usage:
 *
 *     DataPoint<String> point =
 *         new DataPoint<String>("sensor_x", "Foo", new Date().getTime())
 *
 * @param <T> The value type: String, Float, Boolean, Integer, or JSONObject.
 */
public class DataPoint<T> {

    private String id = null;
    private String sensorId = null;
    private T value;
    private long date = 0;
    private boolean synced = false;

    public DataPoint(String sensorId, T value, long date) {
        setSensorId(sensorId);
        setValue(value);
        setDate(date);
    }

    public DataPoint(String sensorId, T value, long date, boolean synced) {
        setSensorId(sensorId);
        setDate(date);
        setValue(value);
        setSynced(synced);
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

    public void setValue(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
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
