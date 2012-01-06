/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service;

import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main;
import nl.sense_os.service.constants.SensePrefs.Status;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class DataTransmitter extends BroadcastReceiver {

    private static final String TAG = "Sense DataTransmitter";
    private static final int REQ_CODE = 0x05E2DDA7A;

    private class Intervals {
        static final long ECO = AlarmManager.INTERVAL_HALF_HOUR;
        static final long NORMAL = 1000 * 60 * 5;
        static final long OFTEN = 1000 * 60 * 1;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "Received broadcast...");

        SharedPreferences statusPrefs = context.getSharedPreferences(SensePrefs.STATUS_PREFS,
                Context.MODE_PRIVATE);
        final boolean alive = statusPrefs.getBoolean(Status.MAIN, false);

        // check if the service is (supposed to be) alive before scheduling next alarm
        if (true == alive) {
            // start send task
            Intent task = new Intent(context.getString(R.string.action_sense_send_data));
            context.startService(task);
        } else {
            Log.v(TAG, "Sense service should not be alive!");
        }
    }

    /**
     * Starts periodic transmission of the buffered sensor data.
     * 
     * @param context
     *            Context to access AlarmManager and sync rate preferences
     */
    public static void scheduleTransmissions(Context context) {

        Intent intent = new Intent(context.getString(R.string.action_sense_data_transmit_alarm));
        PendingIntent operation = PendingIntent.getBroadcast(context, REQ_CODE, intent, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        SharedPreferences mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
                Context.MODE_PRIVATE);
        int syncRate = Integer.parseInt(mainPrefs.getString(Main.SYNC_RATE, "0"));

        // pick interval
        long interval;
        switch (syncRate) {
        case 1: // eco-mode
            interval = Intervals.ECO;
            break;
        case 0: // 5 minute
            interval = Intervals.NORMAL;
            break;
        case -1: // 60 seconds
            interval = Intervals.OFTEN;
            break;
        case -2: // real-time: schedule transmission based on sample time
            int sampleRate = Integer.parseInt(mainPrefs.getString(Main.SAMPLE_RATE, "0"));
            switch (sampleRate) {
            case 1: // rarely
                interval = Intervals.ECO * 3;
                break;
            case 0: // normal
                interval = Intervals.NORMAL * 3;
                break;
            case -1: // often
                interval = Intervals.OFTEN * 3;
                break;
            case -2: // real time
                interval = Intervals.OFTEN;
                break;
            default:
                Log.e(TAG, "Unexpected sample rate value: " + sampleRate);
                return;
            }
            break;
        default:
            Log.e(TAG, "Unexpected sync rate value: " + syncRate);
            return;
        }
        am.cancel(operation);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval,
                operation);
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
