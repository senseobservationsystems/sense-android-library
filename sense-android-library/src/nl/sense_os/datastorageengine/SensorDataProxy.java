package nl.sense_os.datastorageengine;

import android.util.Log;

import org.apache.http.client.HttpResponseException;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

/**
 * A proxy class to handle create, read, update, and delete of sensors and
 * sensor data in the cloud using the Sensor REST API.
 * Does not contain functionality like logging in or creating an account.
 */
public class SensorDataProxy {
    private static final String TAG = "SensorDataProxy";

    public enum SERVER {LIVE, STAGING};

    public static final String BASE_URL_LIVE    = "https://api.sense-os.nl";
    public static final String BASE_URL_STAGING = "http://api.staging.sense-os.nl";

    private String baseUrl = null;
    private String appKey = null;
    private String sessionId = null;

    /**
     * Create a sensor data proxy.
     * @param server     Select whether to use the live or staging server.
     * @param appKey     Application key, identifying the application in the REST API.
     * @param sessionId  The session id of the current user.
     */
    public SensorDataProxy(SERVER server, String appKey, String sessionId) {
        this.baseUrl = (server == SERVER.LIVE) ? BASE_URL_LIVE : BASE_URL_STAGING;
        this.appKey = appKey;
        this.sessionId = sessionId;
    }

