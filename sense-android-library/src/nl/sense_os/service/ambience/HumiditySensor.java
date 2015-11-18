/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.ambience;

import java.math.BigDecimal;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.PeriodicPollAlarmReceiver;
import nl.sense_os.service.shared.PeriodicPollingSensor;
import nl.sense_os.service.shared.SensorDataPoint;
import nl.sense_os.service.subscription.BaseSensor;
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
public class HumiditySensor extends BaseSensor implements SensorEventListener,
        PeriodicPollingSensor {

    private static HumiditySensor sInstance = null;
    private static final String TAG = "HumiditySensor";

    /**
     * Factory method to get the singleton instance.
     * 
     * @param context
     * @return instance
     */
    public static HumiditySensor getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new HumiditySensor(context);
        }
        return sInstance;
    }

    private boolean mActive;
    private Context mContext;
    private PeriodicPollAlarmReceiver mSampleAlarmReceiver;
    private SensorManager mSensorMgr;

    /**
     * Constructor.
     * 
     * @param context
     * @see #getInstance(Context)
     */
    protected HumiditySensor(Context context) {
        this.mContext = context;
        mSampleAlarmReceiver = new PeriodicPollAlarmReceiver(this);
        mSensorMgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void doSample() {
        //Log.v(TAG, "Do sample");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            Sensor sensor = mSensorMgr.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
            if (null != sensor) {
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
        // nothing to do
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;

        String sensorName = SensorNames.RELATIVE_HUMIDITY;

        // relative humidity in percent
        float value = BigDecimal.valueOf(event.values[0]).setScale(2, 0).floatValue();

        this.notifySubscribers();
        SensorDataPoint dataPoint = new SensorDataPoint(value);
        dataPoint.sensorName = sensorName;
        dataPoint.sensorDescription = sensor.getName();
        dataPoint.timeStamp = SNTP.getInstance().getTime();
        this.sendToSubscribers(dataPoint);

        // done with sample
        stopSample();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void startSensing(long sampleDelay) {
        Log.v(TAG, "Start sensing");

        boolean found = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            Sensor sensor = mSensorMgr.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
            if (null != sensor) {
                found = true;
            }
        }
        if (!found) {
            Log.w(TAG, "No humidity sensor available!");
            return;
        }

        setSampleRate(sampleDelay);

        // start polling
        mActive = true;
        mSampleAlarmReceiver.start(mContext);

        // do the first sample immediately
        doSample();
    }

    private void stopSample() {
        Log.v(TAG, "Stop sample");
        try {
            mSensorMgr.unregisterListener(this);
        } catch (Exception e) {
            Log.e(TAG, "Error stopping humidity sensor: " + e);
        }
    }

    @Override
    public void stopSensing() {
        Log.v(TAG, "Stop sensing");

        // stop polling
        mSampleAlarmReceiver.stop(mContext);
        mActive = false;

        // stop sample
        stopSample();
    }
}
