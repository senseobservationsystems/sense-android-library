package nl.sense_os.service.scheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import nl.sense_os.platform.SensePlatform;
import android.content.Context;

public class NewScheduler {
	
	private static final String TAG = "Scheduler";

	private final Context context;
	
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  	private SensePlatform sensePlatform;
	
  	private static NewScheduler instance = null;
  	
  	public static NewScheduler getInstance(Context context) {
		if (instance == null) {
            instance = new NewScheduler(context);
        }
        return instance;
	}
	
	protected NewScheduler(Context context) {
        this.context = context;
	}
	
	public static long gcd(long p, long q) {
	    if (q == 0) {
	      return p;
	    }
	    return gcd(q, p % q);
	}
	
	private static ScheduledFuture beeperHandle;
	
	class Task {
		public Runnable task;
		public long interval;
		public long flexibility;
		
	}

	private List<Task> tasksList = new ArrayList<Task>(); 
	
	
	/*public synchronized void register(Runnable command, long sensorInterval, long sensorFlexibility) {
		Task task = new Task();
		task.task = command;
		task.interval = sensorInterval;
		task.flexibility = sensorFlexibility;
		tasksList.add(task);
	}*/
	
	//TODO deregister (tasklist-- iflist--)
	
	private long interval;
	
	public synchronized void schedule(Runnable command, long sensorInterval, long sensorFlexibility) {
		
		Task task = new Task();
		task.task = command;
		task.interval = sensorInterval;
		task.flexibility = sensorFlexibility;
		
		//task.task.
		
		/*int index = tasksList.indexOf(task);
		
		if (index != -1) {
			tasksList.set(index, task);
		}*/
		
		
		if (tasksList.size() == 1) {
			interval = sensorInterval;
		}
		
		
		if (beeperHandle !=  null) {
			beeperHandle.cancel(true);
		}
		/*
		tasksList.get(0).equals(tasksList.get(1));
		Runnable tasks = new Runnable() {
			public void run() { 
				for (Runnable task : tasksList)
		        {
		            task.run();
		        }
		}};
		*/
		beeperHandle = scheduler.scheduleAtFixedRate(tasksList.get(0).task, interval, interval, TimeUnit.MILLISECONDS);

    }
}
