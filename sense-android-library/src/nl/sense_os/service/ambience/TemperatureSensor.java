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
import nl.sense_os.service.shared.BaseSensor;
import nl.sense_os.service.shared.PeriodicPollAlarmReceiver;
import nl.sense_os.service.shared.PeriodicPollingSensor;
import nl.sense_os.service.shared.SensorDataPoint;

import org.json.JSONObject;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;

/**
 * Represents the temperature sensor. Registers itself for updates from the Android
 * {@link SensorManager}.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class TemperatureSensor extends BaseSensor implements SensorEventListener,
        PeriodicPollingSensor {

    private static final String TAG = "Sense Temperature Sensor";

    /**
     * Factory method to get the singleton instance.
     * 
     * @param context
     * @return instance
     */
    public static TemperatureSensor getInstance(Context context) {
        if (instance == null) {
            instance = new TemperatureSensor(context);
        }
        return instance;
    }

    private Context context;
    private SensorManager sensorManager;
    private long lastSampleTime;
    private PeriodicPollAlarmReceiver pollAlarmReceiver;
    private boolean active;
    private static TemperatureSensor instance = null;

    /**
     * Constructor.
     * 
     * @param context
     * @see #getInstance(Context)
     */
    protected TemperatureSensor(Context context) {
        this.context = context;
        pollAlarmReceiver = new PeriodicPollAlarmReceiver(this);
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void doSample() {
        Log.v(TAG, "Do sample");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
            if (null != sensor) {
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // nothing to do
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (event.timestamp > lastSampleTime + getSampleRate()) {
            Log.v(TAG, "Check temperature");
            lastSampleTime = event.timestamp;

            String sensorName = SensorNames.AMBIENT_TEMPERATURE;

            Map<String, Object> jsonFields = new HashMap<String, Object>();
            jsonFields.put("celsius", event.values[0]);
            JSONObject jsonObj = new JSONObject(jsonFields);
            String value = jsonObj.toString();

            this.notifySubscribers();
            SensorDataPoint dataPoint = new SensorDataPoint(jsonObj);
            dataPoint.sensorName = sensorName;
            dataPoint.sensorDescription = sensor.getName();
            dataPoint.timeStamp = SNTP.getInstance().getTime();
            this.sendToSubscribers(dataPoint);

            // send msg to MsgHandler
            Intent i = new Intent(context.getString(R.string.action_sense_new_data));
            i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
            i.putExtra(DataPoint.VALUE, value);
            i.putExtra(DataPoint.SENSOR_NAME, sensorName);
            i.putExtra(DataPoint.SENSOR_DESCRIPTION, sensor.getName());
            i.putExtra(DataPoint.TIMESTAMP, dataPoint.timeStamp);
            context.startService(i);

            // done with sample
            stopSample();
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void startSensing(long sampleDelay) {
        Log.v(TAG, "Start sensing");

        boolean found = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
            if (null != sensor) {
                found = true;
            }
        }
        if (!found) {
            Log.w(TAG, "No temperature sensor available!");
            return;
        }

        setSampleRate(sampleDelay);

        active = true;
        pollAlarmReceiver.start(context);

        // do the first sample immediately
        doSample();
    }

    private void stopSample() {
        Log.v(TAG, "Stop sample");
        try {
            sensorManager.unregisterListener(this);
        } catch (Exception e) {
            Log.e(TAG, "Error stopping temperature sensor: " + e);
        }
    }

    @Override
    public void stopSensing() {
        Log.v(TAG, "Stop sensing");
        pollAlarmReceiver.stop(context);
        stopSample();
        active = false;
    }
}
