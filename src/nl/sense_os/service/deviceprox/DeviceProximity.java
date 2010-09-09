package nl.sense_os.service.deviceprox;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import nl.sense_os.app.SenseSettings;
import nl.sense_os.service.MsgHandler;

public class DeviceProximity {

	private Context context;
	private boolean scanEnabled = false;
	private BluetoothDeviceProximity bluetoothDP;
	private WIFIDeviceProximity wifiDP;
	private boolean btEnabled;
	private boolean wifiEnabled;
	
	private int scanInterval = 0;

	public DeviceProximity(MsgHandler handler, Context context) 
	{
		this.context 	= context;
		bluetoothDP 	= new BluetoothDeviceProximity(handler, context);
		wifiDP 			= new WIFIDeviceProximity(handler, context);
	}

	public int getScanInterval() {
		return scanInterval;
	}

	public void setScanInterval(int scanInterval) {
		this.scanInterval = scanInterval;
	}

	public boolean getScanEnabled()
	{
		return scanEnabled;
	}
	
	public void startEnvironmentScanning(int interval) {
		scanInterval = interval;		
		scanEnabled = true;
		
		 final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
         if(btEnabled = prefs.getBoolean(SenseSettings.PREF_PROXIMITY_BT, true))
        	 bluetoothDP.startEnvironmentScanning(interval);
         if(wifiEnabled = prefs.getBoolean(SenseSettings.PREF_PROXIMITY_WIFI, true))
        	 wifiDP.startEnvironmentScanning(interval);
	}

	public void stopEnvironmentScanning() {
		scanEnabled = false;
		if(btEnabled)
			bluetoothDP.stopEnvironmentScanning();
		if(wifiEnabled)
			wifiDP.stopEnvironmentScanning();
	}
}
