package nl.sense_os.service.commonsense;

import static nl.sense_os.service.push.GCMReceiver.SENDER_ID;

import java.io.IOException;

import nl.sense_os.service.constants.SensePrefs;

import org.json.JSONException;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
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

		SharedPreferences mainPrefs = getSharedPreferences(SensePrefs.MAIN_PREFS, MODE_PRIVATE);
		long lastVerified = mainPrefs.getLong(SensePrefs.Main.LAST_VERIFIED_SENSORS, 0);
		if (System.currentTimeMillis() - lastVerified < 1000l * 60) { // 1 minute
			// registered sensors were already recently checked
			Log.v(TAG, "Sensor IDs were just verified already");
			return;
		}

		String deviceType = SenseApi.getDefaultDeviceType(this);
		String deviceUuid = SenseApi.getDefaultDeviceUuid(this);

		registerGCM();

		if (verifier.verifySensorIds(deviceType, deviceUuid)) {
			Log.v(TAG, "Sensor IDs verified");
			mainPrefs.edit()
					.putLong(SensePrefs.Main.LAST_VERIFIED_SENSORS, System.currentTimeMillis())
					.commit();
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
	 * Register the device to use Google GCM
	 */
	@SuppressWarnings("unused")
	private void registerGCM() {

		if (true) {
			// FIXME we have disabled the GCM: it conflicts with other GCM registrations in the app
			Log.w(TAG, "CommonSense GCM registration is disabled!");

		} else {
			// get the registration ID
			String registrationId = null;
			try {
				GCMRegistrar.checkDevice(this);
				GCMRegistrar.checkManifest(this);
				registrationId = GCMRegistrar.getRegistrationId(this);
			} catch (IllegalStateException e) {
				Log.w(TAG, "This application does not have the GCM permission");
				return;
			} catch (UnsupportedOperationException e) {
				Log.w(TAG, "This device does not have GCM support");
				return;
			}

			if (registrationId.equals("")) {
				Log.v(TAG, "Device is not registered to Google Cloud Messaging, registering");
				GCMRegistrar.register(this, SENDER_ID);

			} else {
				try {
					SenseApi.registerGCMId(this, registrationId);
				} catch (IOException e) {
					Log.w(TAG, "Failed to register Google Cloud Messaging! " + e);
				} catch (JSONException e) {
					Log.w(TAG, "Failed to register Google Cloud Messaging! " + e);
				} catch (Exception e) {
					Log.w(TAG, "Failed to register Google Cloud Messaging! " + e);
				}
			}
		}
	}
}
