/*
 ************************************************************************************************************
 *     Copyright (C)  2010 Sense Observation Systems, Rotterdam, the Netherlands.  All rights reserved.     *
 ************************************************************************************************************
 */
package nl.sense_os.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class AliveChecker extends BroadcastReceiver {

    private static final String TAG = "Sense AliveChecker";
    public static final String ACTION_CHECK_ALIVE = "nl.sense_os.service.CheckAlive";
    public static final int REQ_CHECK_ALIVE = 1;
    public static final long PERIOD_CHECK_ALIVE = 1000 * 60 * 1;

    @Override
    public void onReceive(Context context, Intent intent) {

        /* set the next check broadcast */
        final Intent alarmIntent = new Intent(AliveChecker.ACTION_CHECK_ALIVE);
        final PendingIntent alarmOp = PendingIntent.getBroadcast(context,
                AliveChecker.REQ_CHECK_ALIVE, alarmIntent, 0);
        final long alarmTime = System.currentTimeMillis() + AliveChecker.PERIOD_CHECK_ALIVE;
        final AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC_WAKEUP, alarmTime, alarmOp);

        /* check if the Sense service should be alive */
        final SharedPreferences statusPrefs = context.getSharedPreferences(Constants.STATUS_PREFS,
                Context.MODE_WORLD_WRITEABLE);
        final boolean alive = statusPrefs.getBoolean(Constants.PREF_ALIVE, false);

        /* if it should be alive, check if it really is still alive */
        if (true == alive) {
            final Intent serviceIntent = new Intent(ISenseService.class.getName());
            if (null == context.startService(serviceIntent)) {
                Log.w(TAG, "Could not start Sense service!");
            }
        } else {
            // Log.d(TAG, "Sense service should NOT be alive. Doing nothing...");
        }
    }
}
