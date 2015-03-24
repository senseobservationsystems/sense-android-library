package nl.sense_os.service.commonsense.senddata;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import nl.sense_os.service.MsgHandler;
import nl.sense_os.service.R;
import nl.sense_os.service.SenseService;
import nl.sense_os.service.commonsense.SenseApi;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main;
import nl.sense_os.service.constants.SenseUrls;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.storage.LocalStorage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

/**
 * Handler for transmit tasks of recently added data. Updates {@link DataPoint#TRANSMIT_STATE} of
 * the data points after the transmission is completed successfully. Note that this handler is
 * re-usable: every time the handler receives a message, it gets the latest data in a Cursor and
 * sends it to CommonSense.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class BufferTransmitHandler extends Handler {

    class SensorDataEntry {
        String sensorId;
        String sensorName;
        String sensorDescription;
        JSONArray data;
    }

	private static final String TAG = "BatchDataTransmitHandler";
	private static final int MAX_POST_DATA = 100;
	private static final int LIMIT_UNSENT_DATA = 1000;
    private final Uri contentUri;
    private final WeakReference<Context> ctxRef;
    private final WeakReference<LocalStorage> storageRef;
	private final String url;
	private final DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ENGLISH);
	private final NumberFormat dateFormatter = new DecimalFormat("##########.###", symbols);

	public BufferTransmitHandler(Context context, LocalStorage storage, Looper looper) {
		super(looper);
		this.ctxRef = new WeakReference<Context>(context);
		this.storageRef = new WeakReference<LocalStorage>(storage);

        contentUri = Uri.parse("content://" + context.getString(R.string.local_storage_authority)
                + DataPoint.CONTENT_URI_PATH);

		SharedPreferences mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
				Context.MODE_PRIVATE);
		boolean devMode = mainPrefs.getBoolean(Main.Advanced.DEV_MODE, false);
		url = devMode ? SenseUrls.SENSOR_DATA_MULTIPLE_DEV : SenseUrls.SENSOR_DATA_MULTIPLE;
	}

	/**
	 * Cleans up after transmission is over. Closes the Cursor with the data and releases the wake
	 * lock. Should always be called after transmission, even if the attempt failed.
	 * 
	 * @param cursor
	 */
	private void cleanup(Cursor cursor, WakeLock wakeLock) {
		if (null != cursor) {
			cursor.close();
			cursor = null;
		}
		if (null != wakeLock) {
			wakeLock.release();
			wakeLock = null;
		}
	}

    private List<SensorDataEntry> getSensorDataList(Cursor cursor) throws IOException,
            JSONException {

        // map of transmission entries, indexed by the sensor name and description
        Map<String, SensorDataEntry> map = new HashMap<String, SensorDataEntry>();
        String name, description, dataType, value, deviceUuid;
        long timestamp;
        int points = 0;
        while ((points < MAX_POST_DATA) && !cursor.isAfterLast()) {

            // get the data point details
            try {
                name = cursor.getString(cursor.getColumnIndexOrThrow(DataPoint.SENSOR_NAME));
                description = cursor.getString(cursor
                        .getColumnIndexOrThrow(DataPoint.SENSOR_DESCRIPTION));
                dataType = cursor.getString(cursor.getColumnIndexOrThrow(DataPoint.DATA_TYPE));
                value = cursor.getString(cursor.getColumnIndexOrThrow(DataPoint.VALUE));
                timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(DataPoint.TIMESTAMP));
                deviceUuid = cursor.getString(cursor.getColumnIndexOrThrow(DataPoint.DEVICE_UUID));

                // set default sensor ID if it is missing
                deviceUuid = deviceUuid != null ? deviceUuid : SenseApi.getDefaultDeviceUuid(ctxRef
                        .get());

            } catch (IllegalArgumentException e) {
                // something is wrong with this data point, skip it
                Log.w(TAG, "Exception getting data point details from cursor: '" + e.getMessage()
                        + "'. Skip data point...");
                cursor.moveToNext();
                continue;
            }

            /*
             * "normal" data is added to the map until we reach the max amount of points
             */
            if (!dataType.equals(SenseDataTypes.FILE)) {

                // construct JSON representation of the value
                JSONObject jsonDataPoint = new JSONObject();
                jsonDataPoint.put("date", dateFormatter.format(timestamp / 1000d));
                jsonDataPoint.put("value", value);

                // put the new value Object in the appropriate sensor's data
                String key = name + description;
                SensorDataEntry sensorEntry = map.get(key);
                JSONArray data = null;
                if (sensorEntry == null) {
                    sensorEntry = new SensorDataEntry();
                    String id = SenseApi.getSensorId(ctxRef.get(), name, description, dataType,
                            deviceUuid);
                    if (null == id) {
                        // skip sensor data that does not have a sensor ID yet
                        Log.w(TAG, "cannot find sensor ID for " + name + " (" + description + ")");
                        cursor.moveToNext();
                        continue;
                    }
                    sensorEntry.sensorId = id;
                    sensorEntry.sensorName = name;
                    sensorEntry.sensorDescription = description;
                    data = new JSONArray();
                } else {
                    data = sensorEntry.data;
                }
                data.put(jsonDataPoint);
                sensorEntry.data = data;
                map.put(key, sensorEntry);

                // count the added point to the total number of sensor data
                points++;

            } else {
                // if the data type is a "file", we need special handling
                sendFile(name, description, dataType, deviceUuid, value, timestamp);

            }

            cursor.moveToNext();
        }

        return new ArrayList<BufferTransmitHandler.SensorDataEntry>(map.values());
    }

	/**
	 * @return Cursor with the data points that have to be sent to CommonSense.
	 */
    private Cursor getUnsentData() {
        try {
            String where = DataPoint.TRANSMIT_STATE + "==0";
            String sortOrder = DataPoint.TIMESTAMP + " ASC";
            Cursor unsent = storageRef.get().query(contentUri, null, where, null, LIMIT_UNSENT_DATA, sortOrder);
            if (null != unsent) {
                Log.v(TAG, "Found " + unsent.getCount() + " unsent data points in local storage");
            } else {
                Log.w(TAG, "Failed to get unsent recent data points from local storage");
            }
            return unsent;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error querying Local Storage!", e);
            return null;
        }
    }

	@Override
	public void handleMessage(Message msg) {
	  
		String cookie = msg.getData().getString("cookie");

		// check if our references are still valid
		if (null == ctxRef.get() || null == storageRef.get()) {
			// parent service has died
			return;
		}

		WakeLock wakeLock = null;
		Cursor cursor = null;
		try {
			// make sure the device stays awake while transmitting
			PowerManager powerMgr = (PowerManager) ctxRef.get().getSystemService(
					Context.POWER_SERVICE);
			wakeLock = powerMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
			wakeLock.acquire();
      
			cursor = getUnsentData();
      if (null != cursor && cursor.moveToFirst()) {
				transmit(cursor, cookie);
			} else {
				// nothing to transmit
			}

		} catch (Exception e) {
			if (null != e.getMessage()) {
				Log.e(TAG, "Exception sending buffered data: '" + e.getMessage()
						+ "'. Data will be resent later.");
			} else {
				Log.e(TAG, "Exception sending cursor data. Data will be resent later.", e);
			}

		} finally {
			cleanup(cursor, wakeLock);
		}
	}

    /**
     * Performs cleanup tasks after transmission was successfully completed. Should update the data
     * point records to show that they have been sent to CommonSense.
     * 
     * @param sensorDatas
     *            List of data that was sent to CommonSense. Contains all the data points that were
     *            transmitted.
     * @throws Exception
     */
    private void onTransmitSuccess(List<SensorDataEntry> sensorDatas)
            throws JSONException{
        // log our great success
        Log.i(TAG, "Sent recent sensor data from the local storage!");

        // new content values with updated transmit state
        ContentValues values = new ContentValues();
        values.put(DataPoint.TRANSMIT_STATE, 1);

        for (SensorDataEntry sensorData : sensorDatas) {

            // get the name of the sensor, to use in the ContentResolver query
            String sensorName = sensorData.sensorName;
            String description = sensorData.sensorDescription;

            // select points for this sensor, between the first and the last time stamp
            JSONArray dataPoints = sensorData.data;
            String frstTimeStamp = dataPoints.getJSONObject(0).getString("date");
            String lastTimeStamp = dataPoints.getJSONObject(dataPoints.length() - 1).getString(
                    "date");
            long min = Math.round(Double.parseDouble(frstTimeStamp) * 1000);
            long max = Math.round(Double.parseDouble(lastTimeStamp) * 1000);
            String where = DataPoint.SENSOR_NAME + "='" + sensorName + "'" + " AND "
                    + DataPoint.SENSOR_DESCRIPTION + "='" + description + "'" + " AND "
                    + DataPoint.TIMESTAMP + ">=" + min + " AND " + DataPoint.TIMESTAMP + " <="
                    + max;

            // update points in local storage
            try {
                int updated = storageRef.get().update(contentUri, values, where, null);
                if (updated == dataPoints.length()) {
                    // Log.v(TAG, "Updated all " + updated + " '" + sensorName
                    // + "' data points in the local storage");
                } else {
                    Log.w(TAG, "Wrong number of '" + sensorName
                            + "' data points updated after transmission! " + updated + " vs. "
                            + dataPoints.length());
                }
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Error updating points in Local Storage!", e);
            }
            
            //storageRef.get().persistRecentData();
        }
    }

    /**
     * POSTs the sensor data points to the main sensor data URL at CommonSense.
     * 
     * @param cookie
     * 
     * @param transmission
     *            JSON Object with data points for transmission
     * @return true if successfully sent
     * @throws JSONException
     * @throws MalformedURLException
     */
    private boolean postData(String cookie, JSONObject transmission) throws JSONException,
            MalformedURLException {

        Map<String, String> response = null;
        try {
            response = SenseApi.request(ctxRef.get(), url, transmission, cookie);
        } catch (IOException e) {
            // handle failure later
        }

        boolean result = false;

        if (response == null) {
            // Error when sending
            Log.w(TAG, "Failed to send buffered data points.\nData will be retried later.");
            result = false;

        } else if (response.get(SenseApi.RESPONSE_CODE).compareToIgnoreCase("201") != 0) {
            // incorrect status code
            String statusCode = response.get(SenseApi.RESPONSE_CODE);

            // if un-authorized: relogin
            if (statusCode.compareToIgnoreCase("403") == 0) {
                final Intent serviceIntent = new Intent(ctxRef.get().getString(
                        R.string.action_sense_service));
                serviceIntent.putExtra(SenseService.EXTRA_RELOGIN, true);
                ctxRef.get().startService(serviceIntent);
            }

            // Show the HTTP response Code
            Log.w(TAG, "Failed to send buffered data points: " + statusCode
                    + ", Response content: '" + response.get(SenseApi.RESPONSE_CONTENT) + "'\n"
                    + "Data will be retried later");

            result = false;

        } else {
            // Data sent successfully
            result = true;
        }

        return result;
    }

	private void sendFile(String name, String description, String dataType, String deviceUuid,
            String value, long timestamp) throws JSONException {

        // create sensor data JSON object with only 1 data point
        JSONObject sensorData = new JSONObject();
        JSONArray dataArray = new JSONArray();
        JSONObject data = new JSONObject();
        data.put("value", value);
        data.put("date", dateFormatter.format(timestamp / 1000d));
        dataArray.put(data);
        sensorData.put("data", dataArray);

        // send data point through MsgHandler
        Context context = ctxRef.get();
        MsgHandler.sendSensorData(context, name, description, dataType, deviceUuid, sensorData);
    }

    /**
     * Transmits the data points from {@link #cursor} to CommonSense. Any "file" type data points
     * will be sent separately via
     * {@link MsgHandler#sendSensorData(String, String, String, JSONObject)}.
     * 
     * @param cookie
     * @param cursor
     * 
     * @throws JSONException
     * @throws IOException
     */
    private void transmit(Cursor cursor, String cookie) throws JSONException, IOException {
      
        // continue until all points in the cursor have been sent
        List<SensorDataEntry> sensorDataList = null;
        while (!cursor.isAfterLast()) {

            // organize the data into a hash map sorted by sensor
            sensorDataList = getSensorDataList(cursor);

            if (sensorDataList.size() < 1) {
                // nothing to transmit
                continue;
            }

            // prepare the main JSON object for transmission
            JSONArray sensors = new JSONArray();
            for (SensorDataEntry sensorDataEntry : sensorDataList) {
                JSONObject transmissionEntry = new JSONObject();
                transmissionEntry.put("sensor_id", sensorDataEntry.sensorId);
                transmissionEntry.put("sensor_name", sensorDataEntry.sensorName);
                transmissionEntry.put("data", sensorDataEntry.data);
                sensors.put(transmissionEntry);
            }
            JSONObject transmission = new JSONObject();
            transmission.put("sensors", sensors);

            // perform the actual POST request
            boolean result = postData(cookie, transmission);
            
            if (result) {
                onTransmitSuccess(sensorDataList);
            } else {
                // abort! abort!
                break;
            }
        }
    }
}
