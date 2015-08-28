package nl.sense_os.service.commonsense;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;

import org.json.JSONException;

import java.io.IOException;

import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.provider.SNTP;

import static nl.sense_os.service.push.GCMReceiver.SENDER_ID;

/**
 * Service that checks if all the sensors on this phone are registered at CommonSense.<br/>
 * <br/>
 * TODO: Also registers the app for GCM notifications, but this should probably be moved somewhere
 * else to keep the code transparent.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 * 
 * @see DefaultSensorRegistrator
 */
public class DefaultSensorRegistrationService extends IntentService {

	private static final String TAG = "DefaultSensorRegistrationService";
    private static final boolean USE_GCM = false;
	private SensorRegistrator verifier = new DefaultSensorRegistrator(this);

	public DefaultSensorRegistrationService() {
		super("DefaultSensorRegistrationService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		SharedPreferences mainPrefs = getSharedPreferences(SensePrefs.MAIN_PREFS, MODE_PRIVATE);
		long lastVerified = mainPrefs.getLong(SensePrefs.Main.LAST_VERIFIED_SENSORS, 0);
		if (SNTP.getInstance().getTime() - lastVerified < 1000l * 60) { // 1 minute
			// registered sensors were already recently checked
			Log.v(TAG, "Sensor IDs were just verified already");
			return;
		}

		String deviceType = SenseApi.getDefaultDeviceType(this);
		String deviceUuid = SenseApi.getDefaultDeviceUuid(this);

        if (USE_GCM) {
            registerGCM();
        } else {
            // TODO: we have disabled the GCM: it conflicts with other GCM registrations in the app
            Log.w(TAG, "CommonSense GCM registration is disabled!");
        }

		if (verifier.verifySensorIds(deviceType, deviceUuid)) {
			Log.v(TAG, "Sensor IDs verified");
			mainPrefs.edit()
					.putLong(SensePrefs.Main.LAST_VERIFIED_SENSORS, SNTP.getInstance().getTime())
					.commit();
		} else {
			// hopefully the IDs will be checked again
		}
	}

	@Override
	public void onDestroy() {
        if (USE_GCM) {
            GCMRegistrar.onDestroy(this); // so that it will not leak
        } else {
            // do not unregister is it was not registered
        }
		super.onDestroy();
	}

    /**
     * Register the device to use Google GCM
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
