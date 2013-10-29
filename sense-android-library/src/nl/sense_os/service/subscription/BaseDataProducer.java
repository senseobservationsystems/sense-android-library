package nl.sense_os.service.subscription;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import nl.sense_os.service.shared.SensorDataPoint;

/**
 * <p>
 * Base implementation for the DataProducer interface. This class gives the ability to have
 * {@link DataConsumer} objects subscribed for the produced data.
 * </p>
 * <p>
 * Subscribers are notified when there are new samples via {@link DataConsumer#startNewSample()}.
 * </p>
 * <p>
 * Via the {@link DataConsumer#isSampleComplete()}, the status of the data consumers is checked. If
 * all subscribers return true on this function, then the data producer will wait until a new sample
 * interval starts.
 * </p>
 * <p>
 * Sensor data is passed to the subscribers via their
 * {@link DataConsumer#onNewData(SensorDataPoint)} method.
 * </p>
 * 
 * @author Ted Schmidt <ted@sense-os.nl>
 */
public abstract class BaseDataProducer implements DataProducer {

	protected final String TAG = "BaseDataProducer";
	
    /** The DataProcessors which are subscribed to this sensor for sensor data */
    protected List<DataConsumer> mSubscribers = new ArrayList<DataConsumer>();

    /**
     * Adds a data consumer as subscriber to this data producer. This method is synchronized to
     * prevent concurrent modifications to the list of subscribers.
     * 
     * @param consumer
     *            The DataProcessor that wants the sensor data as input
     * @return <code>true</code> if the DataConsumer was successfully subscribed
     */
    @Override
    public synchronized boolean addSubscriber(DataConsumer consumer) {
        if (!hasSubscriber(consumer)) {
            return mSubscribers.add(consumer);
        } else {
            return false;
        }
    }

    /**
     * Checks subscribers if the sample is complete. This method uses
     * {@link DataConsumer#isSampleComplete()} to see if the sample is completed, or if the data
     * processors need more data.
     * 
     * @return true when all the subscribers have complete samples
     */
    protected synchronized boolean checkSubscribers() {
        boolean complete = true;
        for (DataConsumer subscriber : mSubscribers) {
            if (subscriber != null) {
                complete &= subscriber.isSampleComplete();
            }
        }
        return complete;
    }

    @Override
    public synchronized boolean hasSubscriber(DataConsumer consumer) {
        for (DataConsumer subscriber : mSubscribers) {
            if (subscriber.equals(consumer)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasSubscribers() {
        return !mSubscribers.isEmpty();
    }

    /**
     * Notifies subscribers that a new sample is starting. This method calls
     * {@link DataConsumer#startNewSample()} on each subscriber.
     */
    protected synchronized void notifySubscribers() {
        for (DataConsumer subscriber : mSubscribers) {
            if (subscriber != null) {
                subscriber.startNewSample();
            }
        }
    }

    /**
     * Removes a data subscriber if present. This method is synchronized to prevent concurrent
     * modifications.
     * 
     * @param subscriber
     *            The DataConsumer that should be unsubscribed
     */
    @Override
    public synchronized boolean removeSubscriber(DataConsumer consumer) {
        if (mSubscribers.contains(consumer)) {
            return mSubscribers.remove(consumer);
        }
        return true;
    }

    /**
     * Sends data to all subscribed data consumers.
     * 
     * @param dataPoint
     *            The SensorDataPoint to send
     */
    // TODO: do a-sync
    protected synchronized void sendToSubscribers(SensorDataPoint dataPoint) {
        for (DataConsumer subscriber : mSubscribers) {
            if (subscriber != null && !subscriber.isSampleComplete()) {
            	try
            	{
            		subscriber.onNewData(dataPoint);
            	}
            	catch(Exception e)
            	{
            		Log.e(TAG, "Error sending data to subscriber.", e);
            	}
            }
        }
    }
}
