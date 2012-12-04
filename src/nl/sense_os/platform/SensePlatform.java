package nl.sense_os.platform;

import java.util.Date;

import nl.sense_os.service.ISenseService;
import nl.sense_os.service.ISenseServiceCallback;
import nl.sense_os.service.R;
import nl.sense_os.service.commonsense.SenseApi;
import nl.sense_os.service.commonsense.SensorRegistrator;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.storage.LocalStorage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
 * A proxy class that acts as a high-level interface to the sense Android library. By instantiating
 * this class you bind (and start if needed) the sense service. You can then use the high level
 * methods of this class, and/or get the service object to work directly with the sense service.
 */
public class SensePlatform {

    private static final String TAG = "SensePlatform";

    /**
     * Context of the enclosing application.
     */
    private final Context context;

    /**
     * Keeps track of the service binding state.
     */
    private boolean isServiceBound = false;

    /**
     * Interface for the SenseService. Gets instantiated by {@link #serviceConn}.
     */
    private ISenseService service;

	/**
	 * Service connection to handle connection with the Sense service. Manages
	 * the <code>service</code> field when the service is connected or
	 * disconnected.
	 */
	private class SenseServiceConn implements ServiceConnection {
        private final ServiceConnection serviceConnection;

        public SenseServiceConn(ServiceConnection serviceConnection) {
            this.serviceConnection = serviceConnection;
		}

		@Override
		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.v(TAG, "Bound to Sense Platform service...");

			service = ISenseService.Stub.asInterface(binder);
			isServiceBound = true;

            // notify the external service connection
            if (serviceConnection != null) {
                serviceConnection.onServiceConnected(className, binder);
            }
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			Log.v(TAG, "Sense Platform service disconnected...");

            /*
             * this is not called when the service is stopped, only when it is suddenly killed!
             */
			service = null;
			isServiceBound = false;

