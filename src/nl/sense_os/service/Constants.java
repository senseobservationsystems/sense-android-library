package nl.sense_os.service;

import android.content.Context;

public class Constants {

    /* ======================================================================================== */
    /* -------------------------Sense service status preferences ------------------------------ */
    /* ======================================================================================== */
    /**
     * Name of shared preferences file holding the desired status of the Sense service.
     * 
     * @see #AUTHENTICATION_PREFS
     * @see #MAIN_PREFS
     */
    public static final String STATUS_PREFS = "service_status_prefs";

    /**
     * Key for the main status of the sensors. Set to <code>false</code> to disable all the sensing
     * components.
     * 
     * @see #STATUS_PREFS
     */
    public static final String PREF_STATUS_MAIN = "main service status";

    /**
     * Key for the status of the "ambience" sensors. Set to <code>true</code> to enable sensing.
     * 
     * @see #STATUS_PREFS
     */
    public static final String PREF_STATUS_AMBIENCE = "ambience component status";

    /**
     * Key for the status of the "device proximity" sensors. Set to <code>true</code> to enable
     * sensing.
     * 
     * @see #STATUS_PREFS
     */
    public static final String PREF_STATUS_DEV_PROX = "device proximity component status";

    /**
     * Key for the status of the external Bluetooth sensors. Set to <code>true</code> to enable
     * sensing.
     * 
     * @see #STATUS_PREFS
     */
    public static final String PREF_STATUS_EXTERNAL = "external services component status";

    /**
     * Key for the status of the location sensors. Set to <code>true</code> to enable sensing.
     * 
     * @see #STATUS_PREFS
     */
    public static final String PREF_STATUS_LOCATION = "location component status";

    /**
     * Key for the status of the motion sensors. Set to <code>true</code> to enable sensing.
     * 
     * @see #STATUS_PREFS
     */
    public static final String PREF_STATUS_MOTION = "motion component status";

    /**
     * Key for the status of the "phone state" sensors. Set to <code>true</code> to enable sensing.
     * 
     * @see #STATUS_PREFS
     */
    public static final String PREF_STATUS_PHONESTATE = "phone state component status";

    /**
     * Key for the status of the questionnaire. Set to <code>true</code> to enable it.
     * 
     * @see #STATUS_PREFS
     */
    public static final String PREF_STATUS_POPQUIZ = "pop quiz component status";

    /**
     * Key for preference to automatically start the Sense service on boot.
     * 
     * @see #STATUS_PREFS
     */
    public static final String PREF_AUTOSTART = "autostart";

    /**
     * Key for storing if the service is supposed to be "alive", used for aggressive restarting
     * after crashes.
     * 
     * @see #STATUS_PREFS
     */
    public static final String PREF_ALIVE = "alive";

    /* ======================================================================================== */
    /* --------------------- CommonSense authentication preferences --------------------------- */
    /* ======================================================================================== */
    /**
     * Name of the shared preferences file used for storing CommonSense authentication data. Use
     * {@link Context#MODE_PRIVATE}.
     * 
     * @see #MAIN_PREFS_PREFS
     * @see #STATUS_PREFS
     */
    public static final String AUTH_PREFS = "authentication";// "login";

    /**
     * Key for generic login preference that displays the login dialog when clicked.
     * 
     * @see #AUTHENTICATION_PREFS
     */
    public static final String PREF_LOGIN = "login";

    /**
     * Key for login preference for session cookie.
     * 
     * @see #AUTHENTICATION_PREFS
     */
    public static final String PREF_LOGIN_COOKIE = "login_cookie";

    /**
     * Key for login preference for email address.
     * 
     * @see #AUTHENTICATION_PREFS
     */
    public static final String PREF_LOGIN_USERNAME = "login_mail";

    /**
     * Key for login preference for username.
     * 
     * @see #AUTHENTICATION_PREFS
     */
    @Deprecated
    public static final String PREF_LOGIN_NAME = "login_name";

    /**
     * Key for storing the online sensor list (type of JSONArray).
     * 
     * @see #AUTHENTICATION_PREFS
     */
    public static final String PREF_SENSOR_LIST = "sensor_list";

