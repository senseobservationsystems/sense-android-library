package nl.sense_os.datastorageengine;

/**
 * A progress callback can be passed to method DataSyncer.sync(callback) to get notified during
 * the different steps that the syncing process takes. The callbacks are triggered in the
 * following order, and are only invoked once:
 *
 *   1. onDeletionCompleted
 *   2. onUploadCompeted
 *   3. onDownloadSensorsCompleted
 *   4. onDownloadSensorDataCompleted
 *   5. onCleanupCompleted
 *
 */
public interface ProgressCallback {
    /**
     * Callback method called after data scheduled for deletion is actually deleted from
     * remote.
     **/
    void onDeletionCompleted();

    /**
     * Callback called after all local data is uploaded to remote
     */
    void onUploadCompeted();

    /**
     * Callback called after all remote sensors (not their data) are downloaded to local
     */
    void onDownloadSensorsCompleted();

    /**
     * Callback called after all remote sensor data is downloaded to local
     */
    void onDownloadSensorDataCompleted();

    /**
     * Callback called after all outdated local data is cleaned up. Data is kept locally for a
     * certain period only, and removed from local when older than this period and synced to remote.
     */
    void onCleanupCompleted();
}
