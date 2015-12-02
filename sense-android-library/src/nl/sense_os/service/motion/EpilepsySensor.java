package nl.sense_os.service.motion;

import nl.sense_os.service.constants.SensorData.SensorNames;

import nl.sense_os.service.shared.SensorDataPoint;
import nl.sense_os.service.shared.SensorDataPoint.DataType;
import nl.sense_os.service.subscription.BaseDataProducer;
import nl.sense_os.service.subscription.DataConsumer;


import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.SystemClock;
import android.util.Log;

public class EpilepsySensor extends BaseDataProducer implements DataConsumer {

    private static final String TAG = "EpilepsySensor";
    private static final long LOCAL_BUFFER_TIME = 15 * 1000;

    private long[] lastLocalSampleTimes = new long[50];
    private long firstTimeSend = 0;
    private JSONArray[] dataBuffer = new JSONArray[10];
    private Context context;

    public EpilepsySensor(Context context) {
        this.context = context;
    }

    @Override
    public boolean isSampleComplete() {
        // never unregister
        return false;
    }

    @Override
    public void onNewData(SensorDataPoint dataPoint) {

    	if(dataPoint.getDataType() != DataType.SENSOREVENT)
        	return;
        
        SensorEvent event = dataPoint.getSensorEventValue(); 
        // check if this is useful data
        Sensor sensor = event.sensor;
        if (sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }
        Log.v(TAG, "New data from " + MotionSensorUtils.getSensorName(sensor));

        JSONObject json = MotionSensorUtils.createJsonValue(event);

        if (dataBuffer[sensor.getType()] == null) {
            dataBuffer[sensor.getType()] = new JSONArray();
        }
        dataBuffer[sensor.getType()].put(json);
        if (lastLocalSampleTimes[sensor.getType()] == 0) {
            lastLocalSampleTimes[sensor.getType()] = SystemClock.elapsedRealtime();
        }

        if (SystemClock.elapsedRealtime() > lastLocalSampleTimes[sensor.getType()]
                + LOCAL_BUFFER_TIME) {
            // send the stuff
            sendData(sensor);

            // reset data buffer
            dataBuffer[sensor.getType()] = new JSONArray();
            lastLocalSampleTimes[sensor.getType()] = SystemClock.elapsedRealtime();
            if (firstTimeSend == 0) {
                firstTimeSend = SystemClock.elapsedRealtime();
            }
        }
    }

    private void sendData(Sensor sensor) {

        String value = "{\"interval\":"
                + Math.round((double) LOCAL_BUFFER_TIME / (double) dataBuffer[sensor.getType()].length())
                + ",\"data\":" + dataBuffer[sensor.getType()].toString() + "}";

        try
        {
        	this.notifySubscribers();        
        	SensorDataPoint dataPoint = new SensorDataPoint(new JSONObject(value));
        	dataPoint.sensorName = SensorNames.ACCELEROMETER_EPI;
        	dataPoint.sensorDescription = sensor.getName();
        	dataPoint.timeStamp = lastLocalSampleTimes[sensor.getType()];        	
        	this.sendToSubscribers(dataPoint);
        }
        catch(Exception e)
        {
        	Log.e(TAG, "Error in sending data to subscribers");
        }
    }

    @Override
    public void startNewSample() {
        // not used
    }

}
