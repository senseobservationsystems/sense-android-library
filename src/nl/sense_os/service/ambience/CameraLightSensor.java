/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.ambience;

import java.util.HashMap;

import nl.sense_os.service.R;
import nl.sense_os.service.ambience.CameraLightValue.CameraLightValueCallback;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.SensorDataPoint;
import nl.sense_os.service.shared.BaseDataProducer;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

/**
 * This class represents the camera light sensor module. It manages its own HandlerThread to sample
 * the camera periodically with a given sample interval.
 * 
 * @author Ted Schmidt <ted@sense-os.nl>
 * 
 * @see CameraLightValue
 */
public class CameraLightSensor extends BaseDataProducer{

	/**
	 * Callback that handles a new light value datapoint and sends it to the MsgHandler.
	 */
	private class NewCameraLightValue implements CameraLightValueCallback {

		@Override
		public void lightValueCallback(float lightValue, int camera_id) {
			// send the light value to commonSense
			String sensorDisplayName = "Camera Light";
			String sensorName = SensorNames.CAMERA_LIGHT;
			String sensorDescription = "camera " + camera_id + " average luminance";
			HashMap<String, Object> dataFields = new HashMap<String, Object>();
			dataFields.put("lux", lightValue);
			JSONObject jsonObj = new JSONObject(dataFields);
			String jsonString = jsonObj.toString();

			notifySubscribers();
			SensorDataPoint dataPoint = new SensorDataPoint(jsonObj);
			dataPoint.sensorName = sensorName;
			dataPoint.sensorDescription = sensorDescription;
			dataPoint.timeStamp = SNTP.getInstance().getTime();        
			sendToSubscribers(dataPoint);

			// pass message to the MsgHandler
			Intent i = new Intent(context.getString(R.string.action_sense_new_data));
			i.putExtra(DataPoint.SENSOR_NAME, sensorName);
			i.putExtra(DataPoint.DISPLAY_NAME, sensorDisplayName);
			i.putExtra(DataPoint.VALUE, jsonString);
			i.putExtra(DataPoint.SENSOR_DESCRIPTION, sensorDescription);
			i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
			i.putExtra(DataPoint.TIMESTAMP, dataPoint.timeStamp);
			context.startService(i);
			// Log.e(TAG, "Sent new camera licht values, camera: "+camera_id+" value: "+lightValue);
			nextUpdate(camera_id);
		}
	}

	/**
	 * Task to capture a new light value from a camera. Reschedules itself when done.
	 */
	private class UpdateAndSendCameraLightValues implements Runnable {
		int camera_id;

		public UpdateAndSendCameraLightValues(int camera_id) {
			this.camera_id = camera_id;
		}

		@Override
		public void run() {
			try {
				if (!cameraLightValue.getLightValue(camera_id, new NewCameraLightValue())) {
					// something failed, probably camera.open so no callback
					nextUpdate(camera_id);
				}
			} catch (Exception e) {
				Log.w(TAG, "Failed to update camera light sensor: " + e);
			}
		}
	}

	private Context context;
	private CameraLightValue cameraLightValue;
	private int sampleDelay;
	private HandlerThread sensorHandlerThread = null;
	private Handler cameraLightValueHandler = null;
	private UpdateAndSendCameraLightValues updateAndSendValues = null;
	private boolean sensorEnabled;

	private String TAG = "Camera Light Sensor";

	private CameraLightSensor(Context context) {
		this.context = context;
		cameraLightValue = new CameraLightValue();
	}

	private static CameraLightSensor instance = null;

	public static CameraLightSensor getInstance(Context context) {
		if(instance == null) {
			instance = new CameraLightSensor(context);
		}
		return instance;
	}

	private void nextUpdate(int camera_id) {
		// check if there are more camera's else wait
		// add again for another run if the sensor is still enabled
		if (sensorEnabled && cameraLightValueHandler != null) {
			// last camera wait schedule a new run
			if (camera_id == cameraLightValue.getNumberOfCameras() - 1) {
				int deplayMilisec = sampleDelay == -1 ? 1000 : sampleDelay;
				cameraLightValueHandler.postDelayed(
						updateAndSendValues = new UpdateAndSendCameraLightValues(0), deplayMilisec);
			} else {
				++camera_id;
				cameraLightValueHandler
				.post(updateAndSendValues = new UpdateAndSendCameraLightValues(camera_id));
			}
		}
	}

	/**
	 * Starts sampling the cameras periodically.
	 * 
	 * @param sampleDelay
	 *            The time between samples (milliseconds)
	 */
	public void startLightSensing(int sampleDelay) {
		try {
			this.sampleDelay = sampleDelay;
			sensorEnabled = true;
			// create a new thread to run this sensor on
			if (sensorHandlerThread == null)
				sensorHandlerThread = new HandlerThread("CameraLightSensor");
			// start the thread;
			if (!sensorHandlerThread.isAlive())
				sensorHandlerThread.start();
			// create a handler on the thread to run the update and send functions
			if (cameraLightValueHandler == null)
				cameraLightValueHandler = new Handler(sensorHandlerThread.getLooper());
			// add the runnable
			if (updateAndSendValues == null)
				cameraLightValueHandler
				.post(updateAndSendValues = new UpdateAndSendCameraLightValues(0));
		} catch (Exception e) {
			Log.e(TAG, "Error in starting the Camera Light sensor: " + e.getMessage());
		}
	}

	public void stopLightSensing() {
		try {
			sensorEnabled = false;
			if (cameraLightValueHandler != null)
				cameraLightValueHandler.removeCallbacks(updateAndSendValues);
			updateAndSendValues = null;
			cameraLightValueHandler = null;
			if (sensorHandlerThread != null)
				sensorHandlerThread.getLooper().quit();
			sensorHandlerThread = null;
		} catch (Exception e) {
			Log.e(TAG, "Error in stopping the camera light sensor");
		}
	}
}
