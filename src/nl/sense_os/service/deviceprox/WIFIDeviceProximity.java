package nl.sense_os.service.deviceprox;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import nl.sense_os.app.SenseSettings;
import nl.sense_os.service.MsgHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class WIFIDeviceProximity {

	/*
	 * WIFI Scan thread 
	 */
	private class WifiScanThread implements Runnable {

		private boolean wifiActiveFromTheStart = false;

		private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {

			public void onReceive(Context context, Intent intent) {
				if(!scanEnabled)
					return;

				Log.d(TAG,"Scan complete");				
				// get the results of the wifi scan
				List<ScanResult> results = wifi.getScanResults();		
				if(results.size() > 0)
				{
					Log.d(TAG,"devices found:"+results.size());
					JSONObject json = new JSONObject();
					try {
						WifiInfo wifiInfo = wifi.getConnectionInfo();
						if(wifiInfo != null)
							json.put("local_wifi_device", wifiInfo.getMacAddress());
						else
							json.put("local_wifi_device", "00:00:00:00:00:00");

						JSONArray jsonArray = new JSONArray();
						for (ScanResult result : results) 
						{    							

							JSONObject deviceJson = new JSONObject();
							deviceJson.put("ssid", result.SSID);
							deviceJson.put("bssid", result.BSSID);
							deviceJson.put("frequency", result.frequency);
							deviceJson.put("rssi", result.level);
							deviceJson.put("capabilities", result.capabilities);							
							jsonArray.put(deviceJson);
						}
						json.put("wifi_devices", jsonArray);

					} catch (JSONException e) {
						Log.e(TAG, "JSONException preparing wifi scan data");
					}

					msgHandler.sendSensorData(WIFI_SCAN, json.toString(), SenseSettings.SENSOR_DATA_TYPE_JSON);

					stop();
					scanHandler.postDelayed(wifiScanThread = new WifiScanThread(), scanInterval);					
				}
			}
		};

		public WifiScanThread() {			
			try {
				wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);				
			} catch (Exception e) {
				Log.e(TAG, "Exception preparing wifi scan thread:", e);
			}
		}

		public void run() {
			if (scanEnabled) {
				if(!(wifiActiveFromTheStart = wifi.isWifiEnabled()))
				{
					wifi.setWifiEnabled(true);
					Log.d(TAG, "WIFI enabled for network scan, waiting 1 sec");
					int cnt = 0;
					try {
						while(!wifi.isWifiEnabled()  && cnt++ < 31)
						{
							Log.d(TAG, "... waiting 1 sec");
							Thread.sleep(1000); // evil but necessary
							if(cnt%10 == 0)
								wifi.setWifiEnabled(true);
						}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}					
				}
				context.registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));				
				Log.d(TAG, "Starting wifi scan");
				wifi.startScan();
			} else {
				stop();
			}
		}

		public void stop()
		{
			try
			{
				Log.d(TAG, "Stopping WIFI network scan");
				context.unregisterReceiver(wifiReceiver);				
				if(!wifiActiveFromTheStart)
					wifi.setWifiEnabled(false);
			}
			catch(Exception e)
			{
				Log.e(TAG,e.getMessage());
			}
		}

	}

	private static final String TAG = "WIFI DeviceProximity";
	private static final String WIFI_SCAN = "wifi scan";	
	private final Context context;
	private final MsgHandler msgHandler;
	private boolean scanEnabled = false;
	private final Handler scanHandler = new Handler(Looper.getMainLooper());
	private int scanInterval = 0;
	private WifiScanThread wifiScanThread = null;
	private WifiManager wifi = null;

	public WIFIDeviceProximity(MsgHandler handler, Context context) {
		this.msgHandler = handler;
		this.context = context;		
	}

	public int getScanInterval() {
		return scanInterval;
	}

	public void setScanInterval(int scanInterval) {
		this.scanInterval = scanInterval;
	}

	public void startEnvironmentScanning(int interval) {
		scanInterval = interval;
		scanEnabled = true;
		Thread t = new Thread() {
			public void run() {
				scanHandler.post(wifiScanThread = new WifiScanThread());			
			}
		};
		this.scanHandler.post(t);
	}

	public void stopEnvironmentScanning() {
		scanEnabled = false;
		try {			
			wifiScanThread.stop();
			scanHandler.removeCallbacks(wifiScanThread);			

		} catch (Exception e) {				
			Log.e(TAG, "Exception in stopping wifi scan thread:", e);
		}
	}
}
