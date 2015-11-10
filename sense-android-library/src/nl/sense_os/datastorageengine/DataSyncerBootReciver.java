package nl.sense_os.datastorageengine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DataSyncerBootReciver extends BroadcastReceiver {
    DataSyncerAlarmReceiver mAlarm = new DataSyncerAlarmReceiver();

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            // Set the mAlarm here.
            mAlarm.setAlarm(context);
        }
    }
}
