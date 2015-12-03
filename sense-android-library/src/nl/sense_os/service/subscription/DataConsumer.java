package nl.sense_os.service.subscription;

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
}
