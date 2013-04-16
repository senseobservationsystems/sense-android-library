package nl.sense_os.service.motion;

import android.hardware.SensorEvent;

/**
 * Interface for motion sensor implementations. Used by the main {@link MotionSensor} to divide the
 * handling of the incoming sensor data over different specific sensor implementations.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public interface MotionSensorInterface {

    /**
     * Starts a new sample.
     * 
     * @see #isSampleComplete()
     */
    void startNewSample();

    /**
     * @return <code>true</code> if the motion sensor has received enough sensor events so that the
     *         sample is complete
     */
    boolean isSampleComplete();

    /**
     * Handles a new data point. Nota bene: the sensor event is not guaranteed to be from the sensor
     * that the MotionSensorInterface is interested in, some implementing classes need to check this.
     * 
     * @param event
     */
    void onNewData(SensorEvent event);
}
