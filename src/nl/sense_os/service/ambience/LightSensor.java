/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.ambience;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.ctrl.Controller;
//import nl.sense_os.service.energy_controller.EnergyController;
//import nl.sense_os.service.standard_controller.Controller;

import java.util.ArrayList;
import java.util.List;

public class LightSensor implements SensorEventListener {

    private static final String TAG = "Sense Light Sensor";

    private long sampleDelay = 0; // in milliseconds
    private long[] lastSampleTimes = new long[50];
    private Context context;
    private List<Sensor> sensors;
    private SensorManager smgr;
    private Handler LightHandler = new Handler();
    private Runnable LightThread = null;
    private boolean LightSensingActive = false;
    private Controller controller;
    
    private static LightSensor instance = null;
	
    protected LightSensor(Context context) {
    	this.context = context;
    	smgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        controller = Controller.getController(context);
        sensors = new ArrayList<Sensor>();
        if (null != smgr.getDefaultSensor(Sensor.TYPE_LIGHT)) {
            sensors.add(smgr.getDefaultSensor(Sensor.TYPE_LIGHT));
        }

	}
    
    public static LightSensor getInstance(Context context) {
	    if(instance == null) {
	    	Log.w(TAG, "NEW LIGHT");
	        instance = new LightSensor(context);
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
            if (sensor.getType() == Sensor.TYPE_LIGHT) {
                sensorName = SensorNames.LIGHT;
            }

            String jsonString = "{";
            int x = 0;
            for (float value : event.values) {
                if (x == 0) {
                    if (sensor.getType() == Sensor.TYPE_LIGHT) {
                        jsonString += "\"lux\":" + value;
                        controller.checkLightSensor(value);
                    }
                }
                x++;
            }
            jsonString += "}";
            // pass message to the MsgHandler
            Intent i = new Intent(context.getString(R.string.action_sense_new_data));
            i.putExtra(DataPoint.SENSOR_NAME, sensorName);
            i.putExtra(DataPoint.VALUE, jsonString);
            i.putExtra(DataPoint.SENSOR_DESCRIPTION, sensor.getName());
            i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
            i.putExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime());
            this.context.startService(i);
        }
        if (sampleDelay > 500 && LightSensingActive) {
            // unregister the listener and start again in sampleDelay seconds
            stopLightSensing();
            LightHandler.postDelayed(LightThread = new Runnable() {//////////////////////////////
            
                @Override
                public void run() {
                    startLightSensing(sampleDelay);
                }
            }, sampleDelay);
        }
    }

    public void setSampleDelay(long _sampleDelay) {
        sampleDelay = _sampleDelay;
    }

    public void startLightSensing(long _sampleDelay) {
    	LightHandler = new Handler();
        LightSensingActive = true;
        setSampleDelay(_sampleDelay);
        for (Sensor sensor : sensors) {
            if (sensor.getType() == Sensor.TYPE_LIGHT) {
                // Log.d(TAG, "registering for sensor " + sensor.getName());
                smgr.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    public void stopLightSensing() {
    	Log.w(TAG, "DEBUG LIGHT");
        try {
            LightSensingActive = false;
            smgr.unregisterListener(this);

            if (LightThread != null) {
                LightHandler.removeCallbacks(LightThread);
            }
            LightThread = null;
        
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }

    public long getSampleDelay() {
        return sampleDelay;
    }
}
