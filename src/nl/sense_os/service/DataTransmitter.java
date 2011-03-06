/*
 * ***********************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 * **
 * ************************************************************************************************
 * *********
 */
package nl.sense_os.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class DataTransmitter extends BroadcastReceiver {

    private static final String TAG = "Sense DataTransmitter";
    public static final int REQID = 26;

    @Override
    public void onReceive(Context context, Intent intent) {

        final SharedPreferences statusPrefs = context.getSharedPreferences(Constants.STATUS_PREFS,
                Context.MODE_WORLD_WRITEABLE);
        final boolean alive = statusPrefs.getBoolean(Constants.PREF_ALIVE, false);

        // check if the service is (supposed to be) alive before scheduling next alarm
        if (true == alive) {

            // determine sync rate to schedule next alarm
            final SharedPreferences mainPrefs = context.getSharedPreferences(Constants.MAIN_PREFS,
                    Context.MODE_WORLD_WRITEABLE);
            final int rate = Integer.parseInt(mainPrefs.getString(Constants.PREF_SYNC_RATE, "0"));
            long nextAlarm = 0;
            switch (rate) {
            case -2: // real-time: clear out the buffer once, reset alarm in 1 hour "just in case"
                nextAlarm = 1000L * 60 * 60;
                return;
            case -1: // 5 seconds
                nextAlarm = 1000L * 5;
                break;
            case 0: // 1 minute
                nextAlarm = 1000L * 60;
                break;
            case 1: // 1 hour
                nextAlarm = 1000L * 60 * 60;
                break;
            default:
                Log.e(TAG, "Unexpected sync rate value: " + rate);
                return;
            }

            // set next alarm
            Intent alarm = new Intent(context, DataTransmitter.class);
            PendingIntent operation = PendingIntent.getBroadcast(context, 1, alarm, 0);
            AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + nextAlarm, operation);
        }

        // start send task
        Intent task = new Intent(MsgHandler.ACTION_SEND_DATA);
        context.startService(task);
    }
}
