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
import nl.sense_os.service.shared.SensorDataPoint;
import nl.sense_os.service.subscription.BaseSensor;
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
public class PressureSensor extends BaseSensor implements SensorEventListener,
        PeriodicPollingSensor {

    private static PressureSensor sInstance = null;
    private static final String TAG = "PressureSensor";

    /**
     * Factory method to get the singleton instance.
     * 
     * @param context
     * @return instance
     */
    public static PressureSensor getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new PressureSensor(context);
        }
        return sInstance;
    }

    private boolean mActive = false;
    private PeriodicPollAlarmReceiver mAlarmReceiver;
    private Context mContext;
    private long[] mLastSampleTimes = new long[50];
    private SensorManager mSensorMgr;
    private List<Sensor> mSensors;
    private WakeLock mWakeLock;

    /**
     * Constructor.
     * 
     * @param context
     * @see #getInstance(Context)
     */
    protected PressureSensor(Context context) {
        mContext = context;
        mSensorMgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensors = new ArrayList<Sensor>();
        if (null != mSensorMgr.getDefaultSensor(Sensor.TYPE_PRESSURE)) {
            mSensors.add(mSensorMgr.getDefaultSensor(Sensor.TYPE_PRESSURE));
        }

        mAlarmReceiver = new PeriodicPollAlarmReceiver(this);
    }

    @Override
    public void doSample() {
       //Log.v(TAG, "Do sample");

        // acquire wake lock
        if (null == mWakeLock) {
            PowerManager powerMgr = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            mWakeLock = powerMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        }
        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire();
        } else {
            // Log.v(TAG, "Wake lock already held");
        }

        // register as sensor listener
        for (Sensor sensor : mSensors) {
            if (sensor.getType() == Sensor.TYPE_PRESSURE) {
                // Log.d(TAG, "registering for sensor " + sensor.getName());
                mSensorMgr.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    /**
     * @return The delay between samples in milliseconds
     */
    @Override
    public boolean isActive() {
        return mActive;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (SNTP.getInstance().getTime() > mLastSampleTimes[sensor.getType()] + getSampleRate()) {
            mLastSampleTimes[sensor.getType()] = SNTP.getInstance().getTime();

            String sensorName = SensorNames.PRESSURE;

            // value is millibar, convert to Pascal
            float millibar = event.values[0];
            float pascal = millibar * 100;
            float value = BigDecimal.valueOf(pascal).setScale(3, 0).floatValue();

            this.notifySubscribers();
            SensorDataPoint dataPoint = new SensorDataPoint(value);
            dataPoint.sensorName = sensorName;
            dataPoint.sensorDescription = sensor.getName();
            dataPoint.timeStamp = SNTP.getInstance().getTime();
            this.sendToSubscribers(dataPoint);

            // send msg to MsgHandler
            Intent i = new Intent(mContext.getString(R.string.action_sense_new_data));
            i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.FLOAT);
            i.putExtra(DataPoint.VALUE, value);
            i.putExtra(DataPoint.SENSOR_NAME, sensorName);
            i.putExtra(DataPoint.SENSOR_DESCRIPTION, sensor.getName());
            i.putExtra(DataPoint.TIMESTAMP, dataPoint.timeStamp);
            i.setPackage(mContext.getPackageName());
            mContext.startService(i);

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
        super.setSampleRate(sampleDelay);
        stopPolling();
        startPolling();
    }

    private void startPolling() {
        Log.v(TAG, "Start polling");
        mAlarmReceiver.start(mContext);
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
        Log.v(TAG, "Start sensing");

        // check if the sensor is physically present on this phone
        boolean found = false;
        for (Sensor sensor : mSensors) {
            if (sensor.getType() == Sensor.TYPE_PRESSURE) {
                found = true;
                break;
            }
        }
        if (!found) {
            Log.w(TAG, "No pressure sensor!");
            return;
        }

        mActive = true;
        setSampleRate(sampleRate);

        // do the first sample immediately
        doSample();
    }

    private void stopPolling() {
        Log.v(TAG, "Stop polling");
        mAlarmReceiver.stop(mContext);
        stopSample();
    }

    private void stopSample() {
        Log.v(TAG, "Stop sample");

        // release wake lock
        if (null != mWakeLock && mWakeLock.isHeld()) {
            mWakeLock.release();
        }

        // unregister sensor listener
        try {
            mSensorMgr.unregisterListener(this);
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * Stops the periodic sampling.
     */
    @Override
    public void stopSensing() {
        Log.v(TAG, "Stop sensing");
        stopPolling();
        mActive = false;
    }
}