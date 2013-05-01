/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service;

import nl.sense_os.service.constants.SensePrefs.Main;
import nl.sense_os.service.scheduler.Scheduler;
import android.app.AlarmManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
	private static final String TAG = "Sense DataTransmitter";
    private static long lastTransmission = 0;
    private static long ADAPTIVE_TRANSMISSION_INTERVAL = AlarmManager.INTERVAL_HOUR;
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

	@Override
    public void run() {

		// check if the service is (supposed to be) alive before scheduling next alarm
		if (true == ServiceStateHelper.getInstance(context).isLoggedIn()) {
            // check if transmission should be started
            ConnectivityManager connManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            Intent task = new Intent(context.getString(R.string.action_sense_send_data));
            // start the transmission if we have WiFi connection
            //if (mWifi.isConnected()) {
                Log.i(TAG, "Start transmission");
                ComponentName service = context.startService(task);
                if (null == service) {
                    Log.w(TAG, "Failed to start data sync service");
                }   
            /*} else {
                // if there is no WiFi connection, postpone the transmission
                if (SystemClock.elapsedRealtime() - lastTransmission >= ADAPTIVE_TRANSMISSION_INTERVAL) {
                    ComponentName service = context.startService(task);
                    if (null == service) {
                        Log.w(TAG, "Failed to start data sync service");
                    } else {
                        lastTransmission = SystemClock.elapsedRealtime();
                    }
                }
            }*/

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
    public void startTransmissions(long interval) {

        // schedule transmissions
        Scheduler.getInstance(context).register(this, interval, (long) (interval * 0.1));
    }

    /**
     * Stops the periodic transmission of sensor data.
     * 
     * @param context
     *            Context to access Scheduler
     */
    public void stopTransmissions() {
        // stop transmissions
        Scheduler.getInstance(context).unregister(this);
	}
}
