package nl.sense_os.service.motion;

import java.util.ArrayList;
import java.util.List;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.ctrl.Controller;
import nl.sense_os.service.provider.SNTP;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.util.Log;

public class MotionBurstSensor implements MotionSensorInterface {

	private Controller controller;
    private static final String TAG = "MotionBurstSensor";
    private static final long LOCAL_BUFFER_TIME = 3 * 1000;
    private int SENSOR_TYPE = 0;
    private String SENSOR_NAME;

    private List<double[]> dataBuffer = new ArrayList<double[]>();
    private Context context;
    private boolean sampleComplete = false;

    public MotionBurstSensor(Context context, int sensorType, String sensorName) {
        this.context = context;
        controller = Controller.getController(context);
        SENSOR_TYPE = sensorType;
        SENSOR_NAME = sensorName;
    }

    @Override
    public boolean isSampleComplete() {
    	return sampleComplete;
    }
    
    @Override
    public void onNewData(SensorEvent event) {
        
    	Log.w(TAG, "Burst! " + SENSOR_NAME);
    	sampleComplete = false;

        Sensor sensor = event.sensor;
        if (sensor.getType() != SENSOR_TYPE) {
            return;
        }
        Log.v(TAG, "New data from " + MotionSensorUtils.getSensorName(sensor));

        JSONObject json = MotionSensorUtils.createJsonValue(event);

        if (dataBuffer == null) {
            dataBuffer = new ArrayList<double[]>();
        }
        dataBuffer.add(MotionSensorUtils.getValues(event));
        
        sampleComplete = controller.stopBurst(json, dataBuffer, SENSOR_TYPE, LOCAL_BUFFER_TIME);
        if (sampleComplete == true) {
        	sendData(sensor);
        	
        	// reset data buffer
        	dataBuffer = new ArrayList<double[]>();
        }
       
    }
    
    private String ListToString(List<double[]> dataBuffer) {
    	String dataBufferString = "";
    	
    	dataBufferString += "[";
    	for (int z = 0; z < dataBuffer.size(); z++) {
    		double[] values = dataBuffer.get(z);
    		dataBufferString += "[";
    		for (int i = 0; i < 3; i++) {
	    		dataBufferString += values[i];
	    		if (i != 2 )
	    			dataBufferString += ", ";    		
    		}		
    		dataBufferString += "]";
    		if (z != (dataBuffer.size() - 1))
    			dataBufferString += ",";
    	}
    	dataBufferString += "]";
    	
    	return dataBufferString;
    }

    private void sendData(Sensor sensor) {
    	
    	String dataBufferString = ListToString(dataBuffer);
    	Log.w(TAG, "Array! " + dataBufferString);
    	String value = "{\"interval\":"
                + Math.round((double) LOCAL_BUFFER_TIME / (double) dataBuffer.size())
                + ",\"header\":\"" + MotionSensorUtils.getSensorHeader(sensor).toString()
                + "\",\"values\":\"" + dataBufferString + "\"}";
    	
        // pass message to the MsgHandler
        Intent i = new Intent(context.getString(R.string.action_sense_new_data));
        
        i.putExtra(DataPoint.SENSOR_NAME, SENSOR_NAME);
        i.putExtra(DataPoint.SENSOR_DESCRIPTION, sensor.getName());
        i.putExtra(DataPoint.VALUE, value);
        i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON_TIME_SERIES);
        i.putExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime() - LOCAL_BUFFER_TIME);
        context.startService(i);
    }

    @Override
    public void startNewSample() {
    	sampleComplete = false;
    }
    

}