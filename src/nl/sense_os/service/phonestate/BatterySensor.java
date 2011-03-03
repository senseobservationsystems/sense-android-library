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
import android.os.BatteryManager;
import android.util.Log;

public class BatterySensor {
    private static final String TAG = "Sense Battery sensor";
    private long sampleDelay = 0; // in milliseconds
    private long lastSampleTime;
    private Context context;
    private static final String BATTERY_SENSOR = "battery sensor";

    public BatterySensor(Context context) {
        this.context = context;
    }

    private BroadcastReceiver batteryChangeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean gotData = false;
            if (System.currentTimeMillis() > lastSampleTime + sampleDelay) {
                // Send a message when the screen state has changed
                JSONObject json = new JSONObject();
                if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
                    gotData = true;
                    int level = intent.getIntExtra("level", 0);
                    int scale = intent.getIntExtra("scale", 100);
                    try {
                        json.put("level", String.valueOf(level * 100 / scale));
                    } catch (JSONException e) {
                        Log.e(TAG, "JSONException preparing screen activity data");
                    }
                    int plugType = intent.getIntExtra("plugged", 0);
                    int status = intent
                            .getIntExtra("status", BatteryManager.BATTERY_STATUS_UNKNOWN);
                    String statusString;
                    if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                        statusString = "charging";
                        if (plugType > 0) {
                            statusString = statusString
                                    + " "
                                    + ((plugType == BatteryManager.BATTERY_PLUGGED_AC)
                                            ? "AC"
                                            : "USB");
                        }
                        try {
                            json.put("status", statusString);
                        } catch (JSONException e) {
                            Log.e(TAG, "JSONException preparing screen activity data");
                        }

                    } else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) {
                        try {
                            json.put("status", "discharging");
                        } catch (JSONException e) {
                            Log.e(TAG, "JSONException preparing screen activity data");
                        }
                    } else if (status == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {
                        try {
                            json.put("status", "not charging");
                        } catch (JSONException e) {
                            Log.e(TAG, "JSONException preparing screen activity data");
                        }
                    } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
                        try {
                            json.put("status", "full");
                        } catch (JSONException e) {
                            Log.e(TAG, "JSONException preparing screen activity data");
                        }
                    } else {
                        try {
                            json.put("status", "unknown");
                        } catch (JSONException e) {
                            Log.e(TAG, "JSONException preparing screen activity data");
                        }
                    }
                } else if (intent.getAction().equals(Intent.ACTION_BATTERY_LOW)) {
                    gotData = true;
                    try {
                        json.put("status", "low");
                    } catch (JSONException e) {
                        Log.e(TAG, "JSONException preparing screen activity data");
                    }
                }
                // check if the intent was a screen change intent
                if (gotData) {
                    Intent i = new Intent(MsgHandler.ACTION_NEW_MSG);
                    i.putExtra(MsgHandler.KEY_DATA_TYPE, Constants.SENSOR_DATA_TYPE_JSON);
                    i.putExtra(MsgHandler.KEY_VALUE, json.toString());
                    i.putExtra(MsgHandler.KEY_SENSOR_NAME, BATTERY_SENSOR);
                    i.putExtra(MsgHandler.KEY_TIMESTAMP, System.currentTimeMillis());
                    lastSampleTime = System.currentTimeMillis();
                    context.startService(i);
                }
                // check if the intent was a activity change intent
            }
        }
    };

    public void setSampleDelay(long _sampleDelay) {
        sampleDelay = _sampleDelay;
    }

    public void startBatterySensing(long _sampleDelay) {
        setSampleDelay(_sampleDelay);
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        context.registerReceiver(batteryChangeReceiver, filter);
    }

    public void stopBatterySensing() {
        try {
            context.unregisterReceiver(batteryChangeReceiver);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }

    public long getSampleDelay() {
        return sampleDelay;
    }
}