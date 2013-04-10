package nl.sense_os.service.motion;

import java.util.ArrayList;
import java.util.List;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.ctrl.Controller;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.BaseDataProducer;
import nl.sense_os.service.shared.DataProcessor;
import nl.sense_os.service.shared.SensorDataPoint;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.hardware.SensorEvent;
import android.os.SystemClock;
import android.util.Log;

public class MotionBurstSensor extends BaseDataProducer implements DataProcessor {

    private Controller controller;
    private static final String TAG = "MotionBurstSensor";
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

    private String listToString(List<double[]> dataBuffer) {

        // initialize with some capacity to avoid too much extending.
        StringBuffer dataBufferString = new StringBuffer(50);

        dataBufferString.append("[");
        boolean isFirstRow = true;
        for (double[] values : dataBuffer) {
            if (false == isFirstRow)
                dataBufferString.append(",");
            isFirstRow = false;
            String row = "[" + values[0] + "," + values[1] + "," + values[2] + "]";
            dataBufferString.append(row);
        }
        dataBufferString.append("]");
        return dataBufferString.toString();
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

    @Override
    public void onNewData(SensorDataPoint dataPoint) {
        SensorEvent event = dataPoint.getSensorEventValue();
        onNewData(event.sensor.getType(), event.sensor.getName(), event.values);
    }

    private void sendData(int sensorType, String hardwareName) {

        // create the data point value
        String dataBufferString = listToString(dataBuffer);
        String value = "{\"interval\":"
                + Math.round((double) LOCAL_BUFFER_TIME / (double) dataBuffer.size())
                + ",\"header\":\"" + MotionSensorUtils.getSensorHeader(sensorType).toString()
                + "\",\"values\":\"" + dataBufferString + "\"}";

        // notify any subscribed data processors
        try {
            this.notifySubscribers();
            SensorDataPoint dataPoint;

            dataPoint = new SensorDataPoint(new JSONObject(value));

            dataPoint.sensorName = SENSOR_NAME;
            dataPoint.sensorDescription = hardwareName;
            dataPoint.timeStamp = SNTP.getInstance().getTime() - LOCAL_BUFFER_TIME;
            this.sendToSubscribers(dataPoint);

        } catch (JSONException e) {
            Log.w(TAG, "Failed to send motion burst data", e);
            return;
        }

        // pass message to the MsgHandler
        Intent i = new Intent(context.getString(R.string.action_sense_new_data));

        i.putExtra(DataPoint.SENSOR_NAME, SENSOR_NAME);
        i.putExtra(DataPoint.SENSOR_DESCRIPTION, hardwareName);
        i.putExtra(DataPoint.VALUE, value);
        i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
        i.putExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime() - LOCAL_BUFFER_TIME);
        context.startService(i);

        // A bit ugly, but for now this sensor knows the controller. TODO: controller can just get
        // the values
        if (null != controller) {
            controller.onMotionBurst(dataBuffer, SENSOR_TYPE);
        }
    }

    @Override
    public void startNewSample() {
        sampleComplete = false;
    }
}