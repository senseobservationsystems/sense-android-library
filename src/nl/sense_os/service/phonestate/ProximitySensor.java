/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.phonestate;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;

import java.util.ArrayList;
import java.util.List;

public class ProximitySensor implements SensorEventListener {

    private static final String TAG = "Sense Proximity Sensor";

    private long sampleDelay = 0; // in milliseconds
    private long[] lastSampleTimes = new long[50];
    private Context context;
    private List<Sensor> sensors;
    private SensorManager smgr;
    private Handler ProximityHandler = new Handler();
    private Runnable ProximityThread = null;
    private boolean ProximitySensingActive = false;

    protected ProximitySensor(Context context) {
        this.context = context;
        smgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensors = new ArrayList<Sensor>();
        if (null != smgr.getDefaultSensor(Sensor.TYPE_PROXIMITY)) {
            sensors.add(smgr.getDefaultSensor(Sensor.TYPE_PROXIMITY));
        }
    }

    private static ProximitySensor instance = null;
    
    public static ProximitySensor getInstance(Context context) {
	    if(instance == null) {
	       instance = new ProximitySensor(context);
	    }
	    return instance;
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Log.d(TAG, "Accuracy changed...");
        // Log.d(TAG, "Sensor: " + sensor.getName() + "(" + sensor.getType() + "), accuracy: " +
        // accuracy);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (System.currentTimeMillis() > lastSampleTimes[sensor.getType()] + sampleDelay) {
            lastSampleTimes[sensor.getType()] = System.currentTimeMillis();

            String sensorName = "";
            if (sensor.getType() == Sensor.TYPE_PROXIMITY) {
                sensorName = SensorNames.PROXIMITY;
            }

            String jsonString = "{";
            int x = 0;
            for (float value : event.values) {
                if (x == 0) {
                    if (sensor.getType() == Sensor.TYPE_PROXIMITY)
                        jsonString += "\"distance\":" + value;
                }
                x++;
            }
            jsonString += "}";

            // pass message to the MsgHandler
            Intent i = new Intent(context.getString(R.string.action_sense_new_data));
            i.putExtra(DataPoint.SENSOR_NAME, sensorName);
            i.putExtra(DataPoint.SENSOR_DESCRIPTION, sensor.getName());
            i.putExtra(DataPoint.VALUE, jsonString);
            i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
            i.putExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime());
            this.context.startService(i);
        }
        if (sampleDelay > 500 && ProximitySensingActive) {
            // unregister the listener and start again in sampleDelay seconds
            stopProximitySensing();
            ProximityHandler.postDelayed(ProximityThread = new Runnable() {

                @Override
                public void run() {
                    startProximitySensing(sampleDelay);
                }
            }, sampleDelay);
        }
    }

    public void setSampleDelay(long _sampleDelay) {
        sampleDelay = _sampleDelay;
    }

    public void startProximitySensing(long _sampleDelay) {
    	ProximityHandler = new Handler();
        ProximitySensingActive = true;
        setSampleDelay(_sampleDelay);
        for (Sensor sensor : sensors) {
            if (sensor.getType() == Sensor.TYPE_PROXIMITY) {
                // Log.d(TAG, "registering for sensor " + sensor.getName());
                smgr.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    public void stopProximitySensing() {
        try {
            ProximitySensingActive = false;
            smgr.unregisterListener(this);

            if (ProximityThread != null)
                ProximityHandler.removeCallbacks(ProximityThread);
            ProximityThread = null;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }

    public long getSampleDelay() {
        return sampleDelay;
    }
}
