package nl.sense_os.service.constants;

import android.content.Context;

public class SensePrefs {

    /**
     * Keys for the authentication-related preferences of the Sense Platform
     */
    public static class Auth {
        /**
         * Key for login preference for session cookie.
         * 
         * @see SensePrefs#AUTH_PREFS
         */
        public static final String LOGIN_COOKIE = "login_cookie";
        /**
         * Key for login preference for email address.
         * 
         * @see SensePrefs#AUTH_PREFS
         */
        public static final String LOGIN_USERNAME = "login_mail";
        /**
         * Key for login preference for hashed password.
         * 
         * @see SensePrefs#AUTH_PREFS
         */
        public static final String LOGIN_PASS = "login_pass";
        /**
         * Key for storing the online sensor list for this device (type of JSONArray).
         * 
         * @see #SENSOR_LIST_COMPLETE
         * @see SensePrefs#AUTH_PREFS
         */
        public static final String SENSOR_LIST = "sensor_list";
        /**
         * Key for storing the online sensor list for this user (type of JSONArray).
         * 
         * @see #SENSOR_LIST
         * @see SensePrefs#AUTH_PREFS
         */
        public static final String SENSOR_LIST_COMPLETE = "sensor_list_complete";
        /**
         * Key for storing the retrieval time of device's online sensor list.
         * 
         * @see #SENSOR_LIST_COMPLETE_TIME
         * @see SensePrefs#AUTH_PREFS
         */
        public static final String SENSOR_LIST_TIME = "sensor_list_timestamp";
        /**
         * Key for storing the retrieval time of complete online sensor list.
         * 
         * @see #SENSOR_LIST_TIME
         * @see SensePrefs#AUTH_PREFS
         */
        public static final String SENSOR_LIST_COMPLETE_TIME = "sensor_list_complete_timestamp";
        /**
         * Key to use the development version of CommonSense.
         * 
         * @see SensePrefs#AUTH_PREFS
         */
        public static final String DEV_MODE = "devmode";
        /**
         * Key for storing the online device id.
         * 
         * @see SensePrefs#AUTH_PREFS
         */
        public static final String DEVICE_ID = "device_id";
        /**
         * Key for storing the retrieval time of the online device id.
         * 
         * @see SensePrefs#AUTH_PREFS
         */
        public static final String DEVICE_ID_TIME = "device_id_timestamp";
        /**
         * Key for storing the online device type.
         * 
         * @see SensePrefs#AUTH_PREFS
         */
        public static final String DEVICE_TYPE = "device_type";
        /**
         * Key for storing the IMEI of the phone.
         * 
         * @see SensePrefs#AUTH_PREFS
         */
        public static final String PHONE_IMEI = "phone_imei";
        /**
         * Key for storing the type of the phone.
         * 
         * @see SensePrefs#AUTH_PREFS
         */
        public static final String PHONE_TYPE = "phone_type";
    }

    /**
     * Keys for the main Sense Platform service preferences
     */
    public static class Main {

        public static class Advanced {
            /**
             * Key for preference that toggles use of compression for transmission. Default is true.
             * 
             * @see SensePrefs#MAIN_PREFS
             */
            public static final String COMPRESS = "compression";
            /**
             * Key for preference that enables local storage, making the sensor data available to
             * other apps through a ContentProvider. Default is true.
             * 
             * @see SensePrefs#MAIN_PREFS
             * @deprecated Local storage is always on.
             */
            public static final String LOCAL_STORAGE = "local_storage";
            /**
             * Key for preference that enables communication with CommonSense. Disable this to work
             * in local-only mode. Default is true.
             */
            public static final String USE_COMMONSENSE = "use_commonsense";
        }

        public static class Ambience {
            /**
             * Key for preference that toggles use of light sensor in ambience sensing.
             * 
             * @see SensePrefs#MAIN_PREFS
             */
            public static final String LIGHT = "ambience_light";
            /**
             * Key for preference that toggles use of the microphone in ambience sensing.
             * 
             * @see SensePrefs#MAIN_PREFS
             */
            public static final String MIC = "ambience_mic";
            /**
             * @see SensePrefs#MAIN_PREFS
             */
            public static final String PRESSURE = "ambience_pressure";
        }

