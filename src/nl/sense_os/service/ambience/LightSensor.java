/*
 ************************************************************************************************************
 *     Copyright (C)  2010 Sense Observation Systems, Rotterdam, the Netherlands.  All rights reserved.     *
 ************************************************************************************************************
 */
package nl.sense_os.service.ambience;

import java.util.List;

import nl.sense_os.service.Constants;
import nl.sense_os.service.MsgHandler;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

public class LightSensor implements SensorEventListener {
    private static final String TAG = "Sense Light Sensor";
    private long sampleDelay = 0; // in milliseconds
    private long[] lastSampleTimes = new long[50];
    private Context context;
    private List<Sensor> sensors;
    private SensorManager smgr;
    private Handler LightHandler = new Handler();
    private Runnable LightThread = null;
    private boolean LightSensingActive = false;

    public LightSensor(Context context) {
        this.context = context;
        smgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensors = smgr.getSensorList(Sensor.TYPE_ALL);
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
            if (sensor.getType() == Sensor.TYPE_LIGHT) {
                sensorName = "light";
            }

            String jsonString = "{";
            int x = 0;
            for (float value : event.values) {
                if (x == 0) {
                    if (sensor.getType() == Sensor.TYPE_LIGHT)
                        jsonString += "\"lux\":" + value;
                }
                x++;
            }
            jsonString += "}";

            // pass message to the MsgHandler
            Intent i = new Intent(MsgHandler.ACTION_NEW_MSG);
            i.putExtra(MsgHandler.KEY_SENSOR_NAME, sensorName);
            i.putExtra(MsgHandler.KEY_VALUE, jsonString);
            i.putExtra(MsgHandler.KEY_SENSOR_DEVICE, sensor.getName());
            i.putExtra(MsgHandler.KEY_DATA_TYPE, Constants.SENSOR_DATA_TYPE_JSON);
            i.putExtra(MsgHandler.KEY_TIMESTAMP, System.currentTimeMillis());
            this.context.startService(i);
        }
        if (sampleDelay > 500 && LightSensingActive) {
            // unregister the listener and start again in sampleDelay seconds
            stopLightSensing();
            LightHandler.postDelayed(LightThread = new Runnable() {

                @Override
                public void run() {
                    startLightSensing(sampleDelay);
                }
            }, sampleDelay);
        }
    }

    public void setSampleDelay(long _sampleDelay) {
        sampleDelay = _sampleDelay;
    }

    public void startLightSensing(long _sampleDelay) {
        LightSensingActive = true;
        setSampleDelay(_sampleDelay);
        for (Sensor sensor : sensors) {
            if (sensor.getType() == Sensor.TYPE_LIGHT) {
                // Log.d(TAG, "registering for sensor " + sensor.getName());
                smgr.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    public void stopLightSensing() {
        try {
            LightSensingActive = false;
            smgr.unregisterListener(this);

            if (LightThread != null)
                LightHandler.removeCallbacks(LightThread);
            LightThread = null;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }

    public long getSampleDelay() {
        return sampleDelay;
    }
}
