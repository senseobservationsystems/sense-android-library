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
import nl.sense_os.service.ctrl.Controller;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.PeriodicPollAlarmReceiver;
import nl.sense_os.service.shared.PeriodicPollingSensor;
import nl.sense_os.service.shared.SensorDataPoint;
import nl.sense_os.service.subscription.BaseSensor;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Represents the standard light sensor. Registers itself for updates from the Android
 * {@link SensorManager}.
 * 
 * @author Ted Schmidt <ted@sense-os.nl>
 */
public class LightSensor extends BaseSensor implements SensorEventListener, PeriodicPollingSensor {

    private static final String TAG = "Sense Light Sensor";
    private static LightSensor instance = null;

    /**
     * Factory method to get the singleton instance.
     * 
     * @param context
     * @return instance
     */
    public static LightSensor getInstance(Context context) {
        if (instance == null) {
            instance = new LightSensor(context);
        }

        return instance;
    }

    private long[] lastSampleTimes = new long[50];
    private Context context;
    private List<Sensor> sensors;
    private SensorManager smgr;
    private PeriodicPollAlarmReceiver pollAlarmReceiver;
    private Controller controller;
    private boolean active;
    private boolean listening;

    /**
     * Constructor.
     * 
     * @param context
     * @see #getInstance(Context)
     */
    protected LightSensor(Context context) {
        this.context = context;
        smgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        pollAlarmReceiver = new PeriodicPollAlarmReceiver(this);
        controller = Controller.getController(context);
        sensors = new ArrayList<Sensor>();
        if (null != smgr.getDefaultSensor(Sensor.TYPE_LIGHT)) {
            sensors.add(smgr.getDefaultSensor(Sensor.TYPE_LIGHT));
        }
    }

    @Override
    public void doSample() {
       //Log.v(TAG, "Do sample");
        for (Sensor sensor : sensors) {
            if (sensor.getType() == Sensor.TYPE_LIGHT) {
                // Log.d(TAG, "Register for sensor " + sensor.getName());

                if (listening) {
                    stopSample();
                }

                listening = smgr.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
                if (!listening) {
                    Log.w(TAG, "Failed to register for light sensor!");
                }
            }
        }
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (SNTP.getInstance().getTime() > lastSampleTimes[sensor.getType()] + getSampleRate()) {
            lastSampleTimes[sensor.getType()] = SNTP.getInstance().getTime();

            String sensorName = "";
            if (sensor.getType() == Sensor.TYPE_LIGHT) {
                sensorName = SensorNames.LIGHT;
            }

            float value = event.values[0];
            long time = SNTP.getInstance().getTime();
            try {
                this.notifySubscribers();
                SensorDataPoint dataPoint = new SensorDataPoint(value);
                dataPoint.sensorName = sensorName;
                dataPoint.sensorDescription = sensor.getName();
                dataPoint.timeStamp = time;
                this.sendToSubscribers(dataPoint);
            } catch (Exception e) {
                Log.e(TAG, "Error in send to subscribers of the light sensor");
            }
            stopSample();
        }
    }

    @Override
    public void startSensing(long sampleDelay) {
        //Log.v(TAG, "Start sensing");

        // check if the sensor is physically present on this phone
        boolean found = false;
        for (Sensor sensor : sensors) {
            if (sensor.getType() == Sensor.TYPE_LIGHT) {
                found = true;
                break;
            }
        }
        if (!found) {
            Log.w(TAG, "No light field sensor!");
            return;
        }

        setSampleRate(sampleDelay);

        active = true;
        pollAlarmReceiver.start(context);

        // do the first sample immediately
        doSample();
    }

    /**
     * Unregisters the listener
     */
    private void stopSample() {
        //Log.v(TAG, "Stop sample");
        try {
            smgr.unregisterListener(this);
            listening = false;
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop light sensor sample!", e);
        }
    }

    @Override
    public void stopSensing() {
        //Log.v(TAG, "Stop sensing");
        pollAlarmReceiver.stop(context);
        stopSample();
        active = false;
    }
}
