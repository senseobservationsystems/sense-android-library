package nl.sense_os.datastorageengine;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * A proxy class to handle create, read, update, and delete of sensors and
 * sensor data in the cloud using the Sensor REST API.
 * Does not contain functionality like logging in or creating an account.
 */
public class SensorDataProxy {
    private static final String TAG = "SensorDataProxy";

    public enum SERVER {LIVE, STAGING}

    public static final String BASE_URL_LIVE = "https://sensor-api.sense-os.nl";
    public static final String BASE_URL_STAGING = "http://sensor-api.staging.sense-os.nl";

    private String mBaseUrl = null;
    private String mAppKey = null;
    private String mSessionId = null;

    /**
     * Create a sensor data proxy.
     * @param server     Select whether to use the live or staging server.
     * @param appKey     Application key, identifying the application in the REST API.
     * @param sessionId  The session id of the current user.
     */
    public SensorDataProxy(SERVER server, String appKey, String sessionId) {
        this.mBaseUrl = (server == SERVER.LIVE) ? BASE_URL_LIVE : BASE_URL_STAGING;
        this.mAppKey = appKey;
        this.mSessionId = sessionId;
    }

    /**
     * Get the currently set session id
     * @return Returns the sessionId, or null if not set
     */
    public String getSessionId() {
        return mSessionId;
    }

    /**
     * Set the session id of the current user
     * @param sessionId   The session id of the current user.
     */
    public void setSessionId(final String sessionId) {
        this.mSessionId = sessionId;
    }

    /**
     * Get all sensor profiles
     * Throws an exception when no sessionId is set or when the sessionId is not valid.
     * @return Returns an array with sensor profiles, structured like:
     *         [{sensor_name: string, data_structure: JSON}, ...]
     */
    public JSONArray getSensorProfiles() throws JSONException, IOException {
        return request("GET", new URL(mBaseUrl + "/sensor_profiles")).toJSONArray();
    }

    /**
     * Get all sensors of the currently logged in user
     * Throws an exception when no sessionId is set or when the sessionId is not valid.
     * @return Returns an array with sensors
     */
    public JSONArray getSensors() throws IOException, JSONException {
        return request("GET", new URL(mBaseUrl + "/sensors")).toJSONArray();
    }

    /**
     * Get all sensors of the current source of logged in user
     * Throws an exception when no sessionId is set or when the sessionId is not valid.
     * @param sourceName     The source name, for example "sense-ios",
     *                       "sense-android", "fitbit", ...
     * @return Returns an array with sensors
     */
    public JSONArray getSensors(final String sourceName) throws JSONException, IOException {
        return request("GET", sensorUrl(sourceName)).toJSONArray();
    }

    /**
     * Get a sensor of the currently logged in user by it's source name
     * and sensor name.
     * Throws an exception when no sessionId is set, when the sessionId is not valid, or when
     * the source or sensor name is invalid.
     * @param sourceName     The source name, for example "sense-ios",
     *                       "sense-android", "fitbit", ...
     * @param sensorName     The sensor name, for example "accelerometer"
     * @return Returns the sensor
     */
    public JSONObject getSensor(final String sourceName, final String sensorName) throws JSONException, IOException {
        return request("GET", sensorUrl(sourceName, sensorName)).toJSONObject();
    }

    /**
     * Update a sensor's `meta` field. Will override the old `meta` field.
     * Throws an exception when no sessionId is set, when the sessionId is not valid, or when
     * the source or sensor name is invalid.
     * @param sourceName     The source name, for example "sense-ios",
     *                       "sense-android", "fitbit", ...
     * @param sensorName     The sensor name, for example "accelerometer"
     * @param meta           JSON object with meta data
     * @return               Returns the sensor object
     */
    public JSONObject updateSensor(final String sourceName, final String sensorName, final JSONObject meta) throws IOException, JSONException {
        JSONObject body = new JSONObject();
        body.put("meta", meta);

        HTTPUtil.Response res = request("PUT", sensorUrl(sourceName, sensorName), body);

        return res.toJSONObject();
    }

    /**
     * Delete a sensor including all its data
     * Throws an exception when no sessionId is set, when the sessionId is not valid, or when
     * the source or sensor name is invalid.
     *
     * WARNING: this is a dangerous method! Use with care. Or better: don't use it at all.
     *
     * @param sourceName     The source name, for example "sense-ios",
     *                       "sense-android", "fitbit", ...
     * @param sensorName     The sensor name, for example "accelerometer"
     */
    public void deleteSensor(final String sourceName, final String sensorName) throws IOException  {
        request("DELETE", sensorUrl(sourceName, sensorName));
    }

    /**
     * Get sensor data.
     * Throws an exception when no sessionId is set, when the sessionId is not valid, when
     * the source or sensor name is invalid, or when the queryOptions are invalid.
     * @param sourceName     The source name, for example "sense-ios",
     *                       "sense-android", "fitbit", ...
     * @param sensorName     The sensor name, for example "accelerometer"
     * @param queryOptions   Query options to set start and end time, and to sort and limit the data
     * @return Returns the sensor data, structured as `[{time: long, value: JSON}, ...]`
     */
    public JSONArray getSensorData(final String sourceName, final String sensorName, final QueryOptions queryOptions) throws IOException, JSONException {
        URL url = new URL(sensorDataUrl(sourceName, sensorName).toString() + queryOptions.toQueryParams());
        return request("GET", url).toJSONObject().getJSONArray("data");
    }

