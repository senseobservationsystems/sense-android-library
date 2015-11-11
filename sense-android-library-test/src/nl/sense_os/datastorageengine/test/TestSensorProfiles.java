package nl.sense_os.datastorageengine.test;

import android.test.AndroidTestCase;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.skyscreamer.jsonassert.JSONAssert;

import nl.sense_os.datastorageengine.DataSyncer;
import nl.sense_os.datastorageengine.DatabaseHandler;
import nl.sense_os.datastorageengine.DatabaseHandlerException;
import nl.sense_os.datastorageengine.SensorProfileException;
import nl.sense_os.datastorageengine.SensorDataProxy;
import nl.sense_os.datastorageengine.SensorProfiles;

public class TestSensorProfiles  extends AndroidTestCase {
    private static final String TAG = "TestSensorDataProxy";

    Map<String, String> mUser;
    String mUserId;
    SensorDataProxy.SERVER mServer = SensorDataProxy.SERVER.STAGING;
    String mAppKey = "E9Noi5s402FYo2Gc6a7pDTe4H3UvLkWa";  // application key for dev, android, Brightr ASML
    String mSessionId;
    SensorDataProxy mProxy;
    DatabaseHandler mDatabaseHandler;
    private byte[] mEncryptionKey = null; // TODO: test with encryption key
    DataSyncer mDataSyncer;
    private SensorProfiles mSensorProfiles;
    CSUtils mCsUtils;

    public void setUp () throws IOException, JSONException, DatabaseHandlerException, SensorProfileException {
        mCsUtils = new CSUtils(false); // staging server

        mUser = mCsUtils.createCSAccount();
        mUserId = mUser.get("id");

        mSessionId = mCsUtils.loginUser(mUser.get("username"), mUser.get("password"));

        mProxy = new SensorDataProxy(mServer, mAppKey, mSessionId);
        mDatabaseHandler = new DatabaseHandler(getContext(), mUserId);
        mSensorProfiles = new SensorProfiles(getContext(), mEncryptionKey);
        mDataSyncer = new DataSyncer(getContext(), mDatabaseHandler, mProxy);
        mDataSyncer.initialize();
    }

    public void tearDown() throws Exception {
        mCsUtils.deleteAccount(mUser.get("username"), mUser.get("password"), mUser.get("id"));
    }

    // TODO: test SensorProfiles.create
    // TODO: test SensorProfiles.createOrUpdate

    public void testHasSensorAccelerometer() throws Exception {
        String sensorName = "accelerometer";
        assertTrue("Should have an accelermoeter profiles", mSensorProfiles.has(sensorName));
    }

    public void testSensorAccelerometer() throws Exception {
        String sensorName = "accelerometer";
        JSONObject expectedProfile = new JSONObject("{\"$schema\": \"http:\\/\\/json-schema.org\\/draft-04\\/schema#\",\"type\": \"object\",\"properties\": {\"x-axis\": {\"description\": \"The acceleration force applied on the x-axis in m\\/s2\", \"type\": \"number\"}, \"y-axis\": { \"description\": \"The acceleration force applied on the y-axis in m\\/s2\", \"type\": \"number\"}, \"z-axis\": {\"description\": \"The acceleration force applied on the z-axis in m\\/s2\", \"type\": \"number\" } }, \"required\": [\"x-axis\",\"y-axis\",\"z-axis\"]}");

        JSONObject profile = mSensorProfiles.get(sensorName);
        JSONAssert.assertEquals(expectedProfile, profile, true);
    }

    public void testSensorAppInfo() throws Exception {
        String sensorName = "app_info";
        JSONObject expectedProfile = new JSONObject("{\"$schema\": \"http:\\/\\/json-schema.org\\/draft-04\\/schema#\",\"type\": \"object\",\"properties\": {\"sense_library_version\": {\"description\": \"The version of the sense library\",\"type\": \"string\"},\"app_build\": {\"description\": \"Application build (version code, e.g. 1413357349)\",\"type\": \"string\"},\"app_name\": {\"description\":\"Application name (e.g. Goalie)\",\"type\": \"string\"},\"app_version\":{\"description\": \"Application version (version name, e.g. 3.0.0)\",\"type\":\"string\"},\"locale\": {\"description\": \"The device region settings (e.g. en_GB)\",\"type\": \"string\"},\"os\": {\"description\": \"OS (e.g Android or iOS)\",\"type\": \"string\"},\"os_version\": {\"description\": \"OS Version (e.g. 4.2.2)\",\"type\":\"string\"},\"device_model\":{\"description\": \"Device model (e.g. Galaxy Nexus)\",\"type\": \"string\"},\"required\": [\"sense_library_version\",\"app_name\",\"app_build\"]}}");

        JSONObject profile = mSensorProfiles.get(sensorName);
        JSONAssert.assertEquals(expectedProfile, profile, true);
    }

