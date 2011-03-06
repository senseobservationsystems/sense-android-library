package nl.sense_os.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
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

public class SenseApi {

    private static final String TAG = "SenseApi";

    /**
     * This method returns a JSONobject from the requested uri
     */
    public static JSONObject getJSONObject(URI uri, String cookie) {
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
     * This method returns the url to which the data must be send, it does this based on the sensor
     * name and device_type. If the sensor cannot be found, then it will be created TODO: create a
     * hashmap to search the sensor in, can we keep this in mem of the service?
     */
    public static String getSensorURL(Context context, String sensorName, String sensorValue,
            String dataType, String deviceType) {
        try {
            final SharedPreferences authPrefs = context.getSharedPreferences(Constants.AUTH_PREFS,
                    Context.MODE_PRIVATE);
            String sensorsStr = authPrefs.getString(Constants.PREF_JSON_SENSOR_LIST, "");
            JSONArray sensors;
            if (sensorsStr.length() > 0) {

                // check all the sensors in the list
                sensors = new JSONArray(sensorsStr);
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

            // Sensor not found, create it at CommonSense
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
            String cookie = authPrefs.getString(Constants.PREF_LOGIN_COOKIE, "");
            HashMap<String, String> response = sendJson(url, postData, "POST", cookie);
            if (response == null) {
                // failed to create the sensor
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
            Editor editor = authPrefs.edit();
            editor.putString(Constants.PREF_JSON_SENSOR_LIST, sensors.toString());
            editor.commit();

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
     * This method sends a Json object to update or create an item it returns the HTTP-response code
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
