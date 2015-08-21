package nl.sense_os.datastorageengine;

import android.content.Context;
import android.content.SharedPreferences;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

import nl.sense_os.service.EncryptionHelper;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SenseUrls;

/**
 * Created by fei on 20/08/15.
 */
public class CommonSenseProxy {

    private static final String TAG = "CommonSenseProxy";
    private static String appKey;				  //The app key
    private String urlBase;				  //The base url to use, will differ based on whether to use live or staging server
    private String urlAuth;				  //The base url to use for authentication, will differ based on whether to use live or staging server
    private int requestTimeoutInterval;	  //Timeout interval in seconds

    public static final String kUrlBaseURLLive              = "https://api.sense-os.nl";
    public static final String kUrlBaseURLStaging           = "http://api.staging.sense-os.nl";
    public static final String kUrlAuthenticationLive       = "https://auth-api.sense-os.nl/v1";
    public static final String kUrlAuthenticationStaging    = "http://auth-api.staging.sense-os.nl/v1";

    public static final String kUrlLogin					= "login";
    public static final String kUrlLogout                   = "logout";
    public static final String kUrlSensorDevice             = "device";
    public static final String kUrlSensors                  = "sensors";
    public static final String kUrlUsers                    = "users";
    public static final String kUrlUploadMultipleSensors    = "sensors/data";
    public static final String kUrlData                     = "data";
    public static final String kUrlDevices                  = "devices";

    public static final String kUrlJsonSuffix               = ".json";

    /**
     * Key for getting the http response code from the Map object that is returned by
     * {@link nl.sense_os.service.commonsense.SenseApi#request(Context, String, JSONObject, String)}
     */
    public static final String RESPONSE_CODE = "http response code";
    /**
     * Key for getting the response content from the Map object that is returned by
     * {@link nl.sense_os.service.commonsense.SenseApi#request(Context, String, JSONObject, String)}
     */
    public static final String RESPONSE_CONTENT = "content";

    //private static SharedPreferences sAuthPrefs;
    //private static SharedPreferences sMainPrefs;
    /**
     * Default constructor for the DSECommonSenseProxy.
     *
     * Takes an app key that will be used throughout the proxies lifetime. Needs to know whether to talk to the live server or the staging server.
     * This cannot be changed during the proxies lifetime. If you need to change this you simply init a new commonsense proxy.
     *
     * @param useLiveServer	If true, the live server will be used. If false, the staging server will be used.
     * @param theAppKey		An application key that identifies the application to the commonsense server. Cannot be empty.
     */
     //- (id) initAndUseLiveServer: (BOOL) useLiveServer withAppKey: (String *) theAppKey;
    public void CommonSenseProxy (boolean useLiveServer, String theAppKey)
    {
        appKey = theAppKey;

        if(useLiveServer) {
            urlBase     = kUrlBaseURLLive;
            urlAuth		= kUrlAuthenticationLive;
        } else {
            urlBase     = kUrlBaseURLStaging;
            urlAuth		= kUrlAuthenticationStaging;
        }

    }


    /**
     * Login a user
     *
     * If a user is already logged in this call will fail. That user should be logged out first before a new login attempt can be made. Otherwise, it will throw an IOException.
     *
     * @param username		A user account in commonsense is uniquely identified by a username. Cannot be empty.
     * @param password		A password in commonsense does not have any specific requirements.
     *                      It will be MD5 hashed before sending to the server so the user does not have to provide a hashed password. Cannot be empty.
     * @throws IOException and JSONException
     * @return				Session ID. Will be null if the call fails.
     */
     // - (String *) loginUser: (NSString *) username andPassword: (NSString *) password andError: (NSError **) error;
    public String loginUser( String username, String password) throws IOException, JSONException
    {
        if(username == null || username.isEmpty() || password == null || password.isEmpty())
            throw new IOException("invalid input of username or password");

        final String url = urlAuth + "/" + kUrlLogin;

        final JSONObject user = new JSONObject();
        user.put("username", username);
        user.put("password", password);

        // TODO disable compressed login
        Map<String, String> response = request( url, user, null);

        // if response code is not 200 (OK), the login was incorrect
        String responseCode = response.get(RESPONSE_CODE);
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

        // create a cookie from the session_id
        String session_id = response.get("session-id");
        String cookie = "";
        if (result == 0 && session_id == null) {
            // something went horribly wrong
            Log.w(TAG, "CommonSense login failed: no cookie received?!");
            result = -1;
        }
        else
            cookie = "session_id="+session_id+"; domain=.sense-os.nl";

        return session_id;
    }


