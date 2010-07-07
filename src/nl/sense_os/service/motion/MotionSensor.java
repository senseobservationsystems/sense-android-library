package nl.sense_os.service.motion;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

import nl.sense_os.service.MsgHandler;

public class MotionSensor implements SensorEventListener {
    private static final String TAG = "MotionSensor";
    private MsgHandler msgHandler;
    
    public MotionSensor(MsgHandler handler) {
        this.msgHandler = handler;
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "Accuracy changed...");
        Log.d(TAG, "Sensor: " + sensor.getName() + "(" + sensor.getType() + "), accuracy: " + accuracy);
    }

    public void onSensorChanged(SensorEvent event) {
        Log.d(TAG, "Sensor changed...");
        
        Sensor sensor = event.sensor;
        String values = "";
        for (float value: event.values) {
            values += value + "; ";
        }
        Log.d(TAG, "Sensor: " + sensor.getName() + "(" + sensor.getType() + ")");
        Log.d(TAG, "Event: " + values + ", accuracy: " + event.accuracy);
    }
}
