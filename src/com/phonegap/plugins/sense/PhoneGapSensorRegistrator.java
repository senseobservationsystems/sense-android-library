package com.phonegap.plugins.sense;

import nl.sense_os.service.commonsense.SensorRegistrator;
import android.content.Context;

public class PhoneGapSensorRegistrator extends SensorRegistrator {

    public PhoneGapSensorRegistrator(Context context) {
        super(context);
    }

    @Override
    public boolean verifySensorIds(String deviceType, String deviceUuid) {
        return false;
    }
}
