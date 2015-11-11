package nl.sense_os.datastorageengine;

/**
 * Handles the response of the DataSyncer synchronously by using a monitor for the ProgressCallback events
 *
 * Created by ted@sense-os.nl on 11/10/15.
 * Copyright (c) 2015 Sense Observation Systems BV. All rights reserved.
 */
class DataSyncerProgressMonitor implements ProgressCallback {
    private boolean isDeletionCompleted = false;
    private boolean isUploadCompleted = false;
    private boolean isDownloadSensorsCompleted = false;
    private boolean isDownloadSensorDataCompleted = false;
    private boolean isCleanupCompleted = false;

    /** Status monitor for the onDeletionCompleted event */
    public final Object isDeletionCompletedMonitor = new Object();
    /** Status monitor for the onUploadCompleted event */
    public final Object isUploadCompletedMonitor = new Object();
    /** Status monitor for the onDownloadSensorsCompleted event */
    public final Object isDownloadSensorsCompletedMonitor = new Object();
    /** Status monitor for the onDownloadSensorDataCompleted event */
    public final Object isDownloadSensorDataCompletedMonitor = new Object();
    /** Status monitor for the onCleanupCompleted event */
    public final Object isCleanupCompletedMonitor = new Object();
    /** The exception thrown by the called function */
    private Exception lastException;

    /**
     * Resets the status of the events.
     * This function needs to be called when the instance is reused.
     */
    public void reset()
    {
        isDeletionCompleted = false;
        isUploadCompleted = false;
        isDownloadSensorsCompleted = false;
        isDownloadSensorDataCompleted = false;
        isCleanupCompleted = false;
        lastException = null;
    }

    /**
     * Returns the status of the data deletion event
     * @return True when onDeletionCompleted has been called
     */
    public boolean getIsDeletionCompleted() {
        return isDeletionCompleted;
    }

    /**
     * Returns the status of the data upload event
     * @return True when onUploadCompleted has been called
     */
    public boolean getIsUploadCompleted() {
        return isUploadCompleted;
    }

    /**
     * Returns the status of the sensors download event
     * @return True when onDownloadSensorsCompleted has been called
     */
    public boolean getIsDownloadSensorsCompleted() {
        return isDownloadSensorsCompleted;
    }

    /**
     * Returns the status of the sensor data download event
     * @return True when onDownloadSensorDataCompleted has been called
     */
    public boolean getIsDownloadSensorDataCompleted() {
        return isDownloadSensorDataCompleted;
    }

    /**
     * Returns the status of the local data clean up event
     * @return True when onCleanupCompleted has been called
     */
    public boolean getIsCleanupCompleted() {
        return isCleanupCompleted;
    }

    /**
     * Returns the last exception thrown by the function called with the ProgressCallback callback
     * @return Null if not exception has been thrown, else one of the following exceptions: IOException,
     * DatabaseHandlerException, SensorException, SensorProfileException, JSONException, SchemaException, ValidationException
     */
    public Exception getLastException() {
        return lastException;
    }

    /**
     * If an error occurs the last exception is set and all status monitors are notified
     * @param e The exception to set as last thrown exception.
     */
    public void setLastException(Exception e)
    {
        lastException = e;
        setIsDeletionCompleted(isDeletionCompleted);
        setIsUploadCompleted(isUploadCompleted);
        setIsDownloadSensorsCompleted(isDownloadSensorsCompleted);
        setIsDownloadSensorDataCompleted(isDownloadSensorDataCompleted);
        setIsCleanupCompleted(isCleanupCompleted);
    }

    private void setIsUploadCompleted(boolean value) {
        synchronized (isUploadCompletedMonitor) {
            isUploadCompleted = value;
            isUploadCompletedMonitor.notifyAll();
        }
    }

    private void setIsDownloadSensorsCompleted(boolean value) {
        synchronized (isDownloadSensorsCompletedMonitor) {
            isDownloadSensorsCompleted = value;
            isDownloadSensorsCompletedMonitor.notifyAll();
        }
    }

    private void setIsDeletionCompleted(boolean value) {
        synchronized (isDeletionCompletedMonitor) {
            isDeletionCompleted = value;
            isDeletionCompletedMonitor.notifyAll();
        }
    }

    private void setIsCleanupCompleted(boolean value) {
        synchronized (isCleanupCompletedMonitor) {
            isCleanupCompleted = value;
            isCleanupCompletedMonitor.notifyAll();
        }
    }

    private void setIsDownloadSensorDataCompleted(boolean value) {
        synchronized (isDownloadSensorDataCompletedMonitor) {
            isDownloadSensorDataCompleted = value;
            isDownloadSensorDataCompletedMonitor.notifyAll();
        }
    }

    @Override
    public void onDeletionCompleted() {
        setIsDeletionCompleted(true);
    }

    @Override
    public void onUploadCompeted() {
        setIsUploadCompleted(true);
    }

    @Override
    public void onDownloadSensorsCompleted() {
        setIsDownloadSensorsCompleted(true);
    }

    @Override
    public void onDownloadSensorDataCompleted() {
        setIsDownloadSensorDataCompleted(true);
    }

    @Override
    public void onCleanupCompleted() {
        setIsCleanupCompleted(true);
    }
}
