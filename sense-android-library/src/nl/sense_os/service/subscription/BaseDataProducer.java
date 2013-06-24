package nl.sense_os.service.subscription;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;

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
    protected Vector<AtomicReference<DataConsumer>> subscribers = new Vector<AtomicReference<DataConsumer>>();

    @Override
    public boolean addSubscriber(DataConsumer dataConsumer) {
        if (!hasSubscriber(dataConsumer)) {
            AtomicReference<DataConsumer> subscriber = new AtomicReference<DataConsumer>(
                    dataConsumer);
            subscribers.add(subscriber);
        } else {
            return false;
        }
        return true;
    }

    /**
     * Checks subscribers if the sample is complete. This method uses
     * {@link DataConsumer#startNewSample()} to see if the sample is completed, or if the data
     * processors need more data.
     * 
     * @return true when all the subscribers have complete samples
     */
    protected boolean checkSubscribers() {
        boolean isComplete = true;
        for (int i = 0; i < subscribers.size(); i++) {
            DataConsumer dp = subscribers.get(i).get();
            if (dp != null) {
                isComplete &= dp.isSampleComplete();
            }
        }
        return isComplete;
    }

    @Override
    public boolean hasSubscriber(DataConsumer dataConsumer) {
        AtomicReference<DataConsumer> subscriber = new AtomicReference<DataConsumer>(
                dataConsumer);

        for (int i = 0; i < subscribers.size(); i++) {
            AtomicReference<DataConsumer> item = subscribers.elementAt(i);
            if (item.get() == subscriber.get())
                return true;
        }
        return false;
    }

    @Override
    public boolean hasSubscribers() {
        return !subscribers.isEmpty();
    }

    /**
     * Notifies subscribers that a new sample is starting. This method calls
     * {@link DataConsumer#startNewSample()} on each subscriber.
     */
    protected void notifySubscribers() {
        for (int i = 0; i < subscribers.size(); i++) {
            DataConsumer dp = subscribers.get(i).get();
            if (dp != null) {
                dp.startNewSample();
            }
        }
    }

    @Override
    public void removeSubscriber(DataConsumer dataConsumer) {
        AtomicReference<DataConsumer> subscriber = new AtomicReference<DataConsumer>(
                dataConsumer);

        for (int i = 0; i < subscribers.size(); i++) {
            AtomicReference<DataConsumer> item = subscribers.elementAt(i);
            if (item.get() == subscriber.get()) {
                subscribers.removeElementAt(i);
                --i;
            }
        }
        if (subscribers.contains(subscriber)) {
            subscribers.remove(subscriber);
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
        for (int i = 0; i < subscribers.size(); i++) {
            DataConsumer dp = subscribers.get(i).get();
            if (dp != null && !dp.isSampleComplete()) {
                dp.onNewData(dataPoint);
            }
        }
    }
}
