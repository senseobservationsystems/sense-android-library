package nl.sense_os.datastorageengine;

import android.util.Log;

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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by fei on 20/08/15.
 */
public class CommonSenseProxy {

    private static final String TAG = "CommonSenseProxy";
    private static String APP_KEY;				  //The app key
    private static String URL_BASE;				  //The base url to use, will differ based on whether to use live or staging server
    private static String URL_AUTH;				  //The base url to use for authentication, will differ based on whether to use live or staging server
    private int requestTimeoutInterval;	  //Timeout interval in seconds

    public static final String BASE_URL_LIVE                = "https://api.sense-os.nl";
    public static final String BASE_URL_STAGING             = "http://api.staging.sense-os.nl";
    public static final String BASE_URL_AUTHENTICATION_LIVE = "https://auth-api.sense-os.nl/v1";
    public static final String BASE_URL_AUTHENTICATION_STAGING = "http://auth-api.staging.sense-os.nl/v1";

    public static final String URL_LOGIN                    = "login";
    public static final String URL_LOGOUT                   = "logout";
    public static final String URL_SENSOR_DEVICE            = "device";
    public static final String URL_SENSORS                  = "sensors";
    public static final String URL_USERS                    = "users";
    public static final String URLE_UPLOAD_MULTIPLE_SENSORS = "sensors/data";
    public static final String URL_DATA                     = "data";
    public static final String URL_DEVICES                  = "devices";

    public static final String URL_JSON_SUFFIX              = ".json";

    public static final String HTTP_METHOD_POST = "POST";
    public static final String HTTP_METHOD_GET = "GET";


    /**
     * Key for getting the http response code from the Map object that is returned by
     * {@link nl.sense_os.datastorageengine.CommonSenseProxy#request(String, JSONObject, String, String)}
     */
    public static final String RESPONSE_CODE = "http response code";
    /**
     * Key for getting the response content from the Map object that is returned by
     * {@link nl.sense_os.datastorageengine.CommonSenseProxy#request(String, JSONObject, String, String)}
     */
    public static final String RESPONSE_CONTENT = "content";

    /**
     * Default constructor for the DSECommonSenseProxy.
     *
     * Takes an app key that will be used throughout the proxies lifetime. Needs to know whether to talk to the live server or the staging server.
     * This cannot be changed during the proxies lifetime. If you need to change this you simply init a new commonsense proxy.
     *
     * @param useLiveServer	If true, the live server will be used. If false, the staging server will be used.
     * @param theAppKey		An application key that identifies the application to the commonsense server. Cannot be empty.
     */
    public CommonSenseProxy (boolean useLiveServer, String theAppKey)
    {
        APP_KEY = theAppKey;
        if(useLiveServer) {
            URL_BASE = BASE_URL_LIVE;
            URL_AUTH = BASE_URL_AUTHENTICATION_LIVE;
        } else {
            URL_BASE = BASE_URL_STAGING;
            URL_AUTH = BASE_URL_AUTHENTICATION_STAGING;
        }
    }


    /**
     * Login a user
     *
     * If a user is already logged in this call will also return the session ID.
     *
     * @param username		A user account in commonsense is uniquely identified by a username. Cannot be empty.
     * @param password		A password in commonsense does not have any specific requirements.
     *                      It will be MD5 hashed before sending to the server so the user does not have to provide a hashed password. Cannot be empty.
     * @throws IOException and JSONException
     * @return				Session ID. Will be null if the call fails.
     */
    public String loginUser( String username, String password) throws IOException, JSONException
    {
        if(username == null || username.isEmpty() || password == null || password.isEmpty())
            throw new IOException("invalid input of username or password");

        final String url = URL_AUTH + "/" + URL_LOGIN;
        final JSONObject user = new JSONObject();
        user.put("username", username);
        user.put("password", password);

        Map<String, String> response = request(url, user, null, HTTP_METHOD_POST);
        // if response code is not 200 (OK), the login was incorrect
        int result = checkResponseCode(response.get(RESPONSE_CODE), "login");

        String session_id = response.get("session-id");

        if (result != 0 && session_id == null) {
            // something went horribly wrong
            Log.w(TAG, "CommonSense login failed: no session id received!");
            throw new IOException("login failed with no session id");
        }
        return session_id;
    }


