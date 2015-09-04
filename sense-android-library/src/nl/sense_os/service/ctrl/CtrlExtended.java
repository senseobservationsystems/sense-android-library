package nl.sense_os.service.ctrl;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.location.LocationSensor;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.storage.LocalStorage;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.util.Log;

/**
 * This class implements a controller that aims to reduce energy consumption by monitoring the state
 * (mode) of different sensors and, depending on these states, switching between different location
 * providers or cutting down the transmission rate.
 */
public class CtrlExtended extends Controller {

    private enum Flag {
        NOT_READY, READY, RECHECK;
    }

    private static final String GPS = "gps";
    private static final String IDLE = "idle";
    private static final String NETWORK = "network";
    private static final String NOMODE = "nomode";
    private static final String TAG = "CtrlExtended";

    private float mAccuracyPref = 0;
    private String mBestProvider;
    private Context mContext;
    private float mGpsAvgAcc = 35;
    private int mGpsIndexAcc = 0;
    private Flag mGpsIndexFlag;
    private float[] mGpsTableAcc = { 0, 0, 0, 0, 0 };
    private long mLastChangedTime;
    private String mLastLightMode = NOMODE;
    private String mLastLocationMode = NOMODE;
    private long mLastLoudTime;
    private String mLastNoiseMode = NOMODE;
    private float mNetworkAvgAcc = 25;
    private int mNetworkIndexAcc = 0;
    private Flag mNetworkIndexFlag;
    private float[] mNetworkTableAcc = { 0, 0, 0, 0, 0 };
    private Flag mPositionFlag;
    private double mPreviousDecibel = 0;
    private float mPreviousLux = 0;
    private int mReadyToGo = 0;
    private long mStartTime;

    public CtrlExtended(Context context) {
        super(context);
        Log.i(TAG, "Creating extended controller");
        mContext = context;
    }

    private String bestProvider(boolean isGpsAllowed, boolean isListeningGps, long time,
            long listenGpsStart, boolean isListeningNw, long listenNwStart, Location lastGpsFix,
            Location lastNwFix) {
        // Log.v(TAG, "BEST PROVIDER CALLED");

        if (isGpsAllowed == false) {
            mGpsIndexAcc = 0;
            mGpsIndexFlag = Flag.READY;
            mNetworkIndexAcc = 0;
            mNetworkIndexFlag = Flag.READY;
            return NETWORK;
        } else if (!isGpsProductive(isListeningGps, time, lastGpsFix, listenGpsStart)
                && (mNetworkIndexAcc == 5)) {
            mGpsIndexAcc = 0;
            mGpsIndexFlag = Flag.READY;
            mNetworkIndexAcc = 0;
            mNetworkIndexFlag = Flag.READY;
            return NETWORK;
        } else if (!isNwProductive(isListeningNw, time, lastNwFix, listenNwStart)
                && (mGpsIndexAcc == 5)) {
            mGpsIndexAcc = 0;
            mGpsIndexFlag = Flag.READY;
            mNetworkIndexAcc = 0;
            mNetworkIndexFlag = Flag.READY;
            return GPS;
        } else {
            if (lastGpsFix != null) {
                mGpsTableAcc[mGpsIndexAcc] = lastGpsFix.getAccuracy();
                mGpsIndexAcc++;
                if (mGpsIndexAcc == 5) {
                    mGpsIndexAcc = 0;
                    mGpsIndexFlag = Flag.READY;
                }
                float tempSum = 0;
                for (int i = 0; i < 5; i++)
                    tempSum = tempSum + mGpsTableAcc[i];
                mGpsAvgAcc = tempSum / 5;
            }
            if (lastNwFix != null) {
                mNetworkTableAcc[mNetworkIndexAcc] = lastNwFix.getAccuracy();
                mNetworkIndexAcc++;
                if (mNetworkIndexAcc == 5) {
                    mNetworkIndexAcc = 0;
                    mNetworkIndexFlag = Flag.READY;
                }
                float tempSum = 0;
                for (int i = 0; i < 5; i++)
                    tempSum = tempSum + mNetworkTableAcc[i];
                mNetworkAvgAcc = tempSum / 5;
            }

            float gpsPrefDiff = mGpsAvgAcc - mAccuracyPref;
            float networkPrefDiff = mNetworkAvgAcc - mAccuracyPref;
            // Use abs()
            if ((gpsPrefDiff < 0) && (networkPrefDiff > 0))
                return GPS;
            else if ((gpsPrefDiff > 0) && (networkPrefDiff < 0))
                return NETWORK;
            else if ((gpsPrefDiff > 0) && (networkPrefDiff > 0))
                if (gpsPrefDiff < networkPrefDiff)
                    return GPS;
                else
                    return NETWORK;
            else if (gpsPrefDiff > networkPrefDiff)
                return GPS;
            else
                return NETWORK;
        }
    }

