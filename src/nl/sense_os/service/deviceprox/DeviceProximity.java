package nl.sense_os.service.deviceprox;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import nl.sense_os.app.SenseSettings;
import nl.sense_os.service.MsgHandler;

import it.gerdavax.android.bluetooth.BluetoothDevice;
import it.gerdavax.android.bluetooth.LocalBluetoothDevice;
import it.gerdavax.android.bluetooth.LocalBluetoothDeviceListener;
import it.gerdavax.android.bluetooth.RemoteBluetoothDevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;


public class DeviceProximity {
	private static final String TAG = "DeviceProximity";
	private MsgHandler msgHandler;
	private int scanInterval = 0;
	private boolean scanEnabled = false;
	private boolean realtime = false;
	private Handler scanHandler = new Handler(Looper.getMainLooper());
	private BluetoothAdapter btAdapter;
	private Context context;
	private static final int SCAN_STARTED = 1;
	private static final int SCAN_FINISHED = 2;
	private static final int DEVICE_FOUND = 3;
	private final String LOCAL_SENSOR_NAME = "local_bt_address";
	private final String BLUETOOTH_DISCOVERY = "bluetooth_discovery";

	public DeviceProximity(MsgHandler handler, Context _context) {
		this.msgHandler = handler;
		this.context = _context;
	}

	public void startEnvironmentScanning(int interval)
	{		
		scanInterval = interval;
		realtime = scanInterval == 1;
		scanEnabled = true;
		Thread t = new Thread()
		{
			public void run() 
			{
				// Check if the phone version, if it is lower than, 2.1 use the bluetooth lib
				if(Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR)
					scanHandler.post(new ScanThread1_6());
				else
				{				
					Looper.prepare();	
					scanHandler.post(new ScanThread2_1());
					Looper.loop();
				}
			}
		};
		t.start();
	}

	public void stopEnvironmentScanning()
	{
		scanEnabled = false;
	}

	public void setScanInterval(int scanInterval) {
		this.scanInterval = scanInterval;
	}

	public int getScanInterval() {
		return scanInterval;
	}

	/*
	 *  Scan thread 2.1
	 */		
	 class ScanThread2_1 implements Runnable  
	 {		
		 
		 Vector<Map<android.bluetooth.BluetoothDevice,Short>> deviceArray;	
		
