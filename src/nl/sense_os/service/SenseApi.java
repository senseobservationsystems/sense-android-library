package nl.sense_os.service;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SenseApi {

    private static final String TAG = "SenseApi";
    private static final long CACHE_REFRESH = 1000l * 60 * 60; // 1 hour

    /**
     * Gets the current device ID for use with CommonSense. The device ID is cached in the
     * preferences if it was fetched earlier.
     * 
     * @param context
     *            Context for getting preferences
     * @return the device ID
     */
    public static int getDeviceId(Context context) {
        try {
            // try to get the device ID from the preferences
            final SharedPreferences authPrefs = context.getSharedPreferences(Constants.AUTH_PREFS,
                    Context.MODE_PRIVATE);
            int cachedId = authPrefs.getInt(Constants.PREF_DEVICE_ID, -1);
            long cacheTime = authPrefs.getLong(Constants.PREF_DEVICE_ID_TIME, 0);
            boolean isOutdated = System.currentTimeMillis() - cacheTime > CACHE_REFRESH;
            if (cachedId != -1 && false == isOutdated) {
                return cachedId;
            }

            Log.d(TAG, "Device ID is missing or outdated, refreshing...");

            // Store phone type and IMEI. These are used to uniquely identify this device
            final TelephonyManager telMgr = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
            final String imei = telMgr.getDeviceId();
            final Editor editor = authPrefs.edit();
            editor.putString(Constants.PREF_PHONE_IMEI, imei);
            editor.putString(Constants.PREF_PHONE_TYPE, Build.MODEL);
            editor.commit();

            // get list of devices that are already registered at CommonSense for this user
            final URI uri = new URI(Constants.URL_GET_DEVICES);
            String cookie = authPrefs.getString(Constants.PREF_LOGIN_COOKIE, "");
            JSONObject response = SenseApi.getJsonObject(uri, cookie);

            // check if this device is in the list
            if (response != null) {
                JSONArray deviceList = response.getJSONArray("devices");
                if (deviceList != null) {

                    for (int x = 0; x < deviceList.length(); x++) {

                        JSONObject device = deviceList.getJSONObject(x);
                        if (device != null) {
                            String uuid = device.getString("uuid");

                            // Found the right device if UUID matches IMEI
                            if (uuid.equalsIgnoreCase(imei)) {

                                // cache device ID in preferences
                                cachedId = Integer.parseInt(device.getString("id"));
                                editor.putString(Constants.PREF_DEVICE_TYPE,
                                        device.getString("type"));
                                editor.putInt(Constants.PREF_DEVICE_ID, cachedId);
                                editor.putLong(Constants.PREF_DEVICE_ID_TIME,
                                        System.currentTimeMillis());
                                editor.remove(Constants.PREF_SENSOR_LIST);
                                editor.commit();

                                return cachedId;
                            }
                        }
                    }
                }
            }
            return -1;
        } catch (Exception e) {
            Log.e(TAG, "Exception determining device ID: " + e.getMessage());
            return -1;
        }
    }
    /**
     * @return a JSONObject from the requested URI
     */
    public static JSONObject getJsonObject(URI uri, String cookie) {
        try {
            final HttpGet get = new HttpGet(uri);
            get.setHeader("Cookie", cookie);
            final HttpClient client = new DefaultHttpClient();

            // client.getConnectionManager().closeIdleConnections(2, TimeUnit.SECONDS);
            final HttpResponse response = client.execute(get);
            if (response == null)
                return null;
            if (response.getStatusLine().getStatusCode() != 200) {
                Log.e(TAG, "Error receiving content for " + uri.toString() + ". Status code: "
                        + response.getStatusLine().getStatusCode());
                return null;
            }

            HttpEntity entity = response.getEntity();
            InputStream is = entity.getContent();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is), 1024);
            String line;
            StringBuffer responseString = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                responseString.append(line);
                responseString.append('\r');
            }
            rd.close();
            return new JSONObject(responseString.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error receiving content for " + uri.toString() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets a list of all registered sensors for this device, and stores it in the preferences.
     * 
     * @param context
     *            Context for getting preferences
     * @see {@link Constants#PREF_SENSOR_LIST}
     */
    public static JSONArray getRegisteredSensors(Context context) {
        try {
            // get device ID to use in communication with CommonSense
            int deviceId = getDeviceId(context);
            if (deviceId == -1) {
                Log.e(TAG, "Cannot get list of sensors: device ID is unknown.");
                return null;
            }

            // check cache retention time for the list of sensors
            final SharedPreferences authPrefs = context.getSharedPreferences(Constants.AUTH_PREFS,
                    Context.MODE_PRIVATE);
            String cachedSensors = authPrefs.getString(Constants.PREF_SENSOR_LIST, null);
            long cacheTime = authPrefs.getLong(Constants.PREF_SENSOR_LIST_TIME, 0);
            boolean isOutdated = System.currentTimeMillis() - cacheTime > CACHE_REFRESH;

            // return cached list of it is still valid
            if (false == isOutdated && null != cachedSensors) {
                return new JSONArray(cachedSensors);
            }

            Log.d(TAG, "List of sensor IDs is missing or outdated, refreshing...");

            // get fresh list of sensors for this device from CommonSense
            String cookie = authPrefs.getString(Constants.PREF_LOGIN_COOKIE, "NO_COOKIE");
            URI uri = new URI(Constants.URL_GET_SENSORS.replaceAll("<id>", "" + deviceId));
            JSONObject response = SenseApi.getJsonObject(uri, cookie);

            // parse response and store the list
            if (response != null) {
                JSONArray sensorList = response.getJSONArray("sensors");
                if (sensorList != null) {
                    Editor authEditor = authPrefs.edit();
                    authEditor.putString(Constants.PREF_SENSOR_LIST, sensorList.toString());
                    authEditor.putLong(Constants.PREF_SENSOR_LIST_TIME, System.currentTimeMillis());
                    authEditor.commit();
                }
                return sensorList;
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in retrieving registered sensors: ", e);
        }
        return null;
    }
    /**
     * This method returns the url to which the data must be send, it does this based on the sensor
     * name and device_type. If the sensor cannot be found, then it will be created
     * 
     * TODO: create a HashMap to search the sensor in, can we keep this in mem of the service?
     */
    public static String getSensorUrl(Context context, String sensorName, String sensorValue,
            String dataType, String deviceType) {
        try {

            JSONArray sensors = getRegisteredSensors(context);

            if (null != sensors) {

                // check all the sensors in the list
                for (int x = 0; x < sensors.length(); x++) {
                    JSONObject sensor = (JSONObject) sensors.get(x);

                    if (sensor.getString("device_type").equalsIgnoreCase(deviceType)
                            && sensor.getString("name").equalsIgnoreCase(sensorName)) {

                        // found the right sensor
                        if (dataType.equals(Constants.SENSOR_DATA_TYPE_FILE)) {
                            return Constants.URL_POST_FILE.replaceFirst("<id>",
                                    sensor.getString("id"));
                        } else {
                            return Constants.URL_POST_SENSOR_DATA.replaceFirst("<id>",
                                    sensor.getString("id"));
                        }
                    }
                }
            } else {
                sensors = new JSONArray();
            }

            /* Sensor not found, create it at CommonSense */

            // prepare request to create new sensor
            URL url = new URL(Constants.URL_CREATE_SENSOR);
            JSONObject postData = new JSONObject();
            JSONObject sensor = new JSONObject();
            sensor.put("name", sensorName);
            sensor.put("device_type", deviceType);
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

            final SharedPreferences authPrefs = context.getSharedPreferences(Constants.AUTH_PREFS,
                    Context.MODE_PRIVATE);
            String cookie = authPrefs.getString(Constants.PREF_LOGIN_COOKIE, "");

            // check if sensor was created successfully
            HashMap<String, String> response = sendJson(url, postData, "POST", cookie);
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

            // store sensor URL in the preferences
            String content = response.get("content");
            Log.d(TAG, "Created sensor: \'" + sensorName + "\'");
            JSONObject responseJson = new JSONObject(content);
            JSONObject JSONSensor = responseJson.getJSONObject("sensor");
            sensors.put(JSONSensor);
            Editor authEditor = authPrefs.edit();
            authEditor.putString(Constants.PREF_SENSOR_LIST, sensors.toString());
            authEditor.commit();

            // Add sensor to this device at CommonSense
            String phoneType = authPrefs.getString(Constants.PREF_PHONE_TYPE, "smartphone");
            url = new URL(Constants.URL_ADD_SENSOR_TO_DEVICE.replaceFirst("<id>",
                    (String) JSONSensor.get("id")));
            postData = new JSONObject();
            JSONObject device = new JSONObject();
            device.put("type", authPrefs.getString(Constants.PREF_DEVICE_TYPE, phoneType));
            device.put("uuid", authPrefs.getString(Constants.PREF_PHONE_IMEI, "0000000000"));
            postData.put("device", device);

            response = sendJson(url, postData, "POST", cookie);
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

            if (dataType.equals(Constants.SENSOR_DATA_TYPE_FILE)) {
                return Constants.URL_POST_FILE.replaceFirst("<id>", (String) JSONSensor.get("id"));
            } else {
                return Constants.URL_POST_SENSOR_DATA.replaceFirst("<id>",
                        (String) JSONSensor.get("id"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in retrieving the right sensor URL: " + e.getMessage());
            return null;
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
     * @return <code>true</code> if successfully logged in
     */
    public static boolean login(Context context, String username, String pass) {
        try {
            final URL url = new URL(Constants.URL_LOGIN);
            final JSONObject user = new JSONObject();
            user.put("username", username);
            user.put("password", pass);
            final HashMap<String, String> response = sendJson(url, user, "POST", "");
            if (response == null) {
                // request failed
                return false;
            }

            final SharedPreferences authPrefs = context.getSharedPreferences(Constants.AUTH_PREFS,
                    Context.MODE_PRIVATE);
            final Editor authEditor = authPrefs.edit();

            // if response code is not 200 (OK), the login was incorrect
            if (response.get("http response code").compareToIgnoreCase("200") != 0) {
                // incorrect login
                Log.e(TAG,
                        "CommonSense login incorrect! Response code: "
                                + response.get("http response code"));
                authEditor.remove(Constants.PREF_LOGIN_COOKIE);
                authEditor.commit();
                return false;
            }

            // if no cookie was returned, something went horribly wrong
            if (response.get("set-cookie") == null) {
                // incorrect login
                Log.e(TAG, "CommonSense login failed: no cookie received.");
                authEditor.remove(Constants.PREF_LOGIN_COOKIE);
                authEditor.commit();
                return false;
            }

            // store cookie in the preferences
            String cookie = response.get("set-cookie");
            Log.d(TAG, "CommonSense login ok!");
            authEditor.putString(Constants.PREF_LOGIN_COOKIE, cookie);
            authEditor.commit();

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Exception during login: " + e.getMessage());

            final SharedPreferences authPrefs = context.getSharedPreferences(Constants.AUTH_PREFS,
                    Context.MODE_PRIVATE);
            final Editor editor = authPrefs.edit();
            editor.remove(Constants.PREF_LOGIN_COOKIE);
            editor.commit();
            return false;
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
     * @return <code>true</code> if registration completed successfully
     */
    public static boolean register(Context context, String username, String pass) {

        // clear cached settings of the previous user
        final SharedPreferences authPrefs = context.getSharedPreferences(Constants.AUTH_PREFS,
                Context.MODE_PRIVATE);
        final Editor authEditor = authPrefs.edit();
        authEditor.remove(Constants.PREF_DEVICE_ID);
        authEditor.remove(Constants.PREF_DEVICE_TYPE);
        authEditor.remove(Constants.PREF_LOGIN_COOKIE);
        authEditor.remove(Constants.PREF_SENSOR_LIST);
        authEditor.commit();

        try {
            final URL url = new URL(Constants.URL_REG);
            final JSONObject data = new JSONObject();
            final JSONObject user = new JSONObject();
            user.put("username", username);
            user.put("password", pass);
            user.put("email", username);
            data.put("user", user);
            final HashMap<String, String> response = SenseApi.sendJson(url, data, "POST", "");
            if (response == null) {
                Log.e(TAG, "Error registering new user. response=null");
                return false;
            }
            if (response.get("http response code").compareToIgnoreCase("201") != 0) {
                Log.e(TAG,
                        "Error registering new user. Got response code:"
                                + response.get("http response code"));
                return false;
            }

            Log.d(TAG, "CommonSense registration: Successful");
        } catch (final IOException e) {
            Log.e(TAG, "IOException during registration!", e);
            return false;
        } catch (final IllegalAccessError e) {
            Log.e(TAG, "IllegalAccessError during registration!", e);
            return false;
        } catch (JSONException e) {
            Log.e(TAG, "JSONException during registration!", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Exception during registration!", e);
            return false;
        }
        return true;
    }
    /**
     * This method sends a JSON object to update or create an item it returns the HTTP-response code
     */
    public static HashMap<String, String> sendJson(URL url, JSONObject json, String method,
            String cookie) {
        HttpURLConnection urlConn = null;
        try {
            // Open New URL connection channel.
            urlConn = (HttpURLConnection) url.openConnection();

            // set post request
            urlConn.setRequestMethod(method);

            // Let the run-time system (RTS) know that we want input.
            urlConn.setDoInput(true);

            // we want to do output.
            urlConn.setDoOutput(true);

            // We want no caching
            urlConn.setUseCaches(false);

            // Set cookie
            urlConn.setRequestProperty("Cookie", cookie);

            // Set content size
            urlConn.setRequestProperty("Content-Length", "" + json.toString().length());
            urlConn.setRequestProperty("Content-Type", "application/json");
            urlConn.setInstanceFollowRedirects(false);
            // Set cookie
            urlConn.setRequestProperty("Cookie", cookie);

            // Send POST output.
            DataOutputStream printout;
            printout = new DataOutputStream(urlConn.getOutputStream());

            printout.writeBytes(json.toString());
            printout.flush();
            printout.close();

            // Get Response
            InputStream is = urlConn.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is), 1024);
            String line;
            StringBuffer responseString = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                responseString.append(line);
                responseString.append('\r');
            }

            rd.close();
            HashMap<String, String> response = new HashMap<String, String>();
            response.put("http response code", "" + urlConn.getResponseCode());
            response.put("content", responseString.toString());
            Map<String, List<String>> headerFields = urlConn.getHeaderFields();
            Iterator<String> it = headerFields.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                String value = headerFields.get(key).toString().substring(1);
                value = value.substring(0, value.length() - 1);
                response.put(key.toLowerCase(), value);
            }
            return response;
        } catch (Exception e) {
            Log.e(TAG, "Error in posting Json: " + json.toString() + "\n" + e.getMessage());
            return null;
        } finally {

            if (urlConn != null) {
                urlConn.disconnect();
            }
        }
    }

}
