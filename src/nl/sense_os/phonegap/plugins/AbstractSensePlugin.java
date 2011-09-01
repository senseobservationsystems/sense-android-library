package nl.sense_os.phonegap.plugins;

import nl.sense_os.service.ISenseService;
import nl.sense_os.service.ISenseServiceCallback;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.phonegap.api.Plugin;

public abstract class AbstractSensePlugin extends Plugin {

    private static final String TAG = "SensePlugin";

    /**
     * Service stub for callbacks from the Sense service.
     */
    class SenseCallback extends ISenseServiceCallback.Stub {

        @Override
        public void statusReport(final int status) {
            Log.v(TAG, "Received status report from Sense Platform service...");
        }
    }

    /**
     * Service connection to handle connection with the Sense service. Manages the
     * <code>service</code> field when the service is connected or disconnected.
     */
    class SenseServiceConn implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.v(TAG, "Bound to Sense Platform service...");

            service = ISenseService.Stub.asInterface(binder);
            try {
                service.getStatus(callback);
            } catch (final RemoteException e) {
                Log.e(TAG, "Error checking service status after binding. ", e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.v(TAG, "Sense Platform service disconnected...");

            /* this is not called when the service is stopped, only when it is suddenly killed! */
            service = null;
            isServiceBound = false;
            checkServiceStatus();
        }
    };

    protected final ISenseServiceCallback callback = new SenseCallback();
    protected final ServiceConnection serviceConn = new SenseServiceConn();
    protected ISenseService service;
    protected boolean isServiceBound;

    /**
     * Binds to the Sense Service, creating it if necessary.
     */
    protected void bindToSenseService() {

        // start the service if it was not running already
        if (!isServiceBound) {
            // Log.v(TAG, "Try to bind to Sense Platform service");
            final Intent serviceIntent = new Intent(ISenseService.class.getName());
            isServiceBound = ctx.bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);
        } else {
            // already bound
        }
    }

    /**
     * Calls {@link ISenseService#getStatus(ISenseServiceCallback)} on the service. This will
     * generate a callback that updates the buttons ToggleButtons showing the service's state.
     */
    private void checkServiceStatus() {
        Log.v(TAG, "Checking service status..");

        if (null != service) {
            try {
                // request status report
                service.getStatus(callback);
            } catch (final RemoteException e) {
                Log.e(TAG, "Error checking service status. ", e);
            }
        } else {
            Log.w(TAG, "Not bound to Sense Platform service! Assume it's not running...");
        }
    }

    /**
     * The final call you receive before your activity is destroyed.
     */
    @Override
    public void onDestroy() {
        unbindFromSenseService();
        super.onDestroy();
    }

    /**
     * Unbinds from the Sense service, resets {@link #service} and {@link #isServiceBound}.
     */
    private void unbindFromSenseService() {

        if (true == isServiceBound && null != serviceConn) {
            Log.v(TAG, "Unbind from Sense Platform service");
            ctx.unbindService(serviceConn);
        } else {
            // already unbound
        }
        service = null;
        isServiceBound = false;
    }
}
