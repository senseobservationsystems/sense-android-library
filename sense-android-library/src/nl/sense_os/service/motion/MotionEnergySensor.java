package nl.sense_os.service.motion;

import java.math.BigDecimal;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.SensorDataPoint;
import nl.sense_os.service.shared.SensorDataPoint.DataType;
import nl.sense_os.service.subscription.BaseDataProducer;
import nl.sense_os.service.subscription.DataConsumer;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.FloatMath;
import android.util.Log;

public class MotionEnergySensor extends BaseDataProducer implements DataConsumer {

    private static final long ENERGY_SAMPLE_LENGTH = 500;
    private static final String TAG = "MotionEnergySensor";

    private long prevSampleTime;
    private double avgSpeedChange;
    private int avgSpeedCount;
    private Context context;
    private boolean hasLinAccSensor;
    private long sampleStartTime = 0;
    private boolean sampleComplete;
    private AccelerationFilter accelFilter;

    public MotionEnergySensor(Context context) {
        this.context = context;
        checkLinAccSensor();
        if(!hasLinAccSensor){
          accelFilter = new AccelerationFilter();
        }
    }

    @SuppressLint("InlinedApi")
    private void checkLinAccSensor() {
        SensorManager mgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        hasLinAccSensor = (null != mgr.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION));
    }

    private boolean isEnoughDatapoints() {
        return (sampleStartTime != 0 && SystemClock.elapsedRealtime() - sampleStartTime > ENERGY_SAMPLE_LENGTH);
    }

    @Override
    public boolean isSampleComplete() {
        return sampleComplete;
    }

    /**
     * Measures the speed change and determines the average, for the motion energy sensor.
     * 
     * @param event
     *            The sensor change event with accelerometer or linear acceleration data.
     */
    @SuppressLint("InlinedApi")
    @Override
    public void onNewData(SensorDataPoint dataPoint) {

        float[] linAcc = null;

        if (dataPoint.getDataType() != DataType.SENSOREVENT)
            return;

        SensorEvent event = dataPoint.getSensorEventValue();

        // check if this is a useful data point
        boolean isEnergySample = false;
        Sensor sensor = event.sensor;
        isEnergySample = !hasLinAccSensor && Sensor.TYPE_ACCELEROMETER == sensor.getType()
                || hasLinAccSensor && Sensor.TYPE_LINEAR_ACCELERATION == sensor.getType();
        if (!isEnergySample) {
            return;
        }

        // approximate linear acceleration if we have no special sensor for it
        if (!hasLinAccSensor && Sensor.TYPE_ACCELEROMETER == event.sensor.getType()) {
            linAcc = accelFilter.calcLinAcc(event.values);
        } else if (hasLinAccSensor && Sensor.TYPE_LINEAR_ACCELERATION == event.sensor.getType()) {
            linAcc = event.values;
        } else {
            // sensor is not the right type
            return;
        }

        // calculate speed change and adjust average
        if (null != linAcc) {

            // record the start of the motion sample
            if (avgSpeedCount == 0) {
                sampleStartTime = SystemClock.elapsedRealtime();
            }

            float timeStep = (SystemClock.elapsedRealtime() - prevSampleTime) / 1000f;
            prevSampleTime = SystemClock.elapsedRealtime();
            if (timeStep > 0 && timeStep < 1) {
                float accLength = FloatMath.sqrt((float) (Math.pow(linAcc[0], 2)
                        + Math.pow(linAcc[1], 2) + Math.pow(linAcc[2], 2)));

                avgSpeedChange = (avgSpeedCount * avgSpeedChange + accLength) / (avgSpeedCount + 1);
                avgSpeedCount++;
            }
        } else {
            Log.w(TAG, "Cannot calculate motion energy! Linear acceleration value is null");

        }
        // check if we gathered enough data points
        if (!sampleComplete && isEnoughDatapoints()) {
            sendData();
            sampleComplete = true;
        }
    }

    /**
     * Sends message with average motion energy to the MsgHandler.
     */
    private void sendData() {

        // round to three decimals
        float value = BigDecimal.valueOf(avgSpeedChange).setScale(3, 0).floatValue();

        this.notifySubscribers();
        SensorDataPoint dataPoint = new SensorDataPoint(value);
        dataPoint.sensorName = SensorNames.MOTION_ENERGY;
        dataPoint.sensorDescription = SensorNames.MOTION_ENERGY;
        dataPoint.timeStamp = SNTP.getInstance().getTime();
        this.sendToSubscribers(dataPoint);

        // TODO: add the MsgHandler as data processor

        // prepare intent to send to MsgHandler
        Intent i = new Intent(context.getString(R.string.action_sense_new_data));
        i.putExtra(DataPoint.SENSOR_NAME, SensorNames.MOTION_ENERGY);
        i.putExtra(DataPoint.SENSOR_DESCRIPTION, SensorNames.MOTION_ENERGY);
        i.putExtra(DataPoint.VALUE, value);
        i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.FLOAT);
        i.putExtra(DataPoint.TIMESTAMP, dataPoint.timeStamp);
        context.startService(i);
    }

    @Override
    public void startNewSample() {
        sampleComplete = false;
        prevSampleTime = 0;
        sampleStartTime = 0;
        avgSpeedChange = 0;
        avgSpeedCount = 0;
    }
}
