package nl.sense_os.datastorageengine;

import android.content.Context;
import android.util.Log;

import org.apache.http.client.HttpResponseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.List;

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
    // TODO move static constants to a separate class
    public final static String SOURCE = "sense-android";
    public final static long SYNC_RATE = 1800000;  // 30 minutes in milliseconds by default
    public final static long PERSIST_PERIOD = 2678400000L; // 31 days in milliseconds

    // set the default persist period and sync rate
    private long mSyncRate = SYNC_RATE;
    private long mPersistPeriod = PERSIST_PERIOD;

    private SensorDataProxy mProxy = null;
    private DatabaseHandler mDatabaseHandler;
    private SensorProfiles mSensorProfiles;
    private Context mContext;

    private Object mLock = new Object();

    /**
     * Create a new DataSyncer
     * @param context          Android Context
     * @param databaseHandler  A DatabaseHandler for local data storage
     * @param proxy            A Proxy for remote data storage (in the Sensor API)
     */
    public DataSyncer(Context context, DatabaseHandler databaseHandler, SensorDataProxy proxy){
        this.mDatabaseHandler = databaseHandler;
        this.mSensorProfiles = new SensorProfiles(context, databaseHandler.getEncryptionKey());
        this.mProxy = proxy;
        mContext = context;
    }

    public void enablePeriodicSync(long syncRate){
        mSyncRate = syncRate;
        PeriodicDataSyncer.setAlarm(mContext, mSyncRate);
    }

    public void enablePeriodicSync(){
        enablePeriodicSync(mSyncRate);
    }

    public void disablePeriodicSync(){
        PeriodicDataSyncer.cancelAlarm(mContext);
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
        Log.d(TAG, "sync start... awaiting lock");

        synchronized (mLock) {
            Log.d(TAG, "sync start... there we go");

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

            // Step 5
            cleanupLocal();
            if (progressCallback != null) {
                progressCallback.onCleanupCompleted();
            }
        }

        Log.d(TAG, "sync end");
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
        List<DataDeletionRequest> dataDeletionRequests = mDatabaseHandler.getDataDeletionRequests();

        //Step 2: delete the data in remote and delete the request in local storage
        if(!dataDeletionRequests.isEmpty()){
            for(DataDeletionRequest request : dataDeletionRequests) {
                try {
                    mProxy.deleteSensorData(request.getSourceName(), request.getSensorName(), request.getStartTime(), request.getEndTime());
                }
                catch (HttpResponseException err) {
                    if (err.getStatusCode() == 404) { // Resource not found
                        // ignore this error: the sensor doesn't yet exist remotely, no problem
                    }
                    else {
                        throw err;
                    }
                }
                mDatabaseHandler.deleteDataDeletionRequest(request.getId());
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
                        // as start time, we take the the current time minus persist period,
                        // minus 10 minutes to account for duration of the process itself
                        final long MINUTE = 1000 * 60;
                        final int LIMIT = 100; // number of data points to retrieve per request, max allowed by the backend is 1000
                        QueryOptions options = new QueryOptions();
                        options.setSortOrder(QueryOptions.SORT_ORDER.DESC);
                        options.setStartTime(new Date().getTime() - mPersistPeriod - 10 * MINUTE);
                        options.setEndTime(null); // undefined, we want to get all data up until to now
                        options.setLimit(LIMIT);

                        boolean done = false;
                        while (!done) {
                            JSONArray dataList = mProxy.getSensorData(sensor.getSource(), sensor.getName(), options);
                            for (int i = 0; i < dataList.length(); i++) {
                                JSONObject dataFromRemote = dataList.getJSONObject(i);
                                DataPoint dataPoint = new DataPoint(sensor.getId(), dataFromRemote.get("value"), dataFromRemote.getLong("time"), true);
                                sensor.insertOrUpdateDataPoint(dataPoint);
                            }

                            if (dataList.length() < LIMIT) {
                                done = true;
                            }
                            else {
                                // we need to retrieve the next 100 number of items
                                // new endTime is the time of the last (oldest) data point. Data points are ordered DESC
                                long endTime = dataList.getJSONObject(dataList.length() - 1).getLong("time");
                                options.setEndTime(endTime);
                            }
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
                    sensor.insertOrUpdateDataPoint(dataPoint);
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
}
