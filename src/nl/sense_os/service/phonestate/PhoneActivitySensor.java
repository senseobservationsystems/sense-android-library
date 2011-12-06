/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.phonestate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;

import org.json.JSONException;
import org.json.JSONObject;

public class PhoneActivitySensor {
    private static final String TAG = "Sense Phone activity";
    private long sampleDelay = 0; // in milliseconds
    private long lastSampleTime;
    private Context context;

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

                    Intent i = new Intent(context.getString(R.string.action_sense_new_data));
                    i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
                    i.putExtra(DataPoint.VALUE, json.toString());
                    i.putExtra(DataPoint.SENSOR_NAME, SensorNames.SCREEN_ACTIVITY);
                    i.putExtra(DataPoint.TIMESTAMP, System.currentTimeMillis());
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