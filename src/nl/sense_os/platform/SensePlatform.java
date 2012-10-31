package nl.sense_os.platform;

import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nl.sense_os.service.R;
import nl.sense_os.service.ISenseService;
import nl.sense_os.service.ISenseServiceCallback;
import nl.sense_os.service.commonsense.SenseApi;
import nl.sense_os.service.commonsense.SensorRegistrator;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.storage.LocalStorage;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * A proxy class that acts as a high-level interface to the sense Android
 * library.
 */
public class SensePlatform {
	protected static final String TAG = "SensePlatform";
	protected Context context_;
	
	/*** Code to bind to the sense service ***/
	protected ServiceConnectionEventHandler delegate;
	protected boolean isServiceBound = false;
	protected ISenseService service;
	private SensorRegistrator sensorRegistrator = new SensorRegistrator(context_) {
		@Override
		public boolean verifySensorIds(String deviceType, String deviceUuid) {
			return false;
		}
	};

	/**
	 * Service stub for callbacks from the Sense service.
	 */
	private class SenseCallback extends ISenseServiceCallback.Stub {
		@Override
		public void onChangeLoginResult(int result) throws RemoteException {}

		@Override
		public void onRegisterResult(int result) throws RemoteException {}

		@Override
		public void statusReport(final int status) {}
	}
	
	protected SenseCallback callback = new SenseCallback();
	
	/**
	 * Service connection to handle connection with the Sense service. Manages the
	 * <code>service</code> field when the service is connected or disconnected.
	 */
	private class SenseServiceConn implements ServiceConnection {
		public final ServiceConnectionEventHandler handler;
		
		public SenseServiceConn(ServiceConnectionEventHandler handler) {
			this.handler = handler;
		}

		@Override
		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.v(TAG, "Bound to Sense Platform service...");

			service = ISenseService.Stub.asInterface(binder);
			isServiceBound = true;
			
			if (handler != null)
				handler.onServiceConnected(className, service);
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			Log.v(TAG, "Sense Platform service disconnected...");

