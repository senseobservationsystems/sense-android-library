package nl.sense_os.service.ctrl;

import java.util.List;

import nl.sense_os.service.DataTransmitter;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main;
import nl.sense_os.service.motion.MotionSensor;
import android.app.AlarmManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.location.Location;
import android.os.SystemClock;
import android.util.Log;

/**
 * A singleton that includes all the intelligent methods of the sense-android-library that define
 * the function of specific sensors. The two different extensions, Default and Extended, implement
 * the default and the energy-saving behavior respectively.
 */
public abstract class Controller {

    private class Intervals {
        static final long ECO = AlarmManager.INTERVAL_HALF_HOUR;
        static final long NORMAL = 1000 * 60 * 5;
        static final long OFTEN = 1000 * 60 * 1;
    }

    private static final long DEFAULT_BURST_RATE = 10 * 1000;
    private static final long IDLE_BURST_RATE = 12 * 1000;
    private static final double IDLE_MOTION_THRESHOLD = 0.09;
    private static final double IDLE_TIME_THRESHOLD = 3 * 60 * 1000;
    private long mFirstIdleDetectedTime = 0;
    private static final String TAG = "Controller";
    private static Controller sInstance;

    /**
     * Returns a controller instance
     * 
     * @param context
     *            Context of the Sense service
     * @return the existed controller if any, otherwise the one which has just been created
     */
    public static synchronized Controller getController(Context context) {
        if (sInstance == null) {
            sInstance = new CtrlDefault(context);
        }
        return sInstance;
    }

    private Context mContext;

    protected Controller(Context context) {
        mContext = context;
    }

    /**
     * Retains the mode of Light sensor, and defines it as changed or idle, if the last light
     * measurement had a noticeable difference compared to the previous one or not, respectively
     * 
     * @param value
     *            Last light measurement, in lux.
     */
    public abstract void checkLightSensor(float value);

    /**
     * Retains the mode of Noise sensor, and defines it as loud or idle, if the last noise
     * measurement had a noticeable difference compared to the previous one or not, respectively
     * 
     * @param dB
     *            Last noise measurement, in db.
     */
    public abstract void checkNoiseSensor(double dB);

    /**
     * Checks to see if the sensor is still doing a useful job or whether it is better if we disable
     * it for a while. This method is a callback for a periodic alarm to check the sensor status.
     * 
     * @param isGpsAllowed
     *            True if Gps usage is allowed by the user.
     * @param isListeningNw
     *            True if we currently listen for Network fix.
     * @param isListeningGps
     *            True if we currently listen for Gps fix.
     * @param time
     *            The time between location refresh attempts.
     * @param lastGpsFix
     *            The last fix by Gps provider.
     * @param listenGpsStart
     *            The last time the Gps provider was enabled.
     * @param lastNwFix
     *            The last fix by Network provider.
     * @param listenNwStart
     *            The last time the Network provider was enabled.
     * @param listenGpsStop
     *            The last time the Gps provider was disabled.
     * @param listenNwStop
     *            The last time the Network provider was disabled.
     * 
     * @see #alarmReceiver
     */
    public void checkSensorSettings(boolean isGpsAllowed, boolean isListeningNw,
            boolean isListeningGps, long time, Location lastGpsFix, long listenGpsStart,
            Location lastNwFix, long listenNwStart, long listenGpsStop, long listenNwStop) {
    }



