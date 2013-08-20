/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service;

import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Status;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

/**
 * This class is responsible for checking if the {@link SenseService} is still alive when it should
 * be. It works by setting periodic alarm broadcasts that are received by this class. The Sense
 * service calls {@link #scheduleChecks(Context)} when it starts sensing.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class AliveChecker extends BroadcastReceiver {

    private static final String TAG = "AliveChecker";
    private static final int REQ_CODE_NORMAL = 0x0C471FE1;
    private static final int REQ_CODE_WAKEUP = 0x0C471FE2;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "Received broadcast");

        // check if the Sense service should be alive
        SharedPreferences statusPrefs = context.getSharedPreferences(SensePrefs.STATUS_PREFS,
                Context.MODE_PRIVATE);
        boolean alive = statusPrefs.getBoolean(Status.MAIN, false);

        // if it should be alive, check if it really is still alive
        if (true == alive) {
            Log.v(TAG, "Sense should be alive: poke it");
            final Intent serviceIntent = new Intent(
                    context.getString(R.string.action_sense_service));
            if (null == context.startService(serviceIntent)) {
                Log.w(TAG, "Could not start Sense service!");
            }
        } else {
            // Sense service should NOT be alive: do nothing
        }
    }

    /**
     * Starts periodic checks on Sense Platform service's alive status.
     * 
     * @param context
     *            Context to access AlarmManager
     */
    public static void scheduleChecks(Context context) {

        Intent intent = new Intent(context.getString(R.string.action_sense_alive_check_alarm));
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // try to check pretty often when the phone is awake

        PendingIntent operation = PendingIntent.getBroadcast(context, REQ_CODE_NORMAL, intent, 0);
        long interval = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
        long triggerAt = SystemClock.elapsedRealtime() + interval;
        mgr.cancel(operation);
        mgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, triggerAt, interval, operation);

        // make sure we wake up at least once an hour
        operation = PendingIntent.getBroadcast(context, REQ_CODE_WAKEUP, intent, 0);
        interval = AlarmManager.INTERVAL_HOUR;
        triggerAt = SystemClock.elapsedRealtime() + interval;
        mgr.cancel(operation);
        mgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, interval,
                operation);
    }

    /**
     * Stops the periodic checks on Sense Platform service's alive status.
     * 
     * @param context
     *            Context to access AlarmManager
     */
    public static void stopChecks(Context context) {
        Intent intent = new Intent(context.getString(R.string.action_sense_alive_check_alarm));
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.cancel(PendingIntent.getBroadcast(context, REQ_CODE_NORMAL, intent, 0));
        mgr.cancel(PendingIntent.getBroadcast(context, REQ_CODE_WAKEUP, intent, 0));
    }
}
