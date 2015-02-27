/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.location;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.PeriodicPollAlarmReceiver;
import nl.sense_os.service.shared.PeriodicPollingSensor;
import nl.sense_os.service.shared.SensorDataPoint;
import nl.sense_os.service.subscription.BaseSensor;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

/**
 * Represents the fused location sensor. 
 * Used the Google Play Services FusedLocationProvider to receive location updates.<br/>
 * <br/>
 * Generates data for the following sensors:
 * <ul>
 * <li>position</li>
 * <li>traveled distance 1h</li>
 * </ul>
 * 
 * @author Ted Schmidt <ted@sense-os.nl>
 */
public class FusedLocationSensor extends BaseSensor implements PeriodicPollingSensor, com.google.android.gms.location.LocationListener, ConnectionCallbacks, OnConnectionFailedListener {

    private static final String TAG = "Sense FusedLocationSensor";
    private static final String DISTANCE_ALARM_ACTION = "nl.sense_os.service.LocationAlarm.distanceAlarm";
    private static final int DISTANCE_ALARM_ID = 70;
    private static FusedLocationSensor instance = null;
    private Context context;
    private GoogleApiClient googleApiClient;
    private boolean active;
    private TraveledDistanceEstimator distanceEstimator;
    private PeriodicPollAlarmReceiver pollAlarmReceiver;

    /**
     * Constructor.
     * 
     * @param context an Android Context object
     * @param googleApiClient a connected GoogleApiClient
     */
    protected FusedLocationSensor(Context context) {
        this.context = context;
        pollAlarmReceiver = new PeriodicPollAlarmReceiver(this);
        distanceEstimator = new TraveledDistanceEstimator();
        initGoogleApiClient();
    }

    private void initGoogleApiClient()
    {
        googleApiClient = new GoogleApiClient.Builder(context)
        .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        .addApi(LocationServices.API)
        .build();
    }

    @Override
    public void onConnectionFailed(ConnectionResult arg0)
    {
        Log.e(TAG, "Failed to connect to Google Api"+ arg0.toString());
    }

    @Override
    public void onConnected(Bundle arg0)
    {
        // if active start the location updates
        if(active)
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, createLocationRequest(), this);
    }

    @Override
    public void onConnectionSuspended(int arg0)
    {
        Log.e(TAG, "Connection to Google Api is suspended");
    }


    /**
     * Factory method to get the singleton instance.
     * 
     * @param context
     * @return instance
     */
    public static FusedLocationSensor getInstance(Context context) {
        if (instance == null) {
            instance = new FusedLocationSensor(context);
        }
        return instance;
    }

    /**
     * Set the location request
     * The interval of the location updates are set to the sample rate.<br>
     * The fastest interval that is allowed is set the half of the sample rate.<br>
     * The priority (battery vs accuracy) is selected based on the main location preferences:<br>
     * SensePrefs.Main.Location.FUSED_PROVIDER_ACCURATE,<br>
     * SensePrefs.Main.Location.FUSED_PROVIDER_BALANCED,<br>
     * SensePrefs.Main.Location.FUSED_PROVIDER_LOW_POWER.
     * @return The location Request object based on the current settings for requesting location updates.
     */
    protected LocationRequest createLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(getSampleRate());
        locationRequest.setFastestInterval(getSampleRate()/2);
        SharedPreferences mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
        String priority = mainPrefs.getString(SensePrefs.Main.Location.FUSED_PROVIDER_PRIORITY, SensePrefs.Main.Location.FusedProviderPriority.BALANCED);
        if(priority.equals(SensePrefs.Main.Location.FusedProviderPriority.LOW_POWER))
            locationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        else if(priority.equals(SensePrefs.Main.Location.FusedProviderPriority.ACCURATE))
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        else
            locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        return locationRequest;
    }

    @Override
    public void onLocationChanged(Location fix)
    {
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
        context.startService(i);
        distanceEstimator.addPoint(fix);
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
            context.startService(i);

            // start counting again, from the last location
            distanceEstimator.reset();
        }
    };

    @Override
    public void doSample() {
        // check if there is still a connection to the Google Play Services
        if(active && googleApiClient != null && !googleApiClient.isConnected() && !googleApiClient.isConnecting())
            googleApiClient.connect();
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setSampleRate(long sampleDelay) {
        super.setSampleRate(sampleDelay);
        stopPolling();
        startPolling();
    }

    private void startAlarms() {
        // register to receive the alarm
        context.getApplicationContext().registerReceiver(distanceAlarmReceiver, new IntentFilter(DISTANCE_ALARM_ACTION));

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // start periodic for distance
        Intent distanceAlarm = new Intent(DISTANCE_ALARM_ACTION);
        PendingIntent operation2 = PendingIntent.getBroadcast(context, DISTANCE_ALARM_ID, distanceAlarm, 0);
        am.cancel(operation2);
        am.setRepeating(AlarmManager.RTC_WAKEUP, SNTP.getInstance().getTime(), 60L * 60 * 1000, operation2);
    }

    /**
     * Registers for periodic polling using the scheduler.
     */
    private void startPolling() {
        pollAlarmReceiver.start(context);
    }

    @Override
    public void startSensing(long sampleDelay) {
        setSampleRate(sampleDelay);
        active = true;
        if(!googleApiClient.isConnected() && !googleApiClient.isConnecting())
            googleApiClient.connect();
        startAlarms();
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
        PendingIntent operation2 = PendingIntent.getBroadcast(context, DISTANCE_ALARM_ID, distanceAlarm, 0);
        am.cancel(operation2);
    }


    private void stopPolling() {
        pollAlarmReceiver.stop(context);
    }

    @Override
    public void stopSensing() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        if(googleApiClient.isConnected())
            googleApiClient.disconnect();

        active = false;
        stopAlarms();
        stopPolling();
    }
}
