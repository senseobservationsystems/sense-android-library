package nl.sense_os.datastorageengine.test;

import android.test.AndroidTestCase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import nl.sense_os.datastorageengine.AsyncCallback;
import nl.sense_os.datastorageengine.DSEConfig;
import nl.sense_os.datastorageengine.DataPoint;
import nl.sense_os.datastorageengine.DataStorageEngine;
import nl.sense_os.datastorageengine.DataSyncer;
import nl.sense_os.datastorageengine.DatabaseHandlerException;
import nl.sense_os.datastorageengine.ErrorCallback;
import nl.sense_os.datastorageengine.QueryOptions;
import nl.sense_os.datastorageengine.Sensor;
import nl.sense_os.datastorageengine.SensorDataProxy;
import nl.sense_os.datastorageengine.SensorException;
import nl.sense_os.datastorageengine.SensorOptions;
import nl.sense_os.datastorageengine.SensorProfileException;
import nl.sense_os.util.json.SchemaException;
import nl.sense_os.util.json.ValidationException;

/**
 * Created by ted on 10/29/15.
 */
public class TestDataStorageEngine extends AndroidTestCase{
    private CSUtils csUtils;
    private Map<String, String> newUser;
    SensorDataProxy.SERVER server = SensorDataProxy.SERVER.STAGING;
    private String appKey = "E9Noi5s402FYo2Gc6a7pDTe4H3UvLkWa";  // application key for dev, android, Brightr ASML
    private String sessionId;
    private DataStorageEngine dataStorageEngine;
    private String source = "sense-android";
    private String sensor_name = "noise";

    /** AsyncCallback class to receive status updates in */
    class DSEAsyncCallback implements AsyncCallback {
        String name;
        public boolean hasSuccess = false;
        public DSEAsyncCallback(String name){ this.name = name; }

        @Override
        public synchronized void onSuccess() { hasSuccess = true; this.notifyAll();}

        @Override
        public void onFailure(Throwable throwable){ fail(name + ":" + throwable.getMessage()); }
    }
    private DSEAsyncCallback onReady = new DSEAsyncCallback("onReadyAsync");
    private DSEAsyncCallback onSensorsDownloaded = new DSEAsyncCallback("onSensorsDownloadedAsync");
    private DSEAsyncCallback onSensorDataDownloaded = new DSEAsyncCallback("onSensorDataDownloadedAsync");
    private DSEAsyncCallback flushData = new DSEAsyncCallback("flushDataAsync");

    /**
     * Set up of the DataStorageEngine with checks for the status updates
     * Will create a new user account before every test function
     **/
    @Override
    protected void setUp () throws Exception {
        csUtils = new CSUtils(false);
        newUser = csUtils.createCSAccount();
        String userId = newUser.get("id");
        sessionId = csUtils.loginUser(newUser.get("username"), newUser.get("password"));
        dataStorageEngine = DataStorageEngine.getInstance(getContext());
        DSEConfig dseConfig = new DSEConfig(sessionId, userId, appKey);
        dseConfig.backendEnvironment = server;
        dseConfig.enableEncryption = true;
        dataStorageEngine.setConfig(dseConfig);
        // Wait and test onReady
        assertEquals(Boolean.TRUE, dataStorageEngine.onReady().get(60, TimeUnit.SECONDS));
    }

    /**
     * Deletes the account after every function call
     */
    @Override
    protected void tearDown () throws Exception {
        csUtils.deleteAccount(newUser.get("username"), newUser.get("password"), newUser.get("id"));
        dataStorageEngine.clearConfig();
        deleteRealm(mContext.getString(R.string.dse_encryption_key).getBytes());
    }

    public void testCallbacks() throws InterruptedException, ExecutionException, TimeoutException {
        /** asynchronous test init */
        // Test onReady
        dataStorageEngine.onReady(onReady);
        // Test onSensorsDownloaded
        dataStorageEngine.onSensorsDownloaded(onSensorsDownloaded);
        // Test onSensorDataDownloaded
        dataStorageEngine.onSensorDataDownloaded(onSensorDataDownloaded);

        /** synchronous tests */
        // Test onReady
        assertEquals(Boolean.TRUE, dataStorageEngine.onReady().get(60, TimeUnit.SECONDS));

        // Test the getStatus
        assertEquals(DataStorageEngine.DSEStatus.READY, dataStorageEngine.getStatus());

        /** asynchronous test result */
        // N.B. We have a race condition here, so wait max 60 seconds
        synchronized (onSensorDataDownloaded){
            if(!onSensorDataDownloaded.hasSuccess) {
                onSensorDataDownloaded.wait(60000);
            }
        }
        // Test onReady
        assertEquals(true, onReady.hasSuccess);
        // Test onSensorsDownloaded
        assertEquals(true, onSensorsDownloaded.hasSuccess);
        // Test onSensorDataDownloaded
        assertEquals(true, onSensorDataDownloaded.hasSuccess);
    }

