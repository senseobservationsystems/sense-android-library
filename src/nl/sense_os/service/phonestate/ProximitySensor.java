/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.phonestate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.BaseSensor;
import nl.sense_os.service.shared.SensorDataPoint;
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
public class ProximitySensor extends BaseSensor implements SensorEventListener {

	private static final String TAG = "Sense Proximity Sensor";

	private Context context;
	private List<Sensor> sensors;
	private SensorManager smgr;
	private Handler proximityHandler = new Handler();
	private Runnable proximityThread = null;

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
		Sensor sensor = event.sensor;

        if (event.values.length < 1 || sensor.getType() != Sensor.TYPE_PROXIMITY
                || Float.isNaN(event.values[0])) {
            // unusable value
            return;
        }

        // scale to meters
        float distance = event.values[0] / 100f;

        // limit two decimal precision
        distance = BigDecimal.valueOf(distance).setScale(2, BigDecimal.ROUND_HALF_DOWN)
                .floatValue();

        try {
			this.notifySubscribers();
            SensorDataPoint dataPoint = new SensorDataPoint(distance);
            dataPoint.sensorName = SensorNames.PROXIMITY;
			dataPoint.sensorDescription = sensor.getName();
            dataPoint.timeStamp = SNTP.getInstance().getTime();
			this.sendToSubscribers(dataPoint);
        } catch (Exception e) {
			Log.e(TAG, "Error in send data to subscribers in ProximitySensor");
		}
		
		// pass message to the MsgHandler
		Intent i = new Intent(context.getString(R.string.action_sense_new_data));
        i.putExtra(DataPoint.SENSOR_NAME, SensorNames.PROXIMITY);
		i.putExtra(DataPoint.SENSOR_DESCRIPTION, sensor.getName());
        i.putExtra(DataPoint.VALUE, distance);
        i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.FLOAT);
		i.putExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime());
		this.context.startService(i);	
	}

    @Override
    public void startSensing(long sampleDelay) {
        proximityHandler = new Handler();
	
		for (Sensor sensor : sensors) {
			if (sensor.getType() == Sensor.TYPE_PROXIMITY) {
				// Log.d(TAG, "registering for sensor " + sensor.getName());
				smgr.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
			}
		}
	}

    @Override
    public void stopSensing() {
        try {
			smgr.unregisterListener(this);

            if (proximityThread != null) {
                proximityHandler.removeCallbacks(proximityThread);
                proximityThread = null;
            }

		} catch (Exception e) {
            Log.e(TAG, "Failed to unregister proximity sensor! " + e.getMessage());
		}
	}
}