    public void testSensorBattery() throws Exception {
        String sensorName = "battery";
        JSONObject expectedProfile = new JSONObject("{\"$schema\": \"http:\\/\\/json-schema.org\\/draft-04\\/schema#\",\"type\":\"object\",\"properties\":{\"status\": {\"description\": \"The status of the battery, e.g. charging, discharging, full\",\"type\": \"string\"},\"level\": {\"description\": \"The battery level in percentage\",\"type\": \"number\"}},\"required\": [\"level\"]}");

        JSONObject profile = mSensorProfiles.get(sensorName);
        JSONAssert.assertEquals(expectedProfile, profile, true);
    }

    public void testSensorCall() throws Exception {
        String sensorName = "call";
        JSONObject expectedProfile = new JSONObject("{\"$schema\": \"http:\\/\\/json-schema.org\\/draft-04\\/schema#\",\"type\": \"object\",\"properties\": {\"state\": {\"description\": \"The state of the phone\",\"enum\": [\"idle\", \"dialing\",\"ringing\", \"calling\"]},\"incomingNumber\": {\"description\": \"The phone number of the in-comming call\",\"type\": \"string\"},\"outgoingNumber\": {\"description\": \"The phone number of the out-going call\", \"type\": \"string\"}},\"required\": [\"state\"]}");

        JSONObject profile = mSensorProfiles.get(sensorName);
        JSONAssert.assertEquals(expectedProfile, profile, true);
    }

    public void testSensorCortexLog() throws Exception {
        String sensorName = "cortex_log";
        JSONObject expectedProfile = new JSONObject("{\"$schema\": \"http:\\/\\/json-schema.org\\/draft-04\\/schema#\",\"type\":\"object\",\"properties\":{\"tag\": {\"description\": \"Log tag\",\"type\":\"string\"},\"type\": {\"description\": \"The type of log information e.g. WARNING\",\"enum\":[\"VERBOSE\", \"DEBUG\", \"INFO\", \"WARNING\",\"ERROR\"]},\"text\": {\"description\": \"The log message\",\"type\": \"string\"}},\"required\": [\"tag\",\"type\",\"text\"]}");

        JSONObject profile = mSensorProfiles.get(sensorName);
        JSONAssert.assertEquals(expectedProfile, profile, true);
    }

    public void testSensorLight() throws Exception {
        String sensorName = "light";
        JSONObject expectedProfile = new JSONObject("{\"$schema\": \"http:\\/\\/json-schema.org\\/draft-04\\/schema#\",\"description\": \"The illuminance in lux\",\"type\": \"number\"}");

        JSONObject profile = mSensorProfiles.get(sensorName);
        JSONAssert.assertEquals(expectedProfile, profile, true);
    }

    public void testSensorMentalResilience() throws Exception {
        String sensorName = "mental_resilience";
        JSONObject expectedProfile = new JSONObject("{\"$schema\": \"http:\\/\\/json-schema.org\\/draft-04\\/schema#\",\"type\": \"object\",\"properties\": {\"question\": {\"description\": \"The question text\",\"type\": \"string\"}, \"score\": {\"description\": \"The mental resilience value (e.g. 30)\",\"type\": \"integer\"},\"question_id\": {\"description\": \"The identifier of the question (e.g. stylemsg_id)\", \"type\": \"integer\"}, \"answer_id\": {\"description\": \"The identifier of the answer (e.g. 2, for a 10 point scale 0-9)\", \"type\": \"integer\"}, \"answer\": {\"description\": \"The string representation of the answer (e.g. 30%)\", \"type\": \"string\"}, \"task_type\": {\"description\": \"The Task.TYPE representing the domain to which this question belongs to (e.g. MR_TASK_DOMAIN_1)\", \"type\": \"string\"}},\"required\": [\"question\",\"score\",\"question_id\",\"answer_id\", \"answer\",\"task_type\"]}");

        JSONObject profile = mSensorProfiles.get(sensorName);
        JSONAssert.assertEquals(expectedProfile, profile, true);
    }

