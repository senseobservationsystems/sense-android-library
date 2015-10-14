package nl.sense_os.datastorageengine.test;

import android.test.AndroidTestCase;
import android.util.Log;

import org.apache.http.client.HttpResponseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import nl.sense_os.datastorageengine.HTTPUtil;
import nl.sense_os.datastorageengine.QueryOptions;
import nl.sense_os.datastorageengine.SensorDataProxy;

public class TestSensorDataProxy extends AndroidTestCase {
    private static final String TAG = "TestSensorDataProxy";

    Map<String, String> user;
    SensorDataProxy.SERVER server = SensorDataProxy.SERVER.STAGING;
    String appKey = "E9Noi5s402FYo2Gc6a7pDTe4H3UvLkWa";  // application key for dev, android, Brightr ASML
    String sessionId;
    String sourceName = "sense-android";
    SensorDataProxy proxy;
    CSUtils csUtils;

    public  void setUp () throws IOException {
        Log.d(TAG, "setUp");

        csUtils = new CSUtils(false); // staging server

        user = csUtils.createCSAccount();
        sessionId = csUtils.loginUser(user.get("username"), user.get("password"));

        proxy = new SensorDataProxy(server, appKey, sessionId);
    }

    public void tearDown() throws IOException {
        Log.d(TAG, "tearDown");

        csUtils.deleteAccount(user.get("username"), user.get("password"),user.get("id"));
    }

    public void testInvalidAppKey () {
        Log.d(TAG, "testInvalidAppKey");

        // create a proxy with invalid appKey
        String invalidAppKey = "uh oh";
        SensorDataProxy proxy2 = new SensorDataProxy(server, invalidAppKey, sessionId);

        try {
            // should fail because of invalid app key
            proxy2.getSensors();

            fail("Missing exception");
        } catch (HttpResponseException e) {
            e.printStackTrace();
            assertEquals("Should throw an unauthorized exception", 401, e.getStatusCode());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            fail("Wrong exception thrown");
        }
    }

    public void testInvalidSessionId () {
        Log.d(TAG, "testInvalidSessionId");

        // create a proxy with invalid appKey
        String invalidSessionId = "no go";
        SensorDataProxy proxy2 = new SensorDataProxy(server, appKey, invalidSessionId);

        try {
            // should fail because of invalid session id
            proxy2.getSensors();

            fail("Missing exception");
        } catch (HttpResponseException e) {
            e.printStackTrace();
            assertEquals("Should throw an unauthorized exception", 401, e.getStatusCode());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            fail("Wrong exception thrown");
        }
    }

    public void testGetSensorProfiles () throws JSONException, IOException {
        Log.d(TAG, "testGetSensorProfiles");

        JSONArray profiles = proxy.getSensorProfiles();
        assertTrue("Should return an empty sensor list", profiles.length() > 0);

        List keys = toArrayList(profiles.getJSONObject(0).keys());
        Collections.sort(keys);
        assertEquals("Should contain two entries", 2, keys.size());
        assertEquals("Should contain two entries", "data_type", keys.get(0));
        assertEquals("Should contain two entries", "sensor_name", keys.get(1));
    }

    public void testGetSensors () throws IOException, JSONException {
        Log.d(TAG, "testGetSensors");

        // initially we should have no sensors
        JSONArray sensors = proxy.getSensors();
        assertEquals("Should return an empty sensor list", 0, sensors.length());

        // create a sensor by putting a meta field
        String sensorName = "accelerometer";
        JSONObject meta = new JSONObject("{\"foo\":\"bar\"}");
        proxy.updateSensor(sourceName, sensorName, meta);

        // test if we get the created sensor back
        JSONArray sensors1 = proxy.getSensors();
        JSONArray expected = new JSONArray();
        JSONObject sensor = new JSONObject();
        sensor.put("source_name", sourceName);
        sensor.put("sensor_name", sensorName);
        sensor.put("meta", meta);
        expected.put(sensor);
        JSONAssert.assertEquals(expected, sensors1, false);
    }