        public static class DevProx {
            /**
             * Key for preference that toggles use of Bluetooth in the DeviceProximity sensor.
             * 
             * @see SensePrefs#MAIN_PREFS
             */
            public static final String BLUETOOTH = "proximity_bt";
            /**
             * Key for preference that toggles use of Wi-Fi in the DeviceProximity sensor.
             * 
             * @see SensePrefs#MAIN_PREFS
             */
            public static final String WIFI = "proximity_wifi";
        }

        public static class External {

            public static class MyGlucoHealth {
                /**
                 * Key for preference that toggles use of the MyGlucohealth sensor.
                 * 
                 * @see SensePrefs#MAIN_PREFS
                 */
                public static final String MAIN = "myglucohealth";
            }

            public static class TanitaScale {
                /**
                 * Key for preference that toggles use of the Tanita scale sensor.
                 * 
                 * @see SensePrefs#MAIN_PREFS
                 */
                public static final String MAIN = "tanita_scale";
            }

            public static class ZephyrBioHarness {

                /**
                 * Key for preference that toggles use of the Zephyr BioHarness.
                 * 
                 * @see SensePrefs#MAIN_PREFS
                 */
                public static final String MAIN = "zephyrBioHarness";
                /**
                 * Key for preference that toggles use of the Zephyr BioHarness Accelerometer.
                 * 
                 * @see SensePrefs#MAIN_PREFS
                 */
                public static final String ACC = "zephyrBioHarness_acc";
                /**
                 * Key for preference that toggles use of the Zephyr BioHarness Heart rate.
                 * 
                 * @see SensePrefs#MAIN_PREFS
                 */
                public static final String HEART_RATE = "zephyrBioHarness_heartRate";
                /**
                 * Key for preference that toggles use of the Zephyr BioHarness Temperature.
                 * 
                 * @see SensePrefs#MAIN_PREFS
                 */
                public static final String TEMP = "zephyrBioHarness_temp";
                /**
                 * Key for preference that toggles use of the Zephyr BioHarness Respiration rate.
                 * 
                 * @see SensePrefs#MAIN_PREFS
                 */
                public static final String RESP = "zephyrBioHarness_resp";
                /**
                 * Key for preference that toggles use of the Zephyr BioHarness worn status.
                 * 
                 * @see SensePrefs#MAIN_PREFS
                 */
                public static final String WORN_STATUS = "zephyrBioHarness_wornStatus";
                /**
                 * Key for preference that toggles use of the Zephyr BioHarness battery level.
                 * 
                 * @see SensePrefs#MAIN_PREFS
                 */
                public static final String BATTERY = "zephyrBioHarness_battery";
            }

            public static class ZephyrHxM {
                /**
                 * Key for preference that toggles use of the Zephyr HxM.
                 * 
                 * @see SensePrefs#MAIN_PREFS
                 */
                public static final String MAIN = "zephyrHxM";
                /**
                 * Key for preference that toggles use of the Zephyr HxM speed.
                 * 
                 * @see SensePrefs#MAIN_PREFS
                 */
                public static final String SPEED = "zephyrHxM_speed";
                /**
                 * Key for preference that toggles use of the Zephyr HxM heart rate.
                 * 
                 * @see SensePrefs#MAIN_PREFS
                 */
                public static final String HEART_RATE = "zephyrHxM_heartRate";
                /**
                 * Key for preference that toggles use of the Zephyr HxM battery.
                 * 
                 * @see SensePrefs#MAIN_PREFS
                 */
                public static final String BATTERY = "zephyrHxM_battery";
                /**
                 * Key for preference that toggles use of the Zephyr HxM distance.
                 * 
                 * @see SensePrefs#MAIN_PREFS
                 */
                public static final String DISTANCE = "zephyrHxM_distance";
                /**
                 * Key for preference that toggles use of the Zephyr HxM strides.
                 * 
                 * @see SensePrefs#MAIN_PREFS
                 */
                public static final String STRIDES = "zephyrHxM_strides";
            }