    /**
     * Logout the currently logged in user.
     *
     * @param sessionID		The sessionID of the user to logout. Cannot be empty.
     * @throws IOException
     * @return				Whether or not the logout finished succesfully.
     */
     //- (BOOL) logoutCurrentUserWithSessionID: (NSString *) sessionID andError: (NSError **) error;
    public static boolean logoutCurrentUserWithSessionID(String sessionID) throws IOException
    {
        if(sessionID == null || sessionID.isEmpty())
            throw new IOException("invalid input of session ID");
    }

    /**
     * Create a new sensor in the commonsense backend
     *
     * Each sensor in commonsense is uniquely identified by a name and devicetype combination. If this combination already exists in the commonsense backend this call will fail.
     *
     * @param name			Name of the sensor to create. This will be used to identify the sensor. Required.
     * @param displayName		Extra field to make sensor name more readable if necessary when displaying it. Not required.
     * @param deviceType		Name of the device type the sensor belongs to. Required.
     * @param dataType		Type of data that the sensor stores. Required.
     * @param dataStructure	Structure of the data in the data; can be used to specify the JSON structure in the sensor. Not required.
     * @param sessionID		The sessionID of the current user. Cannot be empty.
     * @throws IOException
     *@result				Dictionary with the information of the created sensor.
     */
     //- (NSDictionary *) createSensorWithName: (NSString *) name andDisplayName: (NSString *) displayName andDeviceType: (NSString *) deviceType andDataType: (NSString *) dataType andDataStructure: (NSString *) dataStructure andSessionID: (NSString *) sessionID andError: (NSError **) error;
    public static HashMap<String, JSONObject> createSensorWithName(String name, String displayName, String deviceType, String dataType, String dataStructure, String sessionID) throws IOException
    {}
    
    /**
     * Get all sensors for the currently logged in user.
     *
     * This will fetch all the sensors for the current user and pass them on to the success callback as an NSArray. Each element in the array will contain an NSDictionary with data from one sensor.
     *
     * @param sessionID		The sessionID of the current user. Cannot be empty.
     * @throws IOException
     * @result				Array of sensors. Each object will be an NSDictionary with the resulting sensor information. Will be null if an error occurs.
     */
     //- (NSArray *) getSensorsWithSessionID: (NSString *) sessionID andError: (NSError **) error;
    public static JSONArray getSensorsWithSessionID(String sessionID) throws IOException
    {}    

    /**
     * Get all devices for the currently logged in user.
     *
     * This will fetch all the devices for the current user and pass them on to the success callback as an NSArray. Each element in the array will contain an NSDictionary with data from one device.
     *
     * @param sessionID		The sessionID of the current user. Cannot be empty.
     * @throws IOException
     * @result				Array of devices. Each object will be an NSDictionary with the resulting device information. Will be null if an error occurs.
     */
     //- (NSArray *) getDevicesWithSessionID: (NSString *) sessionID andError: (NSError **) error;
    public static JSONArray getDevicesWithSessionID(String sessionID) throws IOException
    {}

    /**
     * Add sensor to a device.
     *
     * This will create a device if it doesn‘t exist yet. This is the only way to create a new device. A device is uniquely identified by a device ID or by a name and UUID. This method will use the device ID as stored in the commonsense server. There is a twin method available that takes a name and UUID instead if the device ID is not available.
     *
     * @param csSensorID		CommonSense sensor ID for the sensor. Cannot be empty.
     * @param csDeviceID		CommonSense device ID for the device. Cannot be empty.
     * @param sessionID		The sessionID of the current user. Cannot be empty.
     * @throws IOException
     * @result				Whether or not the sensor was successfully added to the device.
     */
     //- (BOOL) addSensorWithID: (NSString *) csSensorID toDeviceWithID: (NSString *) csDeviceID andSessionID: (NSString *) sessionID andError: (NSError **) error;
    public static boolean addSensorWithID(String csSensorID, String csDeviceID, String sessionID)
    {}

