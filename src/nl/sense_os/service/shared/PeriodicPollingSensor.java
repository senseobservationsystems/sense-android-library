package nl.sense_os.service.shared;

/**
 * Interface for sensors that use the {@link PeriodicPollAlarmReceiver} to periodically poll their
 * data source.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public interface PeriodicPollingSensor {

    /**
     * Called by the {@link PeriodicPollAlarmReceiver} to check if the sensor is still active.
     * 
     * @return true if the sensor is active
     */
    boolean isActive();

    /**
     * Performs a sample. Called by the {@link PeriodicPollAlarmReceiver}.
     */
    void doSample();
}
