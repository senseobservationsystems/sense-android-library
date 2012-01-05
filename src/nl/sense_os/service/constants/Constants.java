package nl.sense_os.service.constants;

import nl.sense_os.service.constants.SensePrefs.Auth;
import nl.sense_os.service.constants.SensePrefs.Main;
import nl.sense_os.service.constants.SensePrefs.Main.Advanced;
import nl.sense_os.service.constants.SensePrefs.Main.Ambience;
import nl.sense_os.service.constants.SensePrefs.Main.DevProx;
import nl.sense_os.service.constants.SensePrefs.Main.External.MyGlucoHealth;
import nl.sense_os.service.constants.SensePrefs.Main.External.TanitaScale;
import nl.sense_os.service.constants.SensePrefs.Main.External.ZephyrBioHarness;
import nl.sense_os.service.constants.SensePrefs.Main.External.ZephyrHxM;
import nl.sense_os.service.constants.SensePrefs.Main.Location;
import nl.sense_os.service.constants.SensePrefs.Main.Motion;
import nl.sense_os.service.constants.SensePrefs.Main.Quiz;
import nl.sense_os.service.constants.SensePrefs.Status;
import android.content.Context;

/**
 * Class with constants for the Sense Platform service.
 * 
 * @deprecated Use new SensePrefs, SenseStatusCodes, SenseDataTypes SenseUrls classes instead
 */
@Deprecated
public class Constants {

    /* ======================================================================================== */
    /* -------------------------Sense service status preferences ------------------------------ */
    /* ======================================================================================== */
    /**
     * Name of shared preferences file holding the desired status of the Sense service.
     * 
     * @see #AUTH_PREFS
     * @see #MAIN_PREFS
     * @deprecated Use {@link SensePrefs#STATUS_PREFS} instead
     */
    public static final String STATUS_PREFS = SensePrefs.STATUS_PREFS;

    /**
     * Key for the main status of the sensors. Set to <code>false</code> to disable all the sensing
     * components.
     * 
     * @see SensePrefs#STATUS_PREFS
     * @deprecated Use {@link Status#MAIN} instead
     */
    public static final String PREF_STATUS_MAIN = Status.MAIN;

    /**
     * Key for the status of the "ambience" sensors. Set to <code>true</code> to enable sensing.
     * 
     * @see SensePrefs#STATUS_PREFS
     * @deprecated Use {@link Status#AMBIENCE} instead
     */
    public static final String PREF_STATUS_AMBIENCE = Status.AMBIENCE;

    /**
     * Key for the status of the "device proximity" sensors. Set to <code>true</code> to enable
     * sensing.
     * 
     * @see SensePrefs#STATUS_PREFS
     * @deprecated Use {@link Status#DEV_PROX} instead
     */
    public static final String PREF_STATUS_DEV_PROX = Status.DEV_PROX;

    /**
     * Key for the status of the external Bluetooth sensors. Set to <code>true</code> to enable
     * sensing.
     * 
     * @see SensePrefs#STATUS_PREFS
     * @deprecated Use {@link Status#EXTERNAL} instead
     */
    public static final String PREF_STATUS_EXTERNAL = Status.EXTERNAL;

    /**
     * Key for the status of the location sensors. Set to <code>true</code> to enable sensing.
     * 
     * @see SensePrefs#STATUS_PREFS
     * @deprecated Use {@link Status#LOCATION} instead
     */
    public static final String PREF_STATUS_LOCATION = Status.LOCATION;

    /**
     * Key for the status of the motion sensors. Set to <code>true</code> to enable sensing.
     * 
     * @see SensePrefs#STATUS_PREFS
     * @deprecated Use {@link Status#MOTION} instead
     */
    public static final String PREF_STATUS_MOTION = Status.MOTION;

    /**
     * Key for the status of the "phone state" sensors. Set to <code>true</code> to enable sensing.
     * 
     * @see SensePrefs#STATUS_PREFS
     * @deprecated Use {@link Status#PHONESTATE} instead
     */
    public static final String PREF_STATUS_PHONESTATE = Status.PHONESTATE;

