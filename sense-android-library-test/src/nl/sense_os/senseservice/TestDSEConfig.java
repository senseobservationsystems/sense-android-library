package nl.sense_os.senseservice;

import android.test.AndroidTestCase;

import org.json.JSONException;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import nl.sense_os.datastorageengine.DSEConfig;
import nl.sense_os.datastorageengine.DataPoint;
import nl.sense_os.datastorageengine.DataStorageEngine;
import nl.sense_os.datastorageengine.DatabaseHandlerException;
import nl.sense_os.datastorageengine.DefaultSensorOptions;
import nl.sense_os.datastorageengine.QueryOptions;
import nl.sense_os.datastorageengine.Sensor;
import nl.sense_os.datastorageengine.SensorException;
import nl.sense_os.datastorageengine.SensorOptions;
import nl.sense_os.datastorageengine.SensorProfileException;
import nl.sense_os.platform.SensePlatform;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensorData;
import nl.sense_os.util.json.SchemaException;

/**
 * Created by ted@sense-os.nl on 12/1/15.
 * Copyright (c) 2015 Sense Observation Systems BV. All rights reserved.
 */
public class TestDSEConfig extends AndroidTestCase{
    @Override
    protected void setUp() throws Exception {
        SenseServiceUtils.createAccountAndLoginService(getContext());
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        SenseServiceUtils.logout(getContext());
        super.tearDown();
    }

    public void testUpdateConfig() throws InterruptedException, ExecutionException, TimeoutException {
        // set the Sense pref
        SensePlatform sensePlatform = SenseServiceUtils.getSensePlatform(getContext());
        // set local persist period
        sensePlatform.getService().setPrefInt(SensePrefs.Main.Advanced.RETENTION_HOURS, 60);

        // Get the DSE
        DataStorageEngine dataStorageEngine = DataStorageEngine.getInstance(getContext());
        // Wait until the DSE is ready
        dataStorageEngine.onReady().get(60, TimeUnit.SECONDS);
        // get the DSE periodic sync
        DSEConfig dseConfig = dataStorageEngine.getConfig();
        // set local persist period
        sensePlatform.getService().setPrefInt(SensePrefs.Main.Advanced.RETENTION_HOURS, 1);

        // wait one second
        // Wait until the DSE is ready
        dataStorageEngine.onReady().get(60, TimeUnit.SECONDS);
        // check the difference with the previous config
        DSEConfig updatedConfig = dataStorageEngine.getConfig();
        assertFalse(dseConfig.localPersistancePeriod == updatedConfig.localPersistancePeriod);
    }

    /**
     * Tests the uploading of sensor data
     */
    public void testDataSync() throws InterruptedException, JSONException, SchemaException, DatabaseHandlerException, SensorProfileException, SensorException, TimeoutException, ExecutionException {
        // set the sensor to persist it's data locally and upload the data
        SensorOptions sensorOptions =  DefaultSensorOptions.getSensorOptions(getContext(), SensorData.SensorNames.ACCELEROMETER);
        sensorOptions.setPersistLocally(true);
        sensorOptions.setUploadEnabled(true);
        DefaultSensorOptions.setDefaultSensorOptions(getContext(), SensorData.SensorNames.ACCELEROMETER, sensorOptions);
        SensePlatform sensePlatform = SenseServiceUtils.getSensePlatform(getContext());
        // set the Sense pref upload of data every minute
        sensePlatform.getService().setPrefString(SensePrefs.Main.SYNC_RATE, SensePrefs.Main.SyncRate.OFTEN);
        DataStorageEngine dataStorageEngine = DataStorageEngine.getInstance(getContext());
        // wait until the dse is ready
        dataStorageEngine.onReady().get(60, TimeUnit.SECONDS);
        // wait 2 seconds until the DSEDataConsumer is registered
        Thread.sleep(2000);
        // send dummy accelerometer data
        SensorUtils sensorUtils = SensorUtils.getInstance();
        sensorUtils.accelerometerSensor.sendDummyData();
        // wait 1 minute
        Thread.sleep(60000);
        // check if the data has been uploaded
        Sensor sensor = dataStorageEngine.getSensor(SensorData.SourceNames.SENSE_ANDROID, SensorData.SensorNames.ACCELEROMETER);
        QueryOptions queryOptions = new QueryOptions();
        List<DataPoint> data = sensor.getDataPoints(queryOptions);
        assertTrue("No data available", data.size() > 0);
        assertTrue("Data not uploaded", data.get(0).existsInRemote());
    }
}
