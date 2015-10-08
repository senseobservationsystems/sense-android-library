package nl.sense_os.datastorageengine;


import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.Callable;

public class DataCallBack implements Callable {
    private Synchronization callbackService;
    private SensorDataProxy proxy;
    private Sensor sensor;

    public DataCallBack() {
    }

    public DataCallBack(Synchronization callbackService, SensorDataProxy proxy) {
        this.callbackService = callbackService;
        this.proxy = proxy;
    }

    public Object call() {
        JSONArray dataList = new JSONArray();
        try {
            // todo: can default QueryOptions be used here ？
            dataList = proxy.getSensorData(sensor.getSource(),sensor.getName(),new QueryOptions());
        }catch(IOException e){
        }catch(JSONException e){
        }
        //todo: End of the tasks
        callbackService.returnResultAndInsert(dataList);
        return null;
    }

    public void setCallbackService(Synchronization callbackService) {
        this.callbackService = callbackService;
    }

    public Synchronization getCallbackService() {
        return callbackService;
    }

    public void setProxy(SensorDataProxy proxy) {
        this.proxy = proxy;
    }

    public void setSensor(Sensor sensor) {
        this.sensor = sensor;
    }
}
