package nl.sense_os.service.push;

import java.io.IOException;

import nl.sense_os.service.R;
import nl.sense_os.service.ServiceStateHelper;
import nl.sense_os.service.commonsense.SenseApi;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Auth;
import nl.sense_os.service.constants.SensePrefs.Status;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class GCMReceiver extends GCMBaseIntentService {

	private final String TAG = "push";
	public static final String SENDER_ID = "926669645310";
	public static final String KEY_GCM_ID = "gcm_id";
	public static final String TYPE_TOAST = "toast";
	public static final String TYPE_NOTIFICATION = "notification";
	public static final String TYPE_SERVICE = "service";
	public static final String TYPE_UPDATE_CONFIGURATION = "update_configuration";
	public static final String TYPE_USE_CONFIGURATION = "use_configuration";
	private static final int NOTIF_ID = 0x01abc;
	
	public final static String ACTION_GOT_CONFIGURATION = "nl.sense_os.service.push.got_requirement";
	
	
	public GCMReceiver() {
	super(SENDER_ID);
	
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
	Bundle extras = intent.getExtras();
	String recipient = extras.getString("username");

	SharedPreferences authPrefs = getSharedPreferences(SensePrefs.AUTH_PREFS,
			MODE_PRIVATE);
	String username = authPrefs.getString(Auth.LOGIN_USERNAME, null);

	// check if I am the intended recipient or else ignore the message
	if (recipient.equals(username)) {
		String type = extras.getString("type");
		String content = extras.getString("content");

		// switch using
		if (type.equals(TYPE_TOAST)) {
			Toast.makeText(this, content, Toast.LENGTH_LONG).show();
		} else if (type.equals(TYPE_NOTIFICATION)) {
			this.showNoticitacion(context, content);
		} else if (type.equals(TYPE_SERVICE)) {
			this.toggleService(context, content);
		} else if (type.equals(TYPE_UPDATE_CONFIGURATION)) {
			this.updateConfiguration(context);
		} else if (type.equals(TYPE_USE_CONFIGURATION)) {
			this.useConfiguration(context, content);
		}
	}

	}

	@Override
	public void onError(Context context, String errorId) {
	// TODO Handle if out of sync, and other type errors ?

	}

	@Override
	public void onRegistered(Context context, String registrationId) {
	Log.v(TAG, "RegisterGCMIntentService called with registration: " 
			+ registrationId);
	
	try {		
		Log.v(TAG, "Registering gcm id to commonSense server");
		SenseApi.registerGCMId(this, registrationId);				
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

	@Override
	protected void onUnregistered(Context context, String registrationId) {
	Log.i(TAG, "Device unregistered");
	if (GCMRegistrar.isRegisteredOnServer(context)) {
		// ServerUtilities.unregister(context, registrationId);
	} else {
		// This callback results from the call to unregister made on
		// ServerUtilities when the registration to the server failed.
		Log.i(TAG, "Ignoring unregister callback");
	}
	}

	/**
	 * Show notification to user
	 * 
	 * @param context
	 *            application context
	 * @param jsonContent
	 *            String representation of JSon object consist of {title, text}
	 */
	private void showNoticitacion(Context context, String jsonContent) {
	JSONObject content_json = null;
	String title = "";
	String text = "";

	try {
		content_json = new JSONObject(jsonContent);
		title = content_json.getString("title");
		text = content_json.getString("content");

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setDefaults(Notification.DEFAULT_SOUND)
				.setSmallIcon(R.drawable.ic_stat_notify_sense)
				.setWhen(System.currentTimeMillis())
				.setContentTitle(title)
				.setContentText(text)
				.setTicker(text)
				.setContentIntent(
						PendingIntent.getService(this, 0, new Intent(), 0));

		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nm.notify(NOTIF_ID, builder.getNotification());
	} catch (JSONException e) {
		Log.d(TAG, "Error parsing notification json");
		e.printStackTrace();
	}

	}

	/**
	 * Show notification to user
	 * 
	 * @param context
	 *            application context
	 * @param jsonContent
	 *            String representation of JSon object consist of {title, text}
	 */
	private void toggleService(Context context, String toggle) {
	boolean started = ServiceStateHelper.getInstance(context).isStarted();

	if (toggle.equals("1") && !started) {
		Editor editor = getSharedPreferences(SensePrefs.STATUS_PREFS,
				MODE_PRIVATE).edit();
		editor.putBoolean(Status.MAIN, true);
		editor.commit();

		Intent task = new Intent(
				context.getString(R.string.action_sense_service));
		context.startService(task);
	} else if (toggle.equals("0") && started) {
		Editor editor = getSharedPreferences(SensePrefs.STATUS_PREFS,
				MODE_PRIVATE).edit();
		editor.putBoolean(Status.MAIN, false);
		editor.commit();

		Intent task = new Intent(
				context.getString(R.string.action_sense_service));
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
		message.put("content", "Got Configuration update from commonsense");
		showNoticitacion(context, message.toString());

		//TODO: send Broadcast message of this new configuration
		Log.v(TAG, configuration.toString());
	} catch (IOException e) {
		e.printStackTrace();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}
	
	/**
	 * Call commonSense to get the specific configuration
	 */
	private void useConfiguration(Context context, String configurationId) {
	try {		
		String configuration = SenseApi.getDeviceConfiguration(this, configurationId);

		JSONObject configObj = new JSONObject(configuration);
		JSONArray requirementsArr = configObj.getJSONArray("requirements");
		
		JSONObject requirements = new JSONObject();
		if (requirementsArr.length() > 0) {
			JSONObject value = new JSONObject(requirementsArr.getJSONObject(0).getString("value"));
			requirements.put("requirements", value);
		}

		String requrimentStr = requirements.toString();
		Log.v(TAG, requrimentStr);
		boardcastRequirement(context, requrimentStr);
	} catch (IOException e) {
		e.printStackTrace();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}
	
	/**
	 * Boardcast new Requirement
	 */
	private void boardcastRequirement(Context context, String req) {
	Intent i = new Intent(ACTION_GOT_CONFIGURATION);
	i.putExtra("requirements", req);
	sendBroadcast(i);
	}
}
