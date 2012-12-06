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
import android.view.Menu;

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

    private static final String TAG = "Sense Demo";

    private SensePlatform sensePlatform;
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
    protected void onDestroy() {
        // close binding with the Sense service
        // (the service will remain running on its own if it was started!)
        sensePlatform.close();

        super.onDestroy();
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
            // login
			sensePlatform.login("foo", SenseApi.hashPassword("bar"), callback);

			//turn off specific sensors
			ISenseService service = sensePlatform.getService();
			service.setPrefBool(Ambience.LIGHT, false);
			service.setPrefBool(Ambience.CAMERA_LIGHT, false);
			service.setPrefBool(Ambience.PRESSURE, false);
			//turn on specific sensors
			service.setPrefBool(Ambience.MIC, true);
			//NOTE: spectrum might be too heavy for the phone or consume too much energy
			service.setPrefBool(Ambience.AUDIO_SPECTRUM, true);
			
			service.setPrefBool(Location.GPS, true);
			service.setPrefBool(Location.NETWORK, true);
			service.setPrefBool(Location.AUTO_GPS, true);
			
			//set how often to sample
			service.setPrefString(SensePrefs.Main.SAMPLE_RATE, "0");

			//set how often to upload
			// 0 == eco mode
			// 1 == normal (5 min)
			//-1 == often (1 min)
			//-2 == realtime
			//NOTE, this setting affects power consumption considerately!
			service.setPrefString(SensePrefs.Main.SYNC_RATE, "-2");
			
		} catch (Exception e) {
			Log.v(TAG, "Exception " + e + " while setting up sense library.");
			e.printStackTrace();
		}
	}
	
    private void loggedIn() {
		try {
			// turn it on
			ISenseService service = sensePlatform.getService();
			service.toggleMain(true);
			service.toggleAmbience(true);
			service.toggleLocation(true);
			
			sendData();
			getData();
		} catch (Exception e) {
            Log.v(TAG, "Exception " + e + " while starting sense library.", e);
		}
	}
	
    /**
     * An example of how to upload data for a custom sensor.
     */
    private void sendData() {
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
            Log.e(TAG, "Failed to add data point.", e);
		}
	}

    /**
     * An example how to get data from a sensor
     */
    private void getData() {
		try {
			JSONArray data = sensePlatform.getData("position", true, 10);
			Log.v(TAG, "Received:" + data);
			
		} catch (Exception e) {
            Log.e(TAG, "Failed to get data.", e);
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName className) {
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		setupSense();
	}
}