			/* this is not called when the service is stopped, only when it is suddenly killed! */
			service = null;
			isServiceBound = false;
			if (handler != null)
				handler.onServiceDisconnected(className);
		}
	}
	private final ServiceConnection serviceConn;// = new SenseServiceConn();

	/**
	 * Binds to the Sense Service, creating it if necessary.
	 */
	public boolean bindToSenseService() {
		// start the service if it was not running already
		if (!isServiceBound) {
			Log.v(TAG, "Try to bind to Sense Platform service");

			final Intent serviceIntent = new Intent(context_.getString(R.string.action_sense_service));
			boolean bindResult = context_.bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);

			Log.v(TAG, "Result: " + bindResult);
		} else {
			// already bound
		}
		return isServiceBound;
	}
	
	/*** Sense Platform high-level API ***/

	public SensePlatform(Context context, ServiceConnectionEventHandler handler) {
		serviceConn = new SenseServiceConn(handler);
		this.context_ = context;
		bindToSenseService();
	}

	/// Flush data to Common Sense
	public void flushData() {
		Intent flush = new Intent(context_.getString(R.string.action_sense_send_data));
		context_.startService(flush);
	}

	/// Flush data to Common Sense, return after the flush is completed
	public void flushDataAndBlock() {
		flushData();
		// TODO: block till flush finishes or returns an error
	}

	/// Set the credentials to log in on Common Sense
	public boolean login(String user, String password) throws RemoteException {
		service.changeLogin(user, SenseApi.hashPassword(password), callback);
		return true;
	}

	/**
	 * Register a user in Common Sense
	 * 
	 * @return Whether the registration succeeded
	 */
	/*
	public boolean registerUser() {
		return false;
	}
	*/

	/**
	 * Add a data point for a sensor, if the sensor doesn't exist it will be
	 * created
	 */
	public void addDataPoint(String sensorName, String displayName, String description, String dataType, String value, long timestamp) {
		// register the sensor
		sensorRegistrator.checkSensor(sensorName, displayName, dataType, description, value, null, null);

		// send data point
		String action = context_.getString(nl.sense_os.service.R.string.action_sense_new_data);
		Intent intent = new Intent(action);
		intent.putExtra(DataPoint.SENSOR_NAME, sensorName);
		intent.putExtra(DataPoint.DISPLAY_NAME, displayName);
		intent.putExtra(DataPoint.SENSOR_DESCRIPTION, description);
		intent.putExtra(DataPoint.DATA_TYPE, dataType);
		intent.putExtra(DataPoint.VALUE, value);
		intent.putExtra(DataPoint.TIMESTAMP, timestamp);
		context_.startService(intent);
	}

	/**
	 * Retrieve a number of values of a sensor from Common Sense. returns
	 * nrLastPoints of the latest values.
	 * 
	 * @param sensorName
	 *            The name of the sensor to get data from
	 * @param onlyFromDevice
	 *            Whether or not to only look through sensors that are part of
	 *            this device. Searches all sensors, including those of this
	 *            device, if set to NO
	 * @param nrLastPoints
	 *            Number of points to retrieve, this function always returns the
	 *            latest values for the sensor.
	 * @return an JSONArray of data points
	 */
	public JSONArray getData(String sensorName, boolean onlyFromDevice, int nrLastPoints) {
		Log.v(TAG, "getRemoteValues('" + sensorName + "', " + onlyFromDevice + ")");
		
		JSONArray result = new JSONArray();
		
		try {
			//select remote url
			Uri uri = Uri.parse("content://"
					+ context_.getString(R.string.local_storage_authority)
					+ DataPoint.CONTENT_REMOTE_URI_PATH);
			
			//get the data
			result = getValues(sensorName, onlyFromDevice, uri);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Give feedback on a state sensor.
	 * 
	 * @param state
	 *            The state to give feedback on.
	 * @param from
	 *            The start date for the feedback.
	 * @param to
	 *            The end date for the feedback.
	 * @param label
	 *            The label of the Feedback, e.g. 'Sit'
	 */
	public void giveFeedback(String state, Date from, Date to, String label) {
		//TODO: implement
	}
	
	/**
	 * Returns the sense service
	 * @return The sense service
	 */
	public ISenseService service() {
		return service;
	}
	
	/**
	 * Gets array of values from the LocalStorage
	 * 
	 * @param sensorName Name of the sensor to get values from.
	 * @param onlyFromDevice If true this function only looks for sensors attached to this device. 
	 * @param uri The uri to get data from, can be either local or remote.
	 * @return JSONArray with values for the sensor with the selected name and device
	 * @throws JSONException
	 */
	private JSONArray getValues(String sensorName, boolean onlyFromDevice, Uri uri) throws JSONException {
		Cursor cursor = null;
		JSONArray result = new JSONArray();

		String deviceUuid = onlyFromDevice ? SenseApi.getDefaultDeviceUuid(context_) : null;

		String[] projection = new String[] { DataPoint.TIMESTAMP, DataPoint.VALUE };
		String selection = DataPoint.SENSOR_NAME + " = '" + sensorName + "'";
		if (null != deviceUuid) {
			selection += " AND " + DataPoint.DEVICE_UUID + "='" + deviceUuid + "'";
		}
		String[] selectionArgs = null;
		String sortOrder = null;
		try {
			cursor = LocalStorage.getInstance(context_).query(uri, projection, selection, selectionArgs, sortOrder);

			if (null != cursor && cursor.moveToFirst()) {
				while (!cursor.isAfterLast()) {
					JSONObject val = new JSONObject();
					val.put("timestamp", cursor.getString(cursor.getColumnIndex(DataPoint.TIMESTAMP)));
					val.put("value", cursor.getString(cursor.getColumnIndex(DataPoint.VALUE)));
					result.put(val);
					cursor.moveToNext();
				}
			}
		} catch (JSONException je) {
			throw je;
		} finally {
			if (cursor != null) cursor.close();
		}

		return result;
	}
}