    /**
     * Logout the currently logged in user.
     *
     * @param sessionID		The sessionID of the user to logout. Cannot be empty.
     * @throws IOException
     * @return				Whether or not the logout finished successfully.
     */
    public boolean logoutCurrentUser(String sessionID) throws IOException
    {
        if(sessionID == null || sessionID.isEmpty())
            throw new IOException("invalid input of session ID");

        final String url = URL_AUTH + "/" + URL_LOGOUT;
        Map<String, String> response = request(url, null, sessionID, HTTP_METHOD_POST);
        // if response code is not 200 (OK), the logout was incorrect
        String responseCode = response.get(RESPONSE_CODE);
        int result = checkResponseCode(response.get(RESPONSE_CODE), "logout");
        if(result != 0)
            throw new IOException("logout with session id failed ");

        return (result == 0);

    }

    /**
     * Create a new sensor in the commonsense backend
     *
     * Each sensor in commonsense is uniquely identified by a name and device type combination.
     * If this combination already exists in the commonsense backend this call will fail.
     *
     * @param name			Name of the sensor to create. This will be used to identify the sensor. Required.
     * @param displayName	Extra field to make sensor name more readable if necessary when displaying it. Not required.
     * @param deviceType	Name of the device type the sensor belongs to. Required.
     * @param dataType		Type of data that the sensor stores. Required.
     * @param dataStructure	Structure of the data in the data; can be used to specify the JSON structure in the sensor. Not required.
     * @param sessionID		The sessionID of the current user. Cannot be empty.
     * @throws IOException, JSONException
     *@result				JSONObject with the information of the created sensor. Null if the sensor ID is null or empty.
     */
    public JSONObject createSensor(String name, String displayName, String deviceType, String dataType, String dataStructure, String sessionID) throws IOException, JSONException
    {
        if(name == null || name.isEmpty() || sessionID == null || sessionID.isEmpty()|| deviceType == null || deviceType.isEmpty()||  dataType == null || dataType.isEmpty())
            throw new IOException("invalid input of name or sessionID or deviceType or dataType");

        if(dataStructure == null) {
            dataStructure = "";
        }

        if(displayName == null) {
            displayName = "";
        }
        final String url = makeCSRestUrlFor(URL_SENSORS, null);

        JSONObject sensor = new JSONObject();
        sensor.put("name", name);
        sensor.put("device_type", deviceType);
        sensor.put("display_name", displayName);
        sensor.put("pager_type", "");
        sensor.put("data_type", dataType);
        sensor.put("data_structure",dataStructure);

        JSONObject postData = new JSONObject();
        postData.put("sensor", sensor);

        // perform actual request
        Map<String, String> response = request(url, postData, sessionID, HTTP_METHOD_POST);

        // check response code
        String code = response.get(RESPONSE_CODE);
        int result = checkResponseCode(response.get(RESPONSE_CODE), "createSensor");
        if (result != -2) {
            throw new IOException("Incorrect response of createSensor from CommonSense: " + code);
        }

        // retrieve the newly created sensor ID
        String locationHeader = response.get("location");
        String[] split = locationHeader.split("/");
        String id = split[split.length - 1];

        if(id == null || id.isEmpty()){
            Log.w(TAG, "No id for the newly created sensor");
            return null;
        }
        // store the new sensor in the preferences
        sensor.put("sensor_id", id);
        return sensor;
    }
    
