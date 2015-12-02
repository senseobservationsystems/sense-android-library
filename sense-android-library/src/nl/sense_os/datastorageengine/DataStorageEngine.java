package nl.sense_os.datastorageengine;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import nl.sense_os.service.R;
import nl.sense_os.util.json.EncryptionHelper;
import nl.sense_os.util.json.SchemaException;
import nl.sense_os.util.json.ValidationException;

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
    private static DataStorageEngine mDataStorageEngine;
    /** Context needed for the DataSyncer */
    private Context mContext;
    /** Ephemeral initialization status */
    private boolean mInitialized;
    /** DSE configuration */
    private DSEConfig mDSEConfig;

    /** Handles data syncer actions asynchronously */
    private ExecutorService mDataSyncerExecutorService;
    /** Handles the AsyncCallback function calls */
    private ExecutorService mCallBackExecutorService;

    /** The shared preference file name */
    private String SHARED_PREFERENCES_FILE = "data_storage_engine";
    /** The shared preferences keys */
    private String PREFERENCES_APP_KEY  = "app_key";
    private String PREFERENCES_USER_ID = "user_id";
    private String PREFERENCES_SESSION_ID = "session_id";
    private String PREFERENCES_ENABLE_ENCRYPTION = "enable_encryption";
    private String PREFERENCES_BACKEND_ENV = "backend_environment";
    private String PREFERENCES_LOCAL_PERSISTANCE_PERIOD = "local_persistance_period";
    private String PREFERENCES_UPLOAD_INTERVAL = "upload_interval";
    private String PREFERENCES_ENABLE_SYNC = "enable_sync";

    // The default sync rate, should be moved to a constants class
    public final static long SYNC_RATE = 1800000;  // 30 minutes in milliseconds by default
    private FutureTask<Boolean> mInitTask;

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
    private DataStorageEngine(Context context)
    {
        mContext = context;
        mDataSyncerExecutorService = Executors.newSingleThreadExecutor();
        mCallBackExecutorService = Executors.newCachedThreadPool();
        mInitTask = getInitializeTask();
        loadConfiguration();
        initialize();
    }

    /**
     * Loads the latest configuration from the shared preferences in the DSEConfig member
     */
    private void loadConfiguration()
    {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(SHARED_PREFERENCES_FILE, mContext.MODE_PRIVATE);

        if(!sharedPreferences.contains(PREFERENCES_USER_ID)) {
            return;
        }
        Boolean isEncrypted = sharedPreferences.getBoolean(PREFERENCES_ENABLE_ENCRYPTION, false);
        String userID = sharedPreferences.getString(PREFERENCES_USER_ID, "");
        String appKEY = sharedPreferences.getString(PREFERENCES_APP_KEY, "");
        String sessionID = sharedPreferences.getString(PREFERENCES_SESSION_ID, "");
        if(isEncrypted) {
            String encryptionKey = mContext.getString(R.string.dse_encryption_key);
            EncryptionHelper encryptor = new EncryptionHelper(mContext, encryptionKey);
            userID = encryptor.decrypt(userID);
            appKEY = encryptor.decrypt(appKEY);
            sessionID = encryptor.decrypt(sessionID);
        }
        mDSEConfig = new DSEConfig(sessionID, userID, appKEY);
        if(sharedPreferences.contains(PREFERENCES_BACKEND_ENV)){
            String env = sharedPreferences.getString(PREFERENCES_BACKEND_ENV, "");
            mDSEConfig.backendEnvironment = SensorDataProxy.SERVER.valueOf(env);
        }

        if(sharedPreferences.contains(PREFERENCES_ENABLE_ENCRYPTION)){
            mDSEConfig.enableEncryption = sharedPreferences.getBoolean(PREFERENCES_ENABLE_ENCRYPTION, false);
        }

        if(sharedPreferences.contains(PREFERENCES_LOCAL_PERSISTANCE_PERIOD)){
            mDSEConfig.localPersistancePeriod = sharedPreferences.getLong(PREFERENCES_LOCAL_PERSISTANCE_PERIOD, 0);
        }

        if(sharedPreferences.contains(PREFERENCES_UPLOAD_INTERVAL)){
            mDSEConfig.uploadInterval = sharedPreferences.getLong(PREFERENCES_UPLOAD_INTERVAL, 0l);
        }

        if(sharedPreferences.contains(PREFERENCES_ENABLE_SYNC)){
            mDSEConfig.enableSync = sharedPreferences.getBoolean(PREFERENCES_ENABLE_SYNC, true);
        }
    }

    /**
     * Stores the DSEConfig member data to the shared preferences
     */
    private void saveConfiguration()
    {
        if(mDSEConfig == null)
            return;

        SharedPreferences sharedPreferences = mContext.getSharedPreferences(SHARED_PREFERENCES_FILE, mContext.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String userID =  mDSEConfig.getUserID();
        String appKEY = mDSEConfig.getAPPKey();
        String sessionID = mDSEConfig.getSessionID();

        if(mDSEConfig.enableEncryption != null && mDSEConfig.enableEncryption){
            String encryptionKey = mContext.getString(R.string.dse_encryption_key);
            EncryptionHelper encryptor = new EncryptionHelper(mContext, encryptionKey);
            userID = encryptor.encrypt(userID);
            appKEY = encryptor.encrypt(appKEY);
            sessionID = encryptor.encrypt(sessionID);
        }

        editor.putString(PREFERENCES_USER_ID, userID);
        editor.putString(PREFERENCES_APP_KEY, appKEY);
        editor.putString(PREFERENCES_SESSION_ID, sessionID);
        if(mDSEConfig.backendEnvironment != null) {
            editor.putString(PREFERENCES_BACKEND_ENV, mDSEConfig.backendEnvironment.name());
        }
        if(mDSEConfig.enableEncryption != null) {
            editor.putBoolean(PREFERENCES_ENABLE_ENCRYPTION, mDSEConfig.enableEncryption);
        }
        if(mDSEConfig.localPersistancePeriod != null) {
            editor.putLong(PREFERENCES_LOCAL_PERSISTANCE_PERIOD, mDSEConfig.localPersistancePeriod);
        }
        if(mDSEConfig.uploadInterval != null) {
            editor.putLong(PREFERENCES_UPLOAD_INTERVAL, mDSEConfig.uploadInterval);
        }
        if(mDSEConfig.enableSync != null) {
            editor.putBoolean(PREFERENCES_ENABLE_ENCRYPTION, mDSEConfig.enableSync);
        }
        editor.commit();
    }

    /**
     * Creates a singleton DataStorageEngine instance if it does not exists already
     * @param context An Android context
     * @return a static instance of the DataStorageEngine
     */
    public synchronized static DataStorageEngine getInstance(Context context)
    {
        if(mDataStorageEngine == null) {
            mDataStorageEngine = new DataStorageEngine(context);
        }
        return mDataStorageEngine;
    }

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
     * This will override all the current configuration settings
     * @param dseConfig The configuration properties to set
     **/
    public synchronized void setConfig(DSEConfig dseConfig)
    {
        // TODO retrieve the credentials from the shared preferences (use a listener) when the AccountManager is implemented
        if(dseConfig == null || (mDSEConfig != null && dseConfig.equals(mDSEConfig))) {
           return;
        }

        // the credentials have been set, start the initialization
        mDSEConfig = dseConfig;
        saveConfiguration();
        initialize();
    }

    /**
     * Returns the current configuration properties
     * @return The cloned DSEConfig object
     */
    public DSEConfig getConfig()
    {
        if(mDSEConfig == null){
            return null;
        }
        return mDSEConfig.clone();
    }

    /**
     * Clears the configuration and creates a new uninitialized DataStorageEngine instance
     */
    public synchronized void clearConfig()
    {
        // clear the shared preferences configuration
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(SHARED_PREFERENCES_FILE, mContext.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();
        // disable the periodic data syncing
        disablePeriodicSync();
        mDSEConfig = null;
        mInitialized = false;
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
        if(mDSEConfig.enableEncryption != null && mDSEConfig.enableEncryption) {
            // create a new database handler instance with encryption enabled
            String encryptionKey = mContext.getString(R.string.dse_encryption_key);
            mDatabaseHandler = new DatabaseHandler(mContext, encryptionKey.getBytes(), mDSEConfig.getUserID());
        }else {
            // create a new database handler instance without encryption
            mDatabaseHandler = new DatabaseHandler(mContext, mDSEConfig.getUserID());
        }
        // create a new sensor data proxy instance
        mSensorDataProxy = new SensorDataProxy(mDSEConfig.backendEnvironment, mDSEConfig.getAPPKey(), mDSEConfig.getSessionID());

        // create a new DataSyncer instance
        mDataSyncer = new DataSyncer(mContext, mDatabaseHandler, mSensorDataProxy);
        if(mDSEConfig.localPersistancePeriod != null) {
            mDataSyncer.setPersistPeriod(mDSEConfig.localPersistancePeriod);
        }
        mInitTask.cancel(true);
        // create a new FutureTask if we already used the mIinitTask
        if(mInitTask.isCancelled() || mInitTask.isDone()){
            mInitTask = getInitializeTask();
        }
        // execute the initialization of the DataSyncer asynchronously
        mDataSyncerExecutorService.execute(mInitTask);
    }

    /**
     * The FutureTask that performs the DataSyncer initialization (sensor profile downloading)
     * When this task is successful then the status of the DSE will be READY
     */
    private FutureTask<Boolean> getInitializeTask() {
        return new FutureTask<>(new Callable<Boolean>() {
            public Boolean call() throws JSONException, IOException, SensorProfileException, SchemaException, DatabaseHandlerException, SensorException, ValidationException {
                try {
                    // reset the progress tracker status
                    mDataSyncerProgressTracker.reset();
                    mDataSyncer.initialize();
                    // set the status and notify the listeners
                    synchronized (mDataSyncerProgressTracker) {
                        mInitialized = true;
                        // notify the AsyncCallbacks
                        mDataSyncerProgressTracker.onInitializeCompleted();
                    }
                    // when the initialization is done enable the periodic syncing to download the sensor and sensor data
                    if(mDSEConfig.enableSync == null || mDSEConfig.enableSync == true) {
                        if (mDSEConfig.uploadInterval != null) {
                            enablePeriodicSync(mDSEConfig.uploadInterval);
                        } else {
                            enablePeriodicSync();
                        }
                    }
                    return true;
                } catch (Exception e) {
                    // notify the AsyncCallbacks
                    mDataSyncerProgressTracker.onException(e);
                    Log.e(TAG, "Error initializing the DataSyncer", e);
                    throw e;
                }
            }
        });
    }

    /**
     * Receive an update when the DataStorageEngine has done it's initialization
     * Returns a value immediately when the initialization is already done
     * @return A Future<boolean> which will return the status of the initialization of the DataStorageEngine.
     * @throws JSONException
     * @throws IOException
     * @throws SensorProfileException
     */
    public Future<Boolean> onReady() {
        return mDataSyncerExecutorService.submit(new Callable<Boolean>() {
            public Boolean call() throws InterruptedException {
                final Object monitor = new Object();
                // otherwise wait for the async callback
                onReady(new AsyncCallback() {
                            @Override
                            public void onSuccess() {
                                synchronized (monitor) {
                                    monitor.notifyAll();
                                }
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                synchronized (monitor) {
                                    monitor.notifyAll();
                                }
                            }
                        }
                );

                // wait for the notification
                synchronized (monitor) {
                    // if the DSE is ready return true
                    if (getStatus() == DSEStatus.READY) {
                        return true;
                    }
                    // else wait
                    monitor.wait();

                    // got the notification
                    if (DataStorageEngine.this.getStatus() == DSEStatus.READY) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        });
    }

    /**
     * Receive a one time notification when the DataStorageEngine has done it's initialization
     * Sends a notification immediately when the initialization is already done
     * @param asyncCallback The Asynchronous Callback to receive the status of the initialization in
     */
    public void onReady(AsyncCallback asyncCallback) {
        synchronized (mDataSyncerProgressTracker) {
            // if the DSE is ready then return immediately
            if(getStatus() == DSEStatus.READY){
                asyncCallback.onSuccess();
            }else {
                // add to the callback queue
                mDataSyncerProgressTracker.addReadyCallbackQueue(asyncCallback);
            }
        }
    }
    /**
     * Callback invoked for errors during initialization and the a periodic sync.
     * Will send all errors which have occurred since registering the callback
     * @param errorCallback The callback to receive errors on.
     **/
    public void registerOnError(ErrorCallback errorCallback)
    {
        mDataSyncerProgressTracker.addErrorCallbacks(errorCallback);
    }

    /**
     * Unregister the on error callback for errors during initialization and the a periodic sync.
     * @param errorCallback
     **/
    public void unRegisterOnError(ErrorCallback errorCallback)
    {
        mDataSyncerProgressTracker.removeErrorCallbacks(errorCallback);
    }

    /**
     * Receive a one time notification when the sensors have been downloaded
     * @param asyncCallback The AsynchronousCall which will return the status of the sensor download process of the DataStorageEngine.
     */
    public void onSensorsDownloaded(AsyncCallback asyncCallback)
    {
        if (mDataSyncerProgressTracker.isDownloadSensorsCompleted()) {
            asyncCallback.onSuccess();
        } else {
            mDataSyncerProgressTracker.addDownloadSensorsCallbackQueue(asyncCallback);
        }
    }



    /**
     * Receive a one time notification when the sensor data has been downloaded
     * @param asyncCallback The AsynchronousCall which will return the status of the sensor data download process of the DataStorageEngine.
     */
    public void onSensorDataDownloaded(AsyncCallback asyncCallback)
    {
        if (mDataSyncerProgressTracker.isDownloadSensorsCompleted()) {
            asyncCallback.onSuccess();
        } else {
            // send the last exception
            mDataSyncerProgressTracker.addDownloadSensorDataCallbackQueue(asyncCallback);
        }
    }

    /**
     * Synchronizes the local and remote data
     * @return A future which will return the status of the data synchronization action as Boolean or Exception via Future.get()
     * @throws IOException
     * @throws DatabaseHandlerException
     * @throws SensorException
     * @throws SensorProfileException
     * @throws JSONException
     * @throws SchemaException
     * @throws ValidationException
     */
    public synchronized Future<Boolean> syncData() {
        return mDataSyncerExecutorService.submit(new Callable<Boolean>() {
            public Boolean call() throws IOException, DatabaseHandlerException, SensorException, SensorProfileException,
                    JSONException, SchemaException, ValidationException {
                try {
                    if (getStatus() != DSEStatus.READY) {
                        throw new IllegalStateException("The DataStorageEngine is not ready yet");
                    }
                    mDataSyncer.sync(mDataSyncerProgressTracker);
                    return true;
                } catch (Exception e) {
                    mDataSyncerProgressTracker.onException(e);
                    Log.e(TAG, "Error syncing data", e);
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
    protected void getResultAsync(final Future<Boolean> future, final AsyncCallback callback) {
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
    private Sensor createSensor(String source, String name, SensorOptions options) throws SensorProfileException, SchemaException, SensorException, DatabaseHandlerException, JSONException {
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
        Sensor sensor;
        try {
            sensor = mDatabaseHandler.getSensor(source, sensorName);
        }catch(DatabaseHandlerException ex){
            // the sensor is not yet created, create the sensor
            sensor = createSensor(source, sensorName, DefaultSensorOptions.getSensorOptions(mContext, sensorName));
        }
        return sensor;
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

    /**
     * Returns the sensor names which are available in the sensor profiles
     * @return A set with the sensor names available for data storage
     */
    public Set<String> getSensorNames() throws SensorProfileException, JSONException {
        if (getStatus() != DSEStatus.READY) {
            throw new IllegalStateException("The DataStorageEngine is not ready yet");
        }
        return mDataSyncer.getSensorProfilesSensorNames();
    }

    private class DataSyncerProgress implements ProgressCallback {
        private boolean mIsDownloadSensorsCompleted;
        private boolean mIsDownloadSensorDataCompleted;
        private boolean mIsReady;
        // TODO refactor future queue to callback queue
        private Queue<AsyncCallback> mDownloadSensorsCallbackQueue = new ConcurrentLinkedQueue<>();
        private Queue<AsyncCallback> mDownloadSensorDataCallbackQueue = new ConcurrentLinkedQueue<>();
        private Queue<AsyncCallback> mReadyCallbackQueue = new ConcurrentLinkedQueue<>();
        private ArrayList<ErrorCallback> mErrorCallbacks = new ArrayList<>();
        private Exception mLastException = null;

        public synchronized void addDownloadSensorsCallbackQueue(AsyncCallback asyncCallback){
            if(!mDownloadSensorsCallbackQueue.contains(asyncCallback)) {
                mDownloadSensorsCallbackQueue.add(asyncCallback);
            }
        }

        public synchronized void addDownloadSensorDataCallbackQueue(AsyncCallback asyncCallback){
            if(!mDownloadSensorDataCallbackQueue.contains(asyncCallback)) {
                mDownloadSensorDataCallbackQueue.add(asyncCallback);
            }
        }

        public synchronized void addReadyCallbackQueue(AsyncCallback asyncCallback){
            if(!mReadyCallbackQueue.contains(asyncCallback)) {
                mReadyCallbackQueue.add(asyncCallback);
            }
        }

        public synchronized void addErrorCallbacks(ErrorCallback errorCallback){
            if(!mErrorCallbacks.contains(errorCallback)) {
                mErrorCallbacks.add(errorCallback);
            }
        }

        public synchronized void removeErrorCallbacks(ErrorCallback errorCallback){
            if(!mErrorCallbacks.contains(errorCallback)) {
                mErrorCallbacks.remove(errorCallback);
            }
        }

        public synchronized void setLastException(Exception exception){
            mLastException = exception;
        }

        public synchronized boolean isDownloadSensorsCompleted(){
            return mIsDownloadSensorsCompleted;
        }

        public synchronized boolean isDownloadSensorDataCompleted(){
            return mIsDownloadSensorDataCompleted;
        }

        public synchronized boolean isReady(){
            return mIsReady;
        }

        /**
         * Reset the progress status of the DataSyncer
         */
        public synchronized void reset(){
            mIsDownloadSensorDataCompleted = false;
            mIsDownloadSensorsCompleted = false;
            mIsReady = false;
            mLastException = null;
        }

        /** Function to call when an exception is thrown when executing a DataSyncer function with ProgressCallback*/
        public synchronized void onException(Exception e){
            mLastException = e;

            // process all the futures
            for(ErrorCallback errorCallback: mErrorCallbacks){
                if(errorCallback != null){
                    // TODO check if a new thread should be created for a better flow
                    errorCallback.onError(mLastException);
                }
            }
            for(AsyncCallback callback : mDownloadSensorsCallbackQueue){
                callback.onFailure(e);
            }
            for(AsyncCallback callback : mDownloadSensorDataCallbackQueue){
                callback.onFailure(e);
            }
        }

        @Override
        public synchronized void onDownloadSensorsCompleted() {
            mIsDownloadSensorsCompleted = true;
            for (AsyncCallback callback : mDownloadSensorsCallbackQueue) {
                callback.onSuccess();
            }
            mDownloadSensorsCallbackQueue.clear();
        }

        public synchronized void onInitializeCompleted(){
            mIsReady = true;
            for (AsyncCallback callback : mReadyCallbackQueue) {
                callback.onSuccess();
            }
            mReadyCallbackQueue.clear();
        }

        @Override
        public synchronized void onDownloadSensorDataCompleted() {
            mIsDownloadSensorDataCompleted = true;
            for (AsyncCallback callback : mDownloadSensorDataCallbackQueue) {
                callback.onSuccess();
            }
            mDownloadSensorDataCallbackQueue.clear();
        }

        @Override
        public void onDeletionCompleted() {}

        @Override
        public void onUploadCompeted() {}

        @Override
        public void onCleanupCompleted() {}
    }

    /**
     * Enables the periodic data synchronization with the back-end
     * @param syncRate The amount of milliseconds between each synchronization
     */
    public void enablePeriodicSync(long syncRate){
        PeriodicDataSyncer.setAlarm(mContext, syncRate);
    }

    /**
     * Enables the periodic data synchronization with the back-end using the default sync rate #SYNC_RATE
     */
    public void enablePeriodicSync(){
        enablePeriodicSync(SYNC_RATE);
    }

    /**
     * Disables the periodic data synchronization with the back-end
     */
    public void disablePeriodicSync(){
        PeriodicDataSyncer.cancelAlarm(mContext);
    }
}
