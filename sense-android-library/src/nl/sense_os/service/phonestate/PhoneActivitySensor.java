/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.phonestate;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.PeriodicPollAlarmReceiver;
import nl.sense_os.service.shared.PeriodicPollingSensor;
import nl.sense_os.service.shared.SensorDataPoint;
import nl.sense_os.service.subscription.BaseSensor;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

/**
 * Represents the screen activity sensor. Listens for ACTION_SCREEN_ON and ACTION_SCREEN_OFF
 * broadcasts from Android.
 * 
 * @author Ted Schmidt <ted@sense-os.nl>
 */
public class PhoneActivitySensor extends BaseSensor implements PeriodicPollingSensor {

    private static final String TAG = "Sense Screen Activity";
    private final Context context;
        
    private static PhoneActivitySensor instance = null;
    
    /**
     * Factory method to get the singleton instance.
     * 
     * @param context
     * @return instance
     */
    public static PhoneActivitySensor getInstance(Context context) {
	    if(instance == null) {
	       instance = new PhoneActivitySensor(context);
	    }
	    return instance;
    }

    private BroadcastReceiver screenActivityReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            // Send a message when the screen state has changed
            String screen = "";
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                screen = "off";
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                screen = "on";
            } else {
                Log.w(TAG, "Unexpected broadcast action: " + intent.getAction());
                return;
            }
            sendData(screen);
        }
    };
    
    private void sendData(String screen)
    {
    	 // create new data point
        JSONObject json = new JSONObject();
        try {
            json.put("screen", screen);

        } catch (JSONException e) {
            Log.e(TAG, "JSONException preparing screen activity data");
        }

        notifySubscribers();
        SensorDataPoint dataPoint = new SensorDataPoint(json);
        dataPoint.sensorName = SensorNames.SCREEN_ACTIVITY;
        dataPoint.sensorDescription = SensorNames.SCREEN_ACTIVITY;
        dataPoint.timeStamp = SNTP.getInstance().getTime();        
        sendToSubscribers(dataPoint);
        
        Intent i = new Intent(context.getString(R.string.action_sense_new_data));
        i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
        i.putExtra(DataPoint.VALUE, json.toString());
        i.putExtra(DataPoint.SENSOR_NAME, SensorNames.SCREEN_ACTIVITY);
        i.putExtra(DataPoint.TIMESTAMP, dataPoint.timeStamp);
        i.setPackage(context.getPackageName());
        context.startService(i);
    
    }

    private boolean active;
    private PowerManager pm;
    private PeriodicPollAlarmReceiver alarmReceiver;

    /**
     * Constructor.
     * 
     * @param context
     * @see #getInstance(Context)
     */
    protected PhoneActivitySensor(Context context) {
        super();
        this.context = context;
        pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        alarmReceiver = new PeriodicPollAlarmReceiver(this);
    }

    @Override
    public void doSample() {
        boolean isScreenActive;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            isScreenActive = pm.isInteractive();
        } else {
            isScreenActive = pm.isScreenOn();
        }
        sendData(isScreenActive?"on":"off");
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setSampleRate(long sampleDelay) {
        super.setSampleRate(sampleDelay);
        stopPolling();
        startPolling();
    }

    private void startPolling() {
        Log.v(TAG, "start polling");
        alarmReceiver.start(context);
    }

    public void startSensing(long sampleDelay) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);        
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        context.registerReceiver(screenActivityReceiver, filter);

        setSampleRate(sampleDelay);
        // do the first sample immediately
        doSample();
    }

    public void stopPolling() {
        Log.v(TAG, "stop polling");
        alarmReceiver.stop(context);
    }

    public void stopSensing() {
        try {
            context.unregisterReceiver(screenActivityReceiver);
        } catch (IllegalArgumentException e) {
            // probably was not registered
        }
    }
}
