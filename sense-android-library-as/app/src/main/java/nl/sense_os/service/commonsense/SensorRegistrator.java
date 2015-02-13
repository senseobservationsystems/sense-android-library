package nl.sense_os.service.commonsense;

import android.content.Context;
import android.util.Log;

/**
 * Helper class that registers the sensors for a device at CommonSense.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public abstract class SensorRegistrator {

    private static final String TAG = "SensorRegistrator";
    private Context mContext;

    public SensorRegistrator(Context context) {
        mContext = context;
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
    public boolean checkSensor(String name, String displayName, String dataType,
            String description, String value, String deviceType, String deviceUuid) {
        Log.d(TAG, "check " + name + " (" + description + ") @ " + deviceUuid);
        try {
            // set default device type and UUID if it is not specified
            deviceUuid = deviceUuid != null ? deviceUuid : SenseApi.getDefaultDeviceUuid(mContext);
            deviceType = deviceType != null ? deviceType : SenseApi.getDefaultDeviceType(mContext);

            if (null == SenseApi.getSensorId(mContext, name, description, dataType, deviceUuid)) {
                SenseApi.registerSensor(mContext, name, displayName, description, dataType, value,
                        deviceType, deviceUuid);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to check '" + name + "' sensor ID at CommonSense! " + e);
            return false;
        }
        return true;
    }

    /**
     * @return The Context
     */
    public Context getContext() {
        return mContext;
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
