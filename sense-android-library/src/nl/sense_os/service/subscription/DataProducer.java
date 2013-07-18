package nl.sense_os.service.subscription;

/**
 * Interface for sensor data producing components that support subscriptions. Implementing classes
 * should provide the ability to have subscribers for the produced data.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 * 
 */
public interface DataProducer {

    /**
     * Adds a data consumer as subscriber for data from this producer. If an instance of the
     * DataConsumer is already subscribed, then it should be ignored.
     * 
     * @param subscriber
     *            The DataConsumer that wants the sensor data as input
     * @return <code>true</code> if the DataConsumer was successfully subscribed.
     */
    public abstract boolean addSubscriber(DataConsumer subscriber);

    /**
     * Checks if a DataProcessor has been added as a subscriber.
     * 
     * @param subscriber
     * @return <code>true</code> if the DataConsumer is listed as a subscriber
     */
    public abstract boolean hasSubscriber(DataConsumer subscriber);

    /**
     * @return <code>true</code> if there are subscribers
     */
    public abstract boolean hasSubscribers();

    /**
     * Removes a data subscriber if present.
     * 
     * @param subscriber
     *            The DataConsumer that should be unsubscribed
     * @return <code>true</code> if the subscriber was removed
     */
    public abstract boolean removeSubscriber(DataConsumer subscriber);

}
