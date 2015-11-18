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

    private static final String SENSOR_DISPLAY_NAME = "magnetic field";
    private static MagneticFieldSensor sInstance;
    private static final String TAG = "MagneticFieldSensor";

    /**
     * Factory method to get the singleton instance.
     * 
     * @param context
     * @return instance
     */
    public static MagneticFieldSensor getInstance(Context context) {
        if (null == sInstance) {
            sInstance = new MagneticFieldSensor(context);
        }
        return sInstance;
    }

    private boolean mActive;
    private Context mContext;
    private PeriodicPollAlarmReceiver mSampleAlarmReceiver;
    private SensorManager mSensorMgr;
    private List<Sensor> mSensors;
    private WakeLock mWakeLock;

    /**
     * Constructor.
     * 
     * @param context
     * @see #getInstance(Context)
     */
    protected MagneticFieldSensor(Context context) {
        mContext = context;
        mSensorMgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensors = new ArrayList<Sensor>();
        if (null != mSensorMgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)) {
            mSensors.add(mSensorMgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
        }
        mSampleAlarmReceiver = new PeriodicPollAlarmReceiver(this);
    }

    @Override
    public void doSample() {
        //Log.v(TAG, "Do sample");

        // acquire wake lock
        if (null == mWakeLock) {
            PowerManager powerMgr = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            mWakeLock = powerMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            mWakeLock.setReferenceCounted(false);
        }
        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire();
        } else {
            // wake lock already held
        }

        // register as sensor listener
        for (Sensor sensor : mSensors) {
            if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                // Log.v(TAG, "registering for sensor " + sensor.getName());
                mSensorMgr.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    @Override
    public boolean isActive() {
        return mActive;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not used
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;

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

            this.notifySubscribers();
            SensorDataPoint dataPoint = new SensorDataPoint(jsonObj);
            dataPoint.sensorName = sensorName;
            dataPoint.sensorDescription = sensor.getName();
            dataPoint.timeStamp = SNTP.getInstance().getTime();
            this.sendToSubscribers(dataPoint);

            // sample is successful: unregister the listener
            stopSample();
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
        super.setSampleRate(sampleDelay);
        startPolling();
    }

    private void startPolling() {
        //Log.v(TAG, "Start polling");
        mSampleAlarmReceiver.start(mContext);
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
        //Log.v(TAG, "Start sensing");

        // check if the sensor is physically present on this phone
        boolean found = false;
        for (Sensor sensor : mSensors) {
            if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                found = true;
                break;
            }
        }
        if (!found) {
            Log.w(TAG, "No magnetic field sensor!");
            return;
        }

        mActive = true;
        setSampleRate(sampleRate);

        // do the first sample immediately
        doSample();
    }

    private void stopPolling() {
        //Log.v(TAG, "Stop polling");
        mSampleAlarmReceiver.stop(mContext);
        stopSample();
    }

    private void stopSample() {
        //Log.v(TAG, "Stop sample");

        // release wake lock
        if (null != mWakeLock && mWakeLock.isHeld()) {
            mWakeLock.release();
        }

        // unregister sensor listener
        try {
            mSensorMgr.unregisterListener(this);
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop magnetic field sample!", e);
        }

    }

    /**
     * Stops the periodic sampling.
     */
    @Override
    public void stopSensing() {
        //Log.v(TAG, "Stop sensing");
        mActive = false;
        stopPolling();

    }
}
