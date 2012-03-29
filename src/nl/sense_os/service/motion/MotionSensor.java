/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.motion;

import java.math.BigDecimal;
import java.util.ArrayList;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main.Motion;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.states.EpiStateMonitor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.FloatMath;
import android.util.Log;

public class MotionSensor implements SensorEventListener {

    /**
     * BroadcastReceiver that listens for screen state changes. Re-registers the motion sensor when
     * the screen turns off.
     */
    private class ScreenOffListener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Check action just to be on the safe side.
            if (false == intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                return;
            }

            SharedPreferences prefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
                    Context.MODE_PRIVATE);
            boolean useFix = prefs.getBoolean(Motion.SCREENOFF_FIX, false);
            if (useFix) {
                // wait half a second and re-register
                Runnable restartSensing = new Runnable() {

                    @Override
                    public void run() {
                        // Unregisters the motion listener and registers it again.
                        // Log.v(TAG, "Screen went off, re-registering the Motion sensor");
                        stopMotionSensing();
                        startMotionSensing(sampleDelay);
                    };
                };

                new Handler().postDelayed(restartSensing, 500);
            }
        }
    }

    private static final String TAG = "Sense Motion";

    private final BroadcastReceiver screenOffListener = new ScreenOffListener();

    private final FallDetector fallDetector = new FallDetector();
    private final Context context;
    private boolean isFallDetectMode;
    private boolean isEnergyMode;
    private boolean isEpiMode;
    private boolean isUnregisterWhenIdle;
    private boolean firstStart = true;
    private ArrayList<Sensor> sensors;
    private final long[] lastSampleTimes = new long[50];
    private Handler motionHandler = new Handler();
    private boolean motionSensingActive = false;
    private Runnable motionThread = null;
    private long sampleDelay = 0; // in milliseconds
    private long[] lastLocalSampleTimes = new long[50];
    private long localBufferTime = 15 * 1000;
    private long firstTimeSend = 0;
    private JSONArray[] dataBuffer = new JSONArray[10];

    // members for calculating the avg speed change during a time period, for motion energy sensor
    private static final long ENERGY_SAMPLE_LENGTH = 500;
    private long energySampleStart = 0;
    private long prevEnergySampleTime;
    private double avgSpeedChange;
    private int avgSpeedCount;
    private boolean hasLinAccSensor;
    private float[] gravity = { 0, 0, SensorManager.GRAVITY_EARTH };

    // members for waking up the device for sampling
    private static final String ACTION_WAKEUP_ALARM = "nl.sense_os.service.MotionWakeUp";
    private static final int ALARM_ID = 256;

    private BroadcastReceiver wakeReceiver;
    private WakeLock wakeLock;

    private boolean isRegistered;
    private long lastRegistered = -1;
    private static final long DELAY_AFTER_REGISTRATION = 500;

    public MotionSensor(Context context) {
        this.context = context;
    }

    /**
     * Calculates the linear acceleration of a raw accelerometer sample. Tries to determine the
     * gravity component by putting the signal through a first-order low-pass filter.
     * 
     * @param values
     *            Array with accelerometer values for the three axes.
     * @return The approximate linear acceleration of the sample.
     */
    private float[] calcLinAcc(float[] values) {

        // low-pass filter raw accelerometer data to approximate the gravity
        final float alpha = 0.8f; // filter constants should depend on sample rate
        gravity[0] = alpha * gravity[0] + (1 - alpha) * values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * values[2];

        return new float[] { values[0] - gravity[0], values[1] - gravity[1], values[2] - gravity[2] };
    }

    private JSONObject createJsonValue(SensorEvent event) {

        final Sensor sensor = event.sensor;
        final JSONObject json = new JSONObject();

        int axis = 0;
        try {
            for (double value : event.values) {
                // scale to three decimal precision
                value = BigDecimal.valueOf(value).setScale(3, 0).doubleValue();

                switch (axis) {
                case 0:
                    if (sensor.getType() == Sensor.TYPE_ACCELEROMETER
                            || sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD
                            || sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                        json.put("x-axis", value);
                    } else if (sensor.getType() == Sensor.TYPE_ORIENTATION
                            || sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                        json.put("azimuth", value);
                    } else {
                        Log.e(TAG, "Unexpected sensor type creating JSON value");
                        return null;
                    }
                    break;
                case 1:
                    if (sensor.getType() == Sensor.TYPE_ACCELEROMETER
                            || sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD
                            || sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                        json.put("y-axis", value);
                    } else if (sensor.getType() == Sensor.TYPE_ORIENTATION
                            || sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                        json.put("pitch", value);
                    } else {
                        Log.e(TAG, "Unexpected sensor type creating JSON value");
                        return null;
                    }
                    break;
                case 2:
                    if (sensor.getType() == Sensor.TYPE_ACCELEROMETER
                            || sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD
                            || sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                        json.put("z-axis", value);
                    } else if (sensor.getType() == Sensor.TYPE_ORIENTATION
                            || sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                        json.put("roll", value);
                    } else {
                        Log.e(TAG, "Unexpected sensor type creating JSON value");
                        return null;
                    }
                    break;
                default:
                    Log.w(TAG, "Unexpected sensor value! More than three axes?!");
                }
                axis++;
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSONException creating motion JSON value", e);
            return null;
        }

        return json;
    }

    /**
     * Measures the speed change and determines the average, for the motion energy sensor.
     * 
     * @param event
     *            The sensor change event with accelerometer or linear acceleration data.
     */
    private void doEnergySample(SensorEvent event) {

        float[] linAcc = null;

        // approximate linear acceleration if we have no special sensor for it
        if (!hasLinAccSensor && Sensor.TYPE_ACCELEROMETER == event.sensor.getType()) {
            linAcc = calcLinAcc(event.values);
        } else if (hasLinAccSensor && Sensor.TYPE_LINEAR_ACCELERATION == event.sensor.getType()) {
            linAcc = event.values;
        } else {
            // sensor is not the right type
            return;
        }

        // calculate speed change and adjust average
        if (null != linAcc) {

            // record the start of the motion sample
            if (avgSpeedCount == 0) {
                energySampleStart = System.currentTimeMillis();
            }

            float timeStep = (System.currentTimeMillis() - prevEnergySampleTime) / 1000f;
            prevEnergySampleTime = System.currentTimeMillis();
            if (timeStep > 0 && timeStep < 1) {
                float accLength = FloatMath.sqrt((float) (Math.pow(linAcc[0], 2)
                        + Math.pow(linAcc[1], 2) + Math.pow(linAcc[2], 2)));

                float speedChange = accLength * timeStep;
                // Log.v(TAG, "Speed change: " + speedChange);

                avgSpeedChange = (avgSpeedCount * avgSpeedChange + speedChange)
                        / (avgSpeedCount + 1);
                avgSpeedCount++;
            }
        }
    }

    private void doEpiSample(Sensor sensor, JSONObject json) {

        if (dataBuffer[sensor.getType()] == null) {
            dataBuffer[sensor.getType()] = new JSONArray();
        }
        dataBuffer[sensor.getType()].put(json);
        if (lastLocalSampleTimes[sensor.getType()] == 0) {
            lastLocalSampleTimes[sensor.getType()] = System.currentTimeMillis();
        }

        if (System.currentTimeMillis() > lastLocalSampleTimes[sensor.getType()] + localBufferTime) {
            // send the stuff
            // Log.v(TAG, "Transmit accelerodata: " + dataBuffer[sensor.getType()].length());
            // pass message to the MsgHandler
            Intent i = new Intent(context.getString(R.string.action_sense_new_data));
            i.putExtra(DataPoint.SENSOR_NAME, SensorNames.ACCELEROMETER_EPI);
            i.putExtra(DataPoint.SENSOR_DESCRIPTION, sensor.getName());
            i.putExtra(
                    DataPoint.VALUE,
                    "{\"interval\":"
                            + Math.round(localBufferTime / dataBuffer[sensor.getType()].length())
                            + ",\"data\":" + dataBuffer[sensor.getType()].toString() + "}");
            i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON_TIME_SERIES);
            i.putExtra(DataPoint.TIMESTAMP, lastLocalSampleTimes[sensor.getType()]);
            context.startService(i);
            dataBuffer[sensor.getType()] = new JSONArray();
            lastLocalSampleTimes[sensor.getType()] = System.currentTimeMillis();
            if (firstTimeSend == 0) {
                firstTimeSend = System.currentTimeMillis();
            }
        }
    }

    private void doFallSample(SensorEvent event) {
        float aX = event.values[1];
        float aY = event.values[0];
        float aZ = event.values[2];
        float accVecSum = FloatMath.sqrt(aX * aX + aY * aY + aZ * aZ);

        if (fallDetector.fallDetected(accVecSum)) {
            sendFallMessage(true); // send msg
        }
    }

    public long getSampleDelay() {
        return sampleDelay;
    }

    /**
     * @return Time stamp of the oldest sample, or -1 if not all sensors have sampled yet.
     */
    private long getOldestSampleTime() {

        int count = 0;
        long oldestSample = Long.MAX_VALUE;
        for (long time : lastSampleTimes) {
            if (time != 0) {
                count++;
                if (time < oldestSample) {
                    oldestSample = time;
                }
            }
        }

        if (count < sensors.size()) {
            return -1;
        } else {
            return oldestSample;
        }
    }

    /**
     * @return true if it is too long since the sensor was registered.
     */
    private boolean isTimeToRegister() {
        return motionSensingActive
                && !isRegistered
                && System.currentTimeMillis() - getOldestSampleTime() + DELAY_AFTER_REGISTRATION > sampleDelay;
    }

    /**
     * @return true if all active sensors have recently passed a data point.
     */
    private boolean isTimeToUnregister() {

        boolean unregister = isUnregisterWhenIdle;

        // only unregister if all sensors have submitted a new sample
        long oldestSample = getOldestSampleTime();
        if (oldestSample == -1) {
            unregister = false;
        } else {
            unregister = unregister && (lastRegistered < oldestSample);
        }

        // only unregister when sample delay is large enough
        unregister = unregister && sampleDelay > DELAY_AFTER_REGISTRATION;

        // only unregister when fall detection is not active
        unregister = unregister && !isFallDetectMode;

        // only unregister when energy sample has finished
        unregister = unregister
                && (!isEnergyMode || (energySampleStart != 0 && System.currentTimeMillis()
                        - energySampleStart > ENERGY_SAMPLE_LENGTH));

        return unregister;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (!motionSensingActive) {
            Log.w(TAG, "Motion sensor value received when sensor is inactive! (Re)try stopping...");
            stopMotionSensing();
            return;
        }

        final Sensor sensor = event.sensor;

        // pass sensor value to fall detector first
        if (isFallDetectMode && sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            doFallSample(event);
        }

        // if motion energy sensor is active, determine energy of every sample
        boolean isEnergySample = !hasLinAccSensor && Sensor.TYPE_ACCELEROMETER == sensor.getType()
                || hasLinAccSensor && Sensor.TYPE_LINEAR_ACCELERATION == sensor.getType();
        if (isEnergyMode && isEnergySample) {
            doEnergySample(event);
        }

        // check sensor delay
        if (System.currentTimeMillis() > lastSampleTimes[sensor.getType()] + sampleDelay) {
            lastSampleTimes[sensor.getType()] = System.currentTimeMillis();
        } else {
            // new sample is too soon

            // unregister when sensor listener when we can
            if (isTimeToUnregister()) {

                // unregister the listener and start again in sampleDelay seconds
                unregisterSensors();
                motionHandler.postDelayed(motionThread = new Runnable() {

                    @Override
                    public void run() {
                        registerSensors();
                    }
                }, sampleDelay - DELAY_AFTER_REGISTRATION);
            }
            return;
        }

        // Epi-mode is only interested in the accelerometer
        if (isEpiMode && sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }

        // determine sensor name
        String sensorName = "";
        switch (sensor.getType()) {
        case Sensor.TYPE_ACCELEROMETER:
            sensorName = SensorNames.ACCELEROMETER;
            break;
        case Sensor.TYPE_ORIENTATION:
            sensorName = SensorNames.ORIENT;
            break;
        case Sensor.TYPE_MAGNETIC_FIELD:
            sensorName = SensorNames.MAGNET_FIELD;
            break;
        case Sensor.TYPE_GYROSCOPE:
            sensorName = SensorNames.GYRO;
            break;
        case Sensor.TYPE_LINEAR_ACCELERATION:
            sensorName = SensorNames.LIN_ACCELERATION;
            break;
        default:
            Log.w(TAG, "Unexpected sensor type: " + sensor.getType());
            return;
        }

        // prepare JSON object to send to MsgHandler
        final JSONObject json = createJsonValue(event);
        if (null == json) {
            // error occurred creating the JSON object
            return;
        }

        // add the data to the buffer if we are in Epi-mode:
        if (isEpiMode) {
            doEpiSample(sensor, json);
        } else {
            sendNormalMessage(sensor, sensorName, json);
        }

        // send motion energy message
        if (isEnergyMode && isEnergySample) {
            sendEnergyMessage();
        }
    }

    /**
     * Registers for updates from the device's motion sensors.
     */
    private synchronized void registerSensors() {

        if (!isRegistered) {
            // Log.v(TAG, "Register the motion sensor for updates");

            SensorManager mgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

            int delay = isFallDetectMode || isEpiMode || isEnergyMode ? SensorManager.SENSOR_DELAY_GAME
                    : SensorManager.SENSOR_DELAY_NORMAL;

            for (Sensor sensor : sensors) {
                mgr.registerListener(this, sensor, delay);
            }

            isRegistered = true;

        } else {
            // Log.v(TAG, "Did not register for motion sensor updates: already registered");
        }
    }

    /**
     * Sends message with average motion energy to the MsgHandler.
     */
    private void sendEnergyMessage() {

        if (avgSpeedCount > 1) {
            // Log.v(TAG, NAME_MOTION_ENERGY + " value. Count: " + avgSpeedCount);

            // round to three decimals
            float value = BigDecimal.valueOf(avgSpeedChange).setScale(3, 0).floatValue();

            // prepare intent to send to MsgHandler
            Intent i = new Intent(context.getString(R.string.action_sense_new_data));
            i.putExtra(DataPoint.SENSOR_NAME, SensorNames.MOTION_ENERGY);
            i.putExtra(DataPoint.SENSOR_DESCRIPTION, SensorNames.MOTION_ENERGY);
            i.putExtra(DataPoint.VALUE, value);
            i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.FLOAT);
            i.putExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime());
            context.startService(i);

        }
        avgSpeedChange = 0;
        avgSpeedCount = 0;
    }

    private void sendFallMessage(boolean fall) {
        Intent i = new Intent(context.getString(R.string.action_sense_new_data));
        i.putExtra(DataPoint.SENSOR_NAME, SensorNames.FALL_DETECTOR);
        i.putExtra(DataPoint.SENSOR_DESCRIPTION, fallDetector.demo ? "demo fall" : "human fall");
        i.putExtra(DataPoint.VALUE, fall);
        i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.BOOL);
        i.putExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime());
        context.startService(i);
    }

    private void sendNormalMessage(Sensor sensor, String sensorName, JSONObject json) {
        Intent i = new Intent(context.getString(R.string.action_sense_new_data));
        i.putExtra(DataPoint.SENSOR_NAME, sensorName);
        i.putExtra(DataPoint.SENSOR_DESCRIPTION, sensor.getName());
        i.putExtra(DataPoint.VALUE, json.toString());
        i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
        i.putExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime());
        context.startService(i);
    }

    public void setSampleDelay(long sampleDelay) {
        this.sampleDelay = sampleDelay;
    }

    public void startMotionSensing(long sampleDelay) {

        final SharedPreferences mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
                Context.MODE_PRIVATE);
        isEpiMode = mainPrefs.getBoolean(Motion.EPIMODE, false);
        isEnergyMode = mainPrefs.getBoolean(Motion.MOTION_ENERGY, false);
        isUnregisterWhenIdle = mainPrefs.getBoolean(Motion.UNREG, true);

        if (isEpiMode) {
            sampleDelay = 0;

            Log.v(TAG, "Start epi state sensor");
            context.startService(new Intent(context, EpiStateMonitor.class));
        }

        // check if the fall detector is enabled
        isFallDetectMode = mainPrefs.getBoolean(Motion.FALL_DETECT, false);
        if (fallDetector.demo = mainPrefs.getBoolean(Motion.FALL_DETECT_DEMO, false)) {
            isFallDetectMode = true;

            Log.v(TAG, "Start epi state sensor");
            context.startService(new Intent(context, EpiStateMonitor.class));
        }

        if (firstStart && isFallDetectMode) {
            sendFallMessage(false);
            firstStart = false;
        }

        SensorManager mgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensors = new ArrayList<Sensor>();

        // add accelerometer
        if (null != mgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)) {
            sensors.add(mgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        }

        if (!isEpiMode) {
            // add orientation sensor
            if (null != mgr.getDefaultSensor(Sensor.TYPE_ORIENTATION)) {
                sensors.add(mgr.getDefaultSensor(Sensor.TYPE_ORIENTATION));
            }
            // add gyroscope
            if (null != mgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE)) {
                sensors.add(mgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
            }
            // add linear acceleration
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                // only devices with gingerbread+ have linear acceleration sensors
                if (null != mgr.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)) {
                    sensors.add(mgr.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION));
                    hasLinAccSensor = true;
                }
            }
        }

        motionSensingActive = true;
        setSampleDelay(sampleDelay);
        registerSensors();
        startWakeUpAlarms();
        enableScreenOffListener(true);
    }

    private void enableScreenOffListener(boolean enable) {
        if (enable) {
            // Register the receiver for SCREEN OFF events
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            context.registerReceiver(screenOffListener, filter);
        } else {
            // Unregister the receiver for SCREEN OFF events
            try {
                context.unregisterReceiver(screenOffListener);
            } catch (IllegalArgumentException e) {
                // Log.v(TAG, "Ignoring exception when unregistering screen off listener");
            }
        }
    }

    /**
     * Sets a periodic alarm that makes sure the the device is awake for a short while for every
     * sample.
     */
    private void startWakeUpAlarms() {

        // register receiver for wake up alarm
        wakeReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                // Log.v(TAG, "Wake up! " + new SimpleDateFormat("k:mm:ss.SSS").format(new Date()));

                if (null == wakeLock) {
                    PowerManager powerMgr = (PowerManager) context
                            .getSystemService(Context.POWER_SERVICE);
                    wakeLock = powerMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
                }
                if (!wakeLock.isHeld()) {
                    Log.i(TAG, "Acquire wake lock for 500ms");
                    wakeLock.acquire(500);
                } else {
                    // Log.v(TAG, "Wake lock already held");
                }

                if (isTimeToRegister()) {
                    // Log.v(TAG, "Time to register!");
                    registerSensors();
                }
            }
        };
        context.registerReceiver(wakeReceiver, new IntentFilter(ACTION_WAKEUP_ALARM));

        // schedule alarm to go off and wake up the receiver
        Intent wakeUp = new Intent(ACTION_WAKEUP_ALARM);
        PendingIntent operation = PendingIntent.getBroadcast(context, ALARM_ID, wakeUp, 0);
        final AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.cancel(operation);
        mgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, sampleDelay, operation);
    }

    /**
     * Unregisters the listener for updates from the motion sensors, and stops waking up the device
     * for sampling.
     */
    public void stopMotionSensing() {
        // Log.v(TAG, "Stop motion sensor");

        try {
            motionSensingActive = false;
            unregisterSensors();

            if (motionThread != null) {
                motionHandler.removeCallbacks(motionThread);
                motionThread = null;
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        if (isEpiMode || isFallDetectMode) {
            Log.v(TAG, "Stop epi state sensor");
            context.stopService(new Intent(context, EpiStateMonitor.class));
        }

        enableScreenOffListener(false);
        stopWakeUpAlarms();
    }

    /**
     * Stops the periodic alarm to wake up the device and take a sample.
     */
    private void stopWakeUpAlarms() {

        // unregister wake up receiver
        try {
            context.unregisterReceiver(wakeReceiver);
        } catch (IllegalArgumentException e) {
            // do nothing
        }

        // cancel the wake up alarm
        Intent wakeUp = new Intent(ACTION_WAKEUP_ALARM);
        PendingIntent operation = PendingIntent.getBroadcast(context, ALARM_ID, wakeUp, 0);
        final AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.cancel(operation);
    }

    private synchronized void unregisterSensors() {

        if (isRegistered) {
            // Log.v(TAG, "Unregister the motion sensor for updates");
            ((SensorManager) context.getSystemService(Context.SENSOR_SERVICE))
                    .unregisterListener(this);
            lastRegistered = System.currentTimeMillis();
        } else {
            // Log.v(TAG, "Did not unregister for motion sensor updates: already unregistered");
        }

        isRegistered = false;
    }
}
