/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.commonsense;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.telephony.TelephonyManager;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

import nl.sense_os.util.json.EncryptionHelper;
import nl.sense_os.service.SenseServiceStub;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Auth;
import nl.sense_os.service.constants.SensePrefs.Main.Advanced;
import nl.sense_os.service.constants.SenseUrls;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.push.GCMReceiver;

/**
 * Main interface for communicating with the CommonSense API.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class SenseApi {

    private static final long CACHE_REFRESH = 1000l * 60 * 60; // 1 hour
    private static final String TAG = "SenseApi";
    /** Device UUID for sensors that are not physical sensors, i.e. not connected to any device */
    public static final String NO_DEVICE_UUID = "no_device_uuid";
    /**
     * Key for getting the http response code from the Map object that is returned by
     * {@link SenseApi#request(Context, String, JSONObject, String)}
     */
    public static final String RESPONSE_CODE = "http response code";
    /**
     * Key for getting the response content from the Map object that is returned by
     * {@link SenseApi#request(Context, String, JSONObject, String)}
     */
    public static final String RESPONSE_CONTENT = "content";

    private static SharedPreferences sAuthPrefs;
    private static SharedPreferences sMainPrefs;
    private static TelephonyManager sTelManager;
    private static String APPLICATION_KEY;


     /**
     * @param context
     *            Context for accessing phone details
     * @return The default device type, i.e. the phone's model String
     */
    public static String getDefaultDeviceType(Context context) {
        if (null == sAuthPrefs) {
            sAuthPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS, Context.MODE_PRIVATE);
        }
        return sAuthPrefs.getString(Auth.DEVICE_TYPE, Build.MODEL);
    }

    /**
     * @param context
     *            Context for accessing phone details
     * @return The default device UUID, e.g. the phone's IMEI String
     */
    @TargetApi(9)
    public static String getDefaultDeviceUuid(Context context) {
        if (null == sTelManager) {
            sTelManager = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE));
        }
        String uuid = sTelManager.getDeviceId();
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

    /**
     * Get device configuration from commonSense
     *
     * @throws JSONException
     * @throws IOException
     */
    public static String getDeviceConfiguration(Context context) throws IOException, JSONException {
        if (null == sAuthPrefs) {
            sAuthPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS, Context.MODE_PRIVATE);
        }
        if (null == sMainPrefs) {
            sMainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
        }

        String cookie;
        try {
            cookie = getCookie(context);
        } catch (IllegalAccessException e) {
            cookie = null;
        }
        boolean devMode = sMainPrefs.getBoolean(Advanced.DEV_MODE, false);
        String url = devMode ? SenseUrls.DEVICE_CONFIGURATION_DEV : SenseUrls.DEVICE_CONFIGURATION;

        // Get the device ID
        String device_id = getDeviceId(context);

        url = url.replaceFirst("%1", device_id);

        Map<String, String> response = SenseApi.request(context, url, null, cookie);
        String responseCode = response.get(RESPONSE_CODE);
        if (!"200".equals(responseCode)) {
            Log.w(TAG, "Failed to get device configuration! Response code: " + responseCode);
            throw new IOException("Incorrect response from CommonSense: " + responseCode);
        }

        return response.get(RESPONSE_CONTENT);
    }

    /**
     * Get specific configuration from commonSense
     *
     * @throws JSONException
     * @throws IOException
     *
     */
    public static String getDeviceConfiguration(Context context, String configuration_id)
            throws IOException, JSONException {
        final SharedPreferences prefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
                Context.MODE_PRIVATE);

        String cookie;
        try {
            cookie = getCookie(context);
        } catch (IllegalAccessException e) {
            cookie = null;
        }
        boolean devMode = prefs.getBoolean(Advanced.DEV_MODE, false);
        String url = devMode ? SenseUrls.CONFIGURATION_DEV : SenseUrls.CONFIGURATION;

        url = url.replaceFirst("%1", configuration_id);

        Map<String, String> response = SenseApi.request(context, url, null, cookie);
        String responseCode = response.get(RESPONSE_CODE);
        if (!"200".equals(responseCode)) {
            Log.w(TAG, "Failed to get Requirement! Response code: " + responseCode);
            throw new IOException("Incorrect response from CommonSense: " + responseCode);
        }

        return response.get(RESPONSE_CONTENT);
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

        if (null == sAuthPrefs) {
            sAuthPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS, Context.MODE_PRIVATE);
        }
        if (null == sMainPrefs) {
            sMainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
        }

        String cookie;
        try {
            cookie = getCookie(context);
        } catch (IllegalAccessException e) {
            cookie = null;
        }
        boolean devMode = sMainPrefs.getBoolean(Advanced.DEV_MODE, false);
        String url = devMode ? SenseUrls.DEVICES_DEV : SenseUrls.DEVICES;

        Map<String, String> response = SenseApi.request(context, url, null, cookie);
        String responseCode = response.get(RESPONSE_CODE);
        if (!"200".equals(responseCode)) {
            Log.w(TAG, "Failed to get devices data: " + responseCode);
            throw new IOException("Incorrect response from CommonSense: " + responseCode);
        }

        // check registration result
        JSONObject res = new JSONObject(response.get(RESPONSE_CONTENT));
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
    * @param context
    *            Context for getting preferences
    * @return The current CommonSense session ID
    * @throws IllegalAccessException
    *             if the app ID is not valid
    */
    public static String getCookie(Context context) throws IllegalAccessException {
    	if (null == sAuthPrefs) {
    		sAuthPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS, Context.MODE_PRIVATE);
    	}
        if (null == sMainPrefs) {
            sMainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
        }

        String cookie = sAuthPrefs.getString(Auth.LOGIN_COOKIE, null);

        boolean encrypt_credential = sMainPrefs.getBoolean(Advanced.ENCRYPT_CREDENTIAL, false);
        if (encrypt_credential) {
            EncryptionHelper decryptor = new EncryptionHelper(context);
            try {
                cookie = decryptor.decrypt(cookie);
            } catch (EncryptionHelper.EncryptionHelperException e) {
                Log.w(TAG, "Error decrypting cookie. Assume data is not encrypted");
            }
        }

    	return cookie;
    }
    
    /**
     * @param context
     *            Context for getting preferences
     * @return The current CommonSense session ID
     * @throws IllegalAccessException
     *             if the app ID is not valid
     */
    public static String getSessionId(Context context) throws IllegalAccessException {
        if (null == sAuthPrefs) {
            sAuthPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS, Context.MODE_PRIVATE);
        }
        if (null == sMainPrefs) {
            sMainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
        }

        String session_id = sAuthPrefs.getString(Auth.LOGIN_SESSION_ID, null);

        boolean encrypt_credential = sMainPrefs.getBoolean(Advanced.ENCRYPT_CREDENTIAL, false);
        if (encrypt_credential) {
            EncryptionHelper decryptor = new EncryptionHelper(context);
            try {
                session_id = decryptor.decrypt(session_id);
            } catch (EncryptionHelper.EncryptionHelperException e) {
                Log.w(TAG, "Error decrypting session id. Assume data is not encrypted");
            }
        }

        return session_id;
    }

    /**
     * Gets user details from CommonSense
     * 
     * @param context
     *            Context for getting preferences
     * @return JSONObject with user if successful, null otherwise
     * @throws JSONException
     *             In case of unparseable response from CommonSense
     * @throws IOException
     *             In case of communication failure to CommonSense
     */
    public static JSONObject getUser(Context context) throws IOException, JSONException {
        if (null == sAuthPrefs) {
            sAuthPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS, Context.MODE_PRIVATE);
        }
        if (null == sMainPrefs) {
            sMainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
        }

        String cookie;
        try {
            cookie = getCookie(context);
        } catch (IllegalAccessException e) {
            cookie = null;
        }
        boolean devMode = sMainPrefs.getBoolean(Advanced.DEV_MODE, false);

        String url = devMode ? SenseUrls.CURRENT_USER_DEV : SenseUrls.CURRENT_USER;

        // perform actual request
        Map<String, String> response = SenseApi.request(context, url, null, cookie);

        String responseCode = response.get(RESPONSE_CODE);
        JSONObject result = null;
        if ("200".equalsIgnoreCase(responseCode)) {
            result = new JSONObject(response.get(RESPONSE_CONTENT)).getJSONObject("user");
        } else {
            Log.w(TAG, "Failed to get user! Response code: " + responseCode);
            throw new IOException("Incorrect response from CommonSense: " + responseCode);
        }
        return result;
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
     * Joins a group
     *
     * @param context
     *            Context for getting preferences
     * @param groupId
     *            Id of the group to join
     * @return true if joined successfully, false otherwise
     * @throws JSONException
     *             In case of unparseable response from CommonSense
     * @throws IOException
     *             In case of communication failure to CommonSense
     */
    public static boolean joinGroup(Context context, String groupId) throws JSONException,
            IOException {
        if (null == sAuthPrefs) {
            sAuthPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS, Context.MODE_PRIVATE);
        }
        if (null == sMainPrefs) {
            sMainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
        }

        String cookie;
        try {
            cookie = getCookie(context);
        } catch (IllegalAccessException e) {
            cookie = null;
        }
        boolean devMode = sMainPrefs.getBoolean(Advanced.DEV_MODE, false);

        // get userId
        String userId = getUser(context).getString("id");

        String url = devMode ? SenseUrls.GROUP_USERS_DEV : SenseUrls.GROUP_USERS;
        url = url.replaceFirst("%1", groupId);

        // create JSON object to POST
        final JSONObject data = new JSONObject();
        final JSONArray users = new JSONArray();
        final JSONObject item = new JSONObject();
        final JSONObject user = new JSONObject();
        user.put("id", userId);
        item.put("user", user);
        users.put(item);
        data.put("users", users);

        // perform actual request
        Map<String, String> response = SenseApi.request(context, url, data, cookie);

        String responseCode = response.get(RESPONSE_CODE);
        boolean result = false;
        if ("201".equalsIgnoreCase(responseCode)) {
            result = true;
        } else {
            Log.w(TAG, "Failed to join group! Response code: " + responseCode + "Response: "
                    + response);
            result = false;
        }

        return result;
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
     * @see SenseServiceStub#changeLogin(String, String, nl.sense_os.service.ISenseServiceCallback)
     */
    public static int login(Context context, String username, String password)
            throws JSONException, IOException {

        // preferences
        if (null == sAuthPrefs) {
            sAuthPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS, Context.MODE_PRIVATE);
        }
        if (null == sMainPrefs) {
            sMainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
        }

        boolean devMode = sMainPrefs.getBoolean(Advanced.DEV_MODE, false);
        if (devMode) {
            Log.v(TAG, "Using development server to log in");
        }
        final String url = devMode ? SenseUrls.LOGIN_DEV : SenseUrls.LOGIN;
        final JSONObject user = new JSONObject();
        user.put("username", username);
        user.put("password", password);

        // TODO disable compressed login
        Boolean useCompressed = sMainPrefs.getBoolean(SensePrefs.Main.Advanced.COMPRESS, false);
        sMainPrefs.edit().putBoolean(SensePrefs.Main.Advanced.COMPRESS, false).commit();
        // perform actual request
        // set the application id for the login call
        APPLICATION_KEY = sMainPrefs.getString(SensePrefs.Main.APPLICATION_KEY, null);
        Map<String, String> response = request(context, url, user, null);
        APPLICATION_KEY = null;
        // set previous value
        sMainPrefs.edit().putBoolean(SensePrefs.Main.Advanced.COMPRESS, useCompressed).commit();

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

        // handle result
        Editor authEditor = sAuthPrefs.edit();
        switch (result) {
        case 0: // logged in
            boolean encrypt_credential = sMainPrefs.getBoolean(Advanced.ENCRYPT_CREDENTIAL, false);
            if (encrypt_credential) {
                EncryptionHelper encryptor = new EncryptionHelper(context);
                cookie = encryptor.encrypt(cookie);
                session_id = encryptor.encrypt(session_id);
            }
            authEditor.putString(Auth.LOGIN_COOKIE, cookie);
            authEditor.putString(Auth.LOGIN_SESSION_ID, session_id);
            authEditor.commit();
            break;
        case -1: // error
            break;
        case -2: // unauthorized
            authEditor.remove(Auth.LOGIN_COOKIE);
            authEditor.remove(Auth.LOGIN_SESSION_ID);
            authEditor.commit();
            break;
        default:
            Log.e(TAG, "Unexpected login result: " + result);
        }

        return result;
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

        if (null == sAuthPrefs) {
            sAuthPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS, Context.MODE_PRIVATE);
        }
        if (null == sMainPrefs) {
            sMainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
        }

        // Check if already synced with common sense
        String pref_registration_id = sAuthPrefs.getString(Auth.GCM_REGISTRATION_ID, "");

        if (registrationId.equals(pref_registration_id)) {
            // GCM registration id is already sync with commonSense
            return;
        }

        String cookie;
        try {
            cookie = getCookie(context);
        } catch (IllegalAccessException e) {
            cookie = null;
        }
        boolean devMode = sMainPrefs.getBoolean(Advanced.DEV_MODE, false);
        String url = devMode ? SenseUrls.REGISTER_GCM_ID_DEV : SenseUrls.REGISTER_GCM_ID;

        // Get the device ID
        String device_id = getDeviceId(context);

        if (device_id == null) {
            // register GCM ID failed, can not get the device ID for this device
            return;
        }

        url = url.replaceFirst("%1", device_id);

        final JSONObject data = new JSONObject();
        data.put("registration_id", registrationId);

        Map<String, String> response = SenseApi.request(context, url, data, cookie);
        String responseCode = response.get(RESPONSE_CODE);
        if (!"200".equals(responseCode)) {
            throw new IOException("Incorrect response from CommonSense: " + responseCode);
        }

        // check registration result
        JSONObject res = new JSONObject(response.get(RESPONSE_CONTENT));
        if (!registrationId.equals(res.getJSONObject("device").getString(GCMReceiver.KEY_GCM_ID))) {
            throw new IllegalStateException("GCM registration_id not match with response");
        }

        Editor authEditor = sAuthPrefs.edit();
        authEditor.putString(Auth.GCM_REGISTRATION_ID, registrationId);
        authEditor.commit();
    }

    /**
     * Tries to register a new user at CommonSense.
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
     * @see SenseServiceStub#register(String, String, String, String, String, String, String,
     *      String, String, nl.sense_os.service.ISenseServiceCallback)
     */
    public static int registerUser(Context context, String username, String password, String name,
            String surname, String email, String mobile) throws JSONException, IOException {

        if (null == sMainPrefs) {
            sMainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
        }
        boolean devMode = sMainPrefs.getBoolean(Advanced.DEV_MODE, false);

        final String url = devMode ? SenseUrls.REGISTER_DEV : SenseUrls.REGISTER;

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

        String responseCode = response.get(RESPONSE_CODE);
        int result = -1;
        if ("201".equalsIgnoreCase(responseCode)) {
            result = 0;
        } else if ("409".equalsIgnoreCase(responseCode)) {
            Log.w(TAG, "Failed to register new user! User already exists");
            result = -2;
        } else {
            Log.w(TAG, "Failed to register new user! Response code: " + responseCode);
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
     * @return Map with SenseApi.KEY_CONTENT and SenseApi.KEY_RESPONSE_CODE fields, plus fields for
     *         all response headers.
     * @throws IOException
     */
    public static Map<String, String> request(Context context, String urlString,
            JSONObject content, String cookie) throws IOException {

        HttpURLConnection urlConnection = null;
        HashMap<String, String> result = new HashMap<String, String>();
        try {

            // get compression preference
            if (null == sMainPrefs) {
                sMainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
                        Context.MODE_PRIVATE);
            }
            final boolean compress = sMainPrefs.getBoolean(Advanced.COMPRESS, true);

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
            if(null != APPLICATION_KEY)
                urlConnection.setRequestProperty("APPLICATION-KEY", APPLICATION_KEY);

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
            for (Entry<String, List<String>> entry : headerFields.entrySet()) {
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
     * Request a password reset for the given email address.
     * 
     * This function does not use the authentication API and is therefore deprecated
     * 
     * @param context
     *            Application context, used for getting preferences.
     * @param email
     *            Email address for the account that you want to regain access to.
     * @return <code>true</code> if the request wasw accepted
     * @throws IOException
     *             In case of communication failure to CommonSense
     * @throws JSONException
     *             In case of unparseable response from CommonSense
     */
    @Deprecated
    public static boolean resetPassword(Context context, String email) throws IOException,
            JSONException {

        if (null == sMainPrefs) {
            sMainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
        }
        boolean devMode = sMainPrefs.getBoolean(Advanced.DEV_MODE, false);

        // prepare request
        String url = devMode ? SenseUrls.FORGOT_PASSWORD_DEV : SenseUrls.FORGOT_PASSWORD;
        JSONObject content = new JSONObject();
        content.put("email", email);

        // perform request
        Map<String, String> result = request(context, url, content, null);

        // check response code
        String responseCode = result.get(RESPONSE_CODE);
        if ("200".equals(responseCode)) {
            return true;
        } else {
            Log.w(TAG, "Failed to request password reset! Response code " + responseCode);
            return false;
        }
    }

    /**
     * Request a password reset for the given username.
     * 
     * @param context
     *            Application context, used for getting preferences.
     * @param email
     *            Email address for the account that you want to regain access to.
     * @return <code>true</code> if the request wasw accepted
     * @throws IOException
     *             In case of communication failure to CommonSense
     * @throws JSONException
     *             In case of unparseable response from CommonSense
     */
    public static boolean resetPasswordRequest(Context context, String username) throws IOException,
            JSONException {

        if (null == sMainPrefs) {
            sMainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
        }
        boolean devMode = sMainPrefs.getBoolean(Advanced.DEV_MODE, false);

        // prepare request
        String url = devMode ? SenseUrls.RESET_PASSWORD_REQUEST_DEV : SenseUrls.RESET_PASSWORD_REQUEST;
        JSONObject content = new JSONObject();
        content.put("username", username);

        // TODO disable compressed 
        Boolean useCompressed = sMainPrefs.getBoolean(SensePrefs.Main.Advanced.COMPRESS, false);
        sMainPrefs.edit().putBoolean(SensePrefs.Main.Advanced.COMPRESS, false).commit();
        // perform actual request
        // set the application id for the login call
        APPLICATION_KEY = sMainPrefs.getString(SensePrefs.Main.APPLICATION_KEY, null);
        // perform request
        Map<String, String> result = request(context, url, content, null);
        APPLICATION_KEY = null;
        // set previous value
        sMainPrefs.edit().putBoolean(SensePrefs.Main.Advanced.COMPRESS, useCompressed).commit();

        // check response code
        String responseCode = result.get(RESPONSE_CODE);
        if ("202".equals(responseCode)) {
            return true;
        } else {
            Log.w(TAG, "Failed to request password reset! Response code " + responseCode);
            return false;
        }
    }


    /**
     * Change the password of the current user.
     *
     * @param context
     *            Application context, used for getting preferences.
     * @param current_password
     *            The current (hashed) password of the user
     * @param new_password
     *            The new (hashed) password of the user
     * @return <code>true</code> if the password was changed
     * @throws IOException
     *             In case of communication failure to CommonSense
     * @throws JSONException
     *             In case of unparseable response from CommonSense
     */
    public static boolean changePassword(Context context, String current_password, String new_password) throws IOException,
    JSONException
    {
        if (null == sMainPrefs) {
            sMainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
        }
        boolean devMode = sMainPrefs.getBoolean(Advanced.DEV_MODE, false);

        // prepare request
        String url = devMode ? SenseUrls.CHANGE_PASSWORD_DEV : SenseUrls.CHANGE_PASSWORD;
        String cookie;
        try {
            cookie = getCookie(context);
        } catch (IllegalAccessException e) {
            cookie = null;
        }
        JSONObject content = new JSONObject();
        content.put("current_password", current_password);
        content.put("new_password", new_password);

        // perform request
        Map<String, String> result = request(context, url, content, cookie);

        // check response code
        String responseCode = result.get(RESPONSE_CODE);
        if ("200".equals(responseCode)) {
            return true;
        } else {
            Log.w(TAG, "Failed to change the password! Response code " + responseCode);
            return false;
        }
    }

}