    /**
     * Tests the Create, Read and Update of a Sensor and it's SensorOptions
     */
    public void testCRUSensor() throws InterruptedException, ExecutionException, TimeoutException, DatabaseHandlerException, SensorException, JSONException, SensorProfileException, SchemaException, IOException, ValidationException {
        /** CREATE */
        // check the create sensor
        Sensor sensor = dataStorageEngine.getSensor(source, sensor_name);

        /** READ */
        // check if the source is returned correctly
        assertEquals(source, dataStorageEngine.getSources().get(0));
        // check if the get sensor returns the same sensor
        assertEqualsSensor(sensor, dataStorageEngine.getSensor(source, sensor_name));
        // check if the get all sensors returns the right sensor
        assertEqualsSensor(sensor, dataStorageEngine.getSensors(source).get(0));

        /** UPDATE */
        // update the sensor options
        JSONObject meta = new JSONObject();
        meta.put("device-type", "Samsung Galaxy S6");
        SensorOptions sensorOptions = new SensorOptions(meta, true, true, true);
        sensor.setOptions(sensorOptions);

        // check if the options are update
        assertEqualsOptions(sensorOptions, sensor.getOptions());

        // check if the get sensor returns the same sensor
        assertEqualsSensor(sensor, dataStorageEngine.getSensor(source, sensor_name));

        // TODO should we be able to delete a sensor?

        // Test Flush data asynchronously
        dataStorageEngine.syncData(flushData);
        // Test Flush data synchronously
        assertEquals(Boolean.TRUE, dataStorageEngine.syncData().get(60, TimeUnit.SECONDS));
    }

    /**
     * Test the Create, Read, Update and Delete of sensor data
     */
    public void testCRUDSensorData() throws DatabaseHandlerException, SensorException, SensorProfileException, JSONException, SchemaException, ValidationException, IOException, InterruptedException, ExecutionException, TimeoutException {
        /** CREATE */
        // check the create sensor
        Sensor sensor = dataStorageEngine.getSensor(source, sensor_name);
        // create the data point
        Integer value = 10;
        long date = System.currentTimeMillis();
        long date2 = date+1;
        long date3 = date2+1;
        long date4 = date3+1;
        sensor.insertOrUpdateDataPoint(value, date);
        sensor.insertOrUpdateDataPoint(value, date2);
        sensor.insertOrUpdateDataPoint(value, date3);
        sensor.insertOrUpdateDataPoint(value, date4);

        /** READ */
        QueryOptions queryOptions = new QueryOptions(date, date4, null, 2, QueryOptions.SORT_ORDER.DESC);
        List<DataPoint> dataPoints = sensor.getDataPoints(queryOptions);
        // check the limit, and the date range
        assertEquals(2, dataPoints.size());
        // check the ordering and value
        assertEquals(value, dataPoints.get(0).getValue());
        assertEquals(date3, dataPoints.get(0).getTime());
        assertEquals(date2, dataPoints.get(1).getTime());

        /** UPDATE */
        // insert the updated value
        Integer updatedValue = 20;
        sensor.insertOrUpdateDataPoint(updatedValue, date4);
        // get the updated value
        queryOptions = new QueryOptions(date4, null, null, 1, QueryOptions.SORT_ORDER.ASC);
        assertEquals(updatedValue, sensor.getDataPoints(queryOptions).get(0).getValue());

        /** DELETE */
        // delete the last 2 data points, only the date is used with delete
        sensor.deleteDataPoints(date3, date4+1);
        // get the first rest with the same query, should return the first 2 in reversed order
        queryOptions = new QueryOptions(date, date4+1, null, null, QueryOptions.SORT_ORDER.DESC);
        dataPoints = sensor.getDataPoints(queryOptions);
        assertEquals(date2, dataPoints.get(0).getTime());
        assertEquals(date, dataPoints.get(1).getTime());

        // Test Flush data
        assertEquals(Boolean.TRUE, dataStorageEngine.syncData().get(60, TimeUnit.SECONDS));
    }

