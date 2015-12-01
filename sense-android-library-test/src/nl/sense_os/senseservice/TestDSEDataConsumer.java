package nl.sense_os.senseservice;

import android.test.AndroidTestCase;
import android.util.Log;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import nl.sense_os.datastorageengine.DataStorageEngine;
import nl.sense_os.datastorageengine.DatabaseHandlerException;
import nl.sense_os.datastorageengine.DefaultSensorOptions;
import nl.sense_os.datastorageengine.QueryOptions;
import nl.sense_os.datastorageengine.Sensor;
import nl.sense_os.datastorageengine.SensorException;
import nl.sense_os.datastorageengine.SensorOptions;
import nl.sense_os.datastorageengine.SensorProfileException;
import nl.sense_os.service.constants.SensorData;

import nl.sense_os.util.json.SchemaException;

/**
 * Created by ted@sense-os.nl on 11/30/15.
 * Copyright (c) 2015 Sense Observation Systems BV. All rights reserved.
 */
public class TestDSEDataConsumer extends AndroidTestCase {
    private static final String TAG = "TESTDSEDataConsumer";

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
        // Get the DSE
        DataStorageEngine dataStorageEngine = DataStorageEngine.getInstance(getContext());
        // Wait until the DSE is ready
        dataStorageEngine.onReady().get(60, TimeUnit.SECONDS);

        // send the sensor data
        SensorUtils sensorUtils = SensorUtils.getInstance();
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
        // The default source for the Android sensing library
        String source = "sense-android";
        QueryOptions queryOptions = new QueryOptions();
        // check if it's stored
        for(String sensorName : getSensorNames()){
            Sensor sensor = dataStorageEngine.getSensor(source, sensorName);
            SensorOptions sensorOptions = sensor.getOptions();
            assertTrue("Upload not enabled for this sensor", sensorOptions.isUploadEnabled());
            assertTrue("No data stored", sensor.getDataPoints(queryOptions).size()>0);
        }
    }

    /**
     * Test if the settings of the default sensors are correct
     */
    public void testDefaultSensorOptions(){

        for(String sensorName : getSensorNames()){
            SensorOptions sensorOptions = DefaultSensorOptions.getSensorOptions(getContext(), sensorName);
            assertTrue("Upload not enabled for this sensor", sensorOptions.isUploadEnabled());
        }
    }
}