    public void checkLightSensor(float value) {

        if (((value - mPreviousLux) > 10) || ((value - mPreviousLux) < -10)) {
            mLastLightMode = "changed";
            mLastChangedTime = SNTP.getInstance().getTime();
            // scheduleTransmissions();
        } else {
            if ("changed".equals(mLastLightMode)) {
                double timetowait = SNTP.getInstance().getTime() - mLastChangedTime;
                if (timetowait > 5 * 60 * 1000) {
                    mLastLightMode = "idle";
                    // Data-Transmitter to idle?
                    if ("idle".equals(getLastMode())) {
                        // scheduleTransmissions();
                    }
                } else {
                    mLastLightMode = "changed";
                }

            } else {
                mLastLightMode = "idle";
                // Data-Transmitter to idle?
                if ("idle".equals(getLastMode())) {
                    // scheduleTransmissions();
                }
            }
        }
        mPreviousLux = value;

    }

    public void checkNoiseSensor(double dB) {

        if (((dB - mPreviousDecibel) > 10) || ((dB - mPreviousDecibel) < -10) || (dB > 63)) {
            mLastNoiseMode = "loud";
            mLastLoudTime = SNTP.getInstance().getTime();
            // scheduleTransmissions();
        } else {
            if ("loud".equals(mLastNoiseMode)) {
                double timetowait = SNTP.getInstance().getTime() - mLastLoudTime;
                if (timetowait > 5 * 60 * 1000) {
                    mLastNoiseMode = "idle";
                    // Data-Transmitter to idle?
                    if ("idle".equals(getLastMode())) {
                        // scheduleTransmissions();
                    }
                } else {
                    mLastNoiseMode = "loud";
                }

            } else {
                mLastNoiseMode = "idle";
                // Data-Transmitter to idle?
                if ("idle".equals(getLastMode())) {
                    // scheduleTransmissions();
                }
            }
        }
        mPreviousDecibel = dB;

    }

