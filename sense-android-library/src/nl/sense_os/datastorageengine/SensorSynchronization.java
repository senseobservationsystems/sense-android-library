package nl.sense_os.datastorageengine;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import nl.sense_os.service.shared.SensorDataPoint;


public class SensorSynchronization extends Synchronization {
    private SensorDataProxy proxy;
    private DatabaseHandler databaseHandler;
    public SensorSynchronization() {
    }
    public SensorSynchronization(SensorDataProxy proxy, DatabaseHandler databaseHandler ) {
        this.proxy = proxy;
        this.databaseHandler = databaseHandler;
    }

    @Override
    public void returnResultAndInsert(JSONArray sensorList) {
        try{
            for(int i = 0; i < sensorList.length(); i++){
                JSONObject sensorFromRemote = sensorList.getJSONObject(i);
                SensorOptions sensorOptions = new SensorOptions(sensorFromRemote.getJSONObject("meta"), false, false, false);
                databaseHandler.createSensor(sensorFromRemote.getString("source_name"), sensorFromRemote.getString("sensor_name"), SensorDataPoint.DataType.FLOAT, sensorOptions);
            }
        }catch(DatabaseHandlerException e){
        }catch(JSONException e) {
        }
        //todo: need a callback from DSE & APP for sensor, now it is a print instead
        System.out.println("End of the get sensor tasks: Remote sensors are inserted in local storage ");
        List<Sensor> sensorListInLocal = new ArrayList<>();
        try {
            //TODO: how to specify the source here ???
            sensorListInLocal = databaseHandler.getSensors("source");
        }catch(JSONException e){
        }
        // Start data syncing for all sensors
        for(Sensor sensor: sensorListInLocal){
            new DataSynchronization(proxy,databaseHandler).andExecute(sensor);
            try {
                sensor.setCsDataPointsDownloaded(true);
            } catch (DatabaseHandlerException e) {
            }
        }
        //todo: need a callback from DSE & APP for data, now it is a print instead
        System.out.println("End of the insert sensor date tasks: Remote data for all sensors are inserted in local storage ");
    }

    @Override
    public void andExecute() {
        ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        SensorCallBack worker = new SensorCallBack(this, proxy);
        es.submit(worker);
    }
}