package nl.sense_os.service.scheduler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * This class is responsible for executing the batched tasks in case the CPU is awake within the
 * backwards flexibility margin.
 * 
 * @author Kimon Tsitsikas <kimon@sense-os.nl>
 */
public class CpuAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "CpuAlarmReceiver";
    private static Runnable sumTask;

    @Override
    public void onReceive(Context context, Intent intent) {

        if (sumTask != null) {
            sumTask.run();
        }

        ScheduleAlarmTool scheduleTool = ScheduleAlarmTool.getInstance(context);
        // cancel the deterministic execution
        scheduleTool.mgr.cancel(scheduleTool.operation);
        
        scheduleTool.schedule();
    }

    public static void setSumTask(final Runnable task) {
        sumTask = task;
    }
}