    /**
     * Key for storing the retrieval time of online sensor list.
     * 
     * @see #AUTHENTICATION_PREFS
     */
    public static final String PREF_SENSOR_LIST_TIME = "sensor_list_timestamp";

    /**
     * Key for storing the online device id.
     * 
     * @see #AUTHENTICATION_PREFS
     */
    public static final String PREF_DEVICE_ID = "device_id";

    /**
     * Key for storing the retrieval time of the online device id.
     * 
     * @see #AUTHENTICATION_PREFS
     */
    public static final String PREF_DEVICE_ID_TIME = "device_id_timestamp";

    /**
     * Key for storing the online device type.
     * 
     * @see #AUTHENTICATION_PREFS
     */
    public static final String PREF_DEVICE_TYPE = "device_type";

    /**
     * Key for storing the IMEI of the phone.
     * 
     * @see #AUTHENTICATION_PREFS
     */
    public static final String PREF_PHONE_IMEI = "phone_imei";

    /**
     * Key for storing the type of the phone.
     * 
     * @see #AUTHENTICATION_PREFS
     */
    public static final String PREF_PHONE_TYPE = "phone_type";

    /**
     * Key for login preference for hashed password.
     * 
     * @see #AUTHENTICATION_PREFS
     */
    public static final String PREF_LOGIN_PASS = "login_pass";

    /* ======================================================================================== */
    /* -------------------------- Main Sense service preferences ------------------------------ */
    /* ======================================================================================== */
    /**
     * Name of the main preference file, used for storing the settings for the Sense service.
     * 
     * @see #AUTHENTICATION_PREFS
     * @see #STATUS_PREFS
     */
    public static final String MAIN_PREFS = "main";

    /**
     * Key for preference that toggles use of light sensor in ambience sensing.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_AMBIENCE_LIGHT = "ambience_light";

    /**
     * Key for preference that toggles use of the microphone in ambience sensing.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_AMBIENCE_MIC = "ambience_mic";

    /**
     * Key for preference that controls sample frequency of the sensors.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_SAMPLE_RATE = "commonsense_rate";

    /**
     * Key for preference that saves the last running services.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_LAST_STATUS = "last_status";

    /**
     * Key for preference that toggles use of GPS in location sensor.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_LOCATION_GPS = "location_gps";

    /**
     * Key for preference that toggles use of Network in location sensor.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_LOCATION_NETWORK = "location_network";

    /**
     * Key for preference that toggles use of GPS in location sensor.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_PROXIMITY_BT = "proximity_bt";

    /**
     * Key for preference that toggles use of Bluetooth in the DeviceProximity sensor.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_PROXIMITY_WIFI = "proximity_wifi";

    /**
     * Key for preference that toggles use of Bluetooth in the DeviceProximity sensor.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_MOTION_FALL_DETECT = "motion_fall_detector";

    /**
     * Key for preference that toggles use of Bluetooth in the DeviceProximity sensor.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_MOTION_FALL_DETECT_DEMO = "motion_fall_detector_demo";

    /**
     * Key for preference that sets the interval between pop quizzes.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_QUIZ_RATE = "popquiz_rate";

    /**
     * Key for preference that sets the silent mode for pop quizzes.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_QUIZ_SILENT_MODE = "popquiz_silent_mode";

    /**
     * Key for generic preference that starts an update of the quiz questions when clicked.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_QUIZ_SYNC = "popquiz_sync";

    /**
     * Key for preference that holds the last update time of the quiz questions with CommonSense.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_QUIZ_SYNC_TIME = "popquiz_sync_time";

    /**
     * Key for generic preference that shows the registration dialog when clicked.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_REGISTER = "register";

    /**
     * Key for preference that controls sync frequency with CommonSense.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_SYNC_RATE = "sync_rate";

    /**
     * Key for preference that toggles use of the Zephyr BioHarness.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_BIOHARNESS = "zephyrBioHarness";

    /**
     * Key for preference that toggles use of the Zephyr BioHarness Accelerometer.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_BIOHARNESS_ACC = "zephyrBioHarness_acc";
    /**
     * Key for preference that toggles use of the Zephyr BioHarness Heart rate.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_BIOHARNESS_HEART_RATE = "zephyrBioHarness_heartRate";

    /**
     * Key for preference that toggles use of the Zephyr BioHarness Temperature.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_BIOHARNESS_TEMP = "zephyrBioHarness_temp";

    /**
     * Key for preference that toggles use of the Zephyr BioHarness Respiration rate.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_BIOHARNESS_RESP = "zephyrBioHarness_resp";

    /**
     * Key for preference that toggles use of the Zephyr BioHarness Blood pressure.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_BIOHARNESS_BLOOD_PRESSURE = "zephyrBioHarness_bloodP";

    /**
     * Key for preference that toggles use of the Zephyr BioHarness worn status.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_BIOHARNESS_WORN_STATUS = "zephyrBioHarness_wornStatus";

    /**
     * Key for preference that toggles use of the Zephyr BioHarness battery level.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_BIOHARNESS_BATTERY = "zephyrBioHarness_battery";

    /**
     * Key for preference that toggles use of the Zephyr HxM.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_HXM = "zephyrHxM";

    /**
     * Key for preference that toggles use of the Zephyr HxM speed.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_HXM_SPEED = "zephyrHxM_speed";

    /**
     * Key for preference that toggles use of the Zephyr HxM heart rate.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_HXM_HEART_RATE = "zephyrHxM_heartRate";

    /**
     * Key for preference that toggles use of the Zephyr HxM battery.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_HXM_BATTERY = "zephyrHxM_battery";

    /**
     * Key for preference that toggles use of the Zephyr HxM distance.
     * 
     * @see #MAIN_PREFS
     */
    public static final String PREF_HXM_DISTANCE = "zephyrHxM_distance";

