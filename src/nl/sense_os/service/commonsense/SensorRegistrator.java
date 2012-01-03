package nl.sense_os.service.commonsense;

import android.content.Context;
import android.util.Log;

/**
 * Helper class that registers the sensors for a device at CommonSense.
 */
public abstract class SensorRegistrator {

    protected final Context context;

    public SensorRegistrator(Context context) {
        this.context = context;
    }

    /**
     * Verifies existence of a sensor at CommonSense, adding it to the list of registered sensors if
     * it was newly created.
     * 
     * @param name
     *            Sensor name.
     * @param displayName
     *            Pretty display name for the sensor.
     * @param dataType
     *            Sensor data type.
     * @param description
     *            Sensor description (previously 'device_type')
     * @param value
     *            Dummy sensor value (used for producing a data structure).
     * @param deviceType
     *            Type of device that the sensor belongs to.
     * @param deviceUuid
     *            UUID of the sensor's device.
     * @return true if the sensor ID was found or created
     */
    protected synchronized boolean checkSensor(String name, String displayName, String dataType,
            String description, String value, String deviceType, String deviceUuid) {
        try {
            if (null == SenseApi.getSensorId(context, name, description, dataType, deviceUuid)) {
                SenseApi.registerSensor(context, name, displayName, description, dataType, value,
                        deviceType, deviceUuid);
            }
        } catch (Exception e) {
            Log.w("CommonSense sensor registration", "Failed to check '" + name
                    + "' sensor ID at CommonSense! " + e);
            return false;
        }
        return true;
    }

    /**
     * Verifies that all of the phone's sensors exist at CommonSense, and that the phone knows their
     * sensor IDs.
     * 
     * @param deviceType
     *            The type of device that the sensor is connected to. Use null to connect the
     *            sensors to the phone itself.
     * @param deviceUuid
     *            The UUID of the sensor's device. Use null to connect the sensors to the phone
     *            itself.
     */
    public abstract boolean verifySensorIds(String deviceType, String deviceUuid);
}
