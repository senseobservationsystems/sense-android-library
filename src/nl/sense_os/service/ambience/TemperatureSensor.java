/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.ambience;

import java.util.HashMap;
import java.util.Map;

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

public class TemperatureSensor implements SensorEventListener {

    private static final String TAG = "Sense Temperature Sensor";

    private Context context;
    private SensorManager sensorManager;

    private long sampleDelay = 0; // in milliseconds
    private long lastSampleTime;

    private Handler handler = new Handler();
    private Runnable startSampleTask = null;
    private boolean sensorActive = false;

    public TemperatureSensor(Context context) {
        this.context = context;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // nothing to do
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (event.timestamp > lastSampleTime + sampleDelay) {
            lastSampleTime = event.timestamp;

            String sensorName = SensorNames.AMBIENT_TEMPERATURE;

            Map<String, Object> jsonFields = new HashMap<String, Object>();
            jsonFields.put("celsius", event.values[0]);
            String value = new JSONObject(jsonFields).toString();

            // send msg to MsgHandler
            Intent i = new Intent(context.getString(R.string.action_sense_new_data));
            i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
            i.putExtra(DataPoint.VALUE, value);
            i.putExtra(DataPoint.SENSOR_NAME, sensorName);
            i.putExtra(DataPoint.SENSOR_DESCRIPTION, sensor.getName());
            i.putExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime());
            context.startService(i);
        }
        if (sampleDelay > 500 && sensorActive) {
            // unregister the listener and start again in sampleDelay seconds
            stopSensing();

            startSampleTask = new Runnable() {

                @Override
                public void run() {
                    startSensing(sampleDelay);
                }
            };
            handler.postDelayed(startSampleTask, sampleDelay);
        }
    }

    public void setSampleDelay(long sampleDelay) {
        this.sampleDelay = sampleDelay;
    }

    public void startSensing(long sampleDelay) {
        sensorActive = true;
        setSampleDelay(sampleDelay);

        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        if (null != sensor) {
            // Log.d(TAG, "registering for sensor " + sensor.getName());
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void stopSensing() {
        try {
            sensorActive = false;

            sensorManager.unregisterListener(this);

            if (startSampleTask != null) {
                handler.removeCallbacks(startSampleTask);
                startSampleTask = null;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error stopping temperature sensor: " + e);
        }
    }
}
