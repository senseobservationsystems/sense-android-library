package nl.sense_os.service.ctrl;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.location.LocationSensor;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.storage.LocalStorage;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.util.Log;

/**
 * Implements the default behavior of the Location Sensor and DataTransmitter
 */
public class CtrlDefault extends Controller {

    private static final String TAG = "CtrlDefault";

    private Context mContext;

    public CtrlDefault(Context context) {
        super(context);
        Log.i(TAG, "Creating default controller");
        mContext = context;
    }

    @Override
    public void checkLightSensor(float value) {
        // not implemented in this controller
    }

    @Override
    public void checkNoiseSensor(double dB) {
        // not implemented in this controller
    }

    @Override
    public void checkSensorSettings(boolean isGpsAllowed, boolean isListeningNw,
            boolean isListeningGps, long time, Location lastGpsFix, long listenGpsStart,
            Location lastNwFix, long listenNwStart, long listenGpsStop, long listenNwStop) {

       //Log.v(TAG, "Do sample");
        SharedPreferences mainPrefs = mContext.getSharedPreferences(SensePrefs.MAIN_PREFS,
                Context.MODE_PRIVATE);
        boolean selfAwareMode = isGpsAllowed && mainPrefs.getBoolean(Main.Location.AUTO_GPS, true);
        if (selfAwareMode) {
            Log.v(TAG, "Check location sensor settings...");
            LocationSensor locListener = LocationSensor.getInstance(mContext);
            if (isListeningGps) {

                if (!isGpsProductive(isListeningGps, time, lastGpsFix, listenGpsStart)) {
                    // switch off
                    locListener.setGpsListening(false);
                    locListener.notifyListeningStopped("not productive");

                } else {
                    // we're fine
                }

            } else {

                if (isAccelerating()) {
                    // switch on
                    locListener.setGpsListening(true);
                    locListener.notifyListeningRestarted("moved");

                } else if (isPositionChanged()) {
                    // switch on
                    locListener.setGpsListening(true);
                    locListener.notifyListeningRestarted("position changed");

                } else if (isSwitchedOffTooLong(isListeningGps, listenGpsStop)) {
                    // switch on
                    locListener.setGpsListening(true);
                    locListener.notifyListeningRestarted("timeout");

                } else {
                    // we're fine
                }
            }
        }

    }

    private boolean isAccelerating() {
        Log.v(TAG, "Check if device was accelerating recently");

        boolean moving = true;

        Cursor data = null;
        try {
            // get linear acceleration data
            long timerange = 1000 * 60 * 15; // 15 minutes
            Uri uri = Uri.parse("content://" + mContext.getString(R.string.local_storage_authority)
                    + DataPoint.CONTENT_URI_PATH);
            String[] projection = new String[] { DataPoint.SENSOR_NAME, DataPoint.TIMESTAMP,
                    DataPoint.VALUE };
            String selection = DataPoint.SENSOR_NAME + "='" + SensorNames.LIN_ACCELERATION + "'"
                    + " AND " + DataPoint.TIMESTAMP + ">"
                    + (SNTP.getInstance().getTime() - timerange);
            data = LocalStorage.getInstance(mContext).query(uri, projection, selection, null, null);

            if (null == data || data.getCount() == 0) {
                // no movement measurements: assume the device is moving
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
                // device is moving
                moving = true;
            } else {
                // device is not moving
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
    private boolean isGpsProductive(boolean isListeningGps, long time, Location lastGpsFix,
            long listenGpsStart) {

        boolean productive = isListeningGps;
        long maxDelay = 2 * time;
        if (isListeningGps) {

            // check if any updates have been received recently from the GPS sensor
            if (lastGpsFix != null && lastGpsFix.getTime() > listenGpsStart) {
                if (SNTP.getInstance().getTime() - lastGpsFix.getTime() > maxDelay) {
                    // no updates for long time
                    productive = false;
                }
            } else if (SNTP.getInstance().getTime() - listenGpsStart > maxDelay) {
                // no updates for a long time
                productive = false;
            } else {
                // GPS is productive
            }

        } else {
            // not enabled
        }

        return productive;
    }

    private boolean isPositionChanged() {
        Log.v(TAG, "Check if position changed recently");

        boolean moved = true;

        Cursor data = null;
        try {
            // get location data from time since the last check
            long timerange = 1000 * 60 * 15; // 15 minutes
            Uri uri = Uri.parse("content://" + mContext.getString(R.string.local_storage_authority)
                    + DataPoint.CONTENT_URI_PATH);
            String[] projection = new String[] { DataPoint.SENSOR_NAME, DataPoint.TIMESTAMP,
                    DataPoint.VALUE };
            String selection = DataPoint.SENSOR_NAME + "='" + SensorNames.POSITION + "'" + " AND "
                    + DataPoint.TIMESTAMP + ">" + (SNTP.getInstance().getTime() - timerange);
            data = LocalStorage.getInstance(mContext).query(uri, projection, selection, null, null);

            if (null == data || data.getCount() < 2) {
                // no position changes: assume the device is moving
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

            if (distance > accuracy) {
                // Log.v(TAG, "Position has changed");
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

    private boolean isSwitchedOffTooLong(boolean isListeningGps, long listenGpsStop) {

        if (isListeningGps) {
            // no use checking if GPS is switched off too long: it is still listening!
            return false;
        }

        boolean tooLong = false;
        long maxDelay = 1000 * 60 * 60; // 1 hour

        LocationManager locationMgr = (LocationManager) mContext
                .getSystemService(Context.LOCATION_SERVICE);
        if (SNTP.getInstance().getTime() - listenGpsStop > maxDelay) {
            // GPS has been turned off for a long time, or was never even started
            tooLong = true;
        } else if (!(locationMgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER))) {
            // the network provider is disabled: GPS is the only option
            tooLong = true;
        } else {
            tooLong = false;
        }

        return tooLong;
    }
}