            public static class OBD2Dongle {
                /**
                 * Key for preference that toggles use of the OBD-II dongle.
                 * 
                 * @see SensePrefs#MAIN_PREFS
                 */
                public static final String MAIN = "obd2dongle";
            }
        }

        public static class Location {
            /**
             * Key for preference that toggles use of GPS in location sensor.
             * 
             * @see SensePrefs#MAIN_PREFS
             */
            public static final String GPS = "location_gps";
            /**
             * Key for preference that toggles use of Network in location sensor.
             * 
             * @see SensePrefs#MAIN_PREFS
             */
            public static final String NETWORK = "location_network";
            /**
             * Key for preference that toggles use of sensor fusion to toggle th GPS usage.
             * 
             * @see SensePrefs#MAIN_PREFS
             */
            public static final String AUTO_GPS = "automatic_gps";
        }

        public static class Motion {
            /**
             * Key for preference that toggles use of Bluetooth in the DeviceProximity sensor.
             * 
             * @see SensePrefs#MAIN_PREFS
             */
            public static final String FALL_DETECT = "motion_fall_detector";
            /**
             * Key for preference that toggles use of Bluetooth in the DeviceProximity sensor.
             * 
             * @see SensePrefs#MAIN_PREFS
             */
            public static final String FALL_DETECT_DEMO = "motion_fall_detector_demo";
            /**
             * Key for preference that toggles "epi-mode", drastically changing motion sensing
             * 
             * @see SensePrefs#MAIN_PREFS
             */
            public static final String EPIMODE = "epimode";
            /**
             * Key for preference that determines whether to unregister the motion sensor between
             * samples. Nota bene: unregistering the sensor breaks the screen rotation on some
             * phones (e.g. Nexus S).
             * 
             * @see SensePrefs#MAIN_PREFS
             */
            public static final String UNREG = "motion_unregister";
            /**
             * Key for preference that toggles motion energy sensing, which measures average kinetic
             * energy over a sample period.
             * 
             * @see SensePrefs#MAIN_PREFS
             */
            public static final String MOTION_ENERGY = "motion_energy";
            /**
             * Key for preference that enables fix that re-registers the motion sensor when the
             * screen turns off.
             * 
             * @see SensePrefs#MAIN_PREFS
             */
            public static final String SCREENOFF_FIX = "screenoff_fix";
        }

        public static class PhoneState {
            /**
             * @see SensePrefs#MAIN_PREFS
             */
            public static final String BATTERY = "phonestate_battery";
            /**
             * @see SensePrefs#MAIN_PREFS
             */
            public static final String SCREEN_ACTIVITY = "phonestate_screen_activity";
            /**
             * @see SensePrefs#MAIN_PREFS
             */
            public static final String PROXIMITY = "phonestate_proximity";
            /**
             * @see SensePrefs#MAIN_PREFS
             */
            public static final String IP_ADDRESS = "phonestate_ip";
            /**
             * @see SensePrefs#MAIN_PREFS
             */
            public static final String DATA_CONNECTION = "phonestate_data_connection";
            /**
             * @see SensePrefs#MAIN_PREFS
             */
            public static final String UNREAD_MSG = "phonestate_unread_msg";
            /**
             * @see SensePrefs#MAIN_PREFS
             */
            public static final String SERVICE_STATE = "phonestate_service_state";
            /**
             * @see SensePrefs#MAIN_PREFS
             */
            public static final String SIGNAL_STRENGTH = "phonestate_signal_strength";
            /**
             * @see SensePrefs#MAIN_PREFS
             */
            public static final String CALL_STATE = "phonestate_call_state";
        }

