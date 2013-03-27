package nl.sense_os.service.scheduler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class ScheduleAlarmReceiver extends BroadcastReceiver {
	private static final String TAG = "ScheduleAlarmReceiver";
	private SharedPreferences prefs;
	private long motion_c, location_c;
	private static int cycle = 1;
	private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "Received alarm to schedule");
        this.context = context;
		
        //Tasks();
        
        cycle++;
        
        if (cycle ==  1000) {
        	cycle = 1;
        }
    }
    /*
    int cycle0, cycle1;
    //dummy
    public void cycles(int cycle0, int cycle1) {
    	this.cycle0 = cycle0;
    	this.cycle1 = cycle1;
    }
    
    private void Tasks() {
    	if (motionTrigger()) {
    		MotionSensor motion = MotionSensor.getInstance(context);
    		motion.doSample();
    	}
    	if (locationTrigger()) {
    		LocationSensor location = LocationSensor.getInstance(context);
    		location.doSample();
    	}
    }
    
    private boolean motionTrigger() {
    	if ((cycle%motion_c) == 0) {
    		return true;
    	}
    	return false;
    }
    
    private boolean locationTrigger() {
    	Log.v(TAG, "DEBUG" + (cycle%location_c) + " CYCLE " + cycle + " LOCATION C " + location_c);
    	if ((cycle%location_c) == 0) {
    		//Log.v(TAG, "OLE" + (cycle%location_c));
    		return true;
    	}
    	return false;
    }*/
}
