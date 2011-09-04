package nl.sense_os.phonegap.plugins;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.util.Log;

import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;

import nl.sense_os.service.ISenseService;

import org.json.JSONArray;

import java.util.List;

public abstract class SenseConnectedPlugin extends Plugin {

    /**
     * Service connection to handle connection with the Sense service. Manages the
     * <code>service</code> field when the service is connected or disconnected.
     */
    private class SenseServiceConn implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.v(TAG, "Connection to Sense Platform service established...");
            service = ISenseService.Stub.asInterface(binder);
            isServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.v(TAG, "Connection to Sense Platform service lost...");

            /* this is not called when the service is stopped, only when it is suddenly killed! */
            service = null;
            isServiceBound = false;
        }
    }

    protected static final String TAG = "SensePlugin";

    protected final ServiceConnection conn = new SenseServiceConn();
    protected boolean isServiceBound;
    protected ISenseService service;

    /**
     * Binds to the Sense Service, creating it if necessary.
     */
    protected void bindToSenseService() {
        if (!isServiceBound) {
            Log.v(TAG, "Try to connect to Sense Platform service");
            final Intent service = new Intent(ISenseService.class.getName());
            isServiceBound = ctx.bindService(service, conn, Context.BIND_AUTO_CREATE);
        } else {
            // already bound
        }
    }

    @Override
    public PluginResult execute(String action, JSONArray arguments, String callbackId) {
        if ("init".equals(action)) {
            return init(arguments, callbackId);
        } else {
            Log.e(TAG, "Invalid action: '" + action + "'");
            return new PluginResult(Status.INVALID_ACTION);
        }
    }

    protected PluginResult init(JSONArray data, String callbackId) {
        if (!isSenseInstalled()) {
            Log.w(TAG, "Sense is not installed!");
            ctx.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    AlertDialog d = InstallSenseDialog.create(ctx);
                    d.show();
                }
            });
            return new PluginResult(Status.ERROR);
        } else {
            bindToSenseService();
            return new PluginResult(Status.OK);
        }
    }

    protected boolean isSenseInstalled() {
        PackageManager pm = ctx.getPackageManager();
        List<ResolveInfo> list = pm.queryIntentServices(new Intent(
                "nl.sense_os.service.ISenseService"), 0);
        return list.size() > 0;
    }

    @Override
    public boolean isSynch(String action) {
        if ("init".equals(action)) {
            return true;
        } else {
            return super.isSynch(action);
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

    @Override
    public void onPause(boolean multitasking) {
        Log.v(TAG, "onPause");
        super.onPause(multitasking);
    }

    @Override
    public void onResume(boolean multitasking) {
        Log.v(TAG, "onResume");
        super.onResume(multitasking);
    }

    /**
     * Unbinds from the Sense service, resets {@link #service} and {@link #isServiceBound}.
     */
    protected void unbindFromSenseService() {
        if ((true == isServiceBound) && (null != conn)) {
            Log.v(TAG, "Unbind from Sense Platform service");
            ctx.unbindService(conn);
        } else {
            // already unbound
        }
        service = null;
        isServiceBound = false;
    }

    protected void waitForServiceConnection() {
        try {
            final long start = System.currentTimeMillis();
            while (null == service) {
                if ((System.currentTimeMillis() - start) > 5000) {
                    Log.w(TAG, "Connection to Sense Platform was not established in time!");
                    break;
                } else {
                    Thread.sleep(100);
                }
            }
        } catch (final InterruptedException e) {
            Log.e(TAG, "Interrupted while waiting for connection to Sense Platform service", e);
        }
    }
}
