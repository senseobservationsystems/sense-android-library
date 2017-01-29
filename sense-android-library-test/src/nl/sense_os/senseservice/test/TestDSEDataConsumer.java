package nl.sense_os.senseservice.test;

import android.test.AndroidTestCase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import nl.sense_os.datastorageengine.DSEConfig;
import nl.sense_os.datastorageengine.DataStorageEngine;
import nl.sense_os.datastorageengine.DatabaseHandlerException;
import nl.sense_os.datastorageengine.DefaultSensorOptions;
import nl.sense_os.datastorageengine.QueryOptions;
import nl.sense_os.datastorageengine.Sensor;
import nl.sense_os.datastorageengine.SensorDataProxy;
import nl.sense_os.datastorageengine.SensorException;
import nl.sense_os.datastorageengine.SensorOptions;
import nl.sense_os.datastorageengine.SensorProfileException;
import nl.sense_os.service.constants.SensorData;

import nl.sense_os.util.json.SchemaException;

/**
 * Tests the storage of sensor data send by the sensing library
 *
 * Created by ted@sense-os.nl on 11/30/15.
 * Copyright (c) 2015 Sense Observation Systems BV. All rights reserved.
 */
public class TestDSEDataConsumer extends AndroidTestCase {
    // The default source for the Android sensing library
    /**
     * The sensor names for which dummy sensors are created
     * @return The list with the names of the available dummy sensors
     */
    public ArrayList<String> getSensorNames() {
        ArrayList<String> sensorNames = new ArrayList();
        sensorNames.add(SensorData.SensorNames.POSITION);
        sensorNames.add(SensorData.SensorNames.NOISE);
        sensorNames.add(SensorData.SensorNames.TIME_ZONE);
        sensorNames.add(SensorData.SensorNames.ACCELEROMETER);
        sensorNames.add(SensorData.SensorNames.BATTERY);
        sensorNames.add(SensorData.SensorNames.CALL);
        sensorNames.add(SensorData.SensorNames.LIGHT);
        sensorNames.add(SensorData.SensorNames.PROXIMITY);
        sensorNames.add(SensorData.SensorNames.SCREEN);
        sensorNames.add(SensorData.SensorNames.WIFI_SCAN);
        sensorNames.add(SensorData.SensorNames.APP_INFO);
        return sensorNames;
    }

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

    /**
     *  Test the storage of sensor data via the DSEDataConsumer
     */
    public void testDSEDataConsumer() throws InterruptedException, ExecutionException, TimeoutException, JSONException, SchemaException, DatabaseHandlerException, SensorProfileException, SensorException {
        // enable local persistence
        for(String sensorName : getSensorNames()) {
            SensorOptions sensorOptions = DefaultSensorOptions.getSensorOptions(getContext(), sensorName);
            sensorOptions.setPersistLocally(true);
            DefaultSensorOptions.setDefaultSensorOptions(getContext(), sensorName, sensorOptions);
        }
        SensorUtils sensorUtils = SensorUtils.getInstance();
        // wait 60 seconds until the DSEDataConsumer is registered
        sensorUtils.waitForDSEDataConsumer(getContext()).get(60, TimeUnit.SECONDS);
        // Get the DSE
        DataStorageEngine dataStorageEngine = DataStorageEngine.getInstance(getContext());
        // Wait until the DSE is ready
        dataStorageEngine.onReady().get(60, TimeUnit.SECONDS);
        // send the sensor data
        sensorUtils.accelerometerSensor.sendDummyData();
        sensorUtils.appInfo.sendDummyData();
        sensorUtils.batterySensor.sendDummyData();
        sensorUtils.callSensor.sendDummyData();
        sensorUtils.lightSensor.sendDummyData();
        sensorUtils.noiseSensor.sendDummyData();
        sensorUtils.positionSensor.sendDummyData();
        sensorUtils.proximitySensor.sendDummyData();
        sensorUtils.screenSensor.sendDummyData();
        sensorUtils.timeZoneSensor.sendDummyData();
        sensorUtils.wifiScanSensor.sendDummyData();
        QueryOptions queryOptions = new QueryOptions();
        // check if it's stored
        for(String sensorName : getSensorNames()){
            Sensor sensor = dataStorageEngine.getSensor(SensorData.SourceNames.SENSE_LIBRARY, sensorName);
            SensorOptions sensorOptions = sensor.getOptions();
            assertTrue("Upload not enabled for this sensor:"+sensorName, sensorOptions.isUploadEnabled());
            assertTrue("No data stored:"+sensorName, sensor.getDataPoints(queryOptions).size()>0);
        }
    }

    /**
     * Test setting the default sensors and check the default upload settings
     */
    public void testDefaultSensorOptions() throws JSONException, SchemaException, DatabaseHandlerException, SensorProfileException, SensorException {

        // Check the default sensor options set in the xml file
        for(String sensorName : getSensorNames()){
            SensorOptions sensorOptions = DefaultSensorOptions.getSensorOptions(getContext(), sensorName);
            assertTrue("Upload not enabled for this sensor", sensorOptions.isUploadEnabled());
        }

        // check adding new options
        for(String sensorName : getSensorNames()) {
            // get the current options
            SensorOptions sensorOptions = DefaultSensorOptions.getSensorOptions(getContext(), sensorName);
            // enable local persistence
            sensorOptions.setPersistLocally(true);
            DefaultSensorOptions.setDefaultSensorOptions(getContext(), sensorName, sensorOptions);
            // get the options again and check for the local persistence
            sensorOptions = DefaultSensorOptions.getSensorOptions(getContext(), sensorName);
            assertTrue("Local persistence not enabled for this sensor", sensorOptions.isPersistLocally());
        }
    }

    /**
     * Test set default options on sensor download and creation in the DataSyncer
     */
    public void testSetDefaultSensorOptions() throws JSONException, IOException, SensorProfileException, SchemaException, DatabaseHandlerException, SensorException, InterruptedException, ExecutionException, TimeoutException {
        // Set up the proxy to upload data to the back-end and simulate a login on a new device
        DataStorageEngine dataStorageEngine = DataStorageEngine.getInstance(getContext());
        DSEConfig dseConfig = dataStorageEngine.getConfig();
        SensorDataProxy mProxy = new SensorDataProxy(dseConfig.backendEnvironment, dseConfig.getAPPKey(), dseConfig.getSessionID());

        // upload sensor data to the back end
        JSONArray data = new JSONArray();
        data.put(new JSONObject("{\"time\":1444739042100,\"value\":1}"));
        mProxy.putSensorData(SensorData.SourceNames.SENSE_LIBRARY, SensorData.SensorNames.NOISE, data);

        // synchronize data to local so a new sensor is created by the DataSyncer
        dataStorageEngine.syncData().get(60, TimeUnit.SECONDS);

        // check if the sensor has been created with the right sensor options
        Sensor noise = DataStorageEngine.getInstance(getContext()).getSensor(SensorData.SourceNames.SENSE_LIBRARY, SensorData.SensorNames.NOISE);
        SensorOptions sensorOptions = noise.getOptions();
        assertTrue("Upload enabled should be true", sensorOptions.isUploadEnabled());
        assertTrue("Download enabled should be true", sensorOptions.isDownloadEnabled());
        assertTrue("Persist locally enabled should be true", sensorOptions.isPersistLocally());
    }
}