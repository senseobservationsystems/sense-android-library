package nl.sense_os.service.commonsense.senddata;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

/**
 * Handler for tasks that send buffered sensor data to CommonSense. Nota that this handler is
 * re-usable: every time the handler receives a message, it gets the latest data in a Cursor and
 * sends it to CommonSense.<br>
 * <br>
 * Subclasses have to implement {@link #getUnsentData()} and {@link #onTransmitSuccess(JSONObject)}
 * to make them work with their intended data source.
 */
public abstract class BufferTransmitHandler extends Handler {

	private static final String TAG = "BatchDataTransmitHandler";
	private static final int MAX_POST_DATA = 100;
	final WeakReference<Context> ctxRef;
	final WeakReference<LocalStorage> storageRef;
	private final String url;
	private final DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ENGLISH);
	private final NumberFormat dateFormatter = new DecimalFormat("##########.###", symbols);

	public BufferTransmitHandler(Context context, LocalStorage storage, Looper looper) {
		super(looper);
		this.ctxRef = new WeakReference<Context>(context);
		this.storageRef = new WeakReference<LocalStorage>(storage);

		SharedPreferences mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
				Context.MODE_PRIVATE);
		boolean devMode = mainPrefs.getBoolean(Main.Advanced.DEV_MODE, false);
		url = devMode ? SenseUrls.DEV_SENSOR_DATA.replace("/<id>/", "/") : SenseUrls.SENSOR_DATA
				.replace("/<id>/", "/");
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

	/**
	 * @return Cursor with the data points that have to be sent to CommonSense.
	 */
	protected abstract Cursor getUnsentData();

	@Override
	public void handleMessage(Message msg) {

		String cookie = (String) msg.obj;

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
			if ((null != cursor) && cursor.moveToFirst()) {
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
	 * @param transmission
	 *            The JSON Object that was sent to CommonSense. Contains all the data points that
	 *            were transmitted.
	 * @throws Exception
	 */
	protected abstract void onTransmitSuccess(String cookie, JSONObject transmission)
			throws JSONException;

	/**
	 * POSTs the sensor data points to the main sensor data URL at CommonSense.
	 * 
	 * @param cookie
	 * 
	 * @param transmission
	 *            JSON Object with data points for transmission
	 * @throws JSONException
	 * @throws MalformedURLException
	 */
	private void postData(String cookie, JSONObject transmission) throws JSONException,
			MalformedURLException {

		Map<String, String> response = null;
		try {
			response = SenseApi.request(ctxRef.get(), url, transmission, cookie);
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
				final Intent serviceIntent = new Intent(ctxRef.get().getString(
						R.string.action_sense_service));
				serviceIntent.putExtra(SenseService.EXTRA_RELOGIN, true);
				ctxRef.get().startService(serviceIntent);
			}

			// Show the HTTP response Code
			Log.w(TAG, "Failed to send buffered data points: " + statusCode
					+ ", Response content: '" + response.get("content") + "'\n"
					+ "Data will be retried later");
			Log.d(TAG, "transmission: '" + transmission + "'");

		} else {
			// Data sent successfully
			onTransmitSuccess(cookie, transmission);
		}
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
					name = cursor.getString(cursor.getColumnIndexOrThrow(DataPoint.SENSOR_NAME));
					description = cursor.getString(cursor
							.getColumnIndexOrThrow(DataPoint.SENSOR_DESCRIPTION));
					dataType = cursor.getString(cursor.getColumnIndexOrThrow(DataPoint.DATA_TYPE));
					value = cursor.getString(cursor.getColumnIndexOrThrow(DataPoint.VALUE));
					timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(DataPoint.TIMESTAMP));
					deviceUuid = cursor.getString(cursor
							.getColumnIndexOrThrow(DataPoint.DEVICE_UUID));

					// set default sensor ID if it is missing
					deviceUuid = deviceUuid != null ? deviceUuid : SenseApi
							.getDefaultDeviceUuid(ctxRef.get());

				} catch (IllegalArgumentException e) {
					// something is wrong with this data point, skip it
					Log.w(TAG,
							"Exception getting data point details from cursor: '" + e.getMessage()
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
					JSONObject sensorEntry = sensorDataMap.get(key);
					JSONArray data = null;
					if (sensorEntry == null) {
						sensorEntry = new JSONObject();
						String id = SenseApi.getSensorId(ctxRef.get(), name, description, dataType,
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
					/*
					 * if the data type is a "file", we need special handling
					 */

					// create sensor data JSON object with only 1 data point
					JSONObject sensorData = new JSONObject();
					JSONArray dataArray = new JSONArray();
					JSONObject data = new JSONObject();
					data.put("value", value);
					data.put("date", dateFormatter.format(timestamp / 1000d));
					dataArray.put(data);
					sensorData.put("data", dataArray);

					MsgHandler.sendSensorData(ctxRef.get(), name, description, dataType,
							deviceUuid, sensorData);
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
			postData(cookie, transmission);
		}
	}
}