    /**
     * Starts periodic transmission of the buffered sensor data.
     */
    public void scheduleTransmissions() {
        Log.v(TAG, "Schedule transmissions");

        SharedPreferences mainPrefs = mContext.getSharedPreferences(SensePrefs.MAIN_PREFS,
                Context.MODE_PRIVATE);
        int syncRate = Integer.parseInt(mainPrefs.getString(Main.SYNC_RATE, "0"));
        int sampleRate = Integer.parseInt(mainPrefs.getString(Main.SAMPLE_RATE, "0"));

        // pick transmission interval
        long txInterval;
        switch (syncRate) {
        case 1: // eco-mode
            txInterval = Intervals.ECO;
            break;
        case 0: // 5 minute
            txInterval = Intervals.NORMAL;
            break;
        case -1: // 60 seconds
            txInterval = Intervals.OFTEN;
            break;
        case -2: // real-time: schedule transmission based on sample time
            switch (sampleRate) {
            case 1: // rarely
                txInterval = Intervals.ECO * 3;
                break;
            case 0: // normal
                txInterval = Intervals.NORMAL * 3;
                break;
            case -1: // often
                txInterval = Intervals.OFTEN * 3;
                break;
            case -2: // real time
                txInterval = Intervals.OFTEN;
                break;
            default:
                Log.w(TAG, "Unexpected sample rate value: " + sampleRate);
                return;
            }
            break;
        default:
            Log.w(TAG, "Unexpected sync rate value: " + syncRate);
            return;
        }

        // pick transmitter task interval
        long txTaskInterval;
        switch (sampleRate) {
        case -2: // real time
            txTaskInterval = 0;
            break;
        case -1: // often
            txTaskInterval = 10 * 1000;
            break;
        case 0: // normal
            txTaskInterval = 60 * 1000;
            break;
        case 1: // rarely (15 minutes)
            txTaskInterval = 15 * 60 * 1000;
            break;
        default:
            Log.w(TAG, "Unexpected sample rate value: " + sampleRate);
            return;
        }

        DataTransmitter transmitter = DataTransmitter.getInstance(mContext);
        transmitter.startTransmissions(txInterval, txTaskInterval);
    }

    /**
     * Checks to see if burst is completed and resets the motion sample rate in case of idle mode
     * 
     * @param json
     *            The data point.
     * @param dataBuffer
     *            Buffer that contains the data points captured during the burst.
     * @param sensorType
     *            The type of motion sensor.
     * @param localBufferTime
     *            Burst duration.
     * 
     * @see #alarmReceiver
     */
    public void onMotionBurst(List<double[]> dataBuffer, int sensorType) {
        // right now only the accelerometer is used
        if (sensorType != Sensor.TYPE_ACCELEROMETER) {
            return;
        }

        // Initialize with the first vector for the algorithm in the for loop
        double[] firstVector = dataBuffer.get(0);
        double xLast = firstVector[0];
        double yLast = firstVector[1];
        double zLast = firstVector[2];

        double totalMotion = 0;
        for (double[] vector : dataBuffer) {
            // loop over the array to calculate some magic "totalMotion" value that indicates the
            // amount of motion during the burst
            double x = vector[0];
            double y = vector[1];
            double z = vector[2];

            double motion = Math.pow(
                    (Math.abs(x - xLast) + Math.abs(y - yLast) + Math.abs(z - zLast)), 2);
            totalMotion += motion;
            xLast = x;
            yLast = y;
            zLast = z;
        }

        MotionSensor motionSensor = MotionSensor.getInstance(mContext);
        double avgMotion = totalMotion / (dataBuffer.size() - 1);

        // Control logic to choose the sample rate for burst motion sensors
        if (avgMotion > IDLE_MOTION_THRESHOLD) {
            mFirstIdleDetectedTime = 0;
            if (motionSensor.getSampleRate() != DEFAULT_BURST_RATE) {
                motionSensor.setSampleRate(DEFAULT_BURST_RATE);
            }
        } else {
            if (mFirstIdleDetectedTime == 0) {
                mFirstIdleDetectedTime = SystemClock.elapsedRealtime();
            } else {
                if ((SystemClock.elapsedRealtime() > mFirstIdleDetectedTime + IDLE_TIME_THRESHOLD)
                        && (motionSensor.getSampleRate() == DEFAULT_BURST_RATE)) {
                    motionSensor.setSampleRate(IDLE_BURST_RATE);
                }
            }
        }
    }
}
