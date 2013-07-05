/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.phonestate;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.BaseSensor;
import nl.sense_os.service.shared.PeriodicPollAlarmReceiver;
import nl.sense_os.service.shared.PeriodicPollingSensor;
import nl.sense_os.service.shared.SensorDataPoint;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

/**
 * Represents the screen activity sensor. Listens for ACTION_SCREEN_ON and ACTION_SCREEN_OFF
 * broadcasts from Android.
 * 
 * @author Ted Schmidt <ted@sense-os.nl>
 */
public class PhoneActivitySensor extends BaseSensor implements PeriodicPollingSensor{

    private static final String TAG = "Sense Screen Activity";
    private final Context context;
    private String last_screen_value;
    private boolean mActive;
    private static final String ACTION_STOP_SAMPLE = PhoneActivitySensor.class.getName() + ".STOP";
    private static final int REQ_CODE = 1;
    
    private static PhoneActivitySensor instance = null;
    private PeriodicPollAlarmReceiver mStartSampleReceiver;
    private BroadcastReceiver mStopSampleReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            handleLatestValue();
            stopSample();
        }
    };
    
    /**
     * Sets the delay between samples. The sensor registers itself for periodic sampling bursts, and
     * unregisters after it received a sample.
     * 
     * @param sampleDelay
     *            Sample delay in milliseconds
     */
    @Override
    public void setSampleRate(long sampleDelay) {
        super.setSampleRate(sampleDelay);
        stopPolling();
        startPolling();
    }

    private void startPolling() {
        Log.v(TAG, "Start polling");
        mStartSampleReceiver.start(context);
    }
    
    private void stopPolling() {
        Log.v(TAG, "Stop polling");
        mStartSampleReceiver.stop(context);
        stopSample();
    }

    private void stopSample() {
        Log.v(TAG, "Stop sample");  
        // unregister broadcast receiver
        try {
            context.unregisterReceiver(mStopSampleReceiver);
        } catch (IllegalArgumentException e) {
            // ignore
        }
    }
    
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
            last_screen_value = screen;           
        }
    };
    
    private void handleLatestValue()    
    {   
    	 // create new data point
        JSONObject json = new JSONObject();
        try {
            json.put("screen", last_screen_value);

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
        context.startService(i);
    
    }

    protected PhoneActivitySensor(Context context) {
        this.context = context;
        mStartSampleReceiver = new PeriodicPollAlarmReceiver(this);
    }
    
    @Override
    public void startSensing(long sampleDelay) {
    	mActive = true;
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);        
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        context.registerReceiver(screenActivityReceiver, filter);
        setSampleRate(sampleDelay);        
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);  
        last_screen_value = pm.isScreenOn()?"on":"off";
        doSample();
    }
    
    @Override
    public void doSample() {
        Log.v(TAG, "Do sample");
      
        // set alarm for stop the sample
        context.registerReceiver(mStopSampleReceiver, new IntentFilter(ACTION_STOP_SAMPLE));
        Intent intent = new Intent(ACTION_STOP_SAMPLE);
        PendingIntent operation = PendingIntent.getBroadcast(context, REQ_CODE, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 500,
                operation);
    }
    
    @Override
    public void stopSensing() {
        try {
        	mActive = false;
            context.unregisterReceiver(screenActivityReceiver);
        } catch (IllegalArgumentException e) {
            // probably was not registered
        }
    }

	@Override
	public boolean isActive() {
		return mActive;
	}
	
}
