package nl.sense_os.service.ambience;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.SensorSpecifics;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

/**
 * Helper class for {@link NoiseSensor}. Estimates the sound pressure level relative to
 * the standard reference value (http://en.wikipedia.org/wiki/Sound_pressure_level#Sound_pressure_level). This value should be absolute and can therefore be compared between devices and users.
 * 
 * It works by using the lowest value ever recorded as a reference value for silence.
 *  
 * @author Pim Nijdam <pim@sense-os.nl>
 */
public class AutoCalibratedNoiseSensor {
	private static final String TAG = "AutoCalibratedNoiseSensor";
	private static final float DEFAULT_TOTAL_SILENCE = Float.MAX_VALUE;
	private static final float DEFAULT_LOUDEST = Float.MIN_VALUE;
	private static final double MIN_LOUDNESS_DYNAMIC = 10;
	private static final double REF_SILENCE = 25; //assume 20 db(SPL) will be the lowest recording

	private Context context;

	private double totalSilence;
	private double loudest;

	protected AutoCalibratedNoiseSensor(Context context) {
		this.context = context;
		//restore silence
        SharedPreferences sensorSpecifics = context.getSharedPreferences(SensePrefs.SENSOR_SPECIFICS,
                Context.MODE_PRIVATE);
        totalSilence = sensorSpecifics.getFloat(SensorSpecifics.AutoCalibratedNoise.TOTAL_SILENCE, DEFAULT_TOTAL_SILENCE);
        loudest = sensorSpecifics.getFloat(SensorSpecifics.AutoCalibratedNoise.LOUDEST, DEFAULT_LOUDEST);
        Log.v("Sense AutocalibratedNoiseSensor","Loudest " + loudest + ", total silence " + totalSilence);
        
	}
    
	private static AutoCalibratedNoiseSensor instance = null;
    
    public static AutoCalibratedNoiseSensor getInstance(Context context) {
	    if(instance == null) {
	       instance = new AutoCalibratedNoiseSensor(context);
	    }
	    return instance;
    }

	public void onNewNoise(long ms, double dB) {
			double calibrated = calibratedValue(dB);
			//check that at least a certain variance is observed, otherwise don't upload this sensor just yet
			if (loudest - totalSilence > MIN_LOUDNESS_DYNAMIC)
				sendSensorValue(calibrated, ms);
	}

	private double calibratedValue(double db) {
		if (db < totalSilence)
			setLowestEver(db);
		if (db > loudest)
			setHighestEver(db);

		//calibrate
		/* 20 log(Prms / Pref) = 20 * log(Prms) - 20 * log(Pref). */
		double calibrated = db - totalSilence + REF_SILENCE;
		return calibrated;
	}

	private void setLowestEver(double lowest) {
		totalSilence = lowest;
        Editor sensorSpecifics = context.getSharedPreferences(SensePrefs.SENSOR_SPECIFICS,
                Context.MODE_PRIVATE).edit();
        sensorSpecifics.putFloat(SensorSpecifics.AutoCalibratedNoise.TOTAL_SILENCE, (float)totalSilence);
        sensorSpecifics.commit();
	}
	private void setHighestEver(double highest) {
		loudest = highest;
        Editor sensorSpecifics = context.getSharedPreferences(SensePrefs.SENSOR_SPECIFICS,
                Context.MODE_PRIVATE).edit();
        sensorSpecifics.putFloat(SensorSpecifics.AutoCalibratedNoise.LOUDEST, (float)loudest);
        sensorSpecifics.commit();
	}

	private void sendSensorValue(double value, long ms) {
		Intent sensorData = new Intent(
				context.getString(R.string.action_sense_new_data));
		sensorData.putExtra(DataPoint.SENSOR_NAME,
				SensorNames.NOISE);
		sensorData.putExtra(DataPoint.SENSOR_DESCRIPTION,
				"auto-calibrated");
		sensorData.putExtra(DataPoint.VALUE, (float)value);
		sensorData.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.FLOAT);
		sensorData.putExtra(DataPoint.TIMESTAMP, ms);
		context.startService(sensorData);
	}
}
