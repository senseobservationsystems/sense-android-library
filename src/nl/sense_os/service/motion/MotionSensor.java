/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.motion;

import java.util.ArrayList;
import java.util.List;

import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main.Motion;
import nl.sense_os.service.shared.PeriodicPollAlarmReceiver;
import nl.sense_os.service.shared.PeriodicPollingSensor;
import nl.sense_os.service.states.EpiStateMonitor;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

/**
 * Represents the main motion sensor. Listens for events from the Android SensorManager and parses
 * the results.<br/>
 * <br/>
 * The resulting data is divided over several separate sensors in CommonSense: *
 * <ul>
 * <li>accelerometer</li>
 * <li>gyroscope</li>
 * <li>motion energy</li>
 * <li>linear acceleration</li>
 * </ul>
 * Besides these basic sensors, the sensor can also gather data for high-speed epilepsy detection
 * and fall detection.
 * 
 * @author Ted Schmidt <ted@sense-os.nl>
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class MotionSensor implements SensorEventListener, PeriodicPollingSensor {

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
                        stopSensing();
                        startSensing(sampleDelay);
                    };
                };

                new Handler().postDelayed(restartSensing, 500);
            }
        }
    }

    private static final long DELAY_AFTER_REGISTRATION = 500;
    private static final String TAG = "Sense Motion";

    private static MotionSensor instance = null;

    /**
     * Factory method to get the singleton instance.
     * 
     * @param context
     * @return instance
     */
    public static MotionSensor getInstance(Context context) {
        if (instance == null) {
            instance = new MotionSensor(context);
        }
        return instance;
    }

    private final BroadcastReceiver screenOffListener = new ScreenOffListener();

    private final FallDetector fallDetector;
    private EpilepsySensor epiSensor;
    private MotionEnergySensor energySensor;
    private StandardMotionSensor standardSensor;
    private final Context context;
    private boolean isFallDetectMode;
    private boolean isEnergyMode;
    private boolean isEpiMode;
    private boolean isUnregisterWhenIdle;
    private boolean firstStart = true;
    private List<Sensor> sensors;
    private boolean motionSensingActive = false;
    private long sampleDelay = 0; // in milliseconds
    private WakeLock wakeLock;
    private boolean isRegistered;
    private PeriodicPollAlarmReceiver alarmReceiver;

    protected MotionSensor(Context context) {
        this.context = context;
        epiSensor = new EpilepsySensor(context);
        energySensor = new MotionEnergySensor(context);
        fallDetector = new FallDetector(context);
        standardSensor = new StandardMotionSensor(context);
        alarmReceiver = new PeriodicPollAlarmReceiver(this);
    }

    @Override
    public void doSample() {

        // get wake lock
        if (null == wakeLock) {
            PowerManager powerMgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakeLock = powerMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        }
        if (!wakeLock.isHeld()) {
            Log.i(TAG, "Acquire wake lock for 500ms");
            wakeLock.acquire(500);
        } else {
            // Log.v(TAG, "Wake lock already held");
        }

        // notify all special sensors
        epiSensor.startNewSample();
        standardSensor.startNewSample();
        energySensor.startNewSample();
        fallDetector.startNewSample();

        registerSensors();
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

    @Override
    public long getSampleRate() {
        return sampleDelay;
    }

    @Override
    public boolean isActive() {
        return motionSensingActive;
    }

    /**
     * @return true if all active sensors have recently passed a data point.
     */
    private boolean isTimeToUnregister() {

        boolean unregister = isUnregisterWhenIdle;

        // only unregister when sample delay is large enough
        unregister &= sampleDelay > DELAY_AFTER_REGISTRATION;

        if (isEpiMode) {
            unregister &= epiSensor.isSampleComplete();
        } else {
            unregister &= standardSensor.isSampleComplete();
        }

        // check if fall detector is complete
        unregister &= isFallDetectMode ? fallDetector.isSampleComplete() : true;

        // check if motion energy sensor is complete
        unregister &= isEnergyMode ? energySensor.isSampleComplete() : true;

        return unregister;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (!motionSensingActive) {
            Log.w(TAG, "Motion sensor value received when sensor is inactive!");
            return;
        }

        // pass sensor value to fall detector first
        if (isFallDetectMode) {
            fallDetector.onNewData(event);
        }

        // if motion energy sensor is active, determine energy of every sample
        if (isEnergyMode) {
            energySensor.onNewData(event);
        }

        if (isEpiMode) {
            // add the data to the buffer if we are in Epi-mode
            epiSensor.onNewData(event);
        } else {
            // use standard motion sensor
            standardSensor.onNewData(event);
        }

        // unregister sensor listener when we can
        if (isTimeToUnregister()) {

            // unregister the listener and start again in sampleDelay seconds
            stopSample();
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

    @Override
    public void setSampleRate(long sampleDelay) {
        stopPolling();
        this.sampleDelay = sampleDelay;
        startPolling();
    }

    private void startPolling() {
        // Log.v(TAG, "start polling");
        alarmReceiver.start(context);
    }

    @Override
    public void startSensing(long sampleDelay) {
        // Log.v(TAG, "start sensing");

        final SharedPreferences mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
                Context.MODE_PRIVATE);
        isEpiMode = mainPrefs.getBoolean(Motion.EPIMODE, false);
        isEnergyMode = mainPrefs.getBoolean(Motion.MOTION_ENERGY, false);
        isUnregisterWhenIdle = mainPrefs.getBoolean(Motion.UNREG, true);

        if (isEpiMode) {
            sampleDelay = 0;

            context.startService(new Intent(context, EpiStateMonitor.class));
        }

        // check if the fall detector is enabled
        isFallDetectMode = mainPrefs.getBoolean(Motion.FALL_DETECT, false);
        if (fallDetector.demo == mainPrefs.getBoolean(Motion.FALL_DETECT_DEMO, false)) {
            isFallDetectMode = true;

            context.startService(new Intent(context, EpiStateMonitor.class));
        }

        if (firstStart && isFallDetectMode) {
            fallDetector.sendFallMessage(false);
            firstStart = false;
        }

        sensors = MotionSensorUtils.getAvailableMotionSensors(context);
        if (isEpiMode) {
            // only listen to accelerometer in epi mode
            sensors = new ArrayList<Sensor>();
            SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            sensors.add(sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        }

        motionSensingActive = true;
        setSampleRate(sampleDelay);
    }

    private void stopPolling() {
        // Log.v(TAG, "stop polling");
        alarmReceiver.stop(context);
    }

    private void stopSample() {
        // Log.v(TAG, "stop sample");

        // release wake lock
        if (null != wakeLock && wakeLock.isHeld()) {
            wakeLock.release();
        }

        unregisterSensors();
    }

    /**
     * Unregisters the listener for updates from the motion sensors, and stops waking up the device
     * for sampling.
     */
    @Override
    public void stopSensing() {
        // Log.v(TAG, "stop sensing");
        stopSample();
        stopPolling();
        enableScreenOffListener(false);
        motionSensingActive = false;

        if (isEpiMode || isFallDetectMode) {
            context.stopService(new Intent(context, EpiStateMonitor.class));
        }
    }

    private synchronized void unregisterSensors() {

        if (isRegistered) {
            // Log.v(TAG, "Unregister the motion sensor for updates");
            ((SensorManager) context.getSystemService(Context.SENSOR_SERVICE))
                    .unregisterListener(this);
        } else {
            // Log.v(TAG, "Did not unregister for motion sensor updates: already unregistered");
        }

        isRegistered = false;
    }
}
