/**************************************************************************************************
 * Copyright (C) 2012 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.ambience;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.BaseSensor;
import nl.sense_os.service.shared.PeriodicPollAlarmReceiver;
import nl.sense_os.service.shared.PeriodicPollingSensor;
import nl.sense_os.service.shared.SensorDataPoint;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

/**
 * Represents the magnetic field sensor. Registers itself for updates from the Android
 * {@link SensorManager}.
 * 
 * @author Ted Schmidt <ted@sense-os.nl>
 */
public class MagneticFieldSensor extends BaseSensor implements SensorEventListener,
        PeriodicPollingSensor {

    private static final String TAG = "Sense Magnetic Field Sensor";
    private static final String SENSOR_DISPLAY_NAME = "magnetic field";
    private static MagneticFieldSensor instance;

    /**
     * Factory method to get the singleton instance.
     * 
     * @param context
     * @return instance
     */
    public static MagneticFieldSensor getInstance(Context context) {
        if (null == instance) {
            instance = new MagneticFieldSensor(context);
        }
        return instance;
    }

    private long sampleDelay = 0; // in milliseconds
    private long[] lastSampleTimes = new long[50];
    private Context context;
    private List<Sensor> sensors;
    private SensorManager smgr;
    private boolean magneticFieldSensingActive = false;
    private PeriodicPollAlarmReceiver alarmReceiver;
    private WakeLock wakeLock;

    protected MagneticFieldSensor(Context context) {
        this.context = context;
        smgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensors = new ArrayList<Sensor>();
        if (null != smgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)) {
            sensors.add(smgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
        }
        alarmReceiver = new PeriodicPollAlarmReceiver(this);
    }

    @Override
    public void doSample() {
        Log.v(TAG, "Check magnetic field");

        // acquire wake lock
        if (null == wakeLock) {
            PowerManager powerMgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakeLock = powerMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            wakeLock.setReferenceCounted(false);
        }
        if (!wakeLock.isHeld()) {
            wakeLock.acquire(500);
        } else {
            // Log.v(TAG, "Wake lock already held");
        }

        // register as sensor listener
        for (Sensor sensor : sensors) {
            if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                // Log.d(TAG, "registering for sensor " + sensor.getName());
                smgr.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }


    @Override
    public boolean isActive() {
        return magneticFieldSensingActive;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not used
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (System.currentTimeMillis() > lastSampleTimes[sensor.getType()] + sampleDelay) {
            lastSampleTimes[sensor.getType()] = System.currentTimeMillis();

			String sensorName = "";
			if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
				sensorName = SensorNames.MAGNETIC_FIELD;

				double x = event.values[0];
				// scale to three decimal precision
				x = BigDecimal.valueOf(x).setScale(3, 0).doubleValue();
				double y = event.values[1];
				// scale to three decimal precision
				y = BigDecimal.valueOf(y).setScale(3, 0).doubleValue();
				double z = event.values[2];
				// scale to three decimal precision
				z = BigDecimal.valueOf(z).setScale(3, 0).doubleValue();

				HashMap<String, Object> dataFields = new HashMap<String, Object>();
				dataFields.put("x", x);
				dataFields.put("y", y);
				dataFields.put("z", z);
				JSONObject jsonObj = new JSONObject(dataFields);
				String jsonString = jsonObj.toString();
				
				this.notifySubscribers();
				SensorDataPoint dataPoint = new SensorDataPoint(jsonObj);
				dataPoint.sensorName = sensorName;
				dataPoint.sensorDescription = sensor.getName();
				dataPoint.timeStamp = SNTP.getInstance().getTime();        
				this.sendToSubscribers(dataPoint);

				// send msg to MsgHandler
				Intent i = new Intent(context.getString(R.string.action_sense_new_data));
				i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
				i.putExtra(DataPoint.VALUE, jsonString);
				i.putExtra(DataPoint.SENSOR_NAME, sensorName);
				i.putExtra(DataPoint.DISPLAY_NAME, SENSOR_DISPLAY_NAME);
				i.putExtra(DataPoint.SENSOR_DESCRIPTION, sensor.getName());
				i.putExtra(DataPoint.TIMESTAMP, dataPoint.timeStamp);
				context.startService(i);

                // sample is successful: unregister the listener
                stopSample();
			}
        }
    }

    /**
     * Sets the delay between samples. The sensor registers itself for periodic sampling bursts, and
     * unregisters after it received a sample.
     * 
     * @param sampleDelay
     *            Sample delay in milliseconds
     */
    
    public void setSampleRate(long sampleDelay) {
        stopPolling();
        this.sampleDelay = sampleDelay;
        startPolling();
    }

    private void startPolling() {
        // Log.v(TAG, "start polling");
        alarmReceiver.start(context);
    }

    /**
     * Starts sensing by registering for updates at the Android SensorManager. The sensor registers
     * for updates at the rate specified by the sampleDelay parameter.
     * 
     * @param sampleDelay
     *            Delay between samples in milliseconds
     */
    @Override
    public void startSensing(long sampleRate) {

        // check if the sensor is physically present on this phone
        boolean found = false;
        for (Sensor sensor : sensors) {
            if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                found = true;
                break;
            }
        }
        if (!found) {
            Log.w(TAG, "No magnetic field sensor!");
            return;
        }

        magneticFieldSensingActive = true;
        setSampleRate(sampleRate);
    }

    private void stopPolling() {
        // Log.v(TAG, "stop polling");
        alarmReceiver.stop(context);
    }

    private void stopSample() {
        // Log.v(TAG, "stop sample");

        // release wake lock
        if (null != wakeLock && wakeLock.isHeld()) {
            wakeLock.release();
        }

        // unregister sensor listener
        try {
            smgr.unregisterListener(this);
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop magnetic field sample!", e);
        }

    }

    /**
     * Stops the periodic sampling.
     */
    @Override
    public void stopSensing() {
        magneticFieldSensingActive = false;
        stopPolling();

    }
}