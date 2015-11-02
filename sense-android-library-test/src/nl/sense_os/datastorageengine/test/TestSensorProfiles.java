package nl.sense_os.datastorageengine.test;

import android.test.AndroidTestCase;

import org.json.JSONException;

import java.io.IOException;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import nl.sense_os.datastorageengine.DSEConstants;
import nl.sense_os.datastorageengine.DataSyncer;
import nl.sense_os.datastorageengine.DatabaseHandler;
import nl.sense_os.datastorageengine.DatabaseHandlerException;
import nl.sense_os.datastorageengine.SensorDataProxy;
import nl.sense_os.datastorageengine.SensorProfile;

/**
 * Created by fei on 27/10/15.
 */
public class TestSensorProfiles  extends AndroidTestCase {

    private static final String TAG = "TestSensorDataProxy";

    Map<String, String> user;
    String userId;
    String sourceName = "sense-android";
    SensorDataProxy.SERVER server = SensorDataProxy.SERVER.STAGING;
    String appKey = "E9Noi5s402FYo2Gc6a7pDTe4H3UvLkWa";  // application key for dev, android, Brightr ASML
    String sessionId;
    SensorDataProxy proxy;
    DataSyncer dataSyncer;
    CSUtils csUtils;
    int SensorNumber = 16;
    Realm realm;
    RealmConfiguration testConfig;
    DatabaseHandler databaseHandler;
    RealmResults<SensorProfile> results;

    public void setUp () throws IOException, JSONException, DatabaseHandlerException {
        csUtils = new CSUtils(false); // staging server

        user = csUtils.createCSAccount();
        userId = user.get("id");

        sessionId = csUtils.loginUser(user.get("username"), user.get("password"));

        testConfig = new RealmConfiguration.Builder(getContext()).build();
        Realm.deleteRealm(testConfig);
        realm = Realm.getInstance(testConfig);
        databaseHandler = new DatabaseHandler(getContext(),userId);

        proxy = new SensorDataProxy(server, appKey, sessionId);

        dataSyncer = new DataSyncer(getContext(), databaseHandler, proxy);
        dataSyncer.downloadSensorProfiles();
        results = realm.where(SensorProfile.class).findAll();
        results.sort("sensorName", RealmResults.SORT_ORDER_ASCENDING);

    }

    public void tearDown() throws Exception {
        csUtils.deleteAccount(user.get("username"), user.get("password"), user.get("id"));
        databaseHandler.close();
        if (realm != null) {
            realm.close();
        }
    }

    //Helper func
    private void closeRealm() throws Exception {
        databaseHandler.close();
        if (realm != null) {
            realm.close();
        }
    }

    public void testNumberOfSensors() throws Exception {
        assertEquals(SensorNumber, results.size());
        closeRealm();
    }

    public void testSensorAccelerometer() throws Exception {
        SensorProfile sensorProfile = results.get(0);
        String sensorName = "accelerometer";
        String dataStructure = "{\"sensor_name\":\"accelerometer\",\"data_structure\":\"{\\\"$schema\\\": \\\"http:\\/\\/json-schema.org\\/draft-04\\/schema#\\\",\\\"type\\\": \\\"object\\\",\\\"properties\\\": {\\\"x-axis\\\": {\\\"description\\\": \\\"The acceleration force applied on the x-axis in m\\/s2\\\", \\\"type\\\": \\\"number\\\"}, \\\"y-axis\\\": { \\\"description\\\": \\\"The acceleration force applied on the y-axis in m\\/s2\\\", \\\"type\\\": \\\"number\\\"}, \\\"z-axis\\\": {\\\"description\\\": \\\"The acceleration force applied on the z-axis in m\\/s2\\\", \\\"type\\\": \\\"number\\\" } }, \\\"required\\\": [\\\"x-axis\\\",\\\"y-axis\\\",\\\"z-axis\\\"]}\"}";
        assertEquals(sensorName, sensorProfile.getSensorName());
        assertEquals(dataStructure, sensorProfile.getDataStructure());
        String value = "{\"x-axis\":6.6, \"y-axis\":6.7,\"z-axis\":6.8}";
        //databaseHandler.validate(sensorName, value);
        closeRealm();
    }

