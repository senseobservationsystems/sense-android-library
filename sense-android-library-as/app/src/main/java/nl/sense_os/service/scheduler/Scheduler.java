package nl.sense_os.service.scheduler;

import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;
import android.util.Log;

/**
 * This class is responsible for scheduling the sampling tasks of the phone, as also the sensor data
 * transmission. It applies batch scheduling and opportunistic execution algorithms in order to
 * reduce the number of CPU wakeups and thus the energy consumption.
 * 
 * @author Kimon Tsitsikas <kimon@sense-os.nl>
 */
public class Scheduler {

    protected static class Task {
        public Runnable runnable;
        public long interval;
        public long flexibility;
        public long nextExecution;
    }

    private static final String TAG = "Scheduler";
    private static Scheduler sInstance;

    /**
     * Factory method to get the singleton instance.
     * 
     * @param context
     * @return instance
     */
    public static Scheduler getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new Scheduler(context);
        }
        return sInstance;
    }

    private final Context mContext;
    private CopyOnWriteArrayList<Task> mTasks = new CopyOnWriteArrayList<Task>();

    /**
     * Constructor.
     * 
     * @param context
     */
    protected Scheduler(Context context) {
        mContext = context;
    }

    /**
     * Registers a new task to be scheduled with a given interval.
     * 
     * @param command
     *            The runnable of the new task
     * @param sensorInterval
     *            Execution interval of the new task
     * @param sensorFlexibility
     *            Delay tolerance of task execution
     */
    public synchronized void register(Runnable command, long sensorInterval, long sensorFlexibility) {
        Log.v(TAG, "Register new sample task at " + sensorInterval + "ms interval");

        ScheduleAlarmTool scheduleTool = ScheduleAlarmTool.getInstance(mContext);

        // prepare task object
        Task newTask = new Task();
        newTask.runnable = command;
        newTask.interval = sensorInterval;
        newTask.flexibility = sensorFlexibility;

        // add the task to the list of tasks
        int index = -1;
        for (Task task : mTasks) {
            if (task.runnable.equals(newTask.runnable)) {
                index = mTasks.indexOf(task);
            }
        }
        if (index != -1) {
            // remove existing task (probably interval changed)
            mTasks.remove(index);
        }
        // add new task to the end of the list
        mTasks.add(newTask);

        scheduleTool.setTasks(mTasks);
        scheduleTool.resetNextExecution();
        scheduleTool.cancelDeterministicAlarm();
        scheduleTool.cancelOpportunisticAlarm();
        scheduleTool.schedule();
    }

    /**
     * Unregisters a task.
     * 
     * @param command
     */
    public synchronized void unregister(Runnable command) {
        Log.v(TAG, "Unregister sample task");

        for (Task task : mTasks) {
            if (task.runnable.equals(command)) {
                mTasks.remove(task);
            }
        }

        // stop scheduling alarms
        ScheduleAlarmTool scheduleTool = ScheduleAlarmTool.getInstance(mContext);
        scheduleTool.cancelDeterministicAlarm();
        scheduleTool.cancelOpportunisticAlarm();

        // update reschedule alarms
        scheduleTool.setTasks(mTasks);
        if (!mTasks.isEmpty()) {
            // reschedule! there are still other tasks
            scheduleTool.resetNextExecution();
            scheduleTool.cancelDeterministicAlarm();
            scheduleTool.cancelOpportunisticAlarm();
            scheduleTool.schedule();

        } else {
            // nothing to do
        }
    }
}