    /**
     * Test for an error when providing wrong credentials
     */
    public void testOnErrorCallback() throws SensorProfileException, SchemaException, SensorException, DatabaseHandlerException, JSONException, ValidationException, InterruptedException, IOException {
        DSEConfig dseConfig = dataStorageEngine.getConfig();
        try {
            final List<Throwable> receivedErrors = new ArrayList<>();

            // register the on error callback
            dataStorageEngine.registerOnError(new ErrorCallback() {
                @Override
                public void onError(Throwable err) {
                    receivedErrors.add(err);
                }
            });

            DSEConfig badConfig = new DSEConfig("invalid_session_id", "1", "1");
            dataStorageEngine.setConfig(badConfig);

            // wait until we're sure the sync action has taken place
            Thread.sleep(1000);

            // now sync should have failed because of an invalid session id
            assertTrue("Should have thrown an exception", receivedErrors.size() > 0);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // restore settings
            dataStorageEngine.setConfig(dseConfig);
        }
    }

    /**
     * Test whether set and getConfig store and retrieve the configuration correctly
     */
    public void testConfig(){
        DSEConfig oldDseConfig = dataStorageEngine.getConfig();
        DSEConfig newConfig =new DSEConfig("session", "1", "1");
        newConfig.uploadInterval = -1l;
        newConfig.localPersistancePeriod = -1l;
        newConfig.backendEnvironment = SensorDataProxy.SERVER.STAGING;
        newConfig.enableEncryption = true;
        dataStorageEngine.setConfig(newConfig);
        DSEConfig updatedConfig = dataStorageEngine.getConfig();
        assertTrue("The new config should be different then the previous", !oldDseConfig.equals(updatedConfig));
        assertTrue("The new config should be equal to the set DSEConfig object changed", newConfig.equals(updatedConfig));
    }

    /**
     * Test periodic syncing of data
     */
    public void testPeriodicSync () throws SensorProfileException, SchemaException, SensorException, DatabaseHandlerException, JSONException, ValidationException, InterruptedException, IOException, TimeoutException, ExecutionException {

        Long syncRate = 60 * 1000l;        // 60 seconds, AlarmManager doesn't allow shorter intervals.
        Long maxSyncDuration = 5 * 1000l;  // we assume the syncing will never take longer than 5 seconds in this unit test
        DSEConfig dseConfig = dataStorageEngine.getConfig();
        Long originalSyncRate = dseConfig.uploadInterval;
        try {
            SensorOptions sensorOptions = new SensorOptions();
            sensorOptions.setUploadEnabled(true);
            sensorOptions.setDownloadEnabled(true);
            Sensor noise = dataStorageEngine.getSensor(source, sensor_name);
            noise.setOptions(sensorOptions);

            long value1 = 1;
            long time1 = new Date().getTime();
            noise.insertOrUpdateDataPoint(value1, time1);

            List<DataPoint> dataPoints = noise.getDataPoints(new QueryOptions());
            assertEquals("Should contain 1 data points", 1, dataPoints.size());
            DataPoint point = dataPoints.get(0);
            assertFalse("DataPoint should not yet exist in remote", point.existsInRemote());

            // change the sync rate
            dseConfig.uploadInterval = syncRate;
            dataStorageEngine.setConfig(dseConfig);
            // wait until the first sync has taken place
            Thread.sleep(syncRate + maxSyncDuration);

            // by now, the data point should have been synced
            List<DataPoint> dataPoints2 = noise.getDataPoints(new QueryOptions());
            assertTrue("DataPoint should exist in remote", dataPoints2.get(0).existsInRemote());
            Sensor sensor = dataStorageEngine.getSensor(source, sensor_name);
            List<DataPoint> array = sensor.getDataPoints(new QueryOptions());
            assertEquals("Remote should contain 1 data point", 1, array.size());

            // TODO do we need a disable sync?
            // TODO do we need to have an option to only upload on wifi?

            // create a second data point
            long value2 = 2;
            long time2 = time1 + 1;
            noise.insertOrUpdateDataPoint(value2, time2);

            List<DataPoint> dataPoints3 = noise.getDataPoints(new QueryOptions());
            assertEquals("Should contain 2 data points", 2, dataPoints3.size());
            assertEquals("DataPoint 1 should have the right value", value1, dataPoints3.get(0).getValueAsInteger());
            assertEquals("DataPoint 2 should have the right value", value2, dataPoints3.get(1).getValueAsInteger());
            assertTrue("DataPoint 1 should exist in remote", dataPoints3.get(0).existsInRemote());
            assertFalse("DataPoint 2 should NOT exist in remote", dataPoints3.get(1).existsInRemote());

            // sync should not have been executed
            List<DataPoint> dataPoints4 = noise.getDataPoints(new QueryOptions());
            assertEquals("Should contain 2 data points", 2, dataPoints4.size());
            assertEquals("DataPoint 1 should have the right value", value1, dataPoints4.get(0).getValueAsInteger());
            assertEquals("DataPoint 2 should have the right value", value2, dataPoints4.get(1).getValueAsInteger());
            assertTrue("DataPoint 1 should exist in remote", dataPoints4.get(0).existsInRemote());
            assertFalse("DataPoint 2 should NOT exist in remote", dataPoints4.get(1).existsInRemote());
        }
        finally {
            // restore settings
            dseConfig.uploadInterval = originalSyncRate;
            dataStorageEngine.setConfig(dseConfig);
        }
    }