    public void testGetSensor () throws IOException, JSONException {
        Log.d(TAG, "testGetSensor");

        // create a sensor by putting a meta field
        String sensorName = "accelerometer";
        JSONObject meta = new JSONObject("{\"foo\":\"bar\"}");
        proxy.updateSensor(sourceName, sensorName, meta);

        // test if we get the created sensor back
        JSONObject sensor = proxy.getSensor(sourceName, sensorName);
        JSONObject expected = new JSONObject();
        expected.put("source_name", sourceName);
        expected.put("sensor_name", sensorName);
        expected.put("meta", meta);
        JSONAssert.assertEquals(expected, sensor, false);
    }

    public void testGetNonExistingSensor () {
        Log.d(TAG, "testGetNonExistingSensor");

        try {
            String nonExistingSensorName = "accelerometer";
            proxy.getSensor(sourceName, nonExistingSensorName);

            fail("Missing exception");
        } catch (HttpResponseException e) {
            assertEquals("Should throw a not found exception", 404, e.getStatusCode());
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            fail("Wrong exception thrown");
        }
    }

    public void testGetSensorInvalidSource () {
        Log.d(TAG, "testGetSensorInvalidSource");

        try {
            String nonExistingSourceName = "foo";
            String sensorName = "accelerometer";
            proxy.getSensor(nonExistingSourceName, sensorName);

            fail("Missing exception");
        } catch (HttpResponseException e) {
            assertEquals("Should throw a not found exception", 404, e.getStatusCode());
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            fail("Wrong exception thrown");
        }
    }

    public void testUpdateSensor () throws IOException, JSONException {
        Log.d(TAG, "testUpdateSensor");

        // update first time (will create the sensor
        String sensorName = "accelerometer";
        JSONObject meta = new JSONObject("{\"foo\":\"bar\"}");
        JSONObject updated = proxy.updateSensor(sourceName, sensorName, meta);

        JSONObject expected = new JSONObject();
        expected.put("source_name", sourceName);
        expected.put("sensor_name", sensorName);
        expected.put("meta", meta);
        JSONAssert.assertEquals(expected, updated, false);

        // update again
        JSONObject meta2 = new JSONObject("{\"the whether\":\"is nice\"}");
        JSONObject updated2 = proxy.updateSensor(sourceName, sensorName, meta2);

        JSONObject expected2 = new JSONObject();
        expected.put("source_name", sourceName);
        expected.put("sensor_name", sensorName);
        expected.put("meta", meta2);
        JSONAssert.assertEquals(expected2, updated2, false);
    }

    public void testUpdateNonExistingSensor () {
        Log.d(TAG, "testUpdateNonExistingSensor");

        try {
            String nonExistingSensorName = "accelerometer";
            JSONObject meta = new JSONObject("{\"foo\":\"bar\"}");
            proxy.updateSensor(sourceName, nonExistingSensorName, meta);

            fail("Missing exception");
        } catch (HttpResponseException e) {
            assertEquals("Should throw a not found exception", 404, e.getStatusCode());
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            fail("Wrong exception thrown");
        }
    }

    public void testUpdateSensorInvalidSource () {
        Log.d(TAG, "testUpdateSensorInvalidSource");

        try {
            String nonExistingSourceName = "foo";
            String sensorName = "accelerometer";
            JSONObject meta = new JSONObject("{\"foo\":\"bar\"}");
            proxy.updateSensor(nonExistingSourceName, sensorName, meta);

            fail("Missing exception");
        } catch (HttpResponseException e) {
            assertEquals("Should throw a not found exception", 404, e.getStatusCode());
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            fail("Wrong exception thrown");
        }
    }

    public void testDeleteSensor () throws IOException, JSONException {
        Log.d(TAG, "testDeleteSensor");

        // update first time (will create the sensor
        String sensorName = "accelerometer";
        JSONObject meta = new JSONObject("{\"foo\":\"bar\"}");
        JSONObject updated = proxy.updateSensor(sourceName, sensorName, meta);

        // sensor list must contain 1 sensor
        JSONArray sensors = proxy.getSensors();
        JSONArray expected = new JSONArray();
        JSONObject sensor = new JSONObject();
        sensor.put("source_name", sourceName);
        sensor.put("sensor_name", sensorName);
        sensor.put("meta", meta);
        expected.put(sensor);
        JSONAssert.assertEquals(expected, sensors, false);

        // delete the sensor
        proxy.deleteSensor(sourceName, sensorName);

        // sensor list must be empty again
        JSONArray sensors2 = proxy.getSensors();
        JSONArray expected2 = new JSONArray();
        JSONAssert.assertEquals(expected2, sensors2, false);
    }