    /**
     * Get all sensors for the currently logged in user.
     *
     * This will fetch all the sensors for the current user and pass them on JSONArray.
     * Each element in the array will contain a JSONObject with data from one sensor.
     *
     * @param sessionID		The sessionID of the current user. Cannot be empty.
     * @throws IOException, JSONException
     * @result				Array of sensors. Each object will be a JSONObject with the resulting sensor information. Will be null if an error occurs.
     */
    public JSONArray getAllSensors(String sessionID) throws IOException, JSONException
    {
        if(sessionID == null || sessionID.isEmpty())
            throw new IOException("getAllSensors: invalid input of sessionID");
        String params = "&per_page=1000&details=full";
        return getListForURLAction(URL_SENSORS, params, "sensors", sessionID, 1000, "getAllSensors");
    }

    /**
     * Get all devices for the currently logged in user.
     *
     * This will fetch all the devices for the current user and pass them on to a JSONArray. Each element in the array will contain a JSONObject with data from one device.
     *
     * @param sessionID		The sessionID of the current user. Cannot be empty.
     * @throws IOException, JSONException
     * @result				Array of devices. Each object will be a JSONObject with the resulting device information. Will be null if an error occurs.
     */
    public JSONArray getAllDevices(String sessionID) throws IOException, JSONException
    {
        if(sessionID == null || sessionID.isEmpty())
            throw new IOException("getAllDevices: invalid input of sessionID");
        String params = "&per_page=1000&details=full";
        return getListForURLAction(URL_DEVICES, params, "devices", sessionID, 1000,"getAllDevices");
    }

    /**
     * Add sensor to a device.
     *
     * This will create a device if it does not exist yet. This is the only way to create a new device.
     * A device is uniquely identified by a device ID or by a name and UUID.
     * This method will use the device ID as stored in the commonsense server.
     * There is a twin method available that takes a name and UUID instead if the device ID is not available.
     *
     * @param csSensorID	CommonSense sensor ID for the sensor. Cannot be empty.
     * @param deviceType    The name of the device.
     * @param UUID      	CommonSense device ID for the device. Cannot be empty.
     * @param sessionID		The sessionID of the current user. Cannot be empty.
     * @throws IOException, JSONException
     * @result				Whether or not the sensor was successfully added to the device.
     */
    public boolean addSensor(String csSensorID, String deviceType, String UUID, String sessionID)throws IOException, JSONException
    {
        if(csSensorID == null || csSensorID.isEmpty() || deviceType == null || deviceType.isEmpty()|| UUID == null || UUID.isEmpty()|| sessionID == null || sessionID.isEmpty())
            throw new IOException("invalid input of csSensorID or deviceType or UUID or sessionID");

        JSONObject sensor = new JSONObject();
        sensor.put("type", deviceType);
        sensor.put("uuid", UUID);
        JSONObject postData = new JSONObject();
        postData.put("device", sensor);

        String url = URL_SENSORS + "/" + csSensorID + "/" + URL_SENSOR_DEVICE;
               url = makeUrlFor(url,null);
        // perform actual request
        Map<String, String> response = request(url, postData, sessionID, HTTP_METHOD_POST);
        int code = checkResponseCode(response.get(RESPONSE_CODE), "addSensor");
        // check if the response code is 201 CREATED
        if(code != -2)
            throw new IOException("Incorrect response of addSensor from CommonSense: " + code);
        return (code == -2);
    }

    /**
     * Upload sensor data to commonsense
     *
     * Data can be coming from multiple sensors.
     *
     * @param data			JSONArray of datapoints.
     *                      Each datapoint should be a JSONObject with fields called "sensorID", "value", and "date".
     *                      These fields will be parsed into the correct form for uploading to commonsense.
     * @param sessionID		The sessionID of the current user. Cannot be empty.
     * @throws IOException, JSONException
     * @result				Whether or not the post of the data was succesfull.
     */
    public boolean postData(JSONArray data, String sessionID) throws IOException, JSONException
    {
        if(sessionID == null || sessionID.isEmpty() || data == null || data.length() == 0)
            throw new IOException("invalid input of data or sessionID");

        JSONObject postData = new JSONObject();
        postData.put("sensors",data);

        String url = makeUrlFor(URLE_UPLOAD_MULTIPLE_SENSORS,null);
        Map<String, String> response = request(url, postData, sessionID,"POST");
        int code = checkResponseCode(response.get(RESPONSE_CODE), "postData");
        if(code != -2)
            throw new IOException("Incorrect response of postData from CommonSense: " + code);
        return (code == -2);
    }

