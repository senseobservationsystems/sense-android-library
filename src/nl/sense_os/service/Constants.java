package nl.sense_os.service;

public class Constants {

    /* keys for 'service status' preferences, that are used to toggle the active sensor modules */
    public static final String STATUSPREFS = "service_status_prefs";
    public static final String PREF_STATUS_AMBIENCE = "ambience component status";
    public static final String PREF_STATUS_DEV_PROX = "device proximity component status";
    public static final String PREF_STATUS_EXTERNAL = "external services component status";
    public static final String PREF_STATUS_LOCATION = "location component status";
    public static final String PREF_STATUS_MAIN = "main service status";
    public static final String PREF_STATUS_MOTION = "motion component status";
    public static final String PREF_STATUS_PHONESTATE = "phone state component status";
    public static final String PREF_STATUS_POPQUIZ = "pop quiz component status";

    /* keys for private 'login' preferences, storing the cookies and login data */
    /** Name of the private preference file, used for storing login data. */
    public static final String PRIVATE_PREFS = "login";
    /** Key for generic login preference that displays the login dialog when clicked. */
    public static final String PREF_LOGIN = "login";
    /** Key for login preference for session cookie. */
    public static final String PREF_LOGIN_COOKIE = "login_cookie";
    /** Key for login preference for email address. */
    public static final String PREF_LOGIN_MAIL = "login_mail";
    /** Key for login preference for username. */
    @Deprecated
    public static final String PREF_LOGIN_NAME = "login_name";
    /** Key for login preference for hashed password. */
    public static final String PREF_LOGIN_PASS = "login_pass";

    /* keys for the 'main' preferences, setting the sensor parameters */
    /** Key for preference for the version of CommonSense */
    public static final String PREF_COMMONSENSE_VERSION = "cs_version";
    /** Preference for keeping track of the first successful login */
    public static final String PREF_FIRSTLOGIN = "first_login_complete";
    /** Key for storing if the service is "alive", used for aggressive restarting after crashes. */
    public static final String PREF_ALIVE = "alive";
    /** Key for preference to autostart the sense service in boot. */
    public static final String PREF_AUTOSTART = "autostart";

