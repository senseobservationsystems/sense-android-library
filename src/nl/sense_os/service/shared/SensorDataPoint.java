package nl.sense_os.service.shared;

import java.util.ArrayList;

import org.json.JSONObject;

import android.hardware.SensorEvent;

/**
 * Generic holder for sensor data.
 * 
 * @author Ted Schmidt <ted@sense-os.nl>
 */
public class SensorDataPoint {

    public enum DataType {
        INT, FLOAT, BOOL, DOUBLE, STRING, ARRAYLIST, JSON, JSONSTRING, FILE, SENSOREVENT
    };

    /**
     * Name of the sensor that produced the data point.
     */
    public String sensorName;
    /**
     * Description of the sensor that produces the data point. Corresponds to the "sensor_type"
     * field in the CommonSense API.
     */
    public String sensorDescription;
    /**
     * Time stamp of the data point.
     */
    public long timeStamp;
    private DataType dataType;
    private Object value;

    /**
     * @see #SensorDataPoint(int)
     */
    public SensorDataPoint(ArrayList<SensorDataPoint> value) {
        dataType = DataType.ARRAYLIST;
        this.value = value;
    }

    /**
     * @see #SensorDataPoint(int)
     */
    public SensorDataPoint(Boolean value) {
        dataType = DataType.BOOL;
        this.value = value;
    }

    /**
     * @see #SensorDataPoint(int)
     */
    public SensorDataPoint(double value) {
        dataType = DataType.DOUBLE;
        this.value = value;
    }

    /**
     * @see #SensorDataPoint(int)
     */
    public SensorDataPoint(float value) {
        dataType = DataType.FLOAT;
        this.value = value;
    }

    /**
     * Sets the value of the SensorDataPoint based on the input value the dataType. The dataType can
     * be changed by {@link #setDataType(DataType)}.
     * 
     * @param value
     *            The input value which is stored in the SensorDataPoint
     */
    public SensorDataPoint(int value) {
        dataType = DataType.INT;
        this.value = value;
    }

    /**
     * @see #setValue(int)
     */
    public SensorDataPoint(JSONObject value) {
        dataType = DataType.JSON;
        this.value = value;
    }

    /**
     * @see #setValue(int)
     */
    public SensorDataPoint(SensorEvent value) {
        dataType = DataType.SENSOREVENT;
        this.value = value;
    }

    /**
     * @see #setValue(int)
     */
    public SensorDataPoint(String value) {
        dataType = DataType.STRING;
        this.value = value;
    }

    /**
     * @see #getIntValue()
     */
    @SuppressWarnings("unchecked")
    public ArrayList<SensorDataPoint> getArrayValue() {
        return (ArrayList<SensorDataPoint>) value;
    }

    /**
     * @see #getIntValue()
     */
    public Boolean getBoolValue() {
        return (Boolean) value;
    }

    /**
     * Returns the dataType of the SensorDataPoint
     * 
     * @return DataTypes
     */
    public DataType getDataType() {
        return dataType;
    }

    /**
     * @see #getIntValue()
     */
    public double getDoubleValue() {
        return (Double) value;
    }

    /**
     * @see #getIntValue()
     */
    public float getFloatValue() {
        return (Float) value;
    }

    /**
     * Return the value of the SensorDataPoint
     * 
     * @return value
     */
    public int getIntValue() {
        return (Integer) value;
    }

    /**
     * @see #getIntValue()
     */
    public JSONObject getJSONValue() {
        return (JSONObject) value;
    }

    /**
     * @see #getIntValue()
     */
    public SensorEvent getSensorEventValue() {
        return (SensorEvent) value;
    }

    /**
     * @see #getIntValue()
     */
    public String getStringValue() {
        return (String) value;
    }

    /**
     * Sets the data type.
     * 
     * This method overrides the dataType which has been set automatically. This method can be used
     * when a more precise dataType like FILE is needed.
     * 
     * @param dataType
     *            The dataType of the SensorDataPoint
     */
    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }
}
