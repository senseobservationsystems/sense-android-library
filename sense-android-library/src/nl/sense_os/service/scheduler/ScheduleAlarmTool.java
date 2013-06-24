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

    private static final int REQ_CODE_DETERMINISTIC = 0xf01c0de;
    private static final int REQ_CODE_OPPORTUNISTIC = 0xf21d0de;
    private static ScheduleAlarmTool sInstance;
    private static final String TAG = "ScheduleAlarmTool";

    /**
     * Returns the greatest common divisor of p and q
     * 
     * @param p
     * @param q
     */
    private static long gcd(long p, long q) {
        if (q == 0) {
            return p;
        }
        return gcd(q, p % q);
    }

    /**
     * Factory method to get the singleton instance.
     * 
     * @param context
     * @return instance
     */
    public static ScheduleAlarmTool getInstance(Context context) {
        if (null == sInstance) {
            sInstance = new ScheduleAlarmTool(context);
        }
        return sInstance;
    }

    private long mBackwardsFlex;
    private Context mContext;
    private long mNextExecution = 0;
    private long mRemainingFlex;

    private List<Task> mTasks = new ArrayList<Scheduler.Task>();

    /**
     * Constructor.
     * 
     * @param context
     * @param tasksList
     * @see #getInstance(Context)
     */
    protected ScheduleAlarmTool(Context context) {
        mContext = context;
    }

    /**
     * Cancels the alarm
     * 
     * @param context
     *            Context to access AlarmManager
     */
    public void cancelDeterministicAlarm() {

        // re-create the operation that would go off
        Intent intent = new Intent(mContext, ExecutionAlarmReceiver.class);
        intent.putExtra(ExecutionAlarmReceiver.EXTRA_EXECUTION_TYPE,
                ExecutionAlarmReceiver.DETERMINISTIC_TYPE);
        PendingIntent operation = PendingIntent.getBroadcast(mContext, REQ_CODE_DETERMINISTIC,
                intent, PendingIntent.FLAG_ONE_SHOT);

        // cancel the alarm
        AlarmManager mgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        mgr.cancel(operation);
    }

    public void cancelOpportunisticAlarm() {

        // re-create the operation that would go off
        Intent intent = new Intent(mContext, ExecutionAlarmReceiver.class);
        intent.putExtra(ExecutionAlarmReceiver.EXTRA_EXECUTION_TYPE,
                ExecutionAlarmReceiver.OPPORTUNISTIC_TYPE);
        PendingIntent operation = PendingIntent.getBroadcast(mContext, REQ_CODE_OPPORTUNISTIC,
                intent, PendingIntent.FLAG_ONE_SHOT);

        // cancel the alarm
        AlarmManager mgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        mgr.cancel(operation);
    }

    /**
     * Calculates the next execution time and returns the batch of tasks that need to be executed at
     * that time.
     * 
     * @return
     */
    private Runnable getBatchTask() {

        // find the next upcoming execution time
        // TODO: use the normal Arrays.sort() method
        final List<Runnable> tasksToExecute = new CopyOnWriteArrayList<Runnable>();
        Task temp;
        for (int i = 0; i < mTasks.size(); i++) {
            for (int j = 1; j < (mTasks.size() - i); j++) {
                if (mTasks.get(j - 1).nextExecution > mTasks.get(j).nextExecution) {
                    temp = mTasks.get(j - 1);
                    mTasks.set(j - 1, mTasks.get(j));
                    mTasks.set(j, temp);
                }
            }
        }

        // decide which tasks are to be executed at the next execution time
        mNextExecution = mTasks.get(0).nextExecution;
        tasksToExecute.add(mTasks.get(0).runnable);

        // try to delay the execution in order to group multiple tasks together
        mRemainingFlex = mTasks.get(0).flexibility;
        // flexibility for opportunistic execution if CPU is on
        mBackwardsFlex = mTasks.get(0).flexibility;
        int i;
        for (i = 1; i < mTasks.size(); i++) {
            mRemainingFlex -= (mTasks.get(i).nextExecution - mTasks.get(i - 1).nextExecution);
            // see if there is enough flexibility to batch with this task
            if (mRemainingFlex >= 0) {
                // postpone execution time of the batch to this task
                mNextExecution = mTasks.get(i).nextExecution;
                // update the backward flexibility if the new batched task is less flexible
                if (mTasks.get(i).flexibility < mBackwardsFlex) {
                    mBackwardsFlex = mTasks.get(i).flexibility;
                }
                tasksToExecute.add(mTasks.get(i).runnable);
            } else {
                break;
            }
        }

        // prepare the next execution time of the tasks that are going to be executed
        for (int j = 0; j < i; j++) {
            mTasks.get(j).nextExecution = mNextExecution + mTasks.get(j).interval;
        }

        // create one summarized task
        Runnable batchTask = new Runnable() {
            @Override
            public void run() {
                for (Runnable task : tasksToExecute) {
                    task.run();
                }
            }
        };

        return batchTask;
    }

    /**
     * Resets the next scheduled execution time.
     */
    public void resetNextExecution() {
        Log.v(TAG, "Reset next execution");

        mNextExecution = 0;

        // reset the next execution time for all tasks
        for (Task task : mTasks) {
            if (task.nextExecution > SystemClock.elapsedRealtime()) {
                // decrease execution time until it is back to the first possible time
                while (task.nextExecution - task.interval > SystemClock.elapsedRealtime()) {
                    task.nextExecution -= task.interval;
                }
            } else {
                // task does not have a valid next execution time (yet)
                Task foundTask = null;
                long gcd = 1;
                // try to find GCD in the list
                for (Task gcdTask : mTasks) {
                    if (task.equals(gcdTask)) {
                        // do not get the gcd with yourself, dummy
                        continue;
                    }
                    long tempGcd = gcd(task.interval, gcdTask.interval);
                    if (tempGcd > gcd) {
                        gcd = tempGcd;
                        foundTask = gcdTask;
                    }
                }
                if (gcd == 1) {
                    // schedule the next execution in ¨interval¨ milliseconds from now
                    task.nextExecution = SystemClock.elapsedRealtime() + task.interval;
                } else {
                    task.nextExecution = foundTask.nextExecution;
                    while (task.nextExecution - task.interval > SystemClock.elapsedRealtime()) {
                        task.nextExecution -= task.interval;
                    }
                }
            }
        }
    }

    /**
     * Schedules the next task to execute.
     * 
     */
    public void schedule() {

        // check if there is anything to schedule
        if (mTasks.isEmpty()) {
            mNextExecution = 0;
            return;
        }

        // get the next batch of tasks
        Runnable batchTask = getBatchTask();
        ExecutionAlarmReceiver.setBatchTask(batchTask);

        // schedule the alarms
        AlarmManager mgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

        // set the alarm for opportunistic execution
        Intent opportunisticIntent = new Intent(mContext, ExecutionAlarmReceiver.class);
        opportunisticIntent.putExtra(ExecutionAlarmReceiver.EXTRA_EXECUTION_TYPE,
                ExecutionAlarmReceiver.OPPORTUNISTIC_TYPE);
        PendingIntent opportunisticOperation = PendingIntent.getBroadcast(mContext,
                REQ_CODE_OPPORTUNISTIC, opportunisticIntent, PendingIntent.FLAG_ONE_SHOT);
        mgr.set(AlarmManager.ELAPSED_REALTIME, (mNextExecution - mBackwardsFlex),
                opportunisticOperation);

        // set the alarm for deterministic execution
        Intent deterministicIntent = new Intent(mContext, ExecutionAlarmReceiver.class);
        deterministicIntent.putExtra(ExecutionAlarmReceiver.EXTRA_EXECUTION_TYPE,
                ExecutionAlarmReceiver.DETERMINISTIC_TYPE);
        PendingIntent deterministicOperation = PendingIntent.getBroadcast(mContext,
                REQ_CODE_DETERMINISTIC, deterministicIntent, PendingIntent.FLAG_ONE_SHOT);
        mgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, mNextExecution, deterministicOperation);
    }

    /**
     * Sets the task list.
     */
    public void setTasks(List<Task> tasks) {
        mTasks = new CopyOnWriteArrayList<Task>(tasks);
    }
}
