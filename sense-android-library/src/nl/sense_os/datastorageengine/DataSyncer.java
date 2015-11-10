package nl.sense_os.datastorageengine;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.apache.http.client.HttpResponseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import nl.sense_os.datastorageengine.realm.RealmDataDeletionRequest;
import nl.sense_os.util.json.SchemaException;
import nl.sense_os.util.json.ValidationException;

/**
 * DataSyncer handles the synchronization between the local storage and CommonSense.
 * The syncing process is handled automatically and periodically, thus the external
 * user does not need to be aware of the data syncing process at all.
 *
 */
public class DataSyncer {
    private static final String TAG = "DataSyncer";
    public final static String SOURCE = "sense-android";

    private SensorDataProxy mProxy = null;
    private Context mContext;
    private DatabaseHandler mDatabaseHandler;
    private SensorProfiles mSensorProfiles;
    private DataSyncerAlarmReceiver mAlarm;
    private long mPersistPeriod = 2678400000L; // 31 days in milliseconds

    /**
     * Create a new DataSyncer
     * @param context          Android Context
     * @param databaseHandler  A DatabaseHandler for local data storage
     * @param proxy            A Proxy for remote data storage (in the Sensor API)
     */
    public DataSyncer(Context context, DatabaseHandler databaseHandler, SensorDataProxy proxy){
        this.mContext = context;
        this.mDatabaseHandler = databaseHandler;
        this.mSensorProfiles = new SensorProfiles(context);
        this.mProxy = proxy;
        this.mAlarm = new DataSyncerAlarmReceiver();
    }

    public class PeriodicSyncService extends IntentService {
        public static final String TAG = "PeriodicSyncHandlerService";

        public PeriodicSyncService(String name) {
            super(name);
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            try {
                sync();
            }
            catch(Exception e) {
                e.getStackTrace();
            }
            // Release the wake lock provided by the BroadcastReceiver.
            DataSyncerAlarmReceiver.completeWakefulIntent(intent);
        }
    }

    public boolean isPeriodicSyncEnabled() {
        return mAlarm.isRunning();
    }

    public void enablePeriodicSync(){
        mAlarm.setAlarm(mContext);
    }

    public void disablePeriodicSync(){
        mAlarm.cancelAlarm();
    }

    /**
     * Initialize the DataSyncer.
     * This will download the actual list with sensor profiles from remote.
     * @throws JSONException
     * @throws IOException
     * @throws SensorProfileException
     */
    public void initialize() throws JSONException, IOException, SensorProfileException {
        downloadSensorProfiles();
    }

    /**
     * Synchronize data in local and remote storage.
     * Is executed synchronously.
     * @param progressCallback      Optional callback structure to get notified of the syncing progress.
     */
    public void sync(ProgressCallback progressCallback) throws IOException, DatabaseHandlerException, SensorException, SensorProfileException, JSONException, SchemaException, ValidationException {
        // TODO: check whether there is no sync in progress currently, if so throw an exception

        // Step 1
        deletionInRemote();
        if (progressCallback != null) {
            progressCallback.onDeletionCompleted();
        }

        // Step 2
        uploadToRemote();
        if (progressCallback != null) {
            progressCallback.onUploadCompeted();
        }

        // Step 3
        downloadSensorsFromRemote();
        if (progressCallback != null) {
            progressCallback.onDownloadSensorsCompleted();
        }

        // Step 4
        downloadSensorDataFromRemote();
        if (progressCallback != null) {
            progressCallback.onDownloadSensorDataCompleted();
        }

        // Step 4
        cleanupLocal();
        if (progressCallback != null) {
            progressCallback.onCleanupCompleted();
        }
    }

    /**
     * Synchronize data in local and remote storage.
     * Is executed synchronously.
     */
    public void sync() throws IOException, DatabaseHandlerException, SensorException, SensorProfileException,
            JSONException, SchemaException, ValidationException {
        sync(null);
    }

