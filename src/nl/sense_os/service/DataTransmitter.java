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
import android.preference.PreferenceManager;
import android.util.Log;

import nl.sense_os.app.SenseSettings;

public class DataTransmitter extends BroadcastReceiver {

    private static final String TAG = "Sense DataTransmitter";
    public static final int REQID = 26;

    @Override
    public void onReceive(Context context, Intent intent) {

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        // check if the service is (supposed to be) alive before scheduling next alarm
        if (prefs.getBoolean(SenseSettings.PREF_ALIVE, false)) {
            
            // determine sync rate to schedule next alarm
            long nextAlarm = 0;
            switch (Integer.parseInt(prefs.getString(SenseSettings.PREF_SYNC_RATE, "0"))) {
            case -2: // real-time: only clear out the buffer once, reset alarm in 1 hour
                     // "just in case"
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
                final String val = prefs.getString(SenseSettings.PREF_SYNC_RATE, "0");
                Log.e(TAG, "Unexpected sync rate value: " + val);
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
