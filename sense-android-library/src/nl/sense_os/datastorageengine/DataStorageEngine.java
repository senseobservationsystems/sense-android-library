package nl.sense_os.datastorageengine;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides the main interface for creating sensors and sources and setting storage engine specific properties.

 All the Data Storage Engine updates are delivered to the associated delegate object, which is a custom object that you provide. For information about the delegate methods you use to receive events, see DataStorageEngineDelegate interface/protocol.
 *
 * Created by ted@sense-os.nl on 10/29/15.
 */
public class DataStorageEngine {
    private DatabaseHandler mDatabaseHandler;
    private DataSyncer mDataSyncer;
    private static DataStorageEngine mDataStorageEngine;
    private SensorDataProxy mSensorDataProxy;
    private Context mContext;
    private SharedPreferences mSharedPreferences;
    private boolean mHasCredentials;
    private DSEOptions mOptions;
    private String mUserID;
    private String mSessionID;
    private String mAPPKey;
    private List<DataStorageEngineDelegate> mDelegates;

    private final static String PREFERENCE_NAME = "data_storage_engine";
    /** the key for the session_id preference */
    final static String PREF_KEY_SESSION_ID = "session_id";
    /** the key for the user_id preference */
    final static String PREF_KEY_USER_ID = "user_id";
    /** the key for the app_key preference */
    final static String PREF_KEY_APP_KEY = "app_key";


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

    private DataStorageEngine(Context context)
    {
        mContext = context;
        mOptions = new DSEOptions();
        mDelegates = new ArrayList();
        initialize();
    }

    /**
     * Creates a singleton DataStorageEngine instance if it does not exists already
     * @param context An Android context
     * @return a static instance of the DataStorageEngine
     */
    public synchronized static DataStorageEngine DataStorageEngine(Context context)
    {
        if(mDataStorageEngine == null) {
            mDataStorageEngine = new DataStorageEngine(context);
        }
        return mDataStorageEngine;
    }

    /**
     * Returns enum DSEStatus indicating status of DSE.
     * The value could be either AWAITING_CREDENTIALS, AWAITING_SENSOR_PROFILES, READY.
     **/
    public DSEStatus getStatus()
    {
        if(!hasCredentials())
            return DSEStatus.AWAITING_CREDENTIALS;
        //else if(mDataSyncer.)
        return DSEStatus.READY;
    }

    /**
     * Set the options DataStorageOption
     * Options with a null value will be unchanged.
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
     * Set credentials required in DSE.
     *
     * SessionID, AppKey for Http requests. userId for table management in the database.
     * @param sessionID The sessionID given on the latest successful login.
     * @param userId The userId of the current logged in user.
     * @param appKey The appKey of the application using DSE.
     *
     **/
    public void setCredentials(String sessionID, String userId, String appKey) throws IllegalAccessException
    {
        if(sessionID == null || sessionID.length() == 0 )
            throw new IllegalArgumentException("missing sessionID");
        if(userId == null || userId.length() == 0 )
            throw new IllegalArgumentException("missing userID");
        if(appKey == null || appKey.length() == 0 )
            throw new IllegalArgumentException("missing appKey");

        mUserID = userId;
        mSessionID = sessionID;
        mAPPKey = appKey;

        // the credentials have been set, start the initialization
        mHasCredentials = true;
        initialize();
    }

    /**
     * Initialized the DataStorageEngine
     * This will initialize the DSE if there are credentials available.
     */
    private void initialize()
    {
        // if there are no credentials wait with the initialization
        if(!hasCredentials())
            return;

        long persistPeriod = mOptions.localPersistancePeriod;
        mDatabaseHandler = new DatabaseHandler(mContext, mUserID);
        // TODO enable encryption on the database
        mSensorDataProxy = new SensorDataProxy(mOptions.backendEnvironment, mAPPKey, mSessionID);
        // TODO disable periodic sync
        if(mDataSyncer != null)
            mDataSyncer.disablePeriodicSync();
        // TODO enable/disable encryption
        mDataSyncer = new DataSyncer(mContext, mDatabaseHandler,mSensorDataProxy);
        // TODO set the persist period in the DataSyncer
    }

    /**
     * Check whether the credentials have been set.
     * @return If the credentials have been set, it returns true, otherwise false.
     */
    boolean hasCredentials(){
        return mHasCredentials;
    }

    SharedPreferences getSharedPreferences(){
        if(mSharedPreferences == null)
            mSharedPreferences = mContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return mSharedPreferences;
    }

    /**
     * Create a new sensor in database and backend if it does not already exist. Throw exception if it already exists. If it has been created, return the object.
     * an object.
     * @param source The source name (e.g accelerometer)
     * @param name The sensor name (e.g accelerometer)
     * @param options The sensor options
     * @return sensor object
     **/
    public Sensor createSensor(String source, String name, SensorOptions options) throws DatabaseHandlerException, SensorException {
        return mDatabaseHandler.createSensor(source, name, options);
    }

    /**
     * Returns a specific sensor by name and the source it belongs to
     * @param source The name of the source
     * @param sensorName The name of the sensor
     **/
    public Sensor getSensor(String source, String sensorName){
        return mDataStorageEngine.getSensor(source, sensorName);
    }

    /**
     * Returns all the sensors connected to the given source
     * @return List<Sensor> The sensors connected to the given source
     **/
    public List<Sensor> getSensors(String source){
        return mDataStorageEngine.getSensors(source);
    }

    /**
     * Returns all the sources attached to the current user
     * @return List<String> The sources attached to the current user
     **/
    public List<String> getSources(){
        return mDataStorageEngine.getSources();
    }

    /**
     * Delete data from Local Storage and Common Sense.
     * DataPoints will be immediately removed locally, and an event (class DeletionCriteria)
     * is scheduled for the next synchronization round to delete them from Common Sense.
     * @param startTime The start time in epoch milliseconds
     * @param endTime The start time in epoch milliseconds
     *
     **/
    public void deleteDataPoints(Long startTime, Long endTime) throws DatabaseHandlerException {
        for(String source : getSources()){
            for(Sensor sensor : getSensors(source)){
                sensor.deleteDataPoints(new QueryOptions(startTime, endTime, null, null, null));
            }
        }
    }

    /**
     * Flushes the local data to Common Sense
     * The results will be returned via the DataStorageEngineDelegate in onFlushCompleted
     */
    public void flushData()
    {
        // TODO implement upload with status result via the DataStoreEngineDelegate
        //mDataSyncer.uploadToRemote();
    }

    /**
     * Adds a DataStorageEngineDelegate to the lists of delegates to receive updates about the status of the DataStorageEngine
     * @param delegate The DataStorageEngineDelegate to receive updates in
     */
    public void addDelegate(DataStorageEngineDelegate delegate)
    {
        if(!mDelegates.contains(delegate))
            mDelegates.add(delegate);
    }

    /**
     * Remove a DataStorageEngineDelegate from the list of delegates to receive updates about the status of the DataStorageEngine
     * @param delegate The DataStorageEngineDelegate to remove from the list
     */
    public void removeDelegate(DataStorageEngineDelegate delegate)
    {
        if(mDelegates.contains(delegate))
            mDelegates.remove(delegate);
    }

    /**
     * Get the delegates
     * @return The registered DataStorageEngine delegates
     */
    List<DataStorageEngineDelegate> getDelegate()
    {
        return mDelegates;
    }
}