    /**
     * Create or update sensor data for a single sensor.
     * Throws an exception when no sessionId is set, when the sessionId is not valid, or when
     * the source or sensor name is invalid, or when the data contains invalid entries.
     * @param sourceName     The source name, for example "sense-ios",
     *                       "sense-android", "fitbit", ...
     * @param sensorName     The sensor name, for example "accelerometer"
     * @param data           Array with data points, structured as `[{time: long, value: JSON}, ...]`
     */
    public void putSensorData(final String sourceName, final String sensorName, JSONArray data) throws JSONException, IOException {
        putSensorData(sourceName, sensorName, data, null);
    }

    /**
     * Create or update sensor data for a single sensor.
     * Throws an exception when no sessionId is set, when the sessionId is not valid, or when
     * the source or sensor name is invalid, or when the data contains invalid entries.
     * @param sourceName     The source name, for example "sense-ios",
     *                       "sense-android", "fitbit", ...
     * @param sensorName     The sensor name, for example "accelerometer"
     * @param data           Array with data points, structured as `[{time: long, value: JSON}, ...]`
     * @param meta           Optional field to store meta information. Can be left null
     */
    public void putSensorData(final String sourceName, final String sensorName, JSONArray data, JSONObject meta) throws JSONException, IOException {
        // create one sensor data object
        JSONObject sensor = createSensorDataObject(sourceName, sensorName, data, meta);
        JSONArray requestBody = new JSONArray();
        requestBody.put(sensor);

        request("PUT", sensorDataUrl(), requestBody);
    }

    /**
     * Create or update a sensor data of multiple sensors at once.
     *
     * The helper function `createSensorDataObject` can be used to build the JSONObject for
     * each sensor.
     *
     * Throws an exception when no sessionId is set, when the sessionId is not valid, or when
     * the source or sensor name is invalid, or when the data contains invalid entries.
     * @param sensorsData   Array with sensor data of multiple sensors, structured as:
     *
     *                      [
     *                        {
     *                          source_name: string,
     *                          sensor_name, string,
     *                          meta: JSON,   // optional
     *                          data: [
     *                            {time: number, value: JSON},
     *                            // ...
     *                          ]
     *                        },
     *                        // ...
     *                      ]
     */
    public void putSensorData(JSONArray sensorsData) throws JSONException, IOException {
        request("PUT", sensorDataUrl(), sensorsData);
    }

    /**
     * Helper function to create a JSONObject with the following structure:
     *
     *     {
     *       source_name: string,
     *       sensor_name, string,
     *       data: [
     *         {time: number, value: JSON},
     *         ...
     *       ]
     *     }
     *
     * This helper function can be used to prepare the data for putSensorData.
     * @param sourceName     The source name, for example "sense-ios",
     *                       "sense-android", "fitbit", ...
     * @param sensorName     The sensor name, for example "accelerometer"
     * @param data           Array with data points, structured as `[{time: long, value: JSON}, ...]`
     * @param meta           Optional field to store meta information. Can be left null
     */
    public static JSONObject createSensorDataObject (final String sourceName, final String sensorName, JSONArray data, JSONObject meta) throws JSONException {
        JSONObject sensorData = new JSONObject();

        sensorData.put("source_name", sourceName);
        sensorData.put("sensor_name", sensorName);
        if (meta != null) {
            sensorData.put("meta", meta);
        }
        sensorData.put("data", data);

        return sensorData;
    }

    /**
     * Helper function to create a JSONObject with the following structure:
     *
     *     {
     *       source_name: string,
     *       sensor_name, string,
     *       data: [
     *         {time: number, value: JSON},
     *         ...
     *       ]
     *     }
     *
     * This helper function can be used to prepare the data for putSensorData.
     * @param sourceName     The source name, for example "sense-ios",
     *                       "sense-android", "fitbit", ...
     * @param sensorName     The sensor name, for example "accelerometer"
     * @param data           Array with data points, structured as `[{time: long, value: JSON}, ...]`
     */
    public static JSONObject createSensorDataObject (final String sourceName, final String sensorName, JSONArray data) throws JSONException {
        return createSensorDataObject(sourceName, sensorName, data, null);
    }

