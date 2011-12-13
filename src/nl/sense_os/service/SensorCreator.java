package nl.sense_os.service;

import java.util.HashMap;

import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main;
import nl.sense_os.service.constants.SensorData.SensorNames;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;

/**
 * Class that ensures that all the phone's sensors are known at CommonSense.
 */
public class SensorCreator {

    private static final String TAG = "Sensor Creator";

    private static void checkAmbienceSensors(Context context) {

        // preallocate objects
        SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor;
        String name, displayName, description, dataType, value;
        HashMap<String, Object> dataFields = new HashMap<String, Object>();

        // match light sensor
        sensor = sm.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (null != sensor) {
            name = SensorNames.LIGHT;
            displayName = SensorNames.LIGHT;
            description = sensor.getName();
            dataType = SenseDataTypes.JSON;
            dataFields.clear();
            dataFields.put("lux", 0);
            value = new JSONObject(dataFields).toString();
            checkSensor(context, name, displayName, dataType, description, value);
        } else {
            Log.w(TAG, "No light sensor present!");
        }

        // match noise sensor
        name = SensorNames.NOISE;
        displayName = "noise";
        description = SensorNames.NOISE;
        dataType = SenseDataTypes.FLOAT;
        value = "0.0";
        checkSensor(context, name, displayName, dataType, description, value);

        // match pressure sensor
        sensor = sm.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (null != sensor) {
            name = SensorNames.PRESSURE;
            displayName = "";
            description = sensor.getName();
            dataType = SenseDataTypes.JSON;
            dataFields.clear();
            dataFields.put("newton", 0);
            value = new JSONObject(dataFields).toString();
            checkSensor(context, name, displayName, dataType, description, value);
        } else {
            Log.w(TAG, "No pressure sensor present!");
        }
    }

    private static void checkDeviceScanSensors(Context context) {

        // preallocate objects
        String name, displayName, description, dataType, value;
        HashMap<String, Object> dataFields = new HashMap<String, Object>();

        // match Bluetooth scan
        name = SensorNames.BLUETOOTH_DISCOVERY;
        displayName = "bluetooth scan";
        description = SensorNames.BLUETOOTH_DISCOVERY;
        dataType = SenseDataTypes.JSON;
        dataFields.clear();
        dataFields.put("name", "string");
        dataFields.put("address", "string");
        dataFields.put("rssi", 0);
        value = new JSONObject(dataFields).toString();
        checkSensor(context, name, displayName, dataType, description, value);

        // match Wi-Fi scan
        name = SensorNames.WIFI_SCAN;
        displayName = "wi-fi scan";
        description = SensorNames.WIFI_SCAN;
        dataType = SenseDataTypes.JSON;
        dataFields.clear();
        dataFields.put("ssid", "string");
        dataFields.put("bssid", "string");
        dataFields.put("frequency", 0);
        dataFields.put("rssi", 0);
        dataFields.put("capabilities", "string");
        value = new JSONObject(dataFields).toString();
        checkSensor(context, name, displayName, dataType, description, value);
    }

    private static void checkLocationSensors(Context context) {

        // match location sensor
        String name = SensorNames.LOCATION;
        String displayName = SensorNames.LOCATION;
        String description = SensorNames.LOCATION;
        String dataType = SenseDataTypes.JSON;
        HashMap<String, Object> dataFields = new HashMap<String, Object>();
        dataFields.put("longitude", 1.0);
        dataFields.put("latitude", 1.0);
        dataFields.put("altitude", 1.0);
        dataFields.put("accuracy", 1.0);
        dataFields.put("speed", 1.0);
        dataFields.put("bearing", 1.0f);
        dataFields.put("provider", "string");
        String value = new JSONObject(dataFields).toString();
        checkSensor(context, name, displayName, dataType, description, value);
    }