    public void testSensorAppInfo() throws Exception {
        SensorProfile sensorProfile = results.get(1);
        String sensorName = "app_info";
        String dataStructure = "{\"sensor_name\":\"app_info\",\"data_structure\":\"{\\\"$schema\\\": \\\"http:\\/\\/json-schema.org\\/draft-04\\/schema#\\\",\\\"type\\\": \\\"object\\\",\\\"properties\\\": {\\\"sense_library_version\\\": {\\\"description\\\": \\\"The version of the sense library\\\",\\\"type\\\": \\\"string\\\"},\\\"app_build\\\": {\\\"description\\\": \\\"Application build (version code, e.g. 1413357349)\\\",\\\"type\\\": \\\"string\\\"},\\\"app_name\\\": {\\\"description\\\":\\\"Application name (e.g. Goalie)\\\",\\\"type\\\": \\\"string\\\"},\\\"app_version\\\":{\\\"description\\\": \\\"Application version (version name, e.g. 3.0.0)\\\",\\\"type\\\":\\\"string\\\"},\\\"locale\\\": {\\\"description\\\": \\\"The device region settings (e.g. en_GB)\\\",\\\"type\\\": \\\"string\\\"},\\\"os\\\": {\\\"description\\\": \\\"OS (e.g Android or iOS)\\\",\\\"type\\\": \\\"string\\\"},\\\"os_version\\\": {\\\"description\\\": \\\"OS Version (e.g. 4.2.2)\\\",\\\"type\\\":\\\"string\\\"},\\\"device_model\\\":{\\\"description\\\": \\\"Device model (e.g. Galaxy Nexus)\\\",\\\"type\\\": \\\"string\\\"},\\\"required\\\": [\\\"sense_library_version\\\",\\\"app_name\\\",\\\"app_build\\\"]}\"}";
        assertEquals(sensorName, sensorProfile.getSensorName());
        assertEquals(dataStructure, sensorProfile.getDataStructure());
        String value = "{\"sense_library_version\":\"sense-android-1\", \"app_name\":\"brightr\",\"app_build\":\"4.5\"}";
        //databaseHandler.validate(sensorName, value);
        closeRealm();
    }

    public void testSensorBattery() throws Exception {
        SensorProfile sensorProfile = results.get(2);
        String sensorName = "battery";
        String dataStructure = "{\"sensor_name\":\"battery\",\"data_structure\":\"{\\\"$schema\\\": \\\"http:\\/\\/json-schema.org\\/draft-04\\/schema#\\\",\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"status\\\": {\\\"description\\\": \\\"The status of the battery, e.g. charging, discharging, full\\\",\\\"type\\\": \\\"string\\\"},\\\"level\\\": {\\\"description\\\": \\\"The battery level in percentage\\\",\\\"type\\\": \\\"number\\\"}},\\\"required\\\": [\\\"level\\\"]}\"}";
        assertEquals(sensorName, sensorProfile.getSensorName());
        assertEquals(dataStructure, sensorProfile.getDataStructure());
        String value = "{\"level\":80}";
        //databaseHandler.validate(sensorName, value);
        closeRealm();
    }

    public void testSensorCall() throws Exception {
        SensorProfile sensorProfile = results.get(3);
        String sensorName = "call";
        String dataStructure = "{\"sensor_name\":\"call\",\"data_structure\":\"{\\\"$schema\\\": \\\"http:\\/\\/json-schema.org\\/draft-04\\/schema#\\\",\\\"type\\\": \\\"object\\\",\\\"properties\\\": {\\\"state\\\": {\\\"description\\\": \\\"The state of the phone\\\",\\\"enum\\\": [\\\"idle\\\", \\\"dialing\\\",\\\"ringing\\\", \\\"calling\\\"]},\\\"incomingNumber\\\": {\\\"description\\\": \\\"The phone number of the in-comming call\\\",\\\"type\\\": \\\"string\\\"},\\\"outgoingNumber\\\": {\\\"description\\\": \\\"The phone number of the out-going call\\\", \\\"type\\\": \\\"string\\\"}},\\\"required\\\": [\\\"state\\\"]}\"}";
        assertEquals(sensorName, sensorProfile.getSensorName());
        assertEquals(dataStructure, sensorProfile.getDataStructure());
        String value = "{\"state\":\"idle\"}";
        //databaseHandler.validate(sensorName, value);
        closeRealm();
    }

