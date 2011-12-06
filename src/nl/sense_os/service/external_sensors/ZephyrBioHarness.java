/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.external_sensors;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main.External;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;

import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class ZephyrBioHarness {

    /*
     * Process message class
     */
    private class ProcessZephyrBioHarnessMessage {
        private String deviceName;
        private SharedPreferences prefs = null;

        public ProcessZephyrBioHarnessMessage(String deviceName) {
            this.deviceName = deviceName;
            this.prefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
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
                if (prefs.getBoolean(External.ZephyrBioHarness.ACC, true)) {
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
                    // Log.v(TAG, "x:" + x + " y:" + y + " z:" + z);

                    json.put("x-axis", x);
                    json.put("y-axis", y);
                    json.put("z-axis", z);
                    Intent i = new Intent(context.getString(R.string.action_sense_new_data));
                    i.putExtra(DataPoint.SENSOR_NAME, SensorNames.ACCELEROMETER);
                    i.putExtra(DataPoint.SENSOR_DESCRIPTION, "BioHarness " + deviceName);
                    i.putExtra(DataPoint.VALUE, json.toString());
                    i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
                    i.putExtra(DataPoint.TIMESTAMP, System.currentTimeMillis());
                    context.startService(i);
                }

                // send heart rate
                if (prefs.getBoolean(External.ZephyrBioHarness.HEART_RATE, true)) {
                    Short heartRate = (short) (((short) buffer[12]) | (((short) buffer[13]) << 8));
                    // if(heartRate < (short)0)
                    // heartRate = (short)(heartRate+(short)255);

                    // Log.v(TAG, "Heart rate:" + heartRate);
                    Intent heartRateIntent = new Intent(
                            context.getString(R.string.action_sense_new_data));
                    heartRateIntent.putExtra(DataPoint.SENSOR_NAME, SensorNames.HEART_RATE);
                    heartRateIntent.putExtra(DataPoint.SENSOR_DESCRIPTION, "BioHarness "
                            + deviceName);
                    heartRateIntent.putExtra(DataPoint.VALUE, heartRate.intValue());
                    heartRateIntent.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.INT);
                    heartRateIntent.putExtra(DataPoint.TIMESTAMP, System.currentTimeMillis());
                    context.startService(heartRateIntent);
                }

                // send respiration rate
                if (prefs.getBoolean(External.ZephyrBioHarness.RESP, true)) {
                    Short respirationRate = (short) (((short) buffer[14]) | (((short) buffer[15]) << 8));
                    if (respirationRate < 0)
                        respirationRate = (short) (respirationRate + (short) 255);
                    float respirationRateF = (float) (respirationRate.floatValue() / 10.0f);

                    // Log.v(TAG, "Respiration rate:" + respirationRateF);
                    Intent respirationRateIntent = new Intent(
                            context.getString(R.string.action_sense_new_data));
                    respirationRateIntent.putExtra(DataPoint.SENSOR_NAME, SensorNames.RESPIRATION);
                    respirationRateIntent.putExtra(DataPoint.SENSOR_DESCRIPTION, "BioHarness "
                            + deviceName);
                    respirationRateIntent.putExtra(DataPoint.VALUE, respirationRateF);
                    respirationRateIntent.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.FLOAT);
                    respirationRateIntent.putExtra(DataPoint.TIMESTAMP, System.currentTimeMillis());
                    context.startService(respirationRateIntent);
                }

                // send skin temperature
                if (prefs.getBoolean(External.ZephyrBioHarness.TEMP, true)) {
                    Short skinTemperature = (short) (((short) buffer[16]) | (((short) buffer[17]) << 8));
                    if (skinTemperature < 0)
                        skinTemperature = (short) (skinTemperature + (short) 255);
                    float skinTemperatureF = (float) (skinTemperature.floatValue() / 10.0f);

                    // Log.v(TAG, "Skin temperature:" + skinTemperatureF);
                    Intent skinTemperatureIntent = new Intent(
                            context.getString(R.string.action_sense_new_data));
                    skinTemperatureIntent.putExtra(DataPoint.SENSOR_NAME, SensorNames.TEMPERATURE);
                    skinTemperatureIntent.putExtra(DataPoint.SENSOR_DESCRIPTION, "BioHarness "
                            + deviceName);
                    skinTemperatureIntent.putExtra(DataPoint.VALUE, skinTemperatureF);
                    skinTemperatureIntent.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.FLOAT);
                    skinTemperatureIntent.putExtra(DataPoint.TIMESTAMP, System.currentTimeMillis());
                    context.startService(skinTemperatureIntent);
                }

                // send battery level
                if (prefs.getBoolean(External.ZephyrBioHarness.BATTERY, true)) {
                    int batteryLevel = buffer[54];
                    // Log.v(TAG, "Battery level:" + batteryLevel);
                    Intent batteryIntent = new Intent(
                            context.getString(R.string.action_sense_new_data));
                    batteryIntent.putExtra(DataPoint.SENSOR_NAME, SensorNames.BATTERY_LEVEL);
                    batteryIntent
                            .putExtra(DataPoint.SENSOR_DESCRIPTION, "BioHarness " + deviceName);
                    batteryIntent.putExtra(DataPoint.VALUE, batteryLevel);
                    batteryIntent.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.INT);
                    batteryIntent.putExtra(DataPoint.TIMESTAMP, System.currentTimeMillis());
                    context.startService(batteryIntent);
                }

                // // send blood pressure
                // if(prefs.getBoolean(SensePrefs.Keys.PREF_BIOHARNESS_BLOOD_PRESSURE, true))
                // {
                // Short bloodPressure = (short)(((short)buffer[50]) | (((short)buffer[51]) << 8));
                // if(bloodPressure < 0)
                // bloodPressure = (short)(bloodPressure+(short)255);
                // float bloodPressureF = (float)(bloodPressure.floatValue()/1000.0f);
                //
                // Log.d(TAG, "Blood pressure:"+bloodPressureF);
                // Intent batteryIntent = new
                // Intent(context.getString(R.string.action_sense_new_data));
                // batteryIntent.putExtra(DataPoint.SENSOR_NAME, SensorNames.BLOOD_PRESSURE);
                // batteryIntent.putExtra(DataPoint.SENSOR_DESCRIPTION, "BioHarness "+deviceName);
                // batteryIntent.putExtra(DataPoint.VALUE, bloodPressureF);
                // batteryIntent.putExtra(DataPoint.DATA_TYPE,
                // SenseDataTypes.FLOAT);
                // batteryIntent.putExtra(DataPoint.TIMESTAMP, System.currentTimeMillis());
                // context.startService(batteryIntent);
                // }

                // send worn status
                if (prefs.getBoolean(External.ZephyrBioHarness.WORN_STATUS, true)) {
                    boolean wornStatusB = (buffer[55] & 0x10000000) == 0x10000000;

                    // Log.v(TAG, "Worn status:" + wornStatusB);
                    Intent batteryIntent = new Intent(
                            context.getString(R.string.action_sense_new_data));
                    batteryIntent.putExtra(DataPoint.SENSOR_NAME, SensorNames.WORN_STATUS);
                    batteryIntent
                            .putExtra(DataPoint.SENSOR_DESCRIPTION, "BioHarness " + deviceName);
                    batteryIntent.putExtra(DataPoint.VALUE, wornStatusB);
                    batteryIntent.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.BOOL);
                    batteryIntent.putExtra(DataPoint.TIMESTAMP, System.currentTimeMillis());
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
            if (mmInStream == null || mmOutStream == null) {
                InputStream tmpIn = null;
                OutputStream tmpOut = null;

                // Get the input and output streams, using temp objects because member streams are
                // final
                try {
                    tmpIn = btSocket.getInputStream();
                    tmpOut = btSocket.getOutputStream();
                } catch (Exception e) {
                    Log.e(TAG, "Error in update thread constructor:" + e.getMessage());
                }
                mmInStream = tmpIn;
                mmOutStream = tmpOut;
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
                        updateHandler.post(updateThread = new UpdateThread());
                        return;
                    }
                    // check connection
                    if (!sendConnectionAlive()) {
                        connectHandler
                                .postDelayed(
                                        bioHarnessConnectThread = new BioHarnessConnectThread(),
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
                                                bioHarnessConnectThread = new BioHarnessConnectThread(),
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
                    updateHandler.postDelayed(updateThread = new UpdateThread(), 1000);
                } catch (Exception e) {
                    Log.e(TAG, "Error in receiving BioHarness data:" + e.getMessage());
                    e.printStackTrace();
                    // re-connect
                    connectHandler.postDelayed(
                            bioHarnessConnectThread = new BioHarnessConnectThread(), 1000);
                }
            } else
                cancel();
        }

        /* Call this from the main Activity to send data to the remote device */
        public boolean write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (Exception e) {
                Log.e(TAG, "Error in write:" + e.getMessage());
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
                if (enable) {
                    // Log.v(TAG, "Enabling general data...");
                } else {
                    // Log.v(TAG, "Disabling general data...");
                }
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
                // Log.v(TAG,
                // "Enable received:" + Integer.toString(buffer[0] & 0xFF, 16) + ","
                // + Integer.toString(buffer[1] & 0xFF, 16) + ","
                // + Integer.toString(buffer[2] & 0xFF, 16));
                if (nrBytes > 0 && buffer[1] == 0x14 && buffer[4] == 0x06) // ACK
                {
                    if (enable) {
                        // Log.v(TAG, "General data enabled");
                    } else {
                        // Log.v(TAG, "General data disabled");
                    }
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
                Log.i(TAG, "Stopping the BioHarness service");
                setEnableGeneralData(false);
                btSocket.close();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    /*
     * Scan thread
     */
    private class BioHarnessConnectThread implements Runnable {

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
                                .post(bioHarnessConnectThread = new BioHarnessConnectThread());
                        return;
                    }
                }
            }
        };

        public BioHarnessConnectThread() {
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
                    Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

                    // If there are paired devices
                    boolean foundDevice = false;
                    if (pairedDevices.size() > 0) {
                        // Loop through paired devices
                        for (BluetoothDevice device : pairedDevices) {
                            // Add the name and address to an array adapter to show in a ListView
                            if (device.getName().startsWith("BH ZBH")
                                    && device.getAddress().startsWith("00:07:80")) {
                                // Log.v(TAG, "Connecting to BioHarness:" + device.getName());
                                // Get a BluetoothSocket to connect with the given BluetoothDevice
                                try {
                                    btSocket = device
                                            .createRfcommSocketToServiceRecord(serial_uid);
                                    btSocket.connect();
                                    processZBHMessage = new ProcessZephyrBioHarnessMessage(
                                            btSocket.getRemoteDevice().getName());
                                    updateHandler.post(updateThread = new UpdateThread());
                                    foundDevice = true;
                                } catch (Exception e) {
                                    Log.e(TAG,
                                            "Error connecting to BioHarness device: "
                                                    + e.getMessage());
                                }
                            }
                        }
                    }
                    if (!foundDevice) {
                        // Log.v(TAG, "No Paired BioHarness device found. Sleeping for 10 seconds");
                        connectHandler.postDelayed(
                                bioHarnessConnectThread = new BioHarnessConnectThread(),
                                10000);
                    }
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
                // Log.v(TAG, "Stopping the BioHarness service");
                updateHandler.removeCallbacks(updateThread);
                btSocket.close();

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
    private BioHarnessConnectThread bioHarnessConnectThread = null;
    private boolean streamEnabled = false;
    private BluetoothSocket btSocket = null;
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

                connectHandler.post(bioHarnessConnectThread = new BioHarnessConnectThread());
            }
        };
        this.connectHandler.post(t);
    }

    public void stopBioHarness() {
        bioHarnessEnabled = false;
        try {
            if (bioHarnessConnectThread != null) {
                bioHarnessConnectThread.stop();
                connectHandler.removeCallbacks(bioHarnessConnectThread);
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception in stopping Bluetooth scan thread:", e);
        }
    }
}
