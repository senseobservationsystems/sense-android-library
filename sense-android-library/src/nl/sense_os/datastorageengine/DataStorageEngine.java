package nl.sense_os.datastorageengine;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by ted on 10/29/15.
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
     *
     **/
    DSEStatus getStatus()
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
    void setOptions(DSEOptions options)
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
        //if(mDataSyncer != null)
        //    mDataSyncer.disablePeriodicSync();
        // TOODO enable/disable encryption
        mDataSyncer = new DataSyncer(mContext, mUserID, mSensorDataProxy, persistPeriod);
    }

    /**
     * Check whether the credentials have been set.
     * @return If the credentials have been set, it returns true, otherwise false.
     */
    public boolean hasCredentials(){
        return mHasCredentials;
    }

    SharedPreferences getSharedPreferences(){
        if(mSharedPreferences == null)
            mSharedPreferences = mContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return mSharedPreferences;
    }
}
