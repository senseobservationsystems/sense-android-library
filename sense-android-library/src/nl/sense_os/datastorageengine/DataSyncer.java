package nl.sense_os.datastorageengine;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import nl.sense_os.datastorageengine.realm.RealmDatabaseHandler;
import nl.sense_os.service.shared.SensorDataPoint;

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
        Future future = LoginSyncing();
        try{
            future.get();

            //todo: need a callback from DSE & APP for data, now it is a print instead
            System.out.println("End of the insert sensor date tasks: Remote data for all sensors are inserted in local storage ");
        }catch(InterruptedException e){
        }catch(ExecutionException e) {
        }

    }
    public Future LoginSyncing(){
        //new SensorSynchronization(proxy,databaseHandler).andExecute();
        ExecutorService es = Executors.newFixedThreadPool(1);
        return es.submit(new Callable() {
            public Object call() throws Exception {
                //TODO: need a return type ?
                downloadFromRemote();
                return null;
            }
        });
    }

    public void execSync(){}

    public void deletionInRemote(){}

    public void downloadFromRemote(){
        //Step 1: download sensors from remote
        JSONArray sensorList = new JSONArray();
        try {
            sensorList = proxy.getSensors();
        } catch(IOException e){
            e.printStackTrace();
        } catch(JSONException e){
            e.printStackTrace();
        }

        //Step 2: insert sensors into local storage
        try{
            for(int i = 0; i < sensorList.length(); i++){
                JSONObject sensorFromRemote = sensorList.getJSONObject(i);
                SensorOptions sensorOptions = new SensorOptions(sensorFromRemote.getJSONObject("meta"), false, false, false);
                databaseHandler.createSensor(sensorFromRemote.getString("source_name"), sensorFromRemote.getString("sensor_name"), sensorOptions);
            }
        } catch(DatabaseHandlerException e){
            e.printStackTrace();
        } catch(JSONException e) {
            e.printStackTrace();
        } catch (SensorException e) {
            e.printStackTrace();
        }

        //todo: need a callback from DSE & APP for sensor, now it is a print instead
        System.out.println("End of the get sensor tasks: Remote sensors are inserted in local storage ");

        //Step 3: get the sensors from local storage
        List<Sensor> sensorListInLocal = new ArrayList<>();
        try {
            //TODO: how to specify the source here ???
            sensorListInLocal = databaseHandler.getSensors("source");
        } catch(JSONException e){
            e.printStackTrace();
        } catch (SensorException e) {
            e.printStackTrace();
        }

        //Step 4: Start data syncing for all sensors
        for(Sensor sensor: sensorListInLocal){
            JSONArray dataList = new JSONArray();
            try {
                // todo: can default QueryOptions be used here ？
                dataList = proxy.getSensorData(sensor.getSource(),sensor.getName(),new QueryOptions());
            } catch(IOException e){
                e.printStackTrace();
            } catch(JSONException e){
                e.printStackTrace();
            }
            try{
                for(int i = 0; i < dataList.length(); i++){
                    JSONObject dataFromRemote = dataList.getJSONObject(i);
                    sensor.insertOrUpdateDataPoint(dataFromRemote.getJSONObject("value"),dataFromRemote.getLong("date"));
                }
            } catch(JSONException e) {
                e.printStackTrace();
            } catch (SensorException e) {
                e.printStackTrace();
            }
            try {
                sensor.setCsDataPointsDownloaded(true);
            } catch (DatabaseHandlerException e) {
                e.printStackTrace();
            }
        }
    }

    public void uploadToRemote(){}

    public void cleanUpLocalStorage(){}


}
