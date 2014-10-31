package nl.sense_os.service.constants;

import nl.sense_os.service.MsgHandler;
import nl.sense_os.service.storage.LocalStorage;
import android.content.ContentResolver;
import android.provider.BaseColumns;

/**
 * Utiliy class that contains resources for representing sensor data points.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 * 
 * @see LocalStorage
 * @see MsgHandler
 */
public class SensorData {

    /**
     * Column names for Cursors that represent a set of buffered data points for a certain sensor.
     */
    public static class BufferedData implements BaseColumns {

        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
                + "/vnd.sense_os.buffered_data";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
                + "/vnd.sense_os.buffered_data";

        public static final String SENSOR = "sensor";
        public static final String ACTIVE = "active";
        public static final String JSON = "json";

        private BufferedData() {
            // class should not be instantiated
        }
    }

    /**
     * Column names for Cursors that represent a sensor data point.
     */
    public static class DataPoint implements BaseColumns {

        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
                + "/vnd.sense_os.data_point";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
                + "/vnd.sense_os.data_point";
        public static final String CONTENT_URI_PATH = "/recent_values";
        @Deprecated
        public static final String CONTENT_PERSISTED_URI_PATH = "/persisted_values";
        public static final String CONTENT_REMOTE_URI_PATH = "/remote_values";

        /**
         * The name of the sensor that generated the data point. <br>
         * <br>
         * TYPE: String
         */
        public static final String SENSOR_NAME = "sensor_name";
        /**
         * Description of the sensor that generated the data point. Can either be the hardware name,
         * or any other useful description. <br>
         * <br>
         * TYPE: String
         */
        public static final String SENSOR_DESCRIPTION = "sensor_description";
        /**
         * The data type of the data point. <br>
         * <br>
         * TYPE: String
         * 
         * @see SenseDataTypes
         */
        public static final String DATA_TYPE = "data_type";
        /**
         * The human readable display name of the sensor that generated the data point. <br>
         * <br>
         * TYPE: String
         * 
         * @see SenseDataTypes
         */
        public static final String DISPLAY_NAME = "display_name";
        /**
         * Time stamp for the data point, in milliseconds. <br>
         * <br>
         * TYPE: long
         */
        public static final String TIMESTAMP = "timestamp";
        /**
         * Data point value. <br>
         * <br>
         * TYPE: String
         */
        public static final String VALUE = "value";
        /**
         * Transmit state of the data point, signalling whether the point has been sent to
         * CommonSense already.<br>
         * <br>
         * TYPE: integer status code: 0 (not sent), or 1 (sent)
         */
        public static final String TRANSMIT_STATE = "transmit_state";
        /**
         * Device UUID of the sensor. Use this for sensors that are originated from
         * "external sensors", or leave <code>null</code> to use the phone as default device.<br>
         * <br>
         * TYPE: String
         */
        public static final String DEVICE_UUID = "device_uuid";

        private DataPoint() {
            // class should not be instantiated
        }
    }

    /**
     * Standard names for sensors.
     * 
     * @see DataPoint#SENSOR_NAME
     */
    public static class SensorNames {

        /**
         * Fall detector sensor. Can be a real fall or a regular free fall for demo's. Part of the
         * Motion sensors.
         */
        public static final String FALL_DETECTOR = "fall_detector";

        /** Noise level sensor. Part of the Ambience sensors. */
        public static final String NOISE = "noise_sensor";

        /** Noise level sensor (Burst-mode). Part of the Ambience sensors. */
        public static final String NOISE_BURST = "noise_sensor (burst-mode)";

        /** Audio spectrum sensor. Part of the Ambience sensors. */
        public static final String AUDIO_SPECTRUM = "audio_spectrum";

        /** Microphone output. Part of the Ambience sensors (real-time mode only). */
        public static final String MIC = "microphone";

        /** Light sensor. Part of the Ambience sensors. */
        public static final String LIGHT = "light";

        /** Camera Light sensor. Part of the Ambience sensors. */
        public static final String CAMERA_LIGHT = "camera_light";

        /** Bluetooth discovery sensor. Part of the Neighboring Devices sensors. */
        public static final String BLUETOOTH_DISCOVERY = "bluetooth_discovery";

