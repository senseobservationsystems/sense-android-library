package nl.sense_os.datastorageengine;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;

import nl.sense_os.service.R;

/**
 * This class provide access to the default sensor options set in res/values/default_sensor_options.xml
 * It also provides the possibility to override these default options, with manually defined sensorOptions that will be stored in the shared preferences.
 * The DefaultSensorOptions will only be used when new sensors are created during the initialization of the DataStorageEngine or the storage of new sensor data.
 *
 * Created by ted@sense-os.nl on 11/26/15.
 * Copyright (c) 2015 Sense Observation Systems BV. All rights reserved.
 */
public class DefaultSensorOptions {
    private final static String PREFERENCE_NAME = "default_sensor_options";
    private final static String PREFERENCE_UPLOAD_ENABLED = "upload_enabled";
    private final static String PREFERENCE_DOWNLOAD_ENABLED = "download_enabled";
    private final static String PREFERENCE_PERSIST_LOCALLY = "persist_locally";
    private final static String PREFERENCE_SENSOR_NAME = "sensor_name";
    private final static String PREFERENCE_META = "meta";
    private static final String TAG = "defaultSensorOptions";
    private static HashMap<String, SensorOptions> defaultOptions;

    public static SensorOptions getSensorOptions(Context context, String sensorName){
        // get the default sensor option from the xml file
        SensorOptions sensorOptions = getDefaultOption(context, sensorName);
        // get the shared preferences
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        // check if a preference has been set, otherwise get the default set there
        if(sharedPreferences.contains(PREFERENCE_UPLOAD_ENABLED+sensorName)){
            sensorOptions.setUploadEnabled(sharedPreferences.getBoolean(PREFERENCE_UPLOAD_ENABLED+sensorName, false));
        }
        if(sharedPreferences.contains(PREFERENCE_DOWNLOAD_ENABLED+sensorName)){
            sensorOptions.setDownloadEnabled(sharedPreferences.getBoolean(PREFERENCE_DOWNLOAD_ENABLED + sensorName, false));
        }
        if(sharedPreferences.contains(PREFERENCE_PERSIST_LOCALLY+sensorName)){
            sensorOptions.setPersistLocally(sharedPreferences.getBoolean(PREFERENCE_PERSIST_LOCALLY + sensorName, false));
        }
        try {
            if (sharedPreferences.contains(PREFERENCE_META + sensorName)) {
                sensorOptions.setMeta(new JSONObject(sharedPreferences.getString(PREFERENCE_META + sensorName, "")));
            }
        }catch(Exception e){
            Log.e(TAG, "Error processing meta");
        }
        return sensorOptions;
    }

    private static SensorOptions getDefaultOption(Context context, String sensorName){
        SensorOptions sensorOptions = new SensorOptions();

        if(defaultOptions == null) {
            parseSensorOptionsFile(context);
        }

        if(defaultOptions.containsKey(sensorName)){
            return defaultOptions.get(sensorName);
        }else{
            return sensorOptions;
        }
    }

    private static void parseSensorOptionsFile(Context context){
        try {
            defaultOptions = new HashMap<>();
            JSONArray sensorOptions = new JSONArray(context.getString(R.string.sensorOptions));
            for(int x=0; x < sensorOptions.length(); ++x){
                JSONObject jSONSensorOption = sensorOptions.getJSONObject(x);
                defaultOptions.put(jSONSensorOption.getString(PREFERENCE_SENSOR_NAME), getSensorOption(jSONSensorOption));
            }
        } catch (JSONException e) {
            Log.e(TAG, "error parsing default_sensor_options.xml");
        }
    }

    private static SensorOptions getSensorOption(JSONObject jsonObject){
        SensorOptions sensorOption = new SensorOptions();
        try {
            if (jsonObject.has(PREFERENCE_DOWNLOAD_ENABLED)) {
                sensorOption.setDownloadEnabled(jsonObject.getBoolean(PREFERENCE_DOWNLOAD_ENABLED));
            }
            if (jsonObject.has(PREFERENCE_UPLOAD_ENABLED)) {
                sensorOption.setUploadEnabled(jsonObject.getBoolean(PREFERENCE_UPLOAD_ENABLED));
            }
            if (jsonObject.has(PREFERENCE_PERSIST_LOCALLY)) {
                sensorOption.setPersistLocally(jsonObject.getBoolean(PREFERENCE_PERSIST_LOCALLY));
            }
            if (jsonObject.has(PREFERENCE_META)) {
                sensorOption.setMeta(jsonObject.getJSONObject(PREFERENCE_META));
            }
        }catch(Exception e){
            Log.e(TAG, "Error parsing default options file");
        }
        return sensorOption;
    }

    public static void setDefaultSensorOptions(Context context, String sensorName, SensorOptions sensorOptions){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        boolean changed = false;

        if(sensorOptions.isUploadEnabled() != null) {
            editor.putBoolean(PREFERENCE_UPLOAD_ENABLED + sensorName, sensorOptions.isUploadEnabled());
            changed = true;
        }

        if(sensorOptions.isDownloadEnabled() != null) {
            editor.putBoolean(PREFERENCE_DOWNLOAD_ENABLED + sensorName, sensorOptions.isDownloadEnabled());
            changed = true;
        }

        if(sensorOptions.isPersistLocally() != null) {
            editor.putBoolean(PREFERENCE_PERSIST_LOCALLY + sensorName, sensorOptions.isPersistLocally());
            changed = true;
        }

        if(sensorOptions.getMeta() != null) {
            editor.putString(PREFERENCE_META + sensorName, sensorOptions.getMeta().toString());
            changed = true;
        }

        // save the changes
        if(changed){
            editor.commit();
        }
    }
}
