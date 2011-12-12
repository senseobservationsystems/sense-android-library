package nl.sense_os.service;

import java.util.HashMap;

import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.SensorNames;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;

/**
 * Class that ensures that all the phone's sensors are known at CommonSense.
 */
public class SensorCreator {

    private static final String TAG = "Sensor Registrator";

    private static void checkAmbienceSensors(Context context, JSONArray registered) {

        // preallocate objects
        SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor;
        String name, displayName, description, dataType, sensorValue;
        HashMap<String, Object> jsonFields = new HashMap<String, Object>();

        // match light sensor
        sensor = sm.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (null != sensor) {
            name = SensorNames.LIGHT;
            displayName = SensorNames.LIGHT;
            description = sensor.getName();
            dataType = SenseDataTypes.JSON;
            jsonFields.clear();
            jsonFields.put("lux", 0);
            sensorValue = new JSONObject(jsonFields).toString();
            if (null == SenseApi.getSensorId(context, name, displayName, sensorValue, dataType,
                    description)) {
                SenseApi.registerSensor(context, name, displayName, description, dataType,
                        sensorValue);
            }
        }

        // match noise sensor
        name = SensorNames.NOISE;
        displayName = "noise";
        description = SensorNames.NOISE;
        dataType = SenseDataTypes.FLOAT;
        sensorValue = "0.0";
        if (null == SenseApi.getSensorId(context, name, displayName, sensorValue, dataType,
                description)) {
            SenseApi.registerSensor(context, name, displayName, description, dataType, sensorValue);
        }

        // match pressure sensor
        sensor = sm.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (null != sensor) {
            name = SensorNames.PRESSURE;
            displayName = "";
            description = sensor.getName();
            dataType = SenseDataTypes.JSON;
            jsonFields.clear();
            jsonFields.put("newton", 0);
            sensorValue = new JSONObject(jsonFields).toString();
            if (null == SenseApi.getSensorId(context, name, displayName, sensorValue, dataType,
                    description)) {
                SenseApi.registerSensor(context, name, displayName, description, dataType,
                        sensorValue);
            }
        }
    }

    private static void checkDeviceScanSensors(Context context, JSONArray registered) {

        // preallocate objects
        String name, displayName, description, dataType, sensorValue;
        HashMap<String, Object> jsonFields = new HashMap<String, Object>();

        // match Bluetooth scan
        name = SensorNames.BLUETOOTH_DISCOVERY;
        displayName = "bluetooth scan";
        description = SensorNames.BLUETOOTH_DISCOVERY;
        dataType = SenseDataTypes.JSON;
        jsonFields.clear();
        jsonFields.put("name", "string");
        jsonFields.put("address", "string");
        jsonFields.put("rssi", 0);
        sensorValue = new JSONObject(jsonFields).toString();
        if (null == SenseApi.getSensorId(context, name, displayName, sensorValue, dataType,
                description)) {
            SenseApi.registerSensor(context, name, displayName, description, dataType, sensorValue);
        }

        // match Wi-Fi scan
        name = SensorNames.WIFI_SCAN;
        displayName = "wi-fi scan";
        description = SensorNames.WIFI_SCAN;
        dataType = SenseDataTypes.JSON;
        jsonFields.clear();
        jsonFields.put("ssid", "string");
        jsonFields.put("bssid", "string");
        jsonFields.put("frequency", 0);
        jsonFields.put("rssi", 0);
        jsonFields.put("capabilities", "string");
        sensorValue = new JSONObject(jsonFields).toString();
        if (null == SenseApi.getSensorId(context, name, displayName, sensorValue, dataType,
                description)) {
            SenseApi.registerSensor(context, name, displayName, description, dataType, sensorValue);
        }
    }

    private static void checkLocationSensors(Context context, JSONArray registered) {
        // match location sensor
        String name = SensorNames.LOCATION;
        String displayName = SensorNames.LOCATION;
        String description = SensorNames.LOCATION;
        String dataType = SenseDataTypes.JSON;
        HashMap<String, Object> jsonFields = new HashMap<String, Object>();
        jsonFields.put("longitude", 1.0);
        jsonFields.put("latitude", 1.0);
        jsonFields.put("altitude", 1.0);
        jsonFields.put("accuracy", 1.0);
        jsonFields.put("speed", 1.0);
        jsonFields.put("bearing", 1.0f);
        jsonFields.put("provider", "string");
        String sensorValue = new JSONObject(jsonFields).toString();
        if (null == SenseApi.getSensorId(context, name, displayName, sensorValue, dataType,
                description)) {
            SenseApi.registerSensor(context, name, displayName, description, dataType, sensorValue);
        }
    }

