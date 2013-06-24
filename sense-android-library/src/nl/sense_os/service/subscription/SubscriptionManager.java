package nl.sense_os.service.subscription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Manager class for keeping track of DataProducers and DataConsumers
 * </p>
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class SubscriptionManager {

    /** Singleton instance */
    private static SubscriptionManager sInstance;

    /**
     * Factory method for singleton pattern
     * 
     * @return The singleton instance of the subscription manager
     */
    public static SubscriptionManager getInstance() {
        if (null == sInstance) {
            sInstance = new SubscriptionManager();
        }
        return sInstance;
    }

    /** All DataConsumers, mapped by the name of the producer they are subscribed to. */
    private Map<String, List<DataConsumer>> mConsumers;

    /** All registered DataProducers, mapped by their name. */
    private Map<String, List<DataProducer>> mProducers;

    /**
     * Constructor.
     * 
     * @param context
     * @see #getInstance()
     */
    protected SubscriptionManager() {
        mConsumers = new HashMap<String, List<DataConsumer>>();
        mProducers = new HashMap<String, List<DataProducer>>();
    }

    /**
     * @param name
     *            The name of the DataProducer
     * @return A List of data producers that are registered under the given sensor name
     */
    public List<DataProducer> getRegisteredProducers(String name) {
        return mProducers.get(name);
    }

    /**
     * @param name
     *            The name of the DataProcessor
     * @return A List of data consumers that are subscribed to the given sensor name
     */
    public List<DataConsumer> getSubscribedConsumers(String name) {
        return mConsumers.get(name);
    }

    /**
     * @param name
     *            The name of the DataProducer
     * @param consumer
     *            The data consumer instance to check for
     * @return <code>true</code> if this consumer is already subscribed for producers with this name
     */
    public boolean isConsumerSubscribed(String name, DataConsumer consumer) {

        if (!mConsumers.containsKey(name)) {
            // nothing is subscribed to this sensor name
            return false;
        }

        // check if dataProcessor is in the list of subscribed processors
        List<DataConsumer> processors = mConsumers.get(name);
        for (DataConsumer registeredProcessor : processors) {
            if (registeredProcessor.equals(consumer)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param name
     *            The name of the DataProducer
     * @return true if any data producer is already registered under this sensor name
     */
    public boolean isProducerRegistered(String name) {
        return mProducers.containsKey(name);
    }

    /**
     * @param name
     *            The name of the DataProducer
     * @param producer
     *            The DataProducer instance to check for
     * @return true if the data producer is already registered under this sensor name
     */
    public boolean isProducerRegistered(String name, DataProducer producer) {

        if (!isProducerRegistered(name)) {
            // nothing is registered under this sensor name
            return false;
        }

        // check if producer is already in the list of registered producers
        List<DataProducer> producers = mProducers.get(name);
        for (DataProducer registeredProducer : producers) {
            if (registeredProducer.equals(producer)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * Registers a DataProducer with the given name at the SenseService.
     * </p>
     * <p>
     * When a data producer is registered, data consumers can subscribe to its sensor data.
     * Registering a data producer with an existing name will add the new data producer only if this
     * data producer is a different instance from the other data producer.
     * </p>
     * 
     * @param name
     *            The name of the data producer
     * @param producer
     *            The data producer
     */
    public synchronized void registerProducer(String name, DataProducer producer) {

        if (isProducerRegistered(name, producer)) {
            // data producer is already registered
            return;
        }

        // add the producer to the list of registered producers
        List<DataProducer> producers = getRegisteredProducers(name);
        if (null == producers) {
            producers = new ArrayList<DataProducer>();
        }
        producers.add(producer);
        mProducers.put(name, producers);

        // see if there are any data consumers subscribed to this sensor name
        List<DataConsumer> subscribers = getSubscribedConsumers(name);
        if (null != subscribers) {
            // subscribe existing DataProcessors to the new DataProducer
            for (DataConsumer subscriber : subscribers) {
                producer.addSubscriber(subscriber);
            }
        }
    }

    /**
     * <p>
     * Subscribes a DataConsumer to a DataProducer
     * </p>
     * <p>
     * This method subscribes a DataProcessor to receive SensorDataPoints from a DataProducer. If
     * the DataProducer with name to subscribe to is not registered yet then the data processor will
     * be put in the queue and will be subscribed to the DataProducer when it is registered.
     * </p>
     * 
     * @param name
     *            The name of the registered DataProducer
     * @param consumer
     *            The DataConsumer that receives the sensor data
     * @return true if the DataConsumer successfully subscribed to the DataProducer.
     */
    public synchronized boolean subscribeConsumer(String name, DataConsumer consumer) {

        if (isConsumerSubscribed(name, consumer)) {
            // consumer is already subscribed
            return false;
        }

        // add the consumer to the list of subscribed consumers
        List<DataConsumer> consumers = getSubscribedConsumers(name);
        if (null == consumers) {
            consumers = new ArrayList<DataConsumer>();
        }
        consumers.add(consumer);
        mConsumers.put(name, consumers);

        // see if there are any producers for this sensor name
        List<DataProducer> producers = getRegisteredProducers(name);
        if (producers == null) {
            // nothing to subscribe to (yet)
            return true;
        } else {
            // subscribe the new processor to the existing producers
            boolean subscribed = false;
            for (DataProducer producer : producers) {
                subscribed |= producer.addSubscriber(consumer);
            }
            return subscribed;
        }
    }

    /**
     * Unregisters a DataProducer.<br/>
     * <br/>
     * No new data consumers can subscribe to the DataProducer anymore, but DataConsumer which have
     * already subscribed to the DataProducer will remain subscribed.
     * 
     * @param name
     *            The name that the DataProducer is registered under
     * @param producer
     *            The DataProducer to unregister
     */
    public synchronized void unregisterProducer(String name, DataProducer producer) {

        if (!mProducers.containsKey(name)) {
            // this producer is not registered under this name
            return;
        }

        // remove the producer from the list of registered producers for this sensor name
        List<DataProducer> dataProducers = getRegisteredProducers(name);
        dataProducers.remove(producer);

        // update the map of registered producers
        if (dataProducers.size() == 0) {
            mProducers.remove(name);
        } else {
            mProducers.put(name, dataProducers);
        }
    }

    /**
     * Unsubscribes a data consumer from a data producer.
     * 
     * @param name
     *            The name of the DataProducer that the consumer registered for
     * @param consumer
     *            The DataConsumer that receives the sensor data
     */
    public synchronized void unsubscribeConsumer(String name, DataConsumer consumer) {

        if (!mConsumers.containsKey(name)) {
            // there are no consumers registered for this name (?)
            return;
        }

        // remove the processor from the list of subscribed processors
        List<DataConsumer> consumers = getSubscribedConsumers(name);
        consumers.remove(consumer);

        // update the map of consumers
        if (consumers.size() == 0) {
            mConsumers.remove(name);
        } else {
            mConsumers.put(name, consumers);
        }

        // unsubscribe the consumer from the producers for this sensor name
        List<DataProducer> producers = getRegisteredProducers(name);
        for (DataProducer registeredProducer : producers) {
            registeredProducer.removeSubscriber(consumer);
        }
    }
}
