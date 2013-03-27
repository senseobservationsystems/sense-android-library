package nl.sense_os.service.shared;

import nl.sense_os.service.scheduler.Scheduler;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;

/**
 * Generic BroadcastReceiver implementation for {@link PeriodicPollingSensor} classes. Calls
 * {@link PeriodicPollingSensor#doSample()} whenever a broadcast is received.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class PeriodicPollAlarmReceiver extends BroadcastReceiver implements Runnable {

    private final PeriodicPollingSensor sensor;
    private final String action;
    private final int reqCode;

    public PeriodicPollAlarmReceiver(PeriodicPollingSensor sensor) {
        this.sensor = sensor;
        action = sensor.getClass().getName() + ".SAMPLE";
        reqCode = action.hashCode();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (sensor.isActive()) {
            sensor.doSample();
        }
    }
    
    public void run() {
    	if (sensor.isActive()) {
            sensor.doSample();
        }
    }

    /**
     * Starts periodically calling {@link PeriodicPollingSensor#doSample()}. Schedules a periodic
     * alarm, and registers itself as alarm receiver.
     * 
     * @param context
     *            Application context, used to register as BroadcastReceiver, and to schedule alarms
     */
    public void start(Context context) {

        // register for alarms
        context.registerReceiver(this, new IntentFilter(action));

        // schedule alarm broadcasts
        long interval = sensor.getSampleRate();
        Intent alarm = new Intent(action);
        PendingIntent alarmOperation = PendingIntent.getBroadcast(context, reqCode, alarm, 0);
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.cancel(alarmOperation);
        mgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(),
                interval, alarmOperation);
        
        Scheduler.getInstance(context).schedule(this, interval, (long)(interval * 0.1));
    }

    /**
     * Stops the periodic polling. Unregisters as BroadcastReceiver and stops the periodic alarm
     * broadcasts.
     * 
     * @param context
     *            Application context, used to modify broadcast registration and alarm schedule
     */
    public void stop(Context context) {

        // stop alarm broadcasts
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarms.cancel(PendingIntent.getBroadcast(context, reqCode, new Intent(action), 0));

        // unregister as receiver for alarms
        try {
            context.unregisterReceiver(this);
        } catch (IllegalArgumentException e) {
            // ignore
        }
        
        Scheduler.getInstance(context).unRegister(this);
    }
}