        /** Wi-Fi scan sensor. Part of the Neighboring Devices sensors. */
        public static final String WIFI_SCAN = "wifi scan";

        /** NFC sensor. Part of the Neighboring Devices sensors. */
        public static final String NFC_SCAN = "nfc_scan";

        /** Acceleration sensor. Part of the Motion sensors, also used in Zephyr BioHarness */
        public static final String ACCELEROMETER = "accelerometer";

        /** Motion sensor. The basis for the other Motion sensors. */
        public static final String MOTION = "motion";

        /** Linear acceleration sensor name. Part of the Motion sensors. */
        public static final String LIN_ACCELERATION = "linear acceleration";

        /** Gyroscope sensor name. Part of the Motion sensors. */
        public static final String GYRO = "gyroscope";

        /** Magnetic field sensor name. Part of the Ambience sensors. */
        public static final String MAGNETIC_FIELD = "magnetic field";

        /** Orientation sensor name. Part of the Motion sensors. */
        public static final String ORIENT = "orientation";

        /** Epi-mode accelerometer sensor. Special part of the Motion sensors. */
        public static final String ACCELEROMETER_EPI = "accelerometer (epi-mode)";

        /** Burst-mode accelerometer sensor. Special part of the Motion sensors. */
        public static final String ACCELEROMETER_BURST = "accelerometer (burst-mode)";

        /** Burst-mode gyroscope sensor. Special part of the Motion sensors. */
        public static final String GYRO_BURST = "gyroscope (burst-mode)";

        /** Burst-mode linear acceleration sensor. Special part of the Motion sensors. */
        public static final String LINEAR_BURST = "linear acceleration (burst-mode)";

        /** Motion energy sensor name. Special part of the Motion sensors. */
        public static final String MOTION_ENERGY = "motion energy";

        /*** battery sensor for Zephyr BioHarness external sensor */
        public static final String BATTERY_LEVEL = "battery level";

        /** heart rate sensor for Zephyr BioHarness and HxM external sensors */
        public static final String HEART_RATE = "heart rate";

        /** respiration rate sensor for Zephyr BioHarness external sensor */
        public static final String RESPIRATION = "respiration rate";

        /** temperature sensor for Zephyr BioHarness external sensor */
        public static final String TEMPERATURE = "temperature";

        /** worn status for Zephyr BioHarness external sensor */
        public static final String WORN_STATUS = "worn status";

        /** blood pressure sensor */
        public static final String BLOOD_PRESSURE = "blood_pressure";

        /** reaction time sensor */
        public static final String REACTION_TIME = "reaction_time";

        /** speed sensor for Zephyr HxM external sensor */
        public static final String SPEED = "speed";

        /** distance sensor for Zephyr HxM external sensor */
        public static final String DISTANCE = "distance";

        /** battery sensor for Zephyr HxM external sensor */
        public static final String BATTERY_CHARGE = "battery charge";

        /** strides sensor (stappenteller) for Zephyr HxM external sensor */
        public static final String STRIDES = "strides";

        /** Location sensor. */
        public static final String LOCATION = "position";

        /** TimeZone sensor. */
        public static final String TIME_ZONE = "time_zone";

        /** Battery sensor. Part of the Phone State sensors. */
        public static final String BATTERY_SENSOR = "battery sensor";
        
        /** App info sensor. Part of the Phone State sensors. */
        public static final String APP_INFO_SENSOR = "app_info_sensor";

        /** Screen activity sensor. Part of the Phone State sensors. */
        public static final String SCREEN_ACTIVITY = "screen activity";

        /** Pressure sensor. Part of the Phone State sensors. */
        public static final String PRESSURE = "pressure";

        /** Proximity sensor. Part of the Phone State sensors. */
        public static final String PROXIMITY = "proximity";

        /** Call state sensor. Part of the Phone State sensors. */
        public static final String CALL_STATE = "call state";

        /** Data connection state sensor. Part of the Phone State sensors. */
        public static final String DATA_CONN = "data connection";

        /** IP address sensor. Part of the Phone State sensors. */
        public static final String IP_ADDRESS = "ip address";

        /** Mobile service state sensor. Part of the Phone State sensors. */
        public static final String SERVICE_STATE = "service state";

