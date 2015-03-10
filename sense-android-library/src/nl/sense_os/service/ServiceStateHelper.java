/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service;

import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Auth;
import nl.sense_os.service.constants.SensePrefs.Main.Advanced;
import nl.sense_os.service.constants.SenseStatusCodes;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;

/**
 * This class is responsible for keeping track of the Sense service state and updating the status
 * bar notification when the service is started or changes login status. We use the singleton
 * pattern to make sure the entire application can see the same state object.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class ServiceStateHelper {

	private static ServiceStateHelper instance = null;

	/**
	 * ID for the notification in the status bar. Used to cancel the notification.
	 */
	public static final int NOTIF_ID = 1;

	@SuppressWarnings("unused")
	private static final String TAG = "Sense Service State";

	/**
	 * @param context
	 *            Context for lazy creating the ServiceStateHelper. Used to create notifications.
	 * @return Singleton instance of the ServiceStateHelper
	 */
	public static ServiceStateHelper getInstance(Context context) {
		if (null != instance) {
			return instance;
		} else {
			return instance = new ServiceStateHelper(context);
		}
	}

	private final Context context;

	private boolean started, foreground, loggedIn, ambienceActive, devProxActive, externalActive,
			locationActive, motionActive, phoneStateActive, quizActive;

	/**
	 * Private constructor to enforce singleton pattern.
	 * 
	 * @param context
	 * @see ServiceStateHelper#getInstance(Context)
	 */
	private ServiceStateHelper(Context context) {
		this.context = context;
	}

	public Notification getStateNotification() {

		NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

		// icon and content text depend on the current state
		int icon = -1;
		int contentText = -1;
		if (isStarted()) {
			if (isLoggedIn()) {
				icon = R.drawable.ic_stat_sense;
				contentText = R.string.stat_notify_content_on_loggedin;
			} else {
                icon = R.drawable.ic_stat_sense_warning;
				contentText = R.string.stat_notify_content_on_loggedout;
			}
		} else {
			if (isLoggedIn()) {
                icon = R.drawable.ic_stat_sense_error;
				contentText = R.string.stat_notify_content_off_loggedin;
			} else {
                icon = R.drawable.ic_stat_sense_error;
				contentText = R.string.stat_notify_content_off_loggedout;
			}
		}
		builder.setSmallIcon(icon);

		// username will be substituted into the content text
		final SharedPreferences authPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS,
				Context.MODE_PRIVATE);
		String username = authPrefs.getString(Auth.LOGIN_USERNAME,
				context.getString(android.R.string.unknownName));

		boolean encrypt_credential = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE)
                                                 .getBoolean(Advanced.ENCRYPT_CREDENTIAL, false);

		if (encrypt_credential && username != context.getString(android.R.string.unknownName)) {
                        EncryptionHelper encryptor = new EncryptionHelper(context);
                        try {
                            username = encryptor.decrypt(username);
                        } catch (EncryptionHelper.EncryptionHelperException e) {
                            // Log.w(TAG, "Error decrypting username. Assume data is not encrypted");
                        }
		}

		builder.setContentText(context.getString(contentText, username));
		builder.setContentTitle(context.getString(R.string.stat_notify_title));

		// action to take when the notification is tapped
		final Intent notifIntent = new Intent(context.getString(R.string.stat_notify_action));
		notifIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		final PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notifIntent, 0);
		builder.setContentIntent(contentIntent);

		// time of the notification
		builder.setWhen(System.currentTimeMillis());
		builder.setOngoing(true);

		return builder.build();
	}

	/**
	 * @return the current status of the sensing modules
	 */
	public int getStatusCode() {
		int status = 0;
		status = isStarted() ? SenseStatusCodes.RUNNING : status;
		status = isLoggedIn() ? status + SenseStatusCodes.CONNECTED : status;
		status = isPhoneStateActive() ? status + SenseStatusCodes.PHONESTATE : status;
		status = isLocationActive() ? status + SenseStatusCodes.LOCATION : status;
		status = isAmbienceActive() ? status + SenseStatusCodes.AMBIENCE : status;
		status = isQuizActive() ? status + SenseStatusCodes.QUIZ : status;
		status = isDevProxActive() ? status + SenseStatusCodes.DEVICE_PROX : status;
		status = isExternalActive() ? status + SenseStatusCodes.EXTERNAL : status;
		status = isMotionActive() ? status + SenseStatusCodes.MOTION : status;
		return status;
	}

	public boolean isAmbienceActive() {
		return ambienceActive;
	}

	public boolean isDevProxActive() {
		return devProxActive;
	}

	public boolean isExternalActive() {
		return externalActive;
	}

	public boolean isForeground() {
		return foreground;
	}

	public boolean isLocationActive() {
		return locationActive;
	}

	public boolean isLoggedIn() {
		return loggedIn;
	}

	public boolean isMotionActive() {
		return motionActive;
	}

	public boolean isPhoneStateActive() {
		return phoneStateActive;
	}

	public boolean isQuizActive() {
		return quizActive;
	}

	public boolean isStarted() {
		return started;
	}

	public void setAmbienceActive(boolean active) {
	    ambienceActive = active;
	    Intent i = new Intent(context.getString(R.string.action_widget_update));
	    i.setPackage(context.getPackageName());
	    context.startService(i);
	}

	public void setDevProxActive(boolean active) {
		devProxActive = active;
        Intent i = new Intent(context.getString(R.string.action_widget_update));
        i.setPackage(context.getPackageName());
        context.startService(i);
	}

	public void setExternalActive(boolean active) {
		externalActive = active;
	}

	public void setForeground(boolean foreground) {
		if (foreground != isForeground()) {
			this.foreground = foreground;
			// Log.v(TAG, isForeground()
			// ? "Sense Platform Service is in foreground..."
			// : "Sense Platform Service is in background...");
			updateNotification();
		}
        Intent i = new Intent(context.getString(R.string.action_widget_update));
        i.setPackage(context.getPackageName());
        context.startService(i);
	}

	public void setLocationActive(boolean active) {
		locationActive = active;
        Intent i = new Intent(context.getString(R.string.action_widget_update));
        i.setPackage(context.getPackageName());
        context.startService(i);
	}

	public void setLoggedIn(boolean loggedIn) {
		if (loggedIn != isLoggedIn()) {
			this.loggedIn = loggedIn;
			// Log.v(TAG, isLoggedIn() ? "Sense Platform Service logged in..."
			// : "Sense Platform Service logged out...");
			updateNotification();
		}
        Intent i = new Intent(context.getString(R.string.action_widget_update));
        i.setPackage(context.getPackageName());
        context.startService(i);
	}

	public void setMotionActive(boolean active) {
		motionActive = active;
        Intent i = new Intent(context.getString(R.string.action_widget_update));
        i.setPackage(context.getPackageName());
        context.startService(i);
	}

	public void setPhoneStateActive(boolean active) {
		phoneStateActive = active;
        Intent i = new Intent(context.getString(R.string.action_widget_update));
        i.setPackage(context.getPackageName());
        context.startService(i);
	}

	public void setQuizActive(boolean active) {
		quizActive = active;
	}

	public void setStarted(boolean started) {
		if (started != isStarted()) {
			this.started = started;
			// Log.v(TAG, isStarted()
			// ? "Sense Platform Service started..."
			// : "Sense Platform Service stopped...");
			updateNotification();
		}
        Intent i = new Intent(context.getString(R.string.action_widget_update));
        i.setPackage(context.getPackageName());
        context.startService(i);
	}

	/**
	 * Shows a status bar notification that the Sense service is active, also displaying the
	 * username if the service is logged in.
	 * 
	 * @param loggedIn
	 *            set to <code>true</code> if the service is logged in.
	 */
	private void updateNotification() {
		NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		if (isForeground()) {
			nm.notify(NOTIF_ID, getStateNotification());
		} else {
			nm.cancel(NOTIF_ID);
		}
	}
}
