package nl.sense_os.service.location;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

import nl.sense_os.app.SenseSettings;
import nl.sense_os.service.MsgHandler;

import org.json.JSONException;
import org.json.JSONObject;

public class LocationSensor implements LocationListener {
    private static final String TAG = "Sense LocationSensor";
    private static final String NAME = "position";
    private Context context;

    public LocationSensor(Context context) {
        this.context = context;
    }

    public void onLocationChanged(Location fix) {
        JSONObject json = new JSONObject();
        try {
            json.put("latitude", fix.getLatitude());
            json.put("longitude", fix.getLongitude());
            if (fix.hasAccuracy()) {
                json.put("accuracy", fix.getAccuracy());
            }
            if (fix.hasAltitude()) {
                json.put("altitude", fix.getAltitude());
            }
            if (fix.hasSpeed()) {
                json.put("speed", fix.getSpeed());
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSONException in onLocationChanged", e);
            return;
        }

        // pass message to the MsgHandler
        Intent i = new Intent(this.context, MsgHandler.class);
        i.putExtra(MsgHandler.KEY_INTENT_TYPE, MsgHandler.TYPE_NEW_MSG);
        i.putExtra(MsgHandler.KEY_SENSOR_NAME, NAME);
        i.putExtra(MsgHandler.KEY_VALUE, json.toString());
        i.putExtra(MsgHandler.KEY_DATA_TYPE, SenseSettings.SENSOR_DATA_TYPE_JSON);
        i.putExtra(MsgHandler.KEY_TIMESTAMP, System.currentTimeMillis());
        this.context.startService(i);
    }

    public void onProviderDisabled(String provider) {
        // Log.v(TAG, "Provider " + provider + " disabled");
    }

    public void onProviderEnabled(String provider) {
        // Log.v(TAG, "Provider " + provider + " enabled");
    }

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
