package nl.sense_os.datastorageengine;

import android.content.Context;

import nl.sense_os.datastorageengine.realm.RealmDatabaseHandler;

/**
 * DataSyncer handles the synchronization between the local storage and CommonSense.
 * The syncing process is handled automatically and periodically, thus the external
 * user does not need to be aware of the data syncing process at all.
 *
 */
public class DataSyncer {

    private String userId = null;
    private String appKey = null;
    private String sessionId = null;
    private DatabaseHandler databaseHandler = null;
    private SensorDataProxy proxy = null;

    public DataSyncer(Context context, String userId, SensorDataProxy.SERVER server, String appKey, String sessionId){
        this.userId = userId;
        this.databaseHandler = new RealmDatabaseHandler(context, userId);
        this.proxy = new SensorDataProxy(server, appKey, sessionId);
    }

    public void initialize(){
        new SensorSynchronization(proxy,databaseHandler).andExecute();
    }

    public void startSyncing(){}

    public void deletionInRemote(){}

    public void downloadFromRemote(){}

    public void uploadToRemote(){}

    public void cleanUpLocalStorage(){}


}
