package nl.sense_os.platform;

import nl.sense_os.service.commonsense.SensorRegistrator;
import android.content.Context;

/**
 * Trivial sensor registrator implementation.<br/>
 * <br/>
 * In some cases we do not know the properties of the sensors that will be registered by the app in
 * advance. Because of this, {@link #verifySensorIds(String, String)} is a trivial implementation.
 * Users should call {@link #checkSensor(String, String, String, String, String, String, String)}
 * directly instead.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class TrivialSensorRegistrator extends SensorRegistrator {

    public TrivialSensorRegistrator(Context context) {
        super(context);
    }

    @Override
    public boolean verifySensorIds(String deviceType, String deviceUuid) {
        return false;
    }
}
