/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.phonestate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.SensorDataPoint;
import nl.sense_os.service.subscription.BaseDataProducer;

/**
 * Represents the battery sensor. Registers itself for ACTION_BATTERY_CHANGED Broadcasts from
 * Android.
 * 
 * @author Ted Schmidt <ted@sense-os.nl>
 */
public class BatterySensor extends BaseDataProducer{
    private static final String TAG = "Sense Battery sensor";
    private long sampleDelay = 0; // in milliseconds
    private long lastSampleTime;
    private Context context;

    protected BatterySensor(Context context) {
        this.context = context;
    }
    
    private static BatterySensor instance = null;
    
    public static BatterySensor getInstance(Context context) {
	    if(instance == null) {
	       instance = new BatterySensor(context);
	    }
	    return instance;
    }

    private BroadcastReceiver batteryChangeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Log.d(TAG, "BatteryChangeReceiver received broadcast");

            boolean gotData = false;

            if (SNTP.getInstance().getTime() > lastSampleTime + sampleDelay) {

                // Send a message when the battery state has changed
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
                                    + ((plugType == BatteryManager.BATTERY_PLUGGED_AC) ? "AC"
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

                if (gotData) {
                    // Log.v(TAG, "Transmit battery state: " + json.toString());

                	notifySubscribers();
                	SensorDataPoint dataPoint = new SensorDataPoint(json);
                	dataPoint.sensorName = SensorNames.BATTERY;
                	dataPoint.sensorDescription = SensorNames.BATTERY;
                	dataPoint.timeStamp = SNTP.getInstance().getTime();        
                	sendToSubscribers(dataPoint);
                }

            } else {
                // Log.v(TAG, "Skipped battery state update: too soon after last transmission...");
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
