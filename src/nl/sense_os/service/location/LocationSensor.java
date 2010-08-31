package nl.sense_os.service.location;

import java.util.HashMap;
import java.util.Map;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;

import nl.sense_os.service.MsgHandler;

public class LocationSensor implements LocationListener {
    private static final String TAG = "Sense LocationSensor";
    private final MsgHandler handler;
 
    
    public LocationSensor(MsgHandler handler) {
        this.handler = handler;       	
    }
   
    public void onLocationChanged(Location fix) {   
    	Map<String, Object> data = new HashMap<String, Object>();
        data.put("latitude", fix.getLatitude());
        data.put("longitude", fix.getLongitude());
        if (fix.hasAccuracy())
            data.put("accuracy", fix.getAccuracy());
        if (fix.hasAltitude())
            data.put("altitude", fix.getAltitude());
        if (fix.hasSpeed())
            data.put("speed", fix.getSpeed());
    	
        this.handler.sendSensorData("position", data);
    }

    public void onProviderDisabled(String provider) {
        Log.v(TAG, "Provider " + provider + " disabled");
    }

    public void onProviderEnabled(String provider) {
        Log.v(TAG, "Provider " + provider + " enabled");
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        switch (status) {
        case LocationProvider.AVAILABLE:
            Log.v(TAG, "Provider " + provider + " is now AVAILABLE");
        case LocationProvider.OUT_OF_SERVICE:
            Log.v(TAG, "Provider " + provider + " is now OUT OF SERVICE");
        case LocationProvider.TEMPORARILY_UNAVAILABLE:
            Log.v(TAG, "Provider " + provider + " is now TEMPORARILY UNAVAILABLE");
        }
    }
}