		public ScanThread2_1() 
		{ 
			// send address
			try {
				btAdapter = BluetoothAdapter.getDefaultAdapter();				

				btAdapter.enable();
				if (btAdapter.isEnabled()) 
				{						
					Log.d(TAG, "Bluetooth is enabled");
					Log.d(TAG,"My address: " + btAdapter.getAddress());
					// You don't want a dialog
					//Intent discoverableIntent = new	Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
					//discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					//discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
					//context.startActivity(discoverableIntent);
				}
				else
				{
					Log.d(TAG, "Bluetooth not enabled");
				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void run() 
		{	
			try {
				if(scanEnabled)					
				{		
					Log.d(TAG, "Starting discovery");	
					deviceArray = new Vector<Map<android.bluetooth.BluetoothDevice,Short>>();
					context.registerReceiver(bbReceiver, new  IntentFilter(android.bluetooth.BluetoothDevice.ACTION_FOUND));
					context.registerReceiver(bbReceiver, new  IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
					
					btAdapter.startDiscovery();		
				}
			} catch (Exception e) {					
				e.printStackTrace();
			}				
		}

		private BroadcastReceiver bbReceiver = new BroadcastReceiver() { 		
			
			public void onReceive(Context context, Intent intent) { 
				String action = intent.getAction(); 
				// When discovery finds a device 
				if (android.bluetooth.BluetoothDevice.ACTION_FOUND.equals(action)) 
				{ 				
					android.bluetooth.BluetoothDevice remoteDevice = intent.getParcelableExtra(android.bluetooth.BluetoothDevice.EXTRA_DEVICE);
					Short rssi =  intent.getShortExtra(android.bluetooth.BluetoothDevice.EXTRA_RSSI,(short)0);		
					HashMap<android.bluetooth.BluetoothDevice, Short> mapValue = new HashMap<android.bluetooth.BluetoothDevice, Short>();
					mapValue.put(remoteDevice, rssi);
					deviceArray.add(mapValue);
				} 
				if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) 
				{ 
					if(deviceArray.size() == 0 )
						return;
					String sensorValue = "{\"local_bt_address\":\""+btAdapter.getAddress()+"\",\"bt_devices\":[";
					String deviceJSONS = "";
					for (int i = 0; i < deviceArray.size(); i++) {
						Map<android.bluetooth.BluetoothDevice, Short> mapValue  = deviceArray.get(i);	
						android.bluetooth.BluetoothDevice btd = mapValue.entrySet().iterator().next().getKey();
						String name = btd.getName();
						String address = btd.getAddress();
						String rssi = ""+mapValue.entrySet().iterator().next().getValue();
						if(deviceJSONS.length() > 0)
							deviceJSONS += ",";
						deviceJSONS += "{\"address\":\""+address+"\",\"name\":\""+name+"\",\"rssi\":\""+rssi+"\"}";
					}					
					sensorValue += deviceJSONS+"]}";
					msgHandler.sendSensorData(BLUETOOTH_DISCOVERY, sensorValue, SenseSettings.SENSOR_DATA_TYPE_JSON);
					context.unregisterReceiver(bbReceiver);					
					scanHandler.postDelayed(new ScanThread2_1(), scanInterval);
				}
			} 
		}; 

	 }

	 /*
	  *  Scan thread 1.6
	  */
	 class ScanThread1_6 implements Runnable 
	 {
		 private BluetoothDeviceListener btListener;
		 private LocalBluetoothDevice btDevice;

		 public ScanThread1_6() 
		 { 
			 // send address
			 try {
				 btDevice =  LocalBluetoothDevice.initLocalDevice(context);
				 btListener = new BluetoothDeviceListener();
				 btDevice.setEnabled(true);
				 if (btDevice.isEnabled()) 
				 {
					 Log.d(TAG, "Bluetooth is enabled");
					 Log.d(TAG,"My address: " + btDevice.getAddress());	
					 btDevice.setScanMode(LocalBluetoothDevice.SCAN_MODE_CONNECTABLE_DISCOVERABLE);

					 if(scanEnabled)						
						 btDevice.setListener(btListener);
					 else
						 btDevice.close();
				 }
				 else
				 {
					 Log.d(TAG, "Bluetooth not enabled");
				 }

			 } catch (Exception e) {
				 // TODO Auto-generated catch block
				 e.printStackTrace();
			 }
		 }
		 public void run() 
		 {	
			 try {
				 btDevice.scan();
			 } catch (Exception e) {					
				 e.printStackTrace();
			 }

		 }

		 private class BluetoothDeviceListener implements LocalBluetoothDeviceListener {

			 public void bluetoothDisabled() {
				 // TODO Auto-generated method stub

			 }

			 
			 public void bluetoothEnabled() {
				 // TODO Auto-generated method stub

			 }

			 
			 public void deviceFound(String arg0) {
				 // TODO Auto-generated method stub

			 }

			 
			 public void scanCompleted(ArrayList<String> devices) {				
					 try {
						 if(devices.size() == 0 )
								return;
						 String sensorValue = "{\"local_bt_address\":\""+btDevice.getAddress()+"\",\"bt_devices\":[";
							String deviceJSONS = "";
							for (int i = 0; i < devices.size(); i++) {
								 String address = devices.get(i);								
								 RemoteBluetoothDevice rbtDevice = btDevice.getRemoteBluetoothDevice(address);
								 String name = rbtDevice.getName();
								 String  rssi = ""+ rbtDevice.getRSSI();
								if(deviceJSONS.length() > 0)
									deviceJSONS += ",";
								deviceJSONS += "{\"address\":\""+address+"\",\"name\":\""+name+"\",\"rssi\":\""+rssi+"\"}";
							}					
							sensorValue += deviceJSONS+"]}";							
							msgHandler.sendSensorData(BLUETOOTH_DISCOVERY, sensorValue, SenseSettings.SENSOR_DATA_TYPE_JSON);						  
					 }
					 catch (Exception e) {
						 e.printStackTrace();	                           
					 }
				 
				 scanHandler.postDelayed(new ScanThread1_6(), scanInterval);
			 }

			 
			 public void scanStarted() {
				 // TODO Auto-generated method stub

			 }
		 }
	 }
}
