package nl.sense_os.service.commonsense;

import static nl.sense_os.service.push.GCMReceiver.SENDER_ID;

import java.io.IOException;

import org.json.JSONException;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;

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
		registerGCM();
		if (verifier.verifySensorIds(deviceType, deviceUuid)) {
			Log.v(TAG, "Sensor IDs verified");
		} else {
			// hopefully the IDs will be checked again
		}
	}

	@Override
	public void onDestroy() {
		GCMRegistrar.onDestroy(this); // so that it will not leak
		super.onDestroy();
	}

	/**
	 * Register the device to use google GCM
	 */
	private void registerGCM() {

		// get the registration ID
		String registrationId = null;
		try {
			GCMRegistrar.checkDevice(this);
			GCMRegistrar.checkManifest(this);
			registrationId = GCMRegistrar.getRegistrationId(this);
		} catch (IllegalStateException e) {
			Log.w(TAG, "This application does not have the GCM permission");
			return;
		}

		if (registrationId.equals("")) {
			Log.v(TAG, "Device is not registered to gcm, registering");
			GCMRegistrar.register(this, SENDER_ID);

		} else {
			try {
				SenseApi.registerGCMId(this, registrationId);
			} catch (IOException e) {
				Log.d(TAG, "error while registering gcm registration_id");
				e.printStackTrace();
			} catch (JSONException e) {
				Log.d(TAG, "error while parsing json on gcm registration_id");
				e.printStackTrace();
			} catch (Exception e) {
				Log.d(TAG, "error while trying to send gcm registration_id");
			}
		}
	}
}
