/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.states;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SensorData;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.storage.LocalStorage;

/**
 * State monitor that periodically checks the data form the epi sensor and triggers and action when
 * there was a large amount of activity. Used for demo purposes only.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class EpiStateMonitor extends AbstractStateMonitor {

    private static final long TIME_RANGE = 5000;
    private static final String ACTION_UPDATE_STATE = "nl.sense_os.states.EpiStateUpdate";
    private static final String TAG = "EpiStateMonitor";
    private long lastAnalyzed;

    /**
     * Updates the epi-state by analyzing the local data points.
     */
    @Override
    protected void updateState() {
        // Log.d(TAG, "update state");

        Cursor fallData = null;
        Cursor epiData = null;
        try {
            LocalStorage storage = LocalStorage.getInstance(this);

            // query fall detector
            Uri contentUri = Uri.parse("content://" + getString(R.string.local_storage_authority)
                    + DataPoint.CONTENT_URI_PATH);
            String[] projection = new String[] { DataPoint._ID, DataPoint.SENSOR_NAME,
                    DataPoint.VALUE, DataPoint.TIMESTAMP };
            String where = DataPoint.SENSOR_NAME + "='" + SensorData.SensorNames.FALL_DETECTOR
                    + "'" + " AND " + DataPoint.TIMESTAMP + ">"
                    + (SNTP.getInstance().getTime() - TIME_RANGE);
            fallData = storage.query(contentUri, projection, where, null, null);

            if (null != fallData && fallData.moveToFirst()) {
                int result = analyzeFallData(fallData);
                // Log.d(TAG, "Fall analysis result: " + result);
                if (result > 0) {
                    sendAlert(fallData);
                    return;
                }
            } else {
                // Log.d(TAG, "No recent fall data to analyze");
            }

            // query epi acceleration
            projection = new String[] { DataPoint._ID, DataPoint.SENSOR_NAME, DataPoint.VALUE,
                    DataPoint.TIMESTAMP };
            where = DataPoint.SENSOR_NAME + "='" + SensorData.SensorNames.ACCELEROMETER_EPI + "'"
                    + " AND " + DataPoint.TIMESTAMP + ">"
                    + (SNTP.getInstance().getTime() - (TIME_RANGE << 1));
            epiData = storage.query(contentUri, projection, where, null, null);

            if (null != epiData && epiData.moveToFirst()) {
                int result = analyzeEpiData(epiData);
                // Log.d(TAG, "Epi analysis result: " + result);
                if (result > 0) {
                    sendAlert(epiData);
                    return;
                }
            } else {
                // Log.d(TAG, "No recent epi data to analyze");
            }

        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse data");
            return;

        } finally {
            if (null != fallData) {
                fallData.close();
                fallData = null;
            }
            if (null != epiData) {
                epiData.close();
                epiData = null;
            }
        }
    }

    private void sendAlert(Cursor data) {
        Log.w(TAG, "ALERT ALERT!!!! SEIZURE DETECTED!!!");

        Intent alert = new Intent("nl.ask.paige.receiver.IntentRx");
        alert.putExtra("sensorName", "epi state");
        alert.putExtra("value", "SEIZURE!!!");
        alert.putExtra("timestamp", "" + SNTP.getInstance().getTime());
        sendBroadcast(alert);
    }

    private int analyzeEpiData(Cursor data) throws JSONException {

        data.moveToLast();

        // parse the value
        long timestamp = data.getLong(data.getColumnIndex(DataPoint.TIMESTAMP));
        if (lastAnalyzed < timestamp) {
            lastAnalyzed = timestamp;
        } else {
            // Log.d(TAG, "Already analyzed this one");
            return 0;
        }
        String rawValue = data.getString(data.getColumnIndex(DataPoint.VALUE));
        JSONObject value = new JSONObject(rawValue);
        JSONArray array = value.getJSONArray("data");

        // Log.d(TAG, "Found " + array.length() + " epi data points, interval: " + interval +
        // " ms");

        // analyze the array of data points
        double total = 0;
        for (int i = 0; i < array.length(); i++) {
            JSONObject dataPoint = array.getJSONObject(i);
            double x = dataPoint.getDouble("x-axis");
            double y = dataPoint.getDouble("y-axis");
            double z = dataPoint.getDouble("z-axis");
            double length = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));

            total += length;
        }

        double avg = total / array.length();
        // Log.d(TAG, "Average acceleration: " + avg);
        if (avg > 12) {
            return 1;
        } else {
            return 0;
        }
    }

    private int analyzeFallData(Cursor data) throws JSONException {
        data.moveToLast();

        // parse the value
        long timestamp = data.getLong(data.getColumnIndex(DataPoint.TIMESTAMP));
        if (lastAnalyzed < timestamp) {
            lastAnalyzed = timestamp;
            if (data.getString(data.getColumnIndex(DataPoint.VALUE)).equals("true")) {
                return 1;
            } else {
                // Log.d(TAG, "No fall");
                return 0;
            }
        } else {
            // Log.d(TAG, "Already analyzed this one");
            return 0;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startMonitoring(ACTION_UPDATE_STATE, TIME_RANGE >> 1);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMonitoring();
    }
}
