package nl.sense_os.service.external_sensors;

import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.SensorNames;
import android.content.Context;

/**
 * Helper class that registers sensors for a Zephyr HxM device:
 * <ul>
 * <li>heart rate</li>
 * <li>speed</li>
 * <li>distance</li>
 * <li>battery charge</li>
 * <li>strides</li>
 * </ul>
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class ZephyrHxmRegistrator{

    public ZephyrHxmRegistrator() {

    }

    public boolean verifySensorIds(String deviceType, String deviceUuid) {
        // preallocate
        String name, displayName, description, dataType, value;
        boolean success = true;

        // match heart rate sensor
        name = SensorNames.HEART_RATE;
        displayName = SensorNames.HEART_RATE;
        description = "HxM " + deviceType;
        dataType = SenseDataTypes.INT;
        value = "0";
//        success &= checkSensor(name, displayName, dataType, description, value, deviceType,
//                deviceUuid);

        // match speed sensor
        name = SensorNames.SPEED;
        displayName = SensorNames.SPEED;
        description = "HxM " + deviceType;
        dataType = SenseDataTypes.FLOAT;
        value = "0.0";
//        success &= checkSensor(name, displayName, dataType, description, value, deviceType,
//                deviceUuid);

        // match distance sensor
        name = SensorNames.DISTANCE;
        displayName = SensorNames.DISTANCE;
        description = "HxM " + deviceType;
        dataType = SenseDataTypes.FLOAT;
        value = "0.0";
//        success &= checkSensor(name, displayName, dataType, description, value, deviceType,
//                deviceUuid);

        // match battery charge sensor
        name = SensorNames.BATTERY_CHARGE;
        displayName = SensorNames.BATTERY_CHARGE;
        description = "HxM " + deviceType;
        dataType = SenseDataTypes.INT;
        value = "0";
//        success &= checkSensor(name, displayName, dataType, description, value, deviceType,
//                deviceUuid);

        // match strides sensor
        name = SensorNames.STRIDES;
        displayName = SensorNames.STRIDES;
        description = "HxM " + deviceType;
        dataType = SenseDataTypes.INT;
        value = "0";
//        success &= checkSensor(name, displayName, dataType, description, value, deviceType,
//                deviceUuid);

        return success;
    }

}
