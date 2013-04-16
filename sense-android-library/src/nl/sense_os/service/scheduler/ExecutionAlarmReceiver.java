package nl.sense_os.service.scheduler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 
 * @author Kimon Tsitsikas <kimon@sense-os.nl>
 */
public class ExecutionAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "ExecutionAlarmReceiver";
    private static Runnable sumTask;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "Received alarm to execute");

        if (sumTask != null) {
            sumTask.run();
        }

        ScheduleAlarmTool.getInstance(context).schedule();
    }

    public static void setSumTask(final Runnable task) {
        sumTask = task;
    }
}
