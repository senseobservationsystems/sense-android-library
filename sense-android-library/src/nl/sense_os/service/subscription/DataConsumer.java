package nl.sense_os.service.subscription;

import org.json.JSONObject;

import java.util.Map;

import nl.sense_os.service.shared.SensorDataPoint;

/**
 * Interface for data consumer. These consumers can register directly at a DataProducer, or
 * preferably via the SenseService. These modules are to divide the handling of the incoming sensor
 * data over different specific processing implementations.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public interface DataConsumer {

    /**
     * Starts a new sample.
     * 
     * @see #isSampleComplete()
     */
    void startNewSample();

    /**
     * @return <code>true</code> if the data consumer has received enough sensor events so that the
     *         sample is complete
     */
    boolean isSampleComplete();

    /**
     * Handles a new data point. Take care: the sensor event is not guaranteed to be from the sensor
     * that the DataConsumer is interested in, so implementing classes need to check this.
     * 
     * @param dataPoint
     */
    void onNewData(SensorDataPoint dataPoint);

    /**
     * Check whether the requirements are updated for a specific sensor
     *
     * Checks whether the requirements have changed since the last time this function was called with this sensor name
     * @param sensorName The name of the sensor to check the requirements update for
     * @return True when the there are requirements for this sensor name and if they changed since
     * the last time the requirementsAreUpdated was called for this sensor name.
     */
    boolean requirementsAreUpdated(String sensorName);

    /**
     * Get the requirements for a specific sensor
     * @param sensorName The sensor name to get the requirements for
     * @return The requirements object if available, null otherwise
     */
    SensorRequirement getRequirement(String sensorName);
}
