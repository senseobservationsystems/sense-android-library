package nl.sense_os.service.scheduler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * This class is responsible for executing the batched tasks when the alarm for deterministic
 * execution is received.
 * 
 * @author Kimon Tsitsikas <kimon@sense-os.nl>
 */
public class ExecutionAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "ExecutionAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Received alarm to execute
        // Wake up the CPU

    }

}
