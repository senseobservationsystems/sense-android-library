package nl.sense_os.service.storage;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;

import java.util.HashMap;

import nl.sense_os.datastorageengine.AsyncCallback;
import nl.sense_os.datastorageengine.DSEConfig;
import nl.sense_os.datastorageengine.DataStorageEngine;
import nl.sense_os.datastorageengine.DatabaseHandlerException;
import nl.sense_os.datastorageengine.DefaultSensorOptions;
import nl.sense_os.datastorageengine.Sensor;
import nl.sense_os.datastorageengine.SensorException;
import nl.sense_os.datastorageengine.SensorOptions;
import nl.sense_os.datastorageengine.SensorProfileException;
import nl.sense_os.service.shared.SensorDataPoint;
import nl.sense_os.service.subscription.DataConsumer;
import nl.sense_os.service.subscription.SensorRequirement;
import nl.sense_os.service.subscription.SubscriptionManager;
import nl.sense_os.util.json.SchemaException;

/**
 * This class receives data from the sensors that the DataStorageEngine can store.
 *
 * The DSEDataConsumer subscribes to a fixed set of sensor names.
 * A sensor will be created if it does not exists yet with SensorOptions defined per sensor.
 *
 * Created by ted@sense-os.nl on 11/17/15.
 * Copyright (c) 2015 Sense Observation Systems BV. All rights reserved.
 */
public class DSEDataConsumer implements DataConsumer{

    private static final String TAG = "DSEDataConsumer";
    private static DSEDataConsumer mDSEDataConsumer;
    private DataStorageEngine mDataStorageEngine;
    private HashMap<String, Sensor> sensorCache;
    private Context mContext;

    private DSEDataConsumer(Context context){
        mDataStorageEngine = DataStorageEngine.getInstance(context);
        sensorCache = new HashMap<>();
        mContext = context;
        mDataStorageEngine.onReady(subscribeToSensors);
    }

    /**
     * Subscribe to all the sensors in the sensor profiles
     */
    AsyncCallback subscribeToSensors = new AsyncCallback(){
        @Override
        public void onSuccess(){
            SubscriptionManager subscriptionManager = SubscriptionManager.getInstance();

            try {
                for(String sensorName: mDataStorageEngine.getSensorNames()) {
                    subscriptionManager.subscribeConsumer(sensorName, DSEDataConsumer.this);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting sensor names", e);
            }
        }

        @Override
        public void onFailure(Throwable throwable) {
        }
    };

    /**
     * Get the DSE DataConsumer
     * @param context An Android context
     * @return The DSEDataConsumer instance
     */
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
        return false;
    }

    @Override
    public void onNewData(SensorDataPoint dataPoint) {
        try {
            // DSE not ready, we can't store the data store
            if(mDataStorageEngine.getStatus() != DataStorageEngine.DSEStatus.READY){
                return;
            }
            Sensor sensor = getSensor(dataPoint.sensorName, dataPoint.source);
            sensor.insertOrUpdateDataPoint(dataPoint.getValue(), dataPoint.timeStamp);
        }catch(Exception e) {
            Log.e(TAG, "Error handling data for sensor: " + dataPoint.sensorName, e);
        }
    }

    /**
     * Get the sensor corresponding to a specific sensor name and the default source
     * Will create the sensor for this source if it does not exists
     * @param sensorName The name of the sensor
     * @return The sensor object
     * @throws SensorProfileException
     * @throws SchemaException
     * @throws DatabaseHandlerException
     * @throws JSONException
     * @throws SensorException
     */
    private Sensor getSensor(String sensorName, String source) throws SensorProfileException, SchemaException, DatabaseHandlerException, JSONException, SensorException {
        Sensor sensor;
        // get the sensor from memory
        if(sensorCache.containsKey(getSensorCacheKey(sensorName))){
            sensor = sensorCache.get(sensorName);
        }else{
            synchronized (mDataStorageEngine) {
                // check if the sensor is there
                sensor =  mDataStorageEngine.getSensor(source, sensorName);
            }
            // store the sensor in memory
            sensorCache.put(getSensorCacheKey(sensorName), sensor);
        }
        return sensor;
    }

    /**
     * Returns the local cache key for the sensor of the current user
     * @param sensorName The sensor name to return the cache key for
     * @return The sensorCache key string
     */
    private String getSensorCacheKey(String sensorName){
        DSEConfig dseConfig = mDataStorageEngine.getConfig();
        return sensorName+dseConfig.getUserID();
    }
}