    private static void checkMotionSensors(Context context, JSONArray registered) {

        // preallocate objects
        SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor;
        String name, displayName, description, dataType, sensorValue;
        HashMap<String, Object> jsonFields = new HashMap<String, Object>();

        // match accelerometer
        sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (null != sensor) {
            name = SensorNames.ACCELEROMETER;
            displayName = "acceleration";
            description = sensor.getName();
            dataType = SenseDataTypes.JSON;
            jsonFields.clear();
            jsonFields.put("x-axis", 1.0);
            jsonFields.put("y-axis", 1.0);
            jsonFields.put("z-axis", 1.0);
            sensorValue = new JSONObject(jsonFields).toString();
            if (null == SenseApi.getSensorId(context, name, displayName, sensorValue, dataType,
                    description)) {
                SenseApi.registerSensor(context, name, displayName, description, dataType,
                        sensorValue);
            }

            name = SensorNames.ACCELEROMETER_EPI;
            displayName = "acceleration (epi-mode)";
            description = sensor.getName();
            dataType = SenseDataTypes.JSON;
            jsonFields.clear();
            jsonFields.put("interval", 0);
            jsonFields.put("data", new JSONArray());
            sensorValue = new JSONObject(jsonFields).toString();
            if (null == SenseApi.getSensorId(context, name, displayName, sensorValue, dataType,
                    description)) {
                SenseApi.registerSensor(context, name, displayName, description, dataType,
                        sensorValue);
            }

            name = SensorNames.MOTION_ENERGY;
            displayName = SensorNames.MOTION_ENERGY;
            description = SensorNames.MOTION_ENERGY;
            dataType = SenseDataTypes.FLOAT;
            sensorValue = "1.0";
            if (null == SenseApi.getSensorId(context, name, displayName, sensorValue, dataType,
                    description)) {
                SenseApi.registerSensor(context, name, displayName, description, dataType,
                        sensorValue);
            }

            name = SensorNames.FALL_DETECTOR;
            displayName = "fall";
            description = "human fall";
            dataType = SenseDataTypes.BOOL;
            sensorValue = "true";
            if (null == SenseApi.getSensorId(context, name, displayName, sensorValue, dataType,
                    description)) {
                SenseApi.registerSensor(context, name, displayName, description, dataType,
                        sensorValue);
            }

            name = SensorNames.FALL_DETECTOR;
            displayName = "fall";
            description = "demo fall";
            dataType = SenseDataTypes.BOOL;
            sensorValue = "true";
            if (null == SenseApi.getSensorId(context, name, displayName, sensorValue, dataType,
                    description)) {
                SenseApi.registerSensor(context, name, displayName, description, dataType,
                        sensorValue);
            }
        }

        // match linear acceleration sensor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
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
                sensorValue = new JSONObject(jsonFields).toString();
                if (null == SenseApi.getSensorId(context, name, displayName, sensorValue, dataType,
                        description)) {
                    SenseApi.registerSensor(context, name, displayName, description, dataType,
                            sensorValue);
                }
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
            sensorValue = new JSONObject(jsonFields).toString();
            if (null == SenseApi.getSensorId(context, name, displayName, sensorValue, dataType,
                    description)) {
                SenseApi.registerSensor(context, name, displayName, description, dataType,
                        sensorValue);
            }
        }

