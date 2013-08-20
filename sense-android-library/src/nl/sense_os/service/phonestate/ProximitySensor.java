/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.phonestate;

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
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.util.Log;

/**
 * Represents the proximity sensor. Listens for proximity sensor data from the Android
 * SensorManager.
 * 
 * @author Ted Schmidt <ted@sense-os.nl>
 */
public class ProximitySensor extends BaseSensor implements SensorEventListener,
        PeriodicPollingSensor {

    private static ProximitySensor sInstance = null;
    private static final String TAG = "ProximitySensor";
    private static final int REQ_CODE = 1;
    private static final String ACTION_STOP_SAMPLE = ProximitySensor.class.getName() + ".STOP";

    /**
     * Factory method to get the singleton instance.
     * 
     * @param context
     * @return instance
     */
    public static ProximitySensor getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ProximitySensor(context);
        }
        return sInstance;
    }

    private boolean mActive;
    private Context mContext;
    private SensorManager mSensorMgr;
    private List<Sensor> mSensors;
    private WakeLock mWakeLock;
    private float mLatestValue;
    private PeriodicPollAlarmReceiver mStartSampleReceiver;
    private BroadcastReceiver mStopSampleReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            handleLatestValue();
            stopSample();
        }
    };

    /**
     * Constructor.
     * 
     * @param context
     * @see #getInstance(Context)
     */
    protected ProximitySensor(Context context) {
        mContext = context;
        mSensorMgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensors = new ArrayList<Sensor>();
        if (null != mSensorMgr.getDefaultSensor(Sensor.TYPE_PROXIMITY)) {
            mSensors.add(mSensorMgr.getDefaultSensor(Sensor.TYPE_PROXIMITY));
        }
        mStartSampleReceiver = new PeriodicPollAlarmReceiver(this);
    }

    @Override
    public void doSample() {
        Log.v(TAG, "Do sample");

        // acquire wake lock
        if (null == mWakeLock) {
            PowerManager powerMgr = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            mWakeLock = powerMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        }
        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire();
        } else {
            // wake lock already held
        }

        // register as sensor listener
        for (Sensor sensor : mSensors) {
            if (sensor.getType() == Sensor.TYPE_PROXIMITY) {
                // Log.d(TAG, "registering for sensor " + sensor.getName());
                mSensorMgr.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }

        // reset sample state
        mLatestValue = Float.NaN;

        // set alarm for stop the sample
        mContext.registerReceiver(mStopSampleReceiver, new IntentFilter(ACTION_STOP_SAMPLE));
        Intent intent = new Intent(ACTION_STOP_SAMPLE);
        PendingIntent operation = PendingIntent.getBroadcast(mContext, REQ_CODE, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmMgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 500,
                operation);
    }

    /**
     * Accepts the latest sensor value and send it to the MsgHandler.
     */
    private void handleLatestValue() {

        // if the value is NaN, no sample was collected: assume nothing in proximity
        float value = Float.isNaN(mLatestValue) || mLatestValue > 0 ? 1 : 0;

        Sensor sensor = mSensorMgr.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        try {
            notifySubscribers();
            SensorDataPoint dataPoint = new SensorDataPoint(value);
            dataPoint.sensorName = SensorNames.PROXIMITY;
            dataPoint.sensorDescription = sensor.getName();
            dataPoint.timeStamp = SNTP.getInstance().getTime();
            sendToSubscribers(dataPoint);
        } catch (Exception e) {
            Log.e(TAG, "Error in send data to subscribers in ProximitySensor");
        }

        // pass message to the MsgHandler
        Intent i = new Intent(mContext.getString(R.string.action_sense_new_data));
        i.putExtra(DataPoint.SENSOR_NAME, SensorNames.PROXIMITY);
        i.putExtra(DataPoint.SENSOR_DESCRIPTION, sensor.getName());
        i.putExtra(DataPoint.VALUE, value);
        i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.FLOAT);
        i.putExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime());
        mContext.startService(i);
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

        if (event.values.length < 1 || sensor.getType() != Sensor.TYPE_PROXIMITY
                || Float.isNaN(event.values[0])) {
            // unusable value
            return;
        }

        // scale to meters
        mLatestValue = event.values[0];
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
        mStartSampleReceiver.start(mContext);
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
            if (sensor.getType() == Sensor.TYPE_PROXIMITY) {
                found = true;
                break;
            }
        }
        if (!found) {
            Log.w(TAG, "No proximity sensor!");
            return;
        }

        mActive = true;
        setSampleRate(sampleRate);

        // do the first sample immediately
        doSample();
    }

    private void stopPolling() {
        Log.v(TAG, "Stop polling");
        mStartSampleReceiver.stop(mContext);
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
            Log.e(TAG, "Failed to stop proximity field sample!", e);
        }
        // unregister broadcast receiver
        try {
            mContext.unregisterReceiver(mStopSampleReceiver);
        } catch (IllegalArgumentException e) {
            // ignore
        }
    }

    @Override
    public void stopSensing() {
        Log.v(TAG, "Stop sensing");
        stopPolling();
        mActive = false;
    }
}