    /**
     * Download sensor data from commonsense from a certain date till now.
     *
     * The downloaded data will be passed as an JSONArray to the success callback method from which it can be further processed.
     *
     * @param sensorID		Identifier of the sensor from CommonSense for which to download the data. Cannot be empty.
     * @param fromDate	    Date from which to download data. Data points after this date will be included in the download.
     *                          Data points before this date will be ignored. Cannot be null.
     * @param sessionID		    The sessionID of the current user. Cannot be empty.
     * @throws IOException, JSONException
     * @result				    JSONArray with the resulting data. Each object is a JSONObject with the data as provided by the backend. Will be null if an error occurred.
     */
    public JSONArray getData(String sensorID, long fromDate, String sessionID) throws IOException, JSONException
    {
        if(fromDate == 0 || sensorID == null || sensorID.isEmpty() ||sessionID == null || sessionID.isEmpty())
            throw new IOException("invalid input of date or or sensorID or sessionID");
        String params = "?per_page=1000&start_date=" + fromDate + "&end_date=" + System.currentTimeMillis() +"&sort=DESC";
        String urlAction = URL_SENSORS + "/" + sensorID + "/" + URL_DATA;

        return getListForURLAction(urlAction, params, "data", sessionID, 1000, "getData");
    }


    /**
     * Performs request at CommonSense API. Returns the response code, content, and headers.
     *
     * @param urlString
     *            Complete URL to perform request to.
     * @param content
     *            (Optional) Content for the request. If the content is not null, the request method
     *            is automatically POST. The default method is GET.
     * @param sessionID
     *            (Optional) Session ID header for the request.
     * @param requestMethod
     *            The required method for the HTTP request, such as POST or GET.
     * @return Map with SenseApi.KEY_CONTENT and SenseApi.KEY_RESPONSE_CODE fields, plus fields for
     *         all response headers.
     * @throws IOException
     */

