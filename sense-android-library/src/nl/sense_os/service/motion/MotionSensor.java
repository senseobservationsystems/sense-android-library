/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.motion;

import java.util.ArrayList;
import java.util.List;

import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main.Motion;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.PeriodicPollAlarmReceiver;
import nl.sense_os.service.shared.PeriodicPollingSensor;
import nl.sense_os.service.shared.SensorDataPoint;
import nl.sense_os.service.states.EpiStateMonitor;
import nl.sense_os.service.subscription.BaseSensor;
import nl.sense_os.service.subscription.SubscriptionManager;
import android.annotation.TargetApi;
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
import android.util.Log;

/**
 * <p>
 * Represents the main motion sensor. Listens for events from the Android SensorManager and parses
 * the results.
 * </p>
 * <p>
 * The resulting data is divided over several separate sensors in CommonSense:
 * <ul>
 * <li>accelerometer</li>
 * <li>gyroscope</li>
 * <li>motion energy</li>
 * <li>linear acceleration</li>
 * </ul>
 * Besides these basic sensors, the sensor can also gather data for high-speed epilepsy detection
 * and fall detection.
 * </p>
 * 
 * @author Ted Schmidt <ted@sense-os.nl>
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class MotionSensor extends BaseSensor implements SensorEventListener, PeriodicPollingSensor {

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
                        startSensing(getSampleRate());
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
    private final Context context;
    private boolean isFallDetectMode;
    private boolean isEnergyMode;
    private boolean isEpiMode;
    private boolean isBurstMode;
    private boolean isUnregisterWhenIdle;
    private boolean firstStart = true;
    private List<Sensor> sensors;
    private boolean active = false;
    private WakeLock wakeLock;
    private boolean registered;
    private PeriodicPollAlarmReceiver alarmReceiver;
    // TODO:
    // Should be moved to the service where all the sensors are registered
    // and added as data processor when the preference is selected
    // or at least out of startSensing, does not need to create this coupling at startSensing
    private EpilepsySensor epi;
    private FallDetector fall;
    private MotionEnergySensor energy;
    private StandardMotionSensor standard = null;
    private MotionBurstSensor accelerometerBurstSensor;
    private MotionBurstSensor gyroBurstSensor;
    private MotionBurstSensor linearBurstSensor;
    private SubscriptionManager mSubscrMgr;

    /**
     * Constructor.
     * 
     * @param context
     * @see #getInstance(Context)
     */
    protected MotionSensor(Context context) {
        this.context = context;
        alarmReceiver = new PeriodicPollAlarmReceiver(this);
        mSubscrMgr = SubscriptionManager.getInstance();
    }

    @Override
    public void doSample() {
        //Log.v(TAG, "Do sample");

        // get wake lock
        if (null == wakeLock) {
            PowerManager powerMgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakeLock = powerMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        }
        if (!wakeLock.isHeld()) {
            //Log.i(TAG, "Acquire wake lock");
            wakeLock.acquire();
        } else {
            // Log.v(TAG, "Wake lock already held");
        }

        // notify all special sensors
        notifySubscribers();

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

    /**
     * Initializes the burst-mode data processors.
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void initBurstDataProcessors() {
        final SharedPreferences mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
                Context.MODE_PRIVATE);

        if (mainPrefs.getBoolean(Motion.ACCELEROMETER, true)) {
            accelerometerBurstSensor = new MotionBurstSensor(context, Sensor.TYPE_ACCELEROMETER,
                    SensorNames.ACCELEROMETER_BURST);
            addSubscriber(this.accelerometerBurstSensor);
            mSubscrMgr.registerProducer(SensorNames.ACCELEROMETER_BURST,
                    accelerometerBurstSensor);
        }
        if (mainPrefs.getBoolean(Motion.GYROSCOPE, true)) {
            gyroBurstSensor = new MotionBurstSensor(context, Sensor.TYPE_GYROSCOPE,
                    SensorNames.GYRO_BURST);
            addSubscriber(this.gyroBurstSensor);
            mSubscrMgr.registerProducer(SensorNames.GYRO_BURST, gyroBurstSensor);
        }
        if (mainPrefs.getBoolean(Motion.LINEAR_ACCELERATION, true)) {
            if (!MotionSensorUtils.isFakeLinearRequired( context )) {
                linearBurstSensor = new MotionBurstSensor(context,
                        Sensor.TYPE_LINEAR_ACCELERATION,
                        SensorNames.LINEAR_BURST);
                addSubscriber(this.linearBurstSensor);
                mSubscrMgr.registerProducer(SensorNames.LINEAR_BURST,
                        linearBurstSensor);
            }else{
                  linearBurstSensor = new MotionBurstSensor(context,
                  Sensor.TYPE_ACCELEROMETER, SensorNames.LINEAR_BURST);
                  addSubscriber(this.linearBurstSensor);
                  mSubscrMgr.registerProducer(SensorNames.LINEAR_BURST,
                        linearBurstSensor);
                  Log.d( TAG, "Fake linearBurst is registered" );
            }
        }
    }

    /**
     * Initializes the epilepsy data processor.
     */
    private void initEpiDataProcessor() {

        // only listen to accelerometer in epi mode
        sensors = new ArrayList<Sensor>();

        SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensors.add(sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));

        // super fast sample delay
        setSampleRate(0);

        // separate service to check epilepsy (not used anymore?)
        context.startService(new Intent(context, EpiStateMonitor.class));

        // only add epilepsy data processor
        this.epi = new EpilepsySensor(context);
        // register the sensor at the sense Service
        // TODO: connect to the service in a different manner
        mSubscrMgr.registerProducer(SensorNames.ACCELEROMETER_EPI, epi);
        addSubscriber(this.epi);
    }

    /**
     * Initializes the fall detection data processor.
     * 
     * @param demo
     *            boolean to enable the fall detection demo mode
     */
    private void initFallDataProcessor(boolean demo) {

        // add fall detector data processor
        fall = new FallDetector(context);
        fall.demo = demo;

        // Example how to subscribe via the service
        // Context should not be used for this
        mSubscrMgr.subscribeConsumer(SensorNames.MOTION, fall);
        mSubscrMgr.registerProducer(SensorNames.FALL_DETECTOR, fall);

        fall.sendFallMessage(false);
        firstStart = false;
    }

    /**
     * Initializes the standard data processors that create data for the acceleration, orientation,
     * magnetic field rotation rate, linear acceleration and motion energy sensors.
     */
    private void initStandardDataProcessors() {
        standard = new StandardMotionSensor(context);
        mSubscrMgr.registerProducer(SensorNames.ACCELEROMETER, standard);
        mSubscrMgr.registerProducer(SensorNames.ORIENT, standard);
        mSubscrMgr.registerProducer(SensorNames.MAGNETIC_FIELD, standard);
        mSubscrMgr.registerProducer(SensorNames.GYRO, standard);
        mSubscrMgr.registerProducer(SensorNames.LIN_ACCELERATION, standard);
        addSubscriber(this.standard);

        // optional motion energy data processor
        if (isEnergyMode) {
            energy = new MotionEnergySensor(context);
            mSubscrMgr.registerProducer(SensorNames.MOTION_ENERGY, energy);
            addSubscriber(this.energy);
        }
    }

    @Override
    public boolean isActive() {
        return active;
    }

    /**
     * @return true if all active sensors have recently passed a data point.
     */
    private boolean isTimeToUnregister() {

        boolean unregister = isUnregisterWhenIdle;
        // only unregister when sample delay is large enough
        unregister &= getSampleRate() > DELAY_AFTER_REGISTRATION;
        unregister &= this.checkSubscribers();
        return unregister;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (!registered) {
            // ignore
        }
        if (!active) {
            Log.w(TAG, "Motion sensor value received when sensor is inactive!");
            stopSample();
            return;
        }

        SensorDataPoint dataPoint = new SensorDataPoint(event);
        dataPoint.sensorName = SensorNames.MOTION;
        dataPoint.sensorDescription = SensorNames.MOTION;
        dataPoint.timeStamp = SNTP.getInstance().getTime();
        this.sendToSubscribers(dataPoint);

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

        if (!registered) {
            // Log.v(TAG, "Register the motion sensor for updates");

            SensorManager mgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

            int delay = isFallDetectMode || isEpiMode || isBurstMode || isEnergyMode ? SensorManager.SENSOR_DELAY_GAME
                    : SensorManager.SENSOR_DELAY_NORMAL;

            for (Sensor sensor : sensors) {
                mgr.registerListener(this, sensor, delay);
            }

            registered = true;

        } else {
            // Log.v(TAG, "Did not register for motion sensor updates: already registered");
        }
    }

    @Override
    public void setSampleRate(long sampleDelay) {
        super.setSampleRate(sampleDelay);
        stopPolling();
        startPolling();
    }

    private void startPolling() {
        //Log.v(TAG, "start polling");
        alarmReceiver.start(context);
    }

    @Override
    public void startSensing(long sampleDelay) {
        //Log.v(TAG, "Start sensing");

        final SharedPreferences mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
                Context.MODE_PRIVATE);
        isEpiMode = mainPrefs.getBoolean(Motion.EPIMODE, false);
        isEnergyMode = mainPrefs.getBoolean(Motion.MOTION_ENERGY, false);
        isBurstMode = mainPrefs.getBoolean(Motion.BURSTMODE, false);
        isUnregisterWhenIdle = mainPrefs.getBoolean(Motion.UNREG, true);

        // check if the fall detector is enabled
        boolean isFallDetectDemo = mainPrefs.getBoolean(Motion.FALL_DETECT_DEMO, false);
        isFallDetectMode = isFallDetectDemo || mainPrefs.getBoolean(Motion.FALL_DETECT, false);

        sensors = MotionSensorUtils.getAvailableMotionSensors(context);
        if (isEpiMode) {
            initEpiDataProcessor();

        } else {
            // add standard data processor
            initStandardDataProcessors();

            // add fall detection data processor
            if (firstStart && isFallDetectMode) {
                initFallDataProcessor(isFallDetectDemo);
            }

            // add burst mode
            if (isBurstMode) {                
                initBurstDataProcessors();
            }
        }

        active = true;
        setSampleRate(sampleDelay);

        // do the first sample immediately
        doSample();
    }

    public void stopPolling() {
        //Log.v(TAG, "stop polling");
        alarmReceiver.stop(context);
    }

    private void stopSample() {
        //Log.v(TAG, "Stop sample");

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
        //Log.v(TAG, "Stop sensing");
        stopSample();
        stopPolling();
        enableScreenOffListener(false);
        active = false;

        // only remove the DataProcessors added in the class, keep external DataProcessors
        if (this.epi != null)
            removeSubscriber(epi);
        if (this.standard != null)
            removeSubscriber(standard);
        if (this.fall != null)
            removeSubscriber(fall);
        if (this.energy != null)
            removeSubscriber(energy);
        if (this.accelerometerBurstSensor != null)
            removeSubscriber(accelerometerBurstSensor);
        if (this.linearBurstSensor != null)
            removeSubscriber(linearBurstSensor);
        if (this.gyroBurstSensor != null)
            removeSubscriber(gyroBurstSensor);

        epi = null;
        standard = null;
        fall = null;
        energy = null;
        accelerometerBurstSensor = null;
        linearBurstSensor = null;
        gyroBurstSensor = null;

        // stop the epi state monitor service
        // TODO: remove the epi mode and epi state monitor service
        if (isEpiMode || isFallDetectMode || isBurstMode) {
            context.stopService(new Intent(context, EpiStateMonitor.class));
        }
    }

    private synchronized void unregisterSensors() {

        if (registered) {
            // Log.v(TAG, "Unregister the motion sensor for updates");
            ((SensorManager) context.getSystemService(Context.SENSOR_SERVICE))
                    .unregisterListener(this);
        } else {
            // Log.v(TAG, "Did not unregister for motion sensor updates: already unregistered");
        }

        registered = false;
    }
}
