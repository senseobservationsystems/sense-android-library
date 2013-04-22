package nl.sense_os.service.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import nl.sense_os.service.scheduler.Scheduler.Task;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

/**
 * 
 * @author Kimon Tsitsikas <kimon@sense-os.nl>
 */
public class ScheduleAlarmTool {

    private static final String TAG = "ScheduleAlarmTool";
    private static final int REQ_CODE = 0xf01c0de;

    private static ScheduleAlarmTool instance;

    /**
     * Factory method to get the singleton instance.
     * 
     * @param context
     * @return instance
     */
    public static ScheduleAlarmTool getInstance(Context context) {
        if (null == instance) {
            instance = new ScheduleAlarmTool(context);
        }
        return instance;
    }

    private Context context;
    private long nextExecution = 0;
    private long remainedFlexibility;
    private List<Task> tasks = new ArrayList<Scheduler.Task>();

    /**
     * Constructor.
     * 
     * @param context
     * @param tasksList
     */
    protected ScheduleAlarmTool(Context context) {
        this.context = context;
    }

    /**
     * Cancels the alarm
     * 
     * @param context
     *            Context to access AlarmManager
     */
    public void cancel(Context context) {
        Log.v(TAG, "Cancel execution alarm");

        // re-create the operation that would go off
        Intent intent = new Intent(context, ExecutionAlarmReceiver.class);
        PendingIntent operation = PendingIntent.getBroadcast(context, REQ_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // cancel the alarm
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.cancel(operation);
    }

    public long getNextExecution() {
        if (nextExecution == 0) {
            return SystemClock.elapsedRealtime();
        } else {
            return nextExecution;
        }
    }

    /**
     * Schedules the next task to execute.
     */
    public void schedule() {
        Log.v(TAG, "Schedule next execution");

        if (tasks.isEmpty()) {
            // nothing to schedule
            return;
        }

        int i, j;
        final List<Runnable> tasksToExecute = new CopyOnWriteArrayList<Runnable>();

        // bubble sort
        Task temp;
        for (i = 0; i < tasks.size(); i++) {
            for (j = 1; j < (tasks.size() - i); j++) {
                if (tasks.get(j - 1).nextExecution > tasks.get(j).nextExecution) {
                    temp = tasks.get(j - 1);
                    tasks.set(j - 1, tasks.get(j));
                    tasks.set(j, temp);
                }
            }
        }

        nextExecution = SystemClock.elapsedRealtime() + tasks.get(0).interval;
        if (tasks.size() == 1) {
            tasksToExecute.add(tasks.get(0).runnable);
            tasks.get(0).nextExecution = nextExecution + tasks.get(0).interval;
        } else {
            remainedFlexibility = tasks.get(0).flexibility;
            for (i = 0; i < tasks.size() - 1; i++) {
                if (remainedFlexibility >= 0) {
                    nextExecution = tasks.get(i).nextExecution;
                    tasksToExecute.add(tasks.get(i).runnable);
                    remainedFlexibility = remainedFlexibility
                            - (tasks.get(i + 1).nextExecution - tasks.get(i).nextExecution);
                } else {
                    break;
                }
            }

            for (j = 0; j < i; j++) {
                tasks.get(j).nextExecution = nextExecution + tasks.get(j).interval;
            }
        }

        Runnable sumTask = new Runnable() {
            public void run() {
                for (Runnable task : tasksToExecute) {
                    task.run();
                }
            }
        };
        ExecutionAlarmReceiver.setSumTask(sumTask);

        // prepare the operation to go off
        Intent intent = new Intent(context, ExecutionAlarmReceiver.class);
        PendingIntent operation = PendingIntent.getBroadcast(context, REQ_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // set the alarm
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.cancel(operation);
        mgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextExecution, operation);

        // Log.d(TAG, "Next execution: " + (nextExecution - SystemClock.elapsedRealtime()) + "ms");
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = new CopyOnWriteArrayList<Task>(tasks);
    }
}
