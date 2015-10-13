package nl.sense_os.datastorageengine;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import nl.sense_os.datastorageengine.realm.RealmDatabaseHandler;

/**
 * DataSyncer handles the synchronization between the local storage and CommonSense.
 * The syncing process is handled automatically and periodically, thus the external
 * user does not need to be aware of the data syncing process at all.
 *
 */
public class DataSyncer {

    private SensorDataProxy proxy = null;
    private Context context;
    private String userId;
    private ScheduledExecutorService service;
    //Default value of syncing period, 30 mins in milliseconds
    public static long SYNC_RATE = 1800000L;
    // Default value of persistPeriod, 31 days in milliseconds
    //TODO: should it be public, should SYNC_RATE and persistPeriod have setter and getter methods
    private long persistPeriod = 2678400000L;
    private ScheduledFuture<?> scheduledFuture;
    public final static String SOURCE = "sense-android";

    public DataSyncer(Context context, String userId, SensorDataProxy.SERVER server, String appKey, String sessionId, Long persistPeriod){
        this.context = context;
        this.userId = userId;
        this.service = Executors.newSingleThreadScheduledExecutor();

        //Null for default value of persistPeriod
        if(persistPeriod != null){
            this.persistPeriod = persistPeriod;
        }

        this.proxy = new SensorDataProxy(server, appKey, sessionId);
    }

    public class Task implements Runnable {
        @Override
        public void run() {
            try {
                synchronize();
            }catch(InterruptedException e){
                e.printStackTrace();
            }catch (ExecutionException e){
                e.printStackTrace();
            }
        }
    }

    public void enablePeriodicSync(){
        enablePeriodicSync(SYNC_RATE);
    }

    public void enablePeriodicSync(long syncRate){
        scheduledFuture = service.scheduleAtFixedRate(new Task(), 0, syncRate, TimeUnit.MILLISECONDS);
    }

    public void disablePeriodicSync(){
        scheduledFuture.cancel(false);
    }

    public void login() throws InterruptedException, ExecutionException{
        loginAsync().get();

    }
    public Future loginAsync(){
        ExecutorService es = Executors.newFixedThreadPool(1);
        return es.submit(new Callable() {
            public Object call() throws Exception {
                downloadSensorProfile();
                return null;
            }
        });
    }

    public void synchronize() throws InterruptedException, ExecutionException{
        synchronizeAsync().get();

    }

    public Future synchronizeAsync(){
        ExecutorService es = Executors.newFixedThreadPool(1);
        return es.submit(new Callable() {
            public Object call() throws Exception {
                deletionInRemote();
                downloadFromRemote();
                uploadToRemote();
                cleanUpLocalStorage();
                return null;
            }
        });
    }

    public void downloadSensorProfile(){
        //TODO
        //proxy.downloadSensorList();

    }
    public void deletionInRemote() throws IOException{
        DatabaseHandler databaseHandler = new RealmDatabaseHandler(context, userId);
        //Step 1: get the deletion requests from local storage
        List<DataDeletionRequest> dataDeletionRequests = databaseHandler.getDataDeletionRequests();

        //Step 2: delete the data in remote and delete the request in local storage
        if(!dataDeletionRequests.isEmpty()){
            for(DataDeletionRequest request : dataDeletionRequests){
                // take care of -1 for null
                Long startTime = null;
                Long endTime = null;
                if(request.getStartDate() != -1){
                    startTime = request.getStartDate();
                }
                if(request.getEndDate() != -1){
                    endTime = request.getEndDate();
                }
                proxy.deleteSensorData(request.getSourceName(),request.getSensorName(),startTime,endTime);
                databaseHandler.deleteDataDeletionRequest(request.getUuid());
            }
        }
    }

    public void downloadFromRemote() throws IOException, JSONException, SensorException, DatabaseHandlerException{
        DatabaseHandler databaseHandler = new RealmDatabaseHandler(context, userId);
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
                if(sensor.getOptions().isDownloadEnabled() && !sensor.isCsDataPointsDownloaded()) {
                    JSONArray dataList = proxy.getSensorData(sensor.getSource(), sensor.getName(), new QueryOptions());
                    for (int i = 0; i < dataList.length(); i++) {
                        JSONObject dataFromRemote = dataList.getJSONObject(i);
                        sensor.insertOrUpdateDataPoint(dataFromRemote.getJSONObject("value"), dataFromRemote.getLong("date"));
                    }
                    sensor.setCsDataPointsDownloaded(true);
                }
            }
        }
    }

    public void uploadToRemote() throws JSONException, SensorException, DatabaseHandlerException, IOException {
        DatabaseHandler databaseHandler = new RealmDatabaseHandler(context, userId);
        //Step 1: get all the sensors of this source in local storage
        List<Sensor> rawSensorList = databaseHandler.getSensors(SOURCE);

        //Step 2: filter the sensor and its data and upload to remote, mark existsInCS to true afterwards
        /** Data structure of the sensor
         * [
         *  {
         *      source_name: string,
         *      sensor_name, string,
         *      data: [
         *              {date: number, value: JSON},
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
                    jsonDataPoint.put("date", dataPoint.getDate());
                    jsonDataPoint.put("value", dataPoint.getValue());
                    dataArray.put(jsonDataPoint);
                }
                proxy.putSensorData(sensor.getSource(),sensor.getName(),dataArray);
                //TODO: merge into one request
                proxy.updateSensor(sensor.getSource(),sensor.getName(),sensor.getOptions().getMeta());
                for(DataPoint dataPoint: dataPoints){
                    dataPoint.setExistsInCS(true);
                }
            }
        }
    }

    public void cleanUpLocalStorage() throws JSONException, DatabaseHandlerException, SensorException{
        DatabaseHandler databaseHandler = new RealmDatabaseHandler(context, userId);
        //Step 1: get all the sensors of this source in local storage
        List<Sensor> rawSensorList = databaseHandler.getSensors(SOURCE);

        //Step 2: filter the sensor, and set the query options of data point deletion in different conditions.
        for(Sensor sensor: rawSensorList){
            Long persistenceBoundary = new Date().getTime() - persistPeriod;
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
