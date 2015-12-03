/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.scheduler;

import nl.sense_os.datastorageengine.PeriodicDataSyncer;
import nl.sense_os.service.ServiceStateHelper;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main;
import nl.sense_os.service.constants.SensePrefs.Main.Advanced;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

/**
 * This class is responsible for initiating the transmission of buffered data. It works by
 * registering the transmission task to the scheduler. The Sense service calls
 * {@link #scheduleTransmissions()} when it starts sensing. <br/>
 * <br/>
 * When the transmission task is executed, the DataStorageEngine.syncData() is called to synchronize with the back-end<br/>
 * <br/>
 * The transmission frequency is based on the {@link Main#SYNC_RATE} preference. When the sync rate
 * is set to the real-time setting, we look at the and {@link Main#SAMPLE_RATE} to determine
 * periodic "just in case" transmissions. In case of transmission over 3G we transmit every one hour
 * for energy conservation.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class DataTransmitter implements Runnable {

    private static final long ADAPTIVE_TX_INTERVAL = AlarmManager.INTERVAL_HALF_HOUR;
    private static final String TAG = "DataTransmitter";
    private static DataTransmitter sInstance;

    private class Intervals {
        static final long ECO = AlarmManager.INTERVAL_HALF_HOUR;
        static final long RARELY = 1000 * 60 * 15;
        static final long NORMAL = 1000 * 60 * 5;
        static final long OFTEN = 1000 * 60 * 1;
        static final long BALANCED = 1000 * 60 * 3;
    }

    /**
     * Factory method to get the singleton instance.
     * 
     * @param context
     * @return instance
     */
    public static DataTransmitter getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DataTransmitter(context);
        }
        return sInstance;
    }

    private Context mContext;
    private long mLastTxBytes = 0;
    private long mLastTxTime = 0;
    private long mTxInterval;
    private long mTxBytes;

    /**
     * Constructor.
     * 
     * @param context
     * @see #getInstance(Context)
     */
    @TargetApi(Build.VERSION_CODES.FROYO)
    protected DataTransmitter(Context context) {
        mContext = context;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            mTxBytes = TrafficStats.getMobileTxBytes();
        }
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    @Override
    public void run() {

        // check if the service is (supposed to be) alive before scheduling next alarm
        if (true == ServiceStateHelper.getInstance(mContext).isLoggedIn()) {
            // check if transmission should be started
            ConnectivityManager connManager = (ConnectivityManager) mContext
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            // start the transmission if we have WiFi connection
            if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) || wifi.isConnected()) {
                if ((SystemClock.elapsedRealtime() - mLastTxTime >= mTxInterval)) {
                    transmissionService();
                }
            } else {
            	long interval = mTxInterval;
            	
            	SharedPreferences mainPrefs = mContext.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
            	boolean energy_saving = mainPrefs.getBoolean(Advanced.MOBILE_INTERNET_ENERGY_SAVING_MODE, true);
            	boolean wifiUploadOnly = mainPrefs.getBoolean(Advanced.WIFI_UPLOAD_ONLY, false);
            	if(wifiUploadOnly)
            		return;
                if(energy_saving)
                	interval = ADAPTIVE_TX_INTERVAL; //if there is no WiFi connection, postpone the transmission
            	               
                mLastTxBytes = TrafficStats.getMobileTxBytes() - mTxBytes;
                mTxBytes = TrafficStats.getMobileTxBytes();
                if ((SystemClock.elapsedRealtime() - mLastTxTime >= interval)) {
                    transmissionService();
                }
                // if any transmission took place recently, use the tail to transmit the sensor data
                else if (energy_saving && (mLastTxBytes >= 500)
                        && (SystemClock.elapsedRealtime() - mLastTxTime >= ADAPTIVE_TX_INTERVAL
                                - (long) (ADAPTIVE_TX_INTERVAL * 0.2))) {
                    transmissionService();
                } else {
                    // do nothing
                }
            }

        } else {
            // skip transmission: Sense service is not logged in
        }
    }

    /**
     * Starts the periodic transmission of sensor data.
     * @param taskTransmitterInterval The transmission interval
     * @param transmissionInterval The task transmitter interval
     */
    public void startTransmissions(long transmissionInterval, long taskTransmitterInterval) {

        // schedule transmissions
        mTxInterval = transmissionInterval;
        Scheduler.getInstance(mContext).register(this, taskTransmitterInterval,
                (long) (taskTransmitterInterval * 0.2));
    }

    /**
     * Stops the periodic transmission of sensor data.
     */
    public void stopTransmissions() {
        // stop transmissions
        Scheduler.getInstance(mContext).unregister(this);
    }

    /**
     * Initiates the data transmission.
     */
    public void transmissionService() {
        Log.v(TAG, "Start transmission");
        mLastTxTime = SystemClock.elapsedRealtime();
        // tick the periodic data syncer
        Intent intent = new Intent(mContext, PeriodicDataSyncer.class);
        mContext.sendBroadcast(intent);
    }

    /**
     * Starts periodic transmission of the buffered sensor data.
     */
    public void scheduleTransmissions() {
        Log.v(TAG, "Schedule transmissions");

        SharedPreferences mainPrefs = mContext.getSharedPreferences(SensePrefs.MAIN_PREFS,
                Context.MODE_PRIVATE);
        int syncRate = Integer.parseInt(mainPrefs.getString(Main.SYNC_RATE, SensePrefs.Main.SyncRate.NORMAL));
        int sampleRate = Integer.parseInt(mainPrefs.getString(Main.SAMPLE_RATE, SensePrefs.Main.SampleRate.NORMAL));

        // pick transmission interval
        long txInterval;
        switch (syncRate) {
            case 2: // rarely, every 15 minutes
                txInterval = Intervals.RARELY;
                break;
            case 1: // eco-mode
                txInterval = Intervals.ECO;
                break;
            case 0: // 5 minute
                txInterval = Intervals.NORMAL;
                break;
            case -1: // 60 seconds
                txInterval = Intervals.OFTEN;
                break;
            case -2: // real-time: schedule transmission based on sample time
                switch (sampleRate) {
                    case 2: // balanced
                        txInterval = Intervals.BALANCED * 3;
                        break;
                    case 1: // rarely
                        txInterval = Intervals.ECO * 3;
                        break;
                    case 0: // normal
                        txInterval = Intervals.NORMAL * 3;
                        break;
                    case -1: // often
                        txInterval = Intervals.OFTEN * 3;
                        break;
                    case -2: // real time
                        txInterval = Intervals.OFTEN;
                        break;
                    default:
                        Log.w(TAG, "Unexpected sample rate value: " + sampleRate);
                        return;
                }
                break;
            default:
                Log.w(TAG, "Unexpected sync rate value: " + syncRate);
                return;
        }

        // pick transmitter task interval
        long txTaskInterval;
        switch (sampleRate) {
            case -2: // real time
                txTaskInterval = 0;
                break;
            case -1: // often
                txTaskInterval = 10 * 1000;
                break;
            case 0: // normal
                txTaskInterval = 60 * 1000;
                break;
            case 1: // rarely (15 minutes)
                txTaskInterval = 15 * 60 * 1000;
                break;
            case 2: // balanced (3 minutes)
                txTaskInterval = 3 * 60 * 1000;
                break;
            default:
                Log.w(TAG, "Unexpected sample rate value: " + sampleRate);
                return;
        }

        DataTransmitter transmitter = DataTransmitter.getInstance(mContext);
        transmitter.startTransmissions(txInterval, txTaskInterval);
    }
}
