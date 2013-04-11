package nl.sense_os.service.scheduler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ScheduleAlarmReceiver extends BroadcastReceiver {
	private static final String TAG = "ScheduleAlarmReceiver";
	private static Runnable sumTask;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "Received alarm to schedule");
		
        if (sumTask != null) {
        	sumTask.run();
        }
        
        ScheduleAlarmTool.schedule();

    }
    
    public static void setSumTask(final Runnable task) {
    	sumTask = new Runnable() {
			public void run() { 
				task.run();
		}};
    }
    
}
