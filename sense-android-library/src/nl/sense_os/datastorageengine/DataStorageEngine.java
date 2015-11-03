package nl.sense_os.datastorageengine;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * This class provides the main interface for creating sensors and sources and setting storage engine specific properties.
 * Created by ted@sense-os.nl on 10/29/15.
 */
public class DataStorageEngine {
    private static final String TAG = "DataStorageEngine";
    /** The local database handler */
    private DatabaseHandler mDatabaseHandler;
    /** The instance responsible for syncing with the back-end */
    private DataSyncer mDataSyncer;
    /** The proxy for the actual data transfer with the back-end */
    private SensorDataProxy mSensorDataProxy;
    /** Static singleton instance of the DataStorageEngine */
    private static DataStorageEngine mDataStorageEngine;
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

    private ExecutorService mExecutorService;

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
     * Private constructor of the DataStorageEngine
     * @param context The Android context
     */
    private DataStorageEngine(Context context)
    {
        mContext = context;
        mOptions = new DSEOptions();
        mExecutorService = Executors.newFixedThreadPool(1);
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
     * Get an initialized DataStorageEngine
     * @return an Initialed DataStorageEngine
     * @throws RuntimeException When the DataStorageEngine has not been initialized with a context.
     */
    public static DataStorageEngine getInstance()throws RuntimeException{
        if(mDataStorageEngine == null)
            throw new RuntimeException("The DataStorageEngine should be initialized with a context first");
        return mDataStorageEngine;
    }


    /**
     * Returns enum DSEStatus indicating status of DSE.
     * @return The DSEStatus, this could be either AWAITING_CREDENTIALS, AWAITING_SENSOR_PROFILES, READY.
     **/
    public synchronized DSEStatus getStatus()
    {
        if(!mHasCredentials)
            return DSEStatus.AWAITING_CREDENTIALS;
        else if(mInitialized)
            return DSEStatus.READY;
        else
            return DSEStatus.AWAITING_SENSOR_PROFILES;
    }

    /**
     * Set the options DataStorageOption
     * Options with a null value will be unchanged.
     * @param options The options to set for the DataStorageEngine
     */
    public void setOptions(DSEOptions options)
    {
        DSEOptions oldOptions = mOptions;
        mOptions = options;
        // if the options have change initialize the DSE
        if(oldOptions.equals(options))
            initialize();
    }

    /**
     * Set credentials required in DataStorageEngine
     *
     * SessionID, AppKey for Http requests. userId for table management in the database.
     * @param sessionID The sessionID given on the latest successful login.
     * @param userId The userId of the current logged in user.
     * @param appKey The appKey of the application using DSE.
     * @throws IllegalArgumentException When one of the arguments is empty
     **/
    public void setCredentials(String sessionID, String userId, String appKey) throws IllegalArgumentException
    {
        if(sessionID == null || sessionID.length() == 0 )
            throw new IllegalArgumentException("missing sessionID");
        if(userId == null || userId.length() == 0 )
            throw new IllegalArgumentException("missing userID");
        if(appKey == null || appKey.length() == 0 )
            throw new IllegalArgumentException("missing appKey");

        boolean credentialsUpdated = false;
        if(!mUserID.equals(userId) || !mSessionID.equals(sessionID) || !mAPPKey.equals(appKey))
            credentialsUpdated =true;

        mUserID = userId;
        mSessionID = sessionID;
        mAPPKey = appKey;

        // the credentials have been set, start the initialization
        mHasCredentials = true;
        if(credentialsUpdated)
            initialize();
    }

    /**
     * Initialized the DataStorageEngine
     * This will initialize the DSE if there are credentials available.
     */
    private synchronized void initialize()
    {
        // if there are no credentials wait with the initialization
        if(!mHasCredentials)
            return;

        mDatabaseHandler = new DatabaseHandler(mContext, mUserID);
        // TODO enable encryption on the database
        mSensorDataProxy = new SensorDataProxy(mOptions.backendEnvironment, mAPPKey, mSessionID);
        if(mDataSyncer != null)
            mDataSyncer.disablePeriodicSync();
        mDataSyncer = new DataSyncer(mContext, mDatabaseHandler,mSensorDataProxy);
        // TODO set the persist period in the DataSyncer
        long persistPeriod = mOptions.localPersistancePeriod;
        try {
            // execute the initialization of the DataSyncer asynchronously
            mExecutorService.execute(mInitTask);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing the DataSyncer", e);
        }
    }

    /**
     * The FutureTask that performs the DataSyncer initialization
     */
    private FutureTask<Boolean> mInitTask = new FutureTask<>(new Callable<Boolean>() {
        public Boolean call() throws Exception {
            try {
                // TODO what to do when there is no internet?
                mDataSyncer.initialize();
                mExecutorService.execute(mDownloadSensorsTask);
                mInitialized = true;
                return true;
            }catch(Exception e)
            {
                Log.e(TAG, "Error initializing the DataSyncer", e);
                return false;
            }
        }
    });

    /**
     * The FutureTask that performs the DataSyncer getSensors
     */
    private FutureTask<Boolean> mDownloadSensorsTask = new FutureTask<>(new Callable<Boolean>() {
        public Boolean call() throws Exception {
            try {
                // TODO what to do when there is no internet?
                // TODO what to do when there is already a sync
                mDataSyncer.sync();
                return true;
            }catch(Exception e)
            {
                Log.e(TAG, "Error doing the initial sync on the DataSyncer", e);
                return false;
            }
        }
    });

    /**
     * The FutureTask that performs the DataSyncer get sensor data
     */
    private FutureTask<Boolean> mDownloadSensorDataTask = new FutureTask<>(new Callable<Boolean>() {
        public Boolean call() throws Exception {
            try {
                // TODO what to do when there is no internet?
                // TODO what to do when there is already a sync
                mDataSyncer.sync();
                return true;
            }catch(Exception e)
            {
                Log.e(TAG, "Error doing the initial sync on the DataSyncer", e);
                return false;
            }
        }
    });

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
    public Sensor createSensor(String source, String name, SensorOptions options) throws DatabaseHandlerException, SensorException, IllegalStateException{
        if(getStatus() != DSEStatus.READY)
            throw new IllegalStateException("The DataStorageEngine is not ready yet");
        return mDatabaseHandler.createSensor(source, name, options);
    }

    /**
     * Returns a specific sensor by name and the source it belongs to
     * @param source The name of the source
     * @param sensorName The name of the sensor
     * @throws IllegalStateException when the DataStorageEngine is not ready yet
     **/
    public Sensor getSensor(String source, String sensorName){
        if(getStatus() != DSEStatus.READY)
            throw new IllegalStateException("The DataStorageEngine is not ready yet");
        return mDataStorageEngine.getSensor(source, sensorName);
    }

    /**
     * Returns all the sensors connected to the given source
     * @return List<Sensor> The sensors connected to the given source
     * @throws IllegalStateException when the DataStorageEngine is not ready yet
     **/
    public List<Sensor> getSensors(String source){
        if(getStatus() != DSEStatus.READY)
            throw new IllegalStateException("The DataStorageEngine is not ready yet");
        return mDataStorageEngine.getSensors(source);
    }

    /**
     * Returns all the sources attached to the current user
     * @return List<String> The sources attached to the current user
     * @throws IllegalStateException when the DataStorageEngine is not ready yet
     **/
    public List<String> getSources(){
        if(getStatus() != DSEStatus.READY)
            throw new IllegalStateException("The DataStorageEngine is not ready yet");
        return mDataStorageEngine.getSources();
    }

    /**
     * Delete data from Local Storage and Common Sense.
     * DataPoints will be immediately removed locally, and an event (class DeletionCriteria)
     * is scheduled for the next synchronization round to delete them from Common Sense.
     * @param startTime The start time in epoch milliseconds
     * @param endTime The start time in epoch milliseconds
     * @throws IllegalStateException when the DataStorageEngine is not ready yet
     **/
    public void deleteDataPoints(Long startTime, Long endTime) throws DatabaseHandlerException {
        if(getStatus() != DSEStatus.READY)
            throw new IllegalStateException("The DataStorageEngine is not ready yet");
        for(String source : getSources()){
            for(Sensor sensor : getSensors(source)){
                sensor.deleteDataPoints(new QueryOptions(startTime, endTime, null, null, null));
            }
        }
    }

    /**
     * Flushes the local data to Common Sense asynchronously
     * @return A future which will return the status of the flush action as Boolean via Future.get()
     */
    public Future<Boolean> flushDataAsync() {
        return mExecutorService.submit(new Callable<Boolean>() {
            public Boolean call() throws Exception {
                try {
                    if(getStatus() != DSEStatus.READY)
                        throw new IllegalStateException("The DataStorageEngine is not ready yet");

                    mDataSyncer.sync();
                    return true;
                }catch(Exception e)
                {
                    Log.e(TAG, "Error flushing data", e);
                    return false;
                }
            }
        });
    }

    /**
     * Flushes the local data to Common Sense synchronously
     * It uses a blocking flush and performs network operations on the current thread
     * @throws JSONException, DatabaseHandlerException, SensorException, IOException, SensorProfileException when the data flush fails
     * @throws IllegalStateException when the DataStorageEngine is not ready yet
     */
    public void flushData() throws JSONException, DatabaseHandlerException, SensorException, IOException, SensorProfileException {
        if(getStatus() != DSEStatus.READY)
            throw new IllegalStateException("The DataStorageEngine is not ready yet");
        mDataSyncer.sync();
    }

    /**
     * Receive an update when the DataStorageEngine has done it's initialization
     * @return A Future<boolean> which will return the status of the initialization of the DataStorageEngine.
     */
    public Future<Boolean> onInitialized()
    {
        return mInitTask;
    }

    /**
     * Receive an update when the sensors have been downloaded
     * @return A Future<boolean> which will return the status of the sensor download process of the DataStorageEngine.
     */
    public Future<Boolean> onSensorsDownloaded()
    {
        return mDownloadSensorsTask;
    }

    /**
     * Receive an update when the sensor data has been downloaded
     * @return A Future<boolean> which will return the status of the sensor data download process of the DataStorageEngine.
     */
    public Future<Boolean> onSensorDataDownloaded()
    {
        return mDownloadSensorDataTask;
    }
}
