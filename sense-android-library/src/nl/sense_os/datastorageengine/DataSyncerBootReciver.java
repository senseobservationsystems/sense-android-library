package nl.sense_os.datastorageengine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by fei on 30/10/15.
 */
public class DataSyncerBootReciver extends BroadcastReceiver {
    DataSyncerAlarmReceiver alarm = new DataSyncerAlarmReceiver();

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            // Set the alarm here.
            alarm.setAlarm(context);
        }
    }
}