    private static void checkMotionSensors(Context context) {

        // preallocate objects
        SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor;
        String name, displayName, description, dataType, value;
        HashMap<String, Object> jsonFields = new HashMap<String, Object>();

        sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (null != sensor) {

            // match accelerometer
            name = SensorNames.ACCELEROMETER;
            displayName = "acceleration";
            description = sensor.getName();
            dataType = SenseDataTypes.JSON;
            jsonFields.clear();
            jsonFields.put("x-axis", 1.0);
            jsonFields.put("y-axis", 1.0);
            jsonFields.put("z-axis", 1.0);
            value = new JSONObject(jsonFields).toString();
            checkSensor(context, name, displayName, dataType, description, value);

            // match accelerometer (epi)
            SharedPreferences mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
                    Context.MODE_PRIVATE);
            if (mainPrefs.getBoolean(Main.Motion.EPIMODE, false)) {
                name = SensorNames.ACCELEROMETER_EPI;
                displayName = "acceleration (epi-mode)";
                description = sensor.getName();
                dataType = SenseDataTypes.JSON;
                jsonFields.clear();
                jsonFields.put("interval", 0);
                jsonFields.put("data", new JSONArray());
                value = new JSONObject(jsonFields).toString();
                checkSensor(context, name, displayName, dataType, description, value);
            }

            // match motion energy
            if (mainPrefs.getBoolean(Main.Motion.MOTION_ENERGY, false)) {
                name = SensorNames.MOTION_ENERGY;
                displayName = SensorNames.MOTION_ENERGY;
                description = SensorNames.MOTION_ENERGY;
                dataType = SenseDataTypes.FLOAT;
                value = "1.0";
                checkSensor(context, name, displayName, dataType, description, value);
            }

            // match fall detector
            if (mainPrefs.getBoolean(Main.Motion.FALL_DETECT, false)) {
                name = SensorNames.FALL_DETECTOR;
                displayName = "fall";
                description = "human fall";
                dataType = SenseDataTypes.BOOL;
                value = "true";
                checkSensor(context, name, displayName, dataType, description, value);
            }

            // match fall detector
            if (mainPrefs.getBoolean(Main.Motion.FALL_DETECT_DEMO, false)) {
                name = SensorNames.FALL_DETECTOR;
                displayName = "fall";
                description = "demo fall";
                dataType = SenseDataTypes.BOOL;
                value = "true";
                checkSensor(context, name, displayName, dataType, description, value);
            }

        } else {
            Log.w(TAG, "No accelerometer present!");
        }

        // match linear acceleration sensor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {

            sensor = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            if (null != sensor) {
                name = SensorNames.LIN_ACCELERATION;
                displayName = SensorNames.LIN_ACCELERATION;
                description = sensor.getName();
                dataType = SenseDataTypes.JSON;
                jsonFields.clear();
                jsonFields.put("x-axis", 1.0);
                jsonFields.put("y-axis", 1.0);
                jsonFields.put("z-axis", 1.0);
                value = new JSONObject(jsonFields).toString();
                checkSensor(context, name, displayName, dataType, description, value);

            } else {
                Log.w(TAG, "No linear acceleration sensor present!");
            }
        }

        // match orientation
        sensor = sm.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        if (null != sensor) {
            name = SensorNames.ORIENT;
            displayName = SensorNames.ORIENT;
            description = sensor.getName();
            dataType = SenseDataTypes.JSON;
            jsonFields.clear();
            jsonFields.put("azimuth", 1.0);
            jsonFields.put("pitch", 1.0);
            jsonFields.put("roll", 1.0);
            value = new JSONObject(jsonFields).toString();
            checkSensor(context, name, displayName, dataType, description, value);

        } else {
            Log.w(TAG, "No orientation sensor present!");
        }

        // match gyroscope
        sensor = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (null != sensor) {
            name = SensorNames.GYRO;
            displayName = "rotation rate";
            description = sensor.getName();
            dataType = SenseDataTypes.JSON;
            jsonFields.clear();
            jsonFields.put("azimuth rate", 1.0);
            jsonFields.put("pitch rate", 1.0);
            jsonFields.put("roll rate", 1.0);
            value = new JSONObject(jsonFields).toString();
            checkSensor(context, name, displayName, dataType, description, value);

        } else {
            Log.w(TAG, "No gyroscope present!");
        }
    }

    private static void checkPhoneStateSensors(Context context) {

        // preallocate objects
        SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor;
        String name, displayName, description, dataType, value;
        HashMap<String, Object> dataFields = new HashMap<String, Object>();

        // match battery sensor
        name = SensorNames.BATTERY_SENSOR;
        displayName = "battery state";
        description = SensorNames.BATTERY_SENSOR;
        dataType = SenseDataTypes.JSON;
        dataFields.clear();
        dataFields.put("status", "string");
        dataFields.put("level", "string");
        value = new JSONObject(dataFields).toString();
        checkSensor(context, name, displayName, dataType, description, value);

        // match screen activity
        name = SensorNames.SCREEN_ACTIVITY;
        displayName = SensorNames.SCREEN_ACTIVITY;
        description = SensorNames.SCREEN_ACTIVITY;
        dataType = SenseDataTypes.JSON;
        dataFields.clear();
        dataFields.put("screen", "string");
        value = new JSONObject(dataFields).toString();
        checkSensor(context, name, displayName, dataType, description, value);

        // match proximity
        sensor = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if (null != sensor) {
            name = SensorNames.PROXIMITY;
            displayName = SensorNames.PROXIMITY;
            description = sensor.getName();
            dataType = SenseDataTypes.JSON;
            dataFields.clear();
            dataFields.put("distance", 1.0);
            value = new JSONObject(dataFields).toString();
            checkSensor(context, name, displayName, dataType, description, value);
        } else {
            Log.w(TAG, "No proximity sensor present!");
        }

        // match call state
        name = SensorNames.CALL_STATE;
        displayName = SensorNames.CALL_STATE;
        description = SensorNames.CALL_STATE;
        dataType = SenseDataTypes.JSON;
        dataFields.clear();
        dataFields.put("state", "string");
        dataFields.put("incomingNumber", "string");
        value = new JSONObject(dataFields).toString();
        checkSensor(context, name, displayName, dataType, description, value);

        // match connection type
        name = SensorNames.CONN_TYPE;
        displayName = "network type";
        description = SensorNames.CONN_TYPE;
        dataType = SenseDataTypes.STRING;
        value = "string";
        checkSensor(context, name, displayName, dataType, description, value);

        // match ip address
        name = SensorNames.IP_ADDRESS;
        displayName = SensorNames.IP_ADDRESS;
        description = SensorNames.IP_ADDRESS;
        dataType = SenseDataTypes.STRING;
        value = "string";
        checkSensor(context, name, displayName, dataType, description, value);

        // match messages waiting sensor
        name = SensorNames.UNREAD_MSG;
        displayName = "message waiting";
        description = SensorNames.UNREAD_MSG;
        dataType = SenseDataTypes.BOOL;
        value = "true";
        checkSensor(context, name, displayName, dataType, description, value);

        // match data connection
        name = SensorNames.DATA_CONN;
        displayName = SensorNames.DATA_CONN;
        description = SensorNames.DATA_CONN;
        dataType = SenseDataTypes.STRING;
        value = "string";
        checkSensor(context, name, displayName, dataType, description, value);

        // match servie state
        name = SensorNames.SERVICE_STATE;
        displayName = SensorNames.SERVICE_STATE;
        description = SensorNames.SERVICE_STATE;
        dataType = SenseDataTypes.JSON;
        dataFields.clear();
        dataFields.put("state", "string");
        dataFields.put("phone number", "string");
        dataFields.put("manualSet", true);
        value = new JSONObject(dataFields).toString();
        checkSensor(context, name, displayName, dataType, description, value);

        // match signal strength
        name = SensorNames.SIGNAL_STRENGTH;
        displayName = SensorNames.SIGNAL_STRENGTH;
        description = SensorNames.SIGNAL_STRENGTH;
        dataType = SenseDataTypes.JSON;
        dataFields.clear();
        dataFields.put("GSM signal strength", 1);
        dataFields.put("GSM bit error rate", 1);
        value = new JSONObject(dataFields).toString();
        checkSensor(context, name, displayName, dataType, description, value);
    }

    /**
     * Ensures existence of a sensor at CommonSense, adding it to the list of registered sensors if
     * it was newly created.
     * 
     * @param context
     *            Context for setting up communication with CommonSense
     * @param name
     *            Sensor name.
     * @param displayName
     *            Pretty display name for the sensor.
     * @param dataType
     *            Sensor data type.
     * @param description
     *            Sensor description (previously 'device_type')
     * @param value
     *            Dummy sensor value (used for producing a data structure).
     */
    private static void checkSensor(Context context, String name, String displayName,
            String dataType, String description, String value) {
        try {
            if (null == SenseApi.getSensorId(context, name, description, dataType, null)) {
                SenseApi.registerSensor(context, name, displayName, description, dataType, value,
                        null, null);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to check '" + name + "' sensor at CommonSense");
        }
    }

    /**
     * Ensures that all of the phone's sensors exist at CommonSense, and that the phone knows their
     * sensor IDs.
     * 
     * @param context
     *            Context for communication with CommonSense.
     */
    public static synchronized void checkSensorsAtCommonSense(Context context) {

        checkAmbienceSensors(context);
        checkDeviceScanSensors(context);
        checkLocationSensors(context);
        checkMotionSensors(context);
        checkPhoneStateSensors(context);
    }
}