        /** Mobile signal strength sensor. Part of the Phone State sensors. */
        public static final String SIGNAL_STRENGTH = "signal strength";

        /** Unread messages sensor. Part of the Phone State sensors. */
        public static final String UNREAD_MSG = "unread msg";

        /** Data connection type sensor. Part of the Phone State sensors. */
        public static final String CONN_TYPE = "connection type";

        /** Monitor status since DTCs cleared. Part of the OBD-II sensors. */
        public static final String MONITOR_STATUS = "monitor status";

        /** Fuel system status. Part of the OBD-II sensors. */
        public static final String FUEL_SYSTEM_STATUS = "fuel system status";

        /** Calculated engine load. Part of the OBD-II sensors. */
        public static final String ENGINE_LOAD = "calculated engine load value";

        /** Engine coolant. Part of the OBD-II sensors. */
        public static final String ENGINE_COOLANT = "engine coolant";

        /** Short/Long term fuel trim bank 1 & 2. Part of the OBD-II sensors. */
        public static final String FUEL_TRIM = "fuel trim";

        /** Fuel Pressure. Part of the OBD-II sensors. */
        public static final String FUEL_PRESSURE = "fuel pressure";

        /** Intake manifold absolute pressure. Part of the OBD-II sensors. */
        public static final String INTAKE_PRESSURE = "intake manifold absolute pressure";

        /** Engine RPM. Part of the OBD-II sensors. */
        public static final String ENGINE_RPM = "engine RPM";

        /** Vehicle speed. Part of the OBD-II sensors. */
        public static final String VEHICLE_SPEED = "vehicle speed";

        /** Timing advance. Part of the OBD-II sensors. */
        public static final String TIMING_ADVANCE = "timing advance";

        /** Intake air temperature. Part of the OBD-II sensors. */
        public static final String INTAKE_TEMPERATURE = "intake air temperature";

        /** MAF air flow rate. Part of the OBD-II sensors. */
        public static final String MAF_AIRFLOW = "MAF air flow rate";

        /** Throttle position. Part of the OBD-II sensors. */
        public static final String THROTTLE_POSITION = "throttle position";

        /** Commanded secondary air status. Part of the OBD-II sensors. */
        public static final String AIR_STATUS = "commanded secondary air status";

        /** Oxygen sensors. Part of the OBD-II sensors. */
        public static final String OXYGEN_SENSORS = "oxygen sensors";

        /** OBD standards. Part of the OBD-II sensors. */
        public static final String OBD_STANDARDS = "OBD standards";

        /** Auxiliary input status. Part of the OBD-II sensors. */
        public static final String AUXILIARY_INPUT = "auxiliary input status";

        /** Run time since engine start. Part of the OBD-II sensors. */
        public static final String RUN_TIME = "run time";

        /** Ambient temperature sensor. Part of the ambience sensors. From API >= 14 only */
        public static final String AMBIENT_TEMPERATURE = "ambient_temperature";

        /** Relative humidity sensor. Part of the ambience sensors. From API >= 14 only */
        public static final String RELATIVE_HUMIDITY = "relative_humidity";

        /** Bluetooth number of neighbours, count sensor */
        public static final String BLUETOOTH_NEIGHBOURS_COUNT = "bluetooth neighbours count";

        /** Traveled distance for each day */
        public static final String TRAVELED_DISTANCE_24H = "traveled distance 24h";

        /** Traveled distance for each hour */
        public static final String TRAVELED_DISTANCE_1H = "traveled distance 1h";

        public static final String LOUDNESS = "loudness";

        public static final String ATTACHED_TO_MYRIANODE = "attachedToMyriaNode";

        public static final String APP_INSTALLED = "installed_apps";

        public static final String APP_FOREGROUND = "foreground_app";
        
        public static final String GEOFENCE = "geofence_sensor";

        private SensorNames() {
            // class should not be instantiated
        }
    }

    /**
     * Standard descriptions for sensors.
     * 
     * @see DataPoint#SENSOR_DESCRIPTION
     */
    public static class SensorDescriptions {
        /**
         * Auto-calibrated noise level sensor. Part of the Ambience sensors.
         */
        public static final String AUTO_CALIBRATED = "auto-calibrated";
    }

    private SensorData() {
        // do not instantiate
    }
}
