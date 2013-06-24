package nl.sense_os.service.scheduler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This class is responsible for executing the batched tasks when the alarm for deterministic
 * execution is received.
 * 
 * @author Kimon Tsitsikas <kimon@sense-os.nl>
 */
public class ExecutionAlarmReceiver extends BroadcastReceiver {

    public static final String EXTRA_EXECUTION_TYPE = "nl.sense_os.service.EXECUTION_TYPE";
    public static final int DETERMINISTIC_TYPE = 1;
    public static final int OPPORTUNISTIC_TYPE = 2;
    private static final String TAG = "ExecutionAlarmReceiver";

    private static Runnable sBatchTask;

    @Override
    public void onReceive(Context context, Intent intent) {

        int type = intent.getIntExtra(EXTRA_EXECUTION_TYPE, -1);
        switch (type) {
        case DETERMINISTIC_TYPE:
            // do nothing but wake up
            break;

        case OPPORTUNISTIC_TYPE:

            // run the batch of tasks
            if (sBatchTask != null) {
                sBatchTask.run();
                sBatchTask = null;
            }

            // cancel the deterministic execution
            ScheduleAlarmTool scheduleTool = ScheduleAlarmTool.getInstance(context);
            scheduleTool.cancelDeterministicAlarm();

            // do the next schedule
            scheduleTool.schedule();

            break;

        default:
            Log.w(TAG, "Unexpected execution type: " + type);
        }
    }

    /**
     * @param task
     *            The batched task to set
     */
    public static void setBatchTask(Runnable task) {
        sBatchTask = task;
    }
}
