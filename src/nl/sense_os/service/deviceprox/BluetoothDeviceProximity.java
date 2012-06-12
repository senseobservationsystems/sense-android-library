/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.deviceprox;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class BluetoothDeviceProximity {

    /**
     * Receiver for Bluetooth state broadcasts
     */
    private class BluetoothReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
	    if (!scanEnabled || null == scanThread) {
		Log.w(TAG, "Bluetooth broadcast received while sensor is disabled");
		return;
	    }

	    String action = intent.getAction();

	    if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
		onBluetoothStateChanged(intent);
	    } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
		onDeviceFound(intent);
	    } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
		onScanFinished();
	    }
	}
    }

    /**
     * Receiver for alarms to start scan
     */
    private class ScanAlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
	    if (!scanEnabled) {
		Log.w(TAG, "Bluetooth scan alarm received while sensor is disabled");
		return;
	    } else {
		// stop any old threads
		try {
		    if (null != scanThread) {
			scanThread.stop();
			scanHandler.removeCallbacks(scanThread);
		    }
		} catch (Exception e) {
		    Log.e(TAG, "Exception clearing old bluetooth scan threads. " + e);
		}
		// start new scan
		scanHandler.post(scanThread = new ScanThread());
	    }
	}
    }

    /**
     * Bluetooth discovery thread
     */
    private class ScanThread implements Runnable {

	// private boolean btActiveFromTheStart = false; // removed

	private Vector<Map<BluetoothDevice, Short>> deviceArray;

	public ScanThread() {
	    // send address
	    try {
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		// btActiveFromTheStart = btAdapter.isEnabled();
	    } catch (Exception e) {
		Log.e(TAG, "Exception preparing Bluetooth scan thread:", e);
	    }
	}

	@Override
	public void run() {
	    Log.d(TAG, "Run Bluetooth discovery thread");
	    if (scanEnabled) {
		if (btAdapter.isEnabled()) {
		    // start discovery
		    deviceArray = new Vector<Map<BluetoothDevice, Short>>();
		    context.registerReceiver(btReceiver, new IntentFilter(
			    BluetoothDevice.ACTION_FOUND));
		    context.registerReceiver(btReceiver, new IntentFilter(
			    BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

		    Log.i(TAG, "Starting Bluetooth discovery");
		    btAdapter.startDiscovery();
		} else if (btAdapter.getState() == BluetoothAdapter.STATE_TURNING_ON) {
		    // listen for the adapter state to change to STATE_ON
		    context.registerReceiver(btReceiver, new IntentFilter(
			    BluetoothAdapter.ACTION_STATE_CHANGED));
		} else {
		    // ask user for permission to start bluetooth
		    // Log.v(TAG, "Asking user to start bluetooth");
		    Intent startBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startBt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		    context.startActivity(startBt);

		    // listen for the adapter state to change to STATE_ON
		    context.registerReceiver(btReceiver, new IntentFilter(
			    BluetoothAdapter.ACTION_STATE_CHANGED));
		}
	    } else {
		stop();
	    }
	}

	public void stop() {
	    try {
		Log.i(TAG, "Stopping Bluetooth discovery thread");
		context.unregisterReceiver(btReceiver);
		btAdapter.cancelDiscovery();
		/*
		 * do not have to switch off the bluetooth anymore because we ask the user
		 * explicitly
		 */
		// if (!btActiveFromTheStart) { btAdapter.disable(); }
	    } catch (Exception e) {
		Log.e(TAG, "Error in stopping BT discovery:" + e.getMessage());
	    }
	}
    }

    private static final String TAG = "Bluetooth DeviceProximity";
    private static final int REQ_CODE = 333;
    private final BluetoothReceiver btReceiver = new BluetoothReceiver();
    private final ScanAlarmReceiver alarmReceiver = new ScanAlarmReceiver();
    private BluetoothAdapter btAdapter;
    private final Context context;
    private boolean scanEnabled = false;
    private final Handler scanHandler = new Handler(Looper.getMainLooper());
    private int scanInterval = 0;
    private ScanThread scanThread = null;

    public BluetoothDeviceProximity(Context context) {
	this.context = context;
    }

    public int getScanInterval() {
	return scanInterval;
    }

    private void onDeviceFound(Intent intent) {
	BluetoothDevice remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	Short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) 0);
	HashMap<BluetoothDevice, Short> mapValue = new HashMap<BluetoothDevice, Short>();
	mapValue.put(remoteDevice, rssi);
	scanThread.deviceArray.add(mapValue);
    }

    private void onScanFinished() {
	try {
	    for (Map<BluetoothDevice, Short> value : scanThread.deviceArray) {
		BluetoothDevice btd = value.entrySet().iterator().next().getKey();

		JSONObject deviceJson = new JSONObject();
		deviceJson.put("address", btd.getAddress());
		deviceJson.put("name", btd.getName());
		deviceJson.put("rssi", value.entrySet().iterator().next().getValue());

		// pass message to the MsgHandler
		Intent i = new Intent(context.getString(R.string.action_sense_new_data));
		i.putExtra(DataPoint.SENSOR_NAME, SensorNames.BLUETOOTH_DISCOVERY);
		i.putExtra(DataPoint.VALUE, deviceJson.toString());
		i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
		i.putExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime());
		BluetoothDeviceProximity.this.context.startService(i);
	    }

	    // add count of bluetooth devices as a separate sensor value
	    int nrBluetoothNeighbours = scanThread.deviceArray.size();

	    Intent i = new Intent(context.getString(R.string.action_sense_new_data));
	    i.putExtra(DataPoint.SENSOR_NAME, SensorNames.BLUETOOTH_NEIGHBOURS_COUNT);
	    i.putExtra(DataPoint.VALUE, nrBluetoothNeighbours);
	    i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.INT);
	    i.putExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime());
	    BluetoothDeviceProximity.this.context.startService(i);
	    Log.v(TAG, "Found " + nrBluetoothNeighbours + " bluetooth neighbours");

	} catch (JSONException e) {
	    Log.e(TAG, "JSONException preparing bluetooth scan data");
	} finally {
	    scanThread.stop();
	    scanHandler.postDelayed(scanThread = new ScanThread(), scanInterval);
	}
    }

    private void onBluetoothStateChanged(Intent intent) {
	int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
	if (state == BluetoothAdapter.STATE_ON) {
	    scanThread.stop();
	    scanHandler.post(scanThread = new ScanThread());
	}
    }

    public void setScanInterval(int scanInterval) {
	this.scanInterval = scanInterval;
    }

    public void startEnvironmentScanning(int interval) {
	scanInterval = interval;
	scanEnabled = true;

	// register receiver for scan alarms
	String action = "nl.sense_os.app.bluetooth.SCAN";
	context.registerReceiver(alarmReceiver, new IntentFilter(action));

	Intent intent = new Intent(action);
	PendingIntent operation = PendingIntent.getBroadcast(context, REQ_CODE, intent, 0);
	AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	am.cancel(operation);
	am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), scanInterval,
		operation);
    }

    public void stopEnvironmentScanning() {
	scanEnabled = false;

	// stop the thread if it is running
	try {
	    if (scanThread != null) {
		scanThread.stop();
		scanHandler.removeCallbacks(scanThread);
	    }
	} catch (Exception e) {
	    Log.e(TAG, "Exception in stopping Bluetooth scan thread:", e);
	}

	// cancel the alarms
	String action = "nl.sense_os.app.bluetooth.SCAN";
	Intent intent = new Intent(action);
	PendingIntent operation = PendingIntent.getBroadcast(context, REQ_CODE, intent, 0);
	AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	am.cancel(operation);

	// unregister the receiver
	context.unregisterReceiver(alarmReceiver);
    }
}
