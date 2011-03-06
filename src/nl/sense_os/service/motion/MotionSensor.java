/*
 * ***********************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 * **
 * ************************************************************************************************
 * *********
 */
package nl.sense_os.service.motion;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

import nl.sense_os.service.Constants;
import nl.sense_os.service.MsgHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class MotionSensor implements SensorEventListener {

    private static final String NAME_ACCELR = "accelerometer";
    private static final String NAME_GYRO = "gyroscope";
    private static final String NAME_MAGNET = "magnetic_field";
    private static final String NAME_ORIENT = "orientation";
    private static final String TAG = "Sense MotionSensor";
    private FallDetector fallDetector;
    private boolean useFallDetector;
    private boolean firstStart = true;
    private Context context;
    private long[] lastSampleTimes = new long[50];
    private Handler motionHandler = new Handler();
    private boolean motionSensingActive = false;
    private Runnable motionThread = null;
    private long sampleDelay = 0; // in milliseconds
    private List<Sensor> sensors;
    private SensorManager smgr;

    public MotionSensor(Context context) {
        this.context = context;
        smgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensors = smgr.getSensorList(Sensor.TYPE_ALL);
        fallDetector = new FallDetector();
    }

    public long getSampleDelay() {
        return sampleDelay;
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

        if (useFallDetector && sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float aX = event.values[1];
            float aY = event.values[0];
            float aZ = event.values[2];
            float accVecSum = (float) Math.sqrt((aX * aX) + (aY * aY) + (aZ * aZ));

            if (fallDetector.fallDetected(accVecSum))
                sendFallMessage(true); // send msg

        }
        if (System.currentTimeMillis() > lastSampleTimes[sensor.getType()] + sampleDelay) {
            lastSampleTimes[sensor.getType()] = System.currentTimeMillis();

            String sensorName = "";
            switch (sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                sensorName = NAME_ACCELR;
                break;
            case Sensor.TYPE_ORIENTATION:
                sensorName = NAME_ORIENT;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                sensorName = NAME_MAGNET;
                break;
            case Sensor.TYPE_GYROSCOPE:
                sensorName = NAME_GYRO;
                break;
            }

            JSONObject json = new JSONObject();
            int axis = 0;
            try {
                for (float value : event.values) {
                    switch (axis) {
                    case 0:
                        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER
                                || sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                            json.put("x-axis", value);
                        } else if (sensor.getType() == Sensor.TYPE_ORIENTATION
                                || sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                            json.put("azimuth", value);
                        }
                        break;
                    case 1:
                        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER
                                || sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                            json.put("y-axis", value);
                        } else if (sensor.getType() == Sensor.TYPE_ORIENTATION
                                || sensor.getType() == Sensor.TYPE_GYROSCOPE)
                            json.put("pitch", value);
                        break;
                    case 2:
                        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER
                                || sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                            json.put("z-axis", value);
                        } else if (sensor.getType() == Sensor.TYPE_ORIENTATION
                                || sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                            json.put("roll", value);
                        }
                        break;
                    }
                    axis++;
                }
            } catch (JSONException e) {
                Log.e(TAG, "JSONException in onSensorChanged", e);
                return;
            }

            // pass message to the MsgHandler
            Intent i = new Intent(MsgHandler.ACTION_NEW_MSG);
            i.putExtra(MsgHandler.KEY_SENSOR_NAME, sensorName);
            i.putExtra(MsgHandler.KEY_SENSOR_DEVICE, sensor.getName());
            i.putExtra(MsgHandler.KEY_VALUE, json.toString());
            i.putExtra(MsgHandler.KEY_DATA_TYPE, Constants.SENSOR_DATA_TYPE_JSON);
            i.putExtra(MsgHandler.KEY_TIMESTAMP, System.currentTimeMillis());
            this.context.startService(i);
        }
        if (sampleDelay > 500 && motionSensingActive && !useFallDetector) {
            // unregister the listener and start again in sampleDelay seconds
            stopMotionSensing();
            motionHandler.postDelayed(motionThread = new Runnable() {

                @Override
                public void run() {
                    startMotionSensing(sampleDelay);
                }
            }, sampleDelay);
        }
    }

    public void setSampleDelay(long _sampleDelay) {
        sampleDelay = _sampleDelay;
    }

    private void sendFallMessage(boolean fall) {
        Intent i = new Intent(MsgHandler.ACTION_NEW_MSG);
        i.putExtra(MsgHandler.KEY_SENSOR_NAME, "fall detector");
        i.putExtra(MsgHandler.KEY_SENSOR_DEVICE, fallDetector.demo ? "demo fall" : "human fall");
        i.putExtra(MsgHandler.KEY_VALUE, fall);
        i.putExtra(MsgHandler.KEY_DATA_TYPE, Constants.SENSOR_DATA_TYPE_BOOL);
        i.putExtra(MsgHandler.KEY_TIMESTAMP, System.currentTimeMillis());
        this.context.startService(i);
    }

    public void startMotionSensing(long _sampleDelay) {
        // check if the falldetector is enabled
        final SharedPreferences mainPrefs = context.getSharedPreferences(Constants.MAIN_PREFS,
                Context.MODE_WORLD_WRITEABLE);
        useFallDetector = mainPrefs.getBoolean(Constants.PREF_MOTION_FALL_DETECT, true);
        if (fallDetector.demo = mainPrefs.getBoolean(Constants.PREF_MOTION_FALL_DETECT_DEMO, false)) {
            useFallDetector = true;
        }

        if (firstStart && useFallDetector) {
            sendFallMessage(false);
            firstStart = false;
        }

        motionSensingActive = true;
        setSampleDelay(_sampleDelay);
        for (Sensor sensor : sensors) {
            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER
                    || sensor.getType() == Sensor.TYPE_ORIENTATION
                    || sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                // Log.d(TAG, "registering for sensor " + sensor.getName());
                smgr.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    public void stopMotionSensing() {
        try {
            motionSensingActive = false;
            smgr.unregisterListener(this);

            if (motionThread != null) {
                motionHandler.removeCallbacks(motionThread);
                motionThread = null;
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }
}
