package nl.sense_os.datastorageengine.test;

import android.test.AndroidTestCase;

import org.apache.http.client.HttpResponseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    private CSUtils mCsUtils;
    private Map<String, String> mNewUser;
    SensorDataProxy.SERVER mServer = SensorDataProxy.SERVER.STAGING;
    private String mSourceName = "sense-android";
    private String mAppKey = "E9Noi5s402FYo2Gc6a7pDTe4H3UvLkWa";  // application key for dev, android, Brightr ASML
    private String mSessionId;
    private DatabaseHandler mDatabaseHandler;
    private SensorDataProxy mProxy;
    private DataSyncer mDataSyncer;
    private SensorProfiles mSensorProfiles;

    @Override
    protected void setUp () throws Exception {
        // remove old mRealm data
        RealmConfiguration testConfig = new RealmConfiguration.Builder(getContext()).build();
        Realm.deleteRealm(testConfig);

        mCsUtils = new CSUtils(false);
        mNewUser = mCsUtils.createCSAccount();
        String userId = mNewUser.get("id");
        mSessionId = mCsUtils.loginUser(mNewUser.get("username"), mNewUser.get("password"));
        mDatabaseHandler = new DatabaseHandler(getContext(), userId);
        mProxy = new SensorDataProxy(mServer, mAppKey, mSessionId);
        mSensorProfiles = new SensorProfiles(getContext());
        mDataSyncer = new DataSyncer(getContext(), mDatabaseHandler, mProxy);
        mDataSyncer.initialize();
    }

    @Override
    protected void tearDown () throws Exception {
        mCsUtils.deleteAccount(mNewUser.get("username"), mNewUser.get("password"), mNewUser.get("id"));
    }

    public void testInitialize() throws IOException, JSONException, SensorProfileException {
        // clear mRealm again for this specific unit tests
        // (for all other tests, dataSyncer is already initialized)
        RealmConfiguration testConfig = new RealmConfiguration.Builder(getContext()).build();
        Realm.deleteRealm(testConfig);

        assertEquals("Should contain zero profiles", 0, mSensorProfiles.size());
        assertFalse("Should not contain a profile", mSensorProfiles.has("noise"));

        mDataSyncer.initialize();
        assertTrue("Should contain profiles", mSensorProfiles.size() > 0);
        assertTrue("Should contain a profile", mSensorProfiles.has("noise"));
    }

    public void testLocalToRemote() throws SensorProfileException, SchemaException, JSONException, DatabaseHandlerException, SensorException, ValidationException, IOException {
        String sensorName = "noise";
        SensorOptions options = new SensorOptions();
        options.setUploadEnabled(true);
        options.setDownloadEnabled(true);
        Sensor noise = mDatabaseHandler.createSensor(mSourceName, sensorName, options);

        // create a local data point
        int value = 123;
        long time = new Date().getTime();
        noise.insertOrUpdateDataPoint(value, time);

        List<DataPoint> localPoints = noise.getDataPoints(new QueryOptions());
        assertEquals("Should contain one local data point", 1, localPoints.size());

        try {
            mProxy.getSensorData(mSourceName, sensorName, new QueryOptions());
            fail("Should throw an exception");
        }
        catch (HttpResponseException err) {
            assertEquals("Should throw a resource not found error", 404, err.getStatusCode());
        }

        // synchronize with remote
        mDataSyncer.sync();

        // remote should now contain the data point
        JSONArray remotePoints2 = mProxy.getSensorData(mSourceName, sensorName, new QueryOptions());
        assertEquals("Should contain 1 remote data point", 1, remotePoints2.length());
        JSONObject point = new JSONObject();
        point.put("time", time);
        point.put("value", value);
        JSONAssert.assertEquals(point, remotePoints2.getJSONObject(0), true);
    }

    public void testRemoteToLocal() throws SensorProfileException, SchemaException, JSONException, DatabaseHandlerException, SensorException, ValidationException, IOException {
        String sensorName = "noise";
        SensorOptions options = new SensorOptions();
        options.setUploadEnabled(true);
        options.setDownloadEnabled(true);
        Sensor noise = mDatabaseHandler.createSensor(mSourceName, sensorName, options);

        // create a remote data point
        int value = 123;
        long time = new Date().getTime();
        JSONObject dataPoint = new JSONObject();
        dataPoint.put("time", time);
        dataPoint.put("value", value);
        JSONArray dataPoints = new JSONArray();
        dataPoints.put(dataPoint);
        mProxy.putSensorData(mSourceName, sensorName, dataPoints);

        JSONArray remotePoints = mProxy.getSensorData(mSourceName, sensorName, new QueryOptions());
        assertEquals("Should contain 1 remote data point", 1, remotePoints.length());
        JSONAssert.assertEquals(dataPoints, remotePoints, true);

        // should initially not contain local data points
        List<DataPoint> localPoints1 = noise.getDataPoints(new QueryOptions());
        assertEquals("Should contain no local data points", 0, localPoints1.size());

        // synchronize with remote
        mDataSyncer.sync();

        // should not 1 local data point
        List<DataPoint> localPoints2 = noise.getDataPoints(new QueryOptions());
        assertEquals("Should contain 1 local data point", 1, localPoints2.size());
        DataPoint dataPoint2 = localPoints2.get(0);
        assertEquals("Local data point should contain the right value", value, dataPoint2.getValueAsInteger());
        assertEquals("Local data point should contain the right time", time, dataPoint2.getTime());
    }

    public void testMetaDataLocalToRemote() throws SensorProfileException, SchemaException, JSONException, DatabaseHandlerException, SensorException, ValidationException, IOException {
        // create a sensor with meta data
        String sensorName = "noise";
        JSONObject meta = new JSONObject("{\"hello\":\"world\"}");
        SensorOptions options = new SensorOptions();
        options.setMeta(meta);
        options.setUploadEnabled(true);
        options.setDownloadEnabled(true);
        Sensor noise = mDatabaseHandler.createSensor(mSourceName, sensorName, options);
        JSONAssert.assertEquals(meta, noise.getOptions().getMeta(), true);

        // synchronize with remote
        mDataSyncer.sync();

        // get the sensor from remote
        JSONObject expected = new JSONObject();
        expected.put("source_name", mSourceName);
        expected.put("sensor_name", sensorName);
        expected.put("meta", meta);
        JSONObject actual = mProxy.getSensor(mSourceName, sensorName);
        JSONAssert.assertEquals(expected, actual, true);
    }

    public void testMetaDataRemoteToLocal() throws SensorProfileException, SchemaException, JSONException, DatabaseHandlerException, SensorException, ValidationException, IOException {
        // create remote sensor with meta data
        String sensorName = "noise";
        JSONObject meta = new JSONObject("{\"foo\":\"bar\"}");
        JSONObject expected2 = new JSONObject();
        expected2.put("source_name", mSourceName);
        expected2.put("sensor_name", sensorName);
        expected2.put("meta", meta);
        JSONObject actual2 = mProxy.updateSensor(mSourceName, sensorName, meta);
        JSONAssert.assertEquals(expected2, actual2, true);

        // synchronize with remote
        mDataSyncer.sync();

        // local sensor should be created and need to have the meta from remote
        Sensor noise = mDatabaseHandler.getSensor(mSourceName, sensorName);
        JSONAssert.assertEquals(meta, noise.getOptions().getMeta(), true);
    }

    public void testMetaDataConflict() throws SensorProfileException, SchemaException, JSONException, DatabaseHandlerException, SensorException, ValidationException, IOException {
        // create a local sensor with meta data
        String sensorName = "noise";
        JSONObject metaLocal = new JSONObject("{\"created_by\":\"local\"}");
        SensorOptions options = new SensorOptions();
        options.setMeta(metaLocal);
        options.setUploadEnabled(true);
        options.setDownloadEnabled(true);
        Sensor noise = mDatabaseHandler.createSensor(mSourceName, sensorName, options);
        JSONAssert.assertEquals(metaLocal, noise.getOptions().getMeta(), true);

        // create remote sensor with meta data
        JSONObject metaRemote = new JSONObject("{\"created_by\":\"remote\"}");
        JSONObject expected2 = new JSONObject();
        expected2.put("source_name", mSourceName);
        expected2.put("sensor_name", sensorName);
        expected2.put("meta", metaRemote);
        JSONObject actual2 = mProxy.updateSensor(mSourceName, sensorName, metaRemote);
        JSONAssert.assertEquals(expected2, actual2, true);

        // synchronize with remote
        mDataSyncer.sync();

        // remote meta should be overridden by local
        JSONAssert.assertEquals(metaLocal, noise.getOptions().getMeta(), true);
        JSONObject actual3 = mProxy.getSensor(mSourceName, sensorName);
        JSONObject expected3 = new JSONObject();
        expected3.put("source_name", mSourceName);
        expected3.put("sensor_name", sensorName);
        expected3.put("meta", metaLocal);
        JSONAssert.assertEquals(expected3, actual3, true);
    }

    public void testDataConflicts() throws SensorProfileException, SchemaException, JSONException, DatabaseHandlerException, SensorException, ValidationException, IOException {
        long time = new Date().getTime();

        // create a local sensor with a data point
        String sensorName = "noise";
        SensorOptions options = new SensorOptions();
        options.setUploadEnabled(true);
        options.setDownloadEnabled(true);
        Sensor noise = mDatabaseHandler.createSensor(mSourceName, sensorName, options);
        noise.insertOrUpdateDataPoint(12, time);

        // create remote sensor with a different data point
        JSONObject dataPoint = new JSONObject();
        dataPoint.put("time", time);
        dataPoint.put("value", 42);
        JSONArray data = new JSONArray();
        data.put(dataPoint);
        mProxy.putSensorData(mSourceName, sensorName, data);

        // synchronize with remote
        mDataSyncer.sync();

        // local should still have the value 12
        List<DataPoint> actualLocal = noise.getDataPoints(new QueryOptions());
        assertEquals("should contain 1 data point", 1, actualLocal.size());
        assertEquals("should have the right time", time, actualLocal.get(0).getTime());
        assertEquals("should have the right value", 12, actualLocal.get(0).getValueAsInteger());

        // remote value should now be overridden with the local value 12
        JSONArray actualRemote = mProxy.getSensorData(mSourceName, sensorName, new QueryOptions());
        JSONObject remoteDataPoint = new JSONObject();
        remoteDataPoint.put("time", time);
        remoteDataPoint.put("value", 12);
        JSONArray expectedRemote = new JSONArray();
        expectedRemote.put(remoteDataPoint);
        JSONAssert.assertEquals(expectedRemote, actualRemote, true);
    }

    public void testDataUploadOnly() throws SensorProfileException, SchemaException, JSONException, DatabaseHandlerException, SensorException, ValidationException, IOException {
        long time1 = new Date().getTime();
        long time2 = new Date().getTime() + 1;

        // create a local sensor with a data point
        String sensorName = "noise";
        SensorOptions options = new SensorOptions();
        options.setUploadEnabled(true);
        options.setDownloadEnabled(false);
        Sensor noise = mDatabaseHandler.createSensor(mSourceName, sensorName, options);
        noise.insertOrUpdateDataPoint(12, time1);

        // create remote sensor with a different data point at a different time
        JSONObject dataPoint = new JSONObject();
        dataPoint.put("time", time2);
        dataPoint.put("value", 42);
        JSONArray data = new JSONArray();
        data.put(dataPoint);
        mProxy.putSensorData(mSourceName, sensorName, data);

        // synchronize with remote
        mDataSyncer.sync();

        // local should still have the value 12
        List<DataPoint> actualLocal = noise.getDataPoints(new QueryOptions());
        assertEquals("should contain 1 data point", 1, actualLocal.size());
        assertEquals("should have the right time", time1, actualLocal.get(0).getTime());
        assertEquals("should have the right value", 12, actualLocal.get(0).getValueAsInteger());

        // remote should now have both data points
        QueryOptions queryOptions = new QueryOptions();
        queryOptions.setSortOrder(QueryOptions.SORT_ORDER.ASC);
        JSONArray actualRemote = mProxy.getSensorData(mSourceName, sensorName, queryOptions);
        JSONArray expectedRemote = new JSONArray();
        JSONObject p1 = new JSONObject()
            .put("time", time1)
            .put("value", 12);
        expectedRemote.put(p1);
        JSONObject p2 = new JSONObject()
            .put("time", time2)
            .put("value", 42);
        expectedRemote.put(p2);
        JSONAssert.assertEquals(expectedRemote, actualRemote, true);
    }

    public void testDataDownloadOnly() throws SensorProfileException, SchemaException, JSONException, DatabaseHandlerException, SensorException, ValidationException, IOException {
        long time1 = new Date().getTime();
        long time2 = new Date().getTime() + 1;

        // create a local sensor with a data point
        String sensorName = "noise";
        SensorOptions options = new SensorOptions();
        options.setUploadEnabled(false);
        options.setDownloadEnabled(true);
        Sensor noise = mDatabaseHandler.createSensor(mSourceName, sensorName, options);
        noise.insertOrUpdateDataPoint(12, time1);

        // create remote sensor with a different data point at a different time
        JSONObject dataPoint = new JSONObject();
        dataPoint.put("time", time2);
        dataPoint.put("value", 42);
        JSONArray data = new JSONArray();
        data.put(dataPoint);
        mProxy.putSensorData(mSourceName, sensorName, data);

        // synchronize with remote
        mDataSyncer.sync();

        // local should have both values
        List<DataPoint> actualLocal = noise.getDataPoints(new QueryOptions());
        assertEquals("should contain 2 data points", 2, actualLocal.size());
        assertEquals("should have the right time", time1, actualLocal.get(0).getTime());
        assertEquals("should have the right value", 12, actualLocal.get(0).getValueAsInteger());
        assertEquals("should have the right time", time2, actualLocal.get(1).getTime());
        assertEquals("should have the right value", 42, actualLocal.get(1).getValueAsInteger());

        // remote value should still have one data point
        JSONArray actualRemote = mProxy.getSensorData(mSourceName, sensorName, new QueryOptions());
        JSONArray expectedRemote = data;
        JSONAssert.assertEquals(expectedRemote, actualRemote, true);
    }

    public void testNoUploadNoDownload() throws SensorProfileException, SchemaException, JSONException, DatabaseHandlerException, SensorException, ValidationException, IOException {
        long time1 = new Date().getTime();
        long time2 = new Date().getTime() + 1;

        // create a local sensor with a data point
        String sensorName = "noise";
        SensorOptions options = new SensorOptions();
        options.setUploadEnabled(false);
        options.setDownloadEnabled(false);
        Sensor noise = mDatabaseHandler.createSensor(mSourceName, sensorName, options);
        noise.insertOrUpdateDataPoint(12, time1);

        // create remote sensor with a different data point at a different time
        JSONObject dataPoint = new JSONObject();
        dataPoint.put("time", time2);
        dataPoint.put("value", 42);
        JSONArray data = new JSONArray();
        data.put(dataPoint);
        mProxy.putSensorData(mSourceName, sensorName, data);

        // synchronize with remote
        mDataSyncer.sync();

        // local should still have the value 12
        List<DataPoint> actualLocal = noise.getDataPoints(new QueryOptions());
        assertEquals("should contain 1 data point", 1, actualLocal.size());
        assertEquals("should have the right time", time1, actualLocal.get(0).getTime());
        assertEquals("should have the right value", 12, actualLocal.get(0).getValueAsInteger());

        // remote should still have the value 42
        JSONArray actualRemote = mProxy.getSensorData(mSourceName, sensorName, new QueryOptions());
        JSONArray expectedRemote = data;
        JSONAssert.assertEquals(expectedRemote, actualRemote, true);
    }

    public void testNoPersistLocally() throws SensorProfileException, SchemaException, JSONException, DatabaseHandlerException, SensorException, ValidationException, IOException {
        long time1 = new Date().getTime();
        long time2 = new Date().getTime() + 1;

        // create a local sensor with a data point
        String sensorName = "noise";
        SensorOptions options = new SensorOptions();
        options.setUploadEnabled(true);
        options.setDownloadEnabled(true); // should be ignored when persistLocally is false
        options.setPersistLocally(false);
        Sensor noise = mDatabaseHandler.createSensor(mSourceName, sensorName, options);
        noise.insertOrUpdateDataPoint(12, time1);

        // create remote sensor with a different data point at a different time
        JSONObject dataPoint = new JSONObject();
        dataPoint.put("time", time2);
        dataPoint.put("value", 42);
        JSONArray data = new JSONArray();
        data.put(dataPoint);
        mProxy.putSensorData(mSourceName, sensorName, data);

        // synchronize with remote
        mDataSyncer.sync();

        // local should have no data left
        List<DataPoint> actualLocal = noise.getDataPoints(new QueryOptions());
        assertEquals("should contain zero data points", 0, actualLocal.size());

        // remote should now have both data points
        QueryOptions queryOptions = new QueryOptions();
        queryOptions.setSortOrder(QueryOptions.SORT_ORDER.ASC);
        JSONArray actualRemote = mProxy.getSensorData(mSourceName, sensorName, queryOptions);
        JSONArray expectedRemote = new JSONArray();
        JSONObject p1 = new JSONObject()
                .put("time", time1)
                .put("value", 12);
        expectedRemote.put(p1);
        JSONObject p2 = new JSONObject()
                .put("time", time2)
                .put("value", 42);
        expectedRemote.put(p2);
        JSONAssert.assertEquals(expectedRemote, actualRemote, true);
    }

    public void testCleanupOldData() throws SensorProfileException, SchemaException, JSONException, DatabaseHandlerException, SensorException, ValidationException, IOException {
        final long DAY = 1000 * 60 * 60 * 24; // in milliseconds

        // change the persistence period to 2 days
        DataSyncer dataSyncer = new DataSyncer(getContext(), mDatabaseHandler, mProxy);
        dataSyncer.setPersistPeriod(2 * DAY);

        long time1 = new Date().getTime() - 4 * DAY;
        long time2 = new Date().getTime() - DAY;
        int value1 = 12;
        int value2 = 23;

        // create a local sensor with a data point
        String sensorName = "noise";
        SensorOptions options = new SensorOptions();
        options.setUploadEnabled(true);
        options.setDownloadEnabled(false);
        Sensor noise = mDatabaseHandler.createSensor(mSourceName, sensorName, options);
        noise.insertOrUpdateDataPoint(value1, time1);
        noise.insertOrUpdateDataPoint(value2, time2);

        // synchronize (should remove old data
        dataSyncer.sync();

        // local should have one data point left
        List<DataPoint> actualLocal = noise.getDataPoints(new QueryOptions());
        assertEquals("should contain zero data points", 1, actualLocal.size());
        assertEquals("should have the right time", time2, actualLocal.get(0).getTime());
        assertEquals("should have the right value", value2, actualLocal.get(0).getValueAsInteger());

        // remote should now have both data points
        QueryOptions queryOptions = new QueryOptions();
        queryOptions.setSortOrder(QueryOptions.SORT_ORDER.ASC);
        JSONArray actualRemote = mProxy.getSensorData(mSourceName, sensorName, queryOptions);
        JSONArray expectedRemote = new JSONArray();
        JSONObject p1 = new JSONObject()
                .put("time", time1)
                .put("value", value1);
        expectedRemote.put(p1);
        JSONObject p2 = new JSONObject()
                .put("time", time2)
                .put("value", value2);
        expectedRemote.put(p2);
        JSONAssert.assertEquals(expectedRemote, actualRemote, true);
    }

    public void testProgressCallback() throws SensorProfileException, SchemaException, JSONException, DatabaseHandlerException, SensorException, ValidationException, IOException {
        final JSONArray progress = new JSONArray();

        mDataSyncer.sync(new DataSyncer.ProgressCallback() {
            @Override
            public void onDeletionCompleted() {
                progress.put("onDeletionCompleted");
            }

            @Override
            public void onUploadCompeted() {
                progress.put("onUploadCompeted");
            }

            @Override
            public void onDownloadSensorsCompleted() {
                progress.put("onDownloadSensorsCompleted");
            }

            @Override
            public void onDownloadSensorDataCompleted() {
                progress.put("onDownloadSensorDataCompleted");
            }

            @Override
            public void onCleanupCompleted() {
                progress.put("onCleanupCompleted");
            }
        });

        JSONArray expected = new JSONArray()
                .put("onDeletionCompleted")
                .put("onUploadCompeted")
                .put("onDownloadSensorsCompleted")
                .put("onDownloadSensorDataCompleted")
                .put("onCleanupCompleted");
        JSONAssert.assertEquals(expected, progress, true);
    }

    public void testPeriodicSync () throws SensorProfileException, SchemaException, SensorException, DatabaseHandlerException, JSONException, ValidationException, InterruptedException, IOException {
        long originalSyncRate = mDataSyncer.getSyncRate();
        long syncRate = 1000 * 5; // 5 seconds

        try {
            mDataSyncer.setSyncRate(syncRate);
            mDataSyncer.enablePeriodicSync();

            String sensorName = "noise";
            Sensor noise = mDatabaseHandler.createSensor(mSourceName, sensorName, new SensorOptions());

            long value1 = 1;
            long time1 = new Date().getTime();
            noise.insertOrUpdateDataPoint(value1, time1);

            List<DataPoint> dataPoints = noise.getDataPoints(new QueryOptions());
            assertEquals("Should contain 1 data points", 1, dataPoints.size());
            DataPoint point = dataPoints.get(0);
            assertFalse("DataPoint should not yet exist in remote", point.existsInRemote());

            Thread.sleep(syncRate * 2); // wait for twice the sync rate, syncing should be done by then

            // by now, the data point should have been synced
            List<DataPoint> dataPoints2 = noise.getDataPoints(new QueryOptions());
            assertTrue("DataPoint should exist in remote", dataPoints2.get(0).existsInRemote());
            JSONArray array = mProxy.getSensorData(mSourceName, sensorName, new QueryOptions());
            assertEquals("Remote should contain 1 data point", 1, array.length());

            // stop syncing
            mDataSyncer.disablePeriodicSync();

            Thread.sleep(syncRate); // wait another the sync rate period, be sure there is no sync in progress

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

            Thread.sleep(syncRate * 2); // wait for twice the sync rate

            // sync should not have been executed (it is stopped)
            List<DataPoint> dataPoints4 = noise.getDataPoints(new QueryOptions());
            assertEquals("Should contain 2 data points", 2, dataPoints4.size());
            assertEquals("DataPoint 1 should have the right value", value1, dataPoints4.get(0).getValueAsInteger());
            assertEquals("DataPoint 2 should have the right value", value2, dataPoints4.get(1).getValueAsInteger());
            assertTrue("DataPoint 1 should exist in remote", dataPoints4.get(0).existsInRemote());
            assertFalse("DataPoint 2 should NOT exist in remote", dataPoints4.get(1).existsInRemote());
        }
        finally {
            // restore settings
            mDataSyncer.disablePeriodicSync();
            mDataSyncer.setSyncRate(originalSyncRate);
        }
    }


    // TODO: test the four cases for cleaning up old data (currently there's only one)

    // TODO: test scheduler: start/stop/execute
    // TODO: test whether sync cannot run twice at the same time (lock not yet implemented!)
    // TODO: test deleting data
    // TODO: test setSyncRate

    // TODO: implement and test DataSyncer.onError

}
