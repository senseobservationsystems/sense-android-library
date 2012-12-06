/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.ambience;

import java.util.ArrayList;
import java.util.List;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

/**
 * Represents the air pressure sensor. Registers itself for updates from the Android
 * {@link SensorManager}.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class PressureSensor implements SensorEventListener {

    private static final String TAG = "Sense Pressure Sensor";

    private long sampleDelay = 0; // in milliseconds
    private long[] lastSampleTimes = new long[50];
    private Context context;
    private List<Sensor> sensors;
    private SensorManager smgr;
    private Handler pressureHandler = new Handler();
    private Runnable pressureThread = null;
    private boolean pressureSensingActive = false;

    public PressureSensor(Context context) {
        this.context = context;
        smgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensors = new ArrayList<Sensor>();
        if (null != smgr.getDefaultSensor(Sensor.TYPE_PRESSURE)) {
            sensors.add(smgr.getDefaultSensor(Sensor.TYPE_PRESSURE));
        }
    }

    /**
     * @return The delay between samples in milliseconds
     */
    public long getSampleDelay() {
        return sampleDelay;
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
            }

            String jsonString = null;
            if (sensor.getType() == Sensor.TYPE_PRESSURE) {
                jsonString = "{";
                // value is millibar, convert to Pascal
                float millibar = event.values[0];
                float pascal = millibar * 100;
                jsonString += "\"Pascal\":" + pascal;
                jsonString += "}";
            } else {
                // not the right sensor
                return;
            }

            // send msg to MsgHandler
            Intent i = new Intent(context.getString(R.string.action_sense_new_data));
            i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
            i.putExtra(DataPoint.VALUE, jsonString);
            i.putExtra(DataPoint.SENSOR_NAME, sensorName);
            i.putExtra(DataPoint.SENSOR_DESCRIPTION, sensor.getName());
            i.putExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime());
            context.startService(i);
        }
        if (sampleDelay > 500 && pressureSensingActive) {
            // unregister the listener and start again in sampleDelay seconds
            stopPressureSensing();
            pressureHandler.postDelayed(pressureThread = new Runnable() {

                @Override
                public void run() {
                    startPressureSensing(sampleDelay);
                }
            }, sampleDelay);
        }
    }

    /**
     * Sets the delay between samples. The sensor registers itself for periodic sampling bursts, and
     * unregisters after it received a sample.
     * 
     * @param sampleDelay
     *            Sample delay in milliseconds
     */
    public void setSampleDelay(long sampleDelay) {
        this.sampleDelay = sampleDelay;
    }

    /**
     * Starts sensing by registering for updates at the Android SensorManager. The sensor registers
     * for updates at the rate specified by the sampleDelay parameter.
     * 
     * @param sampleDelay
     *            Delay between samples in milliseconds
     */
    public void startPressureSensing(long _sampleDelay) {
        pressureSensingActive = true;
        setSampleDelay(_sampleDelay);
        for (Sensor sensor : sensors) {
            if (sensor.getType() == Sensor.TYPE_PRESSURE) {
                // Log.d(TAG, "registering for sensor " + sensor.getName());
                smgr.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    /**
     * Stops the periodic sampling.
     */
    public void stopPressureSensing() {
        try {
            pressureSensingActive = false;
            smgr.unregisterListener(this);

            if (pressureThread != null)
                pressureHandler.removeCallbacks(pressureThread);
            pressureThread = null;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }
}