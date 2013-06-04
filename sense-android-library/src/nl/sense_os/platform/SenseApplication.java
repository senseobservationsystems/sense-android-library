package nl.sense_os.platform;

import nl.sense_os.service.SenseServiceStub;
import nl.sense_os.service.ServiceStateHelper;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Status;
import android.app.Application;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

/**
 * Application that connects to the Sense platform.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class SenseApplication extends Application implements ServiceConnection {

    private static final String TAG = "SenseApplication";
    private SensePlatform mSensePlatform;

    /**
     * @return The Sense Platform interface object
     */
    public SensePlatform getSensePlatform() {
        return mSensePlatform;
    }

    /**
     * @return The Sense service instance
     */
    public SenseServiceStub getSenseService() {
        return mSensePlatform.getService();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSensePlatform = new SensePlatform(this, this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.v(TAG, "Service connected");

        // check the sense service status
        new Thread() {

            @Override
            public void run() {
                startSense();
            }
        }.start();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        // nothing to do
    }

    /**
     * Starts the Sense service (if it is not running yet)
     */
    protected void startSense() {

        // check the main status preference to see if we need to re-start Sense
        SharedPreferences startusPrefs = getSharedPreferences(SensePrefs.STATUS_PREFS, MODE_PRIVATE);
        boolean mainStatus = startusPrefs.getBoolean(Status.MAIN, false);
        if (true == mainStatus) {
            // check of the service is not already running
            ServiceStateHelper ssh = ServiceStateHelper.getInstance(getApplicationContext());
            if (!ssh.isStarted()) {
                SenseServiceStub service = mSensePlatform.getService();
                service.toggleMain(true);
            }
        }
    }
}
