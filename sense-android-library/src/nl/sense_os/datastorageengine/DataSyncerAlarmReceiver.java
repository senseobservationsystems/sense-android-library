package nl.sense_os.datastorageengine;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.content.WakefulBroadcastReceiver;

public class DataSyncerAlarmReceiver extends WakefulBroadcastReceiver {
    // The app's AlarmManager, which provides access to the system mAlarm services.
    private AlarmManager mAlarmManager;
    // The pending intent that is triggered when the Alarm fires.
    private PendingIntent mAlarmIntent;

    private long mSyncRate = 1800000;  // 30 minutes in milliseconds

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, DataSyncer.PeriodicSyncService.class);
        // Start the service, keeping the device awake while it is launching.
        startWakefulService(context, service);
    }

    public void setAlarm(Context context) {
        mAlarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, DataSyncerAlarmReceiver.class);
        mAlarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        mAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, mSyncRate, mAlarmIntent);
    }

    public void cancelAlarm() {
        // If the Alarm has been set, cancel it.
        if (mAlarmManager != null) {
            mAlarmManager.cancel(mAlarmIntent);
        }
    }

    /**
     * Get the current sync rate
     * @return Returns the sync rate in milliseconds
     */
    public long getSyncRate() {
        return mSyncRate;
    }

    /**
     * Set sync rate
     * @param syncRate Sync rate in milliseconds (1800000 (= 30 minutes) by default)
     */
    public void setSyncRate(long syncRate) {
        this.mSyncRate = syncRate;
    }
}
