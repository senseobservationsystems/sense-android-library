/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.location;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.ctrl.Controller;
import nl.sense_os.service.provider.SNTP;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

/**
 * Represents the location sensor. Implements the basic Android LocationListener to receive location
 * updates from the OS.<br/>
 * <br/>
 * Generates data for the following sensors:
 * <ul>
 * <li>position</li>
 * <li>traveled distance 1h</li>
 * </ul>
 * 
 * @author Ted Schmidt <ted@sense-os.nl>
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class LocationSensor {
	
	private static LocationSensor instance = null;
	
    protected LocationSensor(Context context) {
		this.context = context;
		locMgr = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        controller = Controller.getController(context);
		gpsListener = new MyLocationListener();
		nwListener = new MyLocationListener();
		pasListener = new MyLocationListener();
		distanceEstimator = new TraveledDistanceEstimator();
	}
    
    public static LocationSensor getInstance(Context context) {
	    if(instance == null) {
	       instance = new LocationSensor(context);
	    }
	    return instance;
    }
	
	private class MyLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location fix) {

			if (null != lastGpsFix) {
				if (SNTP.getInstance().getTime() - lastGpsFix.getTime() < MIN_SAMPLE_DELAY) {
					// Log.v(TAG, "New location fix is too fast after last one");
					return;
				}
			}
			
			//controller = Controller.getController(context);
			JSONObject json = new JSONObject();
			try {
				json.put("latitude", fix.getLatitude());
				json.put("longitude", fix.getLongitude());

				// always include all JSON fields, or we get problems with varying data_structure
				json.put("accuracy", fix.hasAccuracy() ? fix.getAccuracy() : -1.0d);
				json.put("altitude", fix.hasAltitude() ? fix.getAltitude() : -1.0);
				json.put("speed", fix.hasSpeed() ? fix.getSpeed() : -1.0d);
				json.put("bearing", fix.hasBearing() ? fix.getBearing() : -1.0d);
				json.put("provider", null != fix.getProvider() ? fix.getProvider() : "unknown");

				if (fix.getProvider().equals(LocationManager.GPS_PROVIDER)) {
					lastGpsFix = fix;
				} 
				else if (fix.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
					lastNwFix = fix;
				}
				else {		
					//do nothing
				} 

			} catch (JSONException e) {
				Log.e(TAG, "JSONException in onLocationChanged", e);
				return;
			}

            // use SNTP time
            long timestamp = SNTP.getInstance().getTime();

			// pass message to the MsgHandler
			Intent i = new Intent(context.getString(R.string.action_sense_new_data));
			i.putExtra(DataPoint.SENSOR_NAME, SensorNames.LOCATION);
			i.putExtra(DataPoint.VALUE, json.toString());
			i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
            i.putExtra(DataPoint.TIMESTAMP, timestamp);
			context.startService(i);

			distanceEstimator.addPoint(fix);
		}

		@Override
		public void onProviderDisabled(String provider) {
			controller.checkSensorSettings(isGpsAllowed, isListeningNw, isListeningGps, time, lastGpsFix, listenGpsStart, lastNwFix, listenNwStart, listenGpsStop, listenNwStop);
		}

		@Override
		public void onProviderEnabled(String provider) {
			controller.checkSensorSettings(isGpsAllowed, isListeningNw, isListeningGps, time, lastGpsFix, listenGpsStart, lastNwFix, listenNwStart, listenGpsStop, listenNwStop);
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// do nothing
		}
	}

	private static final String TAG = "Sense LocationSensor";
	private static final String ALARM_ACTION = "nl.sense_os.service.LocationAlarm";
	private static final String DISTANCE_ALARM_ACTION = "nl.sense_os.service.LocationAlarm.distanceAlarm";
	private static final int ALARM_ID = 56;
	public static final long MIN_SAMPLE_DELAY = 5000; // 5 sec
	private static final int DISTANCE_ALARM_ID = 70;

	private Controller controller;
	private Context context;
	private LocationManager locMgr;
	private final MyLocationListener gpsListener;
	private final MyLocationListener nwListener;
	private final MyLocationListener pasListener;

	private long time;
	private float distance;
	//private Controller controller;

	/**
	 * Receiver for periodic alarms to check on the sensor status.
	 */
	public BroadcastReceiver alarmReceiver = new BroadcastReceiver() {

		
		@Override
		public void onReceive(Context context, Intent intent) {
			//controller = Controller.getController(context);
			controller.checkSensorSettings(isGpsAllowed, isListeningNw, isListeningGps, time, lastGpsFix, listenGpsStart, lastNwFix, listenNwStart, listenGpsStop, listenNwStop);
		}
	};

	private boolean isGpsAllowed;
	private boolean isNetworkAllowed;

	private boolean isListeningGps;
	private boolean isListeningNw;	
	private long listenGpsStart;
	private long listenGpsStop;
	private Location lastGpsFix;
	private Location lastNwFix;
	private long listenNwStart;
	private long listenNwStop;

	private TraveledDistanceEstimator distanceEstimator;

	/**
	 * Receiver for periodic alarms to calculate distance values.
	 */
	public final BroadcastReceiver distanceAlarmReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			double distance = distanceEstimator.getTraveledDistance();
			// pass message to the MsgHandler
			Intent i = new Intent(context.getString(R.string.action_sense_new_data));
			i.putExtra(DataPoint.SENSOR_NAME, SensorNames.TRAVELED_DISTANCE_1H);
			i.putExtra(DataPoint.VALUE, (float) distance);
			i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.FLOAT);
			i.putExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime());
			context.startService(i);

			// start counting again, from the last location
			distanceEstimator.reset();
		}
	};

	
	/**
	 * Stops listening for location updates.
	 */
	public void disable() {
		// Log.v(TAG, "Disable location sensor");

		stopListening();
		stopAlarms();
	}

	/**
	 * Starts listening for location updates, using the provided time and distance parameters.
	 * 
	 * @param time
	 *            The minimum time between notifications, in meters.
	 * @param distance
	 *            The minimum distance interval for notifications, in meters.
	 */
	public void enable(long time, float distance) {
		// Log.v(TAG, "Enable location sensor");

		setTime(time);
		this.distance = distance;

		startListening();
		startAlarms();
		getLastKnownLocation();
	}

	
	private void getLastKnownLocation() {

		// get the most recent location fixes
		Location gpsFix = locMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		Location nwFix = locMgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

		// see which is best
		if (null != gpsFix || null != nwFix) {
			Location bestFix = null;
			if (null != gpsFix) {
				if (SNTP.getInstance().getTime() - 1000 * 60 * 60 * 1 < gpsFix.getTime()) {
					// recent GPS fix
					bestFix = gpsFix;
				}
			}
			if (null != nwFix) {
				if (null == bestFix) {
					bestFix = nwFix;
				} else if (nwFix.getTime() < gpsFix.getTime()
						&& nwFix.getAccuracy() < bestFix.getAccuracy() + 100) {
					// network fix is more recent and pretty accurate
					bestFix = nwFix;
				}
			}
			if (null != bestFix) {
				// use last known location as first sensor value
				gpsListener.onLocationChanged(bestFix);
			} else {
				// no usable last known location
			}
		} else {
			// no last known location
		}
	}



	public void notifyListeningRestarted(String msg) {

		// Intent log = new Intent(MsgHandler.ACTION_NEW_MSG);
		// log.putExtra(MsgHandler.KEY_SENSOR_NAME, "GPS logger");
		// log.putExtra(MsgHandler.KEY_SENSOR_DEVICE, "Sense");
		// log.putExtra(MsgHandler.KEY_TIMESTAMP, SNTP.getInstance().getTime());
		// log.putExtra(MsgHandler.KEY_VALUE, "Start: " + msg);
		// log.putExtra(MsgHandler.KEY_DATA_TYPE, "string");
		// context.startService(log);
	}

	public void notifyListeningStopped(String msg) {

		// Intent log = new Intent(MsgHandler.ACTION_NEW_MSG);
		// log.putExtra(MsgHandler.KEY_SENSOR_NAME, "GPS logger");
		// log.putExtra(MsgHandler.KEY_SENSOR_DEVICE, "Sense");
		// log.putExtra(MsgHandler.KEY_TIMESTAMP, SNTP.getInstance().getTime());
		// log.putExtra(MsgHandler.KEY_VALUE, "Stop: " + msg);
		// log.putExtra(MsgHandler.KEY_DATA_TYPE, "string");
		// context.startService(log);
	}
	
	/**
	 * @param distance
	 *            Minimum distance between location updates.
	 */
	public float getDistance() {
		return this.distance;
	}

	public void setGpsListening(boolean listen) {
		if (listen) { 
			locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, time, distance, gpsListener);
			isListeningGps = true;
			listenGpsStart = SNTP.getInstance().getTime();
			lastGpsFix = null;
		} else {
			locMgr.removeUpdates(gpsListener);
			listenGpsStop = SNTP.getInstance().getTime();
			isListeningGps = false;
		}
	}

	public void setNetworkListening(boolean listen) {
		if (listen/* && isNetworkAllowed*/) { 
			try {
				locMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, time, distance,
						nwListener);
				isListeningNw = true;	
				listenNwStart = SNTP.getInstance().getTime(); 
				lastNwFix = null;	
			} catch (IllegalArgumentException e) {
				Log.e(TAG, "Failed to start listening to network provider! " + e);
				listenNwStop = SNTP.getInstance().getTime(); 
				isListeningNw = false;	
			}
		} else {
			locMgr.removeUpdates(nwListener);
			listenNwStop = SNTP.getInstance().getTime(); 
			isListeningNw = false;	
		}
	}

	/**
	 * Listens to passive location provider. Should only be called in Android versions where the
	 * provider is available.
	 */
	private void setPassiveListening(boolean listen) {
		if (listen) {
			locMgr.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, time, distance,
					pasListener);
		} else {
			locMgr.removeUpdates(pasListener);
		}
	}

	/**
	 * @param time
	 *            Minimum time between location refresh attempts.
	 */
	private void setTime(long time) {
		this.time = time;
	}

	private void startAlarms() {

		// register to receive the alarm
		context.getApplicationContext().registerReceiver(alarmReceiver, new IntentFilter(ALARM_ACTION));
		context.getApplicationContext().registerReceiver(distanceAlarmReceiver, new IntentFilter(DISTANCE_ALARM_ACTION));

		// start periodic alarm
		Intent alarm = new Intent(ALARM_ACTION);
		PendingIntent operation = PendingIntent.getBroadcast(context, ALARM_ID, alarm, 0);
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.cancel(operation);
		am.setRepeating(AlarmManager.RTC_WAKEUP, SNTP.getInstance().getTime(), time, operation);

		// start periodic for distance
		Intent distanceAlarm = new Intent(DISTANCE_ALARM_ACTION);
		PendingIntent operation2 = PendingIntent.getBroadcast(context, DISTANCE_ALARM_ID,
				distanceAlarm, 0);
		am.cancel(operation2);
		am.setRepeating(AlarmManager.RTC_WAKEUP, SNTP.getInstance().getTime(), 60L * 60 * 1000,
				operation2);

		/*
		 * TODO: this also for 24 hours PendingIntent operation3 =
		 * PendingIntent.getBroadcast(context, DISTANCE_ALARM_ID, distanceAlarm, 0);
		 * am.cancel(operation3); am.setRepeating(AlarmManager.RTC_WAKEUP,
		 * SNTP.getInstance().getTime(), 24L * 60 * 60 * 1000, operation3);
		 */
	}

	private void startListening() {

		SharedPreferences mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
				Context.MODE_PRIVATE);
		isGpsAllowed = mainPrefs.getBoolean(Main.Location.GPS, true);
		isNetworkAllowed = mainPrefs.getBoolean(Main.Location.NETWORK, true);

		// start listening to GPS and/or Network location
		if (isGpsAllowed) {
			setGpsListening(true);
		}
		if (isNetworkAllowed) {
			// Log.v(TAG, "Start listening to location updates from Network");
			setNetworkListening(true);
		}

		if (!isGpsAllowed && !isNetworkAllowed
				&& Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			Log.v(TAG, "Start passively listening to location updates for other apps");
			setPassiveListening(true);
		}
	}

	public void stopAlarms() {
		// unregister the receiver
		try {
			context.unregisterReceiver(alarmReceiver);
			context.unregisterReceiver(distanceAlarmReceiver);
		} catch (IllegalArgumentException e) {
			// do nothing
		}

		// stop the alarms
		Intent alarm = new Intent(ALARM_ACTION);
		PendingIntent operation = PendingIntent.getBroadcast(context, ALARM_ID, alarm, 0);
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.cancel(operation);

		Intent distanceAlarm = new Intent(ALARM_ACTION);
		PendingIntent operation2 = PendingIntent.getBroadcast(context, DISTANCE_ALARM_ID,
				distanceAlarm, 0);
		am.cancel(operation2);
	}

	/**
	 * Stops listening for location updates.
	 */
	private void stopListening() {
		setGpsListening(false);
		setNetworkListening(false);
		setPassiveListening(false);
	}
}
