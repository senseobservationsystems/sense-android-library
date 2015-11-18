package nl.sense_os.service.storage;

import android.content.Context;
import android.util.Log;
import org.json.JSONException;
import java.util.HashMap;

import nl.sense_os.datastorageengine.DataStorageEngine;
import nl.sense_os.datastorageengine.DatabaseHandlerException;
import nl.sense_os.datastorageengine.Sensor;
import nl.sense_os.datastorageengine.SensorException;
import nl.sense_os.datastorageengine.SensorOptions;
import nl.sense_os.datastorageengine.SensorProfileException;
import nl.sense_os.service.constants.SensorData;
import nl.sense_os.service.shared.SensorDataPoint;
import nl.sense_os.service.subscription.DataConsumer;
import nl.sense_os.service.subscription.SubscriptionManager;
import nl.sense_os.util.json.SchemaException;

/**
 * Created by ted@sense-os.nl on 11/17/15.
 * Copyright (c) 2015 Sense Observation Systems BV. All rights reserved.
 */
public class DSEDataConsumer implements DataConsumer{

    private static final String TAG = "DSEDataConsumer";
    private static DSEDataConsumer mDSEDataConsumer;
    private DataStorageEngine mDataStorageEngine;
    private final String source = "sense-android";
    private HashMap<String, Sensor> sensorCache;

    private DSEDataConsumer(Context context){
        mDataStorageEngine = DataStorageEngine.getInstance(context);
        sensorCache = new HashMap<>();

        SubscriptionManager subscriptionManager = SubscriptionManager.getInstance();
        // register to receive input from the sensors that the DSE can store
        subscriptionManager.subscribeConsumer(SensorData.SensorNames.TIME_ZONE, this);
        subscriptionManager.subscribeConsumer(SensorData.SensorNames.ACCELEROMETER, this);
        subscriptionManager.subscribeConsumer(SensorData.SensorNames.BATTERY, this);
        subscriptionManager.subscribeConsumer(SensorData.SensorNames.CALL, this);
        subscriptionManager.subscribeConsumer(SensorData.SensorNames.LIGHT, this);
        subscriptionManager.subscribeConsumer(SensorData.SensorNames.NOISE, this);
        subscriptionManager.subscribeConsumer(SensorData.SensorNames.POSITION, this);
        subscriptionManager.subscribeConsumer(SensorData.SensorNames.PROXIMITY, this);
        subscriptionManager.subscribeConsumer(SensorData.SensorNames.SCREEN, this);
        subscriptionManager.subscribeConsumer(SensorData.SensorNames.WIFI_SCAN, this);
        subscriptionManager.subscribeConsumer(SensorData.SensorNames.APP_INFO, this);
    }

    public synchronized static DSEDataConsumer getInstance(Context context){
        if(mDSEDataConsumer == null){
            mDSEDataConsumer = new DSEDataConsumer(context);
        }
        return mDSEDataConsumer;
    }

    @Override
    public void startNewSample() {}

    @Override
    public boolean isSampleComplete() {
        return true;
    }

    @Override
    public void onNewData(SensorDataPoint dataPoint) {
        try {
            // DSE not ready, we can't store the data store
            if(mDataStorageEngine.getStatus() != DataStorageEngine.DSEStatus.READY){
                return;
            }
                Sensor sensor = getSensor(dataPoint.sensorName);
                sensor.insertOrUpdateDataPoint(dataPoint.getValue(), dataPoint.timeStamp);
        }catch(Exception e) {
            Log.e(TAG, "Error handling data for sensor: " + dataPoint.sensorName, e);
        }
    }

    /**
     *  Get the sensor corresponding to a specific sensor name and the default source
     * Will create the sensor for this source if it does not exists
     * @param sensorName The name of the sensor
     * @return The sensor object
     * @throws SensorProfileException
     * @throws SchemaException
     * @throws DatabaseHandlerException
     * @throws JSONException
     * @throws SensorException
     */
    private Sensor getSensor(String sensorName) throws SensorProfileException, SchemaException, DatabaseHandlerException, JSONException, SensorException {
        Sensor sensor;
        // get the sensor from memory
        if(sensorCache.containsKey(sensorName)){
            sensor = sensorCache.get(sensorName);
        }else{
            synchronized (mDataStorageEngine) {
                try {
                    // get the sensor if it exists
                    sensor = mDataStorageEngine.getSensor(source, sensorName);
                } catch (Exception e) {
                    // create the sensor because it does not exist
                    sensor = mDataStorageEngine.createSensor(source, sensorName, getSensorOptions(sensorName));
                }
            }
            // store the sensor in memory
            sensorCache.put(sensorName, sensor);
        }
        return sensor;
    }

    /**
     * Get the sensor options based on the sensor name
     * @param sensorName The sensor name
     * @return The sensor options for a specific sensor
     */
    private SensorOptions getSensorOptions(String sensorName){
        SensorOptions sensorOptions = new SensorOptions();
        // set the upload of all sensors to true
        sensorOptions.setUploadEnabled(true);
        // only set the time_zone sensor to download and persist it's data locally
        if(sensorName.equals(SensorData.SensorNames.TIME_ZONE)) {
            sensorOptions.setDownloadEnabled(true);
            sensorOptions.setPersistLocally(true);
        }
        return sensorOptions;
    }

}
