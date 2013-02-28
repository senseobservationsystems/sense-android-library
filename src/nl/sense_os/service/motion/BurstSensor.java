package nl.sense_os.service.motion;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.SystemClock;
import android.util.Log;

public class BurstSensor implements MotionSensorInterface {

    private static final String TAG = "BurstSensor";
    private static final long LOCAL_BUFFER_TIME = 3 * 1000;
    private static final long DEFAULT_BURST_RATE = 10 * 1000;
    private static final long IDLE_BURST_RATE = 12 * 1000;
    private static final double IDLE_MOTION_THRESHOLD = 0.09;
    private static final double IDLE_TIME_THRESHOLD = 3 * 60 * 1000;

    private long[] lastLocalSampleTimes = new long[50];
    private long firstTimeSend = 0;
    private long firstIdleDetectedTime = 0;
    private JSONArray[] dataBuffer = new JSONArray[10];
    private Context context;
    private boolean sampleComplete = false;

    public BurstSensor(Context context) {
        this.context = context;
    }

    @Override
    public boolean isSampleComplete() {
    	return sampleComplete;
    }
    
    private double avgMotion = 0, motion = 0, totalMotion = 0;
    private double x1 = 0, x2 = 0;
	private double y1 = 0, y2 = 0;
	private double z1 = 0, z2 = 0;
    
    @Override
    public void onNewData(SensorEvent event) {
        
    	Log.w(TAG, "Burst!");
    	sampleComplete = false;
    	MotionSensor motionSensor = MotionSensor.getInstance(context);
        Sensor sensor = event.sensor;
        if (sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }
        Log.v(TAG, "New data from " + MotionSensorUtils.getSensorName(sensor));

        JSONObject json = MotionSensorUtils.createJsonValue(event);
        try {
			x2 = json.getDouble("x-axis");
			y2 = json.getDouble("y-axis");
			z2 = json.getDouble("z-axis");
        } catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        if (dataBuffer[sensor.getType()] == null) {
            dataBuffer[sensor.getType()] = new JSONArray();
        }
        dataBuffer[sensor.getType()].put(json);
        if (lastLocalSampleTimes[sensor.getType()] == 0) {
        	lastLocalSampleTimes[sensor.getType()] = SystemClock.elapsedRealtime();
        	x1 = x2;
            y1 = y2;
            z1 = z2;
        }
        else {
        	motion = Math.pow((Math.abs(x2 - x1) + Math.abs(y2 - y1) + Math.abs(z2 - z1)), 2);
            x1 = x2;
            y1 = y2;
            z1 = z2;
            totalMotion += motion;
        }
        if (SystemClock.elapsedRealtime() > lastLocalSampleTimes[sensor.getType()]
                + LOCAL_BUFFER_TIME) {

        	lastLocalSampleTimes[sensor.getType()] = 0;
        	avgMotion = totalMotion / (dataBuffer[sensor.getType()].length() - 1);
        	
        	if (avgMotion > IDLE_MOTION_THRESHOLD) {
        		firstIdleDetectedTime = 0;
        		if (motionSensor.getSampleRate() != DEFAULT_BURST_RATE) {
        			motionSensor.setSampleRate(DEFAULT_BURST_RATE);
        		}
        	} else {
        		if (firstIdleDetectedTime == 0) {
        			firstIdleDetectedTime = SystemClock.elapsedRealtime();
        		} else {
        			if ((SystemClock.elapsedRealtime() > firstIdleDetectedTime + IDLE_TIME_THRESHOLD) && (motionSensor.getSampleRate() == DEFAULT_BURST_RATE)) {
        				motionSensor.setSampleRate(IDLE_BURST_RATE);
        			}
        		}
        	}
        	Log.w(TAG, "AVG " + avgMotion + " INTERVAL " + motionSensor.getSampleRate());
        	
        	totalMotion = 0;
        	sampleComplete = true;
            sendData(sensor);

            // reset data buffer
            dataBuffer[sensor.getType()] = new JSONArray();
            //lastLocalSampleTimes[sensor.getType()] = 0;
            if (firstTimeSend == 0) {
                firstTimeSend = SystemClock.elapsedRealtime();
            }
        }
    }

    private void sendData(Sensor sensor) {

        String value = "{\"interval\":"
                + Math.round((double) LOCAL_BUFFER_TIME / (double) dataBuffer[sensor.getType()].length())
                + ",\"data\":" + dataBuffer[sensor.getType()].toString() + "}";
        
        // pass message to the MsgHandler
        Intent i = new Intent(context.getString(R.string.action_sense_new_data));
        
        i.putExtra(DataPoint.SENSOR_NAME, SensorNames.ACCELEROMETER_BURST);
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