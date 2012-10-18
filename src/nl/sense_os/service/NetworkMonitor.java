package nl.sense_os.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

/**
 * This class is responsible for monitoring the connectivity status of the phone, and restarts the
 * Sense service when connectivity is regained after it was lost. It receives the
 * {@link ConnectivityManager#CONNECTIVITY_ACTION} broadcast from the Android system.
 */
public class NetworkMonitor extends BroadcastReceiver {

	private static final String TAG = "Sense Network Monitor";

	@Override
	public void onReceive(final Context context, Intent intent) {
		// Log.v(TAG, "CONNECTIVITY_CHANGE");

		ServiceStateHelper state = ServiceStateHelper.getInstance(context);

		if (!state.isStarted()) {
			// Log.v(TAG, "Connectivity changed, but service is not activated");
			return;
		}

		boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY,
				false);

		if (false == noConnectivity) {

			// check that we are not logged in yet before logging in
			if (false == state.isLoggedIn()) {
				Log.i(TAG, "Regained connectivity! Try to log in");
				context.startService(new Intent(context.getString(R.string.action_sense_service)));

			} else {
				// still connected, stay logged in
				// Log.v(TAG, "Still connected. Remain logged in");
			}

		} else {
			// login not possible without connection
			Log.i(TAG, "Lost connectivity! Update login status");
			state.setLoggedIn(false);
		}
	}
}
