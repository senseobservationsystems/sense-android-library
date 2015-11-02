package nl.sense_os.datastorageengine;

/**
 * Created by ted on 10/30/15.
 */
public class DSEOptions {
    /**
     * The amount of milliseconds between the upload sessions
     * The default is DSEConstants.SYNC_RATE
     */
    public Integer uploadInterval = null;
    /**
     * The amount of seconds to persist the data locally
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
     * The default environment will be selected which is SensorDataProxy.SERVER.STAGING
     */
    public SensorDataProxy.SERVER backendEnvironment = null;
    /**
     * The key for encryption for the local storage
     **/
    public Boolean encryptionKey = null;

    @Override
    public boolean equals(Object o) {
        if(uploadInterval != ((DSEOptions)o).uploadInterval)
            return false;
        if(localPersistancePeriod != ((DSEOptions)o).localPersistancePeriod)
            return false;
        if(enableEncryption != ((DSEOptions)o).enableEncryption)
            return false;
        if(encryptionKey != ((DSEOptions)o).encryptionKey)
            return false;
        if(backendEnvironment != ((DSEOptions)o).backendEnvironment)
            return false;
        return true;
    }
}
