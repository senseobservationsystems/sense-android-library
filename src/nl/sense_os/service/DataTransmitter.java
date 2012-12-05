/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service;

import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main;
import android.content.ComponentName;
//import nl.sense_os.service.energy_controller.Controller;
//import nl.sense_os.service.standard_controller.Controller;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class DataTransmitter extends BroadcastReceiver {

	private static final String TAG = "Sense DataTransmitter";
	public static final int REQ_CODE = 0x05E2DDA7A;	
	//private static long interval = 0;	

	@Override
	public void onReceive(Context context, Intent intent) {

		// check if the service is (supposed to be) alive before scheduling next alarm
		if (true == ServiceStateHelper.getInstance(context).isLoggedIn()) {
			// start send task
			Log.i(TAG, "Start transmission");
			Intent task = new Intent(context.getString(R.string.action_sense_send_data));
			ComponentName service = context.startService(task);
			if (null == service) {
				Log.w(TAG, "Failed to start data sync service");
			}
		} else {
			// skip transmission: Sense service is not logged in
		}
	}

	/**
	 * Stops the periodic transmission of sensor data.
	 * 
	 * @param context
	 *            Context to access AlarmManager
	 */
	public static void stopTransmissions(Context context) {
		Intent intent = new Intent(context.getString(R.string.action_sense_data_transmit_alarm));
		PendingIntent operation = PendingIntent.getBroadcast(context, REQ_CODE, intent, 0);
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.cancel(operation);
	}
}
