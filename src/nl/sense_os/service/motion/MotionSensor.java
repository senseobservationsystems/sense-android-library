package nl.sense_os.service.motion;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

import nl.sense_os.app.SenseSettings;
import nl.sense_os.service.MsgHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class MotionSensor implements SensorEventListener {
    
    private static final String NAME_ACCELR = "accelerometer";
    private static final String NAME_GYRO = "gyroscope";
    private static final String NAME_MAGNET = "magnetic_field";
    private static final String NAME_ORIENT = "orientation";
    private static final String TAG = "Sense MotionSensor";
    private Context context;
    private long[] lastSampleTimes = new long[50];
    private Handler motionHandler = new Handler();
    private boolean motionSensingActive = false;
    private Runnable motionThread = null;
    private long sampleDelay = 0; // in milliseconds    
    private List<Sensor> sensors;
    private SensorManager smgr;
    
    public MotionSensor(Context context) {
        this.context = context;
        smgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensors = smgr.getSensorList(Sensor.TYPE_ALL);
    }
    
    public long getSampleDelay() {
        return sampleDelay;
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Log.d(TAG, "Accuracy changed...");
        // Log.d(TAG, "Sensor: " + sensor.getName() + "(" + sensor.getType() + "), accuracy: " +
        // accuracy);
    }

    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (System.currentTimeMillis() > lastSampleTimes[sensor.getType()] + sampleDelay) {
            lastSampleTimes[sensor.getType()] = System.currentTimeMillis();

            String sensorName = "";
            switch (sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                sensorName = NAME_ACCELR;
                break;
            case Sensor.TYPE_ORIENTATION:
                sensorName = NAME_ORIENT;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                sensorName = NAME_MAGNET;
                break;
            case Sensor.TYPE_GYROSCOPE:
                sensorName = NAME_GYRO;
                break;
            }

            JSONObject json = new JSONObject();
            int axis = 0;
            try {
                for (float value : event.values) {
                    switch (axis) {
                    case 0:
                        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER
                                || sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                            json.put("x-axis", value);
                        } else if (sensor.getType() == Sensor.TYPE_ORIENTATION
                                || sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                            json.put("azimuth", value);
                        }
                        break;
                    case 1:
                        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER
                                || sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                            json.put("y-axis", value);
                        } else if (sensor.getType() == Sensor.TYPE_ORIENTATION
                                || sensor.getType() == Sensor.TYPE_GYROSCOPE)
                            json.put("pitch", value);
                        break;
                    case 2:
                        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER
                                || sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                            json.put("z-axis", value);
                        } else if (sensor.getType() == Sensor.TYPE_ORIENTATION
                                || sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                            json.put("roll", value);
                        }
                        break;
                    }
                    axis++;
                }
            } catch (JSONException e) {
                Log.e(TAG, "JSONException in onSensorChanged", e);
                return;
            }

            // pass message to the MsgHandler
            Intent i = new Intent(this.context, MsgHandler.class);
            i.putExtra(MsgHandler.KEY_INTENT_TYPE, MsgHandler.TYPE_NEW_MSG);
            i.putExtra(MsgHandler.KEY_SENSOR_NAME, sensorName);
            i.putExtra(MsgHandler.KEY_SENSOR_DEVICE, sensor.getName());
            i.putExtra(MsgHandler.KEY_VALUE, json.toString());
            i.putExtra(MsgHandler.KEY_DATA_TYPE, SenseSettings.SENSOR_DATA_TYPE_JSON);
            i.putExtra(MsgHandler.KEY_TIMESTAMP, System.currentTimeMillis());
            this.context.startService(i);
        }
        if (sampleDelay > 500 && motionSensingActive) {
            // unregister the listener and start again in sampleDelay seconds
            stopMotionSensing();
            motionHandler.postDelayed(motionThread = new Runnable() {

                public void run() {
                    startMotionSensing(sampleDelay);
                }
            }, sampleDelay);
        }
    }

    public void setSampleDelay(long _sampleDelay) {
        sampleDelay = _sampleDelay;
    }

    public void startMotionSensing(long _sampleDelay) {
        motionSensingActive = true;
        setSampleDelay(_sampleDelay);
        for (Sensor sensor : sensors) {
            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER
                    || sensor.getType() == Sensor.TYPE_ORIENTATION
                    || sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                // Log.d(TAG, "registering for sensor " + sensor.getName());
                smgr.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    public void stopMotionSensing() {
        try {
            motionSensingActive = false;
            smgr.unregisterListener(this);

            if (motionThread != null)
                motionHandler.removeCallbacks(motionThread);
            motionThread = null;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }
}