    /**
     * Delete sensor data.
     * Throws an exception when no sessionId is set, when the sessionId is not valid, when
     * the source or sensor name is invalid, or when startTime or endTime are invalid.
     *
     * WARNING: this is a dangerous method, use with care.
     *
     * @param sourceName     The source name, for example "sense-ios",
     *                       "sense-android", "fitbit", ...
     * @param sensorName     The sensor name, for example "accelerometer".
     * @param startTime      Start time of the data series to be deleted.
     *                       When startTime is null, all data until endTime will be removed.
     * @param endTime        End time of the data series to be deleted.
     *                       When endTime is null, all data from startTime till now will be removed.
     *                       When both startTime and endTime are null, all data will be removed.
     */
    public void deleteSensorData(final String sourceName, final String sensorName, final Long startTime, final Long endTime) throws IOException {
        String query = "";
        if (startTime != null) {
            query += "?start_time=" + startTime;
        }
        if (endTime != null) {
            query += (query.isEmpty() ? "?" : "&") + "end_time=" + endTime;
        }
        URL url = new URL(sensorDataUrl(sourceName, sensorName).toString() + query);
        request("DELETE", url);
    }

    /**
     * Helper function to built up the URL for a sensor
     * @param sourceName
     * @return Returns the sensor url
     * @throws UnsupportedEncodingException
     * @throws MalformedURLException
     */
    protected URL sensorUrl(String sourceName)
            throws UnsupportedEncodingException, MalformedURLException {
        return new URL(mBaseUrl + "/sensors/" + HTTPUtil.encode(sourceName));
    }

    /**
     * Helper function to built up the URL for a sensor
     * @param sourceName
     * @param sensorName
     * @return Returns the sensor url
     * @throws UnsupportedEncodingException
     * @throws MalformedURLException
     */
    protected URL sensorUrl(String sourceName, String sensorName)
            throws UnsupportedEncodingException, MalformedURLException {
        return new URL(mBaseUrl + "/sensors/" + HTTPUtil.encode(sourceName) + "/" + HTTPUtil.encode(sensorName));
    }

    /**
     * Helper function to built up the URL for sensor data
     * @param sourceName
     * @param sensorName
     * @return Returns the sensor url
     * @throws UnsupportedEncodingException
     * @throws MalformedURLException
     */
    protected URL sensorDataUrl(String sourceName, String sensorName)
            throws UnsupportedEncodingException, MalformedURLException {
        return new URL(mBaseUrl + "/sensor_data/" + HTTPUtil.encode(sourceName) + "/" + HTTPUtil.encode(sensorName));
    }

    /**
     * Helper function to built up the URL for sensor data
     * @return Returns the sensor url
     * @throws UnsupportedEncodingException
     * @throws MalformedURLException
     */
    protected URL sensorDataUrl() throws UnsupportedEncodingException, MalformedURLException {
        return new URL(mBaseUrl + "/sensor_data");
    }

    /**
     * Perform an HTTP request to the Sensor data API.
     *
     * @param method The request method, such as POST or GET.
     * @param url Complete URL to perform request to.
     * @param body Content for the request.
     * @throws IOException
     *            IOException is thrown when the inputStream has errors.
     * @return Returns a Response containing response code, body, and response headers.
     */
    protected HTTPUtil.Response request(String method, URL url, JSONObject body) throws IOException {
        return request(method, url, body.toString());
    }

    /**
     * Perform an HTTP request to the Sensor data API.
     *
     * @param method The request method, such as POST or GET.
     * @param url Complete URL to perform request to.
     * @param body Content for the request.
     * @throws IOException
     *            IOException is thrown when the inputStream has errors.
     * @return Returns a Response containing response code, body, and response headers.
     */
    protected HTTPUtil.Response request(String method, URL url, JSONArray body) throws IOException {
        return request(method, url, body.toString());
    }

    /**
     * Perform an HTTP request to the Sensor data API.
     *
     * @param method The request method, such as POST or GET.
     * @param url Complete URL to perform request to.
     * @throws IOException
     *            IOException is thrown when the inputStream has errors.
     * @return Returns a Response containing response code, body, and response headers.
     */
    protected HTTPUtil.Response request(String method, URL url) throws IOException {
        final String body = null;
        return request(method, url, body);
    }

    /**
     * Perform an HTTP request to the Sensor data API.
     * Will automatically add headers with application key and session id,
     * and sends the content type
     *
     * @param method The request method, such as POST or GET.
     * @param url Complete URL to perform request to.
     * @param body Content for the request.
     * @throws IOException
     *            IOException is thrown when the inputStream has errors.
     * @return Returns a Response containing response code, body, and response headers.
     */
    protected HTTPUtil.Response request(String method, URL url, String body) throws IOException {
        Log.d(TAG, "request method=" + method + " url=" + url.toString());
        HttpURLConnection urlConnection = null;

        // validate whether both sessionId and appKey are set
        if (mSessionId == null) {
            throw new IllegalArgumentException("SessionId is null");
        }
        if (mAppKey == null) {
            throw new IllegalArgumentException("Application key is null");
        }

        Map<String,String> headers = new HashMap<>();
        headers.put("APPLICATION-KEY", mAppKey);
        headers.put("SESSION-ID", mSessionId);

        // When no charset is given in the Content-Type header "ISO-8859-1" should be
        // assumed (see http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.7.1).
        // Because we're uploading UTF-8 the charset should be set to UTF-8.
        if (body != null && !body.isEmpty()) {
            headers.put("Content-Type", "application/json; charset=utf-8");
        }
        headers.put("Accept", "application/json");

        return HTTPUtil.request(method, url, headers, body);
    }

}
