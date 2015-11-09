package nl.sense_os.datastorageengine.test;

import android.test.AndroidTestCase;
import android.util.Log;

import org.apache.http.client.HttpResponseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import nl.sense_os.datastorageengine.DataPoint;
import nl.sense_os.datastorageengine.DataSyncer;
import nl.sense_os.datastorageengine.DatabaseHandler;
import nl.sense_os.datastorageengine.DatabaseHandlerException;
import nl.sense_os.datastorageengine.QueryOptions;
import nl.sense_os.datastorageengine.Sensor;
import nl.sense_os.datastorageengine.SensorDataProxy;
import nl.sense_os.datastorageengine.SensorException;
import nl.sense_os.datastorageengine.SensorOptions;
import nl.sense_os.datastorageengine.SensorProfileException;
import nl.sense_os.datastorageengine.SensorProfiles;
import nl.sense_os.util.json.SchemaException;
import nl.sense_os.util.json.ValidationException;

public class TestDataSyncer extends AndroidTestCase {
    private String TAG = "TestDataSyncer";

    private CSUtils csUtils;
    private Map<String, String> newUser;
    SensorDataProxy.SERVER server = SensorDataProxy.SERVER.STAGING;
    private String sourceName = "sense-android";
    private String appKey = "E9Noi5s402FYo2Gc6a7pDTe4H3UvLkWa";  // application key for dev, android, Brightr ASML
    private String sessionId;
    private DatabaseHandler databaseHandler;
    private SensorDataProxy proxy;
    private DataSyncer dataSyncer;

    @Override
    protected void setUp () throws Exception {
        // remove old realm data
        RealmConfiguration testConfig = new RealmConfiguration.Builder(getContext()).build();
        Realm.deleteRealm(testConfig);

        csUtils = new CSUtils(false);
        newUser = csUtils.createCSAccount();
        String userId = newUser.get("id");
        sessionId = csUtils.loginUser(newUser.get("username"), newUser.get("password"));
        databaseHandler = new DatabaseHandler(getContext(), userId);
        proxy = new SensorDataProxy(server, appKey, sessionId);
        dataSyncer = new DataSyncer(getContext(), databaseHandler, proxy);
        dataSyncer.initialize();
    }

    @Override
    protected void tearDown () throws Exception {
        csUtils.deleteAccount(newUser.get("username"), newUser.get("password"), newUser.get("id"));
    }

    public void testInitialize() throws IOException, JSONException, SensorProfileException {
        // clear realm again for this specific unit tests
        // (for all other tests, dataSyncer is already initialized)
        RealmConfiguration testConfig = new RealmConfiguration.Builder(getContext()).build();
        Realm.deleteRealm(testConfig);

        SensorProfiles profiles = dataSyncer.getSensorProfiles();
        assertEquals("Should contain zero profiles", 0, profiles.size());
        assertFalse("Should not contain a profile", profiles.has("noise"));

        dataSyncer.initialize();
        assertTrue("Should contain profiles", profiles.size() > 0);
        assertTrue("Should contain a profile", profiles.has("noise"));
    }

    public void testSyncLocalToRemote() throws SensorProfileException, SchemaException, JSONException, DatabaseHandlerException, SensorException, ValidationException, IOException {
        String sensorName = "noise";
        SensorOptions options = new SensorOptions();
        options.setUploadEnabled(true);
        options.setDownloadEnabled(true);
        Sensor noise = databaseHandler.createSensor(sourceName, sensorName, options);

        // create a local data point
        int value = 123;
        long time = new Date().getTime();
        noise.insertOrUpdateDataPoint(value, time);

        List<DataPoint> localPoints = noise.getDataPoints(new QueryOptions());
        assertEquals("Should contain one local data point", 1, localPoints.size());

        try {
            proxy.getSensorData(sourceName, sensorName, new QueryOptions());
            fail("Should throw an exception");
        }
        catch (HttpResponseException err) {
            assertEquals("Should throw a resource not found error", 404, err.getStatusCode());
        }

        // synchronize with remote
        dataSyncer.sync();

        // remote should now contain the data point
        JSONArray remotePoints2 = proxy.getSensorData(sourceName, sensorName, new QueryOptions());
        assertEquals("Should contain 1 remote data point", 1, remotePoints2.length());
        JSONObject point = new JSONObject();
        point.put("time", time);
        point.put("value", value);
        JSONAssert.assertEquals(point, remotePoints2.getJSONObject(0), true);
    }