    public void testDeleteNonExistingSensor () {
        Log.d(TAG, "testDeleteNonExistingSensor");

        try {
            String nonExistingSensorName = "accelerometer";
            proxy.deleteSensor(sourceName, nonExistingSensorName);

            fail("Missing exception");
        } catch (HttpResponseException e) {
            assertEquals("Should throw a not found exception", 404, e.getStatusCode());
        } catch (IOException e) {
            e.printStackTrace();
            fail("Wrong exception thrown");
        }
    }

    public void testDeleteSensorInvalidSource () {
        Log.d(TAG, "testDeleteSensorInvalidSource");

        try {
            String nonExistingSourceName = "foo";
            String sensorName = "accelerometer";
            proxy.deleteSensor(nonExistingSourceName, sensorName);

            fail("Missing exception");
        } catch (HttpResponseException e) {
            assertEquals("Should throw a not found exception", 404, e.getStatusCode());
        } catch (IOException e) {
            e.printStackTrace();
            fail("Wrong exception thrown");
        }
    }

    public void testPutAndGetSensorData() throws IOException, RuntimeException, JSONException {
        Log.d(TAG, "testPutAndGetSensorData");

        String sensorName = "time_active";
        JSONArray data = new JSONArray();
        data.put(new JSONObject("{\"time\":1444739042200,\"value\":2}"));
        data.put(new JSONObject("{\"time\":1444739042300,\"value\":3}"));
        data.put(new JSONObject("{\"time\":1444739042400,\"value\":4}"));

        // put sensor data
        proxy.putSensorData(sourceName, sensorName, data);

        // get sensor data
        JSONArray retrievedData = proxy.getSensorData(sourceName, sensorName, new QueryOptions());
        JSONAssert.assertEquals(data, retrievedData, false);

        // get limited sensor data
        QueryOptions options = new QueryOptions();
        options.setLimit(2);
        JSONArray limitedData = proxy.getSensorData(sourceName, sensorName, options);
        JSONArray expected = new JSONArray();
        expected.put(data.get(0));
        expected.put(data.get(1));
        JSONAssert.assertEquals(expected, limitedData, false);
    }

    public void testPutSensorDataWithMeta() throws IOException, RuntimeException, JSONException {
        Log.d(TAG, "testPutSensorDataWithMeta");

        String sensorName = "time_active";
        JSONArray data = new JSONArray();
        data.put(new JSONObject("{\"time\":1444739042200,\"value\":2}"));
        data.put(new JSONObject("{\"time\":1444739042300,\"value\":3}"));
        data.put(new JSONObject("{\"time\":1444739042400,\"value\":4}"));
        JSONObject meta = new JSONObject("{\"foo\":\"bar\"}");

        // put sensor data
        proxy.putSensorData(sourceName, sensorName, data, meta);

        // get sensor
        JSONObject sensor = proxy.getSensor(sourceName, sensorName);
        JSONObject expected = new JSONObject();
        expected.put("source_name", sourceName);
        expected.put("sensor_name", sensorName);
        expected.put("meta", meta);
        JSONAssert.assertEquals(expected, sensor, false);

        // get sensor data
        JSONArray retrievedData = proxy.getSensorData(sourceName, sensorName, new QueryOptions());
        JSONAssert.assertEquals(data, retrievedData, false);
    }

