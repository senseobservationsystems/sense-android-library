package nl.sense_os.service.commonsense;

import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main;
import nl.sense_os.service.constants.SensePrefs.Main.Ambience;
import nl.sense_os.service.constants.SensePrefs.Main.Motion;
import nl.sense_os.service.constants.SensorData.SensorDescriptions;
import nl.sense_os.service.constants.SensorData.SensorNames;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.nfc.NfcManager;
import android.os.Build;
import android.telephony.TelephonyManager;

/**
 * Class that verifies that all the phone's sensors are known at CommonSense.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 * 
 * @see DefaultSensorRegistrationService
 */
public class DefaultSensorRegistrator extends SensorRegistrator {

    public DefaultSensorRegistrator(Context context) {
        super(context);
    }

    // private static final String TAG = "Sensor Registration";

    /**
     * Checks the IDs for light, camera light, noise, pressure sensors.
     * 
     * @param deviceUuid
     * @param deviceType
     * 
     * @return true if all sensor IDs are found or created
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private boolean checkAmbienceSensors(String deviceType, String deviceUuid) {

        // preallocate objects
        SensorManager sm = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor;
        boolean success = true;

        // match light sensor
        sensor = sm.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (null != sensor) {
            success &= checkSensor(SensorNames.LIGHT, SensorNames.LIGHT, SenseDataTypes.JSON,
                    sensor.getName(), "{\"lux\":0}", deviceType, deviceUuid);
        } else {
            // Log.v(TAG, "No light sensor present!");
        }

        // match camera light sensor (only for Android < 4.0)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            // multiple camera support starting from Android 2.3
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD) {
                for (int camera_id = 0; camera_id < Camera.getNumberOfCameras(); ++camera_id) {
                    success &= checkSensor(SensorNames.CAMERA_LIGHT, "Camera Light",
                            SenseDataTypes.JSON, "camera " + camera_id + " average luminance",
                            "{\"lux\":0}", deviceType, deviceUuid);
                }
            }
        }

        // match noise sensor
        success &= checkSensor(SensorNames.NOISE, "noise", SenseDataTypes.FLOAT, SensorNames.NOISE,
                "0.0", deviceType, deviceUuid);
        
        // match noise sensor (burst-mode)
        SharedPreferences mainPrefs = getContext().getSharedPreferences(SensePrefs.MAIN_PREFS,
        		Context.MODE_PRIVATE);
        if (mainPrefs.getBoolean(Ambience.BURSTMODE, false)) {
        	success &= checkSensor(SensorNames.NOISE_BURST, "noise (burst-mode)", SenseDataTypes.JSON, "noise (dB)",
        			"{\"interval:\":0,\"data\":[2.23, 19.45, 20.2]}", deviceType, deviceUuid);
        }
        // match auto calibrated noise sensor
        success &= checkSensor(SensorNames.NOISE, "noise", SenseDataTypes.FLOAT,
                SensorDescriptions.AUTO_CALIBRATED, "0.0", deviceType, deviceUuid);

        // match noise spectrum
        success &= checkSensor(
                SensorNames.AUDIO_SPECTRUM,
                "audio spectrum",
                SenseDataTypes.JSON,
                "audio spectrum (dB)",
                "{\"bandwidth\":10, \"spectrum\":[40, 50, 40 , 20, 40, 60, 43, 12, 34, 56]}",
                deviceType, deviceUuid);

        // match pressure sensor
        sensor = sm.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (null != sensor) {
            success &= checkSensor(SensorNames.PRESSURE, SensorNames.PRESSURE,
                    SenseDataTypes.FLOAT, sensor.getName(), "1.0", deviceType, deviceUuid);
        } else {
            // Log.v(TAG, "No pressure sensor present!");
        }

        // match Magnetic Field sensor
        sensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (null != sensor) {
            success &= checkSensor(SensorNames.MAGNETIC_FIELD, SensorNames.MAGNETIC_FIELD,
                    SenseDataTypes.JSON, sensor.getName(), "{\"x\":0, \"y\":0, \"z\":0}",
                    deviceType, deviceUuid);
        } else {
            // Log.v(TAG, "No magnetic field sensor present!");
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            sensor = sm.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
            if (null != sensor) {
                success &= checkSensor(SensorNames.AMBIENT_TEMPERATURE, "ambient temperature",
                        SenseDataTypes.FLOAT, sensor.getName(), "1.0", deviceType, deviceUuid);
            } else {
                // Log.v(TAG, "No ambient temperature sensor present!");
            }

            sensor = sm.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
            if (null != sensor) {
                success &= checkSensor(SensorNames.RELATIVE_HUMIDITY, "relative humidity",
                        SenseDataTypes.FLOAT, sensor.getName(), "1.0", deviceType, deviceUuid);
            } else {
                // Log.v(TAG, "No relative humidity sensor present!");
            }
        }

        // match loudness sensor
        success &= checkSensor(SensorNames.LOUDNESS, SensorNames.LOUDNESS, SenseDataTypes.FLOAT,
                SensorNames.LOUDNESS, "0.0", deviceType, deviceUuid);

        return success;
    }

    private boolean checkDebugSensors(String deviceType, String deviceUuid) {

        boolean success = true;

        // match myrianode sensor
        SharedPreferences mainPrefs = getContext().getSharedPreferences(SensePrefs.MAIN_PREFS,
                Context.MODE_PRIVATE);
        if (mainPrefs.getBoolean(Main.Advanced.LOCATION_FEEDBACK, false)) {
            success &= checkSensor(SensorNames.ATTACHED_TO_MYRIANODE,
                    SensorNames.ATTACHED_TO_MYRIANODE, SenseDataTypes.STRING, "Sense Logger",
                    "string", deviceType, deviceUuid);
        }

        return success;
    }

    /**
     * Checks the IDs for Bluetooth and Wi-Fi scan sensors.
     * 
     * @param deviceUuid
     * @param deviceType
     * 
     * @return true if all sensor IDs are found or created
     */
    @TargetApi(10)
    private boolean checkDeviceScanSensors(String deviceType, String deviceUuid) {

        // preallocate objects
        boolean success = true;

        // match Bluetooth scan
        success &= checkSensor(SensorNames.BLUETOOTH_DISCOVERY, "bluetooth scan",
                SenseDataTypes.JSON, SensorNames.BLUETOOTH_DISCOVERY,
                "{\"name\":\"string\",\"address\":\"string\",\"rssi\":0}", deviceType, deviceUuid);

        // match Bluetooth neighbours count
        success &= checkSensor(SensorNames.BLUETOOTH_NEIGHBOURS_COUNT,
                "bluetooth neighbours count", SenseDataTypes.INT,
                SensorNames.BLUETOOTH_NEIGHBOURS_COUNT, "0", deviceType, deviceUuid);

        // match Wi-Fi scan
        success &= checkSensor(
                SensorNames.WIFI_SCAN,
                "wi-fi scan",
                SenseDataTypes.JSON,
                SensorNames.WIFI_SCAN,
                "{\"ssid\":\"string\",\"bssid\":\"string\",\"frequency\":0,\"rssi\":0,\"capabilities\":\"string\"}",
                deviceType, deviceUuid);

        // match NFC scan
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
            NfcManager nm = (NfcManager) getContext().getSystemService(Context.NFC_SERVICE);
            if (null != nm.getDefaultAdapter()) {
                success &= checkSensor(SensorNames.NFC_SCAN, "nfc scan", SenseDataTypes.JSON,
                        SensorNames.NFC_SCAN,
                        "{\"id\":\"string\",\"technology\":\"string\",\"message\":\"string\"}",
                        deviceType, deviceUuid);
            }
        }