    /**
     * If the phone is in a standby position, both gps and network providers are turned off,
     * otherwise we listen to the Network provider and switch to Gps provider only if the first one
     * is not productive.
     */
    public void checkSensorSettings(boolean isGpsAllowed, boolean isListeningNw,
            boolean isListeningGps, long time, Location lastGpsFix, long listenGpsStart,
            Location lastNwFix, long listenNwStart, long listenGpsStop, long listenNwStop) {

        Log.v(TAG, "Check location sensor settings...");
        LocationSensor locListener = LocationSensor.getInstance(mContext);

        if (mLastLocationMode.equals(NOMODE)) {

            // Log.d(TAG, "NO MODE");
            mBestProvider = bestProvider(isGpsAllowed, isListeningGps, time, listenGpsStart,
                    isListeningNw, listenNwStart, lastGpsFix, lastNwFix);

            if (((mGpsIndexFlag == Flag.NOT_READY) || (mNetworkIndexFlag == Flag.NOT_READY))) {
                // remain in no mode
            } else {
                mLastLocationMode = IDLE;
                mGpsIndexAcc = 0;
                mNetworkIndexAcc = 0;
                mGpsIndexFlag = Flag.NOT_READY;
                mNetworkIndexFlag = Flag.NOT_READY;
            }

            if (!isListeningNw) {
                locListener.setNetworkListening(true);
            }
            if (!isListeningGps && (isGpsAllowed == true)) {
                locListener.setGpsListening(true);
            }
        } else if ((!isAccelerating()) && (!isPositionChanged(0))) {

            // Log.d(TAG, "IDLE");
            mLastLocationMode = IDLE;
            mGpsIndexAcc = 0;
            mNetworkIndexAcc = 0;
            mGpsIndexFlag = Flag.NOT_READY;
            mNetworkIndexFlag = Flag.NOT_READY;
            mReadyToGo = 0;
            if (isListeningNw) {
                locListener.setNetworkListening(false);
            }
            if (isListeningGps) {
                locListener.setGpsListening(false);
            }
            // Data-Transmitter to idle?
            if (IDLE.equals(getLastMode())) {
                // scheduleTransmissions();
            }

        } else {
            if ((isGpsAllowed == true)
                    && (isPositionChanged(200)
                            || isNwSwitchedOffTooLong(isListeningNw, listenNwStop)
                            || isGpsSwitchedOffTooLong(isListeningNw, listenNwStop)
                            || (mGpsIndexFlag == Flag.RECHECK) || (mNetworkIndexFlag == Flag.RECHECK))) {
                /*
                 * if (locListener.time != 30 * 1000) { locSampleRate(30 * 1000); }
                 */
                if (!isListeningNw) {
                    locListener.setNetworkListening(true);
                }
                if (!isListeningGps) {
                    locListener.setGpsListening(true);
                }
                mGpsIndexFlag = Flag.RECHECK;
                mNetworkIndexFlag = Flag.RECHECK;
                String temp = bestProvider(isGpsAllowed, isListeningGps, time, listenGpsStart,
                        isListeningNw, listenNwStart, lastGpsFix, lastNwFix);
                // Log.d(TAG, "FLAG GPS" + gpsIndexFlag + "FLAG NW" + networkIndexFlag);
                if ((mGpsIndexFlag == Flag.READY) || (mNetworkIndexFlag == Flag.READY))
                    mReadyToGo++;
                if (mReadyToGo == 2) {
                    mBestProvider = temp;
                    mGpsIndexAcc = 0;
                    mNetworkIndexAcc = 0;
                    mGpsIndexFlag = Flag.NOT_READY;
                    mNetworkIndexFlag = Flag.NOT_READY;
                    mReadyToGo = 0;
                }
            }
            if (mBestProvider.equals(NETWORK)) {
                /*
                 * if (locListener.time != 30 * 1000) { locSampleRate(30 * 1000); }
                 */
                // Log.d(TAG, "NETWORK MODE");
                mLastLocationMode = NETWORK;
                if (!isListeningNw) {
                    locListener.setNetworkListening(true);
                }
                if (isGpsAllowed == true) {
                    if (!isNwProductive(isListeningNw, time, lastNwFix, listenNwStart)) {
                        locListener.setGpsListening(true);
                    }
                    // gpsIndexFlag == 2 means that we currently sample for the average gps accuracy
                    else if (isListeningGps && (mGpsIndexFlag != Flag.RECHECK)) {
                        locListener.setGpsListening(false);
                    }
                }

            } else if ((mBestProvider.equals(GPS) && isGpsProductive(isListeningGps, time,
                    lastGpsFix, listenGpsStart))
                    || isGpsSwitchedOffTooLong(isListeningGps, listenGpsStop)) {

                // Log.d(TAG, "GPS MODE");
                mLastLocationMode = GPS;
                if (!isListeningGps) {
                    locListener.setGpsListening(true);
                }
                if (!isGpsProductive(isListeningGps, time, lastGpsFix, listenGpsStart)) {
                    locListener.setNetworkListening(true);
                }
                // networkIndexFlag == 2 means that we currently sample for the average network
                // accuracy
                else if (isListeningNw && (mNetworkIndexFlag != Flag.RECHECK)) {
                    locListener.setNetworkListening(false);
                }
            }
        }
    }

    /**
     * Returns the general mode
     * 
     * @return idle if all of the three sensors are in idle mode
     */
    private String getLastMode() {
        if (mLastLocationMode.equals("idle") && mLastLightMode.equals("idle")
                && mLastNoiseMode.equals("idle"))
            return "idle";
        else
            return "normal";
    }

    /**
     * Uses the accelerometer instead of the linear acceleration
     */
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

            String selection = DataPoint.SENSOR_NAME + "='" + SensorNames.ACCELEROMETER + "'"
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
            String value = data.getString(data.getColumnIndex(DataPoint.VALUE));
            JSONObject json = new JSONObject(value);
            double x1 = json.getDouble("x-axis");
            double y1 = json.getDouble("y-axis");
            double z1 = json.getDouble("z-axis");
            data.moveToNext();
            while (!data.isAfterLast()) {
                value = data.getString(data.getColumnIndex(DataPoint.VALUE));
                json = new JSONObject(value);
                double x2 = json.getDouble("x-axis");
                double y2 = json.getDouble("y-axis");
                double z2 = json.getDouble("z-axis");
                double motion = Math.abs(x2 - x1) + Math.abs(y2 - y1) + Math.abs(z2 - z1);
                x1 = x2;
                y1 = y2;
                z1 = z2;
                totalMotion += motion;
                data.moveToNext();
            }
            double avgMotion = totalMotion / (data.getCount() - 1);

