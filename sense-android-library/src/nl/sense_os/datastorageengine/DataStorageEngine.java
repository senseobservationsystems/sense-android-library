package nl.sense_os.datastorageengine;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import nl.sense_os.util.json.SchemaException;

/**
 * This class provides the main interface for creating sensors and sources and setting storage engine specific properties.
 *
 * Created by ted@sense-os.nl on 10/29/15.
 * Copyright (c) 2015 Sense Observation Systems BV. All rights reserved.
 */
public class DataStorageEngine {
    private static final String TAG = "DataStorageEngine";
    /** The local database handler */
    private DatabaseHandler mDatabaseHandler;
    /** The instance responsible for syncing with the back-end */
    private DataSyncer mDataSyncer;
    /** The proxy for the actual data transfer with the back-end */
    private SensorDataProxy mSensorDataProxy;
    /** Synchronous DataSyncer progress monitor */
    DataSyncerProgress mDataSyncerProgressTracker = new DataSyncerProgress();
    /** Static singleton instance of the DataStorageEngine */
    //private static DataStorageEngine mDataStorageEngine;
    /** Context needed for the DataSyncer */
    private Context mContext;
    /** Ephemeral initialization status */
    private boolean mInitialized;
    /** Ephemeral DSE options */
    private DSEConfig mDSEConfig;

    /** Handles data syncer actions asynchronously */
    private ExecutorService mDataSyncerExecutorService;
    /** Handles the AsyncCallback function calls */
    private ExecutorService mCallBackExecutorService;

    /**
     * The possible statuses of the DataStorageEngine
     * AWAITING_CREDENTIALS = there are not credentials set, setCredentials needs to be called
     * AWAITING_SENSOR_PROFILES = the credentials are set and the sensor profiles are being downloaded
     * READY = the engine is ready for use
     */
    public enum DSEStatus{
        AWAITING_CREDENTIALS,
        AWAITING_SENSOR_PROFILES,
        READY
    }

    /**
     * Constructor of the DataStorageEngine
     * @param context The Android context
     */
    public DataStorageEngine(Context context)
    {
        mContext = context;
        mDataSyncerExecutorService = Executors.newSingleThreadExecutor();
        mCallBackExecutorService = Executors.newCachedThreadPool();
        loadConfiguration();
        initialize();
    }

    /**
     * Loads the latest configuration
     */
    private void loadConfiguration()
    {
        // get the configuration properties from the shared preferences
        // TODO get the configuration from the local storage
    }

    /**
     * Stores the currentConfiguration
     */
    private void saveConfiguration()
    {
        // TODO store the configuration in the shared preferences
    }

//TODO see if a singleton construction is needed
//    /**
//     * Creates a singleton DataStorageEngine instance if it does not exists already
//     * @param context An Android context
//     * @return a static instance of the DataStorageEngine
//     */
//    public synchronized static DataStorageEngine getInstance(Context context)
//    {
//        if(mDataStorageEngine == null) {
//            mDataStorageEngine = new DataStorageEngine(context);
//        }
//        return mDataStorageEngine;
//    }
//
//    /**
//     * Get an initialized DataStorageEngine
//     * @return an Initialed DataStorageEngine
//     * @throws RuntimeException When the DataStorageEngine has not been initialized with a context.
//     */
//    public static DataStorageEngine getInstance()throws RuntimeException{
//        if(mDataStorageEngine == null) {
//            throw new RuntimeException("The DataStorageEngine should be initialized with a context first");
//        }
//        return mDataStorageEngine;
//    }

    /**
     * Returns enum DSEStatus indicating status of DSE.
     * @return The DSEStatus, this could be either AWAITING_CREDENTIALS, AWAITING_SENSOR_PROFILES, READY.
     **/
    public synchronized DSEStatus getStatus()
    {
        if(mDSEConfig == null) {
            return DSEStatus.AWAITING_CREDENTIALS;
        } else if(mInitialized) {
            return DSEStatus.READY;
        } else {
            return DSEStatus.AWAITING_SENSOR_PROFILES;
        }
    }