    protected void downloadSensorProfiles () throws JSONException, IOException, SensorProfileException {
        JSONArray profiles = mProxy.getSensorProfiles();

        for(int i = 0; i< profiles.length(); i++) {
            JSONObject profile = profiles.getJSONObject(i);
            Log.d(TAG, "Sensor profile " + i + ": " + profile.toString());

            try {
                mSensorProfiles.createOrUpdate(profile.getString("sensor_name"), profile.getJSONObject("data_structure"));
            }
            catch (Exception err) {
                Log.e(TAG, "Error parsing sensor profile: ", err);
                err.printStackTrace();
            }
        }

        Log.d(TAG, "Sensor profiles downloaded. Number of sensors: " + profiles.length());
    }

    protected void deletionInRemote() throws IOException {
        //Step 1: get the deletion requests from local storage
        List<RealmDataDeletionRequest> dataDeletionRequests = mDatabaseHandler.getDataDeletionRequests();

        //Step 2: delete the data in remote and delete the request in local storage
        if(!dataDeletionRequests.isEmpty()){
            for(RealmDataDeletionRequest request : dataDeletionRequests){
                // FIXME: request.getSourceName() throws an exception as this needs Realm which is already closed.
                mProxy.deleteSensorData(request.getSourceName(), request.getSensorName(), request.getStartTime(), request.getEndTime());
                mDatabaseHandler.deleteDataDeletionRequest(request.getUuid());
            }
        }
    }

    protected void downloadSensorsFromRemote() throws IOException, JSONException, SensorException, SensorProfileException, DatabaseHandlerException, SchemaException, ValidationException {
        //Step 1: download sensors from remote
        JSONArray sensorList = mProxy.getSensors(SOURCE);

        //Step 2: insert sensors into local storage
        if(sensorList.length() !=0 ) {
            for (int i = 0; i < sensorList.length(); i++) {
                JSONObject sensorFromRemote = sensorList.getJSONObject(i);
                SensorOptions sensorOptions = new SensorOptions(sensorFromRemote.getJSONObject("meta"), false, true, false);
                if(!mDatabaseHandler.hasSensor(sensorFromRemote.getString("source_name"), sensorFromRemote.getString("sensor_name"))){
                    mDatabaseHandler.createSensor(sensorFromRemote.getString("source_name"), sensorFromRemote.getString("sensor_name"), sensorOptions);
                }
            }
        }
    }

    protected void downloadSensorDataFromRemote() throws IOException, JSONException, SensorException, SensorProfileException, DatabaseHandlerException, SchemaException, ValidationException {
        //Step 1: get the sensors from local storage
        List<Sensor> sensorListInLocal = mDatabaseHandler.getSensors(SOURCE);

        //Step 2: Start data syncing for all sensors
        if(!sensorListInLocal.isEmpty()) {
            for (Sensor sensor : sensorListInLocal) {
                if(sensor.getOptions().isDownloadEnabled() && !sensor.isRemoteDataPointsDownloaded()) {
                    try {
                        // FIXME: specify the interval for which to retrieve the data, and set the limit to infinity
                        JSONArray dataList = mProxy.getSensorData(sensor.getSource(), sensor.getName(), new QueryOptions());
                        for (int i = 0; i < dataList.length(); i++) {
                            JSONObject dataFromRemote = dataList.getJSONObject(i);
                            sensor.insertOrUpdateDataPoint(dataFromRemote.get("value"), dataFromRemote.getLong("time"));
                        }
                        sensor.setRemoteDataPointsDownloaded(true);
                    }
                    catch (HttpResponseException err) {
                        if (err.getStatusCode() == 404) { // Resource not found
                            // ignore this error: the sensor doesn't yet exist remotely, no problem
                        }
                        else {
                            throw err;
                        }
                    }
                }
            }
        }
    }