    /* ======================================================================================== */
    /* ---------------- Codes to keep track of the active sensing modules --------------------- */
    /* ======================================================================================== */
    public static final int STATUSCODE_AMBIENCE = 0x01;
    public static final int STATUSCODE_CONNECTED = 0x02;
    public static final int STATUSCODE_DEVICE_PROX = 0x04;
    public static final int STATUSCODE_EXTERNAL = 0x08;
    public static final int STATUSCODE_LOCATION = 0x10;
    public static final int STATUSCODE_MOTION = 0x20;
    public static final int STATUSCODE_PHONESTATE = 0x40;
    public static final int STATUSCODE_QUIZ = 0x80;
    public static final int STATUSCODE_RUNNING = 0x100;

    /* ======================================================================================== */
    /* ----------------------------------- CommonSense URls ----------------------------------- */
    /* ======================================================================================== */
    public static final String URL_BASE = "http://api.sense-os.nl/";
    public static final String URL_VERSION = "http://data.sense-os.nl/senseapp/version.php";
    public static final String URL_FORMAT = ".json";
    public static final String URL_GET_DEVICES = URL_BASE + "devices" + URL_FORMAT;
    public static final String URL_GET_SENSORS = URL_BASE + "devices/<id>/sensors" + URL_FORMAT;
    public static final String URL_POST_SENSOR_DATA = URL_BASE + "sensors/<id>/data" + URL_FORMAT;
    public static final String URL_POST_FILE = URL_BASE + "sensors/<id>/file" + URL_FORMAT;
    public static final String URL_CREATE_SENSOR = URL_BASE + "sensors" + URL_FORMAT;
    public static final String URL_ADD_SENSOR_TO_DEVICE = URL_BASE + "sensors/<id>/device"
            + URL_FORMAT;
    public static final String URL_LOGIN = URL_BASE + "login" + URL_FORMAT;
    public static final String URL_REG = URL_BASE + "users" + URL_FORMAT;

    /* ======================================================================================== */
    /* -------------------------- Sensor data type designations ------------------------------- */
    /* ======================================================================================== */
    public static final String SENSOR_DATA_TYPE_BOOL = "bool";
    public static final String SENSOR_DATA_TYPE_FLOAT = "float";
    public static final String SENSOR_DATA_TYPE_INT = "int";
    public static final String SENSOR_DATA_TYPE_JSON = "json";
    public static final String SENSOR_DATA_TYPE_STRING = "string";
    public static final String SENSOR_DATA_TYPE_FILE = "file";

}
