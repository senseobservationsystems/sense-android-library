package nl.sense_os.datastorageengine;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
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
    private DataSyncerProgressMonitor mDataSyncerProgress;
    /** Static singleton instance of the DataStorageEngine */
    //private static DataStorageEngine mDataStorageEngine;
    /** Context needed for the DataSyncer */
    private Context mContext;
    /** Ephemeral credentials set status */
    private boolean mHasCredentials;
    /** Ephemeral initialization status */
    private boolean mInitialized;
    /** Ephemeral DSE options */
    private DSEOptions mOptions;
    /** Ephemeral DSE credentials */
    private String mUserID = "";
    private String mSessionID = "";
    private String mAPPKey = "";

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
        mOptions = new DSEOptions();
        mDataSyncerExecutorService = Executors.newSingleThreadExecutor();
        mCallBackExecutorService = Executors.newCachedThreadPool();
        mDataSyncerProgress = new DataSyncerProgressMonitor();
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
        if(!mHasCredentials) {
            return DSEStatus.AWAITING_CREDENTIALS;
        } else if(mInitialized) {
            return DSEStatus.READY;
        } else {
            return DSEStatus.AWAITING_SENSOR_PROFILES;
        }
    }

    /**
     * Set the options DataStorageOption
     * Options with a null value will be unchanged.
     * @param options The options to set for the DataStorageEngine
     */
    public synchronized void setOptions(DSEOptions options)
    {
        DSEOptions oldOptions = mOptions;
        mOptions = options;
        // if the options have change initialize the DSE
        if(!oldOptions.equals(options)) {
            initialize();
        }
    }

    /**
     * Set credentials required in DataStorageEngine
     *
     * SessionID, AppKey for Http requests. userId for table management in the database.
     *
     * @param sessionID The sessionID given on the latest successful login.
     * @param userId The userId of the current logged in user.
     * @param appKey The appKey of the application using DSE.
     * @throws IllegalArgumentException When one of the arguments is empty
     **/
    public synchronized void setCredentials(String sessionID, String userId, String appKey) throws IllegalArgumentException
    {
        if(sessionID == null || sessionID.length() == 0 ) {
            throw new IllegalArgumentException("missing sessionID");
        }
        if(userId == null || userId.length() == 0 ) {
            throw new IllegalArgumentException("missing userID");
        }
        if(appKey == null || appKey.length() == 0 ) {
            throw new IllegalArgumentException("missing appKey");
        }

        boolean credentialsUpdated = false;
        if(!mUserID.equals(userId) || !mSessionID.equals(sessionID) || !mAPPKey.equals(appKey)) {
            credentialsUpdated = true;
        }

        mUserID = userId;
        mSessionID = sessionID;
        mAPPKey = appKey;

        // the credentials have been set, start the initialization
        mHasCredentials = true;
        if(credentialsUpdated) {
            initialize();
        }
    }

    /**
     * Initialized the DataStorageEngine
     * This will initialize the DSE if there are credentials available.
     */
    private synchronized void initialize()
    {
        // if there are no credentials wait with the initialization
        if(!mHasCredentials) {
            return;
        }
        // reset the initialized status
        mInitialized = false;
        // create a new database handler instance
        mDatabaseHandler = new DatabaseHandler(mContext, mUserID);
        //mDatabaseHandler.enableEncryption(mOptions.enableEncryption);
        if(mOptions.enableEncryption != null && mOptions.enableEncryption) {
            // TODO enable encryption on the database. See https://realm.io/docs/java/latest/#encryption
            // mDatabaseHandler.setEncryptionKey(mOptions.encryptionKey);
        }
        // create a new sensor data proxy instance
        mSensorDataProxy = new SensorDataProxy(mOptions.backendEnvironment, mAPPKey, mSessionID);
        // disable the period syncing of the previous data syncer
        if(mDataSyncer != null) {
            mDataSyncer.disablePeriodicSync();
        }
        // create a new data syncer instance
        mDataSyncer = new DataSyncer(mContext, mDatabaseHandler,mSensorDataProxy);
        if(mOptions.localPersistancePeriod != null) {
            mDataSyncer.setPersistPeriod(mOptions.localPersistancePeriod);
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
                mDataSyncerProgress.reset();
                mDataSyncer.sync(mDataSyncerProgress);
                return true;
            } catch(Exception e) {
                Log.e(TAG, "Error doing the initial sync on the DataSyncer", e);
                mDataSyncerProgress.setLastException(e);
               throw e;
            }
        }
    });

    /**
     * Receive an update when the sensors have been downloaded
     * @return A Future<boolean> which will return the status of the sensor download process of the DataStorageEngine.
     */
    public Future<Boolean> onSensorsDownloaded() {
        return mCallBackExecutorService.submit(new Callable<Boolean>() {
            public Boolean call() throws Exception {
                synchronized (mDataSyncerProgress.isDownloadSensorsCompletedMonitor){
                    while(true) {
                        // the data has already been been downloaded
                        if (mDataSyncerProgress.getIsDownloadSensorsCompleted()) {
                            return true;
                        }
                        // it failed, throw the last known exception
                        if (mDataSyncerProgress.getLastException() != null) {
                            throw mDataSyncerProgress.getLastException();
                        }
                        // no update yet, wait for it...
                        mDataSyncerProgress.isDownloadSensorsCompletedMonitor.wait();
                    }
                }
            }
        });
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
        return mCallBackExecutorService.submit(new Callable<Boolean>() {
            public Boolean call() throws Exception {
                synchronized (mDataSyncerProgress.isDownloadSensorDataCompletedMonitor) {
                    while (true) {
                        // the data has already been been downloaded
                        if (mDataSyncerProgress.getIsDownloadSensorDataCompleted()) {
                            return true;
                        }
                        // it failed, throw the last known exception
                        if (mDataSyncerProgress.getLastException() != null) {
                            throw mDataSyncerProgress.getLastException();
                        }
                        // no update yet, wait for it...
                        mDataSyncerProgress.isDownloadSensorDataCompletedMonitor.wait();
                    }
                }
            }
        });
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
}
