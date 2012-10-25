package nl.sense_os.service.commonsense.senddata;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import nl.sense_os.service.R;
import nl.sense_os.service.commonsense.SenseApi;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.storage.LocalStorage;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

/**
 * Handler for transmission of a file as sensor data to CommonSense.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class FileTransmitHandler extends Handler {

	private static final String TAG = "FileTransmitHandler";
	private final WeakReference<Context> ctxRef;
	private final WeakReference<LocalStorage> storageRef;

	public FileTransmitHandler(Context context, LocalStorage storage, Looper looper) {
		super(looper);
		ctxRef = new WeakReference<Context>(context);
		storageRef = new WeakReference<LocalStorage>(storage);
	}

	private void cleanup(WakeLock wakeLock) {
		if (null != wakeLock) {
			wakeLock.release();
			wakeLock = null;
		}
	}

	@Override
	public void handleMessage(Message message) {

		// get arguments from message
		Bundle args = message.getData();
		String name = args.getString("name");
		String description = args.getString("description");
		String dataType = args.getString("dataType");
		String deviceUuid = args.getString("deviceUuid");
		String cookie = args.getString("cookie");
		JSONObject json = (JSONObject) message.obj;

		// check if our references are still valid
		if (null == ctxRef.get() || null == storageRef.get()) {
			// parent service has died
			return;
		}

		WakeLock wakeLock = null;
		try {
			// make sure the device stays awake while transmitting
			PowerManager powerMgr = (PowerManager) ctxRef.get().getSystemService(
					Context.POWER_SERVICE);
			wakeLock = powerMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
			wakeLock.acquire();

			// get sensor URL from CommonSense
			description = description != null ? description : name;
			String urlStr = SenseApi.getSensorUrl(ctxRef.get(), name, description, dataType,
					deviceUuid);

			if (urlStr == null) {
				Log.w(TAG, "No sensor ID for '" + name + "' (yet). Data is lost.");
				return;
			}

			// submit each file separately
			JSONArray data = json.getJSONArray("data");
			for (int i = 0; i < data.length(); i++) {
				JSONObject object = (JSONObject) data.get(i);
				String fileName = (String) object.get("value");

				HttpURLConnection conn = null;
				DataOutputStream dos = null;

				String lineEnd = "\r\n";
				String twoHyphens = "--";
				String boundary = "----FormBoundary6bYQOdhfGEj4oCSv";

				int bytesRead, bytesAvailable, bufferSize;
				byte[] buffer;
				int maxBufferSize = 1 * 1024 * 1024;

				// ------------------ CLIENT REQUEST

				FileInputStream fileInputStream = new FileInputStream(new File(fileName));

				// open a URL connection to the Servlet
				URL url = new URL(urlStr);

				// Open a HTTP connection to the URL
				conn = (HttpURLConnection) url.openConnection();

				// Allow Inputs
				conn.setDoInput(true);

				// Allow Outputs
				conn.setDoOutput(true);

				// Don't use a cached copy.
				conn.setUseCaches(false);

				// Use a post method.
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Cookie", cookie);
				conn.setRequestProperty("Connection", "Keep-Alive");

				conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

				dos = new DataOutputStream(conn.getOutputStream());

				dos.writeBytes(twoHyphens + boundary + lineEnd);
				dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\""
						+ fileName + "\"" + lineEnd);
				dos.writeBytes(lineEnd);
				// create a buffer of maximum size
				bytesAvailable = fileInputStream.available();
				bufferSize = Math.min(bytesAvailable, maxBufferSize);
				buffer = new byte[bufferSize];

				// read file and write it into form...
				bytesRead = fileInputStream.read(buffer, 0, bufferSize);

				while (bytesRead > 0) {
					dos.write(buffer, 0, bufferSize);
					bytesAvailable = fileInputStream.available();
					bufferSize = Math.min(bytesAvailable, maxBufferSize);
					bytesRead = fileInputStream.read(buffer, 0, bufferSize);
				}

				// send multipart form data necesssary after file data...
				dos.writeBytes(lineEnd);
				dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

				// close streams
				fileInputStream.close();
				dos.flush();
				dos.close();

				if (conn.getResponseCode() != 201) {
					Log.e(TAG, "Failed to send '" + name
							+ "' value file. Data lost. Response code:" + conn.getResponseCode());
				} else {
					Log.i(TAG, "Sent '" + name + "' sensor value file OK!");
					String date = (String) object.get("date");
					onTransmitSuccess(name, date);
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Sending '" + name + "' sensor file failed. Data lost.", e);
		} finally {
			cleanup(wakeLock);
		}
	}

	private void onTransmitSuccess(String sensorName, String timeInSecs) {

		// new content values with updated transmit state
		ContentValues values = new ContentValues();
		values.put(DataPoint.TRANSMIT_STATE, 1);

		long timestamp = Math.round(Double.parseDouble(timeInSecs) * 1000);
		String where = DataPoint.SENSOR_NAME + "='" + sensorName + "'" + " AND "
				+ DataPoint.TIMESTAMP + "=" + timestamp;

		try {
			Uri contentUri = Uri.parse("content://"
					+ ctxRef.get().getString(R.string.local_storage_authority)
					+ DataPoint.CONTENT_URI_PATH);
			int updated = storageRef.get().update(contentUri, values, where, null);
			int deleted = 0;
			if (0 == updated) {
				contentUri = Uri.parse("content://"
						+ ctxRef.get().getString(R.string.local_storage_authority)
						+ DataPoint.CONTENT_PERSISTED_URI_PATH);
				deleted = storageRef.get().delete(contentUri, where, null);
			}
			if ((deleted == 1) || (updated == 1)) {
				// ok
			} else {
				Log.w(TAG,
						"Failed to update the local storage after a file was successfully sent to CommonSense!");
			}
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "Error updating points in Local Storage!", e);
		}

	}
}
