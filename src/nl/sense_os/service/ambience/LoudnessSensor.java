package nl.sense_os.service.ambience;

import java.util.ArrayList;
import java.util.Iterator;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.SensorSpecifics;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;

/* scales to max and min */
public class LoudnessSensor {
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

	private Context context;

	private ArrayList<TimestampValueTuple> window = new ArrayList<LoudnessSensor.TimestampValueTuple>();
	private double totalSilence;
	private double loudest;
	private boolean filled = false;

	public LoudnessSensor(Context context) {
		this.context = context;
		AVERAGING_PERIOD = DEFAULT_AVERAGING_PERIOD;
		//restore silence
        SharedPreferences sensorSpecifics = context.getSharedPreferences(SensePrefs.SENSOR_SPECIFICS,
                Context.MODE_PRIVATE);
        totalSilence = sensorSpecifics.getFloat(SensorSpecifics.Loudness.TOTAL_SILENCE, DEFAULT_TOTAL_SILENCE);
        loudest = sensorSpecifics.getFloat(SensorSpecifics.Loudness.LOUDEST, DEFAULT_LOUDEST);
        
	}

	public void onNewNoise(long ms, double dB) {
		addNoiseInDb(ms, dB);
		// double value = relativeLoudnessOf(dB);
		// Log.v(TAG, "new noise value " + dB + ", loudness: " + value);
		if (filled)
			sendSensorValue(loudness(), ms);
	}

	/*
	private void addOldDataToNoisePD() {
		try {
			// get data
			Uri volatileUri = Uri.parse("content://"
					+ context.getString(R.string.local_storage_authority)
					+ DataPoint.CONTENT_URI_PATH);
			Uri persistentUri = Uri.parse("content://"
					+ context.getString(R.string.local_storage_authority)
					+ DataPoint.CONTENT_PERSISTED_URI_PATH);

			insertValuesFrom(volatileUri);
			insertValuesFrom(persistentUri);

		} catch (Exception e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
		}
	}

	private void insertValuesFrom(Uri uri) throws Exception {
		long startTime = SNTP.getInstance().getTime() - timePeriod;

		LocalStorage storage = LocalStorage.getInstance(context);

		String[] projection = new String[] { DataPoint.VALUE,
				DataPoint.TIMESTAMP, DataPoint.SENSOR_NAME };

		String where = DataPoint.SENSOR_NAME + "='" + SensorNames.NOISE + "'"
				+ " AND " + DataPoint.TIMESTAMP + ">" + startTime;

		Cursor cursor = storage.query(uri, projection, where, null, null);

		// add data to noisePD, TODO: weight with interval so we can
		// correctly handle data sampled with a varying interval
		int count = 0;
		if (cursor.moveToFirst()) {
			do {
				double dB = cursor.getFloat(0);
				noisePD.addValue(dB);
			} while (cursor.moveToNe
			
			
					xt());
		}
		Log.v(TAG, "got " + cursor.getCount() + " values from " + uri);
		cursor.close();
	}
	*/

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

		double mean = 20 * Math.log(meanSum / values) / Math.log(2);

		if (mean < totalSilence)
			setLowestEver(mean);
		if (mean > loudest)
			setHighestEver(mean);
		// make relative
		return (mean - totalSilence) / (loudest - totalSilence) * 10;
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
		Intent sensorData = new Intent(
				context.getString(R.string.action_sense_new_data));
		sensorData.putExtra(DataPoint.SENSOR_NAME,
				SensorNames.LOUDNESS);
		sensorData.putExtra(DataPoint.VALUE, (float)value);
		sensorData.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.FLOAT);
		sensorData.putExtra(DataPoint.TIMESTAMP, ms);
		context.startService(sensorData);
	}
}