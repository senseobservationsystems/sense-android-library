package nl.sense_os.service.shared;

/**
 * Interface for data processors. These processors can register directly at a DataProducer, or
 * preferably via the SenseService. These modules are to divide the handling of the incoming sensor
 * data over different specific processing implementations.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public interface DataProcessor {

    /**
     * Starts a new sample.
     * 
     * @see #isSampleComplete()
     */
    public abstract void startNewSample();

    /**
     * @return <code>true</code> if the data processor has received enough sensor events so that the
     *         sample is complete
     */
    public abstract boolean isSampleComplete();

    /**
     * Handles a new data point. Take care: the sensor event is not guaranteed to be from the sensor
     * that the DataProcessor is interested in, so implementing classes need to check this.
     * 
     * @param dataPoint
     */
    public abstract void onNewData(SensorDataPoint dataPoint);
}
