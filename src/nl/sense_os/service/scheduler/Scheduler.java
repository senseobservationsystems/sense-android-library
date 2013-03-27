package nl.sense_os.service.scheduler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;
import android.util.Log;

public class Scheduler {
	private static final String TAG = "Scheduler";

	private final Context context;
	
	
	//private static Scheduler instance = null;
	
	
	//public CallMe ()

    /**
     * Factory method to get the singleton instance.
     * 
     * @param context
     * @return 
     * @return instance
     */
	
	private static Scheduler instance = null;
	
	public static Scheduler getInstance(Context context) {
		if (instance == null) {
            instance = new Scheduler(context);
        }
        return instance;
	}
	
	protected Scheduler(Context context) {
        this.context = context;
	}
	
	private static long gcd(long p, long q) {
	    if (q == 0) {
	      return p;
	    }
	    return gcd(q, p % q);
	}
	
	class Task {
		public Runnable runnable;
		public long interval;
		public long flexibility;
	}

	private List<Task> tasksList = new CopyOnWriteArrayList<Task>(); 
	
	/*
	 * Called when a sample rate changes
	 * Use Preferences for the rates
	 */

	private long interval = 5000; 
	
	public void unRegister(Runnable command) {
		for (Task task : tasksList)
        {
            if (task.runnable.equals(command)) {
            	tasksList.remove(task);
            }
        }
	}
	
	public void schedule(Runnable command, long sensorInterval, long sensorFlexibility) {
		
		int index = -1;
		Task newTask = new Task();
		newTask.runnable = command;
		newTask.interval = sensorInterval;
		newTask.flexibility = sensorFlexibility;
		for (Task task : tasksList)
        {
            if (task.runnable.equals(newTask.runnable)) {
            	index = tasksList.indexOf(task);
            }
        }
		if (index != -1) {
			tasksList.set(index, newTask);
	    }
	    else {
	    	tasksList.add(newTask);
	    }
		/*
		if (tasksList.size() == 1) {
			interval = sensorInterval;
		}
		else {
			interval = gcd(tasksList.get(0).interval, tasksList.get(1).interval);
		}
		*/
		//int cycle0 = (int)(tasksList.get(0).interval)/(int)interval;
		//int cycle1 = (int)(tasksList.get(1).interval)/(int)interval;
		
		
		Log.w(TAG, "task list size " + tasksList.size() + "interval " + interval);
		
		ScheduleAlarmTool.cancel(context);
		ScheduleAlarmTool.schedule(context, interval);
	}

}
