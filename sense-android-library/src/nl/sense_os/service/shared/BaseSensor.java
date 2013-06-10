package nl.sense_os.service.shared;

/**
 * This is the base class for all the Sense sensors. This class extends the DataProducer interface.
 * This allows data processors to subscribe to the output data stream of this SenseSensor.
 * 
 * TODO: find common sensor features and implement them here
 * 
 * @author Ted Schmidt <ted@sense-os.nl>
 * 
 */
public abstract class BaseSensor extends BaseDataProducer implements DataProducer, SenseSensor {

	/** The delay value for the sample rate */
    private long sampleDelay = 0;

    @Override
    public long getSampleRate() {
    	return sampleDelay;
    }

    @Override
    public void setSampleRate(long sampleDelay) {
    	this.sampleDelay = sampleDelay;
    }

}
