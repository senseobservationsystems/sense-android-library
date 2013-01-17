package nl.sense.demo;

import nl.sense_os.platform.SensePlatform;
import nl.sense_os.service.ISenseService;
import nl.sense_os.service.ISenseServiceCallback;
import nl.sense_os.service.commonsense.SenseApi;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main.Ambience;
import nl.sense_os.service.constants.SensePrefs.Main.Location;

import org.json.JSONArray;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * Main activity of the Sense Platform Demo. This activity is created to demonstrate the most
 * important use cases of the Sense Platform library in Android. The goal is to provide useful code
 * snippets that you can use in your own Android project.<br/>
 * <br/>
 * The activity has a trivial UI, but automatically performs the following tasks when it is started.
 * <ul>
 * <li>Create a {@link SensePlatform} instance for communication with the Sense service.</li>
 * <li>Log in as user "foo".</li>
 * <li>Set some sensing preferences, e.g. sample rate and sync rate.</li>
 * <li>Start a couple of sensor modules.</li>
 * <li>Send data for a non-standard sensor: "position_annotation".</li>
 * <li>Get data from a certain sensor.</li>
 * </ul>
 * This class implements the {@link ServiceConnection} interface so it can receive callbacks from the
 * SensePlatform object.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 * @author Pim Nijdam <pim@sense-os.nl>
 */
public class MainActivity extends Activity implements ServiceConnection {
	
	/**
	 * Service stub for callbacks from the Sense service.
	 */
	private class SenseCallback extends ISenseServiceCallback.Stub {
		@Override
		public void onChangeLoginResult(int result) throws RemoteException {
			switch (result) {
			case 0:
				Log.v(TAG, "Change login OK");
                onLoggedIn();
				break;
			case -1:
				Log.v(TAG, "Login failed! Connectivity problems?");
				break;
			case -2:
				Log.v(TAG, "Login failed! Invalid username or password.");
				break;
			default:
				Log.w(TAG, "Unexpected login result! Unexpected result: " + result);
			}
		}

		@Override
		public void onRegisterResult(int result) throws RemoteException {
            // not used
		}

		@Override
		public void statusReport(final int status) {
            // not used
		}
	}

    private static final String TAG = "Sense Demo";

    private SensePlatform sensePlatform;
	private SenseCallback callback = new SenseCallback();
	

    /**
     * An example how to get data from a sensor
     */
    private void getData() {
		try {
			JSONArray data = sensePlatform.getData("position", true, 10);
            Log.v(TAG, "Received: '" + data + "'");
			
		} catch (Exception e) {
            Log.e(TAG, "Failed to get data!", e);
		}
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // trivial UI
        setContentView(R.layout.activity_main);
    }

	@Override
    protected void onDestroy() {
        // close binding with the Sense service
        // (the service will remain running on its own if it is not explicitly stopped!)
        sensePlatform.close();

        super.onDestroy();
    }

    /**
     * Callback for when the service logged in, gets called from the SenseCallback object.<br/>
     * <br/>
     * Starts sensing, and sends and gets some data. It is recommended to wait until the login has
     * finished before actually starting the sensing.
     */
    private void onLoggedIn() {
		try {
            // start sensing
			ISenseService service = sensePlatform.getService();
			service.toggleMain(true);
			service.toggleAmbience(true);
			service.toggleLocation(true);
			
			sendData();
			getData();
		} catch (Exception e) {
            Log.e(TAG, "Exception while starting sense library.", e);
		}
	}
	
    @Override
	public void onServiceConnected(ComponentName name, IBinder service) {
        // set up the sense service as soon as we are connected to it
		setupSense();
	}
	
    @Override
	public void onServiceDisconnected(ComponentName className) {
        // not used
	}

    @Override
	protected void onStart() {
		Log.v(TAG, "onStart");
		super.onStart();

        // create SensePlatform instance to do the complicated work
        // (when the service is ready, we get a call to onServiceConnected)
		sensePlatform = new SensePlatform(this, this);
	}

	/**
     * An example of how to upload data for a custom sensor.
     */
    private void sendData() {
        // Description of the sensor
		String name = "position_annotation";
		String displayName = "Annotation";
		String dataType = "json";
		String description = name;
        // the value to be sent, in json format
		String value = "{\"latitude\":\"51.903469\",\"longitude\":\"4.459865\",\"comment\":\"What a nice quiet place!\"}"; //json value
		long timestamp = System.currentTimeMillis();
		try {
			sensePlatform.addDataPoint(name, displayName, description, dataType, value, timestamp);
		} catch (Exception e) {
            Log.e(TAG, "Failed to add data point!", e);
		}
	}

	/**
     * Sets up the Sense service preferences and logs in
     */
	private void setupSense() {
		try {
            // log in (you only need to do this once, Sense will remember the login)
            sensePlatform.login("foo", SenseApi.hashPassword("bar"), callback);
            // this is an asynchronous call, we get a call to the callback object when the login is
            // complete

            // turn off some specific sensors
			ISenseService service = sensePlatform.getService();
			service.setPrefBool(Ambience.LIGHT, false);
			service.setPrefBool(Ambience.CAMERA_LIGHT, false);
			service.setPrefBool(Ambience.PRESSURE, false);

            // turn on specific sensors
			service.setPrefBool(Ambience.MIC, true);
            // NOTE: spectrum might be too heavy for the phone or consume too much energy
            service.setPrefBool(Ambience.AUDIO_SPECTRUM, true);
			service.setPrefBool(Location.GPS, true);
			service.setPrefBool(Location.NETWORK, true);
			service.setPrefBool(Location.AUTO_GPS, true);
			
            // set how often to sample
            // 1 := rarely (~every 15 min)
            // 0 := normal (~every 5 min)
            // -1 := often (~every 10 sec)
            // -2 := real time (this setting affects power consumption considerably!)
			service.setPrefString(SensePrefs.Main.SAMPLE_RATE, "0");

            // set how often to upload
            // 1 := eco mode (buffer data for 30 minutes before bulk uploading)
            // 0 := normal (buffer 5 min)
            // -1 := often (buffer 1 min)
            // -2 := real time (every new data point is uploaded immediately)
			service.setPrefString(SensePrefs.Main.SYNC_RATE, "-2");
			
		} catch (Exception e) {
            Log.e(TAG, "Exception while setting up Sense library.", e);
		}
	}
}
