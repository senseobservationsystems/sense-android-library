package com.phonegap.plugins.sense;

import nl.sense_os.service.commonsense.SensorRegistrator;
import android.content.Context;

/**
 * Sensor registration class for PhoneGap plugin.<br/>
 * <br/>
 * Because we do not know the properties of the sensors that will be registered through PhoneGap,
 * {@link #verifySensorIds(String, String)} is a trivial implementation. Users should call
 * {@link #checkSensor(String, String, String, String, String, String, String)} directly instead.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class PhoneGapSensorRegistrator extends SensorRegistrator {

    public PhoneGapSensorRegistrator(Context context) {
        super(context);
    }

    @Override
    public boolean verifySensorIds(String deviceType, String deviceUuid) {
        return false;
    }
}
