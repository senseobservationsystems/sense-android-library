package nl.sense_os.datastorageengine;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;

/**
 * DataSyncer handles the synchronization between the local storage and CommonSense.
 * The syncing process is handled automatically and periodically, thus the external
 * user does not need to be aware of the data syncing process at all.
 *
 */
public class DataSyncer {

    private SensorDataProxy proxy = null;
    private Context context;
    private DatabaseHandler databaseHandler;
    private boolean periodicSyncEnabled = false;
    private DataSyncerAlarmReceiver alarm;
    public final static String SOURCE = "sense-android";

    /**
     * Create a new DataSyncer
     * @param context          Android context
     * @param databaseHandler  A DatabaseHandler for local data storage
     * @param proxy            A proxy for remote data storage (in the Sensor API)
     */
    public DataSyncer(Context context, DatabaseHandler databaseHandler, SensorDataProxy proxy){
        this.context = context;
        this.databaseHandler = databaseHandler;
        this.proxy = proxy;
        this.alarm = new DataSyncerAlarmReceiver();
    }

    public class PeriodicSyncService extends IntentService {
        public static final String TAG = "PeriodicSyncHandlerService";
        public PeriodicSyncService()
        {
            super(TAG);
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
        return this.periodicSyncEnabled;
    }

    public void enablePeriodicSync(){
        // the default sync rate is set in DSEAlarmReceiver
        if(!periodicSyncEnabled) {
            alarm.setAlarm(context);
            periodicSyncEnabled = true;
        }
    }

    public void disablePeriodicSync(){
        alarm.cancelAlarm(context);
        periodicSyncEnabled = false;
    }

    /**
     * Initialize the DataSyncer.
     * This will download the actual list with sensor profiles from remote.
     * @throws JSONException
     * @throws IOException
     * @throws DatabaseHandlerException
     */
    public void initialize() throws JSONException, IOException, DatabaseHandlerException{
        downloadSensorProfiles();
    }

    /**
     * Synchronize data in local and remote storage.
     * Is executed synchronously.
     * @throws IOException
     * @throws DatabaseHandlerException
     * @throws SensorException
     * @throws JSONException
     */
    public void sync() throws IOException, DatabaseHandlerException, SensorException, JSONException {
        // TODO: check whether there is no sync in progress currently, if so throw an exception
        deletionInRemote();
        downloadFromRemote();
        uploadToRemote();
        cleanUpLocalStorage();
    }

    /**
     * Synchronize data in local and remote storage.
     * Is executed synchronously.
     */
    public Future syncAsync() {
        // TODO: implement syncAsync
        return null;
    }

    protected void downloadSensorProfiles () throws JSONException, IOException, DatabaseHandlerException {
        JSONArray sensorProfiles = proxy.getSensorProfiles();
        for(int i = 0; i< sensorProfiles.length(); i++) {
            JSONObject sensorProfile = sensorProfiles.getJSONObject(i);
            if(!databaseHandler.hasSensorProfile(sensorProfile.getString("sensor_name"))) {
                databaseHandler.createSensorProfile(sensorProfile.getString("sensor_name"), sensorProfile.toString());
            }
        }
    }

    protected void deletionInRemote() throws IOException{
        //DatabaseHandler databaseHandler = new DatabaseHandler(context, userId);
        //Step 1: get the deletion requests from local storage
        List<DataDeletionRequest> dataDeletionRequests = databaseHandler.getDataDeletionRequests();

        //Step 2: delete the data in remote and delete the request in local storage
        if(!dataDeletionRequests.isEmpty()){
            for(DataDeletionRequest request : dataDeletionRequests){
                proxy.deleteSensorData(request.getSourceName(),request.getSensorName(),request.getStartTime(),request.getEndTime());
                databaseHandler.deleteDataDeletionRequest(request.getUuid());
            }
        }
    }

    protected void downloadFromRemote() throws IOException, JSONException, SensorException, DatabaseHandlerException{
        //DatabaseHandler databaseHandler = new DatabaseHandler(context, userId);
        //Step 1: download sensors from remote
        JSONArray sensorList = proxy.getSensors(SOURCE);

        //Step 2: insert sensors into local storage
        if(sensorList.length() !=0 ) {
            for (int i = 0; i < sensorList.length(); i++) {
                JSONObject sensorFromRemote = sensorList.getJSONObject(i);
                SensorOptions sensorOptions = new SensorOptions(sensorFromRemote.getJSONObject("meta"), false, true, false);
                if(!databaseHandler.hasSensor(sensorFromRemote.getString("source_name"), sensorFromRemote.getString("sensor_name"))){
                    databaseHandler.createSensor(sensorFromRemote.getString("source_name"), sensorFromRemote.getString("sensor_name"), sensorOptions);
                }
            }
        }

        //Step 3: get the sensors from local storage
        List<Sensor> sensorListInLocal = databaseHandler.getSensors(SOURCE);

        //Step 4: Start data syncing for all sensors
        if(!sensorListInLocal.isEmpty()) {
            for (Sensor sensor : sensorListInLocal) {
                if(sensor.getOptions().isDownloadEnabled() && !sensor.isRemoteDataPointsDownloaded()) {
                    JSONArray dataList = proxy.getSensorData(sensor.getSource(), sensor.getName(), new QueryOptions());
                    for (int i = 0; i < dataList.length(); i++) {
                        JSONObject dataFromRemote = dataList.getJSONObject(i);
                        sensor.insertOrUpdateDataPoint(dataFromRemote.getJSONObject("value"), dataFromRemote.getLong("time"));
                    }
                    sensor.setRemoteDataPointsDownloaded(true);
                }
            }
        }
    }

    protected void uploadToRemote() throws JSONException, SensorException, DatabaseHandlerException, IOException {
        //DatabaseHandler databaseHandler = new DatabaseHandler(context, userId);
        //Step 1: get all the sensors of this source in local storage
        List<Sensor> rawSensorList = databaseHandler.getSensors(SOURCE);

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
                proxy.putSensorData(sensor.getSource(),sensor.getName(),dataArray,sensor.getOptions().getMeta());
                for(DataPoint dataPoint: dataPoints){
                    dataPoint.setExistsInRemote(true);
                    sensor.insertOrUpdateDataPoint(dataPoint.getValue(), dataPoint.getTime(), dataPoint.existsInRemote());
                }
            }
        }
    }

    protected void cleanUpLocalStorage() throws JSONException, DatabaseHandlerException, SensorException{
        //DatabaseHandler databaseHandler = new DatabaseHandler(context, userId);
        //Step 1: get all the sensors of this source in local storage
        List<Sensor> rawSensorList = databaseHandler.getSensors(SOURCE);

        //Step 2: filter the sensor, and set the query options of data point deletion in different conditions.
        for(Sensor sensor: rawSensorList){
            Long persistenceBoundary = new Date().getTime() - DSEConstants.PERSIST_PERIOD;
            if(sensor.getOptions().isUploadEnabled()){
                if(sensor.getOptions().isPersistLocally()){
                   sensor.deleteDataPoints(new QueryOptions(null,persistenceBoundary,true, null, QueryOptions.SORT_ORDER.ASC));
                }else{
                    sensor.deleteDataPoints(new QueryOptions(null,null, true, null, QueryOptions.SORT_ORDER.ASC));
                }
            }else{
                if(sensor.getOptions().isPersistLocally()){
                    sensor.deleteDataPoints(new QueryOptions(null, persistenceBoundary, null, null, QueryOptions.SORT_ORDER.ASC));
                }
            }
        }
    }

}
