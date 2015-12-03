package nl.sense_os.service.ambience;

import java.util.ArrayList;
import java.util.Iterator;

import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.SensorSpecifics;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.SensorDataPoint;
import nl.sense_os.service.subscription.BaseDataProducer;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

/**
 * Helper class for {@link NoiseSensor}. Scales the measured sound level measurement according to
 * the highest and lowest sound levels that have been recorded.
 * 
 * @author Pim Nijdam <pim@sense-os.nl>
 */
public class LoudnessSensor extends BaseDataProducer {
	private class TimestampValueTuple {
		long timestamp;
		double value;

		TimestampValueTuple(long timestamp, double value) {
			this.timestamp = timestamp;
			this.value = value;
		}
	};

	//private static final String TAG = "Loudness sensor";
	private static final float DEFAULT_TOTAL_SILENCE = Float.MAX_VALUE;
	private static final float DEFAULT_LOUDEST = Float.MIN_VALUE;
	private final long AVERAGING_PERIOD;
	private static final long DEFAULT_AVERAGING_PERIOD = 10 * 60 * 1000;
	private static final double MIN_LOUDNESS_DYNAMIC = 10;

	private Context context;

	private ArrayList<TimestampValueTuple> window = new ArrayList<LoudnessSensor.TimestampValueTuple>();
	private double totalSilence;
	private double loudest;
	private boolean filled = false;

	protected LoudnessSensor(Context context) {
		this.context = context;
		AVERAGING_PERIOD = DEFAULT_AVERAGING_PERIOD;
		//restore silence
		SharedPreferences sensorSpecifics = context.getSharedPreferences(SensePrefs.SENSOR_SPECIFICS,
				Context.MODE_PRIVATE);
		totalSilence = sensorSpecifics.getFloat(SensorSpecifics.Loudness.TOTAL_SILENCE, DEFAULT_TOTAL_SILENCE);
		loudest = sensorSpecifics.getFloat(SensorSpecifics.Loudness.LOUDEST, DEFAULT_LOUDEST);
		Log.v("Sense Loudness","Loudest " + loudest + ", total silence " + totalSilence);

	}

	private static LoudnessSensor instance = null;

	public static LoudnessSensor getInstance(Context context) {
		if(instance == null) {
			instance = new LoudnessSensor(context);
		}
		return instance;
	}

	public void onNewNoise(long ms, double dB) {
		addNoiseInDb(ms, dB);
		// double value = relativeLoudnessOf(dB);
		// Log.v(TAG, "new noise value " + dB + ", loudness: " + value);
		if (filled) {
			double l = loudness();
			if (loudest - totalSilence > MIN_LOUDNESS_DYNAMIC)
				sendSensorValue(l, ms);
		}
	}

	private void addNoiseInDb(long timestamp, double dB) {
		// TODO: should we calculate something like phon or sone to better fit
		// perceived volume by humans?
		if (dB == 0)
			return; //discard 0
		window.add(new TimestampValueTuple(timestamp, dB));
		if (timestamp - window.get(0).timestamp > AVERAGING_PERIOD)
			filled = true;
	}

	private double loudness() {
		long startTime = SNTP.getInstance().getTime() - AVERAGING_PERIOD;

		// remove old values and average over past period
		// average over past period
		double meanSum = 0;
		int values = 0;
		Iterator<TimestampValueTuple> iter = window.iterator();
		while (iter.hasNext()) {
			TimestampValueTuple tuple = iter.next();
			if (tuple.timestamp < startTime)
				iter.remove();
			else {
				meanSum += Math.pow(10, tuple.value / 20);
				values += 1;
			}
		}

		double mean = 20 * Math.log(meanSum / values) / Math.log(10);

		if (mean < totalSilence)
			setLowestEver(mean);
		if (mean > loudest)
			setHighestEver(mean);
		// make relative
		if (loudest - totalSilence > 0)
			return (mean - totalSilence) / (loudest - totalSilence) * 10;
		else
			return 5;
	}

	private void setLowestEver(double lowest) {
		totalSilence = lowest;
		Editor sensorSpecifics = context.getSharedPreferences(SensePrefs.SENSOR_SPECIFICS,
				Context.MODE_PRIVATE).edit();
		sensorSpecifics.putFloat(SensorSpecifics.Loudness.TOTAL_SILENCE, (float)totalSilence);
		sensorSpecifics.commit();
	}
	private void setHighestEver(double highest) {
		loudest = highest;
		Editor sensorSpecifics = context.getSharedPreferences(SensePrefs.SENSOR_SPECIFICS,
				Context.MODE_PRIVATE).edit();
		sensorSpecifics.putFloat(SensorSpecifics.Loudness.LOUDEST, (float)loudest);
		sensorSpecifics.commit();
	}

	private void sendSensorValue(double value, long ms) {

		this.notifySubscribers();
		SensorDataPoint dataPoint = new SensorDataPoint(value);
		dataPoint.sensorName = SensorNames.LOUDNESS;
		dataPoint.sensorDescription = SensorNames.LOUDNESS;
		dataPoint.timeStamp = ms;        
		this.sendToSubscribers(dataPoint);
	}
}