    /**
     * Key for the status of the questionnaire. Set to <code>true</code> to enable it.
     * 
     * @see SensePrefs#STATUS_PREFS
     * @deprecated Use {@link Status#POPQUIZ} instead
     */
    public static final String PREF_STATUS_POPQUIZ = Status.POPQUIZ;

    /**
     * Key for preference to automatically start the Sense service on boot.
     * 
     * @see SensePrefs#STATUS_PREFS
     * @deprecated Use {@link Status#AUTOSTART} instead
     */
    public static final String PREF_AUTOSTART = Status.AUTOSTART;

    /**
     * Key for storing if the service is supposed to be "alive", used for aggressive restarting
     * after crashes.
     * 
     * @deprecated Use {@link Status#MAIN} instead.
     * @see SensePrefs#STATUS_PREFS
     */
    public static final String PREF_ALIVE = "alive";

    /* ======================================================================================== */
    /* --------------------- CommonSense authentication preferences --------------------------- */
    /* ======================================================================================== */
    /**
     * Name of the shared preferences file used for storing CommonSense authentication data. Use
     * {@link Context#MODE_PRIVATE}.
     * 
     * @see #MAIN_PREFS
     * @see #STATUS_PREFS
     * @deprecated Use {@link SensePrefs#AUTH_PREFS} instead
     */
    public static final String AUTH_PREFS = SensePrefs.AUTH_PREFS;// "login";

    /**
     * Key for login preference for session cookie.
     * 
     * @see #AUTHENTICATION_PREFS
     * @deprecated Use {@link Auth#LOGIN_COOKIE} instead
     */
    public static final String PREF_LOGIN_COOKIE = Auth.LOGIN_COOKIE;

