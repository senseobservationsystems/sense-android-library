package nl.sense_os.service.commonsense.senddata;

import java.util.List;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.storage.LocalStorage;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Looper;
import android.util.Log;

/**
 * Handler for transmit tasks of recently added data (i.e. data that is stored in system RAM
 * memory). Updates {@link DataPoint#TRANSMIT_STATE} of the data points after the transmission is
 * completed successfully.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class RecentBufferTransmitHandler extends BufferTransmitHandler {

	private static final String TAG = "RecentDataTransmitHandler";
	private final Uri contentUri;

	public RecentBufferTransmitHandler(Context context, LocalStorage storage, Looper looper) {
		super(context, storage, looper);
		contentUri = Uri.parse("content://" + context.getString(R.string.local_storage_authority)
				+ DataPoint.CONTENT_URI_PATH);
	}

	@Override
	protected Cursor getUnsentData() {
		try {
			String where = DataPoint.TRANSMIT_STATE + "=0";
            String sortOrder = DataPoint.TIMESTAMP + " ASC";
            Cursor unsent = storageRef.get().query(contentUri, null, where, null, sortOrder);
			if (null != unsent) {
				Log.v(TAG, "Found " + unsent.getCount() + " unsent data points in local storage");
			} else {
				Log.w(TAG, "Failed to get unsent recent data points from local storage");
			}
			return unsent;
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "Error querying Local Storage!", e);
			return null;
		}
	}

	@Override
    protected void onBufferEmpty() {
        // not used
    }

    @Override
    protected void onTransmitSuccess(List<SensorDataEntry> sensorDatas) throws JSONException {

		// log our great success
        Log.i(TAG, "Sent recent sensor data from the local storage!");

		// new content values with updated transmit state
		ContentValues values = new ContentValues();
		values.put(DataPoint.TRANSMIT_STATE, 1);

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

			// update points in local storage
			try {
				int updated = storageRef.get().update(contentUri, values, where, null);
				if (updated == dataPoints.length()) {
					// Log.v(TAG, "Updated all " + updated + " '" + sensorName
					// + "' data points in the local storage");
				} else {
					Log.w(TAG, "Wrong number of '" + sensorName
							+ "' data points updated after transmission! " + updated + " vs. "
							+ dataPoints.length());
				}
			} catch (IllegalArgumentException e) {
				Log.e(TAG, "Error updating points in Local Storage!", e);
			}
		}
	}
}