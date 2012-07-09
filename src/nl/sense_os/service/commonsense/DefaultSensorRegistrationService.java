package nl.sense_os.service.commonsense;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class DefaultSensorRegistrationService extends IntentService {

    private static final String TAG = "DefaultSensorRegistrationService";
    private SensorRegistrator verifier = new DefaultSensorRegistrator(this);

    public DefaultSensorRegistrationService() {
	super("DefaultSensorRegistrationService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
	String deviceType = SenseApi.getDefaultDeviceType(this);
	String deviceUuid = SenseApi.getDefaultDeviceUuid(this);
	if (verifier.verifySensorIds(deviceType, deviceUuid)) {
	    Log.v(TAG, "Sensor IDs verified");
	    //TODO: put sensor ID registration
	} else {
	    // hopefully the IDs will be checked again
	}
    }
}
