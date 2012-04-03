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

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class BluetoothDeviceProximity {

    /*
     * Scan thread
     */
    private class ScanThread implements Runnable {

        // private boolean btActiveFromTheStart = false; // removed
        private BroadcastReceiver btReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (!scanEnabled) {
                    return;
                }

                String action = intent.getAction();

                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.STATE_OFF);
                    if (state == BluetoothAdapter.STATE_ON) {
                        stop();
                        scanHandler.post(scanThread = new ScanThread());
                        return;
                    }
                }

                // When discovery finds a device
                if (android.bluetooth.BluetoothDevice.ACTION_FOUND.equals(action)) {
                    android.bluetooth.BluetoothDevice remoteDevice = intent
                            .getParcelableExtra(android.bluetooth.BluetoothDevice.EXTRA_DEVICE);
                    Short rssi = intent.getShortExtra(android.bluetooth.BluetoothDevice.EXTRA_RSSI,
                            (short) 0);
                    HashMap<android.bluetooth.BluetoothDevice, Short> mapValue = new HashMap<android.bluetooth.BluetoothDevice, Short>();
                    mapValue.put(remoteDevice, rssi);
                    deviceArray.add(mapValue);
                }

                if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    try {
                        for (Map<android.bluetooth.BluetoothDevice, Short> value : deviceArray) {
                            android.bluetooth.BluetoothDevice btd = value.entrySet().iterator()
                                    .next().getKey();

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
                        int nrBluetoothNeighbours = deviceArray.size();

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
                        stop();
                        scanHandler.postDelayed(scanThread = new ScanThread(), scanInterval);
                    }
                }
            }
        };
        private Vector<Map<android.bluetooth.BluetoothDevice, Short>> deviceArray;

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
            if (scanEnabled) {
                if (btAdapter.isEnabled()) {
                    // start discovery
                    deviceArray = new Vector<Map<android.bluetooth.BluetoothDevice, Short>>();
                    context.registerReceiver(btReceiver, new IntentFilter(
                            android.bluetooth.BluetoothDevice.ACTION_FOUND));
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

    public void setScanInterval(int scanInterval) {
        this.scanInterval = scanInterval;
    }

    public void startEnvironmentScanning(int interval) {
        scanInterval = interval;
        scanEnabled = true;

        Thread t = new Thread() {

            @Override
            public void run() {
                scanHandler.post(scanThread = new ScanThread());
            }
        };
        scanHandler.post(t);
    }

    public void stopEnvironmentScanning() {
        scanEnabled = false;
        try {
            if (scanThread != null) {
                scanThread.stop();
                scanHandler.removeCallbacks(scanThread);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in stopping Bluetooth scan thread:", e);
        }
    }
}