    /**
     * Set the configuration properties for the DataStorageEngine
     * The DataStorageEngine will re-initialize when the current configuration properties are different then the supplied dseConfig.
     * @param dseConfig The configuration properties to set
     **/
    public synchronized void setConfig(DSEConfig dseConfig)
    {
        if(dseConfig == null || (mDSEConfig != null && dseConfig.equals(mDSEConfig))) {
           return;
        }

        // the credentials have been set, start the initialization
        mDSEConfig = dseConfig;
        saveConfiguration();
        initialize();
    }

    /**
     * Initialized the DataStorageEngine
     * This will initialize the DSE if there are credentials available.
     */
    private synchronized void initialize()
    {
        // if there are no credentials wait with the initialization
        if(mDSEConfig == null) {
            return;
        }
        // reset the initialized status
        mInitialized = false;
        // create a new database handler instance
        mDatabaseHandler = new DatabaseHandler(mContext, mDSEConfig.getUserID());
        //mDatabaseHandler.enableEncryption(mDSEConfig.enableEncryption);
        if(mDSEConfig.enableEncryption != null && mDSEConfig.enableEncryption) {
            // TODO enable encryption on the database. See https://realm.io/docs/java/latest/#encryption
            // mDatabaseHandler.setEncryptionKey(mDSEConfig.encryptionKey);
        }
        // create a new sensor data proxy instance
        mSensorDataProxy = new SensorDataProxy(mDSEConfig.backendEnvironment, mDSEConfig.getAPPKey(), mDSEConfig.getSessionID());
        // disable the period syncing of the previous data syncer
        if(mDataSyncer != null) {
            mDataSyncer.disablePeriodicSync();
        }
        // create a new data syncer instance
        mDataSyncer = new DataSyncer(mContext, mDatabaseHandler,mSensorDataProxy);
        if(mDSEConfig.localPersistancePeriod != null) {
            mDataSyncer.setPersistPeriod(mDSEConfig.localPersistancePeriod);
        }
        // execute the initialization of the DataSyncer asynchronously
        mDataSyncerExecutorService.execute(mInitTask);
    }

    /**
     * The FutureTask that performs the DataSyncer initialization (sensor profile downloading)
     * When this task is successful then the status of the DSE will be READY
     */
    private FutureTask<Boolean> mInitTask = new FutureTask<>(new Callable<Boolean>() {
        public Boolean call() throws JSONException, IOException, SensorProfileException {
            try {
                mDataSyncer.initialize();
                // when the initialization is done download the sensors
                mDataSyncerExecutorService.execute(mDataSync);
                mInitialized = true;
                return true;
            } catch(Exception e) {
                Log.e(TAG, "Error initializing the DataSyncer", e);
                throw  e;
            }
        }
    });

    /**
     * Receive an update when the DataStorageEngine has done it's initialization
     * @return A Future<boolean> which will return the status of the initialization of the DataStorageEngine.
     */
    public Future<Boolean> onReady() {
        return mInitTask;
    }

    /**
     * Receive a one time notification when the DataStorageEngine has done it's initialization
     * @param asyncCallback The AsynchronousCall back to receive the status of the initialization in
     */
    public void onReady(AsyncCallback asyncCallback) {
        getResultAsync(onReady(), asyncCallback);
    }

    /**
     * The FutureTask that performs the initial sensors and sensor data download in the DataSyncer.
     * This task will initialize mDataSyncerProgress and set any received exception via mDataSyncerProgress.setLastException.
     */
    private FutureTask<Boolean> mDataSync = new FutureTask<>(new Callable<Boolean>() {
        public Boolean call() throws Exception {
            try {
                mDataSyncer.sync(mDataSyncerProgressTracker);
                return true;
            } catch(Exception e) {
                Log.e(TAG, "Error doing the initial sync on the DataSyncer", e);
                mDataSyncerProgressTracker.onException(e);
               throw e;
            }
        }
    });


