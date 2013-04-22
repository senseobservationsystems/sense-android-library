package nl.sense_os.service.shared;

import nl.sense_os.service.scheduler.Scheduler;
import android.content.Context;

/**
 * Generic BroadcastReceiver implementation for {@link BasePeriodicPollingSensor} classes. Calls
 * {@link BasePeriodicPollingSensor#doSample()} whenever a broadcast is received.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class PeriodicPollAlarmReceiver implements Runnable {

    private final PeriodicPollingSensor sensor;

    public PeriodicPollAlarmReceiver(PeriodicPollingSensor sensor) {
        this.sensor = sensor;
    }

    @Override
    public void run() {
        sensor.doSample();
    }

    /**
     * Starts periodically calling {@link BasePeriodicPollingSensor#doSample()}. Schedules a
     * periodic alarm, and registers itself as alarm receiver.
     * 
     * @param context
     *            Application context, used to register as BroadcastReceiver, and to schedule alarms
     */
    public void start(Context context) {

        // schedule alarm broadcasts
        long interval = sensor.getSampleRate();
        Scheduler.getInstance(context).register(this, interval, (long) (interval * 0.1));
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
        Scheduler.getInstance(context).unregister(this);
    }
}
