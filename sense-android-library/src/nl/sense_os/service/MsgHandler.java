/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import nl.sense_os.service.commonsense.DefaultSensorRegistrationService;
import nl.sense_os.service.commonsense.SenseApi;
import nl.sense_os.service.commonsense.senddata.BufferTransmitHandler;
import nl.sense_os.service.commonsense.senddata.DataTransmitHandler;
import nl.sense_os.service.commonsense.senddata.FileTransmitHandler;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Auth;
import nl.sense_os.service.constants.SensePrefs.Main;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.storage.LocalStorage;
import nl.sense_os.service.EncryptionHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

/**
 * This class is responsible for handling the sensor data that has been collected by the different
 * sensors. It has two main tasks:
 * <ul>
 * <li>Collect incoming sensor data and add it to the buffer.</li>
 * <li>Periodically transmit all sensor data in the buffer to CommonSense.</li>
 * </ul>
 * Sensors that have sampled a new data point should send it to the MsgHandler by sending an Intent
 * with {@link R.string#action_sense_new_data} that contains the details of the datapoint.<br/>
 * <br/>
 * For example:
 * 
 * <pre>
 * Intent sensorData = new Intent(getString(R.string.action_sense_new_data));
 * sensorData.putExtra(DataPoint.SENSOR_NAME, &quot;sensor name&quot;);
 * sensorData.putExtra(DataPoint.VALUE, &quot;foo&quot;);
 * sensorData.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.FLOAT);
 * sensorData.putExtra(DataPoint.TIMESTAMP, System.currentTimeMillis());
 * startService(sensorData);
 * </pre>
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class MsgHandler extends Service {

	private static final String TAG = "Sense MsgHandler";
    /**
     * Key for Intent extra that defines the buffer type to send data from. The value should be
     * either {@link #BUFFER_TYPE_FLASH} or {@link #BUFFER_TYPE_MEMORY}.
     */
    public static final String EXTRA_BUFFER_TYPE = "buffer-type";
    public static final int BUFFER_TYPE_FLASH = 1;
    public static final int BUFFER_TYPE_MEMORY = 0;

	private static FileTransmitHandler fileHandler;
	private static DataTransmitHandler dataTransmitHandler;
	private static BufferTransmitHandler bufferHandler;
	private static LocalStorage storage;

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
	public static void sendSensorData(Context context, String name, String description,
			String dataType, String deviceUuid, JSONObject sensorData) {

		try {
			// get cookie for transmission
			SharedPreferences authPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS,
					MODE_PRIVATE);
			String cookie = authPrefs.getString(Auth.LOGIN_COOKIE, null);

			SharedPreferences mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
					MODE_PRIVATE);

			boolean encrypt_credential = mainPrefs.getBoolean(Main.Advanced.ENCRYPT_CREDENTIAL, false);

			if (encrypt_credential) {
				EncryptionHelper decryptor = new EncryptionHelper(context);
                                try {
                                    cookie = decryptor.decrypt(cookie);
                                } catch (EncryptionHelper.EncryptionHelperException e) {
                                    Log.w(TAG, "Error decrypting cookie. Assume data is not encrypted");
                                }
			}

			if (cookie.length() > 0) {

				// prepare message to let handler run task
				Bundle args = new Bundle();
				args.putString("name", name);
				args.putString("description", description);
				args.putString("dataType", dataType);
				args.putString("deviceUuid", deviceUuid);
				args.putString("cookie", cookie);
				Message msg = Message.obtain();
				msg.setData(args);
				msg.obj = sensorData;

				// check for sending a file
				if (dataType.equals(SenseDataTypes.FILE)) {
					fileHandler.sendMessage(msg);
				} else {
					dataTransmitHandler.sendMessage(msg);
				}
			} else {
				Log.w(TAG, "Cannot send data point! no cookie");
			}

		} catch (Exception e) {
			Log.e(TAG, "Error in sending sensor data:", e);
		}
	}

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
			deviceUuid = deviceUuid != null ? deviceUuid : SenseApi.getDefaultDeviceUuid(this);

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
                try {
                    sensorValue += new JSONObject(intent.getStringExtra(DataPoint.VALUE))
                            .toString();
                } catch (JSONException e) {
                    // assume value is an array
                    sensorValue += new JSONArray(intent.getStringExtra(DataPoint.VALUE)).toString();
                }
			} else if (dataType.equals(SenseDataTypes.STRING)
					|| dataType.equals(SenseDataTypes.FILE)) {
				sensorValue += intent.getStringExtra(DataPoint.VALUE);
			}

			// put the data point in the local storage
			insertToLocalStorage(sensorName, displayName, description, dataType, deviceUuid,
					timestamp, sensorValue);

			/*
			 * check if we can send the data point immediately
			 */
			SharedPreferences mainPrefs = getSharedPreferences(SensePrefs.MAIN_PREFS, MODE_PRIVATE);
			int rate = Integer.parseInt(mainPrefs.getString(Main.SYNC_RATE, "0"));
			boolean isRealTimeMode = rate == -2;
			if (isOnline() && isRealTimeMode) {

				// create sensor data JSON object with only 1 data point
				JSONObject sensorData = new JSONObject();
				JSONArray dataArray = new JSONArray();
				JSONObject data = new JSONObject();
				data.put("value", sensorValue);
				data.put("date", timeInSecs);
				dataArray.put(data);
				sensorData.put("data", dataArray);
				
				sendSensorData(this, sensorName, description, dataType, deviceUuid, sensorData);
			}

		} catch (Exception e) {
			Log.e(TAG, "Failed to handle new data point!", e);
		}
		
		//broadcast this new data intent as well, so anybody can listen to data!
		this.sendBroadcast(intent);
	}

	private void handleSendIntent(Intent intent) {
		//Remove this before release
		Log.d("SendData", "The intent is received at handleSendIntent :" +intent.getAction());
		
		// Log.d(TAG, "handleSendIntent");
		if (isOnline()) {

			// verify the sensor IDs
			startService(new Intent(this, DefaultSensorRegistrationService.class));

			// get the cookie
			SharedPreferences authPrefs = getSharedPreferences(SensePrefs.AUTH_PREFS, MODE_PRIVATE);
			String cookie = authPrefs.getString(Auth.LOGIN_COOKIE, null);

			SharedPreferences mainPrefs = getSharedPreferences(SensePrefs.MAIN_PREFS, MODE_PRIVATE);
			boolean encrypt_credential = mainPrefs.getBoolean(Main.Advanced.ENCRYPT_CREDENTIAL, false);

			if (encrypt_credential) {
				EncryptionHelper decryptor = new EncryptionHelper(this);
                                try {
                                    cookie = decryptor.decrypt(cookie);
                                } catch (EncryptionHelper.EncryptionHelperException e) {
                                    Log.w(TAG, "Error decrypting cookie. Assume data is not encrypted");
                                }
			}


            // send the message to the handler
            Message msg = Message.obtain();
            Bundle args = new Bundle();
            args.putString("cookie", cookie);
            msg.setData(args);
            //Remove this before release
    		Log.d("SendData", "The message is being sent to bufferTransmitter");
            bufferHandler.sendMessage(msg);
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

		{
			HandlerThread handlerThread = new HandlerThread("TransmitRecentDataThread");
			handlerThread.start();
            bufferHandler = new BufferTransmitHandler(this, storage, handlerThread.getLooper());
		}

		{
			HandlerThread handlerThread = new HandlerThread("TransmitFileThread");
			handlerThread.start();
			fileHandler = new FileTransmitHandler(this, storage, handlerThread.getLooper());
		}

		{
			HandlerThread handlerThread = new HandlerThread("TransmitDataPointThread");
			handlerThread.start();
			dataTransmitHandler = new DataTransmitHandler(this, storage, handlerThread.getLooper());
		}
	}

	@Override
	public void onDestroy() {
		Log.v(TAG, "onDestroy");
		emptyBufferToDb();

		// stop buffered data transmission threads
		bufferHandler.getLooper().quit();
		fileHandler.getLooper().quit();
		dataTransmitHandler.getLooper().quit();

		super.onDestroy();
	}

	/**
	 * Handles an incoming Intent that started the service by checking if it wants to store a new
	 * message or if it wants to send data to CommonSense.
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		if (getString(R.string.action_sense_new_data).equals(intent.getAction())) {
			handleNewMsgIntent(intent);
		} else if (getString(R.string.action_sense_send_data).equals(intent.getAction())) {
			handleSendIntent(intent);
		} else {
			Log.e(TAG, "Unexpected intent action: " + intent.getAction());
		}

		// this service is not sticky, it will get an intent to restart it if necessary
		return START_NOT_STICKY;
	}
}