        // match gyroscope
        sensor = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (null != sensor) {
            name = SensorNames.GYRO;
            displayName = "rotation rate";
            description = sensor.getName();
            dataType = SenseDataTypes.JSON;
            jsonFields.clear();
            jsonFields.put("azimuth", 1.0);
            jsonFields.put("pitch", 1.0);
            jsonFields.put("roll", 1.0);
            sensorValue = new JSONObject(jsonFields).toString();
            if (null == SenseApi.getSensorId(context, name, displayName, sensorValue, dataType,
                    description)) {
                SenseApi.registerSensor(context, name, displayName, description, dataType,
                        sensorValue);
            }
        }
    }

    private static void checkPhoneStateSensors(Context context, JSONArray registered) {

        // preallocate objects
        SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor;
        String name, displayName, description, dataType, sensorValue;
        HashMap<String, Object> jsonFields = new HashMap<String, Object>();

        // match battery sensor
        name = SensorNames.BATTERY_SENSOR;
        displayName = "battery state";
        description = SensorNames.BATTERY_SENSOR;
        dataType = SenseDataTypes.JSON;
        jsonFields.clear();
        jsonFields.put("status", "string");
        jsonFields.put("level", "string");
        sensorValue = new JSONObject(jsonFields).toString();
        if (null == SenseApi.getSensorId(context, name, displayName, sensorValue, dataType,
                description)) {
            SenseApi.registerSensor(context, name, displayName, description, dataType, sensorValue);
        }

        // match screen activity
        name = SensorNames.SCREEN_ACTIVITY;
        displayName = SensorNames.SCREEN_ACTIVITY;
        description = SensorNames.SCREEN_ACTIVITY;
        dataType = SenseDataTypes.JSON;
        jsonFields.clear();
        jsonFields.put("screen", "string");
        sensorValue = new JSONObject(jsonFields).toString();
        if (null == SenseApi.getSensorId(context, name, displayName, sensorValue, dataType,
                description)) {
            SenseApi.registerSensor(context, name, displayName, description, dataType, sensorValue);
        }

        // match proximity
        sensor = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if (null != sensor) {
            name = SensorNames.PROXIMITY;
            displayName = SensorNames.PROXIMITY;
            description = sensor.getName();
            dataType = SenseDataTypes.JSON;
            jsonFields.clear();
            jsonFields.put("distance", 1.0);
            sensorValue = new JSONObject(jsonFields).toString();
            if (null == SenseApi.getSensorId(context, name, displayName, sensorValue, dataType,
                    description)) {
                SenseApi.registerSensor(context, name, displayName, description, dataType,
                        sensorValue);
            }
        }

        // match call state
        name = SensorNames.CALL_STATE;
        displayName = SensorNames.CALL_STATE;
        description = SensorNames.CALL_STATE;
        dataType = SenseDataTypes.JSON;
        jsonFields.clear();
        jsonFields.put("state", "string");
        jsonFields.put("incomingNumber", "string");
        sensorValue = new JSONObject(jsonFields).toString();
        if (null == SenseApi.getSensorId(context, name, displayName, sensorValue, dataType,
                description)) {
            SenseApi.registerSensor(context, name, displayName, description, dataType, sensorValue);
        }

        // match connection type
        name = SensorNames.CONN_TYPE;
        displayName = "network type";
        description = SensorNames.CONN_TYPE;
        dataType = SenseDataTypes.STRING;
        sensorValue = "string";
        if (null == SenseApi.getSensorId(context, name, displayName, sensorValue, dataType,
                description)) {
            SenseApi.registerSensor(context, name, displayName, description, dataType, sensorValue);
        }

        // match ip address
        name = SensorNames.IP_ADDRESS;
        displayName = SensorNames.IP_ADDRESS;
        description = SensorNames.IP_ADDRESS;
        dataType = SenseDataTypes.STRING;
        sensorValue = "string";
        if (null == SenseApi.getSensorId(context, name, displayName, sensorValue, dataType,
                description)) {
            SenseApi.registerSensor(context, name, displayName, description, dataType, sensorValue);
        }

        // match ip address
        name = SensorNames.UNREAD_MSG;
        displayName = "message waiting";
        description = SensorNames.UNREAD_MSG;
        dataType = SenseDataTypes.BOOL;
        sensorValue = "true";
        if (null == SenseApi.getSensorId(context, name, displayName, sensorValue, dataType,
                description)) {
            SenseApi.registerSensor(context, name, displayName, description, dataType, sensorValue);
        }

        // match data connection
        name = SensorNames.DATA_CONN;
        displayName = SensorNames.DATA_CONN;
        description = SensorNames.DATA_CONN;
        dataType = SenseDataTypes.STRING;
        sensorValue = "string";
        if (null == SenseApi.getSensorId(context, name, displayName, sensorValue, dataType,
                description)) {
            SenseApi.registerSensor(context, name, displayName, description, dataType, sensorValue);
        }

        // match data connection
        name = SensorNames.SERVICE_STATE;
        displayName = SensorNames.SERVICE_STATE;
        description = SensorNames.SERVICE_STATE;
        dataType = SenseDataTypes.STRING;
        sensorValue = "string";
        if (null == SenseApi.getSensorId(context, name, displayName, sensorValue, dataType,
                description)) {
            SenseApi.registerSensor(context, name, displayName, description, dataType, sensorValue);
        }

        // match signal strength
        name = SensorNames.SIGNAL_STRENGTH;
        displayName = SensorNames.SIGNAL_STRENGTH;
        description = SensorNames.SIGNAL_STRENGTH;
        dataType = SenseDataTypes.JSON;
        jsonFields.clear();
        jsonFields.put("GSM signal strength", 1);
        jsonFields.put("GSM bit error rate", 1);
        sensorValue = new JSONObject(jsonFields).toString();
        if (null == SenseApi.getSensorId(context, name, displayName, sensorValue, dataType,
                description)) {
            SenseApi.registerSensor(context, name, displayName, description, dataType, sensorValue);
        }
    }

    /**
     * Ensures that all of the phone's sensors exist at CommonSense, and that the phone knows their
     * sensor IDs.
     * 
     * @param context
     */
    public static void checkSensorsAtCommonSense(Context context) {

        // get list of known sensors at CommonSense
        JSONArray registered = SenseApi.getRegisteredSensors(context);

        if (null == registered) {
            Log.d(TAG, "failed to get registered sensors from Sense API");
            return;
        }

        checkAmbienceSensors(context, registered);
        checkDeviceScanSensors(context, registered);
        checkLocationSensors(context, registered);
        checkMotionSensors(context, registered);
        checkPhoneStateSensors(context, registered);
    }
}