    /**
     * Key for login preference for email address.
     * 
     * @see #AUTHENTICATION_PREFS
     * @deprecated Use {@link Auth#LOGIN_USERNAME} instead
     */
    public static final String PREF_LOGIN_USERNAME = Auth.LOGIN_USERNAME;

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
     * @deprecated Use {@link Auth#SENSOR_LIST} instead
     */
    public static final String PREF_SENSOR_LIST = Auth.SENSOR_LIST;

    /**
     * Key for storing the retrieval time of online sensor list.
     * 
     * @see #AUTHENTICATION_PREFS
     * @deprecated Use {@link Auth#SENSOR_LIST_TIME} instead
     */
    public static final String PREF_SENSOR_LIST_TIME = Auth.SENSOR_LIST_TIME;

    /**
     * Key to use the development version of CommonSense.
     * 
     * @see #AUTHENTICATION_PREFS
     * @deprecated Use {@link Advanced#DEV_MODE} instead
     */
    public static final String PREF_DEV_MODE = Advanced.DEV_MODE;

    /**
     * Key for storing the online device id.
     * 
     * @see #AUTHENTICATION_PREFS
     * @deprecated Use {@link Auth#DEVICE_ID} instead
     */
    public static final String PREF_DEVICE_ID = Auth.DEVICE_ID;

    /**
     * Key for storing the retrieval time of the online device id.
     * 
     * @see #AUTHENTICATION_PREFS
     * @deprecated Use {@link Auth#DEVICE_ID_TIME} instead
     */
    public static final String PREF_DEVICE_ID_TIME = Auth.DEVICE_ID_TIME;

    /**
     * Key for storing the online device type.
     * 
     * @see #AUTHENTICATION_PREFS
     * @deprecated Use {@link Auth#DEVICE_TYPE} instead
     */
    public static final String PREF_DEVICE_TYPE = Auth.DEVICE_TYPE;

    /**
     * Key for storing the IMEI of the phone.
     * 
     * @see #AUTHENTICATION_PREFS
     * @deprecated Use {@link Auth#PHONE_IMEI} instead
     */
    public static final String PREF_PHONE_IMEI = Auth.PHONE_IMEI;

    /**
     * Key for storing the type of the phone.
     * 
     * @see #AUTHENTICATION_PREFS
     * @deprecated Use {@link Auth#PHONE_TYPE} instead
     */
    public static final String PREF_PHONE_TYPE = Auth.PHONE_TYPE;

    /**
     * Key for login preference for hashed password.
     * 
     * @see #AUTHENTICATION_PREFS
     * @deprecated Use {@link Auth#LOGIN_PASS} instead
     */
    public static final String PREF_LOGIN_PASS = Auth.LOGIN_PASS;

    /* ======================================================================================== */
    /* -------------------------- Main Sense service preferences ------------------------------ */
    /* ======================================================================================== */
    /**
     * Name of the main preference file, used for storing the settings for the Sense service.
     * 
     * @see #AUTH_PREFS
     * @see #STATUS_PREFS
     * @deprecated Use {@link SensePrefs#MAIN_PREFS} instead
     */
    public static final String MAIN_PREFS = SensePrefs.MAIN_PREFS;

    /**
     * Key for preference that toggles use of light sensor in ambience sensing.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link Ambience#LIGHT} instead
     */
    public static final String PREF_AMBIENCE_LIGHT = Ambience.LIGHT;

    /**
     * Key for preference that toggles use of the microphone in ambience sensing.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link Ambience#MIC} instead
     */
    public static final String PREF_AMBIENCE_MIC = Ambience.MIC;

    /**
     * Key for preference that controls sample frequency of the sensors.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link Main#SAMPLE_RATE} instead
     */
    public static final String PREF_SAMPLE_RATE = Main.SAMPLE_RATE;

    /**
     * Key for preference that saves the last running services.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link Main#LAST_STATUS} instead
     */
    public static final String PREF_LAST_STATUS = Main.LAST_STATUS;

    /**
     * Key for preference that toggles use of GPS in location sensor.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link Location#GPS} instead
     */
    public static final String PREF_LOCATION_GPS = Location.GPS;

    /**
     * Key for preference that toggles use of Network in location sensor.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link Location#NETWORK} instead
     */
    public static final String PREF_LOCATION_NETWORK = Location.NETWORK;

    /**
     * Key for preference that toggles use of sensor fusion to toggle th GPS usage.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link Location#AUTO_GPS} instead
     */
    public static final String PREF_LOCATION_AUTO_GPS = Location.AUTO_GPS;

    /**
     * Key for preference that toggles use of GPS in location sensor.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link DevProx#BLUETOOTH} instead
     */
    public static final String PREF_PROXIMITY_BT = DevProx.BLUETOOTH;

    /**
     * Key for preference that toggles use of Bluetooth in the DeviceProximity sensor.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link DevProx#WIFI} instead
     */
    public static final String PREF_PROXIMITY_WIFI = DevProx.WIFI;

    /**
     * Key for preference that toggles use of Bluetooth in the DeviceProximity sensor.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link Motion#FALL_DETECT} instead
     */
    public static final String PREF_MOTION_FALL_DETECT = Motion.FALL_DETECT;

    /**
     * Key for preference that toggles use of Bluetooth in the DeviceProximity sensor.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link Motion#FALL_DETECT_DEMO} instead
     */
    public static final String PREF_MOTION_FALL_DETECT_DEMO = Motion.FALL_DETECT_DEMO;

    /**
     * Key for preference that sets the interval between pop quizzes.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link Quiz#RATE} instead
     */
    public static final String PREF_QUIZ_RATE = Quiz.RATE;

    /**
     * Key for preference that sets the silent mode for pop quizzes.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link Quiz#SILENT_MODE} instead
     */
    public static final String PREF_QUIZ_SILENT_MODE = Quiz.SILENT_MODE;

    /**
     * Key for generic preference that starts an update of the quiz questions when clicked.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link Quiz#SYNC} instead
     */
    public static final String PREF_QUIZ_SYNC = Quiz.SYNC;

    /**
     * Key for preference that holds the last update time of the quiz questions with CommonSense.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link Quiz#SYNC_TIME} instead
     */
    public static final String PREF_QUIZ_SYNC_TIME = Quiz.SYNC_TIME;

    /**
     * Key for preference that controls sync frequency with CommonSense.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link Main#SYNC_RATE} instead
     */
    public static final String PREF_SYNC_RATE = Main.SYNC_RATE;

    /**
     * Key for preference that toggles use of the Zephyr BioHarness.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link ZephyrBioHarness#MAIN} instead
     */
    public static final String PREF_BIOHARNESS = ZephyrBioHarness.MAIN;

    /**
     * Key for preference that toggles use of the Zephyr BioHarness Accelerometer.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link ZephyrBioHarness#ACC} instead
     */
    public static final String PREF_BIOHARNESS_ACC = ZephyrBioHarness.ACC;
    /**
     * Key for preference that toggles use of the Zephyr BioHarness Heart rate.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link ZephyrBioHarness#HEART_RATE} instead
     */
    public static final String PREF_BIOHARNESS_HEART_RATE = ZephyrBioHarness.HEART_RATE;

    /**
     * Key for preference that toggles use of the Zephyr BioHarness Temperature.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link ZephyrBioHarness#TEMP} instead
     */
    public static final String PREF_BIOHARNESS_TEMP = ZephyrBioHarness.TEMP;

    /**
     * Key for preference that toggles use of the Zephyr BioHarness Respiration rate.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link ZephyrBioHarness#RESP} instead
     */
    public static final String PREF_BIOHARNESS_RESP = ZephyrBioHarness.RESP;

    /**
     * Key for preference that toggles use of the Zephyr BioHarness Blood pressure.
     * 
     * @deprecated BioHarness does not actually measure blood pressure.
     * @see SensePrefs#MAIN_PREFS
     */
    @Deprecated
    public static final String PREF_BIOHARNESS_BLOOD_PRESSURE = "zephyrBioHarness_bloodP";

    /**
     * Key for preference that toggles use of the Zephyr BioHarness worn status.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link ZephyrBioHarness#WORN_STATUS} instead
     */
    public static final String PREF_BIOHARNESS_WORN_STATUS = ZephyrBioHarness.WORN_STATUS;

    /**
     * Key for preference that toggles use of the Zephyr BioHarness battery level.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link ZephyrBioHarness#BATTERY} instead
     */
    public static final String PREF_BIOHARNESS_BATTERY = ZephyrBioHarness.BATTERY;

    /**
     * Key for preference that toggles use of the Zephyr HxM.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link ZephyrHxM#MAIN} instead
     */
    public static final String PREF_HXM = ZephyrHxM.MAIN;

    /**
     * Key for preference that toggles use of the Zephyr HxM speed.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link ZephyrHxM#SPEED} instead
     */
    public static final String PREF_HXM_SPEED = ZephyrHxM.SPEED;

    /**
     * Key for preference that toggles use of the Zephyr HxM heart rate.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link ZephyrHxM#HEART_RATE} instead
     */
    public static final String PREF_HXM_HEART_RATE = ZephyrHxM.HEART_RATE;

    /**
     * Key for preference that toggles use of the Zephyr HxM battery.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link ZephyrHxM#BATTERY} instead
     */
    public static final String PREF_HXM_BATTERY = ZephyrHxM.BATTERY;

    /**
     * Key for preference that toggles use of the Zephyr HxM distance.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link ZephyrHxM#DISTANCE} instead
     */
    public static final String PREF_HXM_DISTANCE = ZephyrHxM.DISTANCE;

    /**
     * Key for preference that toggles use of the Zephyr HxM strides.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link ZephyrHxM#STRIDES} instead
     */
    public static final String PREF_HXM_STRIDES = ZephyrHxM.STRIDES;

    /**
     * Key for preference that toggles use of the MyGlucohealth sensor.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link MyGlucoHealth#MAIN} instead
     */
    public static final String PREF_GLUCO = MyGlucoHealth.MAIN;

    /**
     * Key for preference that toggles use of the Tanita scale sensor.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link TanitaScale#MAIN} instead
     */
    public static final String PREF_TANITA_SCALE = TanitaScale.MAIN;

    /**
     * Key for preference that toggles use of compression for transmission.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link Advanced#COMPRESS} instead
     */
    public static final String PREF_COMPRESSION = Advanced.COMPRESS;

    /**
     * Key for preference that toggles "epi-mode", drastically changing motion sensing
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link Motion#EPIMODE} instead
     */
    public static final String PREF_MOTION_EPIMODE = Motion.EPIMODE;

    /**
     * Key for preference that determines whether to unregister the motion sensor between samples.
     * Nota bene: unregistering the sensor breaks the screen rotation on some phones (e.g. Nexus S).
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link Motion#UNREG} instead
     */
    public static final String PREF_MOTION_UNREG = Motion.UNREG;

    /**
     * Key for preference that toggles motion energy sensing, which measures average kinetic energy
     * over a sample period.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link Motion#MOTION_ENERGY} instead
     */
    public static final String PREF_MOTION_ENERGY = Motion.MOTION_ENERGY;

    /**
     * Key for preference that enables fix that re-registers the motion sensor when the screen turns
     * off.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link Motion#SCREENOFF_FIX} instead
     */
    public static final String PREF_SCREENOFF_FIX = Motion.SCREENOFF_FIX;

    /**
     * Key for preference that enables partial wake lock, ensuring the CPU never turns off while
     * sensing.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Not used anymore, Sense automatically uses wake lock when required.
     */
    @Deprecated
    public static final String PREF_WAKELOCK = "partial_wakelock";

    /**
     * Key for preference that enables local storage, making the sensor data available to other apps
     * through a ContentProvider.
     * 
     * @see SensePrefs#MAIN_PREFS
     * @deprecated Use {@link Advanced#LOCAL_STORAGE} instead
     */
    public static final String PREF_LOCAL_STORAGE = Advanced.LOCAL_STORAGE;

    /* ======================================================================================== */
    /* ---------------- Codes to keep track of the active sensing modules --------------------- */
    /* ======================================================================================== */
    /**
     * @deprecated Use {@link SenseStatusCodes#AMBIENCE} instead
     */
    public static final int STATUSCODE_AMBIENCE = SenseStatusCodes.AMBIENCE;
    /**
     * @deprecated Use {@link SenseStatusCodes#CONNECTED} instead
     */
    public static final int STATUSCODE_CONNECTED = SenseStatusCodes.CONNECTED;
    /**
     * @deprecated Use {@link SenseStatusCodes#DEVICE_PROX} instead
     */
    public static final int STATUSCODE_DEVICE_PROX = SenseStatusCodes.DEVICE_PROX;
    /**
     * @deprecated Use {@link SenseStatusCodes#EXTERNAL} instead
     */
    public static final int STATUSCODE_EXTERNAL = SenseStatusCodes.EXTERNAL;
    /**
     * @deprecated Use {@link SenseStatusCodes#LOCATION} instead
     */
    public static final int STATUSCODE_LOCATION = SenseStatusCodes.LOCATION;
    /**
     * @deprecated Use {@link SenseStatusCodes#MOTION} instead
     */
    public static final int STATUSCODE_MOTION = SenseStatusCodes.MOTION;
    /**
     * @deprecated Use {@link SenseStatusCodes#PHONESTATE} instead
     */
    public static final int STATUSCODE_PHONESTATE = SenseStatusCodes.PHONESTATE;
    /**
     * @deprecated Use {@link SenseStatusCodes#QUIZ} instead
     */
    public static final int STATUSCODE_QUIZ = SenseStatusCodes.QUIZ;
    /**
     * @deprecated Use {@link SenseStatusCodes#RUNNING} instead
     */
    public static final int STATUSCODE_RUNNING = SenseStatusCodes.RUNNING;

    /* ======================================================================================== */
    /* ----------------------------------- CommonSense URls ----------------------------------- */
    /* ======================================================================================== */
    /**
     * @deprecated Use {@link SenseUrls#BASE} instead
     */
    public static final String URL_BASE = SenseUrls.BASE;
    /**
     * @deprecated Use {@link SenseUrls#DEV_BASE} instead
     */
    public static final String URL_DEV_BASE = SenseUrls.DEV_BASE;
    /**
     * @deprecated Use {@link SenseUrls#VERSION} instead
     */
    public static final String URL_VERSION = SenseUrls.VERSION;
    /**
     * @deprecated Use {@link SenseUrls#FORMAT} instead
     */
    public static final String URL_FORMAT = SenseUrls.FORMAT;
    /**
     * @deprecated Use {@link SenseUrls#DEVICES} instead
     */
    public static final String URL_DEVICES = SenseUrls.DEVICES;
    /**
     * @deprecated Use {@link SenseUrls#DEV_DEVICES} instead
     */
    public static final String URL_DEV_DEVICES = SenseUrls.DEV_DEVICES;
    /**
     * @deprecated Use {@link SenseUrls#SENSORS} instead
     */
    public static final String URL_SENSORS = SenseUrls.SENSORS;
    /**
     * @deprecated Use {@link SenseUrls#DEV_SENSORS} instead
     */
    public static final String URL_DEV_SENSORS = SenseUrls.DEV_SENSORS;
    /**
     * @deprecated Use {@link SenseUrls#SENSOR_DATA} instead
     */
    public static final String URL_SENSOR_DATA = SenseUrls.SENSOR_DATA;
    /**
     * @deprecated Use {@link SenseUrls#DEV_SENSOR_DATA} instead
     */
    public static final String URL_DEV_SENSOR_DATA = SenseUrls.DEV_SENSOR_DATA;
    /**
     * @deprecated Use {@link SenseUrls#SENSOR_FILE} instead
     */
    public static final String URL_SENSOR_FILE = SenseUrls.SENSOR_FILE;
    /**
     * @deprecated Use {@link SenseUrls#DEV_SENSOR_FILE} instead
     */
    public static final String URL_DEV_SENSOR_FILE = SenseUrls.DEV_SENSOR_FILE;
    /**
     * @deprecated Use {@link SenseUrls#CREATE_SENSOR} instead
     */
    public static final String URL_CREATE_SENSOR = SenseUrls.CREATE_SENSOR;
    /**
     * @deprecated Use {@link SenseUrls#DEV_CREATE_SENSOR} instead
     */
    public static final String URL_DEV_CREATE_SENSOR = SenseUrls.DEV_CREATE_SENSOR;
    /**
     * @deprecated Use {@link SenseUrls#ADD_SENSOR_TO_DEVICE} instead
     */
    public static final String URL_ADD_SENSOR_TO_DEVICE = SenseUrls.ADD_SENSOR_TO_DEVICE;
    /**
     * @deprecated Use {@link SenseUrls#DEV_ADD_SENSOR_TO_DEVICE} instead
     */
    public static final String URL_DEV_ADD_SENSOR_TO_DEVICE = SenseUrls.DEV_ADD_SENSOR_TO_DEVICE;
    /**
     * @deprecated Use {@link SenseUrls#LOGIN} instead
     */
    public static final String URL_LOGIN = SenseUrls.LOGIN;
    /**
     * @deprecated Use {@link SenseUrls#DEV_LOGIN} instead
     */
    public static final String URL_DEV_LOGIN = SenseUrls.DEV_LOGIN;

    /**
     * @deprecated Use {@link SenseUrls#REG} instead
     */
    public static final String URL_REG = SenseUrls.REG;
    /**
     * @deprecated Use {@link SenseUrls#DEV_REG} instead
     */
    public static final String URL_DEV_REG = SenseUrls.DEV_REG;

    /* ======================================================================================== */
    /* -------------------------- Sensor data type designations ------------------------------- */
    /* ======================================================================================== */
    /**
     * @deprecated Use {@link SenseDataTypes#BOOL} instead
     */
    public static final String SENSOR_DATA_TYPE_BOOL = SenseDataTypes.BOOL;
    /**
     * @deprecated Use {@link SenseDataTypes#FLOAT} instead
     */
    public static final String SENSOR_DATA_TYPE_FLOAT = SenseDataTypes.FLOAT;
    /**
     * @deprecated Use {@link SenseDataTypes#INT} instead
     */
    public static final String SENSOR_DATA_TYPE_INT = SenseDataTypes.INT;
    /**
     * @deprecated Use {@link SenseDataTypes#JSON} instead
     */
    public static final String SENSOR_DATA_TYPE_JSON = SenseDataTypes.JSON;
    /**
     * @deprecated Use {@link SenseDataTypes#STRING} instead
     */
    public static final String SENSOR_DATA_TYPE_STRING = SenseDataTypes.STRING;
    /**
     * @deprecated Use {@link SenseDataTypes#FILE} instead
     */
    public static final String SENSOR_DATA_TYPE_FILE = SenseDataTypes.FILE;
    /**
     * @deprecated Use {@link SenseDataTypes#JSON_TIME_SERIE} instead
     */
    public static final String SENSOR_DATA_TYPE_JSON_TIME_SERIE = SenseDataTypes.JSON_TIME_SERIE;
}