            if (avgMotion > 1.2) {
                // device is moving
                mPositionFlag = Flag.NOT_READY;
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

        } else if (mLastLocationMode.equals(NETWORK)) {
            productive = true;
        } else {
            // not enabled
        }

        return productive;
    }

    private boolean isGpsSwitchedOffTooLong(boolean isListeningGps, long listenGpsStop) {

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
            // Log.d(TAG, "GPS has been turned off for a long time, or was never even started");
        } else if (!(locationMgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER))) {
            // the network provider is disabled: GPS is the only option
            tooLong = true;
            // Log.d(TAG, " the network provider is disabled: GPS is the only option");
        } else {
            tooLong = false;
        }

        return tooLong;
    }

    private boolean isNwProductive(boolean isListeningNw, long time, Location lastNwFix,
            long listenNwStart) {

        boolean productive = isListeningNw;
        long maxDelay = 2 * time;
        if (isListeningNw) {
            // check if any updates have been received recently from the Network sensor
            if (lastNwFix != null && lastNwFix.getTime() > listenNwStart) {
                if (SNTP.getInstance().getTime() - lastNwFix.getTime() > maxDelay) {
                    // no updates for long time
                    productive = false;
                }
            } else if (SNTP.getInstance().getTime() - listenNwStart > maxDelay) {
                // no updates for a long time
                productive = false;
            } else {
                // Network is productive
            }

        } else if (mLastLocationMode.equals("idle") || mLastLocationMode.equals("noavailable")) {
            productive = true;
        } else {
            // not enabled
        }

        return productive;
    }

    private boolean isNwSwitchedOffTooLong(boolean isListeningNw, long listenNwStop) {

        if (isListeningNw) {
            // no use checking if Network is switched off too long: it is still listening!
            return false;
        }

        boolean tooLong = false;
        long maxDelay = 1000 * 60 * 60; // 1 hour

        LocationManager locationMgr = (LocationManager) mContext
                .getSystemService(Context.LOCATION_SERVICE);
        if (SNTP.getInstance().getTime() - listenNwStop > maxDelay) {
            // Network has been turned off for a long time, or was never even started
            tooLong = true;
            // Log.d(TAG, "Network has been turned off for a long time, or was never even started");
        } else if (!(locationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER))) {
            // the GPS provider is disabled: Network is the only option
            tooLong = true;
            // Log.d(TAG, " the gps provider is disabled: NETWORK is the only option");
        } else {
            tooLong = false;
        }

        return tooLong;
    }

    private boolean isPositionChanged(int distanceTraveled) {
        Log.v(TAG, "Check if position changed recently");

        boolean moved = true;
        Cursor data = null;
        if (mPositionFlag == Flag.NOT_READY && (distanceTraveled == 0)) {
            // Log.d(TAG, "1A");
            mStartTime = SNTP.getInstance().getTime();
            mPositionFlag = Flag.READY;
            return moved;
        } else if ((SNTP.getInstance().getTime() - mStartTime) >= 4 * 60 * 1000
                || (distanceTraveled != 0)) {

            try {
                // Log.d(TAG, "1B");
                // get location data from time since the last check
                long timerange = 1000 * 60 * 15; // 15 minutes
                Uri uri = Uri.parse("content://"
                        + mContext.getString(R.string.local_storage_authority)
                        + DataPoint.CONTENT_URI_PATH);
                String[] projection = new String[] { DataPoint.SENSOR_NAME, DataPoint.TIMESTAMP,
                        DataPoint.VALUE };
                String selection = DataPoint.SENSOR_NAME + "='" + SensorNames.LOCATION + "'"
                        + " AND " + DataPoint.TIMESTAMP + ">"
                        + (SNTP.getInstance().getTime() - timerange);
                data = LocalStorage.getInstance(mContext).query(uri, projection, selection, null,
                        null);

                if (null == data || data.getCount() < 2) {
                    // no position changes: assume the device is moving
                    // Log.d(TAG, "no position changes: assume the device is moving!");
                    return false;
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
                JSONObject endJson = new JSONObject(data.getString(data
                        .getColumnIndex(DataPoint.VALUE)));
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

                if (distance > accuracy + distanceTraveled) {
                    // Log.v(TAG, "Position has changed");
                    moved = true;
                } else {
                    // position did NOT change
                    moved = false;
                }

            } catch (JSONException e) {
                Log.e(TAG, "Exception parsing location data: ", e);
                // moved = true;
            } finally {
                if (null != data) {
                    data.close();
                }
            }

            return moved;
        } else {
            // Log.d(TAG, "1C");
            return true;
        }
    }
}
