package nl.sense_os.service.storage;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashMap;

import nl.sense_os.datastorageengine.AsyncCallback;
import nl.sense_os.datastorageengine.DSEConfig;
import nl.sense_os.datastorageengine.DataStorageEngine;
import nl.sense_os.datastorageengine.DatabaseHandlerException;
import nl.sense_os.datastorageengine.Sensor;
import nl.sense_os.datastorageengine.SensorException;
import nl.sense_os.datastorageengine.SensorOptions;
import nl.sense_os.datastorageengine.SensorProfileException;
import nl.sense_os.service.R;
import nl.sense_os.service.shared.SensorDataPoint;
import nl.sense_os.service.subscription.DataConsumer;
import nl.sense_os.service.subscription.SensorRequirement;
import nl.sense_os.service.subscription.SubscriptionManager;
import nl.sense_os.util.json.SchemaException;

/**
 * This class receives data from the sensors that the DataStorageEngine can and will store.
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
    private HashMap<String, SensorRequirement> mDefaultRequirements;
    private Context mContext;

    private DSEDataConsumer(Context context){
        mDataStorageEngine = DataStorageEngine.getInstance(context);
        sensorCache = new HashMap<>();
        mContext = context;
        mDefaultRequirements = parseDefaultRequirements();
        mDataStorageEngine.onSensorsDownloaded(sensorsDownloaded);
    }

    /**
     * When the sensors are downloaded set the option to download the data
     */
    private AsyncCallback sensorsDownloaded = new AsyncCallback() {
        @Override
        public void onSuccess() {
            // set the download option in the sensor
            for(SensorRequirement sensorRequirement : mDefaultRequirements.values()){
                if(sensorRequirement.getDownloadEnabled() != true) {
                    continue;
                }
                // create the sensor for all the sources to download from
                for(String source : mDataStorageEngine.getSources()) {
                    try {
                        // create the sensor and put the sensor in the cache
                        getSensor(sensorRequirement.getSensorName(), source);
                    } catch (Exception e) {
                        Log.e(TAG, "Error creating sensor for sensor: "+sensorRequirement.getSensorName()+" and source: " +source, e);
                    }
                }
            }
        }

        @Override
        public void onFailure(Throwable throwable) {
            // schedule for a new update
            // TODO find a way to not imediately loop, maybe change it from a one time
            mDataStorageEngine.onSensorsDownloaded(sensorsDownloaded);
        }
    };

    /**
     * Parse the default requirement from the resource file in res/values/default_sensor_requirements.xml
     * @return A map with for each sensor name the requirement
     */
    HashMap<String, SensorRequirement> parseDefaultRequirements(){
        HashMap<String, SensorRequirement> sensorRequirements = new HashMap<>();
        try {
            JSONArray requirements = new JSONArray(mContext.getString(R.string.requirements));
            SubscriptionManager subscriptionManager = SubscriptionManager.getInstance();
            for(int x=0; x < requirements.length(); x++){
                // Get the requirements object
                SensorRequirement sensorRequirement = new SensorRequirement(requirements.getJSONObject(x));
                if(sensorRequirement != null){
                    // If local persistence or uploading of data is set to true, add the requirement and subscribe to the sensor
                    if((sensorRequirement.getPersistLocally() || sensorRequirement.getUploadEnabled())) {
                        sensorRequirements.put(sensorRequirement.getSensorName(), sensorRequirement);
                        subscriptionManager.subscribeConsumer(sensorRequirement.getSensorName(), this);
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing default requirements");
        }
        return sensorRequirements;
    }

    /**
     * Get the DSE DataConsumer
     * @param context
     * @return
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
        return true;
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

    @Override
    public boolean requirementsAreUpdated(String sensorName) {
        return false;
    }

    @Override
    public SensorRequirement getRequirement(String sensorName) {
        if(mDefaultRequirements.containsKey(sensorName)){
            return mDefaultRequirements.get(sensorName);
        }
        return null;
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
    private Sensor getSensor(String sensorName, String source) throws SensorProfileException, SchemaException, DatabaseHandlerException, JSONException, SensorException {
        Sensor sensor;
        // get the sensor from memory
        if(sensorCache.containsKey(getSensorCacheKey(sensorName))){
            sensor = sensorCache.get(sensorName);
        }else{
            synchronized (mDataStorageEngine) {
                SensorOptions sensorOptions = getSensorOptions(sensorName);
                try {
                    // get the sensor if it exists
                    sensor = mDataStorageEngine.getSensor(source, sensorName);
                    // merge the options and set the new options
                    sensor.setOptions(SensorOptions.merge(sensor.getOptions(), sensorOptions));
                } catch (Exception e) {
                    // create the sensor because it does not exist
                    sensor = mDataStorageEngine.createSensor(source, sensorName, sensorOptions);
                }
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

    /**
     * Get the sensor options based on the sensor name
     * @param sensorName The sensor name
     * @return The sensor options for a specific sensor
     */
    private SensorOptions getSensorOptions(String sensorName){
        SensorOptions sensorOptions = new SensorOptions();
        if(!mDefaultRequirements.containsKey(sensorName)){
            return sensorOptions;
        }
        SensorRequirement sensorRequirement = mDefaultRequirements.get(sensorName);
        sensorOptions.setDownloadEnabled(sensorRequirement.getDownloadEnabled());
        sensorOptions.setUploadEnabled(sensorRequirement.getUploadEnabled());
        sensorOptions.setPersistLocally(sensorRequirement.getPersistLocally());
        return sensorOptions;
    }
}