    public void testSensorNoise() throws Exception {
        String sensorName = "noise";
        JSONObject expectedProfile = new JSONObject("{\"$schema\": \"http:\\/\\/json-schema.org\\/draft-04\\/schema#\",\"description\": \"The Ambient noise in decibel\",\"type\": \"number\"}");

        JSONObject profile = mSensorProfiles.get(sensorName);
        JSONAssert.assertEquals(expectedProfile, profile, true);
    }

    public void testSensorPosition() throws Exception {
        String sensorName = "position";
        JSONObject expectedProfile = new JSONObject("{\"$schema\": \"http:\\/\\/json-schema.org\\/draft-04\\/schema#\",\"type\": \"object\",\"properties\": {\"longitude\": {\"description\": \"The longitude in degrees\",\"type\": \"number\"},\"latitude\": {\"description\": \"The latitude in degrees\",\"type\": \"number\"},\"altitude\": {\"description\": \"altitude in meters above the WGS 84 reference ellipsoid.\",\"type\": \"number\"},\"accuracy\": {\"description\": \"accuracy in meters\",\"type\": \"number\"},\"speed\": {\"description\": \"The speed in meters\\/second over ground.\",\"type\": \"number\"},\"bearing\": {\"description\": \"The average bearing in degrees\",\"type\": \"number\"},\"provider\": {\"description\": \"The location provider, e.g. GPS, NETWORK or FUSED\",\"type\": \"string\"}},\"required\": [\"longitude\",\"latitude\",\"accuracy\",\"provider\"]}");

        JSONObject profile = mSensorProfiles.get(sensorName);
        JSONAssert.assertEquals(expectedProfile, profile, true);
    }

    public void testSensorProximity() throws Exception {
        String sensorName = "proximity";
        JSONObject expectedProfile = new JSONObject("{\"$schema\": \"http:\\/\\/json-schema.org\\/draft-04\\/schema#\",\"description\": \"The proximity to an object in cm\",\"type\": \"number\"}");

        JSONObject profile = mSensorProfiles.get(sensorName);
        JSONAssert.assertEquals(expectedProfile, profile, true);
    }

    public void testSensorScreen() throws Exception {
        String sensorName = "screen";
        JSONObject expectedProfile = new JSONObject("{\"$schema\": \"http:\\/\\/json-schema.org\\/draft-04\\/schema#\",\"description\": \"The status of the screen, e.g. on or off\",\"enum\": [ \"on\", \"off\"]}");

        JSONObject profile = mSensorProfiles.get(sensorName);
        JSONAssert.assertEquals(expectedProfile, profile, true);
    }

    public void testSensorSleep() throws Exception {
        String sensorName = "sleep";
        JSONObject expectedProfile = new JSONObject("{\"$schema\": \"http:\\/\\/json-schema.org\\/draft-04\\/schema#\",\"type\": \"object\",\"properties\": {\"end_date\": {\"description\" : \"The end time of the sleep period in epoch seconds\",\"type\": \"number\"},\"metadata\": {\"type\": \"object\",\"properties\": {\"core version\": {\"description\" : \"The version of the cortex core\",\"type\": \"string\"},\"module version\": {\"description\" : \"The version of the sleep_time module\",\"type\":\"string\"},\"status\": {\"description\" : \"The current status of the sleep_time module for debugging module, e.g. awake: too much noise\",\"type\": \"string\"}}},\"hours\": {\"description\" : \"The number of actual sleep hours\",\"type\": \"number\"},\"start_date\": {\"description\" : \"The start time of the sleep period in epoch seconds\",\"type\": \"number\"}},\"required\": [\"end_time\",\"hours\",\"start_time\"]}");

        JSONObject profile = mSensorProfiles.get(sensorName);
        JSONAssert.assertEquals(expectedProfile, profile, true);
    }