    public void testPutMultipleSensors() throws IOException, RuntimeException, JSONException {
        Log.d(TAG, "testPutMultipleSensors");

        String sensorName1 = "time_active";
        JSONArray data1 = new JSONArray();
        data1.put(new JSONObject("{\"time\":1444739042200,\"value\":2}"));
        data1.put(new JSONObject("{\"time\":1444739042300,\"value\":3}"));
        data1.put(new JSONObject("{\"time\":1444739042400,\"value\":4}"));
        JSONObject sensor1 = proxy.createSensorDataObject(sourceName, sensorName1, data1);

        String sensorName2 = "noise_sensor";
        JSONArray data2 = new JSONArray();
        data2.put(new JSONObject("{\"time\":1444814218000,\"value\":6}"));
        data2.put(new JSONObject("{\"time\":1444814219000,\"value\":7}"));
        data2.put(new JSONObject("{\"time\":1444814220000,\"value\":8}"));
        JSONObject sensor2 = proxy.createSensorDataObject(sourceName, sensorName2, data2);

        JSONArray sensors = new JSONArray();
        sensors.put(sensor1);
        sensors.put(sensor2);

        // put sensor data of multiple sensors. isn't it aaaawesome
        proxy.putSensorData(sensors);

        // get sensor1
        JSONObject sensor1retrieved = proxy.getSensor(sourceName, sensorName1);
        JSONObject expected1 = new JSONObject();
        expected1.put("source_name", sourceName);
        expected1.put("sensor_name", sensorName1);
        expected1.put("meta", null);
        JSONAssert.assertEquals(expected1, sensor1, false);

        // get sensor1 data
        JSONArray retrievedData1 = proxy.getSensorData(sourceName, sensorName1, new QueryOptions());
        JSONAssert.assertEquals(data1, retrievedData1, false);
        
        // get sensor2
        JSONObject sensor2retrieved = proxy.getSensor(sourceName, sensorName2);
        JSONObject expected2 = new JSONObject();
        expected2.put("source_name", sourceName);
        expected2.put("sensor_name", sensorName2);
        expected2.put("meta", null);
        JSONAssert.assertEquals(expected2, sensor2, false);

        // get sensor2 data
        JSONArray retrievedData2 = proxy.getSensorData(sourceName, sensorName2, new QueryOptions());
        JSONAssert.assertEquals(data2, retrievedData2, false);
    }

    public void testPutMultipleSensorsInvalidSource() throws IOException, RuntimeException, JSONException {
        Log.d(TAG, "testPutMultipleSensorsInvalidSource");

        try {
            String nonExistingSourceName = "nonono";
            String sensorName = "time_active";

            JSONArray data = new JSONArray();
            data.put(new JSONObject("{\"time\":1444739042200,\"value\":2}"));
            data.put(new JSONObject("{\"time\":1444739042300,\"value\":3}"));
            data.put(new JSONObject("{\"time\":1444739042400,\"value\":4}"));

            // put sensor data, should fail
            proxy.putSensorData(nonExistingSourceName, sensorName, data);

            fail("Missing exception");
        } catch (HttpResponseException e) {
            assertEquals("Should throw a not found exception", 404, e.getStatusCode());
        } catch (IOException e) {
            e.printStackTrace();
            fail("Wrong exception thrown");
        }
    }

    public void testGetNonExistingSensorData() throws IOException, RuntimeException, JSONException {
        Log.d(TAG, "testGetNonExistingSensorData");

        try {
            String nonExistingSensorName = "real_time_world_population";

            // get sensor data, should fail
            proxy.getSensorData(sourceName, nonExistingSensorName, new QueryOptions());

            fail("Missing exception");
        } catch (HttpResponseException e) {
            assertEquals("Should throw a not found exception", 404, e.getStatusCode());
        } catch (IOException e) {
            e.printStackTrace();
            fail("Wrong exception thrown");
        }
    }

    public void testGetSensorDataInvalidSource() throws IOException, RuntimeException, JSONException {
        Log.d(TAG, "testGetSensorDataInvalidSource");

        try {
            String invalidSourceName = "sense_blackberry";
            String sensorName = "accelerometer";

            // get sensor data, should fail
            proxy.getSensorData(invalidSourceName, sensorName, new QueryOptions());

            fail("Missing exception");
        } catch (HttpResponseException e) {
            assertEquals("Should throw a not found exception", 404, e.getStatusCode());
        } catch (IOException e) {
            e.printStackTrace();
            fail("Wrong exception thrown");
        }
    }

    public void testCreateSensorDataObject() throws IOException, RuntimeException, JSONException {
        Log.d(TAG, "testCreateSensorDataObject");

        String sourceName = "the_source";
        String sensorName = "the_sensor";

        JSONArray data = new JSONArray();
        data.put(new JSONObject("{\"time\":1444739042200,\"value\":2}"));
        data.put(new JSONObject("{\"time\":1444739042300,\"value\":3}"));
        data.put(new JSONObject("{\"time\":1444739042400,\"value\":4}"));


        JSONObject actual = proxy.createSensorDataObject(sourceName, sensorName, data);

        JSONObject expected = new JSONObject();
        expected.put("source_name", sourceName);
        expected.put("sensor_name", sensorName);
        expected.put("data", data);

        JSONAssert.assertEquals(expected, actual, false);
    }

