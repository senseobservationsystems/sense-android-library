package nl.sense.demo;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.Menu;
import nl.sense.demo.R;

//import bunch of sense library stuff
import nl.sense_os.service.ISenseService;
import nl.sense_os.service.ISenseServiceCallback;
import nl.sense_os.service.SenseService;
import nl.sense_os.service.commonsense.SenseApi;
import nl.sense_os.service.commonsense.SensorRegistrator;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main.Ambience;
import nl.sense_os.service.constants.SensePrefs.Main.Location;
import nl.sense_os.service.constants.SensorData.DataPoint;

public class MainActivity extends Activity {
	
	/**
	 * Service stub for callbacks from the Sense service.
	 */
	private class SenseCallback extends ISenseServiceCallback.Stub {

		@Override
		public void onChangeLoginResult(int result) throws RemoteException {
			Log.v(TAG, "Login changed with result " + result);
			sendData();
		}

		@Override
		public void onRegisterResult(int result) throws RemoteException {
			// not used
		}

		@Override
		public void statusReport(final int status) {
			// Log.v(TAG, "Received status report from Sense Platform service...");
		}
	}

	/**
	 * Service connection to handle connection with the Sense service. Manages the
	 * <code>service</code> field when the service is connected or disconnected.
	 */
	private class SenseServiceConn implements ServiceConnection {

		@Override
		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.v(TAG, "Bound to Sense Platform service...");

			service = ISenseService.Stub.asInterface(binder);
			try {
				service.getStatus(callback);
				setupSense();
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
	}

	/**
	 * Receiver for broadcast events from the Sense Service, e.g. when the status of the service
	 * changes.
	 */
	private class SenseServiceListener extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
		}
	}

	private static final String TAG = "SenseDemo";

	private final ISenseServiceCallback callback = new SenseCallback();
	private boolean isServiceBound;
	private ISenseService service;
	private final ServiceConnection serviceConn = new SenseServiceConn();
	private final SenseServiceListener serviceListener = new SenseServiceListener();
	private final SensorRegistrator sensorRegistrator = new SensorRegistrator(this) {
		
		@Override
		public boolean verifySensorIds(String deviceType, String deviceUuid) {
			return false;
		}
	};

	/**
	 * Binds to the Sense Service, creating it if necessary.
	 */
	private void bindToSenseService() {
		// start the service if it was not running already
		if (!isServiceBound) {
			Log.v(TAG, "Try to bind to Sense Platform service");
			final Intent serviceIntent = new Intent(getString(R.string.action_sense_service));
			
			isServiceBound = bindService(serviceIntent, serviceConn, BIND_AUTO_CREATE);
			Log.v(TAG, "Result: " + isServiceBound);
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
			Log.v(TAG, "Not bound to Sense Platform service! Assume it's not running...");
		}
	}


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

		// bind to service as soon as possible
		bindToSenseService();

		// register receiver for updates
		IntentFilter filter = new IntentFilter(SenseService.ACTION_SERVICE_BROADCAST);
		registerReceiver(serviceListener, filter);

		checkServiceStatus();
	}

	@Override
	protected void onStop() {
		// Log.v(TAG, "onStop");

		// unregister service state listener
		try {
			unregisterReceiver(serviceListener);
		} catch (IllegalArgumentException e) {
			// listener was not registered
		}

		super.onStop();
	}
	
	private void setupSense() {
		Log.v(TAG, "setupSense()");
		try {
			service.changeLogin("foo", SenseApi.hashPassword("bar"), callback);

			//turn off specific sensors
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
			
			//and turn it on
			service.toggleMain(true);
			service.toggleAmbience(true);
			service.toggleLocation(true);
		} catch (Exception e) {
			Log.v(TAG, "Exception " + e + " while setting up sense library.");
			e.printStackTrace();
		}
	}
	
	/** An example of how to upload data for a custom sensor
	 */
	void sendData() {
		//Description of the sensor
		String name = "position_annotation";
		String displayName = "Annotation";
		String dataType = "json";
		String description = name;
		//the value to be sent, in json format
		String value = "{\"latitude\":\"51.903469\",\"longitude\":\"4.459865\",\"comment\":\"What a nice quiet place!\"}"; //json value
		//register the sensor
		sensorRegistrator.checkSensor(name, displayName, dataType, description, value, null, null);
		long timestamp = System.currentTimeMillis();

		// send data point
		String action = getString(nl.sense_os.service.R.string.action_sense_new_data);
		Intent intent = new Intent(action);
		intent.putExtra(DataPoint.SENSOR_NAME, name);
		intent.putExtra(DataPoint.DISPLAY_NAME, displayName);
		intent.putExtra(DataPoint.SENSOR_DESCRIPTION, description);
		intent.putExtra(DataPoint.DATA_TYPE, dataType);
		intent.putExtra(DataPoint.VALUE, value);
		intent.putExtra(DataPoint.TIMESTAMP, timestamp);
		startService(intent);
		Log.v(TAG, "Send data point: " + value);
	}
}
