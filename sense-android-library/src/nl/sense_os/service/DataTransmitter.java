/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service;

import nl.sense_os.datastorageengine.DataStorageEngine;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main;
import nl.sense_os.service.constants.SensePrefs.Main.Advanced;
import nl.sense_os.service.scheduler.Scheduler;
import nl.sense_os.service.storage.DSEDataConsumer;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.content.ComponentName;
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
 * {@link #scheduleTransmissions(Context)} when it starts sensing. <br/>
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
     * @param transmissionInterval The task transmitter interal
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
        DataStorageEngine dataStorageEngine = DataStorageEngine.getInstance(mContext);
        dataStorageEngine.syncData();
    }
}
