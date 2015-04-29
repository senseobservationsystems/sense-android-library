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
import nl.sense_os.service.shared.PeriodicPollAlarmReceiver;
import nl.sense_os.service.shared.PeriodicPollingSensor;
import nl.sense_os.service.shared.SensorDataPoint;
import nl.sense_os.service.subscription.BaseSensor;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
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
public class LocationSensor extends BaseSensor implements PeriodicPollingSensor {

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location fix) {

            if (null != lastGpsFix) {
                if (SNTP.getInstance().getTime() - lastGpsFix.getTime() < MIN_SAMPLE_DELAY) {
                    // Log.v(TAG, "New location fix is too fast after last one");
                    return;
                }
            }

            // controller = Controller.getController(context);
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
                } else if (fix.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
                    lastNwFix = fix;
                } else {
                    // do nothing
                }

            } catch (JSONException e) {
                Log.e(TAG, "JSONException in onLocationChanged", e);
                return;
            }

            // use SNTP time
            long timestamp = SNTP.getInstance().getTime();
            notifySubscribers();
            SensorDataPoint dataPoint = new SensorDataPoint(json);
            dataPoint.sensorName = SensorNames.LOCATION;
            dataPoint.sensorDescription = SensorNames.LOCATION;
            dataPoint.timeStamp = timestamp;
            sendToSubscribers(dataPoint);

            // pass message to the MsgHandler
            Intent i = new Intent(context.getString(R.string.action_sense_new_data));
            i.putExtra(DataPoint.SENSOR_NAME, SensorNames.LOCATION);
            i.putExtra(DataPoint.VALUE, json.toString());
            i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
            i.putExtra(DataPoint.TIMESTAMP, timestamp);
            i.setPackage(context.getPackageName());
            context.startService(i);

            distanceEstimator.addPoint(fix);
        }

        @Override
        public void onProviderDisabled(String provider) {
            controller.checkSensorSettings(isGpsAllowed, isListeningNw, isListeningGps,
                    getSampleRate(), lastGpsFix, listenGpsStart, lastNwFix, listenNwStart,
                    listenGpsStop, listenNwStop);
        }

        @Override
        public void onProviderEnabled(String provider) {
            controller.checkSensorSettings(isGpsAllowed, isListeningNw, isListeningGps,
                    getSampleRate(), lastGpsFix, listenGpsStart, lastNwFix, listenNwStart,
                    listenGpsStop, listenNwStop);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // do nothing
        }
    }

    private static final String TAG = "Sense LocationSensor";
    private static final String DISTANCE_ALARM_ACTION = "nl.sense_os.service.LocationAlarm.distanceAlarm";
    public static final long MIN_SAMPLE_DELAY = 5000; // 5 sec
    private static final int DISTANCE_ALARM_ID = 70;
    private static final float MIN_DISTANCE = 0;
    private static LocationSensor instance = null;

    /**
     * Factory method to get the singleton instance.
     * 
     * @param context
     * @return instance
     */
    public static LocationSensor getInstance(Context context) {
        if (instance == null) {
            instance = new LocationSensor(context);
        }
        return instance;
    }

    /**
     * Receiver for periodic alarms to calculate distance values.
     */
    public final BroadcastReceiver distanceAlarmReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            double distance = distanceEstimator.getTraveledDistance();

            notifySubscribers();
            SensorDataPoint dataPoint = new SensorDataPoint(distance);
            dataPoint.sensorName = SensorNames.TRAVELED_DISTANCE_1H;
            dataPoint.sensorDescription = SensorNames.TRAVELED_DISTANCE_1H;
            dataPoint.timeStamp = SNTP.getInstance().getTime();
            sendToSubscribers(dataPoint);

            // pass message to the MsgHandler
            Intent i = new Intent(context.getString(R.string.action_sense_new_data));
            i.putExtra(DataPoint.SENSOR_NAME, SensorNames.TRAVELED_DISTANCE_1H);
            i.putExtra(DataPoint.VALUE, (float) distance);
            i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.FLOAT);
            i.putExtra(DataPoint.TIMESTAMP, dataPoint.timeStamp);
            i.setPackage(context.getPackageName());
            context.startService(i);

            // start counting again, from the last location
            distanceEstimator.reset();
        }
    };

    private Controller controller;
    private Context context;
    private LocationManager locMgr;
    private final MyLocationListener gpsListener;
    private final MyLocationListener nwListener;
    private final MyLocationListener pasListener;
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
    private boolean active;
    private TraveledDistanceEstimator distanceEstimator;
    private PeriodicPollAlarmReceiver pollAlarmReceiver;

    /**
     * Constructor.
     * 
     * @param context
     */
    protected LocationSensor(Context context) {
        this.context = context;
        pollAlarmReceiver = new PeriodicPollAlarmReceiver(this);
        locMgr = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        controller = Controller.getController(context);
        gpsListener = new MyLocationListener();
        nwListener = new MyLocationListener();
        pasListener = new MyLocationListener();
        distanceEstimator = new TraveledDistanceEstimator();
    }

    @Override
    public void doSample() {
        controller.checkSensorSettings(isGpsAllowed, isListeningNw, isListeningGps,
                getSampleRate(), lastGpsFix, listenGpsStart, lastNwFix, listenNwStart,
                listenGpsStop, listenNwStop);
    }

    /**
     * Gets the last known location fixes from the location providers, and sends it to the location
     * listener as first data point.
     */
    private void getLastKnownLocation() {

        // get the most recent location fixes
        Location gpsFix = locMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location nwFix = locMgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        // see which provides the best recent fix
        Location bestFix = gpsFix;
        if (null != nwFix) {
            // see if network provider has better recent fix
            if (null == bestFix) {
                // gps did not provide a recent fix
                bestFix = nwFix;
            } else if (nwFix.getTime() < bestFix.getTime()
                    && nwFix.getAccuracy() < bestFix.getAccuracy() + 100) {
                // network fix is more recent and pretty accurate
                bestFix = nwFix;
            }
        }

        // send best recent fix to location listener
        if (null != bestFix) {
            // check that the fix is not too old or from the future
            long presentTime = SNTP.getInstance().getTime();
            long oldTime = presentTime - 1000 * 60 * 5;
            if (bestFix.getTime() > oldTime && bestFix.getTime() < presentTime) {
                // use last known location as first sensor value
                gpsListener.onLocationChanged(bestFix);
            }
        }
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void notifyListeningRestarted(String msg) {
        // not used
        // TODO: clean up
    }

    public void notifyListeningStopped(String msg) {
        // not used
        // TODO: clean up
    }

    public void setGpsListening(boolean listen) {
        if (listen) {
            locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, getSampleRate(),
                    MIN_DISTANCE, gpsListener);
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
        if (listen/* && isNetworkAllowed */) {
            try {
                locMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, getSampleRate(),
                        MIN_DISTANCE, nwListener);
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
    @TargetApi(Build.VERSION_CODES.FROYO)
    private void setPassiveListening(boolean listen) {
        if (listen) {
            locMgr.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, getSampleRate(),
                    MIN_DISTANCE, pasListener);
        } else {
            locMgr.removeUpdates(pasListener);
        }
    }

    @Override
    public void setSampleRate(long sampleDelay) {
        super.setSampleRate(sampleDelay);
        stopPolling();
        startPolling();
    }

    private void startAlarms() {

        // register to receive the alarm
        context.getApplicationContext().registerReceiver(distanceAlarmReceiver,
                new IntentFilter(DISTANCE_ALARM_ACTION));

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

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

    /**
     * Registers for updates from the location providers.
     */
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

    /**
     * Registers for periodic polling using the scheduler.
     */
    private void startPolling() {
        // Log.v(TAG, "start polling");
        pollAlarmReceiver.start(context);
    }

    @Override
    public void startSensing(long sampleDelay) {
        setSampleRate(sampleDelay);
        active = true;

        startListening();
        startAlarms();
        getLastKnownLocation();
    }

    private void stopAlarms() {
        // unregister the receiver
        try {
            context.unregisterReceiver(distanceAlarmReceiver);
        } catch (IllegalArgumentException e) {
            // do nothing
        }

        // stop the alarm
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent distanceAlarm = new Intent(DISTANCE_ALARM_ACTION);
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

    private void stopPolling() {
        // Log.v(TAG, "stop polling");
        pollAlarmReceiver.stop(context);
    }

    @Override
    public void stopSensing() {
        active = false;
        stopListening();
        stopAlarms();
        stopPolling();
    }
}
