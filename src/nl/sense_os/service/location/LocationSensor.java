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
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.storage.LocalStorage;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

public class LocationSensor {

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location fix) {

            if (null != lastGpsFix) {
                if (SNTP.getInstance().getTime() - lastGpsFix.getTime() < MIN_SAMPLE_DELAY) {
                    // Log.v(TAG, "New location fix is too fast after last one");
                    return;
                }
            }

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

            } catch (JSONException e) {
                Log.e(TAG, "JSONException in onLocationChanged", e);
                return;
            }

            // pass message to the MsgHandler
            Intent i = new Intent(context.getString(R.string.action_sense_new_data));
            i.putExtra(DataPoint.SENSOR_NAME, SensorNames.LOCATION);
            i.putExtra(DataPoint.VALUE, json.toString());
            i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
            i.putExtra(DataPoint.TIMESTAMP, fix.getTime());
            context.startService(i);
            
            distanceEstimator.addPoint(fix);
        }

        @Override
        public void onProviderDisabled(String provider) {
            checkSensorSettings();
        }

        @Override
        public void onProviderEnabled(String provider) {
            checkSensorSettings();
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

    private final Context context;
    private final LocationManager locMgr;
    private final MyLocationListener gpsListener;
    private final MyLocationListener nwListener;
    private final MyLocationListener pasListener;

    private long time;
    private float distance;

    /**
     * Receiver for periodic alarms to check on the sensor status.
     */
    private final BroadcastReceiver alarmReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            checkSensorSettings();
        }
    };

    private boolean isGpsAllowed;
    private boolean isNetworkAllowed;

    private boolean isListeningGps;
    private long listenGpsStart;
    private long listenGpsStop;
    private Location lastGpsFix;
    
    private TraveledDistanceEstimator distanceEstimator;
    
    /**
     * Receiver for periodic alarms to calculate distance values.
     */
    private final BroadcastReceiver distanceAlarmReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
        	double distance = distanceEstimator.getTraveledDistance();
            // pass message to the MsgHandler
            Intent i = new Intent(context.getString(R.string.action_sense_new_data));
            i.putExtra(DataPoint.SENSOR_NAME, SensorNames.TRAVELED_DISTANCE_1H);
            i.putExtra(DataPoint.VALUE, (float)distance);
            i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.FLOAT);
            i.putExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime());
            context.startService(i);
            
            //start counting again, from the last location
            distanceEstimator.reset();  
        }
    };

    public LocationSensor(Context context) {
        this.context = context;
        locMgr = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        gpsListener = new MyLocationListener();
        nwListener = new MyLocationListener();
        pasListener = new MyLocationListener();
        distanceEstimator = new TraveledDistanceEstimator();
    }

    /**
     * Checks to see if the sensor is still doing a useful job or whether it is better if we disable
     * it for a while. This method is a callback for a periodic alarm to check the sensor status.
     * 
     * @see #alarmReceiver
     */
    private void checkSensorSettings() {

        SharedPreferences mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
                Context.MODE_PRIVATE);
        boolean selfAwareMode = isGpsAllowed && mainPrefs.getBoolean(Main.Location.AUTO_GPS, true);

        if (selfAwareMode) {
            // Log.v(TAG, "Check location sensor settings...");

            if (isListeningGps) {

                if (!isGpsProductive()) {
                    // switch off
                    Log.d(TAG, "Switch GPS off because it was not productive");
                    setGpsListening(false);
                    notifyListeningStopped("not productive");

                } else {
                    // we're fine
                    Log.d(TAG, "Keep listening for GPS");
                }

            } else {

                if (isAccelerating()) {
                    // switch on
                    Log.d(TAG, "Switch GPS back on because the device is moving around alot");
                    setGpsListening(true);
                    notifyListeningRestarted("moved");

                } else if (isPositionChanged()) {
                    // switch on
                    Log.d(TAG, "Switch GPS back on because its position changed");
                    setGpsListening(true);
                    notifyListeningRestarted("position changed");

                } else if (isSwitchedOffTooLong()) {
                    // switch on
                    Log.d(TAG, "Switch GPS back on because it has been too long since the last try");
                    setGpsListening(true);
                    notifyListeningRestarted("timeout");

                } else {
                    // we're fine
                    Log.d(TAG, "No need to start listening to GPS");
                }
            }
        }
    }

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

    private boolean isAccelerating() {
        // Log.v(TAG, "Check if device was accelerating recently");

        boolean moving = true;

        Cursor data = null;
        try {
            // get linear acceleration data
            long timerange = 1000 * 60 * 15; // 15 minutes
            Uri uri = Uri.parse("content://" + context.getString(R.string.local_storage_authority)
                    + DataPoint.CONTENT_URI_PATH);
            String[] projection = new String[] { DataPoint.SENSOR_NAME, DataPoint.TIMESTAMP,
                    DataPoint.VALUE };
            String selection = DataPoint.SENSOR_NAME + "='" + SensorNames.LIN_ACCELERATION + "'"
                    + " AND " + DataPoint.TIMESTAMP + ">"
                    + (SNTP.getInstance().getTime() - timerange);
            data = LocalStorage.getInstance(context).query(uri, projection, selection, null, null);

            if (null == data || data.getCount() == 0) {
                // no movement measurements: assume the device is moving
                Log.d(TAG, "No movement measurements: assume the device is moving");
                return true;
            }

            // find the largest motion measurement
            data.moveToFirst();
            double totalMotion = 0;
            while (!data.isAfterLast()) {
                String value = data.getString(data.getColumnIndex(DataPoint.VALUE));
                JSONObject json = new JSONObject(value);
                double x = json.getDouble("x-axis");
                double y = json.getDouble("y-axis");
                double z = json.getDouble("z-axis");
                double motion = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
                totalMotion += motion;
                data.moveToNext();
            }
            double avgMotion = totalMotion / data.getCount();

            if (avgMotion > 4) {
                Log.v(TAG, "Device is moving! Average motion: " + avgMotion);
                moving = true;
            } else {
                Log.d(TAG, "Device is NOT moving. Average motion: " + avgMotion);
                moving = false;
            }

        } catch (JSONException e) {
            Log.e(TAG, "Exception parsing linear acceleration data: ", e);
            moving = true;
        } finally {
            if (null != data) {
                data.close();
            }
        }

        return moving;
    }

    /**
     * @return true if GPS has recently produced new data points.
     */
    private boolean isGpsProductive() {

        boolean productive = isListeningGps;
        long maxDelay = 2 * time;
        if (isListeningGps) {

            // check if any updates have been received recently from the GPS sensor
            if (lastGpsFix != null && lastGpsFix.getTime() > listenGpsStart) {
                if (SNTP.getInstance().getTime() - lastGpsFix.getTime() > maxDelay) {
                    // no updates for long time
                    Log.d(TAG, "GPS is NOT productive: no updates for a long time");
                    productive = false;
                }
            } else if (SNTP.getInstance().getTime() - listenGpsStart > maxDelay) {
                // no updates for a long time
                Log.d(TAG, "GPS is NOT productive: no updates since start listening");
                productive = false;
            } else {
                Log.d(TAG, "GPS is productive");
            }

        } else {
            Log.d(TAG, "GPS is NOT productive: the listener is disabled");
        }

        return productive;
    }

    private boolean isPositionChanged() {
        // Log.v(TAG, "Check if position changed recently");

        boolean moved = true;

        Cursor data = null;
        try {
            // get location data from time since the last check
            long timerange = 1000 * 60 * 15; // 15 minutes
            Uri uri = Uri.parse("content://" + context.getString(R.string.local_storage_authority)
                    + DataPoint.CONTENT_URI_PATH);
            String[] projection = new String[] { DataPoint.SENSOR_NAME, DataPoint.TIMESTAMP,
                    DataPoint.VALUE };
            String selection = DataPoint.SENSOR_NAME + "='" + SensorNames.LOCATION + "'" + " AND "
                    + DataPoint.TIMESTAMP + ">" + (SNTP.getInstance().getTime() - timerange);
            data = LocalStorage.getInstance(context).query(uri, projection, selection, null, null);

            if (null == data || data.getCount() < 2) {
                // no position changes: assume the device is moving
                Log.d(TAG, "No location samples: assume the device is moving");
                return true;
            }

            // find the first motion measurement
            data.moveToFirst();
            JSONObject startJson = new JSONObject(data.getString(data
                    .getColumnIndex(DataPoint.VALUE)));
            Location startLoc = new Location("");
            startLoc.setLatitude(startJson.getDouble("latitude"));
            startLoc.setLongitude(startJson.getDouble("longitude"));
            startLoc.setAccuracy((float) startJson.getDouble("accuracy"));
            startLoc.setTime(data.getLong(data.getColumnIndex(DataPoint.TIMESTAMP)));

            // find the last motion measurement
            data.moveToLast();
            JSONObject endJson = new JSONObject(
                    data.getString(data.getColumnIndex(DataPoint.VALUE)));
            Location endLoc = new Location("");
            endLoc.setLatitude(endJson.getDouble("latitude"));
            endLoc.setLongitude(endJson.getDouble("longitude"));
            endLoc.setAccuracy((float) endJson.getDouble("accuracy"));
            endLoc.setTime(data.getLong(data.getColumnIndex(DataPoint.TIMESTAMP)));

            // calculate the distance traveled
            float distance = 0;
            float accuracy = Float.MAX_VALUE;
            if (null != startLoc && null != endLoc) {
                float[] results = new float[1];
                Location.distanceBetween(startLoc.getLatitude(), startLoc.getLongitude(),
                        endLoc.getLatitude(), endLoc.getLongitude(), results);
                distance = results[0];
                accuracy = startLoc.getAccuracy() + endLoc.getAccuracy();
            }
            Log.d(TAG, "Distance: " + distance + " m, accuracy: " + accuracy + " m");

            if (distance > accuracy) {
                Log.v(TAG, "Position has changed");
                moved = true;
            } else {
                // position did NOT change
                moved = false;
            }

        } catch (JSONException e) {
            Log.e(TAG, "Exception parsing location data: ", e);
            moved = true;
        } finally {
            if (null != data) {
                data.close();
            }
        }

        return moved;
    }

    private boolean isSwitchedOffTooLong() {

        if (isListeningGps) {
            Log.w(TAG, "No use checking if GPS is switched off too long: it is still listening!");
            return false;
        }

        boolean tooLong = false;
        long maxDelay = 1000 * 60 * 60; // 1 hour

        if (SNTP.getInstance().getTime() - listenGpsStop > maxDelay) {
            // GPS has been turned off for a long time, or was never even started
            Log.d(TAG, "GPS has been turned off for a long time, or was never even started");
            tooLong = true;
        } else if (!locMgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            // the network provider is disabled: GPS is the only option
            Log.d(TAG, "The network provider is disabled");
            tooLong = true;
        } else {
            Log.d(TAG, "GPS was only recently switched off");
            tooLong = false;
        }

        return tooLong;
    }

    private void notifyListeningRestarted(String msg) {

        // Intent log = new Intent(MsgHandler.ACTION_NEW_MSG);
        // log.putExtra(MsgHandler.KEY_SENSOR_NAME, "GPS logger");
        // log.putExtra(MsgHandler.KEY_SENSOR_DEVICE, "Sense");
        // log.putExtra(MsgHandler.KEY_TIMESTAMP, SNTP.getInstance().getTime());
        // log.putExtra(MsgHandler.KEY_VALUE, "Start: " + msg);
        // log.putExtra(MsgHandler.KEY_DATA_TYPE, "string");
        // context.startService(log);
    }

    private void notifyListeningStopped(String msg) {

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
    public void setDistance(float distance) {
        this.distance = distance;
    }

    private void setGpsListening(boolean listen) {
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

    private void setNetworkListening(boolean listen) {
        if (listen) {
            locMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, time, distance,
                    nwListener);
        } else {
            locMgr.removeUpdates(nwListener);
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
    public void setTime(long time) {
        this.time = time;
    }

    private void startAlarms() {

        // register to recieve the alarm
        context.registerReceiver(alarmReceiver, new IntentFilter(ALARM_ACTION));
        context.registerReceiver(distanceAlarmReceiver, new IntentFilter(DISTANCE_ALARM_ACTION));

        // start periodic alarm
        Intent alarm = new Intent(ALARM_ACTION);
        PendingIntent operation = PendingIntent.getBroadcast(context, ALARM_ID, alarm, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(operation);
        am.setRepeating(AlarmManager.RTC_WAKEUP, SNTP.getInstance().getTime(), time, operation);
        
        // start periodic for distance
        Intent distanceAlarm = new Intent(DISTANCE_ALARM_ACTION);
        PendingIntent operation2 = PendingIntent.getBroadcast(context, DISTANCE_ALARM_ID, distanceAlarm, 0);
        am.cancel(operation2);
        am.setRepeating(AlarmManager.RTC_WAKEUP, SNTP.getInstance().getTime(), 60L * 60 * 1000, operation2);

        /* TODO: this also for 24 hours
        PendingIntent operation3 = PendingIntent.getBroadcast(context, DISTANCE_ALARM_ID, distanceAlarm, 0);
        am.cancel(operation3);
        am.setRepeating(AlarmManager.RTC_WAKEUP, SNTP.getInstance().getTime(), 24L * 60 * 60 * 1000, operation3);
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

    private void stopAlarms() {
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
        PendingIntent operation2 = PendingIntent.getBroadcast(context, DISTANCE_ALARM_ID, distanceAlarm, 0);
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
