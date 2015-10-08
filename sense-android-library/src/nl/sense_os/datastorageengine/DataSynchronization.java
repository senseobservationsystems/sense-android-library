package nl.sense_os.datastorageengine;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import nl.sense_os.service.shared.SensorDataPoint;


public class DataSynchronization extends Synchronization {
    Object result;
    private SensorDataProxy proxy;
    private DatabaseHandler databaseHandler;
    private Sensor sensor;
    public DataSynchronization() {
    }
    public DataSynchronization(SensorDataProxy proxy, DatabaseHandler databaseHandler ) {
        this.proxy = proxy;
        this.databaseHandler = databaseHandler;
    }

    @Override
    public void returnResultAndInsert(JSONArray dataList) {
        try{
            for(int i = 0; i < dataList.length(); i++){
                JSONObject dataFromRemote = dataList.getJSONObject(i);
                sensor.insertOrUpdateDataPoint(dataFromRemote.getJSONObject("value"),dataFromRemote.getLong("date"));
            }
        }catch(JSONException e) { }

        System.out.println("End of the inset data point for a sensor: Remote data inserted in local storage ");
        //todo: mark the sensor
    }

    @Override
    public void andExecute(Sensor sensor) {
        this.sensor = sensor;
        ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        DataCallBack worker = new DataCallBack(this, proxy);
        worker.setSensor(sensor);
        final Future future = es.submit(worker);

    }
}
