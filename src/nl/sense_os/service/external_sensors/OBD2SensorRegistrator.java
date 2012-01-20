package nl.sense_os.service.external_sensors;

import nl.sense_os.service.commonsense.SensorRegistrator;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.SensorNames;
import android.content.Context;

public class OBD2SensorRegistrator extends SensorRegistrator {

    public OBD2SensorRegistrator(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    @Override
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
        success &= checkSensor(name, displayName, dataType, description, value, deviceType,
                deviceUuid);

        return success;
    }
}
