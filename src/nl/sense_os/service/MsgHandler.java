/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import nl.sense_os.service.commonsense.SenseApi;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Auth;
import nl.sense_os.service.constants.SensePrefs.Main;
import nl.sense_os.service.constants.SenseUrls;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.storage.LocalStorage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class MsgHandler extends Service {

    /**
     * Handler for tasks that send buffered sensor data to CommonSense. Nota that this handler is
     * re-usable: every time the handler receives a message, it gets the latest data in a Cursor and
     * sends it to CommonSense.<br>
     * <br>
     * Subclasses have to implement {@link #getUnsentData()} and
     * {@link #onTransmitSuccess(JSONObject)} to make them work with their intended data source.
     */
    private abstract class AbstractDataTransmitHandler extends Handler {

	final Context context;
	String cookie;
	Cursor cursor;
	private WakeLock wakeLock;
	private String url;
	private final DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ENGLISH);
	private final NumberFormat dateFormatter = new DecimalFormat("##########.###", symbols);

	public AbstractDataTransmitHandler(Context context, Looper looper) {
	    super(looper);
	    this.context = context;
	    SharedPreferences mainPrefs;
	    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
		mainPrefs = getSharedPreferences(SensePrefs.MAIN_PREFS, MODE_MULTI_PROCESS);
	    } else {
		mainPrefs = getSharedPreferences(SensePrefs.MAIN_PREFS, MODE_PRIVATE);
	    }
	    boolean devMode = mainPrefs.getBoolean(Main.Advanced.DEV_MODE, false);
	    url = devMode ? SenseUrls.DEV_SENSOR_DATA.replace("/<id>/", "/")
		    : SenseUrls.SENSOR_DATA.replace("/<id>/", "/");
	}

	/**
	 * Cleans up after transmission is over. Closes the Cursor with the data and releases the
	 * wake lock. Should always be called after transmission, even if the attempt failed.
	 */
	private void cleanup() {
	    if (null != cursor) {
		cursor.close();
		cursor = null;
	    }
	    if (null != wakeLock) {
		wakeLock.release();
		wakeLock = null;
	    }
	}

	/**
	 * @return Cursor with the data points that have to be sent to CommonSense.
	 */
	protected abstract Cursor getUnsentData();

	@Override
	public void handleMessage(Message msg) {
	    try {
		cookie = msg.getData().getString("cookie");
		cursor = getUnsentData();
		if ((null != cursor) && cursor.moveToFirst()) {
		    transmit();
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
		cleanup();
	    }
	}

	/**
	 * @param bytes
	 *            Byte count;
	 * @param si
	 *            true to use SI system, where 1000 B = 1 kB
	 * @return A String with human-readable byte count, including suffix.
	 */
	public String humanReadableByteCount(long bytes, boolean si) {
	    int unit = si ? 1000 : 1024;
	    if (bytes < unit) {
		return bytes + " B";
	    }
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
	    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	/**
	 * Performs cleanup tasks after transmission was successfully completed. Should update the
	 * data point records to show that they have been sent to CommonSense.
	 * 
	 * @param transmission
	 *            The JSON Object that was sent to CommonSense. Contains all the data points
	 *            that were transmitted.
	 * @throws JSONException
	 */
	protected abstract void onTransmitSuccess(JSONObject transmission) throws JSONException;

	/**
	 * POSTs the sensor data points to the main sensor data URL at CommonSense.
	 * 
	 * @param transmission
	 *            JSON Object with data points for transmission
	 * @throws JSONException
	 * @throws MalformedURLException
	 */
	private void postData(JSONObject transmission) throws JSONException, MalformedURLException {

	    Map<String, String> response = null;
	    try {
		response = SenseApi.request(context, url, transmission, cookie);
	    } catch (IOException e) {
		// handle failure later
	    }

	    if (response == null) {
		// Error when sending
		Log.w(TAG, "Failed to send buffered data points.\nData will be retried later.");

	    } else if (response.get("http response code").compareToIgnoreCase("201") != 0) {
		// incorrect status code
		String statusCode = response.get("http response code");

		// if un-authorized: relogin
		if (statusCode.compareToIgnoreCase("403") == 0) {
		    final Intent serviceIntent = new Intent(
			    getString(R.string.action_sense_service));
		    serviceIntent.putExtra(SenseService.EXTRA_RELOGIN, true);
		    context.startService(serviceIntent);
		}

		// Show the HTTP response Code
		Log.w(TAG, "Failed to send buffered data points: " + statusCode
			+ ", Response content: '" + response.get("content") + "'\n"
			+ "Data will be retried later");

	    } else {
		// Data sent successfully
		onTransmitSuccess(transmission);
	    }
	}

	/**
	 * Transmits the data points from {@link #cursor} to CommonSense. Any "file" type data
	 * points will be sent separately via
	 * {@link MsgHandler#sendSensorData(String, String, String, JSONObject)}.
	 * 
	 * @throws JSONException
	 * @throws IOException
	 */
	private void transmit() throws JSONException, IOException {

	    // make sure the device stays awake while transmitting
	    wakeLock = getWakeLock();
	    wakeLock.acquire();

	    // continue until all points in the cursor have been sent
	    HashMap<String, JSONObject> sensorDataMap = null;
	    while (!cursor.isAfterLast()) {

		// organize the data into a hash map sorted by sensor
		sensorDataMap = new HashMap<String, JSONObject>();
		String name, description, dataType, value, deviceUuid;
		long timestamp;
		int points = 0;
		while ((points < MAX_POST_DATA) && !cursor.isAfterLast()) {

		    // get the data point details
		    try {
			name = cursor
				.getString(cursor.getColumnIndexOrThrow(DataPoint.SENSOR_NAME));
			description = cursor.getString(cursor
				.getColumnIndexOrThrow(DataPoint.SENSOR_DESCRIPTION));
			dataType = cursor.getString(cursor
				.getColumnIndexOrThrow(DataPoint.DATA_TYPE));
			value = cursor.getString(cursor.getColumnIndexOrThrow(DataPoint.VALUE));
			timestamp = cursor.getLong(cursor
				.getColumnIndexOrThrow(DataPoint.TIMESTAMP));
			deviceUuid = cursor.getString(cursor
				.getColumnIndexOrThrow(DataPoint.DEVICE_UUID));

			// set default sensor ID if it is missing
			deviceUuid = deviceUuid != null ? deviceUuid : SenseApi
				.getDefaultDeviceUuid(MsgHandler.this);

		    } catch (IllegalArgumentException e) {
			// something is wrong with this data point, skip it
			Log.w(TAG,
				"Exception getting data point details from cursor: '"
					+ e.getMessage() + "'. Skip data point...");
			cursor.moveToNext();
			continue;
		    }

		    // "normal" data is added to the map until we reach the max amount of points
		    if (!dataType.equals(SenseDataTypes.FILE)) {

			// construct JSON representation of the value
			JSONObject jsonDataPoint = new JSONObject();
			jsonDataPoint.put("date", dateFormatter.format(timestamp / 1000d));
			jsonDataPoint.put("value", value);

			// put the new value Object in the appropriate sensor's data
			String key = name + description;
			JSONObject sensorEntry = sensorDataMap.get(key);
			JSONArray data = null;
			if (sensorEntry == null) {
			    sensorEntry = new JSONObject();
			    String id = SenseApi.getSensorId(context, name, description, dataType,
				    deviceUuid);
			    if (null == id) {
				// skip sensor data that does not have a sensor ID yet
				Log.w(TAG, "cannot find sensor ID for " + name + " (" + description
					+ ")");
				cursor.moveToNext();
				continue;
			    }
			    sensorEntry.put("sensor_id", id);
			    sensorEntry.put("sensor_name", name);
			    data = new JSONArray();
			} else {
			    data = sensorEntry.getJSONArray("data");
			}
			data.put(jsonDataPoint);
			sensorEntry.put("data", data);
			sensorDataMap.put(key, sensorEntry);

			// count the added point to the total number of sensor data
			points++;

		    } else {
			// if the data type is a "file", we need special handling
			// Log.d(TAG, "Transmit file separately from the other data points: '" +
			// value + "'");

			// create sensor data JSON object with only 1 data point
			JSONObject sensorData = new JSONObject();
			JSONArray dataArray = new JSONArray();
			JSONObject data = new JSONObject();
			data.put("value", value);
			data.put("date", dateFormatter.format(timestamp / 1000d));
			dataArray.put(data);
			sensorData.put("data", dataArray);

			sendSensorData(name, description, dataType, deviceUuid, sensorData);
		    }

		    cursor.moveToNext();
		}

		if (sensorDataMap.size() < 1) {
		    // no data to transmit
		    continue;
		}

		// prepare the main JSON object for transmission
		JSONArray sensors = new JSONArray();
		for (Entry<String, JSONObject> entry : sensorDataMap.entrySet()) {
		    sensors.put(entry.getValue());
		}
		JSONObject transmission = new JSONObject();
		transmission.put("sensors", sensors);

		// perform the actual POST request
		postData(transmission);
	    }
	}
    }

    /**
     * Handler for transmit tasks of persisted data (i.e. data that was stored in the SQLite
     * database). Removes the data from the database after the transmission is completed
     * successfully.
     */
    private class PersistedDataTransmitHandler extends AbstractDataTransmitHandler {

	private final Uri contentUri;

	public PersistedDataTransmitHandler(Context context, Looper looper) {
	    super(context, looper);
	    contentUri = Uri.parse("content://" + getString(R.string.local_storage_authority)
		    + DataPoint.CONTENT_PERSISTED_URI_PATH);
	}

	@Override
	protected Cursor getUnsentData() {
	    try {
		String where = DataPoint.TRANSMIT_STATE + "=0";
		Cursor unsent = storage.query(contentUri, null, where, null, null);

		if ((null != unsent) && unsent.moveToFirst()) {
		    Log.v(TAG, "Found " + unsent.getCount()
			    + " unsent data points in persistant storage");
		    return unsent;
		} else {
		    Log.v(TAG, "No unsent data points in the persistant storage");
		    return new MatrixCursor(new String[] {});
		}
	    } catch (IllegalArgumentException e) {
		Log.e(TAG, "Error querying Local Storage!", e);
		return new MatrixCursor(new String[] {});
	    }
	}

	@Override
	protected void onTransmitSuccess(JSONObject transmission) throws JSONException {

	    // log our great success
	    int bytes = transmission.toString().getBytes().length;
	    Log.i(TAG, "Sent old sensor data from persistant storage! Raw data size: "
		    + humanReadableByteCount(bytes, false));

	    int totalDatapoints = 0;
	    JSONArray sensorDatas = transmission.getJSONArray("sensors");
	    for (int i = 0; i < sensorDatas.length(); i++) {

		JSONObject sensorData = sensorDatas.getJSONObject(i);

		// get the name of the sensor, to use in the ContentResolver query
		String sensorName = sensorData.getString("sensor_name");

		// select points for this sensor, between the first and the last time stamp
		JSONArray dataPoints = sensorData.getJSONArray("data");
		totalDatapoints += dataPoints.length();
		String frstTimeStamp = dataPoints.getJSONObject(0).getString("date");
		String lastTimeStamp = dataPoints.getJSONObject(dataPoints.length() - 1).getString(
			"date");
		long min = Math.round(Double.parseDouble(frstTimeStamp) * 1000);
		long max = Math.round(Double.parseDouble(lastTimeStamp) * 1000);
		String where = DataPoint.SENSOR_NAME + "='" + sensorName + "'" + " AND "
			+ DataPoint.TIMESTAMP + ">=" + min + " AND " + DataPoint.TIMESTAMP + " <="
			+ max;

		// delete the data from the storage
		try {
		    int deleted = storage.delete(contentUri, where, null);
		    if (deleted == dataPoints.length()) {
			// Log.v(TAG, "Deleted all " + deleted + " '" + sensorName +
			// "' points from the persistant storage");
		    } else {
			Log.w(TAG, "Wrong number of '" + sensorName
				+ "' data points deleted after transmission! " + deleted + " vs. "
				+ dataPoints.length());
		    }
		} catch (IllegalArgumentException e) {
		    Log.e(TAG, "Error deleting points from Local Storage!", e);
		}
	    }

	    // check if there is more data left
	    SharedPreferences mainPrefs;
	    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
		mainPrefs = getSharedPreferences(SensePrefs.MAIN_PREFS, MODE_MULTI_PROCESS);
	    } else {
		mainPrefs = getSharedPreferences(SensePrefs.MAIN_PREFS, MODE_PRIVATE);
	    }
	    int maxDataPoints = LocalStorage.QUERY_RESULTS_LIMIT;

	    if (mainPrefs.getBoolean(Main.Motion.EPIMODE, false))
		maxDataPoints = LocalStorage.QUERY_RESULTS_LIMIT_EPI_MODE;

	    // there is probably more data, try to send more
	    if (totalDatapoints == maxDataPoints) {
		// Log.d(TAG,
		// "There is more data! Sending another batch from the persistant storage.");

		// prepare the data to give to the transmitters
		Bundle msgData = new Bundle();
		msgData.putString("cookie", cookie);

		Message msg = Message.obtain();
		msg.setData(msgData);
		persistedDataTransmitter.sendMessage(msg);
	    }
	}
    }

    /**
     * Handler for transmit tasks of recently added data (i.e. data that is stored in system RAM
     * memory). Updates {@link DataPoint#TRANSMIT_STATE} of the data points after the transmission
     * is completed successfully.
     */
    private class RecentDataTransmitHandler extends AbstractDataTransmitHandler {

	private final Uri contentUri;

	public RecentDataTransmitHandler(Context context, Looper looper) {
	    super(context, looper);
	    contentUri = Uri.parse("content://" + getString(R.string.local_storage_authority)
		    + DataPoint.CONTENT_URI_PATH);
	}

	@Override
	protected Cursor getUnsentData() {
	    try {
		String where = DataPoint.TRANSMIT_STATE + "=0";
		Cursor unsent = storage.query(contentUri, null, where, null, null);
		if ((null != unsent) && unsent.moveToFirst()) {
		    Log.v(TAG, "Found " + unsent.getCount()
			    + " unsent data points in local storage");
		    return unsent;
		} else {
		    Log.v(TAG, "No unsent recent data points");
		    return new MatrixCursor(new String[] {});
		}
	    } catch (IllegalArgumentException e) {
		Log.e(TAG, "Error querying Local Storage!", e);
		return new MatrixCursor(new String[] {});
	    }
	}

	@Override
	protected void onTransmitSuccess(JSONObject transmission) throws JSONException {

	    // log our great success
	    int bytes = transmission.toString().getBytes().length;
	    Log.i(TAG, "Sent recent sensor data from the local storage! Raw data size: "
		    + humanReadableByteCount(bytes, false));

	    // new content values with updated transmit state
	    ContentValues values = new ContentValues();
	    values.put(DataPoint.TRANSMIT_STATE, 1);

	    JSONArray sensorDatas = transmission.getJSONArray("sensors");
	    for (int i = 0; i < sensorDatas.length(); i++) {

		JSONObject sensorData = sensorDatas.getJSONObject(i);

		// get the name of the sensor, to use in the ContentResolver query
		String sensorName = sensorData.getString("sensor_name");

		// select points for this sensor, between the first and the last time stamp
		JSONArray dataPoints = sensorData.getJSONArray("data");
		String frstTimeStamp = dataPoints.getJSONObject(0).getString("date");
		String lastTimeStamp = dataPoints.getJSONObject(dataPoints.length() - 1).getString(
			"date");
		long min = Math.round(Double.parseDouble(frstTimeStamp) * 1000);
		long max = Math.round(Double.parseDouble(lastTimeStamp) * 1000);
		String where = DataPoint.SENSOR_NAME + "='" + sensorName + "'" + " AND "
			+ DataPoint.TIMESTAMP + ">=" + min + " AND " + DataPoint.TIMESTAMP + " <="
			+ max;

		// update points in local storage
		try {
		    int updated = storage.update(contentUri, values, where, null);
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
	    }
	}
    }

    private class SendDataThread extends Handler {

	private final String cookie;
	private final String name;
	private final String description;
	private final String dataType;
	private final String deviceUuid;
	private final Context context;
	private final JSONObject data;
	private WakeLock wakeLock;

	public SendDataThread(String cookie, JSONObject data, String name, String description,
		String dataType, String deviceUuid, Context context, Looper looper) {
	    super(looper);
	    this.cookie = cookie;
	    this.data = data;
	    this.name = name;
	    this.deviceUuid = deviceUuid != null ? deviceUuid : SenseApi
		    .getDefaultDeviceUuid(MsgHandler.this);
	    this.dataType = dataType;
	    this.description = description != null ? description : name;
	    this.context = context;

	}

	@Override
	public void handleMessage(Message msg) {

	    try {
		// make sure the device stays awake while transmitting
		wakeLock = getWakeLock();
		wakeLock.acquire();

		// get sensor URL at CommonSense
		String url = SenseApi
			.getSensorUrl(context, name, description, dataType, deviceUuid);

		if (url == null) {
		    Log.w(TAG, "No sensor ID for '" + name + "' (yet): data will be retried.");
		    return;
		}

		Map<String, String> response = SenseApi.request(context, url, data, cookie);

		// Error when sending
		if ((response == null) || !response.get("http response code").equals("201")) {

		    // if un-authorized: relogin
		    if ((response != null) && response.get("http response code").equals("403")) {
			final Intent serviceIntent = new Intent(
				getString(R.string.action_sense_service));
			serviceIntent.putExtra(SenseService.EXTRA_RELOGIN, true);
			context.startService(serviceIntent);
		    }

		    // Show the HTTP response Code
		    if (response != null) {
			Log.w(TAG,
				"Failed to send '" + name + "' data. Response code:"
					+ response.get("http response code")
					+ ", Response content: '" + response.get("content")
					+ "'\nData will be retried");
		    } else {
			Log.w(TAG, "Failed to send '" + name + "' data.\nData will be retried.");
		    }
		}

		// Data sent successfully
		else {
		    int bytes = data.toString().getBytes().length;
		    Log.i(TAG, "Sent '" + name + "' data! Raw data size: " + bytes + " bytes");

		    onTransmitSuccess();
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
		stopAndCleanup();
	    }
	}

	private void onTransmitSuccess() throws JSONException {
	    // new content values with updated transmit state
	    ContentValues values = new ContentValues();
	    values.put(DataPoint.TRANSMIT_STATE, 1);

	    // select points for this sensor, between the fist and the last time stamp
	    JSONArray dataPoints = data.getJSONArray("data");
	    String frstTimeStamp = dataPoints.getJSONObject(0).getString("date");
	    String lastTimeStamp = dataPoints.getJSONObject(dataPoints.length() - 1).getString(
		    "date");
	    long min = Math.round(Double.parseDouble(frstTimeStamp) * 1000);
	    long max = Math.round(Double.parseDouble(lastTimeStamp) * 1000);
	    String where = DataPoint.SENSOR_NAME + "='" + name + "'" + " AND "
		    + DataPoint.TIMESTAMP + ">=" + min + " AND " + DataPoint.TIMESTAMP + " <="
		    + max;

	    try {
		Uri contentUri = Uri.parse("content://"
			+ getString(R.string.local_storage_authority) + DataPoint.CONTENT_URI_PATH);
		int updated = storage.update(contentUri, values, where, null);
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

	private void stopAndCleanup() {
	    --nrOfSendMessageThreads;
	    wakeLock.release();
	    getLooper().quit();
	}
    }

    private class SendFileThread extends Handler {

	private final String cookie;
	private final JSONObject data;
	private final String sensorName;
	private final String dataType;
	private final String deviceType;
	private final String deviceUuid;
	private final Context context;
	private WakeLock wakeLock;

	public SendFileThread(String cookie, JSONObject data, String name, String description,
		String dataType, String deviceUuid, Context context, Looper looper) {
	    super(looper);
	    this.cookie = cookie;
	    this.data = data;
	    this.sensorName = name;
	    this.dataType = dataType;
	    this.deviceType = description;
	    this.deviceUuid = deviceUuid != null ? deviceUuid : SenseApi
		    .getDefaultDeviceUuid(MsgHandler.this);
	    ;
	    this.context = context;
	}

	@Override
	public void handleMessage(Message message) {

	    try {
		// make sure the device stays awake while transmitting
		wakeLock = getWakeLock();
		wakeLock.acquire();

		// get sensor URL from CommonSense
		String f_deviceType = deviceType != null ? deviceType : sensorName;
		String urlStr = SenseApi.getSensorUrl(context, sensorName, f_deviceType, dataType,
			deviceUuid);

		if (urlStr == null) {
		    Log.w(TAG, "No sensor ID for '" + sensorName + "' (yet). Data is lost.");
		    return;
		}

		// submit each file separately
		JSONArray data = (JSONArray) this.data.get("data");
		for (int i = 0; i < data.length(); i++) {
		    JSONObject object = (JSONObject) data.get(i);
		    String fileName = (String) object.get("value");

		    HttpURLConnection conn = null;
		    DataOutputStream dos = null;

		    String lineEnd = "\r\n";
		    String twoHyphens = "--";
		    String boundary = "----FormBoundary6bYQOdhfGEj4oCSv";

		    int bytesRead, bytesAvailable, bufferSize;
		    byte[] buffer;
		    int maxBufferSize = 1 * 1024 * 1024;

		    // ------------------ CLIENT REQUEST

		    FileInputStream fileInputStream = new FileInputStream(new File(fileName));

		    // open a URL connection to the Servlet
		    URL url = new URL(urlStr);

		    // Open a HTTP connection to the URL
		    conn = (HttpURLConnection) url.openConnection();

		    // Allow Inputs
		    conn.setDoInput(true);

		    // Allow Outputs
		    conn.setDoOutput(true);

		    // Don't use a cached copy.
		    conn.setUseCaches(false);

		    // Use a post method.
		    conn.setRequestMethod("POST");
		    conn.setRequestProperty("Cookie", cookie);
		    conn.setRequestProperty("Connection", "Keep-Alive");

		    conn.setRequestProperty("Content-Type", "multipart/form-data;boundary="
			    + boundary);

		    dos = new DataOutputStream(conn.getOutputStream());

		    dos.writeBytes(twoHyphens + boundary + lineEnd);
		    dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\""
			    + fileName + "\"" + lineEnd);
		    dos.writeBytes(lineEnd);
		    // create a buffer of maximum size
		    bytesAvailable = fileInputStream.available();
		    bufferSize = Math.min(bytesAvailable, maxBufferSize);
		    buffer = new byte[bufferSize];

		    // read file and write it into form...
		    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

		    while (bytesRead > 0) {
			dos.write(buffer, 0, bufferSize);
			bytesAvailable = fileInputStream.available();
			bufferSize = Math.min(bytesAvailable, maxBufferSize);
			bytesRead = fileInputStream.read(buffer, 0, bufferSize);
		    }

		    // send multipart form data necesssary after file data...
		    dos.writeBytes(lineEnd);
		    dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

		    // close streams
		    fileInputStream.close();
		    dos.flush();
		    dos.close();

		    if (conn.getResponseCode() != 201) {
			Log.e(TAG,
				"Failed to send '" + sensorName
					+ "' value file. Data lost. Response code:"
					+ conn.getResponseCode());
		    } else {
			Log.i(TAG, "Sent '" + sensorName + "' sensor value file OK!");
			String date = (String) object.get("date");
			onTransmitSuccess(date);
		    }
		}
	    } catch (Exception e) {
		Log.e(TAG, "Sending '" + sensorName + "' sensor file failed. Data lost.", e);
	    } finally {
		stopAndCleanup();
	    }
	}

	private void onTransmitSuccess(String timeInSecs) {

	    // new content values with updated transmit state
	    ContentValues values = new ContentValues();
	    values.put(DataPoint.TRANSMIT_STATE, 1);

	    long timestamp = Math.round(Double.parseDouble(timeInSecs) * 1000);
	    String where = DataPoint.SENSOR_NAME + "='" + sensorName + "'" + " AND "
		    + DataPoint.TIMESTAMP + "=" + timestamp;

	    try {
		Uri contentUri = Uri.parse("content://"
			+ getString(R.string.local_storage_authority) + DataPoint.CONTENT_URI_PATH);
		int updated = storage.update(contentUri, values, where, null);
		int deleted = 0;
		if (0 == updated) {
		    contentUri = Uri.parse("content://"
			    + getString(R.string.local_storage_authority)
			    + DataPoint.CONTENT_PERSISTED_URI_PATH);
		    deleted = storage.delete(contentUri, where, null);
		}
		if ((deleted == 1) || (updated == 1)) {
		    // ok
		} else {
		    Log.w(TAG,
			    "Failed to update the local storage after a file was successfully sent to CommonSense!");
		}
	    } catch (IllegalArgumentException e) {
		Log.e(TAG, "Error updating points in Local Storage!", e);
	    }

	}

	private void stopAndCleanup() {
	    --nrOfSendMessageThreads;
	    wakeLock.release();
	    getLooper().quit();
	}
    }

    private static final String TAG = "Sense MsgHandler";
    private static final int MAX_NR_OF_SEND_MSG_THREADS = 50;
    private static final int MAX_POST_DATA = 100;
    private int nrOfSendMessageThreads = 0;
    private WakeLock wakeLock;

    private RecentDataTransmitHandler recentDataTransmitter;
    private PersistedDataTransmitHandler persistedDataTransmitter;
    private LocalStorage storage;

    /**
     * Puts data from the buffer in the flash database for long-term storage
     */
    private void emptyBufferToDb() {
	// Log.v(TAG, "Emptying buffer to persistant database...");
	try {
	    Uri contentUri = Uri.parse("content://" + getString(R.string.local_storage_authority)
		    + DataPoint.CONTENT_URI_PATH + "?persist=true");
	    String where = DataPoint.TRANSMIT_STATE + "=" + 0;
	    storage.update(contentUri, new ContentValues(), where, null);
	} catch (IllegalArgumentException e) {
	    Log.e(TAG, "Error updating Local Storage!", e);
	}
    }

    private WakeLock getWakeLock() {
	if (null == wakeLock) {
	    PowerManager powerMgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
	    wakeLock = powerMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
	}
	return wakeLock;
    }

    /**
     * Handles an incoming Intent that started the service by checking if it wants to store a new
     * message or if it wants to send data to CommonSense.
     */
    private void handleIntent(Intent intent, int flags, int startId) {
	if (getString(R.string.action_sense_new_data).equals(intent.getAction())) {
	    handleNewMsgIntent(intent);
	} else if (getString(R.string.action_sense_send_data).equals(intent.getAction())) {
	    handleSendIntent(intent);
	} else {
	    Log.e(TAG, "Unexpected intent action: " + intent.getAction());
	}
    }

    @TargetApi(11)
    private void handleNewMsgIntent(Intent intent) {
	// Log.d(TAG, "handleNewMsgIntent");
	try {
	    DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
	    NumberFormat formatter = new DecimalFormat("##########.###", otherSymbols);

	    // get data point details from Intent
	    String sensorName = intent.getStringExtra(DataPoint.SENSOR_NAME);
	    String displayName = intent.getStringExtra(DataPoint.DISPLAY_NAME);
	    String description = intent.getStringExtra(DataPoint.SENSOR_DESCRIPTION);
	    String dataType = intent.getStringExtra(DataPoint.DATA_TYPE);
	    String deviceUuid = intent.getStringExtra(DataPoint.DEVICE_UUID);
	    long timestamp = intent.getLongExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime());
	    String timeInSecs = formatter.format(timestamp / 1000.0d);

	    // defaults
	    description = description != null ? description : sensorName;
	    displayName = displayName != null ? displayName : sensorName;
	    deviceUuid = deviceUuid != null ? deviceUuid : SenseApi
		    .getDefaultDeviceUuid(MsgHandler.this);

	    // Log.d(TAG, "name: '" + sensorName + "', display: '" + displayName +
	    // "', description: '" + description + "', data type: '" + dataType + "'");

	    // convert sensor value to String
	    String sensorValue = "";
	    if (dataType.equals(SenseDataTypes.BOOL)) {
		sensorValue += intent.getBooleanExtra(DataPoint.VALUE, false);
	    } else if (dataType.equals(SenseDataTypes.FLOAT)) {
		sensorValue += intent.getFloatExtra(DataPoint.VALUE, Float.MIN_VALUE);
	    } else if (dataType.equals(SenseDataTypes.INT)) {
		sensorValue += intent.getIntExtra(DataPoint.VALUE, Integer.MIN_VALUE);
	    } else if (dataType.equals(SenseDataTypes.JSON)
		    || dataType.equals(SenseDataTypes.JSON_TIME_SERIES)) {
		sensorValue += new JSONObject(intent.getStringExtra(DataPoint.VALUE)).toString();
	    } else if (dataType.equals(SenseDataTypes.STRING)
		    || dataType.equals(SenseDataTypes.FILE)) {
		sensorValue += intent.getStringExtra(DataPoint.VALUE);
	    }

	    // put the data point in the local storage
	    insertToLocalStorage(sensorName, displayName, description, dataType, deviceUuid,
		    timestamp, sensorValue);

	    // check if we can send the data point immediately
	    SharedPreferences mainPrefs;
	    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
		mainPrefs = getSharedPreferences(SensePrefs.MAIN_PREFS, MODE_MULTI_PROCESS);
	    } else {
		mainPrefs = getSharedPreferences(SensePrefs.MAIN_PREFS, MODE_PRIVATE);
	    }
	    int rate = Integer.parseInt(mainPrefs.getString(Main.SYNC_RATE, "0"));
	    boolean isMaxThreads = nrOfSendMessageThreads >= (MAX_NR_OF_SEND_MSG_THREADS - 5);
	    boolean isRealTimeMode = rate == -2;
	    if (isOnline() && isRealTimeMode && !isMaxThreads) {
		/* send immediately */

		// create sensor data JSON object with only 1 data point
		JSONObject sensorData = new JSONObject();
		JSONArray dataArray = new JSONArray();
		JSONObject data = new JSONObject();
		data.put("value", sensorValue);
		data.put("date", timeInSecs);
		dataArray.put(data);
		sensorData.put("data", dataArray);

		sendSensorData(sensorName, description, dataType, deviceUuid, sensorData);
	    }

	} catch (Exception e) {
	    Log.e(TAG, "Failed to handle new data point!", e);
	}
    }

    private void handleSendIntent(Intent intent) {
	// Log.d(TAG, "handleSendIntent");
	if (isOnline()) {
	    // get the cookie
	    SharedPreferences authPrefs;
	    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
		authPrefs = getSharedPreferences(SensePrefs.AUTH_PREFS, MODE_MULTI_PROCESS);
	    } else {
		authPrefs = getSharedPreferences(SensePrefs.AUTH_PREFS, MODE_PRIVATE);
	    }
	    String cookie = authPrefs.getString(Auth.LOGIN_COOKIE, null);

	    // prepare the data to give to the transmitters
	    Bundle msgData = new Bundle();
	    msgData.putString("cookie", cookie);

	    Message msg = Message.obtain();
	    msg.setData(msgData);
	    persistedDataTransmitter.sendMessage(msg);

	    msg = Message.obtain();
	    msg.setData(msgData);
	    recentDataTransmitter.sendMessage(msg);
	}
    }

    /**
     * Inserts a data point as new row in the local storage. Removal of old points is done
     * automatically.
     * 
     * @param name
     *            Sensor name.
     * @param displayName
     *            Sensor display name.
     * @param description
     *            Sensor description (previously 'device_type')
     * @param dataType
     *            Sensor data type
     * @param deviceUuid
     *            (Optional) UUID of the sensor's device. Set null to use the phone as the the
     *            default device
     * @param timestamp
     *            Data point time stamp
     * @param value
     *            Data point value
     */
    private void insertToLocalStorage(String name, String displayName, String description,
	    String dataType, String deviceUuid, long timestamp, String value) {

	// new value
	ContentValues values = new ContentValues();
	values.put(DataPoint.SENSOR_NAME, name);
	values.put(DataPoint.DISPLAY_NAME, displayName);
	values.put(DataPoint.SENSOR_DESCRIPTION, description);
	values.put(DataPoint.DATA_TYPE, dataType);
	values.put(DataPoint.DEVICE_UUID, deviceUuid);
	values.put(DataPoint.TIMESTAMP, timestamp);
	values.put(DataPoint.VALUE, value);
	values.put(DataPoint.TRANSMIT_STATE, 0);

	try {
	    Uri contentUri = Uri.parse("content://" + getString(R.string.local_storage_authority)
		    + DataPoint.CONTENT_URI_PATH);
	    storage.insert(contentUri, values);
	} catch (IllegalArgumentException e) {
	    Log.e(TAG, "Error inserting points in Local Storage!", e);
	}
    }

    /**
     * @return <code>true</code> if the phone has network connectivity.
     */
    private boolean isOnline() {
	SharedPreferences mainPrefs;
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
	    mainPrefs = getSharedPreferences(SensePrefs.MAIN_PREFS, MODE_MULTI_PROCESS);
	} else {
	    mainPrefs = getSharedPreferences(SensePrefs.MAIN_PREFS, MODE_PRIVATE);
	}
	boolean isCommonSenseEnabled = mainPrefs.getBoolean(Main.Advanced.USE_COMMONSENSE, true);

	SharedPreferences authPrefs;
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
	    authPrefs = getSharedPreferences(SensePrefs.AUTH_PREFS, MODE_MULTI_PROCESS);
	} else {
	    authPrefs = getSharedPreferences(SensePrefs.AUTH_PREFS, MODE_PRIVATE);
	}
	boolean isLoggedIn = authPrefs.getString(Auth.LOGIN_COOKIE, null) != null;

	final ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
	final NetworkInfo info = cm.getActiveNetworkInfo();
	return (null != info) && info.isConnected() && isCommonSenseEnabled && isLoggedIn;
    }

    @Override
    public IBinder onBind(Intent intent) {
	// you cannot bind to this service
	return null;
    }

    @Override
    public void onCreate() {
	Log.v(TAG, "onCreate");
	super.onCreate();

	storage = LocalStorage.getInstance(this);

	HandlerThread recentDataThread = new HandlerThread("TransmitRecentDataThread");
	recentDataThread.start();
	recentDataTransmitter = new RecentDataTransmitHandler(this, recentDataThread.getLooper());

	HandlerThread persistedDataThread = new HandlerThread("TransmitPersistedDataThread");
	persistedDataThread.start();
	persistedDataTransmitter = new PersistedDataTransmitHandler(this,
		persistedDataThread.getLooper());
    }

    @Override
    public void onDestroy() {
	Log.v(TAG, "onDestroy");
	emptyBufferToDb();

	// stop buffered data transmission threads
	persistedDataTransmitter.getLooper().quit();
	recentDataTransmitter.getLooper().quit();

	super.onDestroy();
    }

    /**
     * Deprecated method for starting the service, used in 1.6 and older.
     */
    @Override
    public void onStart(Intent intent, int startid) {
	handleIntent(intent, 0, startid);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
	handleIntent(intent, flags, startId);

	// this service is not sticky, it will get an intent to restart it if necessary
	return START_NOT_STICKY;
    }

    /**
     * Sends data points for one sensor to CommonSense.
     * 
     * @param name
     *            Sensor name, used to determine the sensor ID at CommonSense
     * @param description
     *            Sensor description (previously 'device_type'), used to determine the sensor ID at
     *            CommonSense
     * @param dataType
     *            Sensor data type, used to determine the sensor ID at CommonSense
     * @param deviceUuid
     *            (Optional) UUID of the sensor's device. Set null to use this phone as the default
     *            device.
     * @param sensorData
     *            JSON Object with the sensor data.
     */
    private void sendSensorData(String name, String description, String dataType,
	    String deviceUuid, JSONObject sensorData) {

	try {
	    if (nrOfSendMessageThreads < MAX_NR_OF_SEND_MSG_THREADS) {

		// get cookie for transmission
		SharedPreferences authPrefs;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
		    authPrefs = getSharedPreferences(SensePrefs.AUTH_PREFS, MODE_MULTI_PROCESS);
		} else {
		    authPrefs = getSharedPreferences(SensePrefs.AUTH_PREFS, MODE_PRIVATE);
		}
		String cookie = authPrefs.getString(Auth.LOGIN_COOKIE, null);

		if (cookie.length() > 0) {
		    ++nrOfSendMessageThreads;

		    // check for sending a file
		    if (dataType.equals(SenseDataTypes.FILE)) {
			// create handler thread and run task on there
			HandlerThread ht = new HandlerThread("sendFileThread");
			ht.start();
			new SendFileThread(cookie, sensorData, name, description, dataType,
				deviceUuid, this, ht.getLooper()).sendEmptyMessage(0);

		    } else {
			// create handler thread and run task on there
			HandlerThread ht = new HandlerThread("sendDataPointThread");
			ht.start();
			new SendDataThread(cookie, sensorData, name, description, dataType,
				deviceUuid, this, ht.getLooper()).sendEmptyMessage(0);

		    }
		} else {
		    Log.w(TAG, "Cannot send data point! no cookie");
		}
	    } else {
		Log.w(TAG, "Maximum number of sensor data transmission threads reached!");
	    }

	} catch (Exception e) {
	    Log.e(TAG, "Error in sending sensor data:", e);
	}
    }
}
