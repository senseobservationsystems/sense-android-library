package nl.sense_os.service;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

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
        public static final Uri CONTENT_URI = Uri.parse("content://"
                + "nl.sense_os.service.provider.LocalStorage" + "/recent_values");

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
         * Noise level sensor. Part of the Ambience sensors.
         */
        public static final String NOISE = "noise_sensor";

        /**
         * Microphone output. Part of the Ambience sensors (real-time mode only).
         */
        public static final String MIC = "microphone";

        /**
         * Light sensor. Part of the Ambience sensors.
         */
        public static final String LIGHT = "light";

        /**
         * Bluetooth discovery sensor. Part of the Neighboring Devices sensors.
         */
        public static final String BLUETOOTH_DISCOVERY = "bluetooth_discovery";

        /**
         * Wi-Fi scan sensor. Part of the Neighboring Devices sensors.
         */
        public static final String WIFI_SCAN = "wifi scan";

        /**
         * Accelerometer. Part of the Motion sensors, also used in Zephyr BioHarness external
         * sensor.
         */
        public static final String ACCELEROMETER = "accelerometer";

        /**
         * Linear acceleration sensor name. Part of the Motion sensors.
         */
        public static final String LIN_ACCELERATION = "linear acceleration";

        /**
         * Gyroscope sensor name. Part of the Motion sensors.
         */
        public static final String GYRO = "gyroscope";

        /**
         * Magnetic field sensor name. Part of the Motion sensors.
         */
        public static final String MAGNET_FIELD = "magnetic_field";

        /**
         * Orientation sensor name. Part of the Motion sensors.
         */
        public static final String ORIENT = "orientation";

        /**
         * Epi-mode accelerometer sensor. Special part of the Motion sensors.
         */
        public static final String ACCELEROMETER_EPI = "accelerometer (epi-mode)";

        /**
         * Motion energy sensor name. Special part of the Motion sensors.
         */
        public static final String MOTION_ENERGY = "motion energy";

        /**
         * battery sensor for Zephyr BioHarness external sensor
         */
        public static final String BATTERY_LEVEL = "battery level";

        /**
         * heart rate sensor for Zephyr BioHarness and HxM external sensors
         */
        public static final String HEART_RATE = "heart rate";

        /**
         * respiration rate sensor for Zephyr BioHarness external sensor
         */
        public static final String RESPIRATION = "respiration rate";

        /**
         * temperature sensor for Zephyr BioHarness external sensor
         */
        public static final String TEMPERATURE = "temperature";

        /**
         * worn status for Zephyr BioHarness external sensor
         */
        public static final String WORN_STATUS = "worn status";

        /**
         * blood pressure sensor
         */
        public static final String BLOOD_PRESSURE = "blood pressure";

        /**
         * strides sensor (stappenteller) for Zephyr HxM external sensor
         */
        public static final String SPEED = "speed";

        /**
         * distance sensor for Zephyr HxM external sensor
         */
        public static final String DISTANCE = "distance";

        /**
         * battery sensor for Zephyr HxM external sensor
         */
        public static final String BATTERY_CHARGE = "battery charge";

        /**
         * strides sensor (stappenteller) for Zephyr HxM external sensor
         */
        public static final String STRIDES = "strides";

        /**
         * Location sensor.
         */
        public static final String LOCATION = "position";

        /**
         * Battery sensor. Part of the Phone State sensors.
         */
        public static final String BATTERY_SENSOR = "battery sensor";

        /**
         * Screen activity sensor. Part of the Phone State sensors.
         */
        public static final String SCREEN_ACTIVITY = "screen activity";

        /**
         * Pressure sensor. Part of the Phone State sensors.
         */
        public static final String PRESSURE = "pressure";

        /**
         * Proximity sensor. Part of the Phone State sensors.
         */
        public static final String PROXIMITY = "proximity";

        /**
         * Call state sensor. Part of the Phone State sensors.
         */
        public static final String CALL_STATE = "call state";

        /**
         * Data connection state sensor. Part of the Phone State sensors.
         */
        public static final String DATA_CONN = "data connection";

        /**
         * IP address sensor. Part of the Phone State sensors.
         */
        public static final String IP_ADDRESS = "ip address";

        /**
         * Mobile service state sensor. Part of the Phone State sensors.
         */
        public static final String SERVICE_STATE = "service state";

        /**
         * Mobile signal strength sensor. Part of the Phone State sensors.
         */
        public static final String SIGNAL_STRENGTH = "signal strength";

        /**
         * Unread messages sensor. Part of the Phone State sensors.
         */
        public static final String UNREAD_MSG = "unread msg";

        /**
         * Data connection type sensor. Part of the Phone State sensors.
         */
        public static final String CONN_TYPE = "connection type";

        private SensorNames() {
            // class should not be instantiated
        }
    }
}
