package nl.sense_os.service.subscription;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class specifies the requirements which can be returned by a DataConsumer.
 * 
 * Created by ted@sense-os.nl on 11/25/15.
 * Copyright (c) 2015 Sense Observation Systems BV. All rights reserved.
 */
public class SensorRequirement {
    private String mSensorName;
    private Boolean mOptional = false;
    private String mSensorID;
    private String mReason;
    private Integer mSampleInterval;
    private Integer mSampleAccuracy;
    private Long mAtTime;
    private Boolean mUploadEnabled;
    private Boolean mDownloadEnabled;
    private Boolean mPersistLocally;

    public final static String REQUIREMENT_FIELD_UPLOAD_ENABLED = "upload_enabled";
    public final static String REQUIREMENT_FIELD_DOWNLOAD_ENABLED = "download_enabled";
    public final static String REQUIREMENT_FIELD_PERSIST_LOCALLY = "persist_locally";
    public final static String REQUIREMENT_FIELD_AT_TIME = "at_time";
    public final static String REQUIREMENT_FIELD_SAMPLE_ACCURACY = "sample_accuracy";
    public final static String REQUIREMENT_FIELD_SAMPLE_INTERVAL = "sample_interval";
    public final static String REQUIREMENT_FIELD_REASON = "reason";
    public final static String REQUIREMENT_FIELD_SENSOR_ID = "sensor_id";
    public final static String REQUIREMENT_FIELD_OPTIONAL = "optional";
    public final static String REQUIREMENT_FIELD_SENSOR_NAME = "sensor_name";


    /**
     * Creates a requirement for a specific sensor
     * @param sensorName The name of the sensor to create the requirement for
     */
    public SensorRequirement(String sensorName){
        this.mSensorName = sensorName;
    }

    /**
     * Creates a SensorRequirements object from a JSONObject
     * @param requirement The JSONObject sensor requirement, with at least a key, value for REQUIREMENT_FIELD_SENSOR_NAME set.
     */
    public SensorRequirement(JSONObject requirement) throws JSONException {
            String sensorName = requirement.getString(SensorRequirement.REQUIREMENT_FIELD_SENSOR_NAME);
            SensorRequirement sensorRequirement = new SensorRequirement(sensorName);
            if(requirement.has(SensorRequirement.REQUIREMENT_FIELD_UPLOAD_ENABLED)) {
                sensorRequirement.setUploadEnabled(requirement.getBoolean(SensorRequirement.REQUIREMENT_FIELD_UPLOAD_ENABLED));
            }
            if(requirement.has(SensorRequirement.REQUIREMENT_FIELD_DOWNLOAD_ENABLED)) {
                sensorRequirement.setDownloadEnabled(requirement.getBoolean(SensorRequirement.REQUIREMENT_FIELD_DOWNLOAD_ENABLED));
            }
            if(requirement.has(SensorRequirement.REQUIREMENT_FIELD_PERSIST_LOCALLY)) {
                sensorRequirement.setPersistLocally(requirement.getBoolean(SensorRequirement.REQUIREMENT_FIELD_PERSIST_LOCALLY));
            }
            if(requirement.has(SensorRequirement.REQUIREMENT_FIELD_AT_TIME)) {
                sensorRequirement.setAtTime(requirement.getLong(SensorRequirement.REQUIREMENT_FIELD_AT_TIME));
            }
            if(requirement.has(SensorRequirement.REQUIREMENT_FIELD_SAMPLE_ACCURACY)) {
                sensorRequirement.setSampleAccuracy(requirement.getInt(SensorRequirement.REQUIREMENT_FIELD_SAMPLE_ACCURACY));
            }
            if(requirement.has(SensorRequirement.REQUIREMENT_FIELD_SAMPLE_INTERVAL)) {
                sensorRequirement.setSampleInterval(requirement.getInt(SensorRequirement.REQUIREMENT_FIELD_SAMPLE_INTERVAL));
            }
            if(requirement.has(SensorRequirement.REQUIREMENT_FIELD_REASON)) {
                sensorRequirement.setReason(requirement.getString(SensorRequirement.REQUIREMENT_FIELD_REASON));
            }
            if(requirement.has(SensorRequirement.REQUIREMENT_FIELD_SENSOR_ID)) {
                sensorRequirement.setSensorID(requirement.getString(SensorRequirement.REQUIREMENT_FIELD_SENSOR_ID));
            }
            if(requirement.has(SensorRequirement.REQUIREMENT_FIELD_OPTIONAL)) {
                sensorRequirement.setOptional(requirement.getBoolean(SensorRequirement.REQUIREMENT_FIELD_OPTIONAL));
            }
    }

