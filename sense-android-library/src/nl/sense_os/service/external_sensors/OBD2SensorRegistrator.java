package nl.sense_os.service.external_sensors;

import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.SensorNames;
import android.content.Context;

/**
 * Helper class that registers sensors for an ODB2 device. Only registers the vehicle speed sensor
 * at this time!
 * 
 * @author Roelof van den Berg <roelof@sense-os.nl>
 */
public class OBD2SensorRegistrator {

    public OBD2SensorRegistrator() {
    }

    public boolean verifySensorIds(String deviceType, String deviceUuid) {
        // preallocate
        String name, displayName, description, dataType, value;
        boolean success = true;

        // match strides sensor
        name = SensorNames.VEHICLE_SPEED;
        displayName = SensorNames.VEHICLE_SPEED;
        description = deviceType;
        dataType = SenseDataTypes.INT;
        value = "0";
//        success &= checkSensor(name, displayName, dataType, description, value, deviceType,
//                deviceUuid);

        return success;
    }
}
