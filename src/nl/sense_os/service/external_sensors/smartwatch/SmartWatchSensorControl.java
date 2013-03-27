/*
 Copyright (c) 2011, Sony Ericsson Mobile Communications AB

 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.

 * Neither the name of the Sony Ericsson Mobile Communications AB nor the names
 of its contributors may be used to endorse or promote products derived from
 this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package nl.sense_os.service.external_sensors.smartwatch;

import nl.sense_os.platform.TrivialSensorRegistrator;
import nl.sense_os.service.R;
import nl.sense_os.service.commonsense.SensorRegistrator;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.motion.MotionBurstSensor;
import nl.sense_os.service.shared.PeriodicPollAlarmReceiver;
import nl.sense_os.service.shared.PeriodicPollingSensor;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.aef.sensor.Sensor;
import com.sonyericsson.extras.liveware.aef.sensor.Sensor.SensorInterruptMode;
import com.sonyericsson.extras.liveware.aef.sensor.Sensor.SensorRates;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensor;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensorEvent;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensorEventListener;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensorException;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensorManager;

/**
 * The sample sensor control handles the accelerometer sensor on an accessory. This class exists in
 * one instance for every supported host application that we have registered to
 */
class SmartWatchSensorControl extends ControlExtension implements PeriodicPollingSensor {

    public static final int WIDTH = 128;
    public static final int HEIGHT = 128;
    private static final Bitmap.Config BITMAP_CONFIG = Bitmap.Config.RGB_565;
    private static final String TAG = "SmartWatchSensorControl";
    private static final String SENSOR_DESCRIPTION = "smartwatch";

    private final AccessorySensorEventListener mListener = new AccessorySensorEventListener() {

        @Override
        public void onSensorEvent(AccessorySensorEvent sensorEvent) {
            onSensorChanged(sensorEvent);
        }
    };

    private AccessorySensor mSensor = null;
    private MotionBurstSensor burstSensor;
    private long sampleRate;
    private boolean active;
    private boolean registered;
    private WakeLock wakeLock;
    private PeriodicPollAlarmReceiver alarmReceiver;

    /**
     * Create sample sensor control.
     * 
     * @param hostAppPackageName
     *            Package name of host application.
     * @param context
     *            The context.
     */
    SmartWatchSensorControl(final String hostAppPackageName, final Context context) {
        super(context, hostAppPackageName);

        AccessorySensorManager manager = new AccessorySensorManager(context, hostAppPackageName);
        mSensor = manager.getSensor(Sensor.SENSOR_TYPE_ACCELEROMETER);

        burstSensor = new MotionBurstSensor(context, android.hardware.Sensor.TYPE_ACCELEROMETER,
                SensorNames.ACCELEROMETER_BURST);
        alarmReceiver = new PeriodicPollAlarmReceiver(this);
    }

    @Override
    public void doSample() {
        Log.v(TAG, "sample");

        // get wake lock
        if (null == wakeLock) {
            PowerManager powerMgr = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            wakeLock = powerMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        }
        if (!wakeLock.isHeld()) {
            Log.i(TAG, "Acquire wake lock for 500ms");
            wakeLock.acquire(500);
        } else {
            // Log.v(TAG, "Wake lock already held");
        }

        // notify the data handler
        burstSensor.startNewSample();

        registerListener();
    }

    @Override
    public long getSampleRate() {
        return sampleRate;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    /**
     * @return true if all active sensors have recently passed a data point.
     */
    private boolean isTimeToUnregister() {
        return burstSensor.isSampleComplete();
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "destroy");
        stopSensing();
    }

    @Override
    public void onPause() {
        Log.v(TAG, "pause");
        unregisterListener();
    }

    @Override
    public void onResume() {
        Log.d(SmartWatchExtensionService.LOG_TAG, "Starting control");

        updateDisplay();

        startSensing(10 * 1000);
    }

    /**
     * Update the display with new accelerometer data.
     * 
     * @param sensorEvent
     *            The sensor event.
     */
    private void onSensorChanged(AccessorySensorEvent sensorEvent) {
        if (!active) {
            Log.v(TAG, "SmartWatch sensor value received when sensor is inactive!");
            return;
        }

        // unregister sensor listener when we can
        if (isTimeToUnregister()) {

            // unregister the listener and start again in sampleDelay seconds
            stopSample();
        } else {

            // Update the values.
            if (sensorEvent != null) {
                float[] values = sensorEvent.getSensorValues();
                burstSensor.onNewData(android.hardware.Sensor.TYPE_ACCELEROMETER,
                        SENSOR_DESCRIPTION, values);
            }
        }
    }

    private void updateDisplay() {

        // Note: Setting the screen to be always on will drain the accessory
        // battery. It is done here solely for demonstration purposes
        setScreenState(Control.Intents.SCREEN_STATE_DIM);

        // Create bitmap to draw in.
        Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, BITMAP_CONFIG);

        // Set default density to avoid scaling.
        bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);

        LinearLayout root = new LinearLayout(mContext);
        root.setLayoutParams(new LayoutParams(WIDTH, HEIGHT));

        LinearLayout layout = (LinearLayout) LinearLayout.inflate(mContext,
                R.layout.smartwatch_layout, root);
        layout.measure(WIDTH, HEIGHT);
        layout.layout(0, 0, layout.getMeasuredWidth(), layout.getMeasuredHeight());

        Canvas canvas = new Canvas(bitmap);
        layout.draw(canvas);

        showBitmap(bitmap);
    }

    /**
     * Registers for updates from the SmartWatch sensors.
     */
    private synchronized void registerListener() {
        if (!registered) {
            // Start listening for sensor updates.
            if (mSensor != null) {
                Log.v(TAG, "Register the listener for updates from the SmartWatch");
                try {
                    mSensor.registerListener(mListener, SensorRates.SENSOR_DELAY_NORMAL,
                            SensorInterruptMode.SENSOR_INTERRUPT_ENABLED);
                    registered = true;
                } catch (AccessorySensorException e) {
                    Log.d(TAG, "Failed to register listener");
                }
            }
        }
    }

    @Override
    public void setSampleRate(long sampleRate) {
        stopPolling();
        this.sampleRate = sampleRate;
        startPolling();
    }

    private void startPolling() {
        Log.v(TAG, "start polling @" + getSampleRate());
        alarmReceiver.start(mContext);
    }

    @Override
    public void startSensing(long sampleRate) {

        // register the sensor
        new Thread() {

            @Override
            public void run() {
                SensorRegistrator registrator = new TrivialSensorRegistrator(mContext);
                registrator.checkSensor(SensorNames.ACCELEROMETER_BURST,
                        "acceleration (burst-mode)", SenseDataTypes.JSON, SENSOR_DESCRIPTION,
                        "{\"interval\":0,\"data\":[]}", null, null);
            }
        }.start();

        active = true;
        setSampleRate(sampleRate);
    }

    private void stopPolling() {
        Log.v(TAG, "stop polling");
        alarmReceiver.stop(mContext);
    }

    private void stopSample() {
        // Log.v(TAG, "stop sample");

        // release wake lock
        if (null != wakeLock && wakeLock.isHeld()) {
            wakeLock.release();
        }

        unregisterListener();
    }

    @Override
    public void stopSensing() {
        stopSample();
        stopPolling();
        active = false;
    }

    private synchronized void unregisterListener() {
        if (registered) {
            Log.v(TAG, "unregister sensor listener");

            // Stop sensor
            if (mSensor != null) {
                mSensor.unregisterListener();
            }
            registered = false;
        }
   }
}
