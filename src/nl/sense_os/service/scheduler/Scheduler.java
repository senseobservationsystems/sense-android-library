package nl.sense_os.service.scheduler;

import android.content.Context;

public class Scheduler {
	private final Context context;
	
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
    
	protected Scheduler(Context context) {
        this.context = context;
    }
	
	/*
	 * Called when a sample rate changes
	 * Use Preferences for the rates
	 * Schedule based on the minimum interval
	 */
	public void schedule() {
		ScheduleAlarmTool.schedule(context, 15*1000);
	}
}
