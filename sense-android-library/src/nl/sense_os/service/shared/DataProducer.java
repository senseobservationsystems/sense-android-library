package nl.sense_os.service.shared;


/**
 * Interface for sensor data producing components that support subscriptions. Implementing classes
 * should provide the ability to have subscribers for the produced data.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 * 
 */
public interface DataProducer {

    /**
     * Adds a DataProcessor as sensor data subscriber for this SenseSensor. If an instance of the
     * DataProcessor is already subscribed then it should be ignored.
     * 
     * @param subscriber
     *            The DataProcessor that wants the sensor data as input
     * @return True if the DataProcessor could subscribe to the service, false if the DataProcessor
     *         was already subscribed
     */
    public abstract boolean addSubscriber(DataProcessor subscriber);

    /**
     * Checks if a DataProcessor has been added as a subscriber.
     * 
     * @param subscriber
     * @return True if the DataProcessor is listed as a subscriber
     */
    public abstract boolean hasSubscriber(DataProcessor subscriber);

    /**
     * @return True if there are subscribers
     */
    public abstract boolean hasSubscribers();

    /**
     * Removes a data subscriber if present.
     * 
     * @param subscriber
     *            The DataProcessor needs to be unsubscribed
     */
    public abstract void removeSubscriber(DataProcessor subscriber);

}
