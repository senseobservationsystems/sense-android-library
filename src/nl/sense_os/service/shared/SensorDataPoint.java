package nl.sense_os.service.shared;

import java.util.ArrayList;
import org.json.JSONObject;
import android.hardware.SensorEvent;

/**
 * This class implements a generic holder for sensor data.
 * 
 * @author Ted Schmidt <ted@sense-os.nl>
 *
 */
public class SensorDataPoint {

	public enum DataType
	{
		INT, 
		FLOAT,
		BOOL,
		DOUBLE, 
		STRING,
		ARRAYLIST,
		JSON,
		JSONSTRING,
		FILE,		
		SENSOREVENT		
	};
	
	public String sensorName;
	public String sensorDescription;	
	private DataType dataType;
	public long timeStamp;	
	private Object value;
	
	/**
	 * Set the value
	 * 
	 * This methods sets the value of the SensorDataPoint
	 * Based on the input value the dataType of the SensorDataPoint is set.
	 * This dataType can be changed by setDataType
	 * @param value
	 * 			The input value which is stored in the SensorDataPoint
	 */
	public SensorDataPoint(int value)
	{
		dataType = DataType.INT;
		this.value = value;
	}
	
	/**
	 * @see #setValue(int)
	 */
	public SensorDataPoint(float value)
	{
		dataType = DataType.FLOAT;
		this.value = value;
	}
	
	/**
	 * @see #setValue(int)
	 */
	public SensorDataPoint(double value)
	{
		//TODO: add DOUBLE to the DataTypes
		dataType = DataType.FLOAT;
		this.value = value;
	}
		
	/**
	 * @see #setValue(int)
	 */
	public SensorDataPoint(String value)
	{
		dataType = DataType.STRING;
		this.value = value;
	}
	
	
	/**
	 * @see #setValue(int)
	 */
	public SensorDataPoint(Boolean value)
	{
		dataType = DataType.BOOL;
		this.value = value;
	}
	
	
	/**
	 * @see #setValue(int)
	 */
	public SensorDataPoint(ArrayList<SensorDataPoint> value)
	{
		dataType = DataType.ARRAYLIST;
		this.value = value;
	}

	
	/**
	 * @see #setValue(int)
	 */
	public SensorDataPoint(SensorEvent value)
	{
		dataType = DataType.SENSOREVENT;
		this.value = value;
	}

	/**
	 * @see #setValue(int)
	 */
	public SensorDataPoint(JSONObject value)
	{
		dataType = DataType.JSON;
		this.value = value;
	}
	
	/**
	 * Set the dataType
	 * 
	 * This method overrides the dataType which has been set automatically.
	 * This method can be used when a more precise dataType like FILE is needed. 
	 * 
	 * @param dataType
	 * 		The dataType of the SensorDataPoint
	 */
	public void setDataType(DataType dataType)
	{		
		this.dataType = dataType;
	}
	

	/**
	 * Returns the dataType of the SensorDataPoint
	 * 
	 * @return DataTypes
	 */	
	public DataType getDataType()
	{
		return dataType;
	}
	

	/**
	 * Return the value of the SensorDataPoint
	 * @return value
	 */
	public int getIntValue()
	{
		return (Integer)value;
	}
	
	
	/**
	 * @see #getIntValue()	
	 */
	public String getStringValue()
	{
		return (String)value;
	}
	
	
	/**
	 * @see #getIntValue()	
	 */
	public float getFloatValue()
	{
		return (Float)value;
	}
	
	
	/**
	 * @see #getIntValue()	
	 */
	public double getDoubleValue()
	{
		return (Double)value;
	}
	
	
	/**
	 * @see #getIntValue()	
	 */
	public Boolean getBoolValue()
	{
		return (Boolean)value;
	}	
	
	/**
	 * @see #getIntValue()	
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<SensorDataPoint> getArrayValue()
	{
		return (ArrayList<SensorDataPoint>)value;
	}
	
	
	/**
	 * @see #getIntValue()	
	 */
	public SensorEvent getSensorEventValue()
	{
		return (SensorEvent)value;
	}
	
	/**
	 * @see #getIntValue()	
	 */
	public JSONObject getJSONValue()
	{
		return (JSONObject)value;
	}

	
}
