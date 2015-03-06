package nl.sense_os.service.motion;

import java.util.ArrayList;
import java.util.List;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main.Motion;
import nl.sense_os.service.constants.SensorData;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.SensorDataPoint;
import nl.sense_os.service.subscription.BaseDataProducer;
import nl.sense_os.service.subscription.DataConsumer;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.SystemClock;
import android.util.Log;

public class MotionBurstSensor extends BaseDataProducer implements DataConsumer {

	private static final String TAG = "MotionBurstSensor";
	private static final long DEFAULT_BURST_DURATION = 3 * 1000;
	private long burstDuration = 3 * 1000;
	private int SENSOR_TYPE = 0;
	private String SENSOR_NAME;

	private List<double[]> dataBuffer = new ArrayList<double[]>();
	private Context context;
	private boolean sampleComplete = false;
	private long timeAtStartOfBurst = -1;
	private boolean isFakeLinear = false;
	private AccelerationFilter accelFilter;
	private MotionSensor motionSensor = null;

	public MotionBurstSensor(Context context, int sensorType, String sensorName) {
		this.context = context;	
		SENSOR_TYPE = sensorType;
		SENSOR_NAME = sensorName;
		isFakeLinear = ((SENSOR_NAME == SensorData.SensorNames.LINEAR_BURST) && (SENSOR_TYPE == Sensor.TYPE_ACCELEROMETER));
		if(isFakeLinear){
		  accelFilter = new AccelerationFilter();
		}
		motionSensor = MotionSensor.getInstance(context);
	}

	@Override
	public boolean isSampleComplete() {
		return sampleComplete;
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

	@Override
	public void onNewData(SensorDataPoint dataPoint) {
		SensorEvent event =  dataPoint.getSensorEventValue();
		Sensor sensor = event.sensor;
		if (sensor.getType() != SENSOR_TYPE) {
			return;
		}

		if (dataBuffer == null) {
			dataBuffer = new ArrayList<double[]>();
		}
		if(isFakeLinear){ 
		  //fakeLinearBurstSensor
		  float[] filteredValues = accelFilter.calcLinAcc( event.values );
		  dataBuffer.add(MotionSensorUtils.getVector(filteredValues));
		}else{
		  dataBuffer.add(MotionSensorUtils.getVector(event));
		}

		if (timeAtStartOfBurst == -1) {
			timeAtStartOfBurst = SystemClock.elapsedRealtime();
		}
		
		boolean buffer_time_reached = SystemClock.elapsedRealtime() > timeAtStartOfBurst + burstDuration;
		
		// check if the interval between samples is larger than the local buffer time
		if(motionSensor.getSampleRate() > burstDuration)		
			sampleComplete = buffer_time_reached;				
			
		if (buffer_time_reached) {
			sendData(sensor);

			// reset data buffer
			dataBuffer.clear();
			timeAtStartOfBurst = -1;
		}
	}

	private void sendData(Sensor sensor) {

		String dataBufferString = listToString(dataBuffer);
		String value = "{\"interval\":"
				+ Math.round((double) burstDuration / (double) dataBuffer.size())
				+ ",\"header\":\"" + MotionSensorUtils.getSensorHeader(sensor).toString()
				+ "\",\"values\":" + dataBufferString + "}";
		String processed = (SENSOR_NAME == SensorData.SensorNames.LINEAR_BURST && SENSOR_TYPE == Sensor.TYPE_ACCELEROMETER)? "(processed)":"";

		try {
			this.notifySubscribers();
			SensorDataPoint dataPoint;

			dataPoint = new SensorDataPoint(new JSONObject(value));

			dataPoint.sensorName = SENSOR_NAME;
			dataPoint.sensorDescription = sensor.getName();
			if(SENSOR_NAME == SensorData.SensorNames.LINEAR_BURST && SENSOR_TYPE == Sensor.TYPE_ACCELEROMETER){ 
			  dataPoint.sensorDescription += "(processed)";
			}
			dataPoint.timeStamp = SNTP.getInstance().getTime() - burstDuration;
			this.sendToSubscribers(dataPoint);

		} catch (JSONException e) {
            Log.w(TAG, "Failed to send motion burst data", e);
            return;
		}

		// pass message to the MsgHandler
		Intent i = new Intent(context.getString(R.string.action_sense_new_data));

		
		
		final SharedPreferences mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
                Context.MODE_PRIVATE);

        if (mainPrefs.getBoolean(Motion.DONT_UPLOAD_BURSTS, false) == false) {
			i.putExtra(DataPoint.SENSOR_NAME, SENSOR_NAME);
			i.putExtra(DataPoint.SENSOR_DESCRIPTION, sensor.getName()+processed);
			i.putExtra(DataPoint.VALUE, value);
			i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON_TIME_SERIES);
			i.putExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime()
					- burstDuration);
			i.setPackage(context.getPackageName());
			context.startService(i);
        }
	}

	@Override
	public void startNewSample() {
		final SharedPreferences mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
		this.burstDuration = mainPrefs.getLong(Motion.BURST_DURATION, DEFAULT_BURST_DURATION);
		sampleComplete = false;
	}
}