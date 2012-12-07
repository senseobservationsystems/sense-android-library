package nl.sense_os.service.ctrl;

import nl.sense_os.service.location.LocationSensor;
import android.content.Context;
import android.location.Location;

/**
 * A singleton that includes all the intelligent methods of the sense-android-library that define
 * the function of specific sensors. The two different extensions, Default and Extended, implement
 * the default and the energy-saving behavior respectively.
 */
public abstract class Controller {

    private static Controller ref;
    public static LocationSensor locListener;

    /**
     * Returns a controller instance
     * 
     * @param context
     *            Context of the Sense service
     * @return the existed controller if any, otherwise the one which has just been created
     */
    public static synchronized Controller getController(Context context) {
        if (ref == null) {
            ref = new CtrlDefault(context);
            locListener = LocationSensor.getInstance(context);
        }
        return ref;
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
    public abstract void scheduleTransmissions();
}
