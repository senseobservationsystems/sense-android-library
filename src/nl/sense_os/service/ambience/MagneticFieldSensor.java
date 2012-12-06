/**************************************************************************************************
 * Copyright (C) 2012 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.ambience;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

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

public class MagneticFieldSensor implements SensorEventListener {

    private static final String TAG = "Sense Magnetic Field Sensor";
    private static String sensor_display_name = "Magnetic field";
    private long sampleDelay = 0; // in milliseconds
    private long[] lastSampleTimes = new long[50];
    private Context context;
    private List<Sensor> sensors;
    private SensorManager smgr;
    private Handler MagneticFieldHandler = new Handler();
    private Runnable MagneticFieldThread = null;
    private boolean MagneticFieldSensingActive = false;

    public MagneticFieldSensor(Context context) {
        this.context = context;
        smgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensors = new ArrayList<Sensor>();
        if (null != smgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)) {
            sensors.add(smgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Log.d(TAG, "Accuracy changed...");
        // Log.d(TAG, "Sensor: " + sensor.getName() + "(" + sensor.getType() + "), accuracy: " +
        // accuracy);
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
            i.putExtra(DataPoint.DISPLAY_NAME, sensor_display_name);
            i.putExtra(DataPoint.SENSOR_DESCRIPTION, sensor.getName());
            i.putExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime());
            context.startService(i);
        }
        if (sampleDelay > 500 && MagneticFieldSensingActive) {
            // unregister the listener and start again in sampleDelay seconds
            stopMagneticFieldSensing();
            MagneticFieldHandler.postDelayed(MagneticFieldThread = new Runnable() {

                @Override
                public void run() {
                    startMagneticFieldSensing(sampleDelay);
                }
            }, sampleDelay);
        }
    }

    public void setSampleDelay(long _sampleDelay) {
        sampleDelay = _sampleDelay;
    }

    public void startMagneticFieldSensing(long _sampleDelay) {
        MagneticFieldSensingActive = true;
        setSampleDelay(_sampleDelay);
        for (Sensor sensor : sensors) {
            if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                // Log.d(TAG, "registering for sensor " + sensor.getName());
                smgr.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    public void stopMagneticFieldSensing() {
        try {
            MagneticFieldSensingActive = false;
            smgr.unregisterListener(this);

            if (MagneticFieldThread != null)
                MagneticFieldHandler.removeCallbacks(MagneticFieldThread);
            MagneticFieldThread = null;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }

    public long getSampleDelay() {
        return sampleDelay;
    }
}