    public void testSyncRemoteToLocal() throws SensorProfileException, SchemaException, JSONException, DatabaseHandlerException, SensorException, ValidationException, IOException {
        String sensorName = "noise";
        SensorOptions options = new SensorOptions();
        options.setUploadEnabled(true);
        options.setDownloadEnabled(true);
        Sensor noise = databaseHandler.createSensor(sourceName, sensorName, options);

        // create a remote data point
        int value = 123;
        long time = new Date().getTime();
        JSONObject dataPoint = new JSONObject();
        dataPoint.put("time", time);
        dataPoint.put("value", value);
        JSONArray dataPoints = new JSONArray();
        dataPoints.put(dataPoint);
        proxy.putSensorData(sourceName, sensorName, dataPoints);

        JSONArray remotePoints = proxy.getSensorData(sourceName, sensorName, new QueryOptions());
        assertEquals("Should contain 1 remote data point", 1, remotePoints.length());
        JSONAssert.assertEquals(dataPoints, remotePoints, true);

        // should initially not contain local data points
        List<DataPoint> localPoints1 = noise.getDataPoints(new QueryOptions());
        assertEquals("Should contain no local data points", 0, localPoints1.size());

        // synchronize with remote
        dataSyncer.sync();

        // should not 1 local data point
        List<DataPoint> localPoints2 = noise.getDataPoints(new QueryOptions());
        assertEquals("Should contain 1 local data point", 1, localPoints2.size());
        DataPoint dataPoint2 = localPoints2.get(0);
        assertEquals("Local data point should contain the right value", value, dataPoint2.getValueAsInteger());
        assertEquals("Local data point should contain the right time", time, dataPoint2.getTime());
    }

    public void testSyncMetaDataLocalToRemote() throws SensorProfileException, SchemaException, JSONException, DatabaseHandlerException, SensorException, ValidationException, IOException {
        // create a sensor with meta data
        String sensorName = "noise";
        JSONObject meta = new JSONObject("{\"hello\":\"world\"}");
        SensorOptions options = new SensorOptions();
        options.setMeta(meta);
        options.setUploadEnabled(true);
        options.setDownloadEnabled(true);
        Sensor noise = databaseHandler.createSensor(sourceName, sensorName, options);
        JSONAssert.assertEquals(meta, noise.getOptions().getMeta(), true);

        // synchronize with remote
        dataSyncer.sync();

        // get the sensor from remote
        JSONObject expected = new JSONObject();
        expected.put("source_name", sourceName);
        expected.put("sensor_name", sensorName);
        expected.put("meta", meta);
        JSONObject actual = proxy.getSensor(sourceName, sensorName);
        JSONAssert.assertEquals(expected, actual, true);
    }

    public void testSyncMetaDataRemoteToLocal() throws SensorProfileException, SchemaException, JSONException, DatabaseHandlerException, SensorException, ValidationException, IOException {
        // create remote sensor with meta data
        String sensorName = "noise";
        JSONObject meta = new JSONObject("{\"foo\":\"bar\"}");
        JSONObject expected2 = new JSONObject();
        expected2.put("source_name", sourceName);
        expected2.put("sensor_name", sensorName);
        expected2.put("meta", meta);
        JSONObject actual2 = proxy.updateSensor(sourceName, sensorName, meta);
        JSONAssert.assertEquals(expected2, actual2, true);

        // synchronize with remote
        dataSyncer.sync();

        // local sensor should be created and need to have the meta from remote
        Sensor noise = databaseHandler.getSensor(sourceName, sensorName);
        JSONAssert.assertEquals(meta, noise.getOptions().getMeta(), true);
    }

    public void testSyncMetaDataConflict() throws SensorProfileException, SchemaException, JSONException, DatabaseHandlerException, SensorException, ValidationException, IOException {
        // create a local sensor with meta data
        String sensorName = "noise";
        JSONObject metaLocal = new JSONObject("{\"created_by\":\"local\"}");
        SensorOptions options = new SensorOptions();
        options.setMeta(metaLocal);
        options.setUploadEnabled(true);
        options.setDownloadEnabled(true);
        Sensor noise = databaseHandler.createSensor(sourceName, sensorName, options);
        JSONAssert.assertEquals(metaLocal, noise.getOptions().getMeta(), true);

        // create remote sensor with meta data
        JSONObject metaRemote = new JSONObject("{\"created_by\":\"remote\"}");
        JSONObject expected2 = new JSONObject();
        expected2.put("source_name", sourceName);
        expected2.put("sensor_name", sensorName);
        expected2.put("meta", metaRemote);
        JSONObject actual2 = proxy.updateSensor(sourceName, sensorName, metaRemote);
        JSONAssert.assertEquals(expected2, actual2, true);

        // synchronize with remote
        dataSyncer.sync();

        // remote meta should be overridden by local
        JSONAssert.assertEquals(metaLocal, noise.getOptions().getMeta(), true);
        JSONObject actual3 = proxy.getSensor(sourceName, sensorName);
        JSONObject expected3 = new JSONObject();
        expected3.put("source_name", sourceName);
        expected3.put("sensor_name", sensorName);
        expected3.put("meta", metaLocal);
        JSONAssert.assertEquals(expected3, actual3, true);
    }


    // TODO: test syncing of duplicate data (locally updated data should override remote data)
    // TODO: test deleting data
    // TODO: test scheduler: start/stop/execute
    // TODO: test whether sync cannot run twice at the same time (lock not yet implemented!)
    // TODO: test removal of old data (older than PERSIST_PERIOD) from local database
    // TODO: test syncing for sensors with different settings for uploadEnabled (true/false)
    // TODO: test syncing for sensors with different settings for downloadEnabled (true/false)
    // TODO: test syncing for sensors with different settings for persistLocally (true/false)


}