    /**
     * Receive an update when the sensors have been downloaded
     * @return A Future<boolean> which will return the status of the sensor download process of the DataStorageEngine.
     */
    public Future<Boolean> onSensorsDownloaded() {
        // this future will only be executed when:
        // 1) the associated call back function is called
        // 2) when an exception is thrown
        // 3) the call back function was already called successfully
       Callable<Boolean> functionCall = new Callable<Boolean>() {
           @Override
           public Boolean call() throws Exception {
               if (mDataSyncerProgressTracker.isDownloadSensorsCompleted) {
                   return true;
               } else {
                   throw mDataSyncerProgressTracker.lastException;
               }
           }
       };

        // if the event has been fired already, submit the function call and return the future
        if(mDataSyncerProgressTracker.isDownloadSensorsCompleted) {
           return mCallBackExecutorService.submit(functionCall);
        }
        // put it in the queue and wait for it to be executed
        else {
            FutureTask<Boolean> future = new FutureTask(functionCall);
            mDataSyncerProgressTracker.downloadSensorsFutureQueue.add(future);
            return future;
        }
    }

    /**
     * Receive a one time notification when the sensors have been downloaded
     * @param asyncCallback The AsynchronousCall which will return the status of the sensor download process of the DataStorageEngine.
     */
    public void onSensorsDownloaded(AsyncCallback asyncCallback)
    {
        getResultAsync(onSensorsDownloaded(), asyncCallback);
    }

