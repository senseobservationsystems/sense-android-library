package nl.sense_os.service.shared;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;
/**
 * This is the base class for all the Sense Sensors.
 * This class implements the Subscribable interface.
 * This allows data processors to subscribe to the output data stream of this SenseSensor.
 * 
 * TODO: find common sensor features and implement them here 
 * @author Ted Schmidt <ted@sense-os.nl>
 *
 */
public abstract class SenseSensor implements Subscribable {

	/** The delay value for the sample rate */
	protected long sampleDelay = 0;
	
	/** The DataProcessors which are subscribed to this sensor for sensor data*/
	protected Vector<AtomicReference<DataProcessor> > subscribers = new Vector<AtomicReference<DataProcessor> >();;
		
	/**
	 * Start sensing
	 * 
	 * This method starts the sensor to sense at a certain sample rate.
	 * 	 
	 * @param sampleDelay
	 * 		Specifies the initial sample rate.
	 */
    public abstract void startSensing(long sampleDelay);
   
    /**
     *  Stop sensing
     *  
     *  This method stops the sensing.
     */
    public abstract void stopSensing();

    /**
     * Get the sample rate
     * 
     * This method returns the sample rate by returning the sample delay value.
     * @return long The sample delay value in seconds
     */
    public long getSampleRate()
    {
    	return sampleDelay;
    }

    /**
     * Set the sample rate
     * 
     * This method sets the rate at which the sensor starts its sensing.
     * A subscribed DataProcessor will get data at least with the frequency of this sample delay,
     * if this SenseSensor has new data.
     * @param sampleDelay
     * 		The sample delay value in seconds
     */
    public void setSampleRate(long sampleDelay)
    {
    	this.sampleDelay = sampleDelay;
    }

    /**
     * Add a sensor data subscriber
     * 
     * This methods adds a DataProcessor as sensor data subscriber for this SenseSensor.
     * If an instance of the DataProcessor is already subscribed then it will be ignored
     * @param subscriber
     * 		The DataProcessor that wants the sensor data as input
     */
	@Override
	public void addSubscriber(AtomicReference<DataProcessor>  subscriber) 
	{
		if(!subscribers.contains(subscriber))		
			subscribers.add(subscriber);
	}

	/**
	 * Remove Subscriber
	 * 
	 * This method removes a data subscriber if present.
	 * @param subscriber
	 * 		The DataProcessor needs to be un-subscribed
	 */
	@Override
	public void removeSubscriber(AtomicReference<DataProcessor> subscriber) 
	{
		if(subscribers.contains(subscriber))
			subscribers.remove(subscriber);
	}
	
	/**
	 * Send data to DataProcessors
	 * 
	 * This method sends a SensorDataPoint to all the subscribers.
	 * @param dataPoint
	 * 		The SensorDataPoint to send
	 */
	protected void sendToSubscribers(SensorDataPoint dataPoint)
	{
		for (int i = 0; i < subscribers.size(); i++)
		{
			DataProcessor dp =  subscribers.get(i).get();
			if(dp != null && !dp.isSampleComplete())
				dp.onNewData(dataPoint);
		}
			
	}
	
	/**
	 * Notify subscribers of new sample
	 * 
	 * This method calls the startNewSample function of the subscribers to notify that a new sample will be send. 
	 */
	protected void notifySubscribers()
	{
		for (int i = 0; i < subscribers.size(); i++)		
		{
			DataProcessor dp =  subscribers.get(i).get();
			if(dp != null)
				dp.startNewSample();
		}
	}
	
	/**
	 * Check subscribers for complete sample
	 * 
	 * This method calls the isSampleComplete function of the subscribers to see if they need more data.
	 * 
	 * 
	 * @return boolean 
	 * 		Returns true when all the subscribers have complete samples.
	 */
	protected Boolean checkSubscribers()
	{
		Boolean isComplete = true;
		for (int i = 0; i < subscribers.size(); i++)		
		{
			DataProcessor dp =  subscribers.get(i).get();
			if(dp != null)
				isComplete &= dp.isSampleComplete();
		}
		return isComplete;
	}
}