    public void testEncryption() throws ValidationException, JSONException, SensorProfileException, SchemaException, DatabaseHandlerException, SensorException, InterruptedException, ExecutionException, TimeoutException {
        // create the sensor
        Sensor sensor = dataStorageEngine.getSensor(source, sensor_name);
        // create the data point
        Integer value = 10;
        long date = System.currentTimeMillis();
        sensor.insertOrUpdateDataPoint(value, date);
        // check if it exists
        QueryOptions queryOptions = new QueryOptions();
        List<DataPoint> dataPoints = sensor.getDataPoints(queryOptions);
        assertEquals(1, dataPoints.size());

        // get the current configuration
        DSEConfig dseConfig = dataStorageEngine.getConfig();
        // disable encryption
        dseConfig.enableEncryption = false;
        dataStorageEngine.setConfig(dseConfig);
        // wait until the DSE is ready
        assertEquals(Boolean.TRUE, dataStorageEngine.onReady().get(60, TimeUnit.SECONDS));
        // check again if we can get the data
        try {
            sensor = dataStorageEngine.getSensor(source, sensor_name);
            dataPoints = sensor.getDataPoints(queryOptions);
            assertEquals(0, dataPoints.size());
            fail("Should have thrown an exception");
        }catch(IllegalArgumentException e){
            // the realm data base should not be available
            assertEquals("Illegal Argument: Invalid format of Realm file.", e.getMessage());
        }
    }

    /** Helper function for comparing sensors */
    public void assertEqualsSensor(Sensor left, Sensor right)
    {
        assertEquals(left.getId(), right.getId());
        assertEquals(left.getName(), right.getName());
        assertEquals(left.getSource(), right.getSource());
        assertEquals(left.getUserId(), right.getUserId());
        assertEqualsOptions(left.getOptions(), right.getOptions());
    }

    /** Helper function for comparing sensor options */
    public void assertEqualsOptions(SensorOptions left, SensorOptions right)
    {
        // for null values the default is selected
        if(left.isDownloadEnabled() != null && (right.isDownloadEnabled() != null)) {
            assertEquals(left.isDownloadEnabled(), right.isDownloadEnabled());
        }
        if(left.isPersistLocally() != null && (right.isPersistLocally() != null)) {
            assertEquals(left.isPersistLocally(), right.isPersistLocally());
        }
        if(left.isUploadEnabled() != null && (right.isUploadEnabled() != null)) {
            assertEquals(left.isUploadEnabled(), right.isUploadEnabled());
        }
        if(left.getMeta() != null && (right.getMeta() != null)) {
            assertEquals(left.getMeta().toString(), right.getMeta().toString());
        }
    }

    /**
     * Helper function to delete realm
     * @param encryptionKey
     */
    void deleteRealm (byte[] encryptionKey) {
        Realm.removeDefaultConfiguration();
        File realmFile = new File(getContext().getFilesDir(), "default.realm");

        RealmConfiguration config = new RealmConfiguration.Builder(getContext()).encryptionKey(encryptionKey).build();
        Realm.deleteRealm(config);

        RealmConfiguration config2 = new RealmConfiguration.Builder(getContext()).build();
        Realm.deleteRealm(config2);
    }
}