    public static Map<String, String> request( String urlString,
                                              JSONObject content, String sessionID, String requestMethod) throws IOException {

        HttpURLConnection urlConnection = null;
        HashMap<String, String> result = new HashMap<String, String>();
        try {
            final boolean compress = true;
            // open new URL connection channel.
            URL url = new URL(urlString);
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
            if(null != APP_KEY)
                urlConnection.setRequestProperty("APPLICATION-KEY", APP_KEY);

            if(sessionID != null)
                urlConnection.setRequestProperty("SESSION-ID", sessionID);

            if(requestMethod != null)
                urlConnection.setRequestMethod(requestMethod);

            // send content (if available)
            if (null != content) {
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
                    int contentLength = content.toString().getBytes("UTF-8").length;
                    urlConnection.setFixedLengthStreamingMode(contentLength);
                    urlConnection.setRequestProperty("Content-Length", "" + contentLength);
                    printout = new DataOutputStream(urlConnection.getOutputStream());
                }
                // Write the string in UTF-8 encoding
                printout.write(content.toString().getBytes("UTF-8"));
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
            result.put(RESPONSE_CONTENT, responseContent.toString());
            result.put(RESPONSE_CODE, Integer.toString(urlConnection.getResponseCode()));

            // clean up
            reader.close();
            reader = null;
            inputStream.close();
            inputStream = null;

            // get headers
            Map<String, List<String>> headerFields = urlConnection.getHeaderFields();
            String key, valueString;
            List<String> value;
            for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
                key = entry.getKey();
                value = entry.getValue();
                if (null != key && null != value) {
                    key = key.toLowerCase(Locale.ENGLISH);
                    valueString = value.toString();
                    valueString = valueString.substring(1, valueString.length() - 1);
                    result.put(key, valueString);
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
     * Upload sensor data to commonsense
     *
     * Data can be coming from multiple sensors.
     *
     * @param responseCode	The response code returned from the request
     * @param method		The method that requires code checking
     *
     * @result	The integer value of the result
     *
     */
    private static int checkResponseCode(String responseCode, String method){
        if ("403".equalsIgnoreCase(responseCode)) {
            Log.w(TAG, "CommonSense" + method + "refused! Response: forbidden!");
            return -3;
        } else if ("201".equalsIgnoreCase(responseCode)) {
            Log.e(TAG, "CommonSense" + method + "created! Response: " + responseCode);
            return -2;
        } else if (!"200".equalsIgnoreCase(responseCode)) {
            Log.w(TAG, "CommonSense" + method + "failed! Response: " + responseCode);
            return -1;
        } else {
            // received 200 response
            Log.e(TAG, "CommonSense" + method + "OK! ");
            return  0;
        }
    }


    /**
     * Make a url with the included action
     *
     * @param action	The required endpoint
     * @param appendix	The appendix of the endpoint, can be null
     *
     * @result	The required url
     */
    private static String makeCSRestUrlFor(String action, String appendix)
    {
        if(action == null || action.isEmpty()){
            return null;
        }
        if(appendix == null)
            appendix = "";
        String url = URL_BASE + "/" + action + URL_JSON_SUFFIX + appendix;
        return url;
    }

    /**
     * Make a url with the included action
     *
     * @param action	The required endpoint
     * @param appendix	The appendix of the endpoint, can be null
     *
     * @result	The required url
     */
    private static String makeUrlFor(String action, String appendix)
    {
        if(action == null || action.isEmpty()){
            return null;
        }
        if(appendix == null)
            appendix = "";
        String url;
        if(action.equals(URL_LOGIN) || action.equals(URL_LOGOUT)){
            url = URL_AUTH + "/" + action + appendix;
        }else{
            url = URL_AUTH + "/" + action + URL_JSON_SUFFIX + appendix;
        }

        return url;
    }

    /**
     * Helper function for getting a list from the cs-rest API
     *
     * @param urlAction     Part og url.
     * @param paramsString  Part of url.
     * @param resultKey     The name of the query field.
     * @param sessionID     The sessionID of the current user.
     * @param pageSize      The number of items in one page.
     * @param methodName    The name of the caller method.
     *                      
     * @throws IOException, JSONException
     * @result	The integer value of the result
     *
     */
   private static JSONArray getListForURLAction(String urlAction, String paramsString, String resultKey, String sessionID, int pageSize, String methodName) throws IOException, JSONException
   {
       int page = 0;
       JSONArray resultList = new JSONArray();
       boolean done = false;
       while (!done) {
           String params = "?page=" + page + paramsString;
           String url =  makeUrlFor(urlAction,params);
           Map<String, String> response = request(url, null, sessionID, HTTP_METHOD_GET);
           int codeResult = checkResponseCode(response.get(RESPONSE_CODE), methodName);
           if (codeResult != 0) {
               throw new IOException("Incorrect response of" + methodName + "from CommonSense");
           }
           // parse response and store the list
           JSONObject content = new JSONObject(response.get(RESPONSE_CONTENT));
           JSONArray paramList = content.getJSONArray(resultKey);
           // put the sensor list in the result array
           for (int i = 0; i < paramList.length(); i++) {
               resultList.put(paramList.getJSONObject(i));
           }
           if (paramList.length() < pageSize) {
               // all sensors received
               done = true;
           } else {
               // get the next page
               page++;
           }
       }
       return resultList;

   }
}
