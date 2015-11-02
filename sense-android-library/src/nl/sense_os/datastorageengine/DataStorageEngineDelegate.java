package nl.sense_os.datastorageengine;

import java.util.List;

/**
 * The DataStorageEngineDelegate interface/protocol defines the methods used to receive updates from a DataStorageEngine.

 * Created by ted on 11/2/15.
 */
public interface DataStorageEngineDelegate {

    /**
     * Callback method called on the completion of DSE initialization
     **/
    void onDSEReady();

    /**
     * Callback method on the completion of downloading Sensors from Remote.
     * @param sensors list/array of sensors that are downloaded from Remote.
     **/
    void onSensorsDownloaded(List<Sensor> sensors);

    /**
     * Callback method called when the flush of local data to the back-end is completed
     * @param successful Whether the data flush was successful or not
     **/
    void onFlushDataCompleted(boolean successful);
}
