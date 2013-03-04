package nl.sense_os.service.motion;

import java.util.List;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.BaseDataProducer;
import nl.sense_os.service.shared.DataProcessor;
import nl.sense_os.service.shared.SensorDataPoint;
import nl.sense_os.service.shared.SensorDataPoint.DataType;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.SystemClock;
import android.util.Log;


public class StandardMotionSensor extends BaseDataProducer implements DataProcessor {

	private static final String TAG = "StandardMotionSensor";
	private Context context;
	private long[] lastSampleTimes = new long[50];
	private final List<Sensor> sensors;

	public StandardMotionSensor(Context context) {
		this.context = context;
		sensors = MotionSensorUtils.getAvailableMotionSensors(context);
	}

    @Override
	public boolean isSampleComplete() {

		// only unregister if all sensors have submitted a new sample
		int count = 0;
		boolean complete = false;
		for (long time : lastSampleTimes) {
			if (time != 0) {
				count++;
				if (count >= sensors.size()) {
					complete = true;
					break;
				}
			}
		}

		return complete;
	}

    @Override
	public void onNewData(SensorDataPoint dataPoint) {

		if(dataPoint.getDataType() != DataType.SENSOREVENT)
			return;

		SensorEvent event = dataPoint.getSensorEventValue(); 
		// check if the data point is not too soon
		Sensor sensor = event.sensor;
		if (lastSampleTimes[sensor.getType()] != 0) {
			// we already have a sample for this sensor
			return;
		}

		// store the sample time
		lastSampleTimes[sensor.getType()] = SystemClock.elapsedRealtime();

		// send data point
		String sensorName = MotionSensorUtils.getSensorName(sensor);
		JSONObject json = MotionSensorUtils.createJsonValue(event);
		sendData(sensor, sensorName, json);
	}

	private void sendData(Sensor sensor, String sensorName, JSONObject json) {
		try
		{
			this.notifySubscribers();
			SensorDataPoint dataPoint = new SensorDataPoint(json);
			dataPoint.sensorName = sensorName;
			dataPoint.sensorDescription = sensor.getName();
			dataPoint.timeStamp = SNTP.getInstance().getTime();        
			this.sendToSubscribers(dataPoint);

			// TODO: implement MsgHandler as data processor
			Intent i = new Intent(context.getString(R.string.action_sense_new_data));
			i.putExtra(DataPoint.SENSOR_NAME, sensorName);
			i.putExtra(DataPoint.SENSOR_DESCRIPTION, sensor.getName());
			i.putExtra(DataPoint.VALUE, json.toString());
			i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
			i.putExtra(DataPoint.TIMESTAMP, dataPoint.timeStamp);
			context.startService(i);
		}
		catch(Exception e)
		{
			Log.e(TAG, "Error seding data from StandardMotionSensor");
		}
	}

    @Override
	public void startNewSample() {
		lastSampleTimes = new long[50];
	}
}
