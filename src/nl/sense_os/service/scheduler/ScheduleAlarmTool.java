package nl.sense_os.service.scheduler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import nl.sense_os.service.scheduler.Scheduler.Task;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

public class ScheduleAlarmTool {
	private static final String TAG = "ScheduleAlarmTool";
    private static final int REQ_CODE = 0xf00c0de;
    private static Context context;

    /**
     * Cancels the alarm to finish the test
     * 
     * @param context
     *            Context to access AlarmManager
     */
    public static void cancel(Context context) {
        Log.v(TAG, "Cancel test finish alarm");

        // re-create the operation that would go off
        Intent intent = new Intent(context, ScheduleAlarmReceiver.class);
        PendingIntent operation = PendingIntent.getBroadcast(context, REQ_CODE, intent, 0);

        // cancel the alarm
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.cancel(operation);
    }

    private static long interval, nextExecution = 0;
    private static long remainedFlexibility;

    public static long getNextExecution() {
    	if (nextExecution == 0) {
    		return SystemClock.elapsedRealtime();
    	}
    	else {
    		return nextExecution;
    	}
    }
    /**
     * Schedules an alarm to finish the current test
     * 
     * @param context
     *            Context to access AlarmManager
     * @param duration
     *            Duration of the test, in milliseconds
     */
    public static void schedule() {
        
    	int i, j;
        final List<Runnable> tasksToExecute = new CopyOnWriteArrayList<Runnable>();
       
        /*
        interval = tasks.get(0).interval;
        for (i = 0; i < tasks.size(); i++) {
        	if (tasks.get(i).nextExecution < SystemClock.elapsedRealtime()) {
				tasksToExecute.add(tasks.get(i).runnable);
				tasks.get(i).nextExecution = SystemClock.elapsedRealtime() + tasks.get(i).interval;
			}
        }
        nextExecution = tasks.get(0).nextExecution;
        */
        
        // /* 
        //bubble sort
        Task temp;
        for(i=0; i < tasks.size(); i++){
            for(j=1; j < (tasks.size()-i); j++){
                    if(tasks.get(j-1).nextExecution > tasks.get(j).nextExecution){
                            temp = tasks.get(j-1);
                            tasks.set(j-1, tasks.get(j));
                            tasks.set(j, temp);
                    }    
            }
        }
         
        nextExecution = SystemClock.elapsedRealtime() + tasks.get(0).interval;
        if (tasks.size() == 1){
        	tasksToExecute.add(tasks.get(0).runnable);
        	tasks.get(0).nextExecution = nextExecution + tasks.get(0).interval;
        }
        else {
        	remainedFlexibility = tasks.get(0).flexibility;
	        for (i = 0; i < tasks.size()-1; i++) {
	        	if (remainedFlexibility >= 0) {
	        		nextExecution = tasks.get(i).nextExecution;
					tasksToExecute.add(tasks.get(i).runnable);
					remainedFlexibility = remainedFlexibility - (tasks.get(i+1).nextExecution - tasks.get(i).nextExecution);
				}
	        	else {
	        		break;
	        	}
	        }

	        for (j = 0; j < i; j++) {
	        	tasks.get(j).nextExecution = nextExecution + tasks.get(j).interval;
	        }
	        
        }
        
        // */
        Runnable sumTask = new Runnable() {
			public void run() { 
				for (Runnable task : tasksToExecute)
		        {
		            task.run();
		        }
		}};
		
		ScheduleAlarmReceiver.setSumTask(sumTask);
        // prepare the operation to go off
        Intent intent = new Intent(context, ScheduleAlarmReceiver.class);
        PendingIntent operation = PendingIntent.getBroadcast(context, REQ_CODE, intent, 0);
        // set the alarm
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.cancel(operation);
        mgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextExecution, operation);
    }

	private static List<Task> tasks = null;//new CopyOnWriteArrayList<Task>(); 
    
    public static void start(Context context, List<Task> tasksList) {
    	
    	ScheduleAlarmTool.context = context;
    	tasks = new CopyOnWriteArrayList<Task>(tasksList);
    	schedule();
    }

    /**
     * Private constructor to prevent instantiation
     */
    private ScheduleAlarmTool() {
        // do not instantiate
    }
}
