/*
 ************************************************************************************************************
 *     Copyright (C)  2010 Sense Observation Systems, Rotterdam, the Netherlands.  All rights reserved.     *
 ************************************************************************************************************
 */
package nl.sense_os.service.deviceprox;

import it.gerdavax.android.bluetooth.BluetoothException;
import it.gerdavax.android.bluetooth.LocalBluetoothDevice;
import it.gerdavax.android.bluetooth.LocalBluetoothDeviceListener;
import it.gerdavax.android.bluetooth.RemoteBluetoothDevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import nl.sense_os.service.Constants;
import nl.sense_os.service.MsgHandler;

import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class BluetoothDeviceProximity {
    /*
     * Scan thread 1.6
     */
    private class ScanThread1_6 implements Runnable {

        private class BluetoothDeviceListener implements LocalBluetoothDeviceListener {

            @Override
            public void bluetoothDisabled() {
                // Auto-generated method stub
            }

            @Override
            public void bluetoothEnabled() {
                // Auto-generated method stub
            }

            @Override
            public void deviceFound(String arg0) {
                // Auto-generated method stub
            }

            @Override
            public void scanCompleted(ArrayList<String> devices) {
                if (!scanEnabled) {
                    return;
                }

                // return immediately if the BT device is closed (e.g. when the service is suddenly
                // stopped)
                if (null == btDevice) {
                    return;
                }

                try {
                    Log.d(TAG, "Bluetooth devices found: " + devices.size());

                    // array of found devices
                    for (String address : devices) {
                        RemoteBluetoothDevice rbtDevice = btDevice
                                .getRemoteBluetoothDevice(address);

                        JSONObject deviceJson = new JSONObject();
                        deviceJson.put("address", address);
                        deviceJson.put("name", rbtDevice.getName());
                        deviceJson.put("rssi", rbtDevice.getRSSI());

                        // pass device data to the MsgHandler
                        Intent i = new Intent(MsgHandler.ACTION_NEW_MSG);
                        i.putExtra(MsgHandler.KEY_SENSOR_NAME, BLUETOOTH_DISCOVERY);
                        i.putExtra(MsgHandler.KEY_VALUE, deviceJson.toString());
                        i.putExtra(MsgHandler.KEY_DATA_TYPE, Constants.SENSOR_DATA_TYPE_JSON);
                        i.putExtra(MsgHandler.KEY_TIMESTAMP, System.currentTimeMillis());
                        BluetoothDeviceProximity.this.context.startService(i);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "JSONException preparing Bluetooth sensing data:", e);
                } catch (BluetoothException e) {
                    Log.e(TAG, "BluetoothException preparing Bluetooth sensing data:", e);
                } finally {
                    stop();
                    scanHandler.postDelayed(scanThread1_6 = new ScanThread1_6(), scanInterval);
                }
            }

            @Override
            public void scanStarted() {
                // Auto-generated method stub
            }
        }

        private boolean bbActiveFromTheStart = false;
        private LocalBluetoothDevice btDevice;
        private BluetoothDeviceListener btListener;

        public ScanThread1_6() {
            // send address
            try {
                btDevice = LocalBluetoothDevice.initLocalDevice(context);

                btListener = new BluetoothDeviceListener();
                btDevice.setListener(btListener);

                bbActiveFromTheStart = btDevice.isEnabled();
                if (!btDevice.isEnabled()) {
                    btDevice.setEnabled(true);
                }
                if (btDevice.getScanMode() != LocalBluetoothDevice.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                    btDevice.setScanMode(LocalBluetoothDevice.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
                }

            } catch (Exception e) {
                Log.e(TAG, "Exception initializing Bluetooth scan thread:", e);
            }
        }

        @Override
        public void run() {
            try {
                if (btDevice != null)
                    btDevice.scan();
            } catch (Exception e) {
                Log.e(TAG, "Exception running Bluetooth scan thread:", e);
            }
        }

        public void stop() {
            try {
                btDevice.stopScanning();
                if (!bbActiveFromTheStart) {
                    btDevice.setEnabled(false);
                }
                btDevice.close();
            } catch (Exception e) {
                Log.e(TAG, "Exception in stopping Bluetooth scan thread:", e);
            }
        }
    }

    /*
     * Scan thread 2.1
     */
    private class ScanThread2_1 implements Runnable {

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
                        scanHandler.post(scanThread2_1 = new ScanThread2_1());
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
                            Intent i = new Intent(MsgHandler.ACTION_NEW_MSG);
                            i.putExtra(MsgHandler.KEY_SENSOR_NAME, BLUETOOTH_DISCOVERY);
                            i.putExtra(MsgHandler.KEY_VALUE, deviceJson.toString());
                            i.putExtra(MsgHandler.KEY_DATA_TYPE, Constants.SENSOR_DATA_TYPE_JSON);
                            i.putExtra(MsgHandler.KEY_TIMESTAMP, System.currentTimeMillis());
                            BluetoothDeviceProximity.this.context.startService(i);
                        }

                    } catch (JSONException e) {
                        Log.e(TAG, "JSONException preparing bluetooth scan data");
                    } finally {
                        stop();
                        scanHandler.postDelayed(scanThread2_1 = new ScanThread2_1(), scanInterval);
                    }
                }
            }
        };
        private Vector<Map<android.bluetooth.BluetoothDevice, Short>> deviceArray;

        public ScanThread2_1() {
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

                    Log.d(TAG, "Starting discovery");
                    btAdapter.startDiscovery();
                } else if (btAdapter.getState() == BluetoothAdapter.STATE_TURNING_ON) {
                    // listen for the adapter state to change to STATE_ON
                    context.registerReceiver(btReceiver, new IntentFilter(
                            BluetoothAdapter.ACTION_STATE_CHANGED));
                } else {
                    // ask user for permission to start bluetooth
                    Log.d(TAG, "Asking user to start bluetooth");
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
                Log.d(TAG, "Stopping BT discovery thread");
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

    private static final String BLUETOOTH_DISCOVERY = "bluetooth_discovery";
    private static final String TAG = "Bluetooth DeviceProximity";
    private BluetoothAdapter btAdapter;
    private final Context context;
    private boolean scanEnabled = false;
    private final Handler scanHandler = new Handler(Looper.getMainLooper());
    private int scanInterval = 0;
    private ScanThread1_6 scanThread1_6 = null;
    private ScanThread2_1 scanThread2_1 = null;

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
                // Check if the phone version, if it is lower than, 2.1 use the bluetooth lib
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR) {
                    scanHandler.post(scanThread1_6 = new ScanThread1_6());
                } else {
                    scanHandler.post(scanThread2_1 = new ScanThread2_1());
                }
            }
        };
        this.scanHandler.post(t);
    }

    public void stopEnvironmentScanning() {
        scanEnabled = false;
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR) {
                if (scanThread1_6 != null) {
                    scanThread1_6.stop();
                    scanHandler.removeCallbacks(scanThread1_6);
                } else if (scanThread2_1 != null) {
                    scanThread2_1.stop();
                    scanHandler.removeCallbacks(scanThread2_1);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception in stopping Bluetooth scan thread:", e);
        }
    }
}
