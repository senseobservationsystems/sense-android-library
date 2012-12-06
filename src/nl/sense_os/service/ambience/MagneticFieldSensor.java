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

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

/**
 * Represents the magnetic field sensor. Registers itself for updates from the Android
 * {@link SensorManager}.
 * 
 * @author Ted Schmidt <ted@sense-os.nl>
 */
public class MagneticFieldSensor implements SensorEventListener {

    private static final String TAG = "Sense Magnetic Field Sensor";
    private static final String SENSOR_DISPLAY_NAME = "magnetic field";
    private long sampleDelay = 0; // in milliseconds
    private long[] lastSampleTimes = new long[50];
    private Context context;
    private List<Sensor> sensors;
    private SensorManager smgr;
    private Handler magneticFieldHandler = new Handler();
    private Runnable magneticFieldThread = null;
    private boolean magneticFieldSensingActive = false;

    public MagneticFieldSensor(Context context) {
        this.context = context;
        smgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensors = new ArrayList<Sensor>();
        if (null != smgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)) {
            sensors.add(smgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
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
            }

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
    		String jsonString = new JSONObject(dataFields).toString();

            // send msg to MsgHandler
            Intent i = new Intent(context.getString(R.string.action_sense_new_data));
            i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
            i.putExtra(DataPoint.VALUE, jsonString);
            i.putExtra(DataPoint.SENSOR_NAME, sensorName);
            i.putExtra(DataPoint.DISPLAY_NAME, SENSOR_DISPLAY_NAME);
            i.putExtra(DataPoint.SENSOR_DESCRIPTION, sensor.getName());
            i.putExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime());
            context.startService(i);
        }
        if (sampleDelay > 500 && magneticFieldSensingActive) {
            // unregister the listener and start again in sampleDelay seconds
            stopMagneticFieldSensing();
            magneticFieldHandler.postDelayed(magneticFieldThread = new Runnable() {

                @Override
                public void run() {
                    startMagneticFieldSensing(sampleDelay);
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
    public void startMagneticFieldSensing(long sampleDelay) {
        magneticFieldSensingActive = true;
        setSampleDelay(sampleDelay);
        for (Sensor sensor : sensors) {
            if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                // Log.d(TAG, "registering for sensor " + sensor.getName());
                smgr.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    /**
     * Stops the periodic sampling.
     */
    public void stopMagneticFieldSensing() {
        try {
            magneticFieldSensingActive = false;
            smgr.unregisterListener(this);

            if (magneticFieldThread != null)
                magneticFieldHandler.removeCallbacks(magneticFieldThread);
            magneticFieldThread = null;

        } catch (Exception e) {
            Log.e(TAG, "Failed to stop sampling! " + e.getMessage());
        }

    }
}