package nl.sense_os.service.shared;

public interface SenseSensor {

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
    public abstract long getSampleRate();

    /**
     * Set the sample rate
     * 
     * This method sets the rate at which the sensor starts its sensing.
     * A subscribed DataProcessor will get data at least with the frequency of this sample delay,
     * if this SenseSensor has new data.
     * @param sampleDelay
     * 		The sample delay value in seconds
     */
    public abstract void setSampleRate(long sampleDelay);

}