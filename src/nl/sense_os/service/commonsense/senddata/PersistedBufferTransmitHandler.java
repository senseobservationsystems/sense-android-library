package nl.sense_os.service.commonsense.senddata;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.Util;
import nl.sense_os.service.storage.LocalStorage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Handler for transmit tasks of persisted data (i.e. data that was stored in the SQLite database).
 * Removes the data from the database after the transmission is completed successfully.
 */
public class PersistedBufferTransmitHandler extends BufferTransmitHandler {

	private static final String TAG = "PersistedDataTransmitHandler";
	private final Uri contentUri;

	public PersistedBufferTransmitHandler(Context context, LocalStorage storage, Looper looper) {
		super(context, storage, looper);
		contentUri = Uri.parse("content://" + context.getString(R.string.local_storage_authority)
				+ DataPoint.CONTENT_PERSISTED_URI_PATH);
	}

	@Override
	protected Cursor getUnsentData() {
		try {
			String where = DataPoint.TRANSMIT_STATE + "=0";
			Cursor unsent = storageRef.get().query(contentUri, null, where, null, null);

			if (null != unsent) {
				Log.v(TAG, "Found " + unsent.getCount()
						+ " unsent data points in persistant storage");
			} else {
				Log.w(TAG, "Failed to get data points from the persistant storage");
			}
			return unsent;
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "Error querying Local Storage!", e);
			return null;
		}
	}

	@Override
	protected void onTransmitSuccess(String cookie, JSONObject transmission) throws JSONException {

		// log our great success
		int bytes = transmission.toString().getBytes().length;
		Log.i(TAG,
				"Sent old sensor data from persistant storage! Raw data size: "
						+ Util.humanReadableByteCount(bytes, false));

		int totalDatapoints = 0;
		JSONArray sensorDatas = transmission.getJSONArray("sensors");
		for (int i = 0; i < sensorDatas.length(); i++) {

			JSONObject sensorData = sensorDatas.getJSONObject(i);

			// get the name of the sensor, to use in the ContentResolver query
			String sensorName = sensorData.getString("sensor_name");

			// select points for this sensor, between the first and the last time stamp
			JSONArray dataPoints = sensorData.getJSONArray("data");
			totalDatapoints += dataPoints.length();
			String frstTimeStamp = dataPoints.getJSONObject(0).getString("date");
			String lastTimeStamp = dataPoints.getJSONObject(dataPoints.length() - 1).getString(
					"date");
			long min = Math.round(Double.parseDouble(frstTimeStamp) * 1000);
			long max = Math.round(Double.parseDouble(lastTimeStamp) * 1000);
			String where = DataPoint.SENSOR_NAME + "='" + sensorName + "'" + " AND "
					+ DataPoint.TIMESTAMP + ">=" + min + " AND " + DataPoint.TIMESTAMP + " <="
					+ max;

			// delete the data from the storage
			try {
				int deleted = storageRef.get().delete(contentUri, where, null);
				if (deleted == dataPoints.length()) {
					// Log.v(TAG, "Deleted all " + deleted + " '" + sensorName +
					// "' points from the persistant storage");
				} else {
					Log.w(TAG, "Wrong number of '" + sensorName
							+ "' data points deleted after transmission! " + deleted + " vs. "
							+ dataPoints.length());
				}
			} catch (IllegalArgumentException e) {
				Log.e(TAG, "Error deleting points from Local Storage!", e);
			}
		}

		// check if there is more data left
		SharedPreferences mainPrefs = ctxRef.get().getSharedPreferences(SensePrefs.MAIN_PREFS,
				Context.MODE_PRIVATE);
		int maxDataPoints = mainPrefs.getBoolean(Main.Motion.EPIMODE, false) ? LocalStorage.QUERY_RESULTS_LIMIT_EPI_MODE
				: LocalStorage.QUERY_RESULTS_LIMIT;

		// there is probably more data, try to send more
		if (totalDatapoints == maxDataPoints) {
			// Log.d(TAG,
			// "There is more data! Sending another batch from the persistant storage.");

			Message msg = obtainMessage(0, cookie);
			this.sendMessage(msg);
		}
	}
}
