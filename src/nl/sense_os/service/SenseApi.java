/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.util.Log;

import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Auth;
import nl.sense_os.service.constants.SensePrefs.Main.Advanced;
import nl.sense_os.service.constants.SenseUrls;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    /**
     * Gets a list of all sensors for this user at the CommonSense API. Uses caching for increased
     * performance.
     * 
     * @param context
     *            Application context, used for getting preferences.
     * @return The list of sensors (can be empty), or <code>null</code> if an error occurred and the
     *         list could not be retrieved.
     */
    public static JSONArray getAllSensors(Context context) {

        final SharedPreferences authPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS,
                Context.MODE_PRIVATE);

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
            // should not happen, we are only using stuff that was previously cached
            Log.e(TAG, "Failed to get list of sensors! Exception while checking cache: ", e);
            return null;
        }

        // if we make it here, the list was not in the cache
        // Log.v(TAG, "List of sensor IDs is missing or outdated, refreshing...");

        try {
            // get fresh list of sensors for this device from CommonSense
            String cookie = authPrefs.getString(Auth.LOGIN_COOKIE, "NO_COOKIE");
            boolean devMode = authPrefs.getBoolean(Auth.DEV_MODE, false);
            String url = devMode ? SenseUrls.DEV_ALL_SENSORS : SenseUrls.ALL_SENSORS;
            HashMap<String, String> response = SenseApi.request(context, url, null, cookie);

            // parse response and store the list
            JSONObject content = new JSONObject(response.get("content"));
            JSONArray sensorList = content.getJSONArray("sensors");

            // store the new sensor list
            Editor authEditor = authPrefs.edit();
            authEditor.putString(Auth.SENSOR_LIST_COMPLETE, sensorList.toString());
            authEditor.putLong(Auth.SENSOR_LIST_COMPLETE_TIME, System.currentTimeMillis());
            authEditor.commit();

            return sensorList;

        } catch (Exception e) {
            Log.e(TAG, "Exception in retrieving registered sensors: ", e);
            return null;

        }
    }

    /**
     * Gets the current device ID for use with CommonSense. The device ID is cached in the
     * preferences if it was fetched earlier.
     * 
     * @param context
     *            Context for getting preferences
     * @return the device ID, or -1 if the device is not registered yet, or -2 if an error occurred.
     */
    public static int getDeviceId(Context context) {

        final SharedPreferences authPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS,
                Context.MODE_PRIVATE);

        // try to get the device ID from the cache
        try {
            int cachedId = authPrefs.getInt(Auth.DEVICE_ID, -1);
            long cacheTime = authPrefs.getLong(Auth.DEVICE_ID_TIME, 0);
            boolean isOutdated = System.currentTimeMillis() - cacheTime > CACHE_REFRESH;

            // return cached ID of it is still valid
            if (cachedId != -1 && false == isOutdated) {
                return cachedId;
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to get device ID! Exception while checking cache: ", e);
            return -2; // error return -2
        }

        // if we make it here, the device ID was not in the cache
        // Log.v(TAG, "Device ID is missing or outdated, refreshing...");

        // get phone IMEI, this is used as the device UUID at CommonSense
        final String imei = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE))
                .getDeviceId();

        try {
            // get list of devices that are already registered at CommonSense for this user
            boolean devMode = authPrefs.getBoolean(Auth.DEV_MODE, false);
            String url = devMode ? SenseUrls.DEV_DEVICES : SenseUrls.DEVICES;
            String cookie = authPrefs.getString(Auth.LOGIN_COOKIE, null);
            HashMap<String, String> response = SenseApi.request(context, url, null, cookie);

            // check if this device is in the list
            JSONObject content = new JSONObject(response.get("content"));
            JSONArray deviceList = content.getJSONArray("devices");

            String uuid = "";
            for (int x = 0; x < deviceList.length(); x++) {

                JSONObject device = deviceList.getJSONObject(x);
                uuid = device.getString("uuid");

                // pad the UUID with leading zeros, CommonSense API removes them
                while (uuid.length() < imei.length()) {
                    uuid = "0" + uuid;
                }

                // Found the right device if UUID matches IMEI
                if (uuid.equalsIgnoreCase(imei)) {

                    // cache device ID in preferences
                    int deviceId = Integer.parseInt(device.getString("id"));
                    final Editor editor = authPrefs.edit();
                    editor.putString(Auth.DEVICE_TYPE, device.getString("type"));
                    editor.putInt(Auth.DEVICE_ID, deviceId);
                    editor.putLong(Auth.DEVICE_ID_TIME, System.currentTimeMillis());
                    editor.remove(Auth.SENSOR_LIST);
                    editor.commit();
                    return deviceId;
                }
            }

            // if we make it here, the device was not registered yet: return -1
            Log.w(TAG, "This device is not registered at CommonSense yet");
            return -1;

        } catch (Exception e) {
            Log.e(TAG, "Failed to get device ID: exception communicating wih CommonSense!", e);
            return -2;

        }
    }

    /**
     * Gets a list of all registered sensors for this device at the CommonSense API. Uses caching
     * for increased performance.
     * 
     * @param context
     *            Application context, used for getting preferences.
     * @return The list of sensors (can be empty), or <code>null</code> if an error occurred and the
     *         list could not be retrieved.
     */
    public static JSONArray getRegisteredSensors(Context context) {

        final SharedPreferences authPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS,
                Context.MODE_PRIVATE);

        // try to get list of sensors from the cache
        try {
            String cachedSensors = authPrefs.getString(Auth.SENSOR_LIST, null);
            long cacheTime = authPrefs.getLong(Auth.SENSOR_LIST_TIME, 0);
            boolean isOutdated = System.currentTimeMillis() - cacheTime > CACHE_REFRESH;

            // return cached list of it is still valid
            if (false == isOutdated && null != cachedSensors) {
                return new JSONArray(cachedSensors);
            }

        } catch (Exception e) {
            // should not happen, we are only using stuff that was previously cached
            Log.e(TAG, "Failed to get list of sensors! Exception while checking cache: ", e);
            return null;
        }

        // if we make it here, the list was not in the cache
        // Log.v(TAG, "List of sensor IDs is missing or outdated, refreshing...");

        try {

            // get device ID to use in communication with CommonSense
            int deviceId = getDeviceId(context);
            if (deviceId == -1) {
                // device is not yet registered, so the sensor list is empty
                Log.w(TAG, "The list of sensors is empty: device is not registered yet.");
                return new JSONArray("[]");

            } else if (deviceId == -2) {
                // there was an error retrieving info from CommonSense: give up
                Log.e(TAG, "Problem getting sensor list: failed to get device ID from CommonSense");
                return null;
            }

            // get fresh list of sensors for this device from CommonSense
            String cookie = authPrefs.getString(Auth.LOGIN_COOKIE, "NO_COOKIE");
            boolean devMode = authPrefs.getBoolean(Auth.DEV_MODE, false);
            String url = devMode ? SenseUrls.DEV_SENSORS : SenseUrls.SENSORS;
            url = url.replaceFirst("<id>", "" + deviceId);
            HashMap<String, String> response = SenseApi.request(context, url, null, cookie);

            // parse response and store the list
            JSONObject content = new JSONObject(response.get("content"));
            JSONArray sensorList = content.getJSONArray("sensors");

            // store the new sensor list
            Editor authEditor = authPrefs.edit();
            authEditor.putString(Auth.SENSOR_LIST, sensorList.toString());
            authEditor.putLong(Auth.SENSOR_LIST_TIME, System.currentTimeMillis());
            authEditor.commit();

            return sensorList;

        } catch (Exception e) {
            Log.e(TAG, "Exception in retrieving registered sensors: ", e);
            return null;

        }
    }

    public static String getSensorId(Context context, String sensorName, String displayName,
            String sensorValue, String dataType, String deviceType) {

        try {
            // get list of all registered sensors for this device
            JSONArray sensors = getRegisteredSensors(context);

            if (null != sensors) {

                // check sensors with similar names in the list
                List<JSONObject> similar = new ArrayList<JSONObject>();
                for (int x = 0; x < sensors.length(); x++) {
                    JSONObject sensor = (JSONObject) sensors.get(x);

                    if (sensor.getString("name").equalsIgnoreCase(sensorName)) {
                        similar.add(sensor);
                    }
                }

                // if there are multiple sensors with the same name, also check other fields
                if (similar.size() > 1) {
                    for (JSONObject sensor : similar) {
                        if (sensor.getString("device_type").equalsIgnoreCase(deviceType)
                                && sensor.getString("data_type").equalsIgnoreCase(dataType)) {
                            return sensor.getString("id");
                        }
                    }
                    // if we make it here, there was no exact match...
                    return similar.get(0).getString("id");

                } else if (similar.size() == 1) {
                    // only one sensor with the correct name
                    return similar.get(0).getString("id");

                } else {
                    // sensor does not exist (yet)
                    return null;
                }

            } else {
                // couldn't get the list of sensors, probably a connection problem: give up
                Log.w(TAG, "Failed to get ID for sensor '" + sensorName
                        + "': there was an error getting the list of sensors");
                return null;
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to get ID for sensor '" + sensorName + "': Exception occurred:", e);
            return null;
        }
    }

    /**
     * This method returns the URL to which the data must be send, it does this based on the sensor
     * name and device_type. If the sensor cannot be found, then it will be created.
     */
    public static String getSensorUrl(Context context, String sensorName, String displayName,
            String sensorValue, String dataType, String deviceType) {

        try {
            String id = getSensorId(context, sensorName, displayName, sensorValue, dataType,
                    deviceType);

            if (id == null) {
                Log.e(TAG, "Failed to get URL for sensor '" + sensorName
                        + "': sensor ID is not available");
                return null;
            }

            final SharedPreferences authPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS,
                    Context.MODE_PRIVATE);
            final boolean devMode = authPrefs.getBoolean(Auth.DEV_MODE, false);

            // found the right sensor
            if (dataType.equals(SenseDataTypes.FILE)) {
                String url = devMode ? SenseUrls.DEV_SENSOR_FILE : SenseUrls.SENSOR_FILE;
                return url.replaceFirst("<id>", id);
            } else {
                String url = devMode ? SenseUrls.DEV_SENSOR_DATA : SenseUrls.SENSOR_DATA;
                return url.replaceFirst("<id>", id);
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to get URL for sensor '" + sensorName + "': Exception occurred:", e);
            return null;
        }
    }

    /**
     * @param context
     *            Context for getting preferences
     * @param appId
     *            Identifier for the app that requests the session ID
     * @return The current CommonSense session ID
     * @throws RemoteException
     *             if the app ID is not valid
     */
    public static String getSessionId(Context context, String appId) throws IllegalAccessException {
        SharedPreferences prefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS,
                Context.MODE_PRIVATE);
        if (AppSecrets.ASK.equals(appId)) {
            // Log.v(TAG, "An ASK app accessed the CommonSense session ID");
            return prefs.getString(Auth.LOGIN_COOKIE, null);
        } else if (AppSecrets.RDAM_CS.equals(appId)) {
            // Log.v(TAG, "A Rotterdam CS app accessed the CommonSense session ID");
            return prefs.getString(Auth.LOGIN_COOKIE, null);
        } else if (AppSecrets.SENSE.equals(appId)) {
            // Log.v(TAG, "A Sense app accessed the CommonSense session ID");
            return prefs.getString(Auth.LOGIN_COOKIE, null);
        } else {
            Log.e(TAG, "App is not allowed access to the CommonSense session!");
            throw new IllegalAccessException(
                    "App is not allowed access to the CommonSense session!");
        }
    }

    /**
     * @param hashMe
     *            "clear" password String to be hashed before sending it to CommonSense
     * @return hashed String
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
     *            username for login
     * @param pass
     *            hashed password for login
     * @return 0 if login completed successfully, -2 if login was forbidden, and -1 for any other
     *         errors.
     */
    public static int login(Context context, String username, String pass) {
        try {
            final SharedPreferences authPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS,
                    Context.MODE_PRIVATE);
            final boolean devMode = authPrefs.getBoolean(Auth.DEV_MODE, false);

            final String url = devMode ? SenseUrls.DEV_LOGIN : SenseUrls.LOGIN;
            final JSONObject user = new JSONObject();
            user.put("username", username);
            user.put("password", pass);
            final HashMap<String, String> response = request(context, url, user, null);
            if (response == null) {
                // request failed
                return -1;
            }

            final Editor authEditor = authPrefs.edit();

            // if response code is not 200 (OK), the login was incorrect
            String responseCode = response.get("http response code");
            if ("403".equalsIgnoreCase(responseCode)) {
                Log.e(TAG, "CommonSense login refused! Response: forbidden!");
                authEditor.remove(Auth.LOGIN_COOKIE);
                authEditor.commit();
                return -2;
            } else if (!"200".equalsIgnoreCase(responseCode)) {
                Log.e(TAG, "CommonSense login failed! Response: " + responseCode);
                authEditor.remove(Auth.LOGIN_COOKIE);
                authEditor.commit();
                return -1;
            }

            // if no cookie was returned, something went horribly wrong
            if (response.get("set-cookie") == null) {
                // incorrect login
                Log.e(TAG, "CommonSense login failed: no cookie received.");
                authEditor.remove(Auth.LOGIN_COOKIE);
                authEditor.commit();
                return -1;
            }

            // store cookie in the preferences
            String cookie = response.get("set-cookie");
            // Log.v(TAG, "CommonSense login OK!");
            authEditor.putString(Auth.LOGIN_COOKIE, cookie);
            authEditor.commit();

            Log.i(TAG, "'" + username + "' logged in at CommonSense...");
            return 0;

        } catch (Exception e) {
            if (null != e.getMessage()) {
                Log.e(TAG, "Exception during login! Message: " + e.getMessage());
            } else {
                Log.e(TAG, "Exception during login!", e);
            }

            final SharedPreferences authPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS,
                    Context.MODE_PRIVATE);
            final Editor editor = authPrefs.edit();
            editor.remove(Auth.LOGIN_COOKIE);
            editor.commit();
            return -1;
        }
    }

    /**
     * Registers a new sensor for this device at CommonSense. Also connects the sensor to this
     * device.
     * 
     * @param context
     *            The application context, used to retrieve preferences.
     * @param sensorName
     *            The name of the sensor.
     * @param deviceType
     *            The sensor device type.
     * @param dataType
     *            The sensor data type.
     * @param sensorValue
     *            An example sensor value, used to determine the data structure for JSON type
     *            sensors.
     * @return The new sensor ID at CommonSense, or <code>null</code> if the registration failed.
     */
    public static String registerSensor(Context context, String sensorName, String displayName,
            String deviceType, String dataType, String sensorValue) {

        final SharedPreferences authPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS,
                Context.MODE_PRIVATE);
        final boolean devMode = authPrefs.getBoolean(Auth.DEV_MODE, false);
        String cookie = authPrefs.getString(Auth.LOGIN_COOKIE, "");

        try {
            // prepare request to create new sensor
            String url = devMode ? SenseUrls.DEV_CREATE_SENSOR : SenseUrls.CREATE_SENSOR;
            JSONObject postData = new JSONObject();
            JSONObject sensor = new JSONObject();
            sensor.put("name", sensorName);
            sensor.put("device_type", deviceType);
            sensor.put("display_name", displayName);
            sensor.put("pager_type", "");
            sensor.put("data_type", dataType);
            if (dataType.compareToIgnoreCase("json") == 0) {
                JSONObject dataStructJSon = new JSONObject(sensorValue);
                JSONArray names = dataStructJSon.names();
                for (int x = 0; x < names.length(); x++) {
                    String name = names.getString(x);
                    int start = dataStructJSon.get(name).getClass().getName().lastIndexOf(".");
                    dataStructJSon.put(name, dataStructJSon.get(name).getClass().getName()
                            .substring(start + 1));
                }
                sensor.put("data_structure", dataStructJSon.toString().replaceAll("\"", "\\\""));
            }
            postData.put("sensor", sensor);

            // check if sensor was created successfully
            HashMap<String, String> response = request(context, url, postData, cookie);
            if (response == null) {
                // failed to create the sensor
                Log.e(TAG, "Error creating sensor. response=null");
                return null;
            }
            if (response.get("http response code").compareToIgnoreCase("201") != 0) {
                String code = response.get("http response code");
                Log.e(TAG, "Error creating sensor. Got response code: " + code);
                return null;
            }

            // retrieve the newly created sensor ID
            String content = response.get("content");
            JSONObject responseJson = new JSONObject(content);
            JSONObject JSONSensor = responseJson.getJSONObject("sensor");
            final String id = (String) JSONSensor.get("id");

            // store the new sensor in the preferences
            JSONArray sensors = getRegisteredSensors(context);
            sensors.put(JSONSensor);
            Editor authEditor = authPrefs.edit();
            authEditor.putString(Auth.SENSOR_LIST, sensors.toString());
            authEditor.commit();

            Log.v(TAG, "Created sensor: '" + sensorName + "'");

            // get device properties from preferences, so it matches the properties in CommonSense
            final String device_uuid = ((TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
            final String device_type = authPrefs.getString(Auth.DEVICE_TYPE, Build.MODEL);

            // Add sensor to this device at CommonSense
            url = devMode ? SenseUrls.DEV_ADD_SENSOR_TO_DEVICE : SenseUrls.ADD_SENSOR_TO_DEVICE;
            url = url.replaceFirst("<id>", id);
            postData = new JSONObject();
            JSONObject device = new JSONObject();
            device.put("type", device_type);
            device.put("uuid", device_uuid);
            postData.put("device", device);

            response = request(context, url, postData, cookie);
            if (response == null) {
                // failed to add the sensor to the device
                Log.e(TAG, "Error adding sensor to device. response=null");
                return null;
            }
            if (response.get("http response code").compareToIgnoreCase("201") != 0) {
                String code = response.get("http response code");
                Log.e(TAG, "Error adding sensor to device. Got response code: " + code);
                return null;
            }

            // return the new sensor ID
            return id;

        } catch (JSONException e) {
            Log.e(TAG, "JSONException registering new sensor '" + sensorName + "':", e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Exception registering new sensor '" + sensorName + "':", e);
            return null;
        }
    }

    /**
     * Tries to register a new user at CommonSense. Discards private data of any previous users.
     * 
     * @param context
     *            Context for getting preferences
     * @param username
     *            username to register
     * @param pass
     *            hashed password for the new user
     * @return 0 if registration completed successfully, -2 if the user already exists, and -1
     *         otherwise.
     */
    public static int registerUser(Context context, String username, String pass, String name,
            String surname, String email, String mobile) {

        // clear cached settings of the previous user
        final SharedPreferences authPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS,
                Context.MODE_PRIVATE);
        final Editor authEditor = authPrefs.edit();
        authEditor.remove(Auth.DEVICE_ID);
        authEditor.remove(Auth.DEVICE_TYPE);
        authEditor.remove(Auth.LOGIN_COOKIE);
        authEditor.remove(Auth.SENSOR_LIST);
        authEditor.commit();

        try {
            final boolean devMode = authPrefs.getBoolean(Auth.DEV_MODE, false);

            final String url = devMode ? SenseUrls.DEV_REG : SenseUrls.REG;

            // create JSON object to POST
            final JSONObject data = new JSONObject();
            final JSONObject user = new JSONObject();
            user.put("username", username);
            user.put("password", pass);
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

            final HashMap<String, String> response = SenseApi.request(context, url, data, null);
            if (response == null) {
                Log.e(TAG, "Error registering new user. response=null");
                return -1;
            }
            String responseCode = response.get("http response code");
            if ("201".equalsIgnoreCase(responseCode)) {
                Log.i(TAG, "CommonSense registration successful for '" + username + "'");
            } else if ("409".equalsIgnoreCase(responseCode)) {
                Log.e(TAG, "Error registering new user! User already exists");
                return -2;
            } else {
                Log.e(TAG, "Error registering new user! Response code: " + responseCode);
                return -1;
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSONException during registration!", e);
            return -1;
        } catch (Exception e) {
            Log.e(TAG, "Exception during registration!", e);
            return -1;
        }
        return 0;
    }

    /**
     * Performs request at CommonSense API. Returns the response code, content, and headers.
     * 
     * @param context
     *            Application context, used to read preferences
     * @param urlString
     *            Complete URL to perform request to
     * @param content
     *            (Optional) content for the request. If the content is not null, the request method
     *            is automatically POST. The default method is GET.
     * @param cookie
     *            (Optional) cookie header for the request.
     * @return
     */
    public static HashMap<String, String> request(Context context, String urlString,
            JSONObject content, String cookie) {
        HttpURLConnection urlConnection = null;
        HashMap<String, String> result = new HashMap<String, String>();
        try {
            // Log.d(TAG, "API request: " + (content == null ? "GET" : "POST") + " " + urlString
            // + " cookie:" + cookie);

            // get compression preference
            final SharedPreferences mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
                    Context.MODE_PRIVATE);
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
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream), 1024);
            String line;
            StringBuffer responseContent = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                responseContent.append(line);
                responseContent.append('\r');
            }
            reader.close();
            result.put("content", responseContent.toString());
            result.put("http response code", "" + urlConnection.getResponseCode());

            // get headers
            Map<String, List<String>> headerFields = urlConnection.getHeaderFields();
            for (Entry<String, List<String>> entry : headerFields.entrySet()) {
                String key = entry.getKey();
                List<String> value = entry.getValue();
                if (null != key && null != value) {
                    key = key.toLowerCase();
                    String valueString = value.toString();
                    valueString = valueString.substring(1, valueString.length() - 1);
                    // Log.d(TAG, "Header field '" + key + "': '" + valueString + "'");
                    result.put(key, valueString);
                } else {
                    // Log.d(TAG, "Skipped header field '" + key + "': '" + value + "'");
                }
            }

            return result;

        } catch (Exception e) {
            if (null == e.getMessage()) {
                Log.e(TAG, "Error executing request: " + urlString + ", content: " + content, e);
            } else {
                // less verbose output
                Log.e(TAG, "Error executing request: " + urlString + ", content: " + content + ". "
                        + e.getMessage());
            }
            return null;

        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    /**
     * Trust every server - dont check for any certificate
     */
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
}
