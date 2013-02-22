package nl.sense_os.service.shared;

/**
 * Interface for sensors that use the {@link PeriodicPollAlarmReceiver} to periodically poll their
 * data source.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public abstract class PeriodicPollingSensor extends SenseSensor {

    /**
     * Called by the {@link PeriodicPollAlarmReceiver} to check if the sensor is still active.
     * 
     * @return true if the sensor is active
     */
	public boolean isActive(){
    	return false;
    }

    /**
     * Performs a sample. Called by the {@link PeriodicPollAlarmReceiver}.
     */
    public abstract void doSample();
}