    /**
     * Get the currently set session id
     * @return Returns the sessionId, or null if not set
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Set the session id of the current user
     * @param sessionId   The session id of the current user.
     */
    public void setSessionId(final String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Get all sensors of the currently logged in user
     * Throws an exception when no sessionId is set or when the sessionId is not valid.
     * @return Returns an array with sensors
     */
    public JSONArray getSensors() throws IOException, JSONException {
        return request("GET", new URL(baseUrl + "/sensors")).toJSONArray();
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
     * the sensor does not exist.
     * @param sourceName     The source name, for example "sense-ios",
     *                       "sense-android", "fitbit", ...
     * @param sensorName     The sensor name, for example "accelerometer"
     * @return Returns the sensor
     */
    public JSONObject getSensor(final String sourceName, final String sensorName)
            throws JSONException, IOException {
        return request("GET", sensorUrl(sourceName, sensorName)).toJSONObject();
    }

    /**
     * Update a sensors `meta` object. Will override the old `meta` object.
     * Throws an exception when no sessionId is set, when the sessionId is not valid, or when
     * the sensor does not exist.
     * @param sourceName     The source name, for example "sense-ios",
     *                       "sense-android", "fitbit", ...
     * @param sensorName     The sensor name, for example "accelerometer"
     * @param meta           JSON object with meta data
     */
    public void updateSensor(final String sourceName, final String sensorName, final JSONObject meta) throws IOException {
        request("PUT", sensorUrl(sourceName, sensorName));
    }

    /**
     * Delete a sensor including all its data
     * Throws an exception when no sessionId is set, when the sessionId is not valid, or when
     * the sensor does not exist.
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
     * the sensor does not exist, or when the queryOptions are invalid.
     * @param sourceName     The source name, for example "sense-ios",
     *                       "sense-android", "fitbit", ...
     * @param sensorName     The sensor name, for example "accelerometer"
     * @param queryOptions   Query options to set start and end time, and to sort and limit the data
     * @return Returns the sensor data, structured as `[{date: long, value: JSON}, ...]`
     */
    public JSONArray getSensorData(final String sourceName, final String sensorName, final QueryOptions queryOptions) throws IOException, JSONException {
        URL url = new URL(sensorDataUrl(sourceName, sensorName).toString() + queryOptions.toQueryParams());
        return request("GET", url).toJSONArray();
    }

    /**
     * Create or update sensor data for a single sensor.
     * Throws an exception when no sessionId is set, when the sessionId is not valid, or when
     * the sensor does not exist, or when the data contains invalid entries.
     * @param sourceName     The source name, for example "sense-ios",
     *                       "sense-android", "fitbit", ...
     * @param sensorName     The sensor name, for example "accelerometer"
     * @param data           Array with data points, structured as `[{date: long, value: JSON}, ...]`
     */
    public void putSensorData(final String sourceName, final String sensorName, JSONArray data) throws JSONException, IOException {
        // create an array with one sensor data object
        JSONArray requestBody = new JSONArray();
        requestBody.put(createSensorDataObject(sourceName, sensorName, data));

        putSensorData(requestBody);
    }

    /**
     * Create or update a sensor data of multiple sensors at once.
     *
     * The helper function `createSensorDataObject` can be used to build the JSONObjects for
     * each sensor.
     *
     * Throws an exception when no sessionId is set, when the sessionId is not valid, or when
     * the sensor does not exist, or when the data contains invalid entries.
     * @param sensorsData   Array with sensor data of multiple sensors, structured as:
     *
     *                      [
     *                        {
     *                          source_name: string,
     *                          sensor_name, string,
     *                          data: [
     *                            {date: number, value: JSON},
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
     *         {date: number, value: JSON},
     *         ...
     *       ]
     *     }
     *
     * This helper function can be used to prepare the data for putSensorData.
     * @param sourceName     The source name, for example "sense-ios",
     *                       "sense-android", "fitbit", ...
     * @param sensorName     The sensor name, for example "accelerometer"
     * @param data           Array with data points, structured as `[{date: long, value: JSON}, ...]`
     */
    public static JSONObject createSensorDataObject (final String sourceName, final String sensorName, JSONArray data) throws JSONException {
        JSONObject sensorData = new JSONObject();

        sensorData.put("source_name", sourceName);
        sensorData.put("sensor_name", sensorName);
        sensorData.put("data", data);

        return sensorData;
    }

    /**
     * Delete sensor data.
     * Throws an exception when no sessionId is set, when the sessionId is not valid, when
     * the sensor does not exist, or when startTime or endTime are invalid.
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
        return new URL(baseUrl + "/sensors/" + encode(sourceName));
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
        return new URL(baseUrl + "/sensors/" + encode(sourceName) + "/" + encode(sensorName));
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
        return new URL(baseUrl + "/sensors/" + encode(sourceName) + "/" + encode(sensorName) + "/data");
    }

    /**
     * Helper function to built up the URL for sensor data
     * @return Returns the sensor url
     * @throws UnsupportedEncodingException
     * @throws MalformedURLException
     */
    protected URL sensorDataUrl() throws UnsupportedEncodingException, MalformedURLException {
        return new URL(baseUrl + "/sensors/data");
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
    protected Response request(String method, URL url, JSONObject body) throws IOException {
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
    protected Response request(String method, URL url, JSONArray body) throws IOException {
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
    protected Response request(String method, URL url) throws IOException {
        final String body = "";
        return request(method, url, body);
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
    protected Response request(String method, URL url, String body) throws IOException {
        Log.d(TAG, "request method=" + method + " url=" + url.toString());
        HttpURLConnection urlConnection = null;

        // TODO: use a library like https://github.com/kevinsawicki/http-request instead of our own baked request method

        // validate whether both sessionId and appKey are set
        if (sessionId == null) {
            throw new IllegalArgumentException("SessionId is null");
        }
        if (appKey == null) {
            throw new IllegalArgumentException("Application key is null");
        }

        try {
            final boolean compress = true;
            // open new URL connection channel.
            if ("https".equals(url.getProtocol().toLowerCase(Locale.ENGLISH))) {
                HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
                https.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                urlConnection = https;
            } else {
                urlConnection = (HttpURLConnection) url.openConnection();
            }
            // some parameters
            urlConnection.setUseCaches(false);
            urlConnection.setInstanceFollowRedirects(false);
            urlConnection.setRequestProperty("Accept", "application/json");

            // set request method
            if(method != null) {
                urlConnection.setRequestMethod(method);
            }

            // set headers
            if(appKey != null) {
                urlConnection.setRequestProperty("APPLICATION-KEY", appKey);
            }
            if(sessionId != null) {
                urlConnection.setRequestProperty("SESSION-ID", sessionId);
            }

            // send content (if available)
            if (body != null) {
                urlConnection.setDoOutput(true);
                // When no charset is given in the Content-Type header "ISO-8859-1" should be
                // assumed (see http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.7.1).
                // Because we're uploading UTF-8 the charset should be set to UTF-8.
                urlConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                // send content
                DataOutputStream printout;
                if (compress) {
                    // do not set content size
                    // use chunked transfer mode instead
                    urlConnection.setChunkedStreamingMode(0);
                    urlConnection.setRequestProperty("Content-Encoding", "gzip");
                    GZIPOutputStream zipStream = new GZIPOutputStream(
                            urlConnection.getOutputStream());
                    printout = new DataOutputStream(zipStream);
                } else {
                    // set content size
                    // The content length should be in bytes. We cannot use string length here
                    // because that counts the number of chars while not accounting for multibyte
                    // chars
                    int contentLength = body.getBytes("UTF-8").length;
                    urlConnection.setFixedLengthStreamingMode(contentLength);
                    urlConnection.setRequestProperty("Content-Length", "" + contentLength);
                    printout = new DataOutputStream(urlConnection.getOutputStream());
                }
                // Write the string in UTF-8 encoding
                printout.write(body.getBytes("UTF-8"));
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
            StringBuffer responseBody = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                responseBody.append(line);
                responseBody.append('\r');
            }

            // clean up
            reader.close();
            reader = null;
            inputStream.close();
            inputStream = null;

            // validate response code, throw an exception when not 200
            int responseCode = urlConnection.getResponseCode();
            Log.d(TAG, "response code=" + responseCode);
            if (responseCode != 200) {
                try {
                    // we expect the body to be a JSONObject like {"code": number, "reason": string}
                    JSONObject errorBody = new JSONObject(responseBody.toString());
                    throw new HttpResponseException(
                            errorBody.getInt("code"),
                            errorBody.getString("reason"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                throw new HttpResponseException(responseCode, responseBody.toString());
            }

            // create the response
            Response response = new Response();
            response.code =    urlConnection.getResponseCode();
            response.headers = urlConnection.getHeaderFields();
            response.body =    responseBody.toString();
            return response;

        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    /**
     * Helper function to encode an URL parameter
     * @param param
     * @return Returns the url encoded parameter
     * @throws UnsupportedEncodingException
     */
    protected String encode (String param) throws UnsupportedEncodingException {
        if (param != null) {
            return URLEncoder.encode(param, "UTF-8");
        }
        else {
            return "";
        }
    }

    /**
     * Helper class containing the code, response body, and headers of an HTTP request.
     * Contains helper methods to parse the response body as a JSONObject or JSONArray.
     */
    public class Response {
        public Integer code = null;
        public String body = null;
        public Map<String, List<String>> headers = new HashMap<>();

        public JSONObject toJSONObject() throws JSONException {
            return new JSONObject(body);
        }

        public JSONArray toJSONArray() throws JSONException {
            return new JSONArray(body);
        }
    }
}