    /**
     * Add sensor to a device.
     *
     * This will create a device if it doesnt exist yet. This is the only way to create a new device. A device is uniquely identified by a device ID or by a name and UUID. This method will use the name and UUID. There is a twin method available that takes a device ID. If the device ID is available this is the preferred method to use.
     *
     * @warning iOS does not provide consistent UUIDs anymore. Whenever an app is deinstalled and later reinstalled on the same device, the UUID retrieved from iOS might be different. This might cause a new device to be created in the backend when using this method to add a sensor to a device.
     *
     * @param csSensorID		CommonSense sensor ID for the sensor. Cannot be empty.
     * @param name			Unique name for the device. Cannot be empty.
     * @param UUID			UUID for the device. Cannot be empty.
     * @param sessionID		The sessionID of the current user. Cannot be empty.
     * @throws IOException
     * @result				Whether or not the sensor was successfully added to the device.
     */
     //- (BOOL) addSensorWithID: (NSString *) csSensorID toDeviceWithType: (NSString *) deviceType andUUID: (NSString *) UUID andSessionID: (NSString *) sessionID andError: (NSError **) error;
    public static boolean addSensorWithID(String csSensorID, String name, String UUID, String sessionID) throws IOException
    {}

    /**
     * Upload sensor data to commonsense
     *
     * Data can be coming from multiple sensors.
     *
     * @param data			NSArray of datapoints. Each datapoint should be an NSDictionary with fields called "sensorID" (NSString), "value" (id), and "date" (NSDate *). These fields will be parsed into the correct form for uploading to commonsense.
     * @param sessionID		The sessionID of the current user. Cannot be empty.
     * @throws IOException
     * @result				Whether or not the post of the data was succesfull.
     */
     //- (BOOL) postData: (NSArray *) data withSessionID: (NSString *) sessionID andError: (NSError **) error;
    public static boolean postData(JSONArray data, String sessionID) throws IOException
    {}

    /**
     * Download sensor data from commonsense from a certain date till now.
     *
     * The downloaded data will be passed as an NSArray * to the success callback method from which it can be further processed.
     *
     * @param csSensorID		Identifier of the sensor from CommonSense for which to download the data. Cannot be empty.
     * @param fromDateDate	Date from which to download data. Datapoints after this date will be included in the download. Datapoints from before this date will be ignored. Cannot be null.
     * @param sessionID		The sessionID of the current user. Cannot be empty.
     * @throws IOException
     * @result				NSArray with the resulting data. Each object is an NSDictionary with the data as provided by the backend. Will be null if an error occured.
     */
     //- (NSArray *) getDataForSensor: (NSString *) csSensorID fromDate: (NSDate *) startDate withSessionID: (NSString *) sessionID andError: (NSError **) error;
    public static JSONArray getDataForSensor(String csSensorID, Date fromDateDate, String sessionID) throws IOException
    {}


    /**
     * Performs request at CommonSense API. Returns the response code, content, and headers.
     *
     * @param urlString
     *            Complete URL to perform request to.
     * @param content
     *            (Optional) Content for the request. If the content is not null, the request method
     *            is automatically POST. The default method is GET.
     * @param cookie
     *            (Optional) Cookie header for the request.
     * @return Map with SenseApi.KEY_CONTENT and SenseApi.KEY_RESPONSE_CODE fields, plus fields for
     *         all response headers.
     * @throws IOException
     */

    public static Map<String, String> request( String urlString,
                                              JSONObject content, String cookie) throws IOException {

        HttpURLConnection urlConnection = null;
        HashMap<String, String> result = new HashMap<String, String>();
        try {

            // get compression preference
//            if (null == sMainPrefs) {
//                sMainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
//                        Context.MODE_PRIVATE);
//            }
            //final boolean compress = sMainPrefs.getBoolean(SensePrefs.Main.Advanced.COMPRESS, true);


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

            // set cookie (if available)
            if (null != cookie) {
                urlConnection.setRequestProperty("Cookie", cookie);
            }

            // set the application id
            if(null != appKey)
                urlConnection.setRequestProperty("APPLICATION-KEY", appKey);

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



}
