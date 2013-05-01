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
    private static Scheduler instance = null;

    /**
     * Factory method to get the singleton instance.
     * 
     * @param context
     * @return instance
     */
    public static Scheduler getInstance(Context context) {
        if (instance == null) {
            instance = new Scheduler(context);
        }
        return instance;
    }

    private final Context context;
    private CopyOnWriteArrayList<Task> tasksList = new CopyOnWriteArrayList<Task>();

    /**
     * Constructor.
     * 
     * @param context
     */
    protected Scheduler(Context context) {
        this.context = context;
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

        ScheduleAlarmTool scheduleTool = ScheduleAlarmTool.getInstance(context);

        // prepare task object
        Task newTask = new Task();
        newTask.runnable = command;
        newTask.interval = sensorInterval;
        newTask.flexibility = sensorFlexibility;

        // add the task to the list of tasks
        int index = -1;
        for (Task task : tasksList) {
            if (task.runnable.equals(newTask.runnable)) {
                index = tasksList.indexOf(task);
            }
        }
        if (index != -1) {
            // replace existing task
            tasksList.set(index, newTask);
        } else {
            tasksList.add(newTask);
        }

        scheduleTool.setTasks(tasksList);
        scheduleTool.resetNextExecution();
        scheduleTool.schedule();
    }

    /**
     * Unregisters a task.
     * 
     * @param command
     */
    public synchronized void unregister(Runnable command) {
        Log.v(TAG, "Unregister sample task");

        for (Task task : tasksList) {
            if (task.runnable.equals(command)) {
                tasksList.remove(task);
            }
        }

        ScheduleAlarmTool scheduleTool = ScheduleAlarmTool.getInstance(context);
        scheduleTool.setTasks(tasksList);
        if (!tasksList.isEmpty()) {
            // nothing to do: there are still other tasks
            scheduleTool.resetNextExecution();
            scheduleTool.schedule();

        } else {
            // stop scheduling alarms
            scheduleTool.cancel(context);
        }
    }
}
