package nl.sense_os.service.feedback;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class FeedbackRx extends BroadcastReceiver {

    private static final String TAG = "Sense Feedback BroadcastReceiver";
    public static final String ACTION_CHECK_FEEDBACK = "nl.sense_os.service.CheckFeedback";
    public static final int REQ_CHECK_FEEDBACK = 2;
    public static final long PERIOD_CHECK_FEEDBACK = 1000 * 30 * 1;

    @Override
    public void onReceive(Context context, Intent intent) {

        /* set the next check broadcast */
        final Intent alarmIntent = new Intent(ACTION_CHECK_FEEDBACK);
        final PendingIntent alarmOp = PendingIntent.getBroadcast(context, REQ_CHECK_FEEDBACK,
                alarmIntent, 0);
        final long alarmTime = System.currentTimeMillis() + PERIOD_CHECK_FEEDBACK;
        final AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.cancel(alarmOp);
        mgr.set(AlarmManager.RTC_WAKEUP, alarmTime, alarmOp);

        /* start the feedback check task */
        ComponentName component = context.startService(new Intent(
                FeedbackChecker.ACTION_CHECK_FEEDBACK));
        if (null == component) {
            Log.w(TAG, "Could not start feedback checker");
        }
    }
}
