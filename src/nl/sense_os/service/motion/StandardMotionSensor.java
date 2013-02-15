package nl.sense_os.service.motion;

import java.util.List;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.DataProcessor;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;

public class StandardMotionSensor implements DataProcessor {

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
    public void onNewData(SensorEvent event) {

        // check if the data point is not too soon
        Sensor sensor = event.sensor;
        if (lastSampleTimes[sensor.getType()] != 0) {
            // we already have a sample for this sensor
            return;
        }

        // store the sample time
        lastSampleTimes[sensor.getType()] = System.currentTimeMillis();

        // send data point
        String sensorName = MotionSensorUtils.getSensorName(sensor);
        JSONObject json = MotionSensorUtils.createJsonValue(event);
        sendData(sensor, sensorName, json);
    }

    private void sendData(Sensor sensor, String sensorName, JSONObject json) {
        Intent i = new Intent(context.getString(R.string.action_sense_new_data));
        i.putExtra(DataPoint.SENSOR_NAME, sensorName);
        i.putExtra(DataPoint.SENSOR_DESCRIPTION, sensor.getName());
        i.putExtra(DataPoint.VALUE, json.toString());
        i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
        i.putExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime());
        context.startService(i);
    }

    @Override
    public void startNewSample() {
        lastSampleTimes = new long[50];
    }
}
