package nl.sense_os.service.scheduler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;
import android.util.Log;

/**
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
    private List<Task> tasksList = new CopyOnWriteArrayList<Task>();

    /**
     * Constructor.
     * 
     * @param context
     */
    protected Scheduler(Context context) {
        this.context = context;
    }

    /**
     * Registers a new command to be scheduled with a given interval.
     * 
     * @param command
     * @param sensorInterval
     * @param sensorFlexibility
     */
    public void register(Runnable command, long sensorInterval, long sensorFlexibility) {

        ScheduleAlarmTool scheduleTool = ScheduleAlarmTool.getInstance(context);

        int index = -1;
        Task newTask = new Task();
        newTask.runnable = command;
        newTask.interval = sensorInterval;
        newTask.flexibility = sensorFlexibility;
        newTask.nextExecution = scheduleTool.getNextExecution() + sensorInterval;
        for (Task task : tasksList) {
            if (task.runnable.equals(newTask.runnable)) {
                index = tasksList.indexOf(task);
            }
        }
        if (index != -1) {
            tasksList.set(index, newTask);
        } else {
            tasksList.add(newTask);
        }

        Log.v(TAG, "task list size " + tasksList.size() + "interval " + tasksList.get(0).interval);

        scheduleTool.setTasks(tasksList);
        scheduleTool.schedule();
    }

    /**
     * Unregisters a command.
     * 
     * @param command
     */
    public void unregister(Runnable command) {
        for (Task task : tasksList) {
            if (task.runnable.equals(command)) {
                tasksList.remove(task);
            }
        }
        if (!tasksList.isEmpty()) {

            Log.v(TAG, "task list size " + tasksList.size() + "interval "
                    + tasksList.get(0).interval);

            ScheduleAlarmTool scheduleTool = ScheduleAlarmTool.getInstance(context);
            scheduleTool.setTasks(tasksList);
            scheduleTool.schedule();
        }
    }
}
