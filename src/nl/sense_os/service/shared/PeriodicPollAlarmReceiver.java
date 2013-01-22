package nl.sense_os.service.shared;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Generic BroadcastReceiver implementation for {@link PeriodicPollingSensor} classes. Calls
 * {@link PeriodicPollingSensor#doSample()} whenever a broadcast is received.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class PeriodicPollAlarmReceiver extends BroadcastReceiver {

    private PeriodicPollingSensor sensor;

    public PeriodicPollAlarmReceiver(PeriodicPollingSensor sensor) {
        this.sensor = sensor;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (sensor.isActive()) {
            sensor.doSample();
        }
    }
}