            // notify the external service connection
            if (serviceConnection != null) {
                serviceConnection.onServiceDisconnected(className);
            }
		}
	}

    /**
     * Callback for events for the binding with the Sense service
     */
	private final ServiceConnection serviceConn;

    /**
     * Binds to the Sense service, creating it if necessary.
     */
    public void bindToSenseService() {
		// start the service if it was not running already
		if (!isServiceBound) {
			Log.v(TAG, "Try to bind to Sense Platform service");

			final Intent serviceIntent = new Intent(context.getString(R.string.action_sense_service));
			boolean bindResult = context.bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);

			Log.v(TAG, "Result: " + bindResult);
		} else {
			// already bound
		}
	}

    /**
     * @param context
     *            Context that the Sense service will bind to.
     * @param serviceConnection
     *            Optional. ServiceConnection that handles callbacks about the binding with the
     *            service.
     */
    public SensePlatform(Context context, ServiceConnection serviceConnection) {
        this.serviceConn = new SenseServiceConn(serviceConnection);
		this.context = context;
		bindToSenseService();
	}

    /**
     * Flush data to Common Sense
     * 
     * @throws IllegalStateException
     *             If the Sense service is not bound yet
     */
    public void flushData() throws IllegalStateException {
		checkSenseService();
		Intent flush = new Intent(context.getString(R.string.action_sense_send_data));
		context.startService(flush);
	}

    /**
     * Flush data to Common Sense, return after the flush is completed
     * 
     * @throws IllegalStateException
     *             If the Sense service is not bound yet
     */
    public void flushDataAndBlock() throws IllegalStateException {
		checkSenseService();
		flushData();
		// TODO: block till flush finishes or returns an error
	}

    /**
     * Tries to log in at CommonSense using the supplied username and password. After login, the
     * service remembers the username and password.
     * 
     * @param username
     *            username for login
     * @param pass
     *            unhashed password for login
     * @param callback
     *            interface to receive callback when login is completed
     * @throws IllegalStateException
     *             If the Sense service is not bound yet
     * @throws RemoteException
     */
    public void login(String user, String password, ISenseServiceCallback.Stub callback)
            throws IllegalStateException, RemoteException {
		checkSenseService();
		service.changeLogin(user, SenseApi.hashPassword(password), callback);
	}

    /**
     * Register a user in Common Sense
     * 
     * @throws IllegalStateException
     *             If the Sense service is not bound yet
     * @throws RemoteException
     */
    public void registerUser(String username, String password, String email, String address,
            String zipCode, String country, String firstName, String surname, String mobileNumber,
            ISenseServiceCallback.Stub callback) throws IllegalStateException, RemoteException {
		checkSenseService();
		service.register(username, password, email, address, zipCode, country, firstName, surname, mobileNumber, callback);
	}

    /**
     * Add a data point for a sensor, if the sensor doesn't exist it will be created
     * 
     * @throws IllegalStateException
     *             If the Sense service is not bound yet
     */
    public void addDataPoint(String sensorName, String displayName, String description,
            String dataType, String value, long timestamp) throws IllegalStateException {
		checkSenseService();

		// register the sensor
        SensorRegistrator registrator = new TrivialSensorRegistrator(context);
        registrator.checkSensor(sensorName, displayName, dataType, description, value, null, null);

		// send data point
		String action = context.getString(nl.sense_os.service.R.string.action_sense_new_data);
		Intent intent = new Intent(action);
		intent.putExtra(DataPoint.SENSOR_NAME, sensorName);
		intent.putExtra(DataPoint.DISPLAY_NAME, displayName);
		intent.putExtra(DataPoint.SENSOR_DESCRIPTION, description);
		intent.putExtra(DataPoint.DATA_TYPE, dataType);
		intent.putExtra(DataPoint.VALUE, value);
		intent.putExtra(DataPoint.TIMESTAMP, timestamp);
		context.startService(intent);
	}

    /**
     * Retrieve a number of values of a sensor from CommonSense. returns nrLastPoints of the latest
     * values.
     * 
     * @param sensorName
     *            The name of the sensor to get data from
     * @param onlyFromDevice
     *            Whether or not to only look through sensors that are part of this device. Searches
     *            all sensors, including those of this device, if set to NO
     * @param nrLastPoints
     *            Number of points to retrieve, this function always returns the latest values for
     *            the sensor.
     * @return an JSONArray of data points
     * @throws IllegalStateException
     *             If the Sense service is not bound yet
     */
    public JSONArray getData(String sensorName, boolean onlyFromDevice, int nrLastPoints)
            throws IllegalStateException {
		checkSenseService();

		Log.v(TAG, "getRemoteValues('" + sensorName + "', " + onlyFromDevice + ")");

		JSONArray result = new JSONArray();

		try {
			// select remote url
			Uri uri = Uri.parse("content://" + context.getString(R.string.local_storage_authority) + DataPoint.CONTENT_REMOTE_URI_PATH);

			// get the data
			// TODO: use nrLastPoints
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
     * @throws IllegalStateException
     *             If the Sense service is not bound yet
     */
    public void giveFeedback(String state, Date from, Date to, String label)
            throws IllegalStateException {
		checkSenseService();
		// TODO: implement
	}

    /**
     * @return The Sense service instance
     */
	public ISenseService service() {
		return service;
	}
	
	/**
	 * Return the intent for new sensor data. This can be used to subscribe to new data.
	 * 
	 * @return The intent action for new sensor data
	 */
	public String newDataAction() {
		return context.getString(R.string.action_sense_new_data);
	}

	/**
	 * Gets array of values from the LocalStorage
	 * 
	 * @param sensorName
	 *            Name of the sensor to get values from.
	 * @param onlyFromDevice
	 *            If true this function only looks for sensors attached to this
	 *            device.
	 * @param uri
	 *            The uri to get data from, can be either local or remote.
	 * @return JSONArray with values for the sensor with the selected name and
	 *         device
	 * @throws JSONException
	 */
	private JSONArray getValues(String sensorName, boolean onlyFromDevice, Uri uri) throws JSONException {
		Cursor cursor = null;
		JSONArray result = new JSONArray();

		String deviceUuid = onlyFromDevice ? SenseApi.getDefaultDeviceUuid(context) : null;

		String[] projection = new String[] { DataPoint.TIMESTAMP, DataPoint.VALUE };
		String selection = DataPoint.SENSOR_NAME + " = '" + sensorName + "'";
		if (null != deviceUuid) {
			selection += " AND " + DataPoint.DEVICE_UUID + "='" + deviceUuid + "'";
		}
		String[] selectionArgs = null;
		String sortOrder = null;
		try {
			cursor = LocalStorage.getInstance(context).query(uri, projection, selection, selectionArgs, sortOrder);

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

    /**
     * Check that the sense service is bound. This method is used for public methods to provide a
     * single check for the sense service.
     * 
     * @throws IllegalStateException
     *             If the Sense service is not bound yet
     */
    private void checkSenseService() throws IllegalStateException {
		if (service == null) {
            throw new IllegalStateException("Sense service not bound");
		}
	}
}