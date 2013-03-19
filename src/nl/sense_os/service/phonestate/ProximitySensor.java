/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.phonestate;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.SensorDataPoint;
import nl.sense_os.service.shared.BaseDataProducer;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

/**
 * Represents the proximity sensor. Listens for proximity sensor data from the Android
 * SensorManager.
 * 
 * @author Ted Schmidt <ted@sense-os.nl>
 */
public class ProximitySensor extends BaseDataProducer implements SensorEventListener {

	private static final String TAG = "Sense Proximity Sensor";

	private long sampleDelay = 0; // in milliseconds
	private long[] lastSampleTimes = new long[50];
	private Context context;
	private List<Sensor> sensors;
	private SensorManager smgr;
	private Handler ProximityHandler = new Handler();
	private Runnable ProximityThread = null;
	private boolean ProximitySensingActive = false;
	private SensorEvent last_event = null;

	protected ProximitySensor(Context context) {
		this.context = context;
		smgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		sensors = new ArrayList<Sensor>();
		if (null != smgr.getDefaultSensor(Sensor.TYPE_PROXIMITY)) {
			sensors.add(smgr.getDefaultSensor(Sensor.TYPE_PROXIMITY));
		}
	}

	private static ProximitySensor instance = null;

	public static ProximitySensor getInstance(Context context) {
		if(instance == null) {
			instance = new ProximitySensor(context);
		}
		return instance;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// do nothing
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// catch the event, and send it when the time is right	
		last_event = event;		
	}

	class processAndSend implements Runnable 
	{
		public void run()
		{	
			if (last_event != null && (System.currentTimeMillis() > lastSampleTimes[last_event.sensor.getType()] + sampleDelay)) 
			{
				Sensor sensor = last_event.sensor;
				lastSampleTimes[sensor.getType()] = System.currentTimeMillis();

				String sensorName = "";
				if (sensor.getType() == Sensor.TYPE_PROXIMITY) {
					sensorName = SensorNames.PROXIMITY;
				}

				String jsonString = "{";
				int x = 0;
				for (float value : last_event.values) {
					if (x == 0) {
						if (sensor.getType() == Sensor.TYPE_PROXIMITY)
							jsonString += "\"distance\":" + value;
					}
					x++;
				}
				jsonString += "}";

				try{

					notifySubscribers();
					SensorDataPoint dataPoint = new SensorDataPoint(new JSONObject(jsonString));
					dataPoint.sensorName = sensorName;
					dataPoint.sensorDescription = sensor.getName();
					dataPoint.timeStamp = SNTP.getInstance().getTime();        
					sendToSubscribers(dataPoint);
				}
				catch(Exception e)
				{
					Log.e(TAG, "Error in send data to subscribers in ProximitySensor");
				}
				// pass message to the MsgHandler
				Intent i = new Intent(context.getString(R.string.action_sense_new_data));
				i.putExtra(DataPoint.SENSOR_NAME, sensorName);
				i.putExtra(DataPoint.SENSOR_DESCRIPTION, sensor.getName());
				i.putExtra(DataPoint.VALUE, jsonString);
				i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
				i.putExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime());
				context.startService(i);
			}
			else
				Log.e(TAG,  "to fast");
			if (ProximitySensingActive) {				
				ProximityHandler.postDelayed(ProximityThread = new processAndSend(), sampleDelay);
			}
		}
	}

		public void setSampleDelay(long _sampleDelay) {
			sampleDelay = _sampleDelay;
		}

		public void startProximitySensing(long _sampleDelay) {
			//_sampleDelay = 0;
			ProximityHandler = new Handler();
			ProximitySensingActive = true;
			setSampleDelay(_sampleDelay);
			for (Sensor sensor : sensors) {
				if (sensor.getType() == Sensor.TYPE_PROXIMITY) {
					// Log.d(TAG, "registering for sensor " + sensor.getName());					
					smgr.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
					ProximityHandler.postDelayed(ProximityThread = new processAndSend(), sampleDelay);
				}
			}
		}

		public void stopProximitySensing() {
			try {
				ProximitySensingActive = false;
				smgr.unregisterListener(this);

				if (ProximityThread != null)
					ProximityHandler.removeCallbacks(ProximityThread);
				ProximityThread = null;

			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
			}

		}

		public long getSampleDelay() {
			return sampleDelay;
		}
	}