    /**
     * Receive an update when the sensor data has been downloaded
     * @return True when the sensor data download was successful, throws and exception otherwise
     */
    public Future<Boolean> onSensorDataDownloaded() {
        // this future will only be executed when:
        // 1) the associated call back function is called
        // 2) when an exception is thrown
        // 3) the call back function was already called successfully
        Callable<Boolean> functionCall = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                if (mDataSyncerProgressTracker.isDownloadSensorDataCompleted) {
                    return true;
                } else {
                    throw mDataSyncerProgressTracker.lastException;
                }
            }
        };

        // if the event has been fired already, submit the function call and return the future
        if(mDataSyncerProgressTracker.isDownloadSensorDataCompleted) {
            return mCallBackExecutorService.submit(functionCall);
        }
        // put it in the queue and wait for it to be executed
        else {
            FutureTask<Boolean> future = new FutureTask(functionCall);
            mDataSyncerProgressTracker.downloadSensorDataFutureQueue.add(future);
            return future;
        }
    }

    /**
     * Receive a one time notification when the sensor data has been downloaded
     * @param asyncCallback The AsynchronousCall which will return the status of the sensor data download process of the DataStorageEngine.
     */
    public void onSensorDataDownloaded(AsyncCallback asyncCallback)
    {
        getResultAsync(onSensorDataDownloaded(), asyncCallback);
    }

    /**
     * Synchronizes the local and remote data
     * @return A future which will return the status of the data synchronization action as Boolean via Future.get()
     */
    public Future<Boolean> syncData() {
        return mDataSyncerExecutorService.submit(new Callable<Boolean>() {
            public Boolean call() throws Exception {
                try {
                    if (getStatus() != DSEStatus.READY) {
                        throw new IllegalStateException("The DataStorageEngine is not ready yet");
                    }
                    mDataSyncer.sync();
                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "Error flushing data", e);
                    throw e;
                }
            }
        });
    }

    /**
     * Synchronizes the local and remote data
     * Receive a one time notification when the DataStorage engine has done the data synchronization
     * @param asyncCallback The AsynchronousCall back to receive the status of the data synchronization in
     */
    public void syncData(AsyncCallback asyncCallback) {
        getResultAsync(syncData(), asyncCallback);
    }

    /**
     * Returns the result from a future via the AsyncCallback
     * @param future The future to get the result from
     * @param callback The AsyncCallback to receive the result in
     */
    protected void getResultAsync(final Future<Boolean> future, final AsyncCallback callback)
    {
        mCallBackExecutorService.submit(new Callable() {
            public Object call() throws Exception {
                try {
                    if (future.get())
                        callback.onSuccess();
                    else
                        callback.onFailure(null);
                } catch (Exception e) {
                    Log.e(TAG, "Error getting async result from future", e);
                    callback.onFailure(e);
                }
                return null;
            }
        });
    }

    /**
     * Create a new sensor in database and backend if it does not already exist.
     * @param source The source name (e.g accelerometer)
     * @param name The sensor name (e.g accelerometer)
     * @param options The sensor options
     * @return The newly created sensor object
     * @throws IllegalStateException when the DataStorageEngine is not ready yet
     * @throws DatabaseHandlerException when the sensor already exists
     * @throws SensorException, when the sensor name is not valid
     **/
    public Sensor createSensor(String source, String name, SensorOptions options) throws SensorProfileException, SchemaException, SensorException, DatabaseHandlerException, JSONException {
        if(getStatus() != DSEStatus.READY) {
            throw new IllegalStateException("The DataStorageEngine is not ready yet");
        }
        return mDatabaseHandler.createSensor(source, name, options);
    }

    /**
     * Returns a specific sensor by name and the source it belongs to
     * @param source The name of the source
     * @param sensorName The name of the sensor
     * @throws IllegalStateException when the DataStorageEngine is not ready yet
     **/
    public Sensor getSensor(String source, String sensorName) throws DatabaseHandlerException, SensorException, JSONException, SensorProfileException, SchemaException {
        if(getStatus() != DSEStatus.READY) {
            throw new IllegalStateException("The DataStorageEngine is not ready yet");
        }
        return mDatabaseHandler.getSensor(source, sensorName);
    }

    /**
     * Returns all the sensors connected to the given source
     * @return List<Sensor> The sensors connected to the given source
     * @throws IllegalStateException when the DataStorageEngine is not ready yet
     **/
    public List<Sensor> getSensors(String source) throws JSONException, SensorException, SensorProfileException, SchemaException {
        if(getStatus() != DSEStatus.READY) {
            throw new IllegalStateException("The DataStorageEngine is not ready yet");
        }
        return mDatabaseHandler.getSensors(source);
    }

    /**
     * Returns all the sources attached to the current user
     * @return List<String> The sources attached to the current user
     * @throws IllegalStateException when the DataStorageEngine is not ready yet
     **/
    public List<String> getSources() {
        if (getStatus() != DSEStatus.READY) {
            throw new IllegalStateException("The DataStorageEngine is not ready yet");
        }
        return mDatabaseHandler.getSources();
    }


    class DataSyncerProgress implements ProgressCallback {
        public boolean isDownloadSensorsCompleted;
        public boolean isDownloadSensorDataCompleted;
        public Queue<FutureTask> downloadSensorsFutureQueue = new ConcurrentLinkedQueue<>();
        public Queue<FutureTask> downloadSensorDataFutureQueue = new ConcurrentLinkedQueue<>();
        public Queue<FutureTask> exceptionFutureQueue = new ConcurrentLinkedQueue<>();
        public Exception lastException = null;

        /** Function to call when an exception is thrown when executing a DataSyncer function with ProgressCallback*/
        public void onException(Exception e){
            lastException = e;

            // process all the futures
            for(FutureTask future : exceptionFutureQueue){
                mCallBackExecutorService.execute(future);
            }
            for(FutureTask future : downloadSensorsFutureQueue){
                mCallBackExecutorService.execute(future);
            }
            for(FutureTask future : downloadSensorDataFutureQueue){
                mCallBackExecutorService.execute(future);
            }
        }

        @Override
        public void onDownloadSensorsCompleted() {
            isDownloadSensorsCompleted = true;
            for(FutureTask future : downloadSensorsFutureQueue){
                mCallBackExecutorService.execute(future);
            }
        }

        @Override
        public void onDownloadSensorDataCompleted() {
            isDownloadSensorDataCompleted = true;
            for(FutureTask future : downloadSensorDataFutureQueue){
                mCallBackExecutorService.execute(future);
            }
        }

        @Override
        public void onDeletionCompleted() {}

        @Override
        public void onUploadCompeted() {}

        @Override
        public void onCleanupCompleted() {}
    }

}
