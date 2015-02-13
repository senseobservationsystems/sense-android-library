/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.states;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

/**
 * Helper class to build a state sensor that periodically analyzes data from certain sensors on the
 * phone. Implementing classes should put the analysis in {@link #updateState()}.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public abstract class AbstractStateMonitor extends Service {

	private PendingIntent operation;
	private final int REQCODE = 0x057A7E;

	/**
	 * Receiver for update alarms.
	 */
	private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO use intent service to do the work
			updateState();
		}
	};

	@Override
	public IBinder onBind(Intent arg0) {
		// binding not possible
		return null;
	}

	protected abstract void updateState();

	protected void startMonitoring(String action, long interval) {

		// register receiver
		registerReceiver(updateReceiver, new IntentFilter(action));

		// start alarms
		Intent alarm = new Intent(action);
		operation = PendingIntent.getBroadcast(this, REQCODE, alarm, 0);
		AlarmManager mgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		mgr.cancel(operation);
		mgr.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, operation);
	}

	protected void stopMonitoring() {

		// unregister receiver
		try {
			unregisterReceiver(updateReceiver);
		} catch (IllegalArgumentException e) {
			// do nothing
		}

		// stop update alarms
		AlarmManager mgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		mgr.cancel(operation);
	}
}
