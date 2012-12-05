/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.commonsense;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import nl.sense_os.service.ISenseService;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Auth;
import nl.sense_os.service.constants.SensePrefs.Main.Advanced;
import nl.sense_os.service.constants.SenseUrls;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.push.GCMReceiver;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SenseApi {

	/**
	 * Application secrets that give an app access to the current CommonSense session ID.
	 * 
	 * @see ISenseService#getSessionId(String)
	 */
	private static class AppSecrets {
		static final String SENSE = "]C@+[G1be,f)@3mz|2cj4gq~Jz(8WE&_$7g:,-KOI;v:iQt<r;1OQ@=mr}jmE8>!";
		static final String ASK = "3$2Sp16096H*Rg!n*<G<411&8QlMvg!pMyN]q?m[5c|<N+$=/~Su{quv$/j5s`+6";
		static final String RDAM_CS = "0$HTLi8e_}9^s7r#[_L~-ndz=t5z)e}I-ai#L22-?0+i7jfF2,~)oyi|H)q*GL$Y";
	}

	private static final String TAG = "SenseApi";
	private static final long CACHE_REFRESH = 1000l * 60 * 60; // 1 hour
	private static SharedPreferences mainPrefs;
	private static SharedPreferences authPrefs;
	private static TelephonyManager telManager;

	/**
	 * Gets a list of all registered sensors for a user at the CommonSense API. Uses caching for
	 * increased performance.
	 * 
	 * @param context
	 *            Application context, used for getting preferences.
	 * @return The list of sensors
	 * @throws IOException
	 *             In case of communication failure to CommonSense
	 * @throws JSONException
	 *             In case of unparseable response from CommonSense
	 */
	public static JSONArray getAllSensors(Context context) throws IOException, JSONException {

		if (null == authPrefs) {
			authPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS, Context.MODE_PRIVATE);
		}

		// try to get list of sensors from the cache
		try {
			String cachedSensors = authPrefs.getString(Auth.SENSOR_LIST_COMPLETE, null);
			long cacheTime = authPrefs.getLong(Auth.SENSOR_LIST_COMPLETE_TIME, 0);
			boolean isOutdated = System.currentTimeMillis() - cacheTime > CACHE_REFRESH;

			// return cached list of it is still valid
			if (false == isOutdated && null != cachedSensors) {
				return new JSONArray(cachedSensors);
			}

		} catch (Exception e) {
			// unlikely to ever happen. Just get the list from CommonSense instead
			Log.e(TAG, "Failed to get list of sensors from cache!", e);
		}

		// if we make it here, the list was not in the cache
		Log.v(TAG, "List of sensor IDs is missing or outdated, refreshing...");

		// request fresh list of sensors for this device from CommonSense
		String cookie = authPrefs.getString(Auth.LOGIN_COOKIE, null);
		if (null == mainPrefs) {
			mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
		}
		boolean devMode = mainPrefs.getBoolean(Advanced.DEV_MODE, false);
		if (devMode) {
			Log.i(TAG, "Using development server to get registered sensors");
		}
		String url = devMode ? SenseUrls.DEV_ALL_SENSORS : SenseUrls.ALL_SENSORS;
		Map<String, String> response = SenseApi.request(context, url, null, cookie);

		String responseCode = response.get("http response code");
		if (!"200".equals(responseCode)) {
			Log.w(TAG, "Failed to get list of sensors! Response code: " + responseCode);
			throw new IOException("Incorrect response from CommonSense: " + responseCode);
		}

		// parse response and store the list
		JSONObject content = new JSONObject(response.get("content"));
		JSONArray sensorList = content.getJSONArray("sensors");

		// store the new sensor list
		Editor authEditor = authPrefs.edit();
		authEditor.putString(Auth.SENSOR_LIST_COMPLETE, sensorList.toString());
		authEditor.putLong(Auth.SENSOR_LIST_COMPLETE_TIME, System.currentTimeMillis());
		authEditor.commit();

		return sensorList;
	}

	/**
	 * @param context
	 *            Context for accessing phone details
	 * @return The default device type, i.e. the phone's model String
	 */
	public static String getDefaultDeviceType(Context context) {
		if (null == authPrefs) {
			authPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS, Context.MODE_PRIVATE);
		}
		return authPrefs.getString(Auth.DEVICE_TYPE, Build.MODEL);
	}

	/**
	 * @param context
	 *            Context for accessing phone details
	 * @return The default device UUID, e.g. the phone's IMEI String
	 */
	@TargetApi(9)
	public static String getDefaultDeviceUuid(Context context) {
		if (null == telManager) {
			telManager = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE));
		}
		String uuid = telManager.getDeviceId();
		if (null == uuid) {
			// device has no IMEI, try using the Android serial code
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
				uuid = Build.SERIAL;
			} else {
				Log.w(TAG, "Cannot get reliable device UUID!");
			}
		}
		return uuid;
	}

	private static List<JSONObject> getMatchingSensors(Context context, String name,
			String description, String dataType) throws IOException, JSONException {

		// get list of all registered sensors for this device
		JSONArray sensors = getAllSensors(context);

		// check sensors with similar names and descriptions in the list
		List<JSONObject> result = new ArrayList<JSONObject>();
		JSONObject sensor;
		for (int i = 0; i < sensors.length(); i++) {
			sensor = (JSONObject) sensors.get(i);

			if (sensor.getString("name").equalsIgnoreCase(name)
					&& ((null == description) || sensor.getString("device_type").equalsIgnoreCase(
							description))
					&& ((null == dataType) || sensor.getString("data_type").equalsIgnoreCase(
							dataType))) {
				result.add(sensor);

			} else if (name.equals(SensorNames.ACCELEROMETER) || name.equals(SensorNames.ORIENT)
					|| name.equals(SensorNames.GYRO) || name.equals(SensorNames.LIN_ACCELERATION)
					|| name.equals(SensorNames.MAGNETIC_FIELD) || name.equals(SensorNames.ACCELEROMETER_EPI)) {
				// special case to take care of changed motion sensor descriptions since Gingerbread
				if (name.equals(sensor.getString("name"))) {
					// Log.d(TAG, "Using inexact match for '" + name + "' sensor ID...");
					result.add(sensor);
				}
			}
		}

		return result;
	}

	/**
	 * Gets the sensor ID at CommonSense , which can be used to modify the sensor information and
	 * data.
	 * 
	 * @param context
	 *            Context for getting preferences
	 * @param name
	 *            Sensor name, to match with registered sensors.
	 * @param description
	 *            Sensor description (previously 'device_type'), to match with registered sensors.
	 * @param dataType
	 *            Sensor data type, to match with registered sensors.
	 * @param deviceUuid
	 *            (Optional) UUID of the device that should hold the sensor.
	 * @return String with the sensor's ID, or null if the sensor does not exist at CommonSense
	 *         (yet).
	 * @throws IOException
	 *             If the request to CommonSense failed.
	 * @throws JSONException
	 *             If the response from CommonSense could not be parsed.
	 */
	public static String getSensorId(Context context, String name, String description,
			String dataType, String deviceUuid) throws IOException, JSONException {

		// Log.d(TAG, "Get sensor ID. name: '" + name + "', deviceUuid: '" + deviceUuid + "'");

		// get list of sensors with matching description
		List<JSONObject> sensors = getMatchingSensors(context, name, description, dataType);

		// check the devices that the sensors are connected to
		String id = null;
		JSONObject device;
		for (JSONObject sensor : sensors) {
			if (null != deviceUuid) {
				// check if the device UUID matches
				device = sensor.optJSONObject("device");
				if (null != device && deviceUuid.equals(device.optString("uuid"))) {
					id = sensor.getString("id");
					break;
				}
			} else {
				// we do not care about the device, just accept the first match we find
				id = sensor.getString("id");
				break;
			}
		}
		return id;
	}

	/**
	 * Gets the sensors that are connected to another sensor. Typically used
	 * 
	 * @param context
	 * @param sensorId
	 * @return List of IDs for connected sensors
	 * @throws IOException
	 * @throws JSONException
	 */
	public static List<String> getConnectedSensors(Context context, String sensorId)
			throws IOException, JSONException {

		if (null == authPrefs) {
			authPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS, Context.MODE_PRIVATE);
		}
		if (null == mainPrefs) {
			mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
		}

		// request fresh list of sensors for this device from CommonSense
		String cookie = authPrefs.getString(Auth.LOGIN_COOKIE, null);
		boolean devMode = mainPrefs.getBoolean(Advanced.DEV_MODE, false);
		if (devMode) {
			Log.i(TAG, "Using development server to get connected sensors");
		}
		String url = devMode ? SenseUrls.DEV_CONNECTED_SENSORS : SenseUrls.CONNECTED_SENSORS;
		url = url.replace("<id>", sensorId);
		Map<String, String> response = SenseApi.request(context, url, null, cookie);

		String responseCode = response.get("http response code");
		if (!"200".equals(responseCode)) {
			Log.w(TAG, "Failed to get list of connected sensors! Response code: " + responseCode);
			throw new IOException("Incorrect response from CommonSense: " + responseCode);
		}

		// parse response and store the list
		JSONObject content = new JSONObject(response.get("content"));
		JSONArray sensorList = content.getJSONArray("sensors");

		List<String> result = new ArrayList<String>();
		for (int i = 0; i < sensorList.length(); i++) {
			JSONObject sensor = sensorList.getJSONObject(0);
			result.add(sensor.getString("id"));
		}
		return result;
	}

	/**
	 * Get this device_id registered in common sense
	 * 
	 * @param context
	 * @throws IOException
	 * @throws JSONException
	 */
	public static String getDeviceId(Context context) throws IOException, JSONException {
		String device_id = null;

		if (null == authPrefs) {
			authPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS, Context.MODE_PRIVATE);
		}
		if (null == mainPrefs) {
			mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
		}

		String cookie = authPrefs.getString(Auth.LOGIN_COOKIE, null);
		boolean devMode = mainPrefs.getBoolean(Advanced.DEV_MODE, false);
		String url = devMode ? SenseUrls.DEV_DEVICES : SenseUrls.DEVICES;

		Map<String, String> response = SenseApi.request(context, url, null, cookie);
		String responseCode = response.get("http response code");
		if (!"200".equals(responseCode)) {
			Log.w(TAG, "Failed to get devices data: " + responseCode);
			throw new IOException("Incorrect response from CommonSense: " + responseCode);
		}

		// check registration result
		JSONObject res = new JSONObject(response.get("content"));
		JSONArray arr = res.getJSONArray("devices");

		String deviceType = getDefaultDeviceType(context);
		String deviceUUID = getDefaultDeviceUuid(context);

		for (int i = 0; i < arr.length(); i++) {
			JSONObject row = arr.getJSONObject(i);
			String cid = row.getString("id");
			String ctype = row.getString("type");
			String cuuid = row.getString("uuid");
			if (deviceType.equals(ctype) && deviceUUID.equals(cuuid)) {
				device_id = cid;
				break;
			}
		}

		return device_id;
	}

	/**
	 * Gets the URL at CommonSense to which the data must be sent.
	 * 
	 * @param context
	 *            Context for getting preferences
	 * @param name
	 *            Sensor name, to match with registered sensors.
	 * @param description
	 *            Sensor description (previously 'device_type'), to match with registered sensors.
	 * @param dataType
	 *            Sensor data type, to match with registered sensors.
	 * @param deviceUuid
	 *            (Optional) UUID of the device that holds the sensor. Set null to use the default
	 *            device.
	 * @return String with the sensor's URL, or null if sensor does not have an ID (yet)
	 * @throws JSONException
	 *             If there was unexpected response getting the sensor ID.
	 * @throws IOException
	 *             If there was a problem during communication with CommonSense.
	 */
	public static String getSensorUrl(Context context, String name, String description,
			String dataType, String deviceUuid) throws IOException, JSONException {

		String id = getSensorId(context, name, description, dataType, deviceUuid);

		if (id == null) {
			Log.e(TAG, "Failed to get URL for sensor '" + name + "': sensor ID is not available");
			return null;
		}

		if (null == mainPrefs) {
			mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
		}
		boolean devMode = mainPrefs.getBoolean(Advanced.DEV_MODE, false);

		// found the right sensor
		if (dataType.equals(SenseDataTypes.FILE)) {
			String url = devMode ? SenseUrls.DEV_SENSOR_FILE : SenseUrls.SENSOR_FILE;
			return url.replaceFirst("<id>", id);
		} else {
			String url = devMode ? SenseUrls.DEV_SENSOR_DATA : SenseUrls.SENSOR_DATA;
			return url.replaceFirst("<id>", id);
		}
	}

	/**
	 * @param context
	 *            Context for getting preferences
	 * @param appId
	 *            Identifier for the app that requests the session ID
	 * @return The current CommonSense session ID
	 * @throws IllegalAccessException
	 *             if the app ID is not valid
	 */
	public static String getSessionId(Context context, String appId) throws IllegalAccessException {
		if (null == mainPrefs) {
			mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
		}
		if (AppSecrets.ASK.equals(appId)) {
			// Log.v(TAG, "An ASK app accessed the CommonSense session ID");
			return mainPrefs.getString(Auth.LOGIN_COOKIE, null);
		} else if (AppSecrets.RDAM_CS.equals(appId)) {
			// Log.v(TAG, "A Rotterdam CS app accessed the CommonSense session ID");
			return mainPrefs.getString(Auth.LOGIN_COOKIE, null);
		} else if (AppSecrets.SENSE.equals(appId)) {
			// Log.v(TAG, "A Sense app accessed the CommonSense session ID");
			return mainPrefs.getString(Auth.LOGIN_COOKIE, null);
		} else {
			Log.e(TAG, "App is not allowed access to the CommonSense session!");
			throw new IllegalAccessException(
					"App is not allowed access to the CommonSense session!");
		}
	}

	/**
	 * @param hashMe
	 *            "clear" password String to be hashed before sending it to CommonSense
	 * @return Hashed String
	 */
	public static String hashPassword(String hashMe) {
		final byte[] unhashedBytes = hashMe.getBytes();
		try {
			final MessageDigest algorithm = MessageDigest.getInstance("MD5");
			algorithm.reset();
			algorithm.update(unhashedBytes);
			final byte[] hashedBytes = algorithm.digest();

			final StringBuffer hexString = new StringBuffer();
			for (final byte element : hashedBytes) {
				final String hex = Integer.toHexString(0xFF & element);
				if (hex.length() == 1) {
					hexString.append(0);
				}
				hexString.append(hex);
			}
			return hexString.toString();
		} catch (final NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Tries to log in at CommonSense using the supplied username and password. After login, the
	 * cookie containing the session ID is stored in the preferences.
	 * 
	 * @param context
	 *            Context for getting preferences
	 * @param username
	 *            Username for authentication
	 * @param password
	 *            Hashed password for authentication
	 * @return 0 if login completed successfully, -2 if login was forbidden, and -1 for any other
	 *         errors.
	 * @throws JSONException
	 *             In case of unparseable response from CommonSense
	 * @throws IOException
	 *             In case of communication failure to CommonSense
	 */
	public static int login(Context context, String username, String password)
			throws JSONException, IOException {

		// preferences
		if (null == authPrefs) {
			authPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS, Context.MODE_PRIVATE);
		}
		if (null == mainPrefs) {
			mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
		}

		boolean devMode = mainPrefs.getBoolean(Advanced.DEV_MODE, false);
		if (devMode) {
			Log.i(TAG, "Using development server to log in");
		}
		final String url = devMode ? SenseUrls.DEV_LOGIN : SenseUrls.LOGIN;
		final JSONObject user = new JSONObject();
		user.put("username", username);
		user.put("password", password);

		// perform actual request
		Map<String, String> response = request(context, url, user, null);

		// if response code is not 200 (OK), the login was incorrect
		String responseCode = response.get("http response code");
		int result = -1;
		if ("403".equalsIgnoreCase(responseCode)) {
			Log.w(TAG, "CommonSense login refused! Response: forbidden!");
			result = -2;
		} else if (!"200".equalsIgnoreCase(responseCode)) {
			Log.w(TAG, "CommonSense login failed! Response: " + responseCode);
			result = -1;
		} else {
			// received 200 response
			result = 0;
		}

		// get the cookie from the response
		String cookie = response.get("set-cookie");
		if (result == 0 && response.get("set-cookie") == null) {
			// something went horribly wrong
			Log.w(TAG, "CommonSense login failed: no cookie received?!");
			result = -1;
		}

		// handle result
		Editor authEditor = authPrefs.edit();
		switch (result) {
		case 0: // logged in
			authEditor.putString(Auth.LOGIN_COOKIE, cookie);
			authEditor.commit();
			break;
		case -1: // error
			break;
		case -2: // unauthorized
			authEditor.remove(Auth.LOGIN_COOKIE);
			authEditor.commit();
			break;
		default:
			Log.e(TAG, "Unexpected login result: " + result);
		}

		return result;
	}

	/**
	 * Registers a new sensor for this device at CommonSense. Also connects the sensor to this
	 * device.
	 * 
	 * @param context
	 *            The application context, used to retrieve preferences.
	 * @param name
	 *            The name of the sensor.
	 * @param displayName
	 *            The sensor's pretty display name.
	 * @param description
	 *            The sensor description (previously "device_type").
	 * @param dataType
	 *            The sensor data type.
	 * @param value
	 *            An example sensor value, used to determine the data structure for JSON type
	 *            sensors.
	 * @param deviceType
	 *            (Optional) Type of the device that holds the sensor. Set null to use the default
	 *            device.
	 * @param deviceUuid
	 *            (Optional) UUID of the device that holds the sensor. Set null to use the default
	 *            device.
	 * @return The new sensor ID at CommonSense, or <code>null</code> if the registration failed.
	 * @throws JSONException
	 *             In case of invalid sensor details or if the request returned unparseable
	 *             response.
	 * @throws IOException
	 *             In case of communication failure during creation of the sensor.
	 */
	public static String registerSensor(Context context, String name, String displayName,
			String description, String dataType, String value, String deviceType, String deviceUuid)
			throws JSONException, IOException {

		if (null == authPrefs) {
			authPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS, Context.MODE_PRIVATE);
		}
		if (null == mainPrefs) {
			mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
		}

		String cookie = authPrefs.getString(Auth.LOGIN_COOKIE, null);
		boolean devMode = mainPrefs.getBoolean(Advanced.DEV_MODE, false);

		// prepare request to create new sensor
		String url = devMode ? SenseUrls.DEV_CREATE_SENSOR : SenseUrls.CREATE_SENSOR;
		JSONObject postData = new JSONObject();
		JSONObject sensor = new JSONObject();
		sensor.put("name", name);
		sensor.put("device_type", description);
		sensor.put("display_name", displayName);
		sensor.put("pager_type", "");
		sensor.put("data_type", dataType);
		if (dataType.compareToIgnoreCase("json") == 0) {
			JSONObject dataStructJSon = new JSONObject(value);
			JSONArray fieldNames = dataStructJSon.names();
			for (int x = 0; x < fieldNames.length(); x++) {
				String fieldName = fieldNames.getString(x);
				int start = dataStructJSon.get(fieldName).getClass().getName().lastIndexOf(".");
				dataStructJSon.put(fieldName, dataStructJSon.get(fieldName).getClass().getName()
						.substring(start + 1));
			}
			sensor.put("data_structure", dataStructJSon.toString().replaceAll("\"", "\\\""));
		}
		postData.put("sensor", sensor);

		// perform actual request
		Map<String, String> response = request(context, url, postData, cookie);

		// check response code
		String code = response.get("http response code");
		if (!"201".equals(code)) {
			Log.e(TAG, "Failed to register sensor at CommonSense! Response code: " + code);
			throw new IOException("Incorrect response from CommonSense: " + code);
		}

		// retrieve the newly created sensor ID
		String content = response.get("content");
		JSONObject responseJson = new JSONObject(content);
		JSONObject jsonSensor = responseJson.getJSONObject("sensor");
		final String id = jsonSensor.getString("id");

		// get device properties from preferences, so it matches the properties in CommonSense
		if (null == deviceUuid) {
			deviceUuid = getDefaultDeviceUuid(context);
			deviceType = getDefaultDeviceType(context);
		}

		// Add sensor to this device at CommonSense
		url = devMode ? SenseUrls.DEV_ADD_SENSOR_TO_DEVICE : SenseUrls.ADD_SENSOR_TO_DEVICE;
		url = url.replaceFirst("<id>", id);
		postData = new JSONObject();
		JSONObject device = new JSONObject();
		device.put("type", deviceType);
		device.put("uuid", deviceUuid);
		postData.put("device", device);

		response = request(context, url, postData, cookie);

		// check response code
		code = response.get("http response code");
		if (!"201".equals(code)) {
			Log.e(TAG, "Failed to add sensor to device at CommonSense! Response code: " + code);
			throw new IOException("Incorrect response from CommonSense: " + code);
		}

		// store the new sensor in the preferences
		jsonSensor.put("device", device);
		JSONArray sensors = getAllSensors(context);
		sensors.put(jsonSensor);
		Editor authEditor = authPrefs.edit();
		authEditor.putString(Auth.SENSOR_LIST_COMPLETE, sensors.toString());
		authEditor.commit();

		Log.d(TAG, "Created sensor: '" + name + "' for device: '" + deviceType + "'");

		// return the new sensor ID
		return id;
	}

	/**
	 * Tries to register a new user at CommonSense. Discards private data of any previous users.
	 * 
	 * @param context
	 *            Context for getting preferences
	 * @param username
	 *            Username to register
	 * @param password
	 *            Hashed password for the new user
	 * @return 0 if registration completed successfully, -2 if the user already exists, and -1 for
	 *         any other unexpected responses.
	 * @throws JSONException
	 *             In case of unparseable response from CommonSense
	 * @throws IOException
	 *             In case of communication failure to CommonSense
	 */
	public static int registerUser(Context context, String username, String password, String name,
			String surname, String email, String mobile) throws JSONException, IOException {

		if (null == mainPrefs) {
			mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
		}
		boolean devMode = mainPrefs.getBoolean(Advanced.DEV_MODE, false);

		final String url = devMode ? SenseUrls.DEV_REG : SenseUrls.REG;

		// create JSON object to POST
		final JSONObject data = new JSONObject();
		final JSONObject user = new JSONObject();
		user.put("username", username);
		user.put("password", password);
		if (null != name) {
			user.put("name", name);
		}
		if (null != surname) {
			user.put("surname", surname);
		}
		if (null != email) {
			user.put("email", email);
		}
		if (null != mobile) {
			user.put("mobile", mobile);
		}
		data.put("user", user);

		// perform actual request
		Map<String, String> response = SenseApi.request(context, url, data, null);

		String responseCode = response.get("http response code");
		int result = -1;
		if ("201".equalsIgnoreCase(responseCode)) {
			result = 0;
		} else if ("409".equalsIgnoreCase(responseCode)) {
			Log.e(TAG, "Error registering new user! User already exists");
			result = -2;
		} else {
			Log.e(TAG, "Error registering new user! Response code: " + responseCode);
			result = -1;
		}

		return result;
	}

	/**
	 * Performs request at CommonSense API. Returns the response code, content, and headers.
	 * 
	 * @param context
	 *            Application context, used to read preferences.
	 * @param urlString
	 *            Complete URL to perform request to.
	 * @param content
	 *            (Optional) Content for the request. If the content is not null, the request method
	 *            is automatically POST. The default method is GET.
	 * @param cookie
	 *            (Optional) Cookie header for the request.
	 * @return Map with "content" and "http response code" fields, plus fields for all response
	 *         headers.
	 * @throws IOException
	 */
	public static Map<String, String> request(Context context, String urlString,
			JSONObject content, String cookie) throws IOException {

		HttpURLConnection urlConnection = null;
		HashMap<String, String> result = new HashMap<String, String>();
		try {
			// Log.d(TAG, "API request: " + (content == null ? "GET" : "POST") + " " + urlString
			// + " cookie:" + cookie);

			// get compression preference
			if (null == mainPrefs) {
				mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
						Context.MODE_PRIVATE);
			}
			final boolean compress = mainPrefs.getBoolean(Advanced.COMPRESS, true);

			// open new URL connection channel.
			URL url = new URL(urlString);
			if ("https".equals(url.getProtocol().toLowerCase())) {
				trustAllHosts();
				HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
				https.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
				urlConnection = https;
			} else {
				urlConnection = (HttpURLConnection) url.openConnection();
			}

			// some parameters
			urlConnection.setUseCaches(false);
			urlConnection.setInstanceFollowRedirects(false);

			// set cookie (if available)
			if (null != cookie) {
				urlConnection.setRequestProperty("Cookie", cookie);
			}

			// send content (if available)
			if (null != content) {
				urlConnection.setDoOutput(true);
				urlConnection.setRequestProperty("Content-Type", "application/json");

				// send content
				DataOutputStream printout;
				if (compress) {
					// do not set content size
					urlConnection.setRequestProperty("Transfer-Encoding", "chunked");
					urlConnection.setRequestProperty("Content-Encoding", "gzip");
					GZIPOutputStream zipStream = new GZIPOutputStream(
							urlConnection.getOutputStream());
					printout = new DataOutputStream(zipStream);
				} else {
					// set content size
					urlConnection.setFixedLengthStreamingMode(content.toString().length());
					urlConnection.setRequestProperty("Content-Length", ""
							+ content.toString().length());
					printout = new DataOutputStream(urlConnection.getOutputStream());
				}
				printout.writeBytes(content.toString());
				printout.flush();
				printout.close();
			}

			// get response, or read error message
			InputStream inputStream;
			try {
				inputStream = urlConnection.getInputStream();
			} catch (IOException e) {
				inputStream = urlConnection.getErrorStream();
			}
			if (null == inputStream) {
				throw new IOException("could not get InputStream");
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream), 1024);
			String line;
			StringBuffer responseContent = new StringBuffer();
			while ((line = reader.readLine()) != null) {
				responseContent.append(line);
				responseContent.append('\r');
			}
			result.put("content", responseContent.toString());
			result.put("http response code", "" + urlConnection.getResponseCode());

			// clean up
			reader.close();
			reader = null;
			inputStream.close();
			inputStream = null;

			// get headers
			Map<String, List<String>> headerFields = urlConnection.getHeaderFields();
			String key, valueString;
			List<String> value;
			for (Entry<String, List<String>> entry : headerFields.entrySet()) {
				key = entry.getKey();
				value = entry.getValue();
				if (null != key && null != value) {
					key = key.toLowerCase();
					valueString = value.toString();
					valueString = valueString.substring(1, valueString.length() - 1);
					// Log.d(TAG, "Header field '" + key + "': '" + valueString + "'");
					result.put(key, valueString);
				} else {
					// Log.d(TAG, "Skipped header field '" + key + "': '" + value + "'");
				}
			}

			return result;

		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
		}
	}

	/**
	 * Trust every server - do not check for any certificate
	 */
	// TODO Solve issue with security certificate for HTTPS.
	private static void trustAllHosts() {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType)
					throws CertificateException {
			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType)
					throws CertificateException {
			}

			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new java.security.cert.X509Certificate[] {};
			}
		} };

		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Push GCM Registration ID for current device
	 * 
	 * @param context
	 *            Application context, used to read preferences.
	 * @param registrationId
	 *            Registration ID given by google
	 * @throws IOException
	 * @throws JSONException
	 * @throws IllegalStateException
	 */
	public static void registerGCMId(Context context, String registrationId) throws IOException,
			JSONException {

		if (null == authPrefs) {
			authPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS, Context.MODE_PRIVATE);
		}
		if (null == mainPrefs) {
			mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
		}

		// Check if already synced with common sense
		String pref_registration_id = authPrefs.getString(Auth.GCM_REGISTRATION_ID, "");

		if (registrationId.equals(pref_registration_id)) {
			// GCM registration id is already sync with commonSense
			return;
		}

		String cookie = authPrefs.getString(Auth.LOGIN_COOKIE, null);
		boolean devMode = mainPrefs.getBoolean(Advanced.DEV_MODE, false);
		String url = devMode ? SenseUrls.DEV_REGISTER_GCM_ID : SenseUrls.REGISTER_GCM_ID;

		// Get the device ID
		String device_id = getDeviceId(context);

		if (device_id == null) {
			// register GCM ID failed, can not get the device ID for this device
			return;
		}

		url = url.replaceFirst("<id>", device_id);

		final JSONObject data = new JSONObject();
		data.put("registration_id", registrationId);

		Map<String, String> response = SenseApi.request(context, url, data, cookie);
		String responseCode = response.get("http response code");
		if (!"200".equals(responseCode)) {
			throw new IOException("Incorrect response from CommonSense: " + responseCode);
		}

		// check registration result
		JSONObject res = new JSONObject(response.get("content"));
		if (!registrationId.equals(res.getJSONObject("device").getString(GCMReceiver.KEY_GCM_ID))) {
			throw new IllegalStateException("GCM registration_id not match with response");
		}

		Editor authEditor = authPrefs.edit();
		authEditor.putString(Auth.GCM_REGISTRATION_ID, registrationId);
		authEditor.commit();
	}

	/**
	 * Get device configuration from commonSense
	 * 
	 * @throws JSONException
	 * @throws IOException
	 */
	public static String getDeviceConfiguration(Context context) throws IOException, JSONException {
		if (null == authPrefs) {
			authPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS, Context.MODE_PRIVATE);
		}
		if (null == mainPrefs) {
			mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
		}

		String cookie = authPrefs.getString(Auth.LOGIN_COOKIE, null);
		boolean devMode = mainPrefs.getBoolean(Advanced.DEV_MODE, false);
		String url = devMode ? SenseUrls.DEV_DEVICE_UPDATE_CONFIGURATION
				: SenseUrls.DEV_DEVICE_UPDATE_CONFIGURATION;

		// Get the device ID
		String device_id = getDeviceId(context);

		url = url.replaceFirst("<id>", device_id);

		Map<String, String> response = SenseApi.request(context, url, null, cookie);
		String responseCode = response.get("http response code");
		if (!"200".equals(responseCode)) {
			Log.w(TAG, "Failed to get device configuration! Response code: " + responseCode);
			throw new IOException("Incorrect response from CommonSense: " + responseCode);
		}

		return response.get("content");
	}
	
	/**
	 * Get specific configuration from commonSense
	 * 
	 * @throws JSONException
	 * @throws IOException
	 * 
	 */
	public static String getDeviceConfiguration(Context context, String configuration_id) throws IOException, JSONException {
		final SharedPreferences authPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS,
				Context.MODE_PRIVATE);
		final SharedPreferences prefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
				Context.MODE_PRIVATE);

		String cookie = authPrefs.getString(Auth.LOGIN_COOKIE, null);
		boolean devMode = prefs.getBoolean(Advanced.DEV_MODE, false);
		String url = devMode ? SenseUrls.DEV_CONFIGURATION
				: SenseUrls.DEV_CONFIGURATION;

		url = url.replaceFirst("<id>", configuration_id);

		Map<String, String> response = SenseApi.request(context, url, null, cookie);
		String responseCode = response.get("http response code");
		if (!"200".equals(responseCode)) {
			Log.w(TAG, "Failed to get Requirement! Response code: " + responseCode);
			throw new IOException("Incorrect response from CommonSense: " + responseCode);
		}

		return response.get("content");
	}
}
