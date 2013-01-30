package nl.sense_os.service.commonsense.senddata;

import java.util.List;

import nl.sense_os.service.MsgHandler;
import nl.sense_os.service.R;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.storage.LocalStorage;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Looper;
import android.util.Log;

/**
 * Handler for transmit tasks of persisted data (i.e. data that was stored in the SQLite database).
 * Removes the data from the database after the transmission is completed successfully.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
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
            String sortOrder = DataPoint.TIMESTAMP + " ASC";
            Cursor unsent = storageRef.get().query(contentUri, null, where, null, sortOrder);

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
    protected void onBufferEmpty() {
        // Log.d(TAG, "Flash buffer is empty! Start sending from memory buffer.");
        Context context = ctxRef.get();
        Intent sendIntent = new Intent(context.getString(R.string.action_sense_send_data));
        sendIntent.putExtra(MsgHandler.EXTRA_BUFFER_TYPE, MsgHandler.BUFFER_TYPE_MEMORY);
        context.startService(sendIntent);
    }

	@Override
    protected void onTransmitSuccess(List<SensorDataEntry> sensorDatas) throws JSONException {

		// log our great success
        Log.i(TAG, "Sent old sensor data from persistant storage!");

        for (SensorDataEntry sensorData : sensorDatas) {

			// get the name of the sensor, to use in the ContentResolver query
            String sensorName = sensorData.sensorName;
            String description = sensorData.sensorDescription;

			// select points for this sensor, between the first and the last time stamp
            JSONArray dataPoints = sensorData.data;
			String frstTimeStamp = dataPoints.getJSONObject(0).getString("date");
			String lastTimeStamp = dataPoints.getJSONObject(dataPoints.length() - 1).getString(
					"date");
			long min = Math.round(Double.parseDouble(frstTimeStamp) * 1000);
			long max = Math.round(Double.parseDouble(lastTimeStamp) * 1000);
            String where = DataPoint.SENSOR_NAME + "='" + sensorName + "'" + " AND "
                    + DataPoint.SENSOR_DESCRIPTION + "='" + description + "'" + " AND "
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
	}
}