    public void testSensorCortexLog() throws Exception {
        SensorProfile sensorProfile = results.get(4);
        String sensorName = "cortex_log";
        String dataStructure = "{\"sensor_name\":\"cortex_log\",\"data_structure\":\"{\\\"$schema\\\": \\\"http:\\/\\/json-schema.org\\/draft-04\\/schema#\\\",\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"tag\\\": {\\\"description\\\": \\\"Log tag\\\",\\\"type\\\":\\\"string\\\"},\\\"type\\\": {\\\"description\\\": \\\"The type of log information e.g. WARNING\\\",\\\"enum\\\":[\\\"VERBOSE\\\", \\\"DEBUG\\\", \\\"INFO\\\", \\\"WARNING\\\",\\\"ERROR\\\"]},\\\"text\\\": {\\\"description\\\": \\\"The log message\\\",\\\"type\\\": \\\"string\\\"}},\\\"required\\\": [\\\"tag\\\",\\\"type\\\",\\\"text\\\"]}\"}";
        assertEquals(sensorName, sensorProfile.getSensorName());
        assertEquals(dataStructure, sensorProfile.getDataStructure());
        String value = "{\"tag\":\"cortex\", \"type\":\"INFO\", \"text\":\"message\"}";
        //databaseHandler.validate(sensorName, value);
        closeRealm();
    }

    public void testSensorLight() throws Exception {
        SensorProfile sensorProfile = results.get(5);
        String sensorName = "light";
        String dataStructure = "{\"sensor_name\":\"light\",\"data_structure\":\"{\\\"$schema\\\": \\\"http:\\/\\/json-schema.org\\/draft-04\\/schema#\\\",\\\"description\\\": \\\"The illuminance in lux\\\",\\\"type\\\": \\\"number\\\"}\"}";
        assertEquals(sensorName, sensorProfile.getSensorName());
        assertEquals(dataStructure, sensorProfile.getDataStructure());
        String value = "{\"value\":2}";
        //databaseHandler.validate(sensorName, value);
        closeRealm();
    }

    public void testSensorMentalResilience() throws Exception {
        SensorProfile sensorProfile = results.get(6);
        String sensorName = "mental_resilience";
        String dataStructure = "{\"sensor_name\":\"mental_resilience\",\"data_structure\":\"{\\\"$schema\\\": \\\"http:\\/\\/json-schema.org\\/draft-04\\/schema#\\\",\\\"type\\\": \\\"object\\\",\\\"properties\\\": {\\\"question\\\": {\\\"description\\\": \\\"The question text\\\",\\\"type\\\": \\\"string\\\"}, \\\"score\\\": {\\\"description\\\": \\\"The mental resilience value (e.g. 30)\\\",\\\"type\\\": \\\"integer\\\"},\\\"question_id\\\": {\\\"description\\\": \\\"The identifier of the question (e.g. stylemsg_id)\\\", \\\"type\\\": \\\"integer\\\"}, \\\"answer_id\\\": {\\\"description\\\": \\\"The identifier of the answer (e.g. 2, for a 10 point scale 0-9)\\\", \\\"type\\\": \\\"integer\\\"}, \\\"answer\\\": {\\\"description\\\": \\\"The string representation of the answer (e.g. 30%)\\\", \\\"type\\\": \\\"string\\\"}, \\\"task_type\\\": {\\\"description\\\": \\\"The Task.TYPE representing the domain to which this question belongs to (e.g. MR_TASK_DOMAIN_1)\\\", \\\"type\\\": \\\"string\\\"}},\\\"required\\\": [\\\"question\\\",\\\"score\\\",\\\"question_id\\\",\\\"answer_id\\\", \\\"answer\\\",\\\"task_type\\\"]}\"}";
        assertEquals(sensorName, sensorProfile.getSensorName());
        assertEquals(dataStructure, sensorProfile.getDataStructure());
        String value = "{\"question\":\"how do you feel today \",\"score\":2, \"question_id\":0,\"answer_id\":0,\"answer\":\"leave me alone\",\"task_type\":\"MR_TASK_DOMAIN_1\"}";
        //databaseHandler.validate(sensorName, value);
        closeRealm();
    }

