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

import nl.sense_os.datastorageengine.realm.RealmDatabaseHandler;

/**
 * DataSyncer handles the synchronization between the local storage and CommonSense.
 * The syncing process is handled automatically and periodically, thus the external
 * user does not need to be aware of the data syncing process at all.
 *
 */
public class DataSyncer {

    private DatabaseHandler databaseHandler = null;
    private SensorDataProxy proxy = null;

    public DataSyncer(Context context, String userId, SensorDataProxy.SERVER server, String appKey, String sessionId){
        this.databaseHandler = new RealmDatabaseHandler(context, userId);
        this.proxy = new SensorDataProxy(server, appKey, sessionId);
    }

    public void initialize() throws InterruptedException, ExecutionException{
        loginSyncing().get();
        //todo: need a callback from DSE & APP for data, now it is a print instead
        System.out.println("End of the insert sensor date tasks: Remote data for all sensors are inserted in local storage ");

    }
    public Future loginSyncing(){
        ExecutorService es = Executors.newFixedThreadPool(1);
        return es.submit(new Callable() {
            public Object call() throws Exception {
                //TODO: need a return type ?
                downloadFromRemote();
                return null;
            }
        });
    }

    public void execScheduler() throws InterruptedException, ExecutionException{
        synchronize().get();
        //todo: callback needed ?
        System.out.println("End of periodic syncing ");

    }

    public Future synchronize(){
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

    public void deletionInRemote() throws IOException{
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

    public void downloadFromRemote() throws IOException, JSONException, SensorException, DatabaseHandlerException {
        //Step 1: download sensors from remote
        JSONArray sensorList = proxy.getSensors();

        //Step 2: insert sensors into local storage
        for(int i = 0; i < sensorList.length(); i++) {
            JSONObject sensorFromRemote = sensorList.getJSONObject(i);
            //TODO: set csDownloadEnabled to true for sensors downloaded from remote
            SensorOptions sensorOptions = new SensorOptions(sensorFromRemote.getJSONObject("meta"), false, true, false);
            try {
                databaseHandler.createSensor(sensorFromRemote.getString("source_name"), sensorFromRemote.getString("sensor_name"), sensorOptions);
            //TODO: this could happen when a sensor has already been created locally during the initialization or previous syncing.
            } catch (DatabaseHandlerException e) {
                e.printStackTrace();
            }
        }
        //todo: need a callback from DSE & APP for sensor, now it is a print instead
        System.out.println("End of the get sensor tasks: Remote sensors are inserted in local storage ");

        //Step 3: get the sensors from local storage
        //TODO: how to specify the source here ???
        List<Sensor> sensorListInLocal = databaseHandler.getSensors("source");

        //Step 4: Start data syncing for all sensors
        if(!sensorListInLocal.isEmpty()) {
            for (Sensor sensor : sensorListInLocal) {
                if(sensor.getOptions().isDownloadEnabled() && !sensor.isCsDataPointsDownloaded()) {
                    // todo: can default QueryOptions be used here ？
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
        //Step 1: get all the sensors of this source in local storage
        //TODO: specify the source
        List<Sensor> rawSensorList = databaseHandler.getSensors("source");

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
                    JSONObject meta = new JSONObject();
                    meta.put("date",dataPoint.getDate());
                    //TODO: verify the value type
                    meta.put("value",dataPoint.getValueAsJSONObject());
                    dataArray.put(meta);
                }
                proxy.putSensorData(sensor.getSource(),sensor.getName(),dataArray);
                //TODO: set existsInCS to true after putSensorData, if error occurs in remote, then may have duplicate data upload next time
                for(DataPoint dataPoint: dataPoints){
                    dataPoint.setExistsInCS(true);
                }
            }
        }
    }

    public void cleanUpLocalStorage() throws JSONException, DatabaseHandlerException, SensorException{
        //Step 1: get all the sensors of this source in local storage
        //TODO: specify the source
        List<Sensor> rawSensorList = databaseHandler.getSensors("source");

        //Step 2: filter the sensor, and set the query options of data point deletion in different conditions.
        for(Sensor sensor: rawSensorList){
            //TODO: manually set the period to one month, waiting for dse configuration
            Long persistenceBoundary = new Date().getTime() - 1000*60*60*24*31;
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
