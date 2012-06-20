package nl.sense_os.service.push;

import java.io.IOException;

import nl.sense_os.service.commonsense.SenseApi;

import org.json.JSONException;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class C2DMRegistrationIntentService extends IntentService {
	private final String TAG = "c2dm";
	
	public C2DMRegistrationIntentService() {		
	super("C2DMRegistrationIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
	String registrationId = intent.getStringExtra("registration_id");
	Log.v(TAG, "RegisterC2DMIntentService called with registration: " + registrationId);
	
	try {
		Log.d(TAG, "Registering c2dm id to commonsense server");
		SenseApi.registerC2DMId(this, registrationId);
	} catch (IOException e) {
		Log.e(TAG, "Sending registration id failed");
		e.printStackTrace();
	} catch (JSONException e) {
		Log.e(TAG, "Error parsing JSON");
		e.printStackTrace();
	}
	}
}