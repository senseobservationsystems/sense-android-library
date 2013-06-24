package nl.sense_os.service.subscription;

import java.util.ArrayList;
import java.util.List;

import nl.sense_os.service.shared.SensorDataPoint;

/**
 * <p>
 * Base implementation for the DataProducer interface. This class gives the ability to have
 * DataProcessors subscribed for the produced data.
 * </p>
 * <p>
 * Subscribers are notified when there are new samples via the DataProcessor method
 * {@link DataConsumer#startNewSample()}
 * </p>
 * <p>
 * Via the DataProcessor method isSampleComplete() the status of the data processors is checked. If
 * all subscribers return true on this function, then the DataProducer will wait until a new sample
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

    /** The DataProcessors which are subscribed to this sensor for sensor data */
    protected List<DataConsumer> mSubscribers = new ArrayList<DataConsumer>();

    @Override
    public boolean addSubscriber(DataConsumer consumer) {
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
    protected boolean checkSubscribers() {
        boolean complete = true;
        for (DataConsumer subscriber : mSubscribers) {
            if (subscriber != null) {
                complete &= subscriber.isSampleComplete();
            }
        }
        return complete;
    }

    @Override
    public boolean hasSubscriber(DataConsumer consumer) {
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
    protected void notifySubscribers() {
        for (DataConsumer subscriber : mSubscribers) {
            if (subscriber != null) {
                subscriber.startNewSample();
            }
        }
    }

    @Override
    public void removeSubscriber(DataConsumer consumer) {
        if (mSubscribers.contains(consumer)) {
            mSubscribers.remove(consumer);
        }
    }

    /**
     * Sends data to all subscribed DataProcessors.
     * 
     * @param dataPoint
     *            The SensorDataPoint to send
     */
    protected void sendToSubscribers(SensorDataPoint dataPoint) {
        // TODO: do a-sync
        for (DataConsumer subscriber : mSubscribers) {
            if (subscriber != null && !subscriber.isSampleComplete()) {
                subscriber.onNewData(dataPoint);
            }
        }
    }
}
