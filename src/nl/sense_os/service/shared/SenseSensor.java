package nl.sense_os.service.shared;

/**
 * This is the base class for all the Sense Sensors.
 * This class extends the Subscribable class.
 * This allows data processors to subscribe to the output data stream of this SenseSensor.
 * 
 * TODO: find common sensor features and implement them here 
 * @author Ted Schmidt <ted@sense-os.nl>
 *
 */
public abstract class SenseSensor extends Subscribable {

	/** The delay value for the sample rate */
	protected long sampleDelay = 0;
		
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

   
}
