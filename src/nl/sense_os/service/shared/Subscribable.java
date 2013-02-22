package nl.sense_os.service.shared;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Interface for sensor data producing components.
 * This interface gives the ability to have subscribers for the produced data.
 * 
 * Subscribers are notified when there are new samples via the DataProcessor method startNewSample()
 * 
 * Via the DataProcessor method isSampleComplete() the status of the data processors is checked.
 * If all subscribers return true on this function, then the Subscribable will wait until a new sample interval starts.
 * 
 * Sensor data is passed to the subscribers via the DataProcessor method onNewData(SensorData data)
 * 
 * @author Ted Schmidt <ted@sense-os.nl>
 * 
 */
public interface Subscribable {

    /**
     * Add a subscriber
     * 
     * This methods add a data processor as subscriber for new sensor data
     * A subscriber is only added once to the subscribers list 
     * 
     * @param subscriber 
     * 		The subscriber for the sensor data      
     */
    public void addSubscriber(AtomicReference<DataProcessor> subscriber);

    /**
     * Remove a subscriber
     * 
     * This method removes a data processor from the subscriber list.
     *
     * @param subscriber 
     * 		The subscriber for the sensor data      
     */
    public void removeSubscriber(AtomicReference<DataProcessor> subscriber);
            
}
