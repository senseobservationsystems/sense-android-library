/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.phonestate;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main.Advanced;
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
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.util.Log;

/**
 * Represents the battery sensor. Registers itself for ACTION_BATTERY_CHANGED Broadcasts from
 * Android.
 * 
 * @author Ted Schmidt <ted@sense-os.nl>
 */
public class BatterySensor extends BaseSensor implements PeriodicPollingSensor {
    private static final String TAG = "Sense Battery sensor";
    private long lastSampleTime;
    private boolean active;
    private Context context;

    private PeriodicPollAlarmReceiver alarmReceiver;

    protected BatterySensor(Context context) {
        this.context = context;
        alarmReceiver = new PeriodicPollAlarmReceiver(this);
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
            if (System.currentTimeMillis() > lastSampleTime + getSampleRate()) {
                updateBatteryState(intent);
            } else {
                //Log.v(TAG, "Skipped battery state update: too soon after last transmission...");
            }
        }
    };

    private void updateBatteryState(Intent intent) {
        boolean gotData = false;

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
            //Log.v(TAG, "Transmit battery state: " + json.toString());

            notifySubscribers();
            SensorDataPoint dataPoint = new SensorDataPoint(json);
            dataPoint.sensorName = SensorNames.BATTERY_SENSOR;
            dataPoint.sensorDescription = SensorNames.BATTERY_SENSOR;
            dataPoint.timeStamp = SNTP.getInstance().getTime();
            sendToSubscribers(dataPoint);

            Intent i = new Intent(context.getString(R.string.action_sense_new_data));
            i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
            i.putExtra(DataPoint.VALUE, json.toString());
            i.putExtra(DataPoint.SENSOR_NAME, SensorNames.BATTERY_SENSOR);
            i.putExtra(DataPoint.TIMESTAMP, dataPoint.timeStamp);
            lastSampleTime = System.currentTimeMillis();
            context.startService(i);
        }
    }

    @Override
    public void doSample() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        Intent batteryStateIntent = context.registerReceiver(null, filter);
        updateBatteryState(batteryStateIntent);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    private void startPolling() {
        //Log.v(TAG, "start polling");
        alarmReceiver.start(context);
    }

    public void startSensing(long sampleDelay) {
        setSampleRate(sampleDelay);
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        context.registerReceiver(batteryChangeReceiver, filter);

        SharedPreferences mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
                Context.MODE_PRIVATE);

        if (mainPrefs.getBoolean(Advanced.HYBRID_EVENT_BASED_SENSOR, false))
          startPolling();
    }

    public void stopPolling() {
        //Log.v(TAG, "stop polling");
        alarmReceiver.stop(context);
    }

    public void stopSensing() {
        try {
            context.unregisterReceiver(batteryChangeReceiver);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        stopPolling();
    }
}