    /** Key for preference that toggles use of light sensor in ambience sensing. */
    public static final String PREF_AMBIENCE_LIGHT = "ambience_light";
    /** Key for preference that toggles use of the microphone in ambience sensing. */
    public static final String PREF_AMBIENCE_MIC = "ambience_mic";
    /** Key for preference that controls sample frequency of the sensors. */
    public static final String PREF_SAMPLE_RATE = "commonsense_rate";
    /** Key for preference that saves the last running services. */
    public static final String PREF_LAST_STATUS = "last_status";
    /** Key for preference that toggles use of GPS in location sensor. */
    public static final String PREF_LOCATION_GPS = "location_gps";
    /** Key for preference that toggles use of Network in location sensor. */
    public static final String PREF_LOCATION_NETWORK = "location_network";
    /** Key for storing the online device id. */
    public static final String PREF_DEVICE_ID = "device_id";
    /** Key for storing the online device type. */
    public static final String PREF_DEVICE_TYPE = "device_type";
    /** Key for storing the online sensor list (type of JSONArray). */
    public static final String PREF_JSON_SENSOR_LIST = "json_sensor_list";
    /** Key for storing the imei of the phone. */
    public static final String PREF_PHONE_IMEI = "phone_imei";
    /** Key for storing the type of the phone. */
    public static final String PREF_PHONE_TYPE = "phone_type";
    /** Key for preference that toggles use of GPS in location sensor. */
    public static final String PREF_PROXIMITY_BT = "proximity_bt";
    /** Key for preference that toggles use of Bluetooth in the DeviceProximity sensor. */
    public static final String PREF_PROXIMITY_WIFI = "proximity_wifi";
    /** Key for preference that toggles use of Bluetooth in the DeviceProximity sensor. */
    public static final String PREF_MOTION_FALL_DETECT = "motion_fall_detector";
    /** Key for preference that toggles use of Bluetooth in the DeviceProximity sensor. */
    public static final String PREF_MOTION_FALL_DETECT_DEMO = "motion_fall_detector_demo";
    /** Key for preference that sets the interval between pop quizzes. */
    public static final String PREF_QUIZ_RATE = "popquiz_rate";
    /** Key for preference that sets the silent mode for pop quizzes. */
    public static final String PREF_QUIZ_SILENT_MODE = "popquiz_silent_mode";
    /** Key for generic preference that starts an update of the quiz questions when clicked. */
    public static final String PREF_QUIZ_SYNC = "popquiz_sync";
    /** Key for preference that holds the last update time of the quiz questions with CommonSense. */
    public static final String PREF_QUIZ_SYNC_TIME = "popquiz_sync_time";
    /** Key for generic preference that shows the registration dialog when clicked. */
    public static final String PREF_REGISTER = "register";
    /** Key for preference that controls sync frequency with CommonSense. */
    public static final String PREF_SYNC_RATE = "sync_rate";
    /** Key for preference that toggles use of the Zephyr BioHarness. */
    public static final String PREF_BIOHARNESS = "zephyrBioHarness";
    /** Key for preference that toggles use of the Zephyr BioHarness Accelerometer. */
    public static final String PREF_BIOHARNESS_ACC = "zephyrBioHarness_acc";
    /** Key for preference that toggles use of the Zephyr BioHarness Heart rate. */
    public static final String PREF_BIOHARNESS_HEART_RATE = "zephyrBioHarness_heartRate";
    /** Key for preference that toggles use of the Zephyr BioHarness Temperature. */
    public static final String PREF_BIOHARNESS_TEMP = "zephyrBioHarness_temp";
    /** Key for preference that toggles use of the Zephyr BioHarness Respiration rate. */
    public static final String PREF_BIOHARNESS_RESP = "zephyrBioHarness_resp";
    /** Key for preference that toggles use of the Zephyr BioHarness Blood pressure. */
    public static final String PREF_BIOHARNESS_BLOOD_PRESSURE = "zephyrBioHarness_bloodP";
    /** Key for preference that toggles use of the Zephyr BioHarness worn status. */
    public static final String PREF_BIOHARNESS_WORN_STATUS = "zephyrBioHarness_wornStatus";
    /** Key for preference that toggles use of the Zephyr BioHarness battery level. */
    public static final String PREF_BIOHARNESS_BATTERY = "zephyrBioHarness_battery";
    /** Key for preference that toggles use of the Zephyr HxM. */
    public static final String PREF_HXM = "zephyrHxM";
    /** Key for preference that toggles use of the Zephyr HxM speed. */
    public static final String PREF_HXM_SPEED = "zephyrHxM_speed";
    /** Key for preference that toggles use of the Zephyr HxM heart rate. */
    public static final String PREF_HXM_HEART_RATE = "zephyrHxM_heartRate";
    /** Key for preference that toggles use of the Zephyr HxM battery. */
    public static final String PREF_HXM_BATTERY = "zephyrHxM_battery";
    /** Key for preference that toggles use of the Zephyr HxM distance. */
    public static final String PREF_HXM_DISTANCE = "zephyrHxM_distance";

    /* Codes to keep track of the active sensing modules */
    public static final int STATUSCODE_AMBIENCE = 0x01;
    public static final int STATUSCODE_CONNECTED = 0x02;
    public static final int STATUSCODE_DEVICE_PROX = 0x04;
    public static final int STATUSCODE_EXTERNAL = 0x08;
    public static final int STATUSCODE_LOCATION = 0x10;
    public static final int STATUSCODE_MOTION = 0x20;
    public static final int STATUSCODE_PHONESTATE = 0x40;
    public static final int STATUSCODE_QUIZ = 0x80;
    public static final int STATUSCODE_RUNNING = 0x100;

    /* CommonSense URLs */
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

    /* Sensor data type designations */
    public static final String SENSOR_DATA_TYPE_BOOL = "bool";
    public static final String SENSOR_DATA_TYPE_FLOAT = "float";
    public static final String SENSOR_DATA_TYPE_INT = "int";
    public static final String SENSOR_DATA_TYPE_JSON = "json";
    public static final String SENSOR_DATA_TYPE_STRING = "string";
    public static final String SENSOR_DATA_TYPE_FILE = "file";
}