    public void testSensorNoise() throws Exception {
        SensorProfile sensorProfile = results.get(7);
        String sensorName = "noise";
        String dataStructure = "{\"sensor_name\":\"noise\",\"data_structure\":\"{\\\"$schema\\\": \\\"http:\\/\\/json-schema.org\\/draft-04\\/schema#\\\",\\\"description\\\": \\\"The Ambient noise in decibel\\\",\\\"type\\\": \\\"number\\\"}\"}";
        assertEquals(sensorName, sensorProfile.getSensorName());
        assertEquals(dataStructure, sensorProfile.getDataStructure());
        String value = "{\"value\":20}";
        //databaseHandler.validate(sensorName, value);
        closeRealm();
    }

    public void testSensorPosition() throws Exception {
        SensorProfile sensorProfile = results.get(8);
        String sensorName = "position";
        String dataStructure = "{\"sensor_name\":\"position\",\"data_structure\":\"{\\\"$schema\\\": \\\"http:\\/\\/json-schema.org\\/draft-04\\/schema#\\\",\\\"type\\\": \\\"object\\\",\\\"properties\\\": {\\\"longitude\\\": {\\\"description\\\": \\\"The longitude in degrees\\\",\\\"type\\\": \\\"number\\\"},\\\"latitude\\\": {\\\"description\\\": \\\"The latitude in degrees\\\",\\\"type\\\": \\\"number\\\"},\\\"altitude\\\": {\\\"description\\\": \\\"altitude in meters above the WGS 84 reference ellipsoid.\\\",\\\"type\\\": \\\"number\\\"},\\\"accuracy\\\": {\\\"description\\\": \\\"accuracy in meters\\\",\\\"type\\\": \\\"number\\\"},\\\"speed\\\": {\\\"description\\\": \\\"The speed in meters\\/second over ground.\\\",\\\"type\\\": \\\"number\\\"},\\\"bearing\\\": {\\\"description\\\": \\\"The average bearing in degrees\\\",\\\"type\\\": \\\"number\\\"},\\\"provider\\\": {\\\"description\\\": \\\"The location provider, e.g. GPS, NETWORK or FUSED\\\",\\\"type\\\": \\\"string\\\"}},\\\"required\\\": [\\\"longitude\\\",\\\"latitude\\\",\\\"accuracy\\\",\\\"provider\\\"]}\"}";
        assertEquals(sensorName, sensorProfile.getSensorName());
        assertEquals(dataStructure, sensorProfile.getDataStructure());
        String value = "{\"longitude\":63.5,\"latitude\":44.8,\"accuracy\":90,\"provider\":\"GPS\"}";
        //databaseHandler.validate(sensorName, value);
        closeRealm();

    }

    public void testSensorProximity() throws Exception {
        SensorProfile sensorProfile = results.get(9);
        String sensorName = "proximity";
        String dataStructure = "{\"sensor_name\":\"proximity\",\"data_structure\":\"{\\\"$schema\\\": \\\"http:\\/\\/json-schema.org\\/draft-04\\/schema#\\\",\\\"description\\\": \\\"The proximity to an object in cm\\\",\\\"type\\\": \\\"number\\\"}\"}";
        assertEquals(sensorName, sensorProfile.getSensorName());
        assertEquals(dataStructure, sensorProfile.getDataStructure());
        String value = "{\"value\":30}";
        //databaseHandler.validate(sensorName, value);
        closeRealm();
    }