        public static class Quiz {
            /**
             * Key for preference that sets the interval between pop quizzes.
             * 
             * @see SensePrefs#MAIN_PREFS
             */
            public static final String RATE = "popquiz_rate";
            /**
             * Key for preference that sets the silent mode for pop quizzes.
             * 
             * @see SensePrefs#MAIN_PREFS
             */
            public static final String SILENT_MODE = "popquiz_silent_mode";
            /**
             * Key for generic preference that starts an update of the quiz questions when clicked.
             * 
             * @see SensePrefs#MAIN_PREFS
             */
            public static final String SYNC = "popquiz_sync";
            /**
             * Key for preference that holds the last update time of the quiz questions with
             * CommonSense.
             * 
             * @see SensePrefs#MAIN_PREFS
             */
            public static final String SYNC_TIME = "popquiz_sync_time";
        }

        /**
         * Key for preference that controls sample frequency of the sensors.
         * 
         * @see SensePrefs#MAIN_PREFS
         */
        public static final String SAMPLE_RATE = "commonsense_rate";
        /**
         * Key for preference that controls sync frequency with CommonSense.
         * 
         * @see SensePrefs#MAIN_PREFS
         */
        public static final String SYNC_RATE = "sync_rate";
        /**
         * Key for preference that saves the last running services.
         * 
         * @see SensePrefs#MAIN_PREFS
         */
        public static final String LAST_STATUS = "last_status";
    }

    /**
     * Keys for the status preferences of the Sense Platform service
     */
    public static class Status {
        /**
         * Key for the main status of the sensors. Set to <code>false</code> to disable all the
         * sensing components.
         * 
         * @see SensePrefs#STATUS_PREFS
         */
        public static final String MAIN = "main service status";
        /**
         * Key for the status of the "ambience" sensors. Set to <code>true</code> to enable sensing.
         * 
         * @see SensePrefs#STATUS_PREFS
         */
        public static final String AMBIENCE = "ambience component status";
        /**
         * Key for the status of the "device proximity" sensors. Set to <code>true</code> to enable
         * sensing.
         * 
         * @see SensePrefs#STATUS_PREFS
         */
        public static final String DEV_PROX = "device proximity component status";
        /**
         * Key for the status of the external Bluetooth sensors. Set to <code>true</code> to enable
         * sensing.
         * 
         * @see SensePrefs#STATUS_PREFS
         */
        public static final String EXTERNAL = "external services component status";
        /**
         * Key for the status of the location sensors. Set to <code>true</code> to enable sensing.
         * 
         * @see SensePrefs#STATUS_PREFS
         */
        public static final String LOCATION = "location component status";
        /**
         * Key for the status of the motion sensors. Set to <code>true</code> to enable sensing.
         * 
         * @see SensePrefs#STATUS_PREFS
         */
        public static final String MOTION = "motion component status";
        /**
         * Key for the status of the "phone state" sensors. Set to <code>true</code> to enable
         * sensing.
         * 
         * @see SensePrefs#STATUS_PREFS
         */
        public static final String PHONESTATE = "phone state component status";
        /**
         * Key for the status of the questionnaire. Set to <code>true</code> to enable it.
         * 
         * @see SensePrefs#STATUS_PREFS
         */
        public static final String POPQUIZ = "pop quiz component status";
        /**
         * Key for preference to automatically start the Sense service on boot.
         * 
         * @see SensePrefs#STATUS_PREFS
         */
        public static final String AUTOSTART = "autostart";
    }

    /**
     * Name of the shared preferences file used for storing CommonSense authentication data. Use
     * {@link Context#MODE_PRIVATE}.
     * 
     * @see #MAIN_PREFS_PREFS
     * @see #STATUS_PREFS
     */
    public static final String AUTH_PREFS = "authentication";// "login";
    /**
     * Name of the main preference file, used for storing the settings for the Sense service.
     * 
     * @see #AUTH_PREFS
     * @see #STATUS_PREFS
     */
    public static final String MAIN_PREFS = "main";
    /**
     * Name of shared preferences file holding the desired status of the Sense service.
     * 
     * @see #AUTH_PREFS
     * @see #MAIN_PREFS
     */
    public static final String STATUS_PREFS = "service_status_prefs";

    private SensePrefs() {
        // private constructor to prevent instantiation
    }
}