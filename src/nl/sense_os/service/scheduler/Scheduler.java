package nl.sense_os.service.scheduler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;
import android.util.Log;

public class Scheduler {
	private static final String TAG = "Scheduler";

	private final Context context;
	
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
		public long nextExecution;
	}

	private List<Task> tasksList = new CopyOnWriteArrayList<Task>(); 
	
	/*
	 * Called when a sample rate changes
	 * Use Preferences for the rates
	 */
	
	public void unregister(Runnable command) {
		for (Task task : tasksList)
        {
            if (task.runnable.equals(command)) {
            	tasksList.remove(task);
            }
        }
		if (!tasksList.isEmpty()){
			//In order to maintain the task with the shortest interval in position 0
			long interval = tasksList.get(0).interval;
			Task temp = null;
			int index = 0;
	    	for (int i = 1; i < tasksList.size(); i++) {
				if (tasksList.get(i).interval < interval) {
					interval = tasksList.get(i).interval;
					index = i;
					temp = tasksList.get(i);
				}
	        }
	    	if (index != 0) {
	    		tasksList.remove(index);
	    		tasksList.add(0, temp);
	    	}
			
			Log.w(TAG, "task list size " + tasksList.size() + "interval " + tasksList.get(0).interval);
			
			ScheduleAlarmTool.start(context, tasksList);
		}
	}
	
	public void register(Runnable command, long sensorInterval, long sensorFlexibility) {
		
		int index = -1;
		Task newTask = new Task();
		newTask.runnable = command;
		newTask.interval = sensorInterval;
		newTask.flexibility = sensorFlexibility;
		newTask.nextExecution = ScheduleAlarmTool.getNextExecution() + sensorInterval;
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
		
		//In order to maintain the task with the shortest interval in position 0
		long interval = tasksList.get(0).interval;
		Task temp = null;
		index = 0;
    	for (int i = 1; i < tasksList.size(); i++) {
			if (tasksList.get(i).interval < interval) {
				interval = tasksList.get(i).interval;
				index = i;
				temp = tasksList.get(i);
			}
        }
    	if (index != 0) {
    		tasksList.remove(index);
    		tasksList.add(0, temp);
    	}
				
		Log.w(TAG, "task list size " + tasksList.size() + "interval " + tasksList.get(0).interval);
		
		ScheduleAlarmTool.start(context, tasksList);
	}

}