    /**
     * Get the name of the sensor for which this requirement is created
     * @return The sensor name
     */
    public String getSensorName() {
        return mSensorName;
    }

    /**
     * Sets the name of the sensor for which this requirement is created
     * @param sensorName The name of the sensor as specified in SensePrefs
     */
    public void setSensorName(String sensorName) {
        this.mSensorName = sensorName;
    }

    /**
     * Returns whether this sensor requirement is optional
     * @return True when this requirement is set to be optional, false otherwise
     */
    public Boolean getOptional() {
        return mOptional;
    }

    /**
     * Sets whether this sensor requirement is optional
     * @param optional The boolean to set the optional value
     */
    public void setOptional(Boolean optional) {
        this.mOptional = optional;
    }

    /**
     * Returns the sensor identifier set for this requirement
     * @return The sensor identifier if set, null otherwise
     */
    public String getSensorID() {
        return mSensorID;
    }

    /**
     * Sets the specific sensor identifier for which this requirement is only meant
     * @param sensorID An string uniquely identifiable with the current sensor name
     */
    public void setSensorID(String sensorID) {
        this.mSensorID = sensorID;
    }

    /**
     * Returns the reason of having this requirement
     * @return The reason if set, null otherwise
     */
    public String getReason() {
        return mReason;
    }

    /**
     * Sets the reason for having this requirement
     * @param reason The reason
     */
    public void setReason(String reason) {
        this.mReason = reason;
    }

    /**
     * Returns the sample interval requirement for the sensor
     * @return The sample interval in seconds if set, null otherwise
     */
    public Integer getSampleInterval() {
        return mSampleInterval;
    }

    /**
     * Sets the sample interval requirement for the sensor
     * @param sampleInterval The sample interval in seconds
     */
    public void setSampleInterval(Integer sampleInterval) {
        this.mSampleInterval = sampleInterval;
    }

    /**
     * Returns the sample accuracy requirement for the sensor
     * @return The sample accuracy if set, null otherwise
     */
    public Integer getSampleAccuracy() {
        return mSampleAccuracy;
    }

    /**
     * Sets the sample accuracy requirement for the sensor
     * @param sampleAccuracy The sample accuracy in a unit known to the sensor if set, null otherwise
     */
    public void setSampleAccuracy(Integer sampleAccuracy) {
        this.mSampleAccuracy = sampleAccuracy;
    }

    /**
     * Returns the time requirement for receiving the sensor data at a specific moment
     * @return The time in epoch milliseconds for when to receive the data if set, null otherwise
     */
    public Long getAtTime() {
        return mAtTime;
    }

    /**
     * Sets the time requirement for receiving the sensor data at a specific moment
     * @param atTime The time in epoch milliseconds to receive the sensor data point
     */
    public void setAtTime(Long atTime) {
        this.mAtTime = atTime;
    }

    /**
     * Returns the upload data requirement for the sensor
     * @return The upload data requirement if set, null otherwise
     */
    public Boolean getUploadEnabled() {
        return mUploadEnabled;
    }

    /**
     * Sets the upload requirement for the sensor
     * @param uploadEnabled The boolean for enabling and disabling uploading of sensor data
     */
    public void setUploadEnabled(Boolean uploadEnabled) {
        this.mUploadEnabled = uploadEnabled;
    }

    /**
     * Returns the download data requirement for the sensor
     * @return The download data requirement if set, null otherwise
     */
    public Boolean getDownloadEnabled() {
        return mDownloadEnabled;
    }

    /**
     * Sets the download data requirement for the sensor
     * @param downloadEnabled The boolean for enabling and disabling downloading of sensor data
     */
    public void setDownloadEnabled(Boolean downloadEnabled) {
        this.mDownloadEnabled = downloadEnabled;
    }

    /**
     * Returns the persist locally requirement for the sensor
     * @return The persist locally requirement if set, null otherwise
     */
    public Boolean getPersistLocally() {
        return mPersistLocally;
    }

    /**
     * Sets the persist locally requirement for the sensor
     * @param persistLocally The boolean for enabling and disabling the local sensor data persistence
     */
    public void setPersistLocally(Boolean persistLocally) {
        this.mPersistLocally = persistLocally;
    }
}
