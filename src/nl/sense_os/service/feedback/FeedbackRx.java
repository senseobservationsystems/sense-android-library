/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.feedback;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import nl.sense_os.service.R;

public class FeedbackRx extends BroadcastReceiver {

    private static final String TAG = "Sense Feedback BroadcastReceiver";
    public static final int REQ_CHECK_FEEDBACK = 2;

    @Override
    public void onReceive(Context context, Intent intent) {

        int periodCheckSensor = intent.getIntExtra("period", (1000 * 60 * 2));
        String sensorName = intent.getStringExtra("sensor_name");
        String actionAfterCheck = intent.getStringExtra("broadcast_after");

        /* set the next check broadcast */
        final Intent alarmIntent = new Intent(
                context.getString(R.string.action_sense_feedback_check_alarm));
        alarmIntent.putExtra("period", periodCheckSensor);
        alarmIntent.putExtra("sensor_name", sensorName);
        alarmIntent.putExtra("broadcast_after", actionAfterCheck);

        final PendingIntent alarmOp = PendingIntent.getBroadcast(context, REQ_CHECK_FEEDBACK,
                alarmIntent, 0);
        final long alarmTime = System.currentTimeMillis() + periodCheckSensor;
        final AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (periodCheckSensor != 0) {

            mgr.cancel(alarmOp);
            mgr.set(AlarmManager.RTC_WAKEUP, alarmTime, alarmOp);

            /* start the feedback check task */
            Intent checkFeedback = new Intent(
                    context.getString(R.string.action_sense_feedback_check));
            checkFeedback.putExtra("sensor_name", sensorName);
            checkFeedback.putExtra("broadcast_after", actionAfterCheck);

            ComponentName component = context.startService(checkFeedback);

            if (null == component) {
                Log.w(TAG, "Could not start feedback checker");
            }
        } else {
            mgr.cancel(alarmOp);
        }

    }
}
