package nl.sense_os.service.ambience;

import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.os.Handler;
import android.util.Log;

import nl.sense_os.service.MsgHandler;

public class LightSensor implements SensorEventListener {
	private static final String TAG = "Sense Light Sensor";
	private MsgHandler msgHandler;
	private long sampleDelay = 0; //in milliseconds    
	private long[] lastSampleTimes = new long[50];
	private Context context;
	private List<Sensor> sensors;
	private SensorManager smgr;
	private Handler LightHandler = new Handler();
	private Runnable LightThread = null;
	private boolean LightSensingActive = false;
	public LightSensor(MsgHandler handler, Context _context) {
		this.msgHandler = handler;
		this.context = _context;
		smgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);		
		sensors = smgr.getSensorList(Sensor.TYPE_ALL);

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
			if(sensor.getType()==Sensor.TYPE_LIGHT)
			{
				sensorName = "light";
			}
			
			String jsonString = "{";	        
			int x = 0;
			for (float value: event.values) {
				if(x==0)
				{	
					if(sensor.getType()==Sensor.TYPE_LIGHT)
						jsonString += "\"lux\":"+value;				
				}				
				x++;
			}
			jsonString += "}";
			this.msgHandler.sendSensorData(sensorName, jsonString, "json", sensor.getName()); 	       
		}
		if(sampleDelay > 500 && LightSensingActive)
		{
			// unregister the listener and start again in sampleDelay seconds	
			stopLightSensing();
			LightHandler.postDelayed(LightThread= new Runnable() {

				public void run() 
				{					
					startLightSensing(sampleDelay);
				}
			},sampleDelay);
		}
	}

	public void setSampleDelay(long _sampleDelay)
	{
		sampleDelay = _sampleDelay;    	
	}

	public void startLightSensing(long _sampleDelay)
	{
		LightSensingActive = true;
		setSampleDelay(_sampleDelay);		
		for (Sensor sensor : sensors) {
			if (sensor.getType()==Sensor.TYPE_LIGHT)
			{
				//Log.d(TAG, "registering for sensor " + sensor.getName());
				smgr.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
			}
		}		
	}

	public void stopLightSensing()
	{
		try{
			LightSensingActive = false;
			smgr.unregisterListener(this);

			if(LightThread != null)		
				LightHandler.removeCallbacks(LightThread);
			LightThread = null;
			
		}catch(Exception e)
		{
			Log.e(TAG, e.getMessage());
		}

	}

	public long getSampleDelay()
	{
		return sampleDelay;
	}
}
