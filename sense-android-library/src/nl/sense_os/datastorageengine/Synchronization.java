package nl.sense_os.datastorageengine;


import org.json.JSONArray;

public abstract class Synchronization {

    public void returnResultAndInsert(String result){}

    public void returnResultAndInsert(JSONArray result){}

    public void returnFinalResult(Object result) {}

    public void andExecute(){}

    public void andExecute(Sensor sensor){}
}
