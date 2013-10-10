/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.ambience;

import java.util.Arrays;
import java.util.jar.JarEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main.Ambience;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.shared.SensorDataPoint;
import nl.sense_os.service.subscription.BaseDataProducer;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Audio Sensor
 * 
 * This sensor returns audio samples which it gets from the noise sensor. 
 * It's interval, sample rate and sample length are set in the noise sensor class.
 * The sensor returns a json object with the fields "sample rate" and "values", resp the sample rate as integer and the audio data as array.
 * 
 * @author Ted Schmidt <ted@sense-os.nl>
 */
public class AudioSensor extends BaseDataProducer {

	private Context context;
	private final String TAG = "Audio sensor";
	
	protected AudioSensor(Context context) {
		this.context = context;
	}

	private static AudioSensor instance = null;

	public static AudioSensor getInstance(Context context) {
		if(instance == null) {
			instance = new AudioSensor(context);
		}
		return instance;
	}

	public void onNewData(long timestamp, float[] audio, int sample_rate) {
		try {
			JSONObject json = new JSONObject();
			json.put("sample rate", sample_rate);			
			JSONArray jarr = new JSONArray();
			for (float f : audio) {
				jarr.put(f);				
			}						
			json.put("values", jarr);			
			sendSensorValue(json, timestamp);
		} catch (JSONException e) {			
			e.printStackTrace();
		}
	}


	private void sendSensorValue(JSONObject value, long ms) {
		
		this.notifySubscribers();
		if(this.hasSubscribers())
		{
			SensorDataPoint dataPoint = new SensorDataPoint(value);			
			dataPoint.sensorName = SensorNames.AUDIO;
			dataPoint.sensorDescription = SensorNames.AUDIO;
			dataPoint.timeStamp = ms;        
			this.sendToSubscribers(dataPoint);
		}
		SharedPreferences mainPrefs = context.getSharedPreferences(
				SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
		if (!mainPrefs.getBoolean(Ambience.AUDIO_UPLOAD, false))			
			return;
		
		Intent sensorData = new Intent(
				context.getString(R.string.action_sense_new_data));
		sensorData.putExtra(DataPoint.SENSOR_NAME, SensorNames.AUDIO);			
		String valueStr = value.toString();		
		sensorData.putExtra(DataPoint.VALUE, valueStr);		
		sensorData.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
		sensorData.putExtra(DataPoint.TIMESTAMP, ms);
		context.startService(sensorData);
	}
}
