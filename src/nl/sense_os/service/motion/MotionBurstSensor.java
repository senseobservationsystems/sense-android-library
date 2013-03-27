package nl.sense_os.service.motion;

import java.util.ArrayList;
import java.util.List;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.ctrl.Controller;
import nl.sense_os.service.provider.SNTP;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorEvent;
import android.os.SystemClock;

public class MotionBurstSensor implements MotionSensorInterface {

	private Controller controller;
    private static final long LOCAL_BUFFER_TIME = 3 * 1000;
    private int SENSOR_TYPE = 0;
    private String SENSOR_NAME;

    private List<double[]> dataBuffer = new ArrayList<double[]>();
    private Context context;
    private boolean sampleComplete = false;
    private long timeAtStartOfBurst = -1;

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
        onNewData(event.sensor.getType(), event.sensor.getName(), event.values);
    }

    public void onNewData(int sensorType, String hardwareName, float[] values) {

    	sampleComplete = false;

        if (sensorType != SENSOR_TYPE) {
            return;
        }

        if (dataBuffer == null) {
            dataBuffer = new ArrayList<double[]>();
        }
        dataBuffer.add(MotionSensorUtils.getVector(values));
        
        if (timeAtStartOfBurst == -1) {
            timeAtStartOfBurst = SystemClock.elapsedRealtime();
        }
        sampleComplete = SystemClock.elapsedRealtime() > timeAtStartOfBurst + LOCAL_BUFFER_TIME;
        if (sampleComplete == true) {
            sendData(sensorType, hardwareName);
        	
        	// reset data buffer
        	dataBuffer.clear();
        	timeAtStartOfBurst = -1;
        }
    }
    
    private String listToString(List<double[]> dataBuffer) {

    	//initialize with some capacity to avoid too much extending.
    	StringBuffer dataBufferString = new StringBuffer(50); 
    	
    	dataBufferString.append("[");
    	boolean isFirstRow = true;
    	for (double[] values : dataBuffer) {
    		if (false == isFirstRow)
    			dataBufferString.append(",");
    		isFirstRow = false;
    		String row = "["+values[0]+","+values[1]+","+values[2]+"]";
    		dataBufferString.append(row);
    	}
    	dataBufferString.append("]");
    	return dataBufferString.toString();
    }

    private void sendData(int sensorType, String hardwareName) {

    	String dataBufferString = listToString(dataBuffer);
    	String value = "{\"interval\":"
                + Math.round((double) LOCAL_BUFFER_TIME / (double) dataBuffer.size())
                + ",\"header\":\"" + MotionSensorUtils.getSensorHeader(sensorType).toString()
                + "\",\"values\":\"" + dataBufferString + "\"}";

        // pass message to the MsgHandler
        Intent i = new Intent(context.getString(R.string.action_sense_new_data));

        i.putExtra(DataPoint.SENSOR_NAME, SENSOR_NAME);
        i.putExtra(DataPoint.SENSOR_DESCRIPTION, hardwareName);
        i.putExtra(DataPoint.VALUE, value);
        i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
        i.putExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime() - LOCAL_BUFFER_TIME);
        context.startService(i);

        //A bit ugly, but for now this sensor knows the controller. TODO: controller can just get the values
        if (null != controller) {
            controller.onMotionBurst(dataBuffer, SENSOR_TYPE);
        }
    }

    @Override
    public void startNewSample() {
    	sampleComplete = false;
    }
}