package nl.sense_os.platform;
import nl.sense_os.service.ISenseServiceCallback;
import nl.sense_os.service.SenseService.SenseBinder;
import nl.sense_os.service.SenseServiceStub;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
     * Binds to the Sense service, creating it if necessary.
     */
    private void bindToSenseService() {
        // start the service if it was not running already
        if (!mServiceBound) {
            Log.v(TAG, "Try to bind to Sense Platform service");

            final Intent serviceIntent = new Intent(getContext(), nl.sense_os.service.SenseService.class);
            boolean bindResult = mContext.bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

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

    public Context getContext() {
        return mContext;
    }

    /**
     * @return The Sense service instance
     */
    public SenseServiceStub getService() {
        checkSenseService();
        return mSenseService;
    }

    /**
     * Tries to log in at CommonSense using the supplied username and password. After login, the
     * service remembers the username and password.
     * 
     * @param user
     *            Username for login
     * @param password
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