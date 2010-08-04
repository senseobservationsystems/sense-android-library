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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import it.gerdavax.android.bluetooth.BluetoothException;
import it.gerdavax.android.bluetooth.LocalBluetoothDevice;
import it.gerdavax.android.bluetooth.LocalBluetoothDeviceListener;
import it.gerdavax.android.bluetooth.RemoteBluetoothDevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class DeviceProximity {
    /*
     * Scan thread 1.6
     */
    private class ScanThread1_6 implements Runnable {
        private class BluetoothDeviceListener implements LocalBluetoothDeviceListener {

            public void bluetoothDisabled() {
                // Auto-generated method stub
            }

            public void bluetoothEnabled() {
                // Auto-generated method stub
            }

            public void deviceFound(String arg0) {
                // Auto-generated method stub
            }

            public void scanCompleted(ArrayList<String> devices) {
                // return immediately if the BT device is closed (i.e. when the service is suddenly stopped)
                if (null == btDevice) {
                    return;
                }
                
                try {
                    if (devices.size() == 0)
                        return;

                    JSONObject json = new JSONObject();

                    // local bluetooth address property
                    json.put("local_bt_address", btDevice.getAddress());

                    // array of found devices
                    JSONArray deviceArray = new JSONArray();
                    for (String address : devices) {                        
                        RemoteBluetoothDevice rbtDevice = btDevice
                                .getRemoteBluetoothDevice(address);

                        JSONObject deviceJson = new JSONObject();
                        deviceJson.put("address", address);
                        deviceJson.put("name", rbtDevice.getName());
                        deviceJson.put("rssi", rbtDevice.getRSSI());
                        deviceArray.put(deviceJson);
                    }
                    json.put("bt_devices", deviceArray);

                    msgHandler.sendSensorData(BLUETOOTH_DISCOVERY, json.toString(),
                            SenseSettings.SENSOR_DATA_TYPE_JSON);
                } catch (JSONException e) {
                    Log.e(TAG, "JSONException preparing Bluetooth sensing data:", e);
                } catch (BluetoothException e) {
                    Log.e(TAG, "BluetoothException preparing Bluetooth sensing data:", e);
                }

                scanHandler.postDelayed(new ScanThread1_6(), scanInterval);
            }

            public void scanStarted() {
                // Auto-generated method stub
            }
        }

        private LocalBluetoothDevice btDevice;
        private BluetoothDeviceListener btListener;

        public ScanThread1_6() {
            // send address
            try {
                btDevice = LocalBluetoothDevice.initLocalDevice(context);
                btListener = new BluetoothDeviceListener();
                btDevice.setEnabled(true);
                if (btDevice.isEnabled()) {
                    // Log.d(TAG, "Bluetooth is enabled");
                    // Log.d(TAG, "My address: " + btDevice.getAddress());
                    btDevice.setScanMode(LocalBluetoothDevice.SCAN_MODE_CONNECTABLE_DISCOVERABLE);

                    if (scanEnabled)
                        btDevice.setListener(btListener);
                    else
                        btDevice.close();
                } else {
                    // Log.d(TAG, "Bluetooth not enabled");
                }

            } catch (Exception e) {
                Log.e(TAG, "Exception initializing Bluetooth scan thread:", e);
            }
        }

        public void run() {
            try {
            	if(btDevice != null)
            		btDevice.scan();
            } catch (Exception e) {
                Log.e(TAG, "Exception running Bluetooth scan thread:", e);
            }
        }
    }

    /*
     * Scan thread 2.1
     */
    private class ScanThread2_1 implements Runnable {

        private BroadcastReceiver bbReceiver = new BroadcastReceiver() {

            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
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
                    if (deviceArray.size() == 0)
                        return;

                    JSONObject json = new JSONObject();
                    try {
                        json.put("local_bt_device", btAdapter.getAddress());

                        JSONArray jsonArray = new JSONArray();
                        for (Map<android.bluetooth.BluetoothDevice, Short> value : deviceArray) {
                            android.bluetooth.BluetoothDevice btd = value.entrySet().iterator()
                                    .next().getKey();

                            JSONObject deviceJson = new JSONObject();
                            deviceJson.put("address", btd.getAddress());
                            deviceJson.put("name", btd.getName());
                            deviceJson.put("rssi", ""
                                    + value.entrySet().iterator().next().getValue());
                            jsonArray.put(deviceJson);
                        }
                        json.put("bt_devices", jsonArray);

                    } catch (JSONException e) {
                        Log.e(TAG, "JSONException preparing bluetooth scan data");
                    }

                    msgHandler.sendSensorData(BLUETOOTH_DISCOVERY, json.toString(),
                            SenseSettings.SENSOR_DATA_TYPE_JSON);

                    context.unregisterReceiver(bbReceiver);
                    scanHandler.postDelayed(new ScanThread2_1(), scanInterval);
                }
            }
        };

        Vector<Map<android.bluetooth.BluetoothDevice, Short>> deviceArray;

        public ScanThread2_1() {
            // send address
            try {
                btAdapter = BluetoothAdapter.getDefaultAdapter();
                btAdapter.enable();
                
                if (btAdapter.isEnabled()) {
                    // Log.d(TAG, "Bluetooth is enabled");
                    // Log.d(TAG, "My address: " + btAdapter.getAddress());

                    /* You don't want a dialog */
                    // Intent discoverableIntent = new
                    // Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    // discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    // discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
                    // 300);
                    // context.startActivity(discoverableIntent);
                } else {
                    // Log.d(TAG, "Bluetooth not enabled");
                }

            } catch (Exception e) {
                Log.e(TAG, "JSONException preparing Bluetooth scan thread:", e);
            }
        }

        public void run() {
            if (scanEnabled) {
                // Log.d(TAG, "Starting discovery");
                deviceArray = new Vector<Map<android.bluetooth.BluetoothDevice, Short>>();
                context.registerReceiver(bbReceiver, new IntentFilter(
                        android.bluetooth.BluetoothDevice.ACTION_FOUND));
                context.registerReceiver(bbReceiver, new IntentFilter(
                        BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

                btAdapter.startDiscovery();
            } else {
                // do nothing
            }
        }
    }

    private static final String TAG = "Sense DeviceProximity";
    private static final String BLUETOOTH_DISCOVERY = "bluetooth_discovery";
    private BluetoothAdapter btAdapter;
    private final Context context;
    private boolean isRealtime = false;
    private final MsgHandler msgHandler;
    private boolean scanEnabled = false;
    private final Handler scanHandler = new Handler(Looper.getMainLooper());
    private int scanInterval = 0;

    public DeviceProximity(MsgHandler handler, Context context) {
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
        isRealtime = scanInterval == 1;
        scanEnabled = true;
        Thread t = new Thread() {
            private final Handler handler = new Handler(Looper.getMainLooper());

            public void run() {
                // Check if the phone version, if it is lower than, 2.1 use the bluetooth lib
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR)
                    this.handler.post(new ScanThread1_6());
                else {
                    this.handler.post(new ScanThread2_1());
                }
            }
        };
        this.scanHandler.post(t);
    }

    public void stopEnvironmentScanning() {
        scanEnabled = false;
    }
}
