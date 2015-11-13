package nl.sense_os.datastorageengine;

/**
 * Created by ted on 10/30/15.
 */
public class DSEConfig {
    /**
     * The number of milliseconds between the upload sessions
     * The default is DSEConstants.SYNC_RATE
     */
    public Integer uploadInterval = null;
    /**
     * The number of seconds to persist the data locally
     * The default value is DSEConstants.PERSIST_PERIOD
     */
    public Long localPersistancePeriod = null;
    /**
     * Enable data encryption for the local storage
     * The default behavior is to encrypt the local storage
     **/
    public Boolean enableEncryption = null;
    /**
     * The back-end environment to select
     * The default environment will be selected which is SensorDataProxy.SERVER.LIVE
     */
    public SensorDataProxy.SERVER backendEnvironment = null;

    /** Ephemeral DSE credentials */
    private String mUserID = "";
    private String mSessionID = "";
    private String mAPPKey = "";

    /**
     * Create a DataStorageEngine configuration object
     *
     * @param sessionID The session id of the user
     * @param userId The user id
     * @param appKey The application key
     * @throws IllegalArgumentException
     */
    public DSEConfig(String sessionID, String userId, String appKey) throws IllegalArgumentException {
        if (sessionID == null || sessionID.length() == 0) {
            throw new IllegalArgumentException("missing sessionID");
        }
        if (userId == null || userId.length() == 0) {
            throw new IllegalArgumentException("missing userID");
        }
        if (appKey == null || appKey.length() == 0) {
            throw new IllegalArgumentException("missing appKey");
        }

        mUserID = userId;
        mSessionID = sessionID;
        mAPPKey = appKey;
    }

    /**
     * Gets the user id
     * @return the user id string
     */
    public String getUserID(){
        return mUserID;
    }

    /**
     * Gets the session id
     * @return the session id string
     */
    public String getSessionID(){
        return mSessionID;
    }

    /**
     * Gets the application id
     * @return the application id string
     */
    public String getAPPKey(){
        return mAPPKey;
    }


    @Override
    public boolean equals(Object o) {
        if(uploadInterval != ((DSEConfig)o).uploadInterval)
            return false;
        if(localPersistancePeriod != ((DSEConfig)o).localPersistancePeriod)
            return false;
        if(enableEncryption != ((DSEConfig)o).enableEncryption)
            return false;
        if(backendEnvironment != ((DSEConfig)o).backendEnvironment)
            return false;
        if(!mUserID.equals(((DSEConfig)o).mUserID))
            return false;
        if(!mSessionID.equals(((DSEConfig)o).mSessionID))
            return false;
        if(!mAPPKey.equals(((DSEConfig)o).mAPPKey))
            return false;
        return true;
    }
}
