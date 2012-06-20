package nl.sense_os.service.push;

import java.io.IOException;

import nl.sense_os.service.R;
import nl.sense_os.service.commonsense.SenseApi;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Auth;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.c2dm.C2DMBaseReceiver;
import com.google.android.c2dm.C2DMessaging;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class C2DMReceiver extends C2DMBaseReceiver {
	private final String TAG = "c2dm";
	public static final String EXTRA_REGISTRATION_ID = "registration_id";
	public static final String KEY_C2DM_ID = "c2dm_id";
	private static final int NOTIF_ID = 0x01abc;
	
	public C2DMReceiver() {
	super("ahmy@sense-os.nl");
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
	Bundle extras = intent.getExtras();
	String recipient = extras.getString("username");
	
	SharedPreferences authPrefs = getSharedPreferences(SensePrefs.AUTH_PREFS, MODE_PRIVATE);
	String username = authPrefs.getString(Auth.LOGIN_USERNAME, null);
	
	//check if I am the intended recipient or else ignore the message
	if (recipient.equals(username)) {
		String type = extras.getString("type");
		String content = extras.getString("content");
		
		if (type.equals("toast")) {
			Toast.makeText(this, content, Toast.LENGTH_LONG).show();
		} else if (type.equals("notification")) {
			JSONObject content_json = null;
			String title = "";			
			String text = "";
			
			try {
				content_json = new JSONObject(content);
				title = content_json.getString("title");
				text = content_json.getString("text");
			} catch (JSONException e) {
				Log.d(TAG, "Error parsing notification json");
				e.printStackTrace();
			}
			
			
			NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
			builder.setDefaults(Notification.DEFAULT_SOUND)
				.setSmallIcon(R.drawable.ic_stat_notify_sense)
				.setWhen(System.currentTimeMillis())
				.setContentTitle(title)
				.setContentText(text)
				.setTicker(text)
				.setContentIntent(PendingIntent.getService(this, 0, new Intent(), 0));	
			

			NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			nm.notify(NOTIF_ID, builder.getNotification());	
		}
	}
	
	}

	@Override
	public void onError(Context context, String errorId) {
	// TODO Auto-generated method stub
	
	}

	@Override
	public void onRegistration(Context context, String registrationId) {
	Log.v(TAG, "RegisterC2DMIntentService called with registration: " + registrationId);
	
	try {
		Log.v(TAG, "Registering c2dm id to commonSense server");
		SenseApi.registerC2DMId(this, registrationId);
		C2DMessaging.setRegistrationId(this, registrationId);
		Log.v(TAG, "Successfully registerd c2dm id to common sense");
	} catch (IOException e) {
		Log.e(TAG, "Sending registration id failed");
		e.printStackTrace();
	} catch (JSONException e) {
		Log.e(TAG, "Error parsing JSON");
		e.printStackTrace();
	} catch (IllegalStateException e) {
		Log.e(TAG, "Invalid response from common sense");
		e.printStackTrace();
		
	}
	}

}
