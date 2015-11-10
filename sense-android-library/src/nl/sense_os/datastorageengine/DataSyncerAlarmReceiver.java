package nl.sense_os.datastorageengine;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

public class DataSyncerAlarmReceiver extends WakefulBroadcastReceiver {
    private static String TAG = "DataSyncerAlarmReceiver";

    private Object mLock = new Object();

    private Context mContext;
    private DataSyncer mDataSyncer;
    private ErrorCallback mErrorCallback;
    private AlarmManager mAlarmManager;  // The app's AlarmManager, which provides access to the system mAlarm services.
    private PendingIntent mAlarmIntent;  // The pending intent that is triggered when the Alarm fires.

    private long mSyncRate = 1800000;  // 30 minutes in milliseconds by default

    public DataSyncerAlarmReceiver () {
        // TODO: remove this constructor?
    }

    public DataSyncerAlarmReceiver (Context context, DataSyncer dataSyncer, ErrorCallback errorCallback) {
        mContext = context;
        mDataSyncer = dataSyncer;
        mErrorCallback = errorCallback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");

        try {
            mDataSyncer.sync();
        } catch (Exception err) {
            if (mErrorCallback != null) {
                mErrorCallback.onError(err);
            }
            else {
                err.printStackTrace();
            }
        }
    }

    public void setAlarm() {
        // cancel if already running
        cancelAlarm();

        synchronized (mLock) {
            // create a new alarm
            mAlarmManager = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(mContext, DataSyncerAlarmReceiver.class);
            mAlarmIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
            mAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, mSyncRate, mAlarmIntent);
        }
    }

    public void cancelAlarm() {
        synchronized (mLock) {
            // If the Alarm has been set, cancel it.
            if (mAlarmManager != null) {
                mAlarmManager.cancel(mAlarmIntent);
                mAlarmManager = null;
            }
       }
    }

    public boolean isRunning() {
        synchronized (mLock) {
            return mAlarmManager != null;
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
     * Set sync rate.
     * After changing the sync rate, the alarm receiver must be started again using setAlarm.
     * @param syncRate Sync rate in milliseconds (1800000 (= 30 minutes) by default)
     */
    public void setSyncRate(long syncRate) {
        this.mSyncRate = syncRate;
    }
}
