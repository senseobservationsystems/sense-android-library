package sensei.sense_os.nl.sensedemo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.RemoteException;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

import nl.sense_os.service.EncryptionHelper;
import nl.sense_os.service.SenseService;
import nl.sense_os.service.commonsense.DefaultSensorRegistrator;
import nl.sense_os.service.commonsense.SenseApi;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensorData;
import nl.sense_os.service.ctrl.Controller;
import nl.sense_os.service.storage.LocalStorage;

/**
 * Created by ted on 10/27/15.
 */
public class StoreAndSyncData {

    private static final String TAG = "StoreAndSyncData";
    public static String sensorName = "test_sensor";
    public static String description = "Test sensor";
    public static String dataType = SenseDataTypes.JSON;
    public static String displayName = "Test Sensor";

    public static boolean setup(Context context)
    {
        setSyncRateOften(context);
        startDataSyncer(context);
        boolean success = createSensor(context);
        success &= shareSensor(context, SetupUser.publicGroupID);
        success &= shareSensor(context, SetupUser.privateGroupID);
        return success;
    }

    public static boolean shareSensor(Context context, String groupID)
    {
        boolean success = true;
        try {
            // try to get the sensor id if available
            String deviceUUID = SenseApi.getDefaultDeviceUuid(context);
            String sensorID =  SenseApi.getSensorId(context, sensorName, description, dataType, deviceUUID);
            Log.d(TAG, "SensorID:"+sensorID);
            if(sensorID != null)
                success &= SenseApi.shareSensor(context, sensorID, groupID);
            if(!success)
                throw new RuntimeException("Error sharing sensor id: "+sensorID+" to group: "+groupID);
        } catch (Exception e) {
            String message = e.getMessage() != null? e.getMessage() : e.toString();
            Log.e(TAG,message);
            return false;
        }
        return success;
    }

    /**
     * Will create a test sensor if it does not already exists.
     *
     * @param context
     */
    public static boolean createSensor(Context context)
    {
        DefaultSensorRegistrator defaultSensorRegistrator = new DefaultSensorRegistrator(context);

        String value = "{\"status\":\"OK\"}";
        String deviceType = SenseApi.getDefaultDeviceType(context);
        String deviceUUID = SenseApi.getDefaultDeviceUuid(context);
        return defaultSensorRegistrator.checkSensor(sensorName, displayName, dataType, description, value, deviceType, deviceUUID);
    }

    /**
     * Store a sensor data value
     * @param context A context object
     * @param statusValue The status value to upload
     */
    public static void storeSensorData(Context context, String statusValue)
    {
        String value = "{\"status\":\""+statusValue+"\"}";
        Intent sensorData = new Intent(context.getString(nl.sense_os.service.R.string.action_sense_new_data));
        sensorData.putExtra(SensorData.DataPoint.SENSOR_NAME, sensorName);
        sensorData.putExtra(SensorData.DataPoint.SENSOR_DESCRIPTION, description);
        sensorData.putExtra(SensorData.DataPoint.DISPLAY_NAME, displayName);
        sensorData.putExtra(SensorData.DataPoint.VALUE, value);
        sensorData.putExtra(SensorData.DataPoint.DATA_TYPE, SenseDataTypes.JSON);
        sensorData.putExtra(SensorData.DataPoint.TIMESTAMP, System.currentTimeMillis());
        sensorData.setPackage(context.getPackageName());
        context.startService(sensorData);
    }

    public static boolean sendDataManually(Context context, String statusValue){
        try {
            // try to get the sensor id if available
            String deviceUUID = SenseApi.getDefaultDeviceUuid(context);
            String sensorID =  SenseApi.getSensorId(context, sensorName, description, dataType, deviceUUID);
            if(sensorID == null)
                throw new RuntimeException("Error missing sensor id, the sensor needs to be created first");

            // create the data object
            JSONObject value = new JSONObject("{\"status\":\""+statusValue+"\"}");
            JSONObject sensorData = new JSONObject();
            JSONArray dataArray = new JSONArray();
            JSONObject data = new JSONObject();
            data.put("value", value);
            data.put("date", System.currentTimeMillis());
            dataArray.put(data);
            sensorData.put("data", dataArray);

            // get the url to post the data to
            String url = SenseApi.getSensorUrl(context, sensorName, description, dataType, deviceUUID);
            // get the session id cookie

            // send the data
            Map<String, String> response = SenseApi.request(context, url, sensorData, getCookie(context));

            // Error when sending
            if ((response == null) || !response.get(SenseApi.RESPONSE_CODE).equals("201")) {

                // if un-authorized: relogin
                if ((response != null) && response.get(SenseApi.RESPONSE_CODE).equals("403")) {
                    final Intent serviceIntent = new Intent(context.getString(R.string.action_sense_service));
                    serviceIntent.putExtra(SenseService.EXTRA_RELOGIN, true);
                    serviceIntent.setPackage(context.getPackageName());
                    context.startService(serviceIntent);
                }

                // Show the HTTP response Code
                if (response != null) {
                    throw new RemoteException("Failed to send '" + sensorName+ "' data. Response code:"
                                    + response.get(SenseApi.RESPONSE_CODE) + ", Response content: '"
                                    + response.get(SenseApi.RESPONSE_CONTENT) + "'\nData will be retried");
                } else {
                    throw new RemoteException("Failed to send '" + sensorName+ "' data.");
                }
            }
            return true;

        } catch (Exception e) {
            String message = e.getMessage() != null? e.getMessage() : e.toString();
            Log.e(TAG,message);
            return false;
        }
    }

    /**
     * Get the cookie stored after the last login
     * @param context An Android context
     * @return The cookie string
     */
    public static String getCookie(Context context){
        SharedPreferences authPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS, context.MODE_PRIVATE);
        String cookie = authPrefs.getString(SensePrefs.Auth.LOGIN_COOKIE, null);

        SharedPreferences mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS,context.MODE_PRIVATE);

        boolean encrypt_credential = mainPrefs.getBoolean(SensePrefs.Main.Advanced.ENCRYPT_CREDENTIAL, false);

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
     * Upload data that has not been uploaded yet
     * @param context A context object
     */
    public static void flushData(Context context)
    {
        // start the MsgHandler for syncing in the background
        Intent sendDataIntent = new Intent(context.getString(nl.sense_os.service.R.string.action_sense_send_data));
        sendDataIntent.setPackage(context.getPackageName());
        context.startService(sendDataIntent);
    }

    /**
     * Starts the scheduler for data transmission to api.sense-os.nl
     * @param context A context object
     */
    public static void startDataSyncer(Context context)
    {
        // start the MsgHandler by sending the ned data
        flushData(context);
        // schedule the transmission with the controller
        Controller controller = Controller.getController(context);
        controller.scheduleTransmissions();
    }

    /**
     * Set sync rate to OFTEN every minute
     */
    public static void setSyncRateOften(Context context)
    {
        SharedPreferences mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
        mainPrefs.edit().putString(SensePrefs.Main.SYNC_RATE, SensePrefs.Main.SyncRate.OFTEN).commit();
    }

    /**
     * Clear the preferences of the previous user
     * @param context
     */
    public static void clearSensePreferences(Context context)
    {
        SharedPreferences mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
        mainPrefs.edit().clear().commit();

        mainPrefs = context.getSharedPreferences(SensePrefs.SENSOR_SPECIFICS, Context.MODE_PRIVATE);
        mainPrefs.edit().clear().commit();

        mainPrefs = context.getSharedPreferences(SensePrefs.STATUS_PREFS, Context.MODE_PRIVATE);
        mainPrefs.edit().clear().commit();
    }
}