    public void testSensorScreen() throws Exception {
        SensorProfile sensorProfile = results.get(10);
        String sensorName = "screen";
        String dataStructure = "{\"sensor_name\":\"screen\",\"data_structure\":\"{\\\"$schema\\\": \\\"http:\\/\\/json-schema.org\\/draft-04\\/schema#\\\",\\\"description\\\": \\\"The status of the screen, e.g. on or off\\\",\\\"enum\\\": [ \\\"on\\\", \\\"off\\\"]}\"}";
        assertEquals(sensorName, sensorProfile.getSensorName());
        assertEquals(dataStructure, sensorProfile.getDataStructure());
        String value = "{\"value\":\"on\"}";
        //databaseHandler.validate(sensorName, value);
        closeRealm();
    }

    public void testSensorSleep() throws Exception {
        SensorProfile sensorProfile = results.get(11);
        String sensorName = "sleep";
        String dataStructure = "{\"sensor_name\":\"sleep\",\"data_structure\":\"{\\\"$schema\\\": \\\"http:\\/\\/json-schema.org\\/draft-04\\/schema#\\\",\\\"type\\\": \\\"object\\\",\\\"properties\\\": {\\\"end_date\\\": {\\\"description\\\" : \\\"The end time of the sleep period in epoch seconds\\\",\\\"type\\\": \\\"number\\\"},\\\"metadata\\\": {\\\"type\\\": \\\"object\\\",\\\"properties\\\": {\\\"core version\\\": {\\\"description\\\" : \\\"The version of the cortex core\\\",\\\"type\\\": \\\"string\\\"},\\\"module version\\\": {\\\"description\\\" : \\\"The version of the sleep_time module\\\",\\\"type\\\":\\\"string\\\"},\\\"status\\\": {\\\"description\\\" : \\\"The current status of the sleep_time module for debugging module, e.g. awake: too much noise\\\",\\\"type\\\": \\\"string\\\"}}},\\\"hours\\\": {\\\"description\\\" : \\\"The number of actual sleep hours\\\",\\\"type\\\": \\\"number\\\"},\\\"start_date\\\": {\\\"description\\\" : \\\"The start time of the sleep period in epoch seconds\\\",\\\"type\\\": \\\"number\\\"}},\\\"required\\\": [\\\"end_time\\\",\\\"hours\\\",\\\"start_time\\\"]}\"}";
        assertEquals(sensorName, sensorProfile.getSensorName());
        assertEquals(dataStructure, sensorProfile.getDataStructure());
        String value = "{\"start_time\":\"456123312\",\"end_time\":\"456123321\",\"hours\":2}";
        //databaseHandler.validate(sensorName, value);
        closeRealm();
    }

    public void testSensorSleepEstimate() throws Exception {
        SensorProfile sensorProfile = results.get(12);
        String sensorName = "sleep_estimate";
        String dataStructure = "{\"sensor_name\":\"sleep_estimate\",\"data_structure\":\"{\\\"$schema\\\": \\\"http:\\/\\/json-schema.org\\/draft-04\\/schema#\\\",\\\"type\\\": \\\"object\\\",\\\"properties\\\": {\\\"end_time\\\": { \\\"description\\\" : \\\"The end time of the sleep period in epoch seconds\\\",\\\"type\\\":\\\"number\\\"},\\\"history_based\\\": {\\\"description\\\" : \\\"Whether the sleep value is based on the history or on the actual sleep period\\\",\\\"type\\\": \\\"boolean\\\"},\\\"metadata\\\": {\\\"type\\\":\\\"object\\\",\\\"properties\\\": {\\\"core version\\\": {\\\"description\\\" : \\\"The version of the cortex core\\\",\\\"type\\\": \\\"string\\\"},\\\"module version\\\": {\\\"description\\\" : \\\"The version of the sleep_time module\\\",\\\"type\\\": \\\"string\\\" },\\\"status\\\": {\\\"description\\\" : \\\"The current status of the sleep_time module for debugging module, e.g. awake: too much noise\\\", \\\"type\\\": \\\"string\\\" } } }, \\\"hours\\\":{\\\"description\\\" : \\\"The number of actual sleep hours\\\",\\\"type\\\": \\\"number\\\"},\\\"start_time\\\": { \\\"description\\\" : \\\"The start time of the sleep period in epoch seconds\\\",\\\"type\\\": \\\"number\\\"}},\\\"required\\\": [\\\"end_time\\\",\\\"hours\\\",\\\"start_time\\\"]}\"}";
        assertEquals(sensorName, sensorProfile.getSensorName());
        assertEquals(dataStructure, sensorProfile.getDataStructure());
        String value = "{\"start_time\":\"456123312\",\"end_time\":\"456123321\",\"hours\":2}";
        //databaseHandler.validate(sensorName, value);
        closeRealm();
    }

