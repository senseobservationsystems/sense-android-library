/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.ambience;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.PeriodicPollAlarmReceiver;
import nl.sense_os.service.shared.PeriodicPollingSensor;
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
 * Represents the air pressure sensor. Registers itself for updates from the Android
 * {@link SensorManager}.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class PressureSensor implements SensorEventListener, PeriodicPollingSensor {

    private static final String TAG = "Sense Pressure Sensor";
    private static PressureSensor instance = null;

    /**
     * Factory method to get the singleton instance.
     * 
     * @param context
     * @return instance
     */
    public static PressureSensor getInstance(Context context) {
	    if(instance == null) {
	       instance = new PressureSensor(context);
	    }
	    return instance;
    }

    private long sampleDelay = 0; // in milliseconds
    private long[] lastSampleTimes = new long[50];
    private Context context;
    private List<Sensor> sensors;
    private SensorManager smgr;
    private boolean pressureSensingActive = false;
    private PeriodicPollAlarmReceiver alarmReceiver;
    private WakeLock wakeLock;
    
    protected PressureSensor(Context context) {
        this.context = context;
        smgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensors = new ArrayList<Sensor>();
        if (null != smgr.getDefaultSensor(Sensor.TYPE_PRESSURE)) {
            sensors.add(smgr.getDefaultSensor(Sensor.TYPE_PRESSURE));
        }

        alarmReceiver = new PeriodicPollAlarmReceiver(this);
    }

    @Override
    public void doSample() {
        // Log.v(TAG, "start sample");

        // acquire wake lock
        if (null == wakeLock) {
            PowerManager powerMgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakeLock = powerMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        }
        if (!wakeLock.isHeld()) {
            wakeLock.acquire(500);
        } else {
            // Log.v(TAG, "Wake lock already held");
        }

        // register as sensor listener
        for (Sensor sensor : sensors) {
            if (sensor.getType() == Sensor.TYPE_PRESSURE) {
                // Log.d(TAG, "registering for sensor " + sensor.getName());
                smgr.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    /**
     * @return The delay between samples in milliseconds
     */
    @Override
    public long getSampleRate() {
        return sampleDelay;
    }

    @Override
    public boolean isActive() {
        return pressureSensingActive;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (System.currentTimeMillis() > lastSampleTimes[sensor.getType()] + sampleDelay) {
            lastSampleTimes[sensor.getType()] = System.currentTimeMillis();

            String sensorName = "";
            if (sensor.getType() == Sensor.TYPE_PRESSURE) {
                sensorName = SensorNames.PRESSURE;

                // value is millibar, convert to Pascal
                float millibar = event.values[0];
                float pascal = millibar * 100;
                float value = BigDecimal.valueOf(pascal).setScale(3, 0).floatValue();

                // send msg to MsgHandler
                Intent i = new Intent(context.getString(R.string.action_sense_new_data));
                i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.FLOAT);
                i.putExtra(DataPoint.VALUE, value);
                i.putExtra(DataPoint.SENSOR_NAME, sensorName);
                i.putExtra(DataPoint.SENSOR_DESCRIPTION, sensor.getName());
                i.putExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime());
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
    @Override
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
        pressureSensingActive = true;
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
            Log.e(TAG, "Failed to stop pressure field sample!", e);
        }

    }

    /**
     * Stops the periodic sampling.
     */
    @Override
    public void stopSensing() {
        // Log.v(TAG, "stop sensor");
        stopPolling();
        pressureSensingActive = false;
    }
}