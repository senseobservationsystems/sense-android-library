package nl.sense_os.service.constants;

import nl.sense_os.service.commonsense.SenseApi;

/**
 * Contains URL resources for communication with the CommonSense API
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 * 
 * @see SenseApi
 */
public class SenseUrls {
    /** Host name of CommonSense Authentication API */
    public static final String AUTH_API = "https://auth-api.sense-os.nl/v1/";
    /** Host name of CommonSense Staging Authentication API */
    public static final String AUTH_STAGING_API = "http://auth-api.staging.sense-os.nl/v1/";
    /** Host name of CommonSense API */
    public static final String API = "https://api.sense-os.nl/";
    /** Host name of CommonSense dev API */
    public static final String API_DEV = "http://api.staging.sense-os.nl/";
    /** Default page size for getting lists at CommonSense */
    public static final int PAGE_SIZE = 1000;

    public static final String ALL_SENSORS = API + "sensors" + "?per_page=" + PAGE_SIZE
            + "&details=full";
    public static final String ALL_SENSORS_DEV = API_DEV + "sensors" + "?per_page=" + PAGE_SIZE
            + "&details=full";
    public static final String CONFIGURATION = API + "configurations/%1";
    public static final String CONFIGURATION_DEV = API_DEV + "configurations/%1";
    public static final String CONNECTED_SENSORS = API + "sensors/%1/sensors" + "?per_page="
            + PAGE_SIZE + "&details=full";
    public static final String CONNECTED_SENSORS_DEV = API_DEV + "sensors/%1/sensors"
            + "?per_page=" + PAGE_SIZE + "&details=full";
    public static final String CREATE_SENSOR_DEV = API_DEV + "sensors";
    public static final String CURRENT_USER = API + "users/current";
    public static final String CURRENT_USER_DEV = API_DEV + "users/current";
    public static final String DEVICE_CONFIGURATION = API + "devices/%1/configuration";
    public static final String DEVICE_CONFIGURATION_DEV = API_DEV + "devices/%1/configuration";
    public static final String DEVICE_SENSORS = API + "devices/%1/sensors";
    public static final String DEVICE_SENSORS_DEV = API_DEV + "devices/%1/sensors";
    public static final String DEVICES = API + "devices";
    public static final String DEVICES_DEV = API_DEV + "devices";
    public static final String FORGOT_PASSWORD = API + "requestPasswordReset";
    public static final String FORGOT_PASSWORD_DEV = API_DEV + "requestPasswordReset";
    public static final String GROUP_USERS = API + "groups/%1/users";
    public static final String GROUP_USERS_DEV = API_DEV + "groups/%1/users";
    public static final String LOGIN = AUTH_API + "login";
    public static final String LOGIN_DEV = AUTH_STAGING_API + "login";
    public static final String LOGOUT = AUTH_API + "logout";
    public static final String LOGOUT_DEV = AUTH_STAGING_API + "logout";
    public static final String MANUAL_LEARN = API + "sensors/%1/services/%2/manualLearn";
    public static final String MANUAL_LEARN_DEV = API_DEV + "sensors/%1/services/%2/manualLearn";
    public static final String REGISTER = API + "users";
    public static final String REGISTER_DEV = API_DEV + "users";
    public static final String REGISTER_GCM_ID = API + "devices/%1/push/register";
    public static final String REGISTER_GCM_ID_DEV = API_DEV + "devices/%1/push/register";
    public static final String SENSOR_DATA = API + "sensors/%1/data";
    public static final String SENSOR_DATA_DEV = API_DEV + "sensors/%1/data";
    public static final String SENSOR_DATA_MULTIPLE = API + "sensors/data";
    public static final String SENSOR_DATA_MULTIPLE_DEV = API_DEV + "sensors/data";
    public static final String SENSOR_DEVICE = API + "sensors/%1/device";
    public static final String SENSOR_DEVICE_DEV = API_DEV + "sensors/%1/device";
    public static final String SENSOR_FILE = API + "sensors/%1/file";
    public static final String SENSOR_FILE_DEV = API_DEV + "sensors/%1/file";
    public static final String SENSOR_USERS = API + "sensors/%1/users";
    public static final String SENSOR_USERS_DEV = API_DEV + "sensors/%1/users";
    public static final String SENSORS = API + "sensors";
    public static final String VERSION = "http://data.sense-os.nl/senseapp/version.php";
    public static final String DATAPROCESSOR_FILE = API + "dataprocessors/files";
    public static final String DATAPROCESSOR_FILE_DEV = API_DEV + "dataprocessors/files";
    public static final String CHANGE_PASSWORD = API + "change_password";
    public static final String CHANGE_PASSWORD_DEV = API_DEV + "change_password";
    public static final String RESET_PASSWORD_REQUEST = AUTH_API + "reset_password/request";
    public static final String RESET_PASSWORD_REQUEST_DEV = AUTH_STAGING_API + "reset_password/request";

    private SenseUrls() {
        // private constructor to prevent instantiation
    }
}
