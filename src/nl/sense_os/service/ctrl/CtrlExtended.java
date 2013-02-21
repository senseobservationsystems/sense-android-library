package nl.sense_os.service.ctrl;

import nl.sense_os.service.DataTransmitter;
import nl.sense_os.service.R;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.location.LocationSensor;
import nl.sense_os.service.motion.MotionSensor;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.storage.LocalStorage;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

    private enum flag {
        NOT_READY, READY, RECHECK;
    }

    private class Intervals {
        static final long ECO = AlarmManager.INTERVAL_HALF_HOUR;
        static final long NORMAL = 1000 * 60 * 5;
        static final long OFTEN = 1000 * 60 * 1;
    }

    private static final String TAG = "Sense Controller";
    private static final String NOMODE = "nomode";
    private static final String IDLE = "idle";
    private static final String NETWORK = "network";
    private static final String GPS = "gps";

    private LocationManager locMgr;
    private Context context;
    private String lastlocationmode = NOMODE;
    private flag networkIndexFlag;
    private flag gpsIndexFlag;
    private flag positionFlag;
    private float[] networkTableAcc = { 0, 0, 0, 0, 0 };
    private float networkAvgAcc = 25;
    private int networkIndexAcc = 0;
    private float[] gpsTableAcc = { 0, 0, 0, 0, 0 };
    private float gpsAvgAcc = 35;
    private int gpsIndexAcc = 0;
    private float accuracyPref = 0;
    private String bestProvider;
    private int readyToGo = 0;
    private long startTime;
    private static float previous_lux = 0;
    private static long lastchangedtime;
    private static String lastlightmode = NOMODE;
    private static double previous_dB = 0;
    private static long lastloudtime;
    private static String lastnoisemode = NOMODE;

    public CtrlExtended(Context context) {
        super();
        this.context = context;
        locMgr = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public String bestProvider(boolean isListeningGps, long time, long listenGpsStart,
            boolean isListeningNw, long listenNwStart, Location lastGpsFix, Location lastNwFix) {
        // Log.v(TAG, "BEST PROVIDER CALLED");

        if (!isGpsProductive(isListeningGps, time, lastGpsFix, listenGpsStart)
                && (networkIndexAcc == 5)) {
            gpsIndexAcc = 0;
            gpsIndexFlag = flag.READY;
            networkIndexAcc = 0;
            networkIndexFlag = flag.READY;
            return NETWORK;
        } else if (!isNwProductive(isListeningNw, time, lastNwFix, listenNwStart)
                && (gpsIndexAcc == 5)) {
            gpsIndexAcc = 0;
            gpsIndexFlag = flag.READY;
            networkIndexAcc = 0;
            networkIndexFlag = flag.READY;
            return GPS;
        } else {
            if (lastGpsFix != null) {
                gpsTableAcc[gpsIndexAcc] = lastGpsFix.getAccuracy();
                gpsIndexAcc++;
                if (gpsIndexAcc == 5) {
                    gpsIndexAcc = 0;
                    gpsIndexFlag = flag.READY;
                }
                float tempSum = 0;
                for (int i = 0; i < 5; i++)
                    tempSum = tempSum + gpsTableAcc[i];
                gpsAvgAcc = tempSum / 5;
            }
            if (lastNwFix != null) {
                networkTableAcc[networkIndexAcc] = lastNwFix.getAccuracy();
                networkIndexAcc++;
                if (networkIndexAcc == 5) {
                    networkIndexAcc = 0;
                    networkIndexFlag = flag.READY;
                }
                float tempSum = 0;
                for (int i = 0; i < 5; i++)
                    tempSum = tempSum + networkTableAcc[i];
                networkAvgAcc = tempSum / 5;
            }

            float gpsPrefDiff = gpsAvgAcc - accuracyPref;
            float networkPrefDiff = networkAvgAcc - accuracyPref;
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

        if (((value - previous_lux) > 10) || ((value - previous_lux) < -10)) {
            lastlightmode = "changed";
            lastchangedtime = SNTP.getInstance().getTime();
            // scheduleTransmissions();
        } else {
            if ("changed".equals(lastlightmode)) {
                double timetowait = SNTP.getInstance().getTime() - lastchangedtime;
                if (timetowait > 5 * 60 * 1000) {
                    lastlightmode = "idle";
                    // Data-Transmitter to idle?
                    if ("idle".equals(getLastMode())) {
                        // scheduleTransmissions();
                    }
                } else {
                    lastlightmode = "changed";
                }

            } else {
                lastlightmode = "idle";
                // Data-Transmitter to idle?
                if ("idle".equals(getLastMode())) {
                    // scheduleTransmissions();
                }
            }
        }
        previous_lux = value;

    }

    public void checkNoiseSensor(double dB) {

        if (((dB - previous_dB) > 10) || ((dB - previous_dB) < -10) || (dB > 63)) {
            lastnoisemode = "loud";
            lastloudtime = SNTP.getInstance().getTime();
            // scheduleTransmissions();
        } else {
            if ("loud".equals(lastnoisemode)) {
                double timetowait = SNTP.getInstance().getTime() - lastloudtime;
                if (timetowait > 5 * 60 * 1000) {
                    lastnoisemode = "idle";
                    // Data-Transmitter to idle?
                    if ("idle".equals(getLastMode())) {
                        // scheduleTransmissions();
                    }
                } else {
                    lastnoisemode = "loud";
                }

            } else {
                lastnoisemode = "idle";
                // Data-Transmitter to idle?
                if ("idle".equals(getLastMode())) {
                    // scheduleTransmissions();
                }
            }
        }
        previous_dB = dB;

    }

    /**
     * If the phone is in a standby position, both gps and network providers are turned off,
     * otherwise we listen to the Network provider and switch to Gps provider only if the first one
     * is not productive.
     */
    public void checkSensorSettings(boolean isGpsAllowed, boolean isListeningNw,
            boolean isListeningGps, long time, Location lastGpsFix, long listenGpsStart,
            Location lastNwFix, long listenNwStart, long listenGpsStop, long listenNwStop) {

        LocationSensor locListener = LocationSensor.getInstance(context);

        if (lastlocationmode.equals(NOMODE)) {

            // Log.v(TAG, "NO MODE");
            for (int i = 0; i < 5; i++) {
                // Log.d(TAG, "Table Nw" + networkTableAcc[i]);
            }
            for (int i = 0; i < 5; i++) {
                Log.d(TAG, "Table Gps" + gpsTableAcc[i]);
            }
            /*
             * if (locListener.time != 30 * 1000) { locSampleRate(30 * 1000); }
             */
            bestProvider = bestProvider(isListeningGps, time, listenGpsStart, isListeningNw,
                    listenNwStart, lastGpsFix, lastNwFix);

            if (((gpsIndexFlag == flag.NOT_READY) || (networkIndexFlag == flag.NOT_READY))) {
                // remain in no mode
            } else {
                lastlocationmode = IDLE;
                gpsIndexAcc = 0;
                networkIndexAcc = 0;
                gpsIndexFlag = flag.NOT_READY;
                networkIndexFlag = flag.NOT_READY;
            }

            if (!isListeningNw) {
                locListener.setNetworkListening(true);
            }
            if (!isListeningGps) {
                locListener.setGpsListening(true);
            }
        } else if ((!isAccelerating()) && (!isPositionChanged(0))) {
            /*
             * if (motion.sampleDelay != 1000) { motionSampleRate(1000); }
             */
            // Log.v(TAG, "IDLE");
            /*
             * if (locListener.time != 60 * 1000) { locSampleRate(60 * 1000); }
             */
            lastlocationmode = IDLE;
            gpsIndexAcc = 0;
            networkIndexAcc = 0;
            gpsIndexFlag = flag.NOT_READY;
            networkIndexFlag = flag.NOT_READY;
            readyToGo = 0;
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
            if (isPositionChanged(200) || NwisSwitchedOffTooLong(isListeningNw, listenNwStop)
                    || isSwitchedOffTooLong(isListeningNw, listenNwStop)
                    || (gpsIndexFlag == flag.RECHECK) || (networkIndexFlag == flag.RECHECK)) {
                /*
                 * if (locListener.time != 30 * 1000) { locSampleRate(30 * 1000); }
                 */
                if (!isListeningNw) {
                    locListener.setNetworkListening(true);
                }
                if (!isListeningGps) {
                    locListener.setGpsListening(true);
                }
                gpsIndexFlag = flag.RECHECK;
                networkIndexFlag = flag.RECHECK;
                String temp = bestProvider(isListeningGps, time, listenGpsStart, isListeningNw,
                        listenNwStart, lastGpsFix, lastNwFix);
                // Log.d(TAG, "FLAG GPS" + gpsIndexFlag + "FLAG NW" + networkIndexFlag);
                if ((gpsIndexFlag == flag.READY) || (networkIndexFlag == flag.READY))
                    readyToGo++;
                if (readyToGo == 2) {
                    bestProvider = temp;
                    gpsIndexAcc = 0;
                    networkIndexAcc = 0;
                    gpsIndexFlag = flag.NOT_READY;
                    networkIndexFlag = flag.NOT_READY;
                    readyToGo = 0;
                }
            }
            if (bestProvider.equals(NETWORK)) {
                /*
                 * if (locListener.time != 30 * 1000) { locSampleRate(30 * 1000); }
                 */
                // Log.v(TAG, "NETWORK MODE");
                for (int i = 0; i < 5; i++) {
                    // Log.d(TAG, "Table Nw" + networkTableAcc[i]);
                }
                for (int i = 0; i < 5; i++) {
                    // Log.d(TAG, "Table Gps" + gpsTableAcc[i]);
                }
                // Log.d(TAG, "AVG Nw" + networkAvgAcc);
                // Log.d(TAG, "AVG Gps" + gpsAvgAcc);
                lastlocationmode = NETWORK;
                if (!isListeningNw) {
                    locListener.setNetworkListening(true);
                }
                if (!isNwProductive(isListeningNw, time, lastNwFix, listenNwStart)) {
                    locListener.setGpsListening(true);
                }
                // gpsIndexFlag == 2 means that we currently sample for the average gps accuracy
                else if (isListeningGps && (gpsIndexFlag != flag.RECHECK)) {
                    locListener.setGpsListening(false);
                }

            } else if ((bestProvider.equals(GPS) && isGpsProductive(isListeningGps, time,
                    lastGpsFix, listenGpsStart))
                    || isSwitchedOffTooLong(isListeningGps, listenGpsStop)) {
                /*
                 * if (locListener.time != 3 * 60 * 1000) { locSampleRate(3 * 60 * 1000); }
                 */
                // Log.v(TAG, "GPS MODE");
                for (int i = 0; i < 5; i++) {
                    // Log.d(TAG, "Table Nw" + networkTableAcc[i]);
                }
                for (int i = 0; i < 5; i++) {
                    // Log.d(TAG, "Table Gps" + gpsTableAcc[i]);
                }
                // Log.d(TAG, "AVG Nw" + networkAvgAcc);
                // Log.d(TAG, "AVG Gps" + gpsAvgAcc);
                lastlocationmode = GPS;
                if (!isListeningGps) {
                    locListener.setGpsListening(true);
                }
                if (!isGpsProductive(isListeningGps, time, lastGpsFix, listenGpsStart)) {
                    locListener.setNetworkListening(true);
                }
                // networkIndexFlag == 2 means that we currently sample for the average network
                // accuracy
                else if (isListeningNw && (networkIndexFlag != flag.RECHECK)) {
                    locListener.setNetworkListening(false);
                }

            }
        }
        /*
         * else {
         * 
         * if ("idle".equals(getLastMode())) { lastlocationmode = "noavailable";
         * //scheduleTransmissions(); } lastlocationmode = "noavailable"; if (isListeningNw) {
         * locListener.setNetworkListening(false); } if (isListeningGps) {
         * locListener.setGpsListening(false); } }
         */

    }

    /**
     * Returns the general mode
     * 
     * @return idle if all of the three sensors are in idle mode
     */
    public String getLastMode() {
        if (lastlocationmode.equals("idle") && lastlightmode.equals("idle")
                && lastnoisemode.equals("idle"))
            return "idle";
        else
            return "normal";
    }

    /**
     * Uses the accelerometer instead of the linear acceleration
     */
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

            String selection = DataPoint.SENSOR_NAME + "='" + SensorNames.ACCELEROMETER + "'"
                    + " AND " + DataPoint.TIMESTAMP + ">"
                    + (SNTP.getInstance().getTime() - timerange);

            data = LocalStorage.getInstance(context).query(uri, projection, selection, null, null);

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
                positionFlag = flag.NOT_READY;
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

        } else if (lastlocationmode.equals(NETWORK)) {
            productive = true;
        } else {
            // not enabled
        }

        return productive;
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

        } else if (lastlocationmode.equals("idle") || lastlocationmode.equals("noavailable")) {
            productive = true;
        } else {
            // not enabled
        }

        return productive;
    }

    private boolean isPositionChanged(int distanceTraveled) {
        // Log.v(TAG, "Check if position changed recently");

        boolean moved = true;
        Cursor data = null;
        if (positionFlag == flag.NOT_READY && (distanceTraveled == 0)) {
            // Log.d(TAG, "1A");
            startTime = System.currentTimeMillis();
            positionFlag = flag.READY;
            return moved;
        } else if ((System.currentTimeMillis() - startTime) >= 4 * 60 * 1000
                || (distanceTraveled != 0)) {

            try {
                // Log.d(TAG, "1B");
                // get location data from time since the last check
                long timerange = 1000 * 60 * 15; // 15 minutes
                Uri uri = Uri.parse("content://"
                        + context.getString(R.string.local_storage_authority)
                        + DataPoint.CONTENT_URI_PATH);
                String[] projection = new String[] { DataPoint.SENSOR_NAME, DataPoint.TIMESTAMP,
                        DataPoint.VALUE };
                String selection = DataPoint.SENSOR_NAME + "='" + SensorNames.LOCATION + "'"
                        + " AND " + DataPoint.TIMESTAMP + ">"
                        + (SNTP.getInstance().getTime() - timerange);
                data = LocalStorage.getInstance(context).query(uri, projection, selection, null,
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
        }

        else {
            // Log.d(TAG, "1C");
            return true;
        }
    }

    private boolean isSwitchedOffTooLong(boolean isListeningGps, long listenGpsStop) {

        if (isListeningGps) {
            // Log.d(TAG,"No use checking if GPS is switched off too long: it is still listening!");
            return false;
        }

        boolean tooLong = false;
        long maxDelay = 1000 * 60 * 60; // 1 hour

        if (SNTP.getInstance().getTime() - listenGpsStop > maxDelay) {
            // GPS has been turned off for a long time, or was never even started
            tooLong = true;
            // Log.d(TAG, "GPS has been turned off for a long time, or was never even started");
        } else if (!(locMgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER))) {
            // the network provider is disabled: GPS is the only option
            tooLong = true;
            // Log.d(TAG, " the network provider is disabled: GPS is the only option");
        } else {
            tooLong = false;
        }

        return tooLong;
    }

    /**
     * Resets the location sample rate
     * 
     * @param time
     *            New location sample interval
     */
    public void locSampleRate(long time) {
        LocationSensor locListener = LocationSensor.getInstance(context);
        locListener.disable();
        locListener.enable(time, locListener.getDistance());
    }

    /**
     * Resets the motion sample rate
     * 
     * @param sampleDelay
     *            New motion sample interval
     */
    public void motionSampleRate(long sampleDelay) {
        MotionSensor motion = MotionSensor.getInstance(context);
        motion.stopSensing();
        motion.startSensing(sampleDelay);
    }

    private boolean NwisSwitchedOffTooLong(boolean isListeningNw, long listenNwStop) {

        if (isListeningNw) {
            // Log.d(TAG,
            // "No use checking if Network is switched off too long: it is still listening!");
            return false;
        }

        boolean tooLong = false;
        long maxDelay = 1000 * 60 * 60; // 1 hour

        if (SNTP.getInstance().getTime() - listenNwStop > maxDelay) {
            // Network has been turned off for a long time, or was never even started
            tooLong = true;
            // Log.d(TAG, "Network has been turned off for a long time, or was never even started");
        } else if (!(locMgr.isProviderEnabled(LocationManager.GPS_PROVIDER))) {
            // the GPS provider is disabled: Network is the only option
            tooLong = true;
            // Log.d(TAG, " the gps provider is disabled: NETWORK is the only option");
        } else {
            tooLong = false;
        }

        return tooLong;
    }

    /**
     * If the last general mode is idle, the transmission rate is rescheduled to every half hour.
     */
    public void scheduleTransmissions() {

        Intent intent = new Intent(context.getString(R.string.action_sense_data_transmit_alarm));
        PendingIntent operation = PendingIntent.getBroadcast(context, DataTransmitter.REQ_CODE,
                intent, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        SharedPreferences mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
                Context.MODE_PRIVATE);
        int syncRate = Integer.parseInt(mainPrefs.getString(Main.SYNC_RATE, "0"));

        // pick interval
        long interval;
        // if ("normal".equals(getLastMode())) {
        switch (syncRate) {
        case 1: // eco-mode
            interval = Intervals.ECO;
            break;
        case 0: // 5 minute
            interval = Intervals.NORMAL;
            break;
        case -1: // 60 seconds
            interval = Intervals.OFTEN;
            break;
        case -2: // real-time: schedule transmission based on sample time
            int sampleRate = Integer.parseInt(mainPrefs.getString(Main.SAMPLE_RATE, "0"));
            switch (sampleRate) {
            case 1: // rarely
                interval = Intervals.ECO * 3;
                break;
            case 0: // normal
                interval = Intervals.NORMAL * 3;
                break;
            case -1: // often
                interval = Intervals.OFTEN * 3;
                break;
            case -2: // real time
                interval = Intervals.OFTEN;
                break;
            default:
                Log.e(TAG, "Unexpected sample rate value: " + sampleRate);
                return;
            }
            break;
        default:
            Log.e(TAG, "Unexpected sync rate value: " + syncRate);
            return;
        }
        /*
         * } else { interval = Intervals.ECO * 3; }
         */
        am.cancel(operation);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval,
                operation);
    }
}