    protected void uploadToRemote() throws JSONException, SensorException, SensorProfileException, DatabaseHandlerException, IOException, SchemaException, ValidationException {
        //Step 1: get all the sensors of this source in local storage
        List<Sensor> rawSensorList = mDatabaseHandler.getSensors(SOURCE);

        //Step 2: filter the sensor and its data and upload to remote, mark existsInRemote to true afterwards
        /** Data structure of the sensor
         * [
         *  {
         *      source_name: string,
         *      sensor_name, string,
         *      data: [
         *              {time: number, value: JSON},
         *               ...
         *            ]
         *   },
         *  ...
         * ]
         **/
        for(Sensor sensor: rawSensorList){
            if(sensor.getOptions().isUploadEnabled()){
                List<DataPoint> dataPoints = sensor.getDataPoints(new QueryOptions(null, null, false, null, QueryOptions.SORT_ORDER.ASC));

                JSONArray dataArray = new JSONArray();
                for(DataPoint dataPoint: dataPoints){
                    JSONObject jsonDataPoint = new JSONObject();
                    jsonDataPoint.put("time", dataPoint.getTime());
                    jsonDataPoint.put("value", dataPoint.getValue());
                    dataArray.put(jsonDataPoint);
                }
                mProxy.putSensorData(sensor.getSource(), sensor.getName(), dataArray, sensor.getOptions().getMeta());
                for(DataPoint dataPoint: dataPoints){
                    dataPoint.setExistsInRemote(true);
                    sensor.insertOrUpdateDataPoint(dataPoint.getValue(), dataPoint.getTime(), dataPoint.existsInRemote());
                }
            }
        }
    }

    protected void cleanupLocal() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException, SchemaException {
        //Step 1: get all the sensors of this source in local storage
        List<Sensor> rawSensorList = mDatabaseHandler.getSensors(SOURCE);

        //Step 2: filter the sensor, and set the query options of data point deletion in different conditions.
        for(Sensor sensor: rawSensorList){
            Long persistenceBoundary = new Date().getTime() - mPersistPeriod;
            if(sensor.getOptions().isUploadEnabled()){
                if(sensor.getOptions().isPersistLocally()){
                   sensor.deleteDataPoints(null,persistenceBoundary);
                }else{
                    sensor.deleteDataPoints(null,null); // delete all
                }
            }else{
                if(sensor.getOptions().isPersistLocally()){
                    sensor.deleteDataPoints(null, persistenceBoundary);
                }
            }
        }
    }

    /**
     * Get the persist period, the period for which sensor data is kept in the local database.
     * @return Returns the persist period in milliseconds
     */
    public long getPersistPeriod() {
        return mPersistPeriod;
    }

    /**
     * Set the persist period, the period for which sensor data is kept in the local database.
     * @param persistPeriod   The persist period in milliseconds, 2678400000 (=31 days) by default.
     */
    public void setPersistPeriod(long persistPeriod) {
        this.mPersistPeriod = persistPeriod;
    }

    /**
     * A progress callback can be passed to method DataSyncer.sync(callback) to get notified during
     * the different steps that the syncing process takes. The callbacks are triggered in the
     * following order, and are only invoked once:
     *
     *   1. onDeletionCompleted
     *   2. onUploadCompeted
     *   3. onDownloadSensorsCompleted
     *   4. onDownloadSensorDataCompleted
     *   5. onCleanupCompleted
     *
     */
    public interface ProgressCallback {
        /**
         * Callback method called after data scheduled for deletion is actually deleted from
         * remote.
         **/
        void onDeletionCompleted();

        /**
         * Callback called after all local data is uploaded to remote
         */
        void onUploadCompeted();

        /**
         * Callback called after all remote sensors (not their data) are downloaded to local
         */
        void onDownloadSensorsCompleted();

        /**
         * Callback called after all remote sensor data is downloaded to local
         */
        void onDownloadSensorDataCompleted();

        /**
         * Callback called after all outdated local data is cleaned up. Data is kept locally for a
         * certain period only, and removed from local when older than this period and synced to remote.
         */
        void onCleanupCompleted();
    }

    /**
     * Get the current sync rate
     * @return Returns the sync rate in milliseconds
     */
    public long getSyncRate() {
        return mAlarm.getSyncRate();
    }

    /**
     * Set sync rate
     * @param syncRate Sync rate in milliseconds (1800000 (= 30 minutes) by default)
     */
    public void setSyncRate(long syncRate) {
        this.mAlarm.setSyncRate(syncRate);
    }
}
