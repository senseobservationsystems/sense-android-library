package nl.sense.demo;

import org.json.JSONArray;

import android.os.Bundle;
import android.app.Activity;
import android.content.ComponentName;
import android.util.Log;
import android.view.Menu;
import nl.sense.demo.R;

//import bunch of sense library stuff
import nl.sense_os.platform.SensePlatform;
import nl.sense_os.platform.ServiceConnectionEventHandler;
import nl.sense_os.service.ISenseService;
import nl.sense_os.service.ISenseServiceCallback;
import android.os.RemoteException;
import nl.sense_os.service.commonsense.SenseApi;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main.Ambience;
import nl.sense_os.service.constants.SensePrefs.Main.Location;

public class MainActivity extends Activity implements ServiceConnectionEventHandler {
	private static final String TAG = "Sense Demo"; 
	private SensePlatform sensePlatform;
	
	/**
	 * Service stub for callbacks from the Sense service.
	 */
	private class SenseCallback extends ISenseServiceCallback.Stub {
		@Override
		public void onChangeLoginResult(int result) throws RemoteException {
			switch (result) {
			case 0:
				Log.v(TAG, "Change login OK");
				loggedIn();
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
		}

		@Override
		public void statusReport(final int status) {
		}
	}

	private SenseCallback callback = new SenseCallback();
	

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
	@Override
	protected void onStart() {
		Log.v(TAG, "onStart");
		super.onStart();
		sensePlatform = new SensePlatform(this, this);
	}

	@Override
	protected void onStop() {
		Log.v(TAG, "onStop");
		super.onStop();
	}

	private void setupSense() {
		try {
			sensePlatform.login("foo", "bar", callback);
			//turn off specific sensors
			sensePlatform.service().setPrefBool(Ambience.LIGHT, false);
			sensePlatform.service().setPrefBool(Ambience.CAMERA_LIGHT, false);
			sensePlatform.service().setPrefBool(Ambience.PRESSURE, false);
			//turn on specific sensors
			sensePlatform.service().setPrefBool(Ambience.MIC, true);
			//NOTE: spectrum might be too heavy for the phone or consume too much energy
			sensePlatform.service().setPrefBool(Ambience.AUDIO_SPECTRUM, true);
			
			sensePlatform.service().setPrefBool(Location.GPS, true);
			sensePlatform.service().setPrefBool(Location.NETWORK, true);
			sensePlatform.service().setPrefBool(Location.AUTO_GPS, true);
			
			//set how often to sample
			sensePlatform.service().setPrefString(SensePrefs.Main.SAMPLE_RATE, "0");

			//set how often to upload
			// 0 == eco mode
			// 1 == normal (5 min)
			//-1 == often (1 min)
			//-2 == realtime
			//NOTE, this setting affects power consumption considerately!
			sensePlatform.service().setPrefString(SensePrefs.Main.SYNC_RATE, "-2");
			
		} catch (Exception e) {
			Log.v(TAG, "Exception " + e + " while setting up sense library.");
			e.printStackTrace();
		}
	}
	
	void loggedIn() {
		try {
			// turn it on
			sensePlatform.service().toggleMain(true);
			sensePlatform.service().toggleAmbience(true);
			sensePlatform.service().toggleLocation(true);
		} catch (Exception e) {
			Log.v(TAG, "Exception " + e + " while starting sense library.");
			e.printStackTrace();
		}
	}
	
	/** An example of how to upload data for a custom sensor.
	 */
	void sendData() {
		//Description of the sensor
		String name = "position_annotation";
		String displayName = "Annotation";
		String dataType = "json";
		String description = name;
		//the value to be sent, in json format
		String value = "{\"latitude\":\"51.903469\",\"longitude\":\"4.459865\",\"comment\":\"What a nice quiet place!\"}"; //json value
		long timestamp = System.currentTimeMillis();
		try {
			sensePlatform.addDataPoint(name, displayName, description, dataType, value, timestamp);
		} catch (Exception e) {
			Log.e(TAG, "Failed to add data point.");
			e.printStackTrace();
		}
	}

	/** An example how to get data from a sensor
	 * 
	 */
	void getData() {
		try {
			JSONArray data = sensePlatform.getData("position", true, 10);
			Log.v(TAG, "Received:" + data);
			
		} catch (Exception e) {
			Log.e(TAG, "Failed to get data.");
			e.printStackTrace();
		}
	}

	@Override
	public void onServiceConnected(ComponentName className, ISenseService service) {
		setupSense();
		sendData();
		//TODO: it seems this request is performed too early?
		getData();
	}

	@Override
	public void onServiceDisconnected(ComponentName className) {
	}
}
