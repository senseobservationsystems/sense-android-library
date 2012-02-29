/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.phonestate;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class PhoneActivitySensor {

    private static final String TAG = "Sense Screen Activity";
    private final Context context;

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

            // create new data point
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
            i.putExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime());
            context.startService(i);
        }
    };

    public PhoneActivitySensor(Context context) {
        this.context = context;
    }

    public void startPhoneActivitySensing(long sampleDelay) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        context.registerReceiver(screenActivityReceiver, filter);
    }

    public void stopPhoneActivitySensing() {
        try {
            context.unregisterReceiver(screenActivityReceiver);
        } catch (IllegalArgumentException e) {
            // probably was not registered
        }
    }
}