    public void testSensorSleepEstimate() throws Exception {
        String sensorName = "sleep_estimate";
        JSONObject expectedProfile = new JSONObject("{\"$schema\": \"http:\\/\\/json-schema.org\\/draft-04\\/schema#\",\"type\": \"object\",\"properties\": {\"end_time\": { \"description\" : \"The end time of the sleep period in epoch seconds\",\"type\":\"number\"},\"history_based\": {\"description\" : \"Whether the sleep value is based on the history or on the actual sleep period\",\"type\": \"boolean\"},\"metadata\": {\"type\":\"object\",\"properties\": {\"core version\": {\"description\" : \"The version of the cortex core\",\"type\": \"string\"},\"module version\": {\"description\" : \"The version of the sleep_time module\",\"type\": \"string\" },\"status\": {\"description\" : \"The current status of the sleep_time module for debugging module, e.g. awake: too much noise\", \"type\": \"string\" } } }, \"hours\":{\"description\" : \"The number of actual sleep hours\",\"type\": \"number\"},\"start_time\": { \"description\" : \"The start time of the sleep period in epoch seconds\",\"type\": \"number\"}},\"required\": [\"end_time\",\"hours\",\"start_time\"]}");

        JSONObject profile = mSensorProfiles.get(sensorName);
        JSONAssert.assertEquals(expectedProfile, profile, true);
    }

    public void testSensorTimeActive() throws Exception {
        String sensorName = "time_active";
        JSONObject expectedProfile = new JSONObject("{\"$schema\": \"http:\\/\\/json-schema.org\\/draft-04\\/schema#\", \"description\": \"The time physically active in seconds\", \"type\": \"integer\"}");

        JSONObject profile = mSensorProfiles.get(sensorName);
        JSONAssert.assertEquals(expectedProfile, profile, true);
    }

    public void testSensorTimeZone() throws Exception {
        String sensorName = "time_zone";
        JSONObject expectedProfile = new JSONObject("{\"$schema\": \"http:\\/\\/json-schema.org\\/draft-04\\/schema#\",\"type\": \"object\",\"properties\": {\"id\": {\"description\": \"The time-zone id, e.g. Europe\\/Paris\",\"type\": \"string\"},\"offset\": {\"description\": \"The offset from GMT in seconds\",\"type\": \"integer\"}},\"required\": [\"offset\"]}");

        JSONObject profile = mSensorProfiles.get(sensorName);
        JSONAssert.assertEquals(expectedProfile, profile, true);
    }

    public void testSensorWifiScan() throws Exception {
        String sensorName = "wifi_scan";
        JSONObject expectedProfile = new JSONObject("{\"$schema\": \"http:\\/\\/json-schema.org\\/draft-04\\/schema#\",\"type\": \"object\",\"properties\": {\"ssid\": {\"description\": \"The name of the detected wifi device\",\"type\": \"string\"},\"bssid\": {\"description\": \"The mac address of the detected wifi device\",\"type\": \"string\"},\"frequency\": {\"description\":\"The signal frequency of the detected wifi device\",\"type\":\"integer\"},\"rssi\": {\"description\": \"The signal strength of the detected wifi device\",\"type\": \"integer\"},\"capabilities\": {\"description\": \"The capabilities of the detected wifi device, e.g. encryption, WPS\",\"type\": \"string\"}},\"required\": [\"bssid\"]}");

        JSONObject profile = mSensorProfiles.get(sensorName);
        JSONAssert.assertEquals(expectedProfile, profile, true);
    }

    public void testNumberOfProfiles() throws Exception {
        assertEquals("Should contain 16 profiles", 16, mSensorProfiles.size());
    }

    public void testGetSensorNames() throws Exception {
        Set<String> expected = new HashSet<String>(Arrays.asList(
                "accelerometer", "app_info", "battery", "call", "cortex_log", "light",
                "mental_resilience", "noise", "position", "proximity", "screen",
                "sleep", "sleep_estimate", "time_active", "time_zone", "wifi_scan"));

        assertEquals("Should returns all sensorNames", expected, mSensorProfiles.getSensorNames());
    }
}
