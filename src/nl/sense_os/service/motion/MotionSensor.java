/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.motion;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import nl.sense_os.service.SenseService;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main.Motion;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.shared.BaseSensor;
import nl.sense_os.service.shared.DataProcessor;
import nl.sense_os.service.shared.DataProducer;
import nl.sense_os.service.shared.PeriodicPollAlarmReceiver;
import nl.sense_os.service.shared.PeriodicPollingSensor;
import nl.sense_os.service.shared.SensorDataPoint;
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

    private final Context context;
    private boolean isFallDetectMode;
    private boolean isEnergyMode;
    private boolean isEpiMode;
    private boolean isUnregisterWhenIdle;
    private boolean firstStart = true;
    private List<Sensor> sensors;    
    private boolean motionSensingActive = false;    
    private WakeLock wakeLock;
    private boolean isRegistered;
    private PeriodicPollAlarmReceiver alarmReceiver;
    // TODO:
    // Should be moved to the service where all the sensors are registered
    // and added as data processor when the preference is selected
    // or at least out of startSensing, does not need to create this coupling at startSensing 
    private AtomicReference<DataProcessor> epi, fall, energy, standard = null;

    protected MotionSensor(Context context) {
        this.context = context;
        alarmReceiver = new PeriodicPollAlarmReceiver(this);
    }
    
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

       
            unregister &= this.checkSubscribers();
       

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

        sendToSubscribers(new SensorDataPoint(event));
        
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
        super.setSampleRate(sampleDelay);
        stopPolling();
        startPolling();
    }

    private void startPolling() {
        // Log.v(TAG, "start polling");
        alarmReceiver.start(context);
    }

    
    public void startSensing(long sampleDelay) {
        // Log.v(TAG, "start sensing");

        final SharedPreferences mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
                Context.MODE_PRIVATE);
        isEpiMode = mainPrefs.getBoolean(Motion.EPIMODE, false);
        isEnergyMode = mainPrefs.getBoolean(Motion.MOTION_ENERGY, false);
        isUnregisterWhenIdle = mainPrefs.getBoolean(Motion.UNREG, true);

        // check if the fall detector is enabled
        boolean isFallDetectDemo = mainPrefs.getBoolean(Motion.FALL_DETECT_DEMO, false);
        isFallDetectMode = isFallDetectDemo || mainPrefs.getBoolean(Motion.FALL_DETECT, false);

        sensors = MotionSensorUtils.getAvailableMotionSensors(context);
        if (isEpiMode) {
            // only listen to accelerometer in epi mode
            sensors = new ArrayList<Sensor>();
            SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            sensors.add(sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));

            // super fast sample delay
            sampleDelay = 0;

            // separate service to check epilepsy (not used anymore?)
            context.startService(new Intent(context, EpiStateMonitor.class));

            // only add epilepsy data processor
            EpilepsySensor epiSensor = new EpilepsySensor(context);    
            this.epi = new AtomicReference<DataProcessor>(epiSensor);
            // register the sensor at the sense Service
            // TODO: connect to the service in a different manner
            ((SenseService) context).registerDataProducer(SensorNames.ACCELEROMETER_EPI,
                    new AtomicReference<DataProducer>(epiSensor));
            addSubscriber(this.epi);

        } else {
            // add standard data processor
            StandardMotionSensor standard = new StandardMotionSensor(context);
            this.standard = new AtomicReference<DataProcessor>(standard);
            ((SenseService) context).registerDataProducer(SensorNames.ACCELEROMETER,
                    new AtomicReference<DataProducer>(standard));
            ((SenseService) context).registerDataProducer(SensorNames.ORIENT,
                    new AtomicReference<DataProducer>(standard));
            ((SenseService) context).registerDataProducer(SensorNames.MAGNETIC_FIELD,
                    new AtomicReference<DataProducer>(standard));
            ((SenseService) context).registerDataProducer(SensorNames.GYRO,
                    new AtomicReference<DataProducer>(standard));
            ((SenseService) context).registerDataProducer(SensorNames.LIN_ACCELERATION,
                    new AtomicReference<DataProducer>(standard));
            addSubscriber(this.standard);

            // add motion energy data processor
            if (isEnergyMode) {
                MotionEnergySensor energy = new MotionEnergySensor(context);
                this.energy = new AtomicReference<DataProcessor>(energy);
                ((SenseService) context).registerDataProducer(SensorNames.MOTION_ENERGY,
                        new AtomicReference<DataProducer>(energy));
                addSubscriber(this.energy);
            }

            // add fall detection data processor
            if (firstStart && isFallDetectMode) {

                // add fall detector data processor
                FallDetector fallDetector = new FallDetector(context);
                fallDetector.demo = isFallDetectDemo;
                
                // Example how to subscribe via the service
                // Context should not be used for this                
                this.fall =  new AtomicReference<DataProcessor>(fallDetector);
                ((SenseService)context).subscribeToSensor(SensorNames.MOTION, this.fall);
                ((SenseService) context).registerDataProducer(SensorNames.FALL_DETECTOR,
                        new AtomicReference<DataProducer>(fallDetector));

                fallDetector.sendFallMessage(false);
                firstStart = false;
            }
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

        // only remove the DataProcessors added in the class
        // keep external DataProcessors
        if(this.epi != null)        
        	removeSubscriber(epi);
        if(this.standard != null)
        	removeSubscriber(standard);
        if(this.fall != null)
        	removeSubscriber(fall);
        if(this.energy != null)
        	removeSubscriber(energy);
        
    	epi = standard = fall = energy = null;
        
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
