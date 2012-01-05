package nl.sense_os.service.external_sensors;

import java.util.HashMap;

import nl.sense_os.service.commonsense.SensorRegistrator;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.SensorNames;

import org.json.JSONObject;

import android.content.Context;

public class ZephyrBioHarnessRegistrator extends SensorRegistrator {

    public ZephyrBioHarnessRegistrator(Context context) {
        super(context);
    }

    @Override
    public boolean verifySensorIds(String deviceType, String deviceUuid) {
        // preallocate
        String name, displayName, description, dataType, value;
        HashMap<String, Object> dataFields = new HashMap<String, Object>();
        boolean success = true;

        // match accelerometer
        name = SensorNames.ACCELEROMETER;
        displayName = "acceleration";
        description = "BioHarness " + deviceType;
        dataType = SenseDataTypes.JSON;
        dataFields.clear();
        dataFields.put("x-axis", 1.1);
        dataFields.put("y-axis", 1.1);
        dataFields.put("z-axis", 1.1);
        value = new JSONObject(dataFields).toString();
        success &= checkSensor(name, displayName, dataType, description, value, deviceType,
                deviceUuid);

        // match heart rate
        name = SensorNames.HEART_RATE;
        displayName = SensorNames.HEART_RATE;
        description = "BioHarness " + deviceType;
        dataType = SenseDataTypes.INT;
        value = "0";
        success &= checkSensor(name, displayName, dataType, description, value, deviceType,
                deviceUuid);

        // match respiration rate sensor
        name = SensorNames.RESPIRATION;
        displayName = SensorNames.RESPIRATION;
        description = "BioHarness " + deviceType;
        dataType = SenseDataTypes.FLOAT;
        value = "0.0";
        success &= checkSensor(name, displayName, dataType, description, value, deviceType,
                deviceUuid);

        // match skin temperature sensor
        name = SensorNames.TEMPERATURE;
        displayName = "skin temperature";
        description = "BioHarness " + deviceType;
        dataType = SenseDataTypes.FLOAT;
        value = "0.0";
        success &= checkSensor(name, displayName, dataType, description, value, deviceType,
                deviceUuid);

        // match battery level sensor
        name = SensorNames.BATTERY_LEVEL;
        displayName = SensorNames.BATTERY_LEVEL;
        description = "BioHarness " + deviceType;
        dataType = SenseDataTypes.INT;
        value = "0";
        success &= checkSensor(name, displayName, dataType, description, value, deviceType,
                deviceUuid);

        // match worn status sensor
        name = SensorNames.WORN_STATUS;
        displayName = SensorNames.WORN_STATUS;
        description = "BioHarness " + deviceType;
        dataType = SenseDataTypes.BOOL;
        value = "true";
        success &= checkSensor(name, displayName, dataType, description, value, deviceType,
                deviceUuid);

        return success;
    }

}
