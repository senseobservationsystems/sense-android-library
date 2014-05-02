package nl.sense_os.service.commonsense.senddata;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SenseUrls;
import nl.sense_os.service.constants.SensePrefs.Main.Advanced;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.storage.LocalStorage;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
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

			
			SharedPreferences sMainPrefs = ctxRef.get().getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);        
			boolean devMode = sMainPrefs.getBoolean(Advanced.DEV_MODE, false);
			String urlStr = devMode ? SenseUrls.DATAPROCESSOR_FILE_DEV : SenseUrls.DATAPROCESSOR_FILE;	
			
			// submit each file separately
			JSONArray data = json.getJSONArray("data");
			for (int i = 0; i < data.length(); i++) {
				JSONObject object = (JSONObject) data.get(i);
				String fileName = (String) object.get("value");

				HttpURLConnection conn = null;
				DataOutputStream dos = null;

				int bytesRead, bytesAvailable, bufferSize;
				byte[] buffer;
				int maxBufferSize = 1 * 1024 * 1024;

				// ------------------ CLIENT REQUEST

				File file = new File(fileName);
				FileInputStream fileInputStream = new FileInputStream(file);

				// get the filename without the directory
				String strippedFile = fileName.substring(fileName.lastIndexOf("/"));
				urlStr += strippedFile;
				
				// open a URL connection to the Servlet
				URL url = new URL(urlStr);
				if ("https".equals(url.getProtocol().toLowerCase(Locale.ENGLISH))) {
					HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
					https.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
					conn = https;
				} else {
					conn = (HttpURLConnection) url.openConnection();
				}
							

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
				conn.setRequestProperty("Transfer-Encoding", "chunked");
				conn.setRequestProperty("Content-Encoding", "gzip");

				dos = new DataOutputStream(conn.getOutputStream());
				GZIPOutputStream zipStream = new GZIPOutputStream(conn.getOutputStream());
				dos = new DataOutputStream(zipStream);

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

				// close streams
				fileInputStream.close();
				dos.flush();
				dos.close();

				if (conn.getResponseCode() != 200) {
					Log.e(TAG, "Failed to send '" + name
							+ "' value file. Data lost. Response code:" + conn.getResponseCode());
				} else {
					Log.i(TAG, "Sent '" + name + "' sensor value file OK!");
					String date = (String) object.get("date");
					onTransmitSuccess(name, date);
					// remove temp file
					if(file.exists())
					{
						try
						{
							Log.d(TAG, "Removing File: "+fileName);
							file.delete();
						}
						catch(Exception e)
						{
							Log.e(TAG,  "Error removing temporary file", e);
						}
					}
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
            if (updated != 1) {
				Log.w(TAG,
						"Failed to update the local storage after a file was successfully sent to CommonSense!");
			}
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "Error updating points in Local Storage!", e);
		}

	}
}
