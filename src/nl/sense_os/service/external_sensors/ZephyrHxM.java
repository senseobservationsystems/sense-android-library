/*
 ************************************************************************************************************
 *     Copyright (C)  2010 Sense Observation Systems, Rotterdam, the Netherlands.  All rights reserved.     *
 ************************************************************************************************************
 */
package nl.sense_os.service.external_sensors;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import nl.sense_os.service.Constants;
import nl.sense_os.service.MsgHandler;

import it.gerdavax.android.bluetooth.LocalBluetoothDevice;
import it.gerdavax.android.bluetooth.LocalBluetoothDeviceListener;
import it.gerdavax.android.bluetooth.RemoteBluetoothDevice;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class ZephyrHxM {

    /*
     * Process message class
     */
    private class ProcessZephyrHxMMessage {
        private String deviceName;
        private SharedPreferences prefs = null;

        public ProcessZephyrHxMMessage(String deviceName) {
            this.deviceName = deviceName;
            this.prefs = context.getSharedPreferences(Constants.MAIN_PREFS,
                    Context.MODE_WORLD_WRITEABLE);
        }

        public void processMessage(byte[] buffer) {
            try {
                // received general data
                if (buffer[0] == 0x02 && buffer[1] == 0x26 && buffer[2] == 55) {
                    // send heart rate
                    if (prefs.getBoolean(Constants.PREF_HXM_HEART_RATE, true)) {
                        int heartRate = Byte.valueOf(buffer[12]).intValue();
                        if (heartRate < 0)
                            heartRate = (heartRate + 255);

                        Log.d(TAG, "Heart rate:" + heartRate);
                        Intent heartRateIntent = new Intent(MsgHandler.ACTION_NEW_MSG);
                        heartRateIntent.putExtra(MsgHandler.KEY_SENSOR_NAME, "heart rate");
                        heartRateIntent.putExtra(MsgHandler.KEY_SENSOR_DEVICE, "HxM " + deviceName);
                        heartRateIntent.putExtra(MsgHandler.KEY_VALUE, heartRate);
                        heartRateIntent.putExtra(MsgHandler.KEY_DATA_TYPE,
                                Constants.SENSOR_DATA_TYPE_INT);
                        heartRateIntent.putExtra(MsgHandler.KEY_TIMESTAMP,
                                System.currentTimeMillis());
                        context.startService(heartRateIntent);
                    }
                    // send speed
                    if (prefs.getBoolean(Constants.PREF_HXM_SPEED, true)) {
                        int speed = (((int) buffer[52]) | (((int) buffer[53]) << 8));
                        float speedF = (float) speed / 256f;

                        Log.d(TAG, "Speed:" + speedF);
                        Intent heartRateIntent = new Intent(MsgHandler.ACTION_NEW_MSG);
                        heartRateIntent.putExtra(MsgHandler.KEY_SENSOR_NAME, "speed");
                        heartRateIntent.putExtra(MsgHandler.KEY_SENSOR_DEVICE, "HxM " + deviceName);
                        heartRateIntent.putExtra(MsgHandler.KEY_VALUE, speedF);
                        heartRateIntent.putExtra(MsgHandler.KEY_DATA_TYPE,
                                Constants.SENSOR_DATA_TYPE_FLOAT);
                        heartRateIntent.putExtra(MsgHandler.KEY_TIMESTAMP,
                                System.currentTimeMillis());
                        context.startService(heartRateIntent);
                    }
                    // send distance
                    if (prefs.getBoolean(Constants.PREF_HXM_DISTANCE, true)) {
                        int distance = (((int) buffer[50]) | (((int) buffer[51]) << 8));
                        float distanceF = (float) distance / 16F;

                        Log.d(TAG, "Distance:" + distanceF);
                        Intent heartRateIntent = new Intent(MsgHandler.ACTION_NEW_MSG);
                        heartRateIntent.putExtra(MsgHandler.KEY_SENSOR_NAME, "distance");
                        heartRateIntent.putExtra(MsgHandler.KEY_SENSOR_DEVICE, "HxM " + deviceName);
                        heartRateIntent.putExtra(MsgHandler.KEY_VALUE, distanceF);
                        heartRateIntent.putExtra(MsgHandler.KEY_DATA_TYPE,
                                Constants.SENSOR_DATA_TYPE_FLOAT);
                        heartRateIntent.putExtra(MsgHandler.KEY_TIMESTAMP,
                                System.currentTimeMillis());
                        context.startService(heartRateIntent);
                    }
                    // send battery charge
                    if (prefs.getBoolean(Constants.PREF_HXM_BATTERY, true)) {
                        Short battery = (short) buffer[11];

                        Log.d(TAG, "Battery charge:" + battery.intValue());
                        Intent heartRateIntent = new Intent(MsgHandler.ACTION_NEW_MSG);
                        heartRateIntent.putExtra(MsgHandler.KEY_SENSOR_NAME, "battery charge");
                        heartRateIntent.putExtra(MsgHandler.KEY_SENSOR_DEVICE, "HxM " + deviceName);
                        heartRateIntent.putExtra(MsgHandler.KEY_VALUE, battery.intValue());
                        heartRateIntent.putExtra(MsgHandler.KEY_DATA_TYPE,
                                Constants.SENSOR_DATA_TYPE_INT);
                        heartRateIntent.putExtra(MsgHandler.KEY_TIMESTAMP,
                                System.currentTimeMillis());
                        context.startService(heartRateIntent);
                    }

                }

            } catch (Exception e) {
                Log.e(TAG, "Error in ProcessZephyrHxMMessage:" + e.getMessage());
            }
        }
    }

    /*
     * Update thread
     */
    private class UpdateThread extends Thread {

        private InputStream mmInStream;
        private OutputStream mmOutStream;

        public UpdateThread() {

            if (btSocket1_6 == null) {
                if (mmInStream == null || mmOutStream == null) {
                    try {
                        mmInStream = btSocket2_1.getInputStream();
                        mmOutStream = btSocket2_1.getOutputStream();
                    } catch (Exception e) {
                        Log.d(TAG, "Error in update thread constructor:" + e.getMessage());
                    }
                }
            } else {
                try {

                    if (mmInStream == null || mmOutStream == null) {
                        mmInStream = btSocket1_6.getInputStream();
                        mmOutStream = btSocket1_6.getOutputStream();
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Error in update thread constructor:" + e.getMessage());
                }
            }
        }

        @Override
        public void run() {
            // Keep listening to the InputStream until an exception occurs
            if (hxmEnabled) {
                try {
                    if (System.currentTimeMillis() > lastSampleTime + updateInterval) {
                        lastSampleTime = System.currentTimeMillis();
                        // Read from the InputStream
                        byte[] buffer = new byte[80];
                        int bytes; // bytes returned from read()
                        bytes = mmInStream.read(buffer);
                        if (bytes > 0)
                            processZHxMMessage.processMessage(buffer);
                        buffer = null;
                    }

                    if (btSocket1_6 == null)
                        updateHandler.postDelayed(updateThread = new UpdateThread(), 1000);
                    else
                        updateHandler.postDelayed(updateThread = new UpdateThread(), 1000);
                } catch (Exception e) {
                    Log.e(TAG, "Error in receiving HxM data:" + e.getMessage());
                    e.printStackTrace();
                    // re-connect
                    processZHxMMessage = null;
                    if (btSocket1_6 == null)
                        connectHandler.post(hxmConnectThread2_1 = new HxMConnectThread2_1());
                    else
                        connectHandler.post(hxmConnectThread1_6 = new HxMConnectThread1_6());
                    btSocket2_1 = null;
                    btSocket1_6 = null;
                }
            } else
                cancel();
        }

        /* Call this from the main Activity to shutdown the connection */
        public void cancel() {
            try {
                Log.d(TAG, "Stopping the HxM service");
                processZHxMMessage = null;
                if (btSocket1_6 == null) {
                    btSocket2_1.close();
                    btSocket2_1 = null;
                } else {
                    btSocket1_6.closeSocket();
                    btSocket1_6 = null;
                }

            } catch (Exception e) {
                Log.d(TAG, "Error in stopping the servicve:" + e.getMessage());
            }
        }
    }

    /*
     * Scan thread 1.6
     */
    private class HxMConnectThread1_6 implements Runnable {

        @SuppressWarnings("unused")
        private boolean bbActiveFromTheStart = false; // TODO use this

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
                boolean foundDevice = false;
                if (!hxmEnabled) {
                    stop();
                    return;
                }
                // return immediately if the BT device is closed (i.e. when the service is suddenly
                // stopped)
                if (null == btDevice) {
                    return;
                }

                try {
                    if (devices.size() != 0) {
                        // find a paired HxM

                        for (int x = 0; x < devices.size(); ++x) {
                            RemoteBluetoothDevice rbtDevice = btDevice
                                    .getRemoteBluetoothDevice(devices.get(x));
                            if (rbtDevice.isPaired() && rbtDevice.getName().startsWith("HXM")
                                    && rbtDevice.getAddress().startsWith("00:07:80")) {
                                btSocket1_6 = rbtDevice.openSocket(1);
                                processZHxMMessage = new ProcessZephyrHxMMessage(btSocket1_6
                                        .getRemoteBluetoothDevice().getName());
                                updateHandler.post(updateThread = new UpdateThread());
                                foundDevice = true;
                                break;
                            }
                        }
                        // connect to a unpaired device
                        // if(!foundDevice)
                        // {
                        // Log.d(TAG, "No paired device found, searching for available device...");
                        //
                        // for (int x=0 ;x < devices.size();++x)
                        // {
                        // RemoteBluetoothDevice rbtDevice =
                        // btDevice.getRemoteBluetoothDevice(devices.get(x));
                        // if(rbtDevice.getName().startsWith("BH ZBH") &&
                        // rbtDevice.getAddress().startsWith("00:07:80"))
                        // {
                        // Log.d(TAG, "found device, pairing...");
                        // rbtDevice.setPin("1234");
                        // rbtDevice.pair();
                        // it.gerdavax.android.bluetooth.BluetoothSocket btSocket =
                        // rbtDevice.openSocket(1);
                        // updateHandler.post(updateThread = new UpdateThread(btSocket));
                        // foundDevice = true;
                        // break;
                        // }
                        // }
                        //
                        // }

                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error connecting to HxM:" + e.getMessage());
                }
                // wait 10 seconds for another scan
                if (!foundDevice)
                    connectHandler.postDelayed(hxmConnectThread1_6 = new HxMConnectThread1_6(),
                            10000);
            }

            @Override
            public void scanStarted() {
                // Auto-generated method stub
            }
        }

        private LocalBluetoothDevice btDevice;
        private BluetoothDeviceListener btListener;

        public HxMConnectThread1_6() {
            // send address
            try {
                streamEnabled = false;
                btDevice = LocalBluetoothDevice.initLocalDevice(context);

                btListener = new BluetoothDeviceListener();
                btDevice.setListener(btListener);

                bbActiveFromTheStart = btDevice.isEnabled();
                if (!btDevice.isEnabled())
                    btDevice.setEnabled(true);

            } catch (Exception e) {
                Log.e(TAG, "Exception initializing HxM:" + e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                if (btDevice != null)
                    btDevice.scan();

            } catch (Exception e) {
                Log.e(TAG, "Exception running HxM:" + e.getMessage());
            }
        }

        public void stop() {
            try {
                btDevice.stopScanning();
                // if(!bbActiveFromTheStart)
                // btDevice.setEnabled(false);
                btDevice.close();
            } catch (Exception e) {
                Log.e(TAG, "Exception in stopping HxM:" + e.getMessage());
            }
        }
    }

    /*
     * Scan thread 2.1
     */
    private class HxMConnectThread2_1 implements Runnable {

        private BroadcastReceiver btReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                if (!hxmEnabled) {
                    return;
                }

                String action = intent.getAction();

                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.STATE_OFF);
                    if (state == BluetoothAdapter.STATE_ON) {
                        stop();
                        connectHandler.post(hxmConnectThread2_1 = new HxMConnectThread2_1());
                        return;
                    }
                }
            }
        };

        public HxMConnectThread2_1() {
            // send address
            try {
                btAdapter = BluetoothAdapter.getDefaultAdapter();
            } catch (Exception e) {
                Log.e(TAG, "Exception preparing Bluetooth scan thread:", e);
            }
        }

        @Override
        public void run() {
            if (hxmEnabled) {
                streamEnabled = false;
                if (btAdapter.isEnabled()) {

                    // check if there is a paired device with the name HxM
                    Set<android.bluetooth.BluetoothDevice> pairedDevices = btAdapter
                            .getBondedDevices();
                    // If there are paired devices
                    boolean foundDevice = false;
                    if (pairedDevices.size() > 0) {
                        // Loop through paired devices
                        for (BluetoothDevice device : pairedDevices) {
                            // Add the name and address to an array adapter to show in a ListView
                            if (device.getName().startsWith("HXM")
                                    && device.getAddress().startsWith("00:07:80")) {
                                Log.d(TAG, "Connecting to HxM:" + device.getName());
                                // Get a BluetoothSocket to connect with the given BluetoothDevice
                                try {
                                    btSocket2_1 = device
                                            .createRfcommSocketToServiceRecord(serial_uid);
                                    btSocket2_1.connect();
                                    processZHxMMessage = new ProcessZephyrHxMMessage(btSocket2_1
                                            .getRemoteDevice().getName());
                                    updateHandler.post(updateThread = new UpdateThread());
                                    foundDevice = true;
                                } catch (Exception e) {
                                    Log.d(TAG,
                                            "Error in connecting to HxM device:" + e.getMessage());
                                }
                            }
                        }
                    }

                    if (!foundDevice) {
                        Log.d(TAG, "No Paired HxM device found. Sleeping for 10 seconds");
                        connectHandler.postDelayed(hxmConnectThread2_1 = new HxMConnectThread2_1(),
                                10000);
                    }

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
                Log.d(TAG, "Stopping the HxM service");
                updateHandler.removeCallbacks(updateThread);
                btSocket2_1.close();

                context.unregisterReceiver(btReceiver);

            } catch (Exception e) {
                Log.e(TAG, "Error in stopping the HxM service:" + e.getMessage());
            }
        }

    }

    private static final String TAG = "Zephyr HxM";
    private BluetoothAdapter btAdapter;
    private final Context context;
    private boolean hxmEnabled = false;
    private UUID serial_uid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private final Handler connectHandler = new Handler(Looper.getMainLooper());
    private Handler updateHandler = new Handler(Looper.getMainLooper());
    private int updateInterval = 0;
    private UpdateThread updateThread = null;
    private long lastSampleTime = 0;
    private HxMConnectThread2_1 hxmConnectThread2_1 = null;
    private HxMConnectThread1_6 hxmConnectThread1_6 = null;
    @SuppressWarnings("unused")
    private boolean streamEnabled = false; // TODO use this
    private BluetoothSocket btSocket2_1 = null;
    private it.gerdavax.android.bluetooth.BluetoothSocket btSocket1_6 = null;
    private ProcessZephyrHxMMessage processZHxMMessage = null;

    public ZephyrHxM(Context context) {
        this.context = context;
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateInterval(int scanInterval) {
        this.updateInterval = scanInterval;
    }

    public void startHxM(int interval) {
        updateInterval = interval;
        hxmEnabled = true;

        Thread t = new Thread() {

            @Override
            public void run() {
                // Check if the phone version, if it is lower than, 2.1 use the bluetooth lib
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR)
                    connectHandler.post(hxmConnectThread1_6 = new HxMConnectThread1_6());
                else {
                    connectHandler.post(hxmConnectThread2_1 = new HxMConnectThread2_1());
                }
            }
        };
        this.connectHandler.post(t);
    }

    public void stopHxM() {
        hxmEnabled = false;
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR)
                if (hxmConnectThread1_6 != null) {
                    hxmConnectThread1_6.stop();
                    connectHandler.removeCallbacks(hxmConnectThread1_6);
                } else if (hxmConnectThread2_1 != null) {
                    hxmConnectThread2_1.stop();
                    connectHandler.removeCallbacks(hxmConnectThread2_1);
                }

        } catch (Exception e) {
            Log.e(TAG, "Exception in stopping Bluetooth scan thread:", e);
        }
    }
}
