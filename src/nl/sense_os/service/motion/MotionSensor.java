package nl.sense_os.service.motion;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import nl.sense_os.service.MsgHandler;

public class MotionSensor implements SensorEventListener {
    @SuppressWarnings("unused")
    private static final String TAG = "Sense MotionSensor";
    private MsgHandler msgHandler;
    private long sampleDelay = 0; //in milliseconds    
    private long[] lastSampleTimes = new long[50];
    public MotionSensor(MsgHandler handler) {
        this.msgHandler = handler;
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
//        Log.d(TAG, "Accuracy changed...");
//        Log.d(TAG, "Sensor: " + sensor.getName() + "(" + sensor.getType() + "), accuracy: " + accuracy);
    }

    public void onSensorChanged(SensorEvent event) {
    	Sensor sensor = event.sensor;
    	if(System.currentTimeMillis() > lastSampleTimes[sensor.getType()]+sampleDelay)
    	{
    		lastSampleTimes[sensor.getType()] = System.currentTimeMillis();	 
	        
	        String sensorName = "";
	        if(sensor.getType()==Sensor.TYPE_ACCELEROMETER)
	        {
	        	sensorName = "accelerometer";
	        }
	        if(sensor.getType()==Sensor.TYPE_ORIENTATION)
	        {
	        	sensorName = "orientation";
	        }
	        if(sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD)
	        {
	        	sensorName = "magnetic_field";
	        }
	        if(sensor.getType()==Sensor.TYPE_GYROSCOPE)
	        {
	        	sensorName = "gyroscope";
	        }
	        String jsonString = "{";	        
	        int x = 0;
	        for (float value: event.values) {
	        	if(x==0)
	        	{
	        		//jsonString += "{";
	        		if(sensor.getType()==Sensor.TYPE_ACCELEROMETER || sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD)
	        			jsonString += "\"x-axis\":\""+value+"\"";
	        		if(sensor.getType()==Sensor.TYPE_ORIENTATION || sensor.getType() == Sensor.TYPE_GYROSCOPE)
	        			jsonString += "\"azimuth\":\""+value+"\"";	        
	        		//jsonString += "}";	        		
	        	}
	        	if(x==1)
	        	{
	        		jsonString += ",";
	        		//jsonString += ",{";
	        		if(sensor.getType()==Sensor.TYPE_ACCELEROMETER || sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD)
	        			jsonString += "\"y-axis\":\""+value+"\"";
	        		if(sensor.getType()==Sensor.TYPE_ORIENTATION || sensor.getType() == Sensor.TYPE_GYROSCOPE)
	        			jsonString += "\"pitch\":\""+value+"\"";	        		
	        	//	jsonString += "}";
	        	}
	        	if(x==2)
	        	{
	        		jsonString += ",";
	        		//jsonString += ",{";
	        		if(sensor.getType()==Sensor.TYPE_ACCELEROMETER || sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD)
	        			jsonString += "\"z-axis\":\""+value+"\"";
	        		if(sensor.getType()==Sensor.TYPE_ORIENTATION || sensor.getType() == Sensor.TYPE_GYROSCOPE)
	        			jsonString += "\"roll\":\""+value+"\"";	        		
	        		//jsonString += "}";
	        		
	        	}	        	
	        	x++;
	        }
	        jsonString += "}";
	        this.msgHandler.sendSensorData(sensorName, jsonString, "json", sensor.getName()); 	       
    	}        
    }
    
    public void setSampleDelay(long _sampleDelay)
    {
    	sampleDelay = _sampleDelay;
    }
    
    public long getSampleDelau()
    {
    	return sampleDelay;
    }
}
