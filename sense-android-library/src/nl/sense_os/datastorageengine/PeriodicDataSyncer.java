package nl.sense_os.datastorageengine;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

public class PeriodicDataSyncer extends WakefulBroadcastReceiver {
    private static String TAG = "DataSyncerAlarm";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Start the PeriodSyncService while holding the wake lock
        Intent timeIntent = new Intent(context, PeriodicSyncService.class);
        startWakefulService(context, timeIntent);
    }

    /**
     * Set the periodic alarm to start the PeriodicSyncService with a given interval
     * An alarm will also be fired immediately
     * @param context An Android context
     * @param interval The alarm interval in milliseconds
     */
    public static synchronized void setAlarm(Context context, Long interval) {
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, interval, getPendingIntent(context));
    }

    private static PendingIntent getPendingIntent(Context context)
    {
        Intent intent = new Intent(context, PeriodicDataSyncer.class);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    /**
     * Disable the periodic syncing alarm
     * @param context An Android context
     */
    public static synchronized void cancelAlarm(Context context) {
        // If the Alarm has been set, cancel it.
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getPendingIntent(context));
    }

    /**
     * Handle the data synchronization in an IntentService
     */
    public static class PeriodicSyncService extends IntentService {
        public static final String TAG = "PeriodicSyncService";

        public PeriodicSyncService() {
         super(TAG);
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            try {
                DataStorageEngine dse = DataStorageEngine.getInstance(getApplicationContext());
                // wait until the dse is ready
                dse.onReady().get();
                // wait until the sync is completed
                dse.syncData().get();
            }
            catch(Exception e) {
                Log.e(TAG, "Error synchronising data");
            }
            // Release the wake lock provided by the BroadcastReceiver.
            PeriodicDataSyncer.completeWakefulIntent(intent);
        }
    }

}
