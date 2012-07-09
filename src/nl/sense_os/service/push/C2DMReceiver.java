package nl.sense_os.service.push;

import java.io.IOException;

import nl.sense_os.service.R;
import nl.sense_os.service.ServiceStateHelper;
import nl.sense_os.service.commonsense.SenseApi;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Auth;
import nl.sense_os.service.constants.SensePrefs.Status;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.c2dm.C2DMBaseReceiver;
import com.google.android.c2dm.C2DMessaging;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class C2DMReceiver extends C2DMBaseReceiver {
	private final String TAG = "c2dm";	
	public static final String EXTRA_REGISTRATION_ID = "registration_id";
	public static final String KEY_C2DM_ID = "c2dm_id";
	public static final String TYPE_TOAST = "toast";
	public static final String TYPE_NOTIFICATION = "notification";
	public static final String TYPE_SERVICE = "service";
	public static final String TYPE_UPDATE_CONFIGURATION = "update_configuration";
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
		
		// switch using 
		if (type.equals(C2DMReceiver.TYPE_TOAST)) {
			Toast.makeText(this, content, Toast.LENGTH_LONG).show();
		} else if (type.equals(C2DMReceiver.TYPE_NOTIFICATION)) {
			this.showNoticitacion(context, content);
		} else if (type.equals(C2DMReceiver.TYPE_SERVICE)) {
			this.toggleService(context, content);
		} else if (type.equals(C2DMReceiver.TYPE_UPDATE_CONFIGURATION)) {
			this.updateConfiguration(context);
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
	
	/**
	 * Show notification to user
	 * @param context application context
	 * @param jsonContent String representation of JSon object consist of {title, text}
	 */
	private void showNoticitacion(Context context, String jsonContent) {
	JSONObject content_json = null;
	String title = "";			
	String text = "";
	
	try {
		content_json = new JSONObject(jsonContent);
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

	/**
	 * Show notification to user
	 * @param context application context
	 * @param jsonContent String representation of JSon object consist of {title, text}
	 */
	private void toggleService(Context context, String toggle) {
	boolean started = ServiceStateHelper.getInstance(context).isStarted();	
	
	if (toggle.equals("1") && !started) {
		Editor editor = getSharedPreferences(SensePrefs.STATUS_PREFS, MODE_PRIVATE).edit();
		editor.putBoolean(Status.MAIN, true);
		editor.commit();
	
		Intent task = new Intent(context.getString(R.string.action_sense_service));
		context.startService(task);			
	} else if (toggle.equals("0") && started) {
		Editor editor = getSharedPreferences(SensePrefs.STATUS_PREFS, MODE_PRIVATE).edit();
		editor.putBoolean(Status.MAIN, false);
		editor.commit();
		
		Intent task = new Intent(context.getString(R.string.action_sense_service));
		context.startService(task);
	}		
	}
	
	/**
	 * Call commonSense to get the new configuration
	 */
	private void updateConfiguration(Context context) {
		try {
			String configuration = SenseApi.getDeviceConfiguration(this);
			
			JSONObject message = new JSONObject();
			message.put("title", "CommonSense");
			message.put("text", "Got Configuration update from commonsense");
			showNoticitacion(context, message.toString());
			
			Log.v(TAG, configuration.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
