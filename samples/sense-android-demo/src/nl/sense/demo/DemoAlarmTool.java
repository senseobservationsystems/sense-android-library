package nl.sense.demo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

public class DemoAlarmTool {

    private static final String TAG = "FinishAlarmTool";
    private static final int REQ_CODE = 0xf00c0de;

    /**
     * Cancels the alarm to finish the test
     * 
     * @param context
     *            Context to access AlarmManager
     */
    public static void cancel(Context context) {
        Log.v(TAG, "Cancel test finish alarm");

        // re-create the operation that would go off
        Intent intent = new Intent(context, DemoAlarmTool.class);
        PendingIntent operation = PendingIntent.getBroadcast(context, REQ_CODE, intent, 0);

        // cancel the alarm
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.cancel(operation);
    }

    public static void schedule(Context context, long duration) {
        Log.v(TAG, "Schedule test finish in " + duration + "ms");
        // prepare the operation to go off
        Intent intent = new Intent(context, DemoAlarmReceiver.class);
        PendingIntent operation = PendingIntent.getBroadcast(context, REQ_CODE, intent, 0);
        // set the alarm
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.cancel(operation);
        mgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + duration,
                operation);
    }

    /**
     * Private constructor to prevent instantiation
     */
    private DemoAlarmTool() {
        // do not instantiate
    }
}
