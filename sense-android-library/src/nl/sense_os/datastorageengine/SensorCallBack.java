package nl.sense_os.datastorageengine;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.Callable;


public class SensorCallBack implements Callable{
    private Synchronization callbackService;
    private SensorDataProxy proxy;

    public SensorCallBack() {
    }

    public SensorCallBack(Synchronization callbackService, SensorDataProxy proxy) {
        this.callbackService = callbackService;
        this.proxy = proxy;
    }

    public Object call() {
        JSONArray sensorList = new JSONArray();
        try {
            sensorList = proxy.getSensors();
        }catch(IOException e){
        }catch(JSONException e){
        }
        //todo: End of the tasks
        callbackService.returnResultAndInsert(sensorList);
        return null;
    }

    public void setCallbackService(Synchronization callbackService) {
        this.callbackService = callbackService;
    }

    public Synchronization getCallbackService() {
        return callbackService;
    }

    public void setProxy(SensorDataProxy proxy){
        this.proxy = proxy;
    }
}
