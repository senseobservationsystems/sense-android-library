package nl.sense_os.datastorageengine.test;

import android.test.AndroidTestCase;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import nl.sense_os.datastorageengine.HTTPUtil;
import nl.sense_os.datastorageengine.QueryOptions;
import nl.sense_os.datastorageengine.SensorDataProxy;

public class TestSensorDataProxy extends AndroidTestCase {
    private static final String TAG = "TestSensorDataProxy";

    Map<String, String> user;
    String appKey = "E9Noi5s402FYo2Gc6a7pDTe4H3UvLkWa";  // dev, android, Brightr ASML
    String sessionId;
    String sourceName = "sense-android";
    CSUtils csUtils = new CSUtils(false);
    SensorDataProxy proxy;

    public  void setUp () throws IOException {
        Log.d(TAG, "setUp");

        SensorDataProxy.SERVER server = SensorDataProxy.SERVER.STAGING;
        user = csUtils.createCSAccount();
        sessionId = csUtils.loginUser(user.get("username"), user.get("password"));

        proxy = new SensorDataProxy(server, appKey, sessionId);
    }

    public void tearDown() throws IOException {
        Log.d(TAG, "tearDown");

        csUtils.deleteAccount(user.get("username"), user.get("password"),user.get("id"));
    }

    public void testGetSensors() throws IOException, RuntimeException, JSONException {
        Log.d(TAG, "testGetSensors");

        JSONArray sensors = proxy.getSensors();

        assertEquals("Sensor list must be empty", 0, sensors.length());
    }

    public void testUpdateSensor() throws IOException, RuntimeException, JSONException {
        Log.d(TAG, "testUpdateSensor");

        String sensorName = "accelerometer";
        JSONObject meta = new JSONObject();
        meta.put("foo", "bar");
        proxy.updateSensor(sourceName, sensorName, meta);

        JSONObject sensor = proxy.getSensor(sourceName, sensorName);
        Log.d(TAG, "sensor=" + sensor.toString());

        assertEquals("meta field contains the added field", "{\"foo\":\"bar\"}", sensor.getString("meta"));
        assertEquals("meta field contains the added field", "{\"foo\":\"bar\"}", sensor.getJSONObject("meta").toString());
    }

    public void testPutSensorData() throws IOException, RuntimeException, JSONException {
        Log.d(TAG, "testPutSensorData");

        String sensorName = "time_active";
        JSONArray data = new JSONArray();
        data.put(new JSONObject("{\"date\":1444739042200,\"value\":2}"));
        data.put(new JSONObject("{\"date\":1444739042300,\"value\":3}"));
        data.put(new JSONObject("{\"date\":1444739042400,\"value\":4}"));

        proxy.putSensorData(sourceName, sensorName, data);

        JSONArray retrievedData = proxy.getSensorData(sourceName, sensorName, new QueryOptions());

        assertEquals("returns the right number of data points", 3, retrievedData.length());
    }

}
