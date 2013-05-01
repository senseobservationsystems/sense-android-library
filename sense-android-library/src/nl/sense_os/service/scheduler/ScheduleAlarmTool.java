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
    private long remainingFlexibility, backwardsFlexibility;
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

    /**
     * Returns the next scheduled execution time.
     */
    public long getNextExecution() {
        return nextExecution;
    }

    /**
     * Resets the next scheduled execution time.
     */
    public void resetNextExecution() {

        nextExecution = 0;

        // reset the next execution time for all tasks
        for (Task task : tasks) {
            long now = SystemClock.elapsedRealtime();
            if (task.nextExecution > now) {
                // decrease execution time until it is back to the first possible time
                while (task.nextExecution - task.interval > now) {
                    task.nextExecution -= task.interval;
                }
            } else {
                // task does not have a valid next execution time (yet)
                Task foundTask = new Task();
            	long gcd = 1;
            	for (int i = 0; i < (tasks.size()-1); i++) {
            		long tempGcd = gcd(task.interval, tasks.get(i).interval);
                    if (tempGcd > gcd) {
                    	gcd = tempGcd;
                        foundTask = tasks.get(i);
                    }
                }
                if (gcd == 1) {
            		// schedule the next execution in ¨interval¨ milliseconds from now
            		task.nextExecution = now + task.interval;
            	} else {
            		task.nextExecution = foundTask.nextExecution;
            		while (task.nextExecution - task.interval > now) {
                        task.nextExecution -= task.interval;
                    }
            	}
            }
        }
        
    }
    
    private Intent intent;
    private Intent intentCpu;
    public PendingIntent operation;
    public PendingIntent operationCpu;
    public AlarmManager mgr;
    public AlarmManager mgrCpu;
    
    /**
     * Returns the greatest common divisor of p and q
     * 
     * @param p
     * @param q
     */
    public long gcd(long p, long q) {
    	if (q == 0) {
    		return p;
    	}
    	return gcd(q, p % q);
    }

    /**
     * Schedules the next task to execute.
     */
    public void schedule() {
        Log.v(TAG, "Schedule next execution of sample tasks");

        if (tasks.isEmpty()) {
            // nothing to schedule
            nextExecution = 0;
            return;
        }

        // find the next upcoming execution time
        // TODO: use the normal Arrays.sort() method
        final List<Runnable> tasksToExecute = new CopyOnWriteArrayList<Runnable>();
        Task temp;
        for (int i = 0; i < tasks.size(); i++) {
            for (int j = 1; j < (tasks.size() - i); j++) {
                if (tasks.get(j - 1).nextExecution > tasks.get(j).nextExecution) {
                    temp = tasks.get(j - 1);
                    tasks.set(j - 1, tasks.get(j));
                    tasks.set(j, temp);
                }
            }
        }

        // decide which tasks are to be executed at the next execution time
        nextExecution = tasks.get(0).nextExecution;
        tasksToExecute.add(tasks.get(0).runnable);

        // try to delay the execution in order to group multiple tasks together
        remainingFlexibility = tasks.get(0).flexibility;
        // flexibility for opportunistic execution if CPU is on
        backwardsFlexibility = tasks.get(0).flexibility;
        int i;
        for (i = 1; i < tasks.size(); i++) {
        	remainingFlexibility -= (tasks.get(i).nextExecution - tasks.get(i - 1).nextExecution);
        	// see if there is enough flexibility to batch with this task
            if (remainingFlexibility >= 0) {
            	// postpone execution time of the batch to this task
                nextExecution = tasks.get(i).nextExecution;
                // update the backward flexibility if the new batched task is less flexible 
                if (tasks.get(i).flexibility < backwardsFlexibility) {
                	backwardsFlexibility = tasks.get(i).flexibility;
                }
                tasksToExecute.add(tasks.get(i).runnable);                
            } else {
                break;
            }
        }

        // prepare the next execution time of the tasks that are going to be executed
        for (int j = 0; j < i; j++) {
            tasks.get(j).nextExecution = nextExecution + tasks.get(j).interval;
        }

        // create one summarized task
        Runnable sumTask = new Runnable() {
            public void run() {
                for (Runnable task : tasksToExecute) {
                    task.run();
                }
            }
        };

        CpuAlarmReceiver.setSumTask(sumTask);

        intent = new Intent(context, ExecutionAlarmReceiver.class);
        intentCpu = new Intent(context, CpuAlarmReceiver.class);
        operation = PendingIntent.getBroadcast(context, REQ_CODE, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        operationCpu = PendingIntent.getBroadcast(context, REQ_CODE, intentCpu,
                PendingIntent.FLAG_CANCEL_CURRENT);
        mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgrCpu = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // set the alarm for deterministic execution
        mgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextExecution, operation);
                
        // set the alarm for opportunistic execution
        mgrCpu.set(AlarmManager.ELAPSED_REALTIME, (nextExecution - backwardsFlexibility), operationCpu);
        
    }

    /**
     * Sets the task list.
     */
    public void setTasks(List<Task> tasks) {
        this.tasks = new CopyOnWriteArrayList<Task>(tasks);
    }
}
