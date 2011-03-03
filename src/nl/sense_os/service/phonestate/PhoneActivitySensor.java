/*
 ************************************************************************************************************
 *     Copyright (C)  2010 Sense Observation Systems, Rotterdam, the Netherlands.  All rights reserved.     *
 ************************************************************************************************************
 */
package nl.sense_os.service.phonestate;

import nl.sense_os.service.Constants;
import nl.sense_os.service.MsgHandler;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class PhoneActivitySensor {
    private static final String TAG = "Sense Phone activity";
    private long sampleDelay = 0; // in milliseconds
    private long lastSampleTime;
    private Context context;
    private static final String PHONE_ACTIVITY = "screen activity";

    public PhoneActivitySensor(Context context) {
        this.context = context;
    }

    private BroadcastReceiver screenActivityReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (System.currentTimeMillis() > lastSampleTime + sampleDelay) {
                // Send a message when the screen state has changed
                String screen = "";
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
                    screen = "off";
                else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
                    screen = "on";
                // check if the intent was a screen change intent
                if (screen.length() > 0) {
                    JSONObject json = new JSONObject();
                    try {
                        json.put("screen", screen);

                    } catch (JSONException e) {
                        Log.e(TAG, "JSONException preparing screen activity data");
                    }

                    Intent i = new Intent(MsgHandler.ACTION_NEW_MSG);
                    i.putExtra(MsgHandler.KEY_DATA_TYPE, Constants.SENSOR_DATA_TYPE_JSON);
                    i.putExtra(MsgHandler.KEY_VALUE, json.toString());
                    i.putExtra(MsgHandler.KEY_SENSOR_NAME, PHONE_ACTIVITY);
                    i.putExtra(MsgHandler.KEY_TIMESTAMP, System.currentTimeMillis());
                    PhoneActivitySensor.this.context.startService(i);
                    lastSampleTime = System.currentTimeMillis();
                }
                // check if the intent was a activity change intent
            }
        }
    };

    public void setSampleDelay(long _sampleDelay) {
        sampleDelay = _sampleDelay;
    }

    public void startPhoneActivitySensing(long _sampleDelay) {
        lastSampleTime = 0;
        setSampleDelay(_sampleDelay);
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        context.registerReceiver(screenActivityReceiver, filter);
    }

    public void stopPhoneActivitySensing() {
        try {
            context.unregisterReceiver(screenActivityReceiver);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }

    public long getSampleDelay() {
        return sampleDelay;
    }
}