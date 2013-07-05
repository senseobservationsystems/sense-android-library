package nl.sense_os.platform;

import java.io.IOException;
import java.util.Date;

import nl.sense_os.service.ISenseServiceCallback;
import nl.sense_os.service.R;
import nl.sense_os.service.SenseService.SenseBinder;
import nl.sense_os.service.SenseServiceStub;
import nl.sense_os.service.commonsense.SenseApi;
import nl.sense_os.service.commonsense.SensorRegistrator;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.feedback.FeedbackManager;
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

    /**
     * Service connection to handle connection with the Sense service. Manages the
     * <code>service</code> field when the service is connected or disconnected.
     */
    private class SenseServiceConn implements ServiceConnection {
        private final ServiceConnection mServiceConnection;

        public SenseServiceConn(ServiceConnection serviceConnection) {
            mServiceConnection = serviceConnection;
        }

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.v(TAG, "Bound to Sense Platform service...");

            mSenseService = ((SenseBinder) binder).getService();
            mServiceBound = true;

            // notify the external service connection
            if (mServiceConnection != null) {
                mServiceConnection.onServiceConnected(className, binder);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.v(TAG, "Sense Platform service disconnected...");

            // this is not called when the service is stopped, only when it is suddenly killed!

            mSenseService = null;
            mServiceBound = false;

            // notify the external service connection
            if (mServiceConnection != null) {
                mServiceConnection.onServiceDisconnected(className);
            }
        }
    }

    private static final String TAG = "SensePlatform";

    /** Context of the enclosing application */
    private final Context mContext;

    /** Interface for the SenseService. Gets instantiated by {@link #mServiceConnection}. */
    private SenseServiceStub mSenseService;

    /** Keeps track of the service binding state */
    private boolean mServiceBound = false;

    /** Callback for events for the binding with the Sense service */
    private final ServiceConnection mServiceConnection;

    /**
     * @param context
     *            Context that the Sense service will bind to
     */
    public SensePlatform(Context context) {
        this(context, null);
    }

    /**
     * @param context
     *            Context that the Sense service will bind to.
     * @param serviceConnection
     *            ServiceConnection to receive callbacks about the binding with the service.
     */
    public SensePlatform(Context context, ServiceConnection serviceConnection) {
        mServiceConnection = new SenseServiceConn(serviceConnection);
        mContext = context;
        bindToSenseService();
    }

    /**
     * Convenience method to add a data point without specifying the device UUID
     * 
     * @param sensorName
     *            Name of the sensor
     * @param displayName
     *            Display name of the sensor
     * @param description
     *            Description of the sensor, i.e. CommonSense "device_type"
     * @param dataType
     *            Data type, e.g. json, string, float, bool
     * @param value
     *            Data point value
     * @param timestamp
     *            Data point time stamp
     * @return true if the data point was sent to the Sense service
     * @throws IllegalStateException
     *             If the Sense service is not bound yet
     * @see #addDataPoint(String, String, String, String, String, String, long)
     */
    public boolean addDataPoint(String sensorName, String displayName, String description,
            String dataType, Object value, long timestamp) throws IllegalStateException {
        return addDataPoint(sensorName, displayName, description, dataType, null, value, timestamp);
    }

    /**
     * Adds a data point for a sensor at CommonSense. If the sensor does not exist yet, it will be
     * created.
     * 
     * @param sensorName
     *            Name of the sensor
     * @param displayName
     *            Display name of the sensor
     * @param description
     *            Description of the sensor, i.e. CommonSense "device_type"
     * @param dataType
     *            Data type, e.g. json, string, float, bool
     * @param deviceUuid
     *            (Optional) Device UUID, set null to make sure the sensor is connected to the
     *            current device
     * @param value
     *            Data point value
     * @param timestamp
     *            Data point time stamp
     * @return true if the data point was sent to the Sense service
     * @throws IllegalStateException
     *             If the Sense service is not bound yet
     */
    public boolean addDataPoint(String sensorName, String displayName, String description,
            String dataType, String deviceUuid, Object value, long timestamp)
            throws IllegalStateException {
        checkSenseService();

        if (null == deviceUuid) {
            deviceUuid = SenseApi.getDefaultDeviceUuid(mContext);
        }

        // register the sensor
        SensorRegistrator registrator = new TrivialSensorRegistrator(mContext);
        registrator.checkSensor(sensorName, displayName, dataType, description, "" + value, null,
                deviceUuid);

        // send data point
        String action = mContext.getString(nl.sense_os.service.R.string.action_sense_new_data);
        Intent intent = new Intent(action);
        intent.putExtra(DataPoint.SENSOR_NAME, sensorName);
        intent.putExtra(DataPoint.DISPLAY_NAME, displayName);
        intent.putExtra(DataPoint.SENSOR_DESCRIPTION, description);
        intent.putExtra(DataPoint.DATA_TYPE, dataType);
        intent.putExtra(DataPoint.DEVICE_UUID, deviceUuid);
        if (dataType.equals(SenseDataTypes.JSON)
                || dataType.equals(SenseDataTypes.JSON_TIME_SERIES)) {
            intent.putExtra(DataPoint.VALUE, value.toString());
        } else if (dataType.equals(SenseDataTypes.BOOL)) {
            intent.putExtra(DataPoint.VALUE, (Boolean) value);
        } else if (dataType.equals(SenseDataTypes.FLOAT)) {
            intent.putExtra(DataPoint.VALUE, (Float) value);
        } else if (dataType.equals(SenseDataTypes.INT)) {
            intent.putExtra(DataPoint.VALUE, (Integer) value);
        } else if (dataType.equals(SenseDataTypes.STRING)) {
            intent.putExtra(DataPoint.VALUE, (String) value);
        } else {
            intent.putExtra(DataPoint.VALUE, (String) value);
        }
        intent.putExtra(DataPoint.TIMESTAMP, timestamp);
        ComponentName serviceName = mContext.startService(intent);

        if (null != serviceName) {
            return true;
        } else {
            Log.w(TAG, "Could not start MsgHandler service!");
            return false;
        }
    }

    /**
     * Binds to the Sense service, creating it if necessary.
     */
    private void bindToSenseService() {
        // start the service if it was not running already
        if (!mServiceBound) {
            Log.v(TAG, "Try to bind to Sense Platform service");

            final Intent serviceIntent = new Intent(
                    mContext.getString(R.string.action_sense_service));
            boolean bindResult = mContext.bindService(serviceIntent, mServiceConnection,
                    Context.BIND_AUTO_CREATE);

            Log.v(TAG, "Result: " + bindResult);
        } else {
            // already bound
        }
    }

    /**
     * Check that the sense service is bound. This method is used for public methods to provide a
     * single check for the sense service.
     * 
     * @throws IllegalStateException
     *             If the Sense service is not bound yet
     */
    private void checkSenseService() throws IllegalStateException {
        if (mSenseService == null) {
            throw new IllegalStateException("Sense service not bound");
        }
    }

    /**
     * Closes the service connection to the Sense service and cleans up the binding.
     */
    public void close() {
        unbindFromSenseService();
    }

    /**
     * Flush data to CommonSense
     * 
     * @return true if the flush task was successfully started
     * @throws IllegalStateException
     *             If the Sense service is not bound yet
     */
    public boolean flushData() throws IllegalStateException {
        checkSenseService();
        Intent flush = new Intent(mContext.getString(R.string.action_sense_send_data));
        ComponentName started = mContext.startService(flush);
        return null != started;
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

    public Context getContext() {
        return mContext;
    }

    /**
     * Retrieve a number of values of a sensor from CommonSense.
     * 
     * @param sensorName
     *            The name of the sensor to get data from
     * @param onlyFromDevice
     *            Whether or not to only look through sensors that are part of this device. Searches
     *            all sensors, including those of this device, if set to NO
     * @return JSONArray of data points
     * @throws IllegalStateException
     *             If the Sense service is not bound yet
     * @throws JSONException
     *             If the response from CommonSense could not be parsed
     */
    public JSONArray getData(String sensorName, boolean onlyFromDevice)
            throws IllegalStateException, JSONException {
        return getData(sensorName, onlyFromDevice, 100);
    }

    /**
     * Retrieve a number of values of a sensor from CommonSense.
     * 
     * @param sensorName
     *            The name of the sensor to get data from
     * @param onlyFromDevice
     *            Whether or not to only look through sensors that are part of this device. Searches
     *            all sensors, including those of this device, if set to NO
     * @param limit
     *            Maximum amount of data points.
     * @return JSONArray of data points
     * @throws IllegalStateException
     *             If the Sense service is not bound yet
     * @throws JSONException
     *             If the response from CommonSense could not be parsed
     */
    public JSONArray getData(String sensorName, boolean onlyFromDevice, int limit)
            throws IllegalStateException, JSONException {
        checkSenseService();

        JSONArray result = new JSONArray();

        // select remote path in local storage
        String localStorage = mContext.getString(R.string.local_storage_authority);
        Uri uri = Uri.parse("content://" + localStorage + DataPoint.CONTENT_REMOTE_URI_PATH);

        // get the data
        result = getValues(sensorName, onlyFromDevice, limit, uri);

        return result;
    }

    /**
     * Retrieve a number of values of a sensor from the local storage.
     * 
     * @param sensorName
     *            The name of the sensor to get data from
     * @param onlyFromDevice
     *            Whether or not to only look through sensors that are part of this device. Searches
     *            all sensors, including those of this device, if set to NO
     * @return JSONArray of data points
     * @throws IllegalStateException
     *             If the Sense service is not bound yet
     * @throws JSONException
     *             If the response from CommonSense could not be parsed
     */
    public JSONArray getLocalData(String sensorName) throws IllegalStateException, JSONException {
        return getLocalData(sensorName, 100);
    }

    /**
     * Retrieve a number of values of a sensor from the local storage.
     * 
     * @param sensorName
     *            The name of the sensor to get data from
     * @param onlyFromDevice
     *            Whether or not to only look through sensors that are part of this device. Searches
     *            all sensors, including those of this device, if set to NO
     * @param limit
     *            Maximum amount of data points.
     * @return JSONArray of data points
     * @throws IllegalStateException
     *             If the Sense service is not bound yet
     * @throws JSONException
     *             If the response from CommonSense could not be parsed
     */
    public JSONArray getLocalData(String sensorName, int limit) throws IllegalStateException,
            JSONException {
        checkSenseService();

        JSONArray result = new JSONArray();

        // select remote path in local storage
        String localStorage = mContext.getString(R.string.local_storage_authority);
        Uri uri = Uri.parse("content://" + localStorage + DataPoint.CONTENT_URI_PATH);

        // get the data
        result = getValues(sensorName, true, limit, uri);

        return result;
    }

    /**
     * @return The intent action for new sensor data. This can be used to subscribe to new data.
     */
    public String getNewDataAction() {
        return mContext.getString(R.string.action_sense_new_data);
    }

    /**
     * @return The Sense service instance
     */
    public SenseServiceStub getService() {
        checkSenseService();
        return mSenseService;
    }

    /**
     * Gets array of values from the LocalStorage in <code>DESC</code> order.
     * 
     * @param sensorName
     *            Name of the sensor to get values from.
     * @param onlyFromDevice
     *            If true this function only looks for sensors attached to this device.
     * @param limit
     *            Maximum amount of data points. Optional, use null to set the default limit (100).
     * @param uri
     *            The uri to get data from, can be either local or remote.
     * @return JSONArray with values for the sensor with the selected name and device
     * @throws JSONException
     * @see #getValues(String, boolean, Integer, android.net.Uri, String)
     */
    private JSONArray getValues(String sensorName, boolean onlyFromDevice, int limit, Uri uri)
            throws JSONException {
        String orderBy = DataPoint.TIMESTAMP + " DESC";
        return getValues(sensorName, onlyFromDevice, limit, uri, orderBy);
    }

    /**
     * Gets array of values from the LocalStorage
     * 
     * @param sensorName
     *            Name of the sensor to get values from.
     * @param onlyFromDevice
     *            If true this function only looks for sensors attached to this device.
     * @param limit
     *            Maximum amount of data points. Optional, use null to set the default limit (100).
     * @param uri
     *            The uri to get data from, can be either local or remote.
     * @param sortOrder
     *            The sort order, one of <code>DESC</code> or <code>ASC</code>.
     * @return JSONArray with values for the sensor with the selected name and device
     * @throws JSONException
     */
    private JSONArray getValues(String sensorName, boolean onlyFromDevice, Integer limit, Uri uri,
            String sortOrder) throws JSONException {
        Cursor cursor = null;
        JSONArray result = new JSONArray();

        String deviceUuid = onlyFromDevice ? SenseApi.getDefaultDeviceUuid(mContext) : null;

        String[] projection = new String[] { DataPoint.TIMESTAMP, DataPoint.VALUE };
        String selection = DataPoint.SENSOR_NAME + " = '" + sensorName + "'";
        if (null != deviceUuid) {
            selection += " AND " + DataPoint.DEVICE_UUID + "='" + deviceUuid + "'";
        }
        String[] selectionArgs = null;

        // make sure the limit is feasible
        if (limit < 1) {
            limit = 100;
        }

        try {
            cursor = LocalStorage.getInstance(mContext).query(uri, projection, selection,
                    selectionArgs, limit, sortOrder);

            if (null != cursor && cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    JSONObject val = new JSONObject();
                    val.put("date", cursor.getLong(cursor.getColumnIndex(DataPoint.TIMESTAMP)));
                    val.put("value", cursor.getString(cursor.getColumnIndex(DataPoint.VALUE)));
                    result.put(val);
                    cursor.moveToNext();
                }
            }
        } catch (JSONException je) {
            throw je;
        } finally {
            if (cursor != null)
                cursor.close();
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
     * @return true if the feedback was received at CommonSense
     * @throws IllegalStateException
     *             If the Sense service is not bound yet
     * @throws JSONException
     *             If the response from CommonSense could not be parsed
     * @throws IOException
     *             If the communication with CommonSense failed
     */
    public boolean giveFeedback(String state, Date from, Date to, String label)
            throws IllegalStateException, IOException, JSONException {
        checkSenseService();

        // make sure the latest data is sent to CommonSense
        flushData();

        // use feedback manager
        FeedbackManager fm = new FeedbackManager(mContext);
        boolean result = fm.giveFeedback(state, from.getTime(), to.getTime(), label);

        return result;
    }

    /**
     * Tries to log in at CommonSense using the supplied username and password. After login, the
     * service remembers the username and password.
     * 
     * @param username
     *            Username for login
     * @param pass
     *            Hashed password for login
     * @param callback
     *            Interface to receive callback when login is completed
     * @throws IllegalStateException
     *             If the Sense service is not bound yet
     * @throws RemoteException
     */
    public void login(String user, String password, ISenseServiceCallback callback)
            throws IllegalStateException, RemoteException {
        checkSenseService();
        mSenseService.changeLogin(user, password, callback);
    }

    /**
     * Logs out a user, destroying his or her records.
     */
    public void logout() throws IllegalStateException, RemoteException {
        checkSenseService();
        mSenseService.logout();
    }

    /**
     * Registers a new user at CommonSense and logs in immediately.
     * 
     * @param username
     *            Username for the new user
     * @param password
     *            Hashed password String for the new user
     * @param email
     *            Email address
     * @param address
     *            Street address (optional, null if not required)
     * @param zipCode
     *            ZIP code (optional, null if not required)
     * @param country
     *            Country
     * @param firstName
     *            First name (optional, null if not required)
     * @param surname
     *            Surname (optional, null if not required)
     * @param mobileNumber
     *            Phone number, preferably in E164 format (optional, null if not required)
     * @param callback
     *            Interface to receive callback when login is completed
     */
    public void registerUser(String username, String password, String email, String address,
            String zipCode, String country, String firstName, String surname, String mobileNumber,
            ISenseServiceCallback callback) throws IllegalStateException, RemoteException {
        checkSenseService();
        mSenseService.register(username, password, email, address, zipCode, country, firstName,
                surname, mobileNumber, callback);
    }

    /**
     * Unbinds from the Sense service, resets {@link #mSenseService} and {@link #mServiceBound}.
     */
    private void unbindFromSenseService() {
        if (true == mServiceBound && null != mServiceConnection) {
            Log.v(TAG, "Unbind from Sense Platform service");
            mContext.unbindService(mServiceConnection);
        } else {
            // already unbound
        }
        mSenseService = null;
        mServiceBound = false;
    }
}