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

import org.json.JSONObject;

import it.gerdavax.android.bluetooth.LocalBluetoothDevice;
import it.gerdavax.android.bluetooth.LocalBluetoothDeviceListener;
import it.gerdavax.android.bluetooth.RemoteBluetoothDevice;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class ZephyrBioHarness {

    /*
     * Process message class
     */
    private class ProcessZephyrBioHarnessMessage {
        private String deviceName;
        private SharedPreferences prefs = null;

        public ProcessZephyrBioHarnessMessage(String deviceName) {
            this.deviceName = deviceName;
            this.prefs = context
                    .getSharedPreferences(Constants.MAIN_PREFS, Context.MODE_WORLD_WRITEABLE);
        }

        public boolean processMessage(byte[] buffer) throws Exception {
            // received general data
            if (buffer[0] == 0x02 && buffer[1] == 0x20 && buffer[2] == 53) {
                // check that payload is not completely empty
                boolean hasPayload = false;
                for (int i = 12; i < 58; i++) {
                    if (buffer[i] != 0) {
                        hasPayload = true;
                        break;
                    }
                }
                if (false == hasPayload) {
                    Log.w(TAG, "No sensor data payload received");
                    // return true because the message type was correct, only the data is wrong
                    return true;
                }

                // send acceleration data in m/s^2
                if (prefs.getBoolean(Constants.PREF_BIOHARNESS_ACC, true)) {
                    float g = 9.80665f;
                    JSONObject json = new JSONObject();
                    Short xmin = (short) (((short) buffer[32]) | (((short) buffer[33]) << 8));
                    Short xmax = (short) (((short) buffer[34]) | (((short) buffer[35]) << 8));
                    float x = ((xmin.floatValue() + xmax.floatValue()) / 200.0f) * g;
                    Short ymin = (short) (((short) buffer[36]) | (((short) buffer[37]) << 8));
                    Short ymax = (short) (((short) buffer[38]) | (((short) buffer[39]) << 8));
                    float y = ((ymin.floatValue() + ymax.floatValue()) / 200.0f) * g;
                    Short zmin = (short) (((short) buffer[40]) | (((short) buffer[41]) << 8));
                    Short zmax = (short) (((short) buffer[42]) | (((short) buffer[43]) << 8));
                    float z = ((zmin.floatValue() + zmax.floatValue()) / 200.0f) * g;
                    Log.d(TAG, "x:" + x + " y:" + y + " z:" + z);

                    json.put("x-axis", x);
                    json.put("y-axis", y);
                    json.put("z-axis", z);
                    Intent i = new Intent(MsgHandler.ACTION_NEW_MSG);
                    i.putExtra(MsgHandler.KEY_SENSOR_NAME, "accelerometer");
                    i.putExtra(MsgHandler.KEY_SENSOR_DEVICE, "BioHarness " + deviceName);
                    i.putExtra(MsgHandler.KEY_VALUE, json.toString());
                    i.putExtra(MsgHandler.KEY_DATA_TYPE, Constants.SENSOR_DATA_TYPE_JSON);
                    i.putExtra(MsgHandler.KEY_TIMESTAMP, System.currentTimeMillis());
                    context.startService(i);
                }

                // send heart rate
                if (prefs.getBoolean(Constants.PREF_BIOHARNESS_HEART_RATE, true)) {
                    Short heartRate = (short) (((short) buffer[12]) | (((short) buffer[13]) << 8));
                    // if(heartRate < (short)0)
                    // heartRate = (short)(heartRate+(short)255);

                    Log.d(TAG, "Heart rate:" + heartRate);
                    Intent heartRateIntent = new Intent(MsgHandler.ACTION_NEW_MSG);
                    heartRateIntent.putExtra(MsgHandler.KEY_SENSOR_NAME, "heart rate");
                    heartRateIntent.putExtra(MsgHandler.KEY_SENSOR_DEVICE, "BioHarness "
                            + deviceName);
                    heartRateIntent.putExtra(MsgHandler.KEY_VALUE, heartRate.intValue());
                    heartRateIntent.putExtra(MsgHandler.KEY_DATA_TYPE,
                            Constants.SENSOR_DATA_TYPE_INT);
                    heartRateIntent.putExtra(MsgHandler.KEY_TIMESTAMP, System.currentTimeMillis());
                    context.startService(heartRateIntent);
                }

                // send respiration rate
                if (prefs.getBoolean(Constants.PREF_BIOHARNESS_RESP, true)) {
                    Short respirationRate = (short) (((short) buffer[14]) | (((short) buffer[15]) << 8));
                    if (respirationRate < 0)
                        respirationRate = (short) (respirationRate + (short) 255);
                    float respirationRateF = (float) (respirationRate.floatValue() / 10.0f);

                    Log.d(TAG, "Respiration rate:" + respirationRateF);
                    Intent respirationRateIntent = new Intent(MsgHandler.ACTION_NEW_MSG);
                    respirationRateIntent.putExtra(MsgHandler.KEY_SENSOR_NAME, "respiration rate");
                    respirationRateIntent.putExtra(MsgHandler.KEY_SENSOR_DEVICE, "BioHarness "
                            + deviceName);
                    respirationRateIntent.putExtra(MsgHandler.KEY_VALUE, respirationRateF);
                    respirationRateIntent.putExtra(MsgHandler.KEY_DATA_TYPE,
                            Constants.SENSOR_DATA_TYPE_FLOAT);
                    respirationRateIntent.putExtra(MsgHandler.KEY_TIMESTAMP,
                            System.currentTimeMillis());
                    context.startService(respirationRateIntent);
                }

                // send skin temperature
                if (prefs.getBoolean(Constants.PREF_BIOHARNESS_TEMP, true)) {
                    Short skinTemperature = (short) (((short) buffer[16]) | (((short) buffer[17]) << 8));
                    if (skinTemperature < 0)
                        skinTemperature = (short) (skinTemperature + (short) 255);
                    float skinTemperatureF = (float) (skinTemperature.floatValue() / 10.0f);

                    Log.d(TAG, "Skin temperature:" + skinTemperatureF);
                    Intent skinTemperatureIntent = new Intent(MsgHandler.ACTION_NEW_MSG);
                    skinTemperatureIntent.putExtra(MsgHandler.KEY_SENSOR_NAME, "temperature");
                    skinTemperatureIntent.putExtra(MsgHandler.KEY_SENSOR_DEVICE, "BioHarness "
                            + deviceName);
                    skinTemperatureIntent.putExtra(MsgHandler.KEY_VALUE, skinTemperatureF);
                    skinTemperatureIntent.putExtra(MsgHandler.KEY_DATA_TYPE,
                            Constants.SENSOR_DATA_TYPE_FLOAT);
                    skinTemperatureIntent.putExtra(MsgHandler.KEY_TIMESTAMP,
                            System.currentTimeMillis());
                    context.startService(skinTemperatureIntent);
                }

                // send battery level
                if (prefs.getBoolean(Constants.PREF_BIOHARNESS_BATTERY, true)) {
                    int batteryLevel = buffer[54];
                    Log.d(TAG, "Battery level:" + batteryLevel);
                    Intent batteryIntent = new Intent(MsgHandler.ACTION_NEW_MSG);
                    batteryIntent.putExtra(MsgHandler.KEY_SENSOR_NAME, "battery level");
                    batteryIntent
                            .putExtra(MsgHandler.KEY_SENSOR_DEVICE, "BioHarness " + deviceName);
                    batteryIntent.putExtra(MsgHandler.KEY_VALUE, batteryLevel);
                    batteryIntent
                            .putExtra(MsgHandler.KEY_DATA_TYPE, Constants.SENSOR_DATA_TYPE_INT);
                    batteryIntent.putExtra(MsgHandler.KEY_TIMESTAMP, System.currentTimeMillis());
                    context.startService(batteryIntent);
                }

                // // send blood pressure
                // if(prefs.getBoolean(Constants.PREF_BIOHARNESS_BLOOD_PRESSURE, true))
                // {
                // Short bloodPressure = (short)(((short)buffer[50]) | (((short)buffer[51]) << 8));
                // if(bloodPressure < 0)
                // bloodPressure = (short)(bloodPressure+(short)255);
                // float bloodPressureF = (float)(bloodPressure.floatValue()/1000.0f);
                //
                // Log.d(TAG, "Blood pressure:"+bloodPressureF);
                // Intent batteryIntent = new Intent(MsgHandler.ACTION_NEW_MSG);
                // batteryIntent.putExtra(MsgHandler.KEY_SENSOR_NAME, "blood pressure");
                // batteryIntent.putExtra(MsgHandler.KEY_SENSOR_DEVICE, "BioHarness "+deviceName);
                // batteryIntent.putExtra(MsgHandler.KEY_VALUE, bloodPressureF);
                // batteryIntent.putExtra(MsgHandler.KEY_DATA_TYPE,
                // Constants.SENSOR_DATA_TYPE_FLOAT);
                // batteryIntent.putExtra(MsgHandler.KEY_TIMESTAMP, System.currentTimeMillis());
                // context.startService(batteryIntent);
                // }

                // send worn status
                if (prefs.getBoolean(Constants.PREF_BIOHARNESS_WORN_STATUS, true)) {
                    boolean wornStatusB = (buffer[55] & 0x10000000) == 0x10000000;

                    Log.d(TAG, "Worn status:" + wornStatusB);
                    Intent batteryIntent = new Intent(MsgHandler.ACTION_NEW_MSG);
                    batteryIntent.putExtra(MsgHandler.KEY_SENSOR_NAME, "worn status");
                    batteryIntent
                            .putExtra(MsgHandler.KEY_SENSOR_DEVICE, "BioHarness " + deviceName);
                    batteryIntent.putExtra(MsgHandler.KEY_VALUE, wornStatusB);
                    batteryIntent.putExtra(MsgHandler.KEY_DATA_TYPE,
                            Constants.SENSOR_DATA_TYPE_BOOL);
                    batteryIntent.putExtra(MsgHandler.KEY_TIMESTAMP, System.currentTimeMillis());
                    context.startService(batteryIntent);
                }
                return true;
            } else
                return false;
        }
    }

    /*
     * Update thread
     */
    private class UpdateThread extends Thread {

        private InputStream mmInStream;
        private OutputStream mmOutStream;

        public UpdateThread() {
            if (btSocket2_1 != null) {
                if (mmInStream == null || mmOutStream == null) {
                    InputStream tmpIn = null;
                    OutputStream tmpOut = null;

                    // Get the input and output streams, using temp objects because
                    // member streams are final
                    try {
                        tmpIn = btSocket2_1.getInputStream();
                        tmpOut = btSocket2_1.getOutputStream();
                    } catch (Exception e) {
                        Log.d(TAG, "Error in update thread constructor:" + e.getMessage());
                    }
                    mmInStream = tmpIn;
                    mmOutStream = tmpOut;
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
            boolean readMessage = false;
            if (bioHarnessEnabled) {
                try {
                    if (!streamEnabled) {
                        streamEnabled = setEnableGeneralData(true);
                        if (btSocket1_6 == null)
                            updateHandler.post(updateThread = new UpdateThread());
                        else
                            updateHandler.post(updateThread = new UpdateThread());
                        return;
                    }
                    // check connection
                    if (!sendConnectionAlive()) {
                        connectHandler
                                .postDelayed(
                                        bioHarnessConnectThread2_1 = new BioHarnessConnectThread2_1(),
                                        1000);
                        return;
                    }

                    if (System.currentTimeMillis() > lastSampleTime + updateInterval) {
                        while (!readMessage) {
                            lastSampleTime = System.currentTimeMillis();
                            // Read from the InputStream
                            byte[] buffer = new byte[80];
                            int bytes; // bytes returned from read()
                            // check connection
                            if (!sendConnectionAlive()) {
                                connectHandler
                                        .postDelayed(
                                                bioHarnessConnectThread2_1 = new BioHarnessConnectThread2_1(),
                                                1000);
                                return;
                            }
                            bytes = mmInStream.read(buffer);
                            if (bytes > 0)
                                readMessage = processZBHMessage.processMessage(buffer);
                            buffer = null;
                        }
                    }
                    // update every second
                    if (btSocket1_6 == null)
                        updateHandler.postDelayed(updateThread = new UpdateThread(), 1000);
                    else
                        updateHandler.postDelayed(updateThread = new UpdateThread(), 1000);
                } catch (Exception e) {
                    Log.e(TAG, "Error in receiving BioHarness data:" + e.getMessage());
                    e.printStackTrace();
                    // re-connect
                    connectHandler.postDelayed(
                            bioHarnessConnectThread2_1 = new BioHarnessConnectThread2_1(), 1000);
                }
            } else
                cancel();
        }

        /* Call this from the main Activity to send data to the remote device */
        public boolean write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (Exception e) {
                Log.d(TAG, "Error in write:" + e.getMessage());
                return false;
            }
            return true;
        }

        public boolean sendConnectionAlive() {
            try {
                byte[] writeBuffer = new byte[128];

                writeBuffer[0] = 0x02; // STC
                writeBuffer[1] = 0x23; // MSG_ID // alive
                writeBuffer[2] = 0; // DLC
                writeBuffer[3] = 0; // Payload
                writeBuffer[4] = 0; // CRC
                writeBuffer[5] = 0x03; // ETX
                return write(writeBuffer);
            } catch (Exception e) {
                Log.e(TAG, "Error in setEnableGeneralData:" + e.getMessage());
                return false;
            }
        }

        public boolean setEnableGeneralData(boolean enable) {
            try {
                if (enable)
                    Log.d(TAG, "Enabling general data...");
                else
                    Log.d(TAG, "Disabling general data...");
                byte[] buffer = new byte[128];
                byte[] writeBuffer = new byte[128];
                byte[] data = new byte[1];
                if (enable) // 1 = enable 0 = disable
                    data[0] = (byte) 1;
                else
                    data[0] = (byte) 0;

                writeBuffer[0] = 0x02; // STC
                writeBuffer[1] = 0x14; // MSG_ID // general packet enable
                writeBuffer[2] = 1; // DLC
                writeBuffer[3] = data[0];
                writeBuffer[4] = CRC8.checksum2(data); // CRC
                writeBuffer[5] = 0x03; // ETX
                write(writeBuffer);
                int nrBytes = mmInStream.read(buffer);
                Log.d(TAG,
                        "Enable received:" + Integer.toString(buffer[0] & 0xFF, 16) + ","
                                + Integer.toString(buffer[1] & 0xFF, 16) + ","
                                + Integer.toString(buffer[2] & 0xFF, 16));
                if (nrBytes > 0 && buffer[1] == 0x14 && buffer[4] == 0x06) // ACK
                {
                    if (enable)
                        Log.d(TAG, "General data enabled");
                    else
                        Log.d(TAG, "General data disabled");
                    return true;
                } else
                    return false;
            } catch (Exception e) {
                Log.e(TAG, "Error in setEnableGeneralData:" + e.getMessage());
                return false;
            }
        }

        /* Call this from the main Activity to shutdown the connection */
        public void cancel() {
            try {
                Log.d(TAG, "Stopping the BioHarness service");
                setEnableGeneralData(false);
                if (btSocket1_6 == null)
                    btSocket2_1.close();
                else
                    btSocket1_6.closeSocket();
            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
            }
        }
    }

    /*
     * Scan thread 1.6
     */
    private class BioHarnessConnectThread1_6 implements Runnable {

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
                if (!bioHarnessEnabled) {
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
                        // find a paired BioHarness

                        for (int x = 0; x < devices.size(); ++x) {
                            RemoteBluetoothDevice rbtDevice = btDevice
                                    .getRemoteBluetoothDevice(devices.get(x));
                            if (rbtDevice.isPaired() && rbtDevice.getName().startsWith("BH ZBH")
                                    && rbtDevice.getAddress().startsWith("00:07:80")) {
                                btSocket1_6 = rbtDevice.openSocket(1);
                                processZBHMessage = new ProcessZephyrBioHarnessMessage(
                                        rbtDevice.getName());
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
                    Log.e(TAG, "Error connecting to BioHarness:", e);
                }
                // wait 10 seconds for another scan
                if (!foundDevice)
                    connectHandler.postDelayed(
                            bioHarnessConnectThread1_6 = new BioHarnessConnectThread1_6(), 10000);
            }

            @Override
            public void scanStarted() {
                // Auto-generated method stub
            }
        }

        private LocalBluetoothDevice btDevice;
        private BluetoothDeviceListener btListener;

        public BioHarnessConnectThread1_6() {
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
                Log.e(TAG, "Exception initializing the bioHarness connectThread:" + e.getMessage());
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
                // if(!bbActiveFromTheStart)
                // btDevice.setEnabled(false);
                btDevice.close();
            } catch (Exception e) {
                Log.e(TAG, "Exception in stopping the bioHarness thread:" + e.getMessage());
            }
        }
    }

    /*
     * Scan thread 2.1
     */
    private class BioHarnessConnectThread2_1 implements Runnable {

        private BroadcastReceiver btReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                if (!bioHarnessEnabled) {
                    return;
                }

                String action = intent.getAction();

                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.STATE_OFF);
                    if (state == BluetoothAdapter.STATE_ON) {
                        stop();
                        connectHandler
                                .post(bioHarnessConnectThread2_1 = new BioHarnessConnectThread2_1());
                        return;
                    }
                }
            }
        };

        public BioHarnessConnectThread2_1() {
            // send address
            try {
                btAdapter = BluetoothAdapter.getDefaultAdapter();
            } catch (Exception e) {
                Log.e(TAG, "Exception preparing Bluetooth scan thread:", e);
            }
        }

        @Override
        public void run() {
            if (bioHarnessEnabled) {
                streamEnabled = false;
                if (btAdapter.isEnabled()) {

                    // check if there is a paired device with the name BioHarness
                    Set<android.bluetooth.BluetoothDevice> pairedDevices = btAdapter
                            .getBondedDevices();

                    // If there are paired devices
                    boolean foundDevice = false;
                    if (pairedDevices.size() > 0) {
                        // Loop through paired devices
                        for (BluetoothDevice device : pairedDevices) {
                            // Add the name and address to an array adapter to show in a ListView
                            if (device.getName().startsWith("BH ZBH")
                                    && device.getAddress().startsWith("00:07:80")) {
                                Log.d(TAG, "Connecting to BioHarness:" + device.getName());
                                // Get a BluetoothSocket to connect with the given BluetoothDevice
                                try {
                                    btSocket2_1 = device
                                            .createRfcommSocketToServiceRecord(serial_uid);
                                    btSocket2_1.connect();
                                    processZBHMessage = new ProcessZephyrBioHarnessMessage(
                                            btSocket2_1.getRemoteDevice().getName());
                                    updateHandler.post(updateThread = new UpdateThread());
                                    foundDevice = true;
                                } catch (Exception e) {
                                    Log.d(TAG,
                                            "Error in connecting to BioHarness device:"
                                                    + e.getMessage());
                                }
                            }
                        }
                    }
                    if (!foundDevice) {
                        Log.d(TAG, "No Paired BioHarness device found. Sleeping for 10 seconds");
                        connectHandler.postDelayed(
                                bioHarnessConnectThread2_1 = new BioHarnessConnectThread2_1(),
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
                Log.d(TAG, "Stopping the BioHarness service");
                updateHandler.removeCallbacks(updateThread);
                btSocket2_1.close();

                context.unregisterReceiver(btReceiver);

            } catch (Exception e) {
                Log.e(TAG, "Error in stopping bioHarness service" + e.getMessage());
            }
        }

    }

    private static final String TAG = "Zephyr BioHarness";
    private BluetoothAdapter btAdapter;
    private final Context context;
    private boolean bioHarnessEnabled = false;
    private UUID serial_uid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private final Handler connectHandler = new Handler(Looper.getMainLooper());
    private Handler updateHandler = new Handler(Looper.getMainLooper());
    private int updateInterval = 0;
    private UpdateThread updateThread = null;
    private long lastSampleTime = 0;
    private BioHarnessConnectThread2_1 bioHarnessConnectThread2_1 = null;
    private BioHarnessConnectThread1_6 bioHarnessConnectThread1_6 = null;
    private boolean streamEnabled = false;
    private BluetoothSocket btSocket2_1 = null;
    private it.gerdavax.android.bluetooth.BluetoothSocket btSocket1_6 = null;
    private ProcessZephyrBioHarnessMessage processZBHMessage = null;

    public ZephyrBioHarness(Context context) {
        this.context = context;
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateInterval(int scanInterval) {
        this.updateInterval = scanInterval;
    }

    public void startBioHarness(int interval) {
        updateInterval = interval;
        bioHarnessEnabled = true;

        Thread t = new Thread() {
            @Override
            public void run() {
                // Check if the phone version, if it is lower than, 2.1 use the bluetooth lib
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR)
                    connectHandler
                            .post(bioHarnessConnectThread1_6 = new BioHarnessConnectThread1_6());
                else {
                    connectHandler
                            .post(bioHarnessConnectThread2_1 = new BioHarnessConnectThread2_1());
                }
            }
        };
        this.connectHandler.post(t);
    }

    public void stopBioHarness() {
        bioHarnessEnabled = false;
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR)
                if (bioHarnessConnectThread1_6 != null) {
                    bioHarnessConnectThread1_6.stop();
                    connectHandler.removeCallbacks(bioHarnessConnectThread1_6);
                } else if (bioHarnessConnectThread2_1 != null) {
                    bioHarnessConnectThread2_1.stop();
                    connectHandler.removeCallbacks(bioHarnessConnectThread2_1);
                }

        } catch (Exception e) {
            Log.e(TAG, "Exception in stopping Bluetooth scan thread:", e);
        }
    }
}
