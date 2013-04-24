package nl.sense_os.service.commonsense.senddata;

import java.lang.ref.WeakReference;
import java.util.Map;

import nl.sense_os.service.R;
import nl.sense_os.service.SenseService;
import nl.sense_os.service.commonsense.SenseApi;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.storage.LocalStorage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

/**
 * Handler for transmission of a simple JSONObject containing sensor data for one sensor.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class DataTransmitHandler extends Handler {

    private static final String TAG = "DataTransmitHandler";
    private final WeakReference<Context> ctxRef;
    private final WeakReference<LocalStorage> storageRef;

    public DataTransmitHandler(Context context, LocalStorage storage, Looper looper) {
        super(looper);
        ctxRef = new WeakReference<Context>(context);
        storageRef = new WeakReference<LocalStorage>(storage);
    }

    private void cleanup(WakeLock wakeLock) {
        if (null != wakeLock) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    @Override
    public void handleMessage(Message msg) {

        // get arguments from message
        Bundle args = msg.getData();
        String name = args.getString("name");
        String description = args.getString("description");
        String dataType = args.getString("dataType");
        String deviceUuid = args.getString("deviceUuid");
        String cookie = args.getString("cookie");
        JSONObject json = (JSONObject) msg.obj;

        // check if our references are still valid
        if (null == ctxRef.get() || null == storageRef.get()) {
            // parent service has died
            return;
        }

        WakeLock wakeLock = null;
        try {
            // make sure the device stays awake while transmitting
            PowerManager powerMgr = (PowerManager) ctxRef.get().getSystemService(
                    Context.POWER_SERVICE);
            wakeLock = powerMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            wakeLock.acquire();

            // get sensor URL at CommonSense
            String url = SenseApi.getSensorUrl(ctxRef.get(), name, description, dataType,
                    deviceUuid);

            if (url == null) {
                Log.w(TAG, "No sensor ID for '" + name + "' (yet): data will be retried.");
                return;
            }

            Map<String, String> response = SenseApi.request(ctxRef.get(), url, json, cookie);

            // Error when sending
            if ((response == null) || !response.get("http response code").equals("201")) {

                // if un-authorized: relogin
                if ((response != null) && response.get("http response code").equals("403")) {
                    final Intent serviceIntent = new Intent(ctxRef.get().getString(
                            R.string.action_sense_service));
                    serviceIntent.putExtra(SenseService.EXTRA_RELOGIN, true);
                    ctxRef.get().startService(serviceIntent);
                }

                // Show the HTTP response Code
                if (response != null) {
                    Log.w(TAG,
                            "Failed to send '" + name + "' data. Response code:"
                                    + response.get("http response code") + ", Response content: '"
                                    + response.get("content") + "'\nData will be retried");
                } else {
                    Log.w(TAG, "Failed to send '" + name + "' data.\nData will be retried.");
                }
            }

            // Data sent successfully
            else {
                int bytes = json.toString().getBytes().length;
                Log.i(TAG, "Sent '" + name + "' data! Raw data size: " + bytes + " bytes");

                onTransmitSuccess(name, description, json);
            }

        } catch (Exception e) {
            if (null != e.getMessage()) {
                Log.e(TAG,
                        "Exception sending '" + name + "' data, data will be retried: "
                                + e.getMessage());
            } else {
                Log.e(TAG, "Exception sending '" + name + "' data, data will be retried.", e);
            }

        } finally {
            cleanup(wakeLock);
        }
    }

    private void onTransmitSuccess(String name, String description, JSONObject json)
            throws JSONException {
        // new content values with updated transmit state
        ContentValues values = new ContentValues();
        values.put(DataPoint.TRANSMIT_STATE, 1);

        // select points for this sensor, between the fist and the last time stamp
        JSONArray dataPoints = json.getJSONArray("data");
        String frstTimeStamp = dataPoints.getJSONObject(0).getString("date");
        String lastTimeStamp = dataPoints.getJSONObject(dataPoints.length() - 1).getString("date");
        long min = Math.round(Double.parseDouble(frstTimeStamp) * 1000);
        long max = Math.round(Double.parseDouble(lastTimeStamp) * 1000);
        String where = DataPoint.SENSOR_NAME + "='" + name + "'" + " AND "
                + DataPoint.SENSOR_DESCRIPTION + "='" + description + "'" + " AND "
                + DataPoint.TIMESTAMP + ">=" + min + " AND " + DataPoint.TIMESTAMP + " <=" + max;

        try {
            Uri contentUri = Uri.parse("content://"
                    + ctxRef.get().getString(R.string.local_storage_authority)
                    + DataPoint.CONTENT_URI_PATH);
            int updated = storageRef.get().update(contentUri, values, where, null);
            if (updated == dataPoints.length()) {
                // Log.v(TAG, "Updated all " + updated + " rows in the local storage");
            } else {
                Log.w(TAG, "Wrong number of local storage points updated! " + updated + " vs. "
                        + dataPoints.length());
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error updating points in Local Storage!", e);
        }
    }
}
