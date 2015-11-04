package nl.sense_os.datastorageengine.test;

import android.test.AndroidTestCase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import nl.sense_os.datastorageengine.DataPoint;
import nl.sense_os.datastorageengine.DataStorageEngine;
import nl.sense_os.datastorageengine.DatabaseHandlerException;
import nl.sense_os.datastorageengine.QueryOptions;
import nl.sense_os.datastorageengine.Sensor;
import nl.sense_os.datastorageengine.SensorDataProxy;
import nl.sense_os.datastorageengine.SensorException;
import nl.sense_os.datastorageengine.SensorOptions;
import nl.sense_os.datastorageengine.SensorProfileException;

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
    private String source = "DSE-Test";
    private String sensor_name = "noise";


    @Override
    protected void setUp () throws Exception {
        csUtils = new CSUtils(false);
        newUser = csUtils.createCSAccount();
        String userId = newUser.get("id");
        sessionId = csUtils.loginUser(newUser.get("username"), newUser.get("password"));
        dataStorageEngine = DataStorageEngine.getInstance(getContext());
        dataStorageEngine.setCredentials(sessionId, userId, appKey);

        // Test onReady
        assertEquals(Boolean.TRUE, dataStorageEngine.onReady().get(60, TimeUnit.SECONDS));

        // Test onSensorsDownloaded
        assertEquals(Boolean.TRUE, dataStorageEngine.onSensorsDownloaded().get(60, TimeUnit.SECONDS));

        // Test onSensorDataDownloaded
        assertEquals(Boolean.TRUE, dataStorageEngine.onSensorDataDownloaded().get(60, TimeUnit.SECONDS));

        // Test the getStatus
        assertEquals(DataStorageEngine.DSEStatus.READY, dataStorageEngine.getStatus());
    }

    @Override
    protected void tearDown () throws Exception {
        // Test Flush data
        dataStorageEngine.flushData();

        csUtils.deleteAccount(newUser.get("username"), newUser.get("password"), newUser.get("id"));
    }

    public void testCRUSensor() throws InterruptedException, ExecutionException, TimeoutException, DatabaseHandlerException, SensorException, JSONException {
        /** CREATE */
        // check the create sensor
        Sensor sensor = dataStorageEngine.createSensor(source, sensor_name, new SensorOptions());

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
    }

    public void testCRUDSensorData() throws DatabaseHandlerException, SensorException, SensorProfileException, JSONException {
        /** CREATE */
        // check the create sensor
        Sensor sensor = dataStorageEngine.createSensor(source, sensor_name, new SensorOptions());
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
        QueryOptions queryOptions = new QueryOptions(date, date3, null, 2, QueryOptions.SORT_ORDER.DESC);
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
        queryOptions = new QueryOptions(date4, date4, null, 2, QueryOptions.SORT_ORDER.ASC);
        assertEquals(updatedValue, sensor.getDataPoints(queryOptions).get(0).getValue());

        /** DELETE */
        // delete the last 2 data points
        queryOptions = new QueryOptions(date, date4, null, 2, QueryOptions.SORT_ORDER.DESC);
        sensor.deleteDataPoints(queryOptions);
        // get the first rest with the same query, should return the first 2 in reversed order
        dataPoints = sensor.getDataPoints(queryOptions);
        assertEquals(date2, dataPoints.get(0).getTime());
        assertEquals(date, dataPoints.get(1).getTime());
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
        assertEquals(left.isDownloadEnabled(), right.isDownloadEnabled());
        assertEquals(left.isPersistLocally(), right.isPersistLocally());
        assertEquals(left.isUploadEnabled(), right.isUploadEnabled());
        assertEquals(left.getMeta().toString(), right.getMeta().toString());
    }

}