    public void testSensorTimeActive() throws Exception {
        SensorProfile sensorProfile = results.get(13);
        String sensorName = "time_active";
        String dataStructure = "{\"sensor_name\":\"time_active\",\"data_structure\":\"{\\\"$schema\\\": \\\"http:\\/\\/json-schema.org\\/draft-04\\/schema#\\\", \\\"description\\\": \\\"The time physically active in seconds\\\", \\\"type\\\": \\\"integer\\\"}\"}";
        assertEquals(sensorName, sensorProfile.getSensorName());
        assertEquals(dataStructure, sensorProfile.getDataStructure());
        String value = "{\"value\":6}";
        //databaseHandler.validate(sensorName, value);
        closeRealm();
    }

    public void testSensorTimeZone() throws Exception {
        SensorProfile sensorProfile = results.get(14);
        String sensorName = "time_zone";
        String dataStructure = "{\"sensor_name\":\"time_zone\",\"data_structure\":\"{\\\"$schema\\\": \\\"http:\\/\\/json-schema.org\\/draft-04\\/schema#\\\",\\\"type\\\": \\\"object\\\",\\\"properties\\\": {\\\"id\\\": {\\\"description\\\": \\\"The time-zone id, e.g. Europe\\/Paris\\\",\\\"type\\\": \\\"string\\\"},\\\"offset\\\": {\\\"description\\\": \\\"The offset from GMT in seconds\\\",\\\"type\\\": \\\"integer\\\"}},\\\"required\\\": [\\\"offset\\\"]}\"}";
        assertEquals(sensorName, sensorProfile.getSensorName());
        assertEquals(dataStructure, sensorProfile.getDataStructure());
        String value = "{\"offset\":1}";
        //databaseHandler.validate(sensorName, value);
        closeRealm();

    }

    public void testSensorWifiScan() throws Exception {
        SensorProfile sensorProfile = results.get(15);
        String sensorName = "wifi_scan";
        String dataStructure = "{\"sensor_name\":\"wifi_scan\",\"data_structure\":\"{\\\"$schema\\\": \\\"http:\\/\\/json-schema.org\\/draft-04\\/schema#\\\",\\\"type\\\": \\\"object\\\",\\\"properties\\\": {\\\"ssid\\\": {\\\"description\\\": \\\"The name of the detected wifi device\\\",\\\"type\\\": \\\"string\\\"},\\\"bssid\\\": {\\\"description\\\": \\\"The mac address of the detected wifi device\\\",\\\"type\\\": \\\"string\\\"},\\\"frequency\\\": {\\\"description\\\":\\\"The signal frequency of the detected wifi device\\\",\\\"type\\\":\\\"integer\\\"},\\\"rssi\\\": {\\\"description\\\": \\\"The signal strength of the detected wifi device\\\",\\\"type\\\": \\\"integer\\\"},\\\"capabilities\\\": {\\\"description\\\": \\\"The capabilities of the detected wifi device, e.g. encryption, WPS\\\",\\\"type\\\": \\\"string\\\"}},\\\"required\\\": [\\\"bssid\\\"]}\"}";
        assertEquals(sensorName, sensorProfile.getSensorName());
        assertEquals(dataStructure, sensorProfile.getDataStructure());
        String value = "{\"bssid\":\"remcowasnothere\"}";
        //databaseHandler.validate(sensorName, value);
        closeRealm();
    }
}
