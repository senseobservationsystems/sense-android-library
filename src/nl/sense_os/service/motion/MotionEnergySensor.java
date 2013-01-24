package nl.sense_os.service.motion;

import java.math.BigDecimal;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.util.FloatMath;

public class MotionEnergySensor implements MotionSensorInterface {

    private static final long ENERGY_SAMPLE_LENGTH = 500;

    private long prevEnergySampleTime;
    private double avgSpeedChange;
    private int avgSpeedCount;
    private float[] gravity = { 0, 0, SensorManager.GRAVITY_EARTH };
    private Context context;
    private boolean hasLinAccSensor;
    private long energySampleStart = 0;

    public MotionEnergySensor(Context context) {
        this.context = context;
        checkLinAccSensor();
    }

    /**
     * Calculates the linear acceleration of a raw accelerometer sample. Tries to determine the
     * gravity component by putting the signal through a first-order low-pass filter.
     * 
     * @param values
     *            Array with accelerometer values for the three axes.
     * @return The approximate linear acceleration of the sample.
     */
    private float[] calcLinAcc(float[] values) {

        // low-pass filter raw accelerometer data to approximate the gravity
        final float alpha = 0.8f; // filter constants should depend on sample rate
        gravity[0] = alpha * gravity[0] + (1 - alpha) * values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * values[2];

        return new float[] { values[0] - gravity[0], values[1] - gravity[1], values[2] - gravity[2] };
    }

    private void checkLinAccSensor() {
        SensorManager mgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        hasLinAccSensor = (null != mgr.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION));
    }

    @Override
    public boolean isSampleComplete() {
        return (energySampleStart != 0 && System.currentTimeMillis() - energySampleStart > ENERGY_SAMPLE_LENGTH);
    }

    /**
     * Measures the speed change and determines the average, for the motion energy sensor.
     * 
     * @param event
     *            The sensor change event with accelerometer or linear acceleration data.
     */
    @Override
    public void onNewData(SensorEvent event) {
        float[] linAcc = null;

        // check if this is a useful data point
        Sensor sensor = event.sensor;
        boolean isEnergySample = !hasLinAccSensor && Sensor.TYPE_ACCELEROMETER == sensor.getType()
                || hasLinAccSensor && Sensor.TYPE_LINEAR_ACCELERATION == sensor.getType();
        if (!isEnergySample) {
            return;
        }

        // approximate linear acceleration if we have no special sensor for it
        if (!hasLinAccSensor && Sensor.TYPE_ACCELEROMETER == event.sensor.getType()) {
            linAcc = calcLinAcc(event.values);
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
                energySampleStart = System.currentTimeMillis();
            }

            float timeStep = (System.currentTimeMillis() - prevEnergySampleTime) / 1000f;
            prevEnergySampleTime = System.currentTimeMillis();
            if (timeStep > 0 && timeStep < 1) {
                float accLength = FloatMath.sqrt((float) (Math.pow(linAcc[0], 2)
                        + Math.pow(linAcc[1], 2) + Math.pow(linAcc[2], 2)));

                avgSpeedChange = (avgSpeedCount * avgSpeedChange + accLength) / (avgSpeedCount + 1);
                avgSpeedCount++;
            }
        }

        // check if we gathered enough data points
        if (isSampleComplete()) {
            sendData();
        }
    }

    /**
     * Sends message with average motion energy to the MsgHandler.
     */
    private void sendData() {

            // round to three decimals
            float value = BigDecimal.valueOf(avgSpeedChange).setScale(3, 0).floatValue();

            // prepare intent to send to MsgHandler
            Intent i = new Intent(context.getString(R.string.action_sense_new_data));
            i.putExtra(DataPoint.SENSOR_NAME, SensorNames.MOTION_ENERGY);
            i.putExtra(DataPoint.SENSOR_DESCRIPTION, SensorNames.MOTION_ENERGY);
            i.putExtra(DataPoint.VALUE, value);
            i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.FLOAT);
            i.putExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime());
            context.startService(i);
    }

    @Override
    public void startNewSample() {
        prevEnergySampleTime = 0;
        energySampleStart = 0;
        avgSpeedChange = 0;
        avgSpeedCount = 0;
    }
}
