/*
 ************************************************************************************************************
 *     Copyright (C)  2010 Sense Observation Systems, Rotterdam, the Netherlands.  All rights reserved.     *
 ************************************************************************************************************
 */
package nl.sense_os.service.location;

import nl.sense_os.service.Constants;
import nl.sense_os.service.MsgHandler;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

public class LocationSensor implements LocationListener {
    private static final String TAG = "Sense LocationSensor";
    private static final String NAME = "position";
    private Context context;

    public LocationSensor(Context context) {
        this.context = context;
    }

    @Override
    public void onLocationChanged(Location fix) {
        JSONObject json = new JSONObject();
        try {
            json.put("latitude", fix.getLatitude());
            json.put("longitude", fix.getLongitude());

            // always include all JSON fields, otherwise we get a problem posting data with varying
            // data_structure
            json.put("accuracy", fix.hasAccuracy() ? fix.getAccuracy() : -1.0f);
            json.put("altitude", fix.hasAltitude() ? fix.getAltitude() : -1.0d);
            json.put("speed", fix.hasSpeed() ? fix.getSpeed() : -1.0f);
            json.put("bearing", fix.hasBearing() ? fix.getBearing() : -1.0f);
            json.put("provider", null != fix.getProvider() ? fix.getProvider() : "unknown");
        } catch (JSONException e) {
            Log.e(TAG, "JSONException in onLocationChanged", e);
            return;
        }

        // pass message to the MsgHandler
        Intent i = new Intent(MsgHandler.ACTION_NEW_MSG);
        i.putExtra(MsgHandler.KEY_SENSOR_NAME, NAME);
        i.putExtra(MsgHandler.KEY_VALUE, json.toString());
        i.putExtra(MsgHandler.KEY_DATA_TYPE, Constants.SENSOR_DATA_TYPE_JSON);
        i.putExtra(MsgHandler.KEY_TIMESTAMP, System.currentTimeMillis());
        this.context.startService(i);
    }

    @Override
    public void onProviderDisabled(String provider) {
        // Log.v(TAG, "Provider " + provider + " disabled");
    }

    @Override
    public void onProviderEnabled(String provider) {
        // Log.v(TAG, "Provider " + provider + " enabled");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // switch (status) {
        // case LocationProvider.AVAILABLE:
        // Log.v(TAG, "Provider " + provider + " is now AVAILABLE");
        // case LocationProvider.OUT_OF_SERVICE:
        // Log.v(TAG, "Provider " + provider + " is now OUT OF SERVICE");
        // case LocationProvider.TEMPORARILY_UNAVAILABLE:
        // Log.v(TAG, "Provider " + provider + " is now TEMPORARILY UNAVAILABLE");
        // }
    }
}