        return success;
    }

    /**
     * Checks the ID for the location sensor
     * 
     * @param deviceUuid
     * @param deviceType
     * 
     * @return true if the sensor ID is found or created
     */
    private boolean checkLocationSensors(String deviceType, String deviceUuid) {
        boolean succes = true;
        // match location sensor
        succes &= checkSensor(
                SensorNames.LOCATION,
                SensorNames.LOCATION,
                SenseDataTypes.JSON,
                SensorNames.LOCATION,
                "{\"longitude\":1.0,\"laitude\":1.0,\"altitude\":1.0,\"accuracy\":1.0,\"speed\":1.0,\"bearing\":1.0,\"provider\":\"provider\"}",
                deviceType, deviceUuid);

        // traveled distance sensor
        succes &= checkSensor(SensorNames.TRAVELED_DISTANCE_1H, SensorNames.TRAVELED_DISTANCE_1H,
                SenseDataTypes.FLOAT, SensorNames.TRAVELED_DISTANCE_1H, "0.0", deviceType,
                deviceUuid);

        return succes;
    }

    /**
     * Checks the IDs for the accelerometer, orientation, fall detector, motion energy sensors.
     * 
     * @param deviceUuid
     * @param deviceType
     * 
     * @return true if the sensor ID is found or created
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @SuppressWarnings("deprecation")
    // TODO: replace orientation sensor with modern version
    private boolean checkMotionSensors(String deviceType, String deviceUuid) {

        // preallocate objects
        SensorManager sm = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor;
        boolean success = true;
        SharedPreferences mainPrefs = getContext().getSharedPreferences(SensePrefs.MAIN_PREFS,
                Context.MODE_PRIVATE);

        sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (null != sensor) {

            // match accelerometer
            if (mainPrefs.getBoolean(Motion.ACCELEROMETER, true)) {
                success &= checkSensor(SensorNames.ACCELEROMETER, "acceleration",
                        SenseDataTypes.JSON, sensor.getName(),
                        "{\"x-axis\":1.0,\"y-axis\":1.0,\"z-axis\":1.0}", deviceType, deviceUuid);
            }

            if (mainPrefs.getBoolean(Motion.EPIMODE, false)) {
                success &= checkSensor(SensorNames.ACCELEROMETER_EPI, "acceleration (epi-mode)",
                        SenseDataTypes.JSON, sensor.getName(), "{\"interval\":0,\"data\":[]}",
                        deviceType, deviceUuid);
            }

            if (mainPrefs.getBoolean(Motion.BURSTMODE, false)) {
                success &= checkSensor(SensorNames.ACCELEROMETER_BURST,
                        "acceleration (burst-mode)", SenseDataTypes.JSON, sensor.getName(),
                        "{\"interval\":0,\"data\":[]}", deviceType, deviceUuid);
            }

            // match motion energy
            if (mainPrefs.getBoolean(Main.Motion.MOTION_ENERGY, false)) {
                success &= checkSensor(SensorNames.MOTION_ENERGY, SensorNames.MOTION_ENERGY,
                        SenseDataTypes.FLOAT, SensorNames.MOTION_ENERGY, "1.0", deviceType,
                        deviceUuid);
            }

            // match fall detector
            if (mainPrefs.getBoolean(Main.Motion.FALL_DETECT, false)) {
                success &= checkSensor(SensorNames.FALL_DETECTOR, "fall", SenseDataTypes.BOOL,
                        "human fall", "true", deviceType, deviceUuid);
            }

            // match fall detector
            if (mainPrefs.getBoolean(Main.Motion.FALL_DETECT_DEMO, false)) {
                success &= checkSensor(SensorNames.FALL_DETECTOR, "fall (demo)",
                        SenseDataTypes.BOOL, "demo fall", "true", deviceType, deviceUuid);
            }
            
            

            // match linear acceleration
            if (mainPrefs.getBoolean(Motion.LINEAR_ACCELERATION, true)) {
                //check if actual linear accelerometer exists,
                //if not we set burst-mode only
                String processed ="";

                if (null != sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)) {
                  success &= checkSensor(SensorNames.LIN_ACCELERATION,
                          SensorNames.LIN_ACCELERATION, SenseDataTypes.JSON, sensor.getName(),
                          "{\"x-axis\":1.0,\"y-axis\":1.0,\"z-axis\":1.0}", deviceType,
                          deviceUuid);
                }else {
                  processed = "processed ";
                }

                if (mainPrefs.getBoolean(Motion.BURSTMODE, false)) {
                    success &= checkSensor(SensorNames.LINEAR_BURST,
                            "linear acceleration " + processed + "(burst-mode)", SenseDataTypes.JSON,
                            sensor.getName(), "{\"interval\":0,\"data\":[]}", deviceType,
                            deviceUuid);
                }
            }

        } else {
            // Log.v(TAG, "No accelerometer present!");
        }



        // match orientation
        if (mainPrefs.getBoolean(Motion.ORIENTATION, true)) {
            sensor = sm.getDefaultSensor(Sensor.TYPE_ORIENTATION);
            if (null != sensor) {
                success &= checkSensor(SensorNames.ORIENT, SensorNames.ORIENT, SenseDataTypes.JSON,
                        sensor.getName(), "{\"azimuth\":1.0,\"pitch\":1.0,\"roll\":1.0}",
                        deviceType, deviceUuid);

            } else {
                // Log.v(TAG, "No orientation sensor present!");
            }
        }
        // match gyroscope
        if (mainPrefs.getBoolean(Motion.GYROSCOPE, true)) {
            sensor = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            if (null != sensor) {
                success &= checkSensor(SensorNames.GYRO, "rotation rate", SenseDataTypes.JSON,
                        sensor.getName(),
                        "{\"azimuth rate\":1.0,\"pitch rate\":1.0,\"roll rate\":1.0}", deviceType,
                        deviceUuid);
                if (mainPrefs.getBoolean(Motion.BURSTMODE, false)) {
                    success &= checkSensor(SensorNames.GYRO_BURST, "rotation rate (burst-mode)",
                            SenseDataTypes.JSON, sensor.getName(), "{\"interval\":0,\"data\":[]}",
                            deviceType, deviceUuid);
                }

            } else {
                // Log.v(TAG, "No gyroscope present!");
            }
        }
        return success;
    }

    /**
     * Checks IDs for the battery, screen activity, proximity, cal state, connection type, service
     * state, signal strength sensors.
     * 
     * @param deviceUuid
     * @param deviceType
     * 
     * @return true if all of the sensor IDs were found or created
     */
    private boolean checkPhoneStateSensors(String deviceType, String deviceUuid) {

        // preallocate objects
        SensorManager sm = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor;
        boolean success = true;

        // match battery sensor
        success &= checkSensor(SensorNames.BATTERY_SENSOR, "battery state", SenseDataTypes.JSON,
                SensorNames.BATTERY_SENSOR, "{\"status\":\"string\",\"level\":\"string\"}",
                deviceType, deviceUuid);

        // match screen activity
        success &= checkSensor(SensorNames.SCREEN_ACTIVITY, SensorNames.SCREEN_ACTIVITY,
                SenseDataTypes.JSON, SensorNames.SCREEN_ACTIVITY, "{\"screen\":\"string\"}",
                deviceType, deviceUuid);

        // match proximity
        sensor = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if (null != sensor) {
            success &= checkSensor(SensorNames.PROXIMITY, SensorNames.PROXIMITY,
                    SenseDataTypes.FLOAT, sensor.getName(), "0.0", deviceType, deviceUuid);
        } else {
            // Log.v(TAG, "No proximity sensor present!");
        }

        TelephonyManager tm = (TelephonyManager) getContext()
                .getSystemService(Context.TELEPHONY_SERVICE);
        if (null != tm.getDeviceId()) {
            // match call state
            success &= checkSensor(SensorNames.CALL_STATE, SensorNames.CALL_STATE,
                    SenseDataTypes.JSON, SensorNames.CALL_STATE,
                    "{\"state\":\"string\",\"incomingNumber\":\"string\"}", deviceType, deviceUuid);

            // match service state
            success &= checkSensor(SensorNames.SERVICE_STATE, SensorNames.SERVICE_STATE,
                    SenseDataTypes.JSON, SensorNames.SERVICE_STATE,
                    "{\"state\":\"string\",\"phone number\":\"string\",\"manualSet\":true}",
                    deviceType, deviceUuid);

            // match signal strength
            success &= checkSensor(SensorNames.SIGNAL_STRENGTH, SensorNames.SIGNAL_STRENGTH,
                    SenseDataTypes.JSON, SensorNames.SIGNAL_STRENGTH,
                    "{\"GSM signal strength\":1,\"GSM bit error rate\":1}", deviceType, deviceUuid);
        } else {
            // Log.v(TAG, "No telephony present");
        }

        // match connection type
        success &= checkSensor(SensorNames.CONN_TYPE, "network type", SenseDataTypes.STRING,
                SensorNames.CONN_TYPE, "string", deviceType, deviceUuid);

        // match ip address
        success &= checkSensor(SensorNames.IP_ADDRESS, SensorNames.IP_ADDRESS,
                SenseDataTypes.STRING, SensorNames.IP_ADDRESS, "string", deviceType, deviceUuid);

        // match messages waiting sensor
        success &= checkSensor(SensorNames.UNREAD_MSG, "message waiting", SenseDataTypes.BOOL,
                SensorNames.UNREAD_MSG, "true", deviceType, deviceUuid);

        // match data connection
        success &= checkSensor(SensorNames.DATA_CONN, SensorNames.DATA_CONN, SenseDataTypes.STRING,
        		SensorNames.DATA_CONN, "string", deviceType, deviceUuid);
        // match data connection
        success &= checkSensor(SensorNames.DATA_CONN, SensorNames.DATA_CONN, SenseDataTypes.STRING,
        		SensorNames.DATA_CONN, "string", deviceType, deviceUuid);

        // match installed apps sensor
        success &= checkSensor(SensorNames.APP_INSTALLED, "installed apps", SenseDataTypes.JSON,
        		SensorNames.APP_INSTALLED, "{\"installed\":[]}", deviceType, deviceUuid);
        // TODO figure out a better way to send an array of objects

        // match foreground app sensor
        success &= checkSensor(SensorNames.APP_FOREGROUND, "foreground app", SenseDataTypes.JSON,
        		SensorNames.APP_FOREGROUND, "{\"label\":\"Sense app\",\"process\":\"nl.sense_os.app\",\"activity\":\"SenseMainActivity\"}", deviceType, deviceUuid);
        return success;
    }

    @Override
    public boolean verifySensorIds(String deviceType, String deviceUuid) {
        boolean success = checkAmbienceSensors(deviceType, deviceUuid);
        success &= checkDeviceScanSensors(deviceType, deviceUuid);
        success &= checkLocationSensors(deviceType, deviceUuid);
        success &= checkMotionSensors(deviceType, deviceUuid);
        success &= checkPhoneStateSensors(deviceType, deviceUuid);
        success &= checkDebugSensors(deviceType, deviceUuid);
        return success;
    }
}
