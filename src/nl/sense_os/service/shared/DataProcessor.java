package nl.sense_os.service.shared;

/**
 * Abstract class for data processors. These processors can register at a Subscribable SenseSensor or DataProcessor directly 
 * or preferably via the SenseService.
 * These modules are to divide the handling of the incoming sensor data over different specific processing
 * implementations.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public abstract class DataProcessor extends Subscribable{

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
     * @param event
     */
    public abstract void onNewData(SensorDataPoint dataPoint);
}
