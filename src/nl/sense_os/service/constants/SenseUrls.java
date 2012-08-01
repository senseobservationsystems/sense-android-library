package nl.sense_os.service.constants;

public class SenseUrls {

	public static final String BASE = "https://api.sense-os.nl/";
	public static final String DEV_BASE = "http://api.dev.sense-os.nl/";
	public static final String VERSION = "http://data.sense-os.nl/senseapp/version.php";

	public static final String FORMAT = ".json";

	public static final String DEVICES = BASE + "devices" + FORMAT;
	public static final String DEV_DEVICES = DEV_BASE + "devices" + FORMAT;

	public static final String SENSORS = BASE + "devices/<id>/sensors" + FORMAT;
	public static final String DEV_SENSORS = DEV_BASE + "devices/<id>/sensors" + FORMAT;

	public static final String ALL_SENSORS = BASE + "sensors" + FORMAT
			+ "?per_page=1000&details=full";
	public static final String DEV_ALL_SENSORS = DEV_BASE + "sensors" + FORMAT
			+ "?per_page=1000&details=full";

	public static final String REGISTER_GCM_ID = BASE + "devices/<id>/push/register" + FORMAT;
	public static final String DEV_REGISTER_GCM_ID = DEV_BASE + "devices/<id>/push/register"
			+ FORMAT;

	public static final String DEVICE_UPDATE_CONFIGURATION = BASE + "devices/<id>/configuration"
			+ FORMAT;
	public static final String DEV_DEVICE_UPDATE_CONFIGURATION = DEV_BASE
			+ "devices/<id>/configuration" + FORMAT;

	public static final String CONFIGURATION = BASE + "configurations/<id>" + FORMAT;
	public static final String DEV_CONFIGURATION = DEV_BASE + "configurations/<id>" + FORMAT;

	public static final String SENSOR_DATA = BASE + "sensors/<id>/data" + FORMAT;
	public static final String DEV_SENSOR_DATA = DEV_BASE + "sensors/<id>/data" + FORMAT;

	public static final String SENSOR_FILE = BASE + "sensors/<id>/file" + FORMAT;
	public static final String DEV_SENSOR_FILE = DEV_BASE + "sensors/<id>/file" + FORMAT;

	public static final String CREATE_SENSOR = BASE + "sensors" + FORMAT;
	public static final String DEV_CREATE_SENSOR = DEV_BASE + "sensors" + FORMAT;

	public static final String ADD_SENSOR_TO_DEVICE = BASE + "sensors/<id>/device" + FORMAT;
	public static final String DEV_ADD_SENSOR_TO_DEVICE = DEV_BASE + "sensors/<id>/device" + FORMAT;

	public static final String LOGIN = BASE + "login" + FORMAT;
	public static final String DEV_LOGIN = DEV_BASE + "login" + FORMAT;

	public static final String CONNECTED_SENSORS = BASE + "sensors/<id>/sensors" + FORMAT
			+ "?per_page=1000&details=full";
	public static final String DEV_CONNECTED_SENSORS = DEV_BASE + "sensors/<id>/sensors" + FORMAT
			+ "?per_page=1000&details=full";

	public static final String MANUAL_LEARN = BASE + "sensors/%1/services/%2/manualLearn" + FORMAT;
	public static final String DEV_MANUAL_LEARN = DEV_BASE + "sensors/%1/services/%2/manualLearn"
			+ FORMAT;

	public static final String REG = BASE + "users" + FORMAT;
	public static final String DEV_REG = DEV_BASE + "users" + FORMAT;

	private SenseUrls() {
		// private constructor to prevent instantiation
	}
}