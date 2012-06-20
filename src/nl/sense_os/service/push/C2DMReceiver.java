package nl.sense_os.service.push;

import nl.sense_os.service.constants.SensePrefs;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;

import android.util.Log;

public class C2DMReceiver extends BroadcastReceiver {
	private final String TAG = "SenseApi";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.v("c2dm", "A broadcast has been received for sense!");
		
		if (intent.getAction().equals("com.google.android.c2dm.intent.REGISTRATION")) {
	        handleRegistration(context, intent);
	    } else if (intent.getAction().equals("com.google.android.c2dm.intent.RECEIVE")) {
	        handleMessage(context, intent);
	    }
	}	
	
	private void handleRegistration(Context context, Intent intent) {
	    String registration = intent.getStringExtra("registration_id");
	    if (intent.getStringExtra("error") != null) {
	        // Registration failed, should try again later.
		    Log.d(TAG, "registration failed");
		    String error = intent.getStringExtra("error");
		    if(error == "SERVICE_NOT_AVAILABLE"){
		    	Log.d(TAG, "SERVICE_NOT_AVAILABLE");
		    }else if(error == "ACCOUNT_MISSING"){
		    	Log.d(TAG, "ACCOUNT_MISSING");
		    }else if(error == "AUTHENTICATION_FAILED"){
		    	Log.d(TAG, "AUTHENTICATION_FAILED");
		    }else if(error == "TOO_MANY_REGISTRATIONS"){
		    	Log.d(TAG, "TOO_MANY_REGISTRATIONS");
		    }else if(error == "INVALID_SENDER"){
		    	Log.d(TAG, "INVALID_SENDER");
		    }else if(error == "PHONE_REGISTRATION_ERROR"){
		    	Log.d(TAG, "PHONE_REGISTRATION_ERROR");
		    }
	    } else if (intent.getStringExtra("unregistered") != null) {
	        // unregistration done, new messages from the authorized sender will be rejected
	    	Log.d(TAG, "unregistered");

	    } else if (registration != null) {
	    	Log.d(TAG, registration);
	    	Editor editor =
                context.getSharedPreferences(SensePrefs.AUTH_PREFS, Context.MODE_PRIVATE).edit();

    		editor.commit();
    		
    		// Send the registration ID to the 3rd party site that is sending the messages.
    		// This should be done in a separate thread.
    		// When done, remember that all registration is done.
    		Intent intentRegistration = new Intent(context, C2DMRegistrationIntentService.class);
    		intentRegistration.putExtra("registration_id", registration);
    		context.startService(intentRegistration);
	    }
	}

	private void handleMessage(Context context, Intent intent)
	{
		Log.d(TAG, "Got message");
	}
}
