package nl.sense.demo;

import nl.sense_os.platform.SensePlatform;
import android.app.Application;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * Application that manages a connection to the Sense service. By having the application take care
 * of this, individual activities do not have to manage the connections on their own.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class SenseApplication extends Application {

    private static final String TAG = "SenseApplication";
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // nothing to do
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v(TAG, "Connected to Sense service");
        }
    };
    private SensePlatform sensePlatform;

    @Override
    public void onCreate() {
        super.onCreate();
        sensePlatform = new SensePlatform(this, serviceConnection);
    }

    /**
     * @return An interface to the Sense service
     */
    public SensePlatform getSensePlatform() {
        return sensePlatform;
    }
}
