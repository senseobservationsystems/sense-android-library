/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service;

import nl.sense_os.service.constants.SensePrefs.Main;
import nl.sense_os.service.scheduler.Scheduler;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
 * When the transmission task is executed, an Intent is sent to the {@link MsgHandler} to empty its
 * buffer.<br/>
 * <br/>
 * The transmission frequency is based on the {@link Main#SYNC_RATE} preference. When the sync rate
 * is set to the real-time setting, we look at the and {@link Main#SAMPLE_RATE} to determine
 * periodic "just in case" transmissions. In case of transmission over 3G we transmit every one hour
 * for energy conservation.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class DataTransmitter implements Runnable {

    private static Context context;
    private long transmissionInterval;
	private static final String TAG = "Sense DataTransmitter";
    private static long ADAPTIVE_TRANSMISSION_INTERVAL = AlarmManager.INTERVAL_HALF_HOUR;
	public static final int REQ_CODE = 0x05E2DDA7A;

    private static DataTransmitter instance = null;

    /**
     * Factory method to get the singleton instance.
     * 
     * @param context
     * @return instance
     */
    public static DataTransmitter getInstance(Context context) {
        if (instance == null) {
            instance = new DataTransmitter(context);
        }
        return instance;
    }

    /**
     * Constructor.
     * 
     * @param context
     * @see #getInstance(Context)
     */
    protected DataTransmitter(Context context) {
        DataTransmitter.context = context;
    }

    @SuppressLint("NewApi")
    private long transmittedBytes = TrafficStats.getMobileTxBytes();
    private long lastTransmissionBytes = 0;
    private long lastTransmissionTime = 0;

    @SuppressLint("NewApi")
    @Override
    public void run() {

		// check if the service is (supposed to be) alive before scheduling next alarm
		if (true == ServiceStateHelper.getInstance(context).isLoggedIn()) {
            // check if transmission should be started
            ConnectivityManager connManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            // start the transmission if we have WiFi connection
            if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) || mWifi.isConnected()) {
                if ((SystemClock.elapsedRealtime() - lastTransmissionTime >= transmissionInterval)) {
                    transmissionService();
                }
            } else {
                // if there is no WiFi connection, postpone the transmission
                lastTransmissionBytes = TrafficStats.getMobileTxBytes() - transmittedBytes;
                transmittedBytes = TrafficStats.getMobileTxBytes();
                if ((SystemClock.elapsedRealtime() - lastTransmissionTime >= ADAPTIVE_TRANSMISSION_INTERVAL)) {
                    transmissionService();
                }
                // If any transmission took place recently, transmit the sensor data
                else if ((lastTransmissionBytes >= 500)
                        && (SystemClock.elapsedRealtime() - lastTransmissionTime >= ADAPTIVE_TRANSMISSION_INTERVAL
                                - (long) (ADAPTIVE_TRANSMISSION_INTERVAL * 0.2))) {
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
     * 
     * @param context
     *            Context to access Scheduler
     */
    public void startTransmissions(long transmissionInterval, long taskTransmitterInterval) {

        // schedule transmissions
        this.transmissionInterval = transmissionInterval;
        Scheduler.getInstance(context).register(this, taskTransmitterInterval,
                (long) (taskTransmitterInterval * 0.2));
    }

    /**
     * Stops the periodic transmission of sensor data.
     */
    public void stopTransmissions() {
        // stop transmissions
        Scheduler.getInstance(context).unregister(this);
	}

    /**
     * Initiates the data transmission.
     */
    public void transmissionService() {
        Log.v(TAG, "Start transmission");
        Intent task = new Intent(context.getString(R.string.action_sense_send_data));
        lastTransmissionTime = SystemClock.elapsedRealtime();
        ComponentName service = context.startService(task);
        if (null == service) {
            Log.w(TAG, "Failed to start data sync service");
        }
    }
}
