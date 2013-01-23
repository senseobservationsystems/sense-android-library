package nl.sense_os.service.shared;

public interface SenseSensor {

    void startSensing(long sampleRate);

    void stopSensing();

    long getSampleRate();

    void setSampleRate(long sampleRate);
}