    public void testCreateSensorDataObjectWithMeta() throws IOException, RuntimeException, JSONException {
        Log.d(TAG, "testCreateSensorDataObjectWithMeta");

        String sourceName = "the_source";
        String sensorName = "the_sensor";

        JSONArray data = new JSONArray();
        data.put(new JSONObject("{\"time\":1444739042200,\"value\":2}"));
        data.put(new JSONObject("{\"time\":1444739042300,\"value\":3}"));
        data.put(new JSONObject("{\"time\":1444739042400,\"value\":4}"));

        JSONObject meta = new JSONObject();
        meta.put("foo", "bar");

        JSONObject actual = proxy.createSensorDataObject(sourceName, sensorName, data, meta);

        JSONObject expected = new JSONObject();
        expected.put("source_name", sourceName);
        expected.put("sensor_name", sensorName);
        expected.put("data", data);
        expected.put("meta", meta);

        JSONAssert.assertEquals(expected, actual, false);
    }

    public void testDeleteSensorData() throws IOException, RuntimeException, JSONException {
        Log.d(TAG, "testDeleteSensorData");

        String sensorName = "time_active";

        // create sensor data
        JSONArray data = new JSONArray();
        data.put(new JSONObject("{\"time\":1444739042200,\"value\":1}"));
        data.put(new JSONObject("{\"time\":1444739042200,\"value\":2}"));
        data.put(new JSONObject("{\"time\":1444739042300,\"value\":3}"));
        data.put(new JSONObject("{\"time\":1444739042400,\"value\":4}"));
        data.put(new JSONObject("{\"time\":1444739042500,\"value\":5}"));
        proxy.putSensorData(sourceName, sensorName, data);

        // delete sensor data
        long startTime = 1444739042200l;  // time of point with value 2
        long endTime = 1444739042400l;    // time of point with value 4
        proxy.deleteSensorData(sourceName, sensorName, startTime, endTime);

        // see whether point 2 and 3 are deleted
        // (not point 4, endTime itself should be execluded)
        JSONArray actual = proxy.getSensorData(sourceName, sensorName, new QueryOptions());
        JSONArray expected = new JSONArray();
        data.put(new JSONObject("{\"time\":1444739042200,\"value\":1}"));
        data.put(new JSONObject("{\"time\":1444739042400,\"value\":4}"));
        data.put(new JSONObject("{\"time\":1444739042500,\"value\":5}"));
        JSONAssert.assertEquals(expected, actual, false);
    }

    public void testDeleteSensorDataInvalidSource() throws IOException, RuntimeException, JSONException {
        Log.d(TAG, "testDeleteSensorDataInvalidSource");

        try {
            String invalidSourceName = "imaginary";
            String sensorName = "accelerometer";

            // delete sensor data, should fail
            proxy.deleteSensorData(invalidSourceName, sensorName, 0l, 1l);

            fail("Missing exception");
        } catch (HttpResponseException e) {
            assertEquals("Should throw a not found exception", 404, e.getStatusCode());
        } catch (IOException e) {
            e.printStackTrace();
            fail("Wrong exception thrown");
        }
    }

    public void testDeleteNonExistingSensorData() throws IOException, RuntimeException, JSONException {
        Log.d(TAG, "testDeleteNonExistingSensorData");

        try {
            String nonExistingSensorName = "foo";

            // delete sensor data, should fail
            proxy.deleteSensorData(sourceName, nonExistingSensorName, 0l, 1l);

            fail("Missing exception");
        } catch (HttpResponseException e) {
            assertEquals("Should throw a not found exception", 404, e.getStatusCode());
        } catch (IOException e) {
            e.printStackTrace();
            fail("Wrong exception thrown");
        }
    }

    /**
     * Helper function to convert an iterator into an ArrayList
     * @param iterator  Iterator over which to loop
     * @return Returns an ArrayList
     */
    public <T> ArrayList<T> toArrayList(Iterator<T> iterator) {
        ArrayList<T> array= new ArrayList<T>();
        while (iterator.hasNext()) {
            array.add(iterator.next());
        }
        return array;
    }

}
