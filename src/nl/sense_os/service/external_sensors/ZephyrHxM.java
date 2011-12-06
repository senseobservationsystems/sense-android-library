/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.external_sensors;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main.External;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;

import it.gerdavax.android.bluetooth.LocalBluetoothDevice;
import it.gerdavax.android.bluetooth.LocalBluetoothDeviceListener;
import it.gerdavax.android.bluetooth.RemoteBluetoothDevice;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

public class ZephyrHxM {

    /*
     * Process message class
     */
    private class ProcessZephyrHxMMessage {
        private String deviceName;
        private SharedPreferences prefs = null;
        private Vector<Byte> bufferBuffer;
        private int gotStart = -1;
        int msgSize = 60;

        public ProcessZephyrHxMMessage(String deviceName) {
            bufferBuffer = new Vector<Byte>();
            this.deviceName = deviceName;
            this.prefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
        }

        public byte[] getMessage(byte[] buffer, boolean useBuffer) {
            byte[] msgBuffer = new byte[msgSize];
            // check if we have the start from the previous run
            if (gotStart > -1 && useBuffer) {
                for (int i = 0; i < msgSize; i++) {
                    if (gotStart + i < bufferBuffer.size())
                        msgBuffer[i] = bufferBuffer.elementAt(gotStart + i);
                    else {
                        if (((gotStart + i) - bufferBuffer.size()) < buffer.length)
                            msgBuffer[i] = buffer[(gotStart + i) - bufferBuffer.size()];
                        else {
                            // msg to short store and bailout
                            bufferBuffer.clear();
                            for (int j = 0; j < msgBuffer.length; j++) {
                                bufferBuffer.add(msgBuffer[j]);
                                gotStart = 0;
                            }
                            return new byte[0];
                        }
                    }
                }
                // check for the remaining part
                int rest = msgSize - (bufferBuffer.size() - gotStart);
                if (rest < buffer.length) {
                    bufferBuffer.clear();
                    gotStart = -1;
                    for (int i = rest; i < buffer.length; i++) {
                        bufferBuffer.add(buffer[i]);
                        if (i > 1 && buffer[i - 2] == 0x02 && buffer[i - 1] == 0x26
                                && buffer[i] == 55 && gotStart < 0)
                            gotStart = bufferBuffer.size() - 3;
                    }
                }
                if (msgBuffer.length == 60)
                    return msgBuffer;
                else
                    return new byte[0];
            } else {
                if (!useBuffer)
                    bufferBuffer.clear();

                // combine old buffer with new one
                for (int i = 0; i < buffer.length; i++) {
                    bufferBuffer.add(buffer[i]);
                }
                // find the start in the current buffer
                for (int i = 0; i < bufferBuffer.size(); i++) {
                    // got the start copy the rest
                    if (i + 2 < bufferBuffer.size() && bufferBuffer.get(i) == 0x02
                            && bufferBuffer.get(i + 1) == 0x26 && bufferBuffer.get(i + 2) == 55) {
                        // can it hold a message
                        if (bufferBuffer.size() - i >= 60) {
                            Vector<Byte> tempBuffer = new Vector<Byte>();
                            for (int j = 0; j + i < bufferBuffer.size(); j++) {
                                if (j < 60)
                                    msgBuffer[j] = bufferBuffer.get(i + j);
                                else // copy the remaining part in the temp buffer
                                {
                                    if (i + j + 2 < bufferBuffer.size()
                                            && bufferBuffer.get(i + j) == 0x02
                                            && bufferBuffer.get(i + j + 1) == 0x26
                                            && bufferBuffer.get(i + j + 2) == 55 && gotStart < 0)
                                        gotStart = 60 - j;
                                    tempBuffer.add(bufferBuffer.get(i + j));
                                }
                            }
                            // copy the tempbuffer in the main buffer
                            bufferBuffer.clear();
                            for (int j = 0; j < tempBuffer.size(); j++) {
                                bufferBuffer.addElement(tempBuffer.get(j));
                            }
                            return msgBuffer;
                        } else {
                            return new byte[0];
                        }
                    }
                }
            }
            return new byte[0];
        }

        public boolean processMessage(byte[] inputBuffer, boolean useBuffer) {
            try {
                byte[] buffer = getMessage(inputBuffer, useBuffer);

                if (buffer.length == 0)
                    return false;
                // received general data
                if (buffer[0] == 0x02 && buffer[1] == 0x26 && buffer[2] == 55) {
                	Log.d(TAG, "Found start of message");
                    // send heart rate
                    if (prefs.getBoolean(External.ZephyrHxM.HEART_RATE, true)) {
                        int heartRate = Byte.valueOf(buffer[12]).intValue();
                        if (heartRate < 0)
                            heartRate = (heartRate + 255);

                        // Log.v(TAG, "Heart rate:" + heartRate);
                        sendDataPoint(SensorNames.HEART_RATE, "HxM " + deviceName, heartRate,
                                SenseDataTypes.INT);

                    }
                    // send speed
                    if (prefs.getBoolean(External.ZephyrHxM.SPEED, true)) {
                        int speed = (((int) buffer[52]) | (((int) buffer[53]) << 8));
                        float speedF = (float) speed / 256f;

                        // Log.v(TAG, "Speed:" + speedF);
                        sendDataPoint(SensorNames.SPEED, "HxM " + deviceName, speedF,
                                SenseDataTypes.FLOAT);
                    }
                    // send distance
                    if (prefs.getBoolean(External.ZephyrHxM.DISTANCE, true)) {
                        int distance = (((int) buffer[50]) | (((int) buffer[51]) << 8));
                        float distanceF = (float) distance / 16F;

                        // Log.v(TAG, "Distance:" + distanceF);
                        sendDataPoint(SensorNames.DISTANCE, "HxM " + deviceName, distanceF,
                                SenseDataTypes.FLOAT);
                    }
                    // send battery charge
                    if (prefs.getBoolean(External.ZephyrHxM.BATTERY, true)) {
                        int battery = Byte.valueOf(buffer[11]).intValue();

                        // Log.v(TAG, "Battery charge:" + battery.intValue());
                        sendDataPoint(SensorNames.BATTERY_CHARGE, "HxM " + deviceName, battery,
                                SenseDataTypes.INT);

                        if (notifyOnEmptyBattery && battery < 5 && battery != 0
                                && (System.currentTimeMillis() - lastEmptyBatteryNotify) > 300000) {
                            sendNotification("Zephyr HxM empty battery warning: " + battery + "%");
                            lastEmptyBatteryNotify = System.currentTimeMillis();
                        }
                        // every 5 min
                        if (battery == 0
                                && (System.currentTimeMillis() - lastNoChestConnectionNofify) > 300000) {
                            sendNotification("Zephyr HxM not properly connected to the chest, make sure the strap is wet enough.");
                            lastNoChestConnectionNofify = System.currentTimeMillis();
                        }
                    }
                    // send strides count
                    if (prefs.getBoolean(External.ZephyrHxM.STRIDES, true)) {
                    	int strides = buffer[54];
                    	strides = strides < 0 ? strides+256:strides;                        
                        // Short strides = (short)buffer[54];

                        // Log.v(TAG, "Battery charge:" + battery.intValue());
                        sendDataPoint(SensorNames.STRIDES, "HxM " + deviceName, strides,
                                SenseDataTypes.INT);
                    }
                    return true;

                } else {
                    // find the rest of the data
                    Log.d(TAG,
                            "Unexpected first three bytes: { "
                                    + Integer.toHexString(inputBuffer[0] & 0xFF) + ", "
                                    + Integer.toHexString(inputBuffer[1] & 0xFF) + ", "
                                    + Integer.toHexString(inputBuffer[2] & 0xFF) + " }");

                    String totalBuffer = "";
                    for (byte b : inputBuffer) {
                        String s = Integer.toHexString(b & 0xFF);
                        if (s.length() == 1) {
                            s = "0" + s;
                        }
                        totalBuffer += s + ", ";
                    }
                    Log.d(TAG, "Total buffer content:\n" + totalBuffer);

                }
                if (bufferBuffer.size() >= msgSize)
                    return processMessage(new byte[0], true);

            } catch (Exception e) {
                Log.e(TAG, "Error in ProcessZephyrHxMMessage:" + e.getMessage());
                return false;
            }
            return false;
        }

        private void sendDataPoint(String sensorName, String description, Object value,
                String dataType) {
            Intent intent = new Intent(context.getString(R.string.action_sense_new_data));
            intent.putExtra(DataPoint.SENSOR_NAME, sensorName);
            intent.putExtra(DataPoint.SENSOR_DESCRIPTION, description);
            intent.putExtra(DataPoint.DATA_TYPE, dataType);
            if (dataType.equals(SenseDataTypes.BOOL)) {
                intent.putExtra(DataPoint.VALUE, (Boolean) value);
            } else if (dataType.equals(SenseDataTypes.FLOAT)) {
                intent.putExtra(DataPoint.VALUE, (Float) value);
            } else if (dataType.equals(SenseDataTypes.INT)) {
                intent.putExtra(DataPoint.VALUE, (Integer) value);
            } else if (dataType.equals(SenseDataTypes.JSON)) {
                intent.putExtra(DataPoint.VALUE, (String) value);
            } else if (dataType.equals(SenseDataTypes.STRING)) {
                intent.putExtra(DataPoint.VALUE, (String) value);
            } else {
                Log.w(TAG, "Error sending data point: unexpected data type! '" + dataType + "'");
            }
            intent.putExtra(DataPoint.TIMESTAMP, System.currentTimeMillis());
            context.startService(intent);
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
                        Log.e(TAG, "Error in update thread constructor:" + e.getMessage());
                    }
                }
            } else {
                try {

                    if (mmInStream == null || mmOutStream == null) {
                        mmInStream = btSocket1_6.getInputStream();
                        mmOutStream = btSocket1_6.getOutputStream();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in update thread constructor:" + e.getMessage());
                }
            }
        }

        @Override
        public void run() {
            // Keep listening to the InputStream until an exception occurs
            if (hxmEnabled) {
                try {
                    boolean firstRun = true;
                    boolean done = false;
                    while (!done) {
                        Log.v(TAG, "Try to read from Bluetooth input stream...");
                        lastSampleTime = System.currentTimeMillis();
                        // Read from the InputStream
                        // the msg size is 60
                        byte[] buffer = new byte[60];
                        int bytes; // bytes returned from read()
                        bytes = mmInStream.read(buffer);
                        if (bytes > 0) {
                           // Log.d(TAG, "Read " + bytes + " bytes from Bluetooth input stream");
                            
                            // copy the buffer                           
                            byte[] newBuffer = new byte[bytes];
                            for (int i = 0; i < bytes; i++) {
//                            	int byteNr = buffer[i];
//                            	byteNr = byteNr < 0 ? byteNr+256:byteNr;
//                            	Log.d(TAG, ""+ byteNr);	
                                newBuffer[i] = buffer[i];
                            }                            
                                                       
                            done = processZHxMMessage.processMessage(newBuffer, !firstRun);
                            firstRun = false;
                        } else {
                            Log.d(TAG, "No bytes read from Bluetooth input stream");
                        }
                        buffer = null;
                    }

                    if (btSocket1_6 == null)
                        updateHandler
                                .postDelayed(updateThread = new UpdateThread(), updateInterval);
                    else
                        updateHandler
                                .postDelayed(updateThread = new UpdateThread(), updateInterval);

                } catch (Exception e) {
                    Log.e(TAG, "Error in receiving HxM data: ", e);

                    // re-connect
                    processZHxMMessage = null;
                    if (btSocket1_6 == null)
                        connectHandler.post(hxmConnectThread2_1 = new HxMConnectThread2_1());
                    else
                        connectHandler.post(hxmConnectThread1_6 = new HxMConnectThread1_6());
                    btSocket2_1 = null;
                    btSocket1_6 = null;
                }
            } else {
                Log.d(TAG, "HxM not enabled. Cancelling update thread");
                cancel();
            }
        }

        /* Call this from the main Activity to shutdown the connection */
        public void cancel() {
            try {
                Log.i(TAG, "Stopping the HxM service");
                processZHxMMessage = null;
                if (btSocket1_6 == null) {
                    btSocket2_1.close();
                    btSocket2_1 = null;
                } else {
                    btSocket1_6.closeSocket();
                    btSocket1_6 = null;
                }

            } catch (Exception e) {
                Log.e(TAG, "Error in stopping the servicve:" + e.getMessage());
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
                    connectionErrorCount++;
                    if (connectionErrorCount > 30) // set to 5 minutes => 30
                    {
                        sendNotification("Error connecting to Zephyr HxM module, please enable the device.");
                        connectionErrorCount = 0;
                    }
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
                    Log.v(TAG, "Bluetooth state changed");

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

            // check is sensor is enabled
            if (!hxmEnabled) {
                Log.d(TAG, "Sensor is not enabled, skipping update thread.");
                stop();
                return;
            }

            streamEnabled = false;
            if (btAdapter.isEnabled()) {

                // check if there is a paired device with the name HxM
                Set<android.bluetooth.BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
                // If there are paired devices
                boolean foundDevice = false;
                if (pairedDevices != null) {
                    // Loop through paired devices
                    for (BluetoothDevice device : pairedDevices) {
                        Log.v(TAG, "Found paired device '" + device.getName() + "'");

                        // Add the name and address to an array adapter to show in a ListView
                        if (device.getName().startsWith("HXM")
                                && device.getAddress().startsWith("00:07:80")) {
                            Log.v(TAG, "Connecting to device: " + device.getName());
                            // Get a BluetoothSocket to connect with the BluetoothDevice
                            try {
                            	
                                //btSocket2_1 = device.createInsecureRfcommSocketToServiceRecord(serial_uid);
                            	btSocket2_1 = device.createRfcommSocketToServiceRecord(serial_uid);
                                btSocket2_1.connect();
                                processZHxMMessage = new ProcessZephyrHxMMessage(btSocket2_1
                                        .getRemoteDevice().getName());
                                updateHandler.post(updateThread = new UpdateThread());
                                foundDevice = true;

                            } catch (Exception e) {
                                connectionErrorCount++;
                                if (connectionErrorCount > 30) // set to 5 minutes => 30
                                {
                                    sendNotification("Error connecting to Zephyr HxM module, please enable the device.");
                                    connectionErrorCount = 0;
                                }
                                Log.e(TAG, "Error in connecting to HxM device!", e);
                            }
                        }
                    }
                }

                if (!foundDevice) {
                    Log.v(TAG, "No paired HxM device found. Sleeping for 10 seconds");
                    connectHandler.postDelayed(hxmConnectThread2_1 = new HxMConnectThread2_1(),
                            10000);
                }

            } else if (btAdapter.getState() == BluetoothAdapter.STATE_TURNING_ON) {
                // listen for the adapter state to change to STATE_ON
                Log.v(TAG, "Bluetooth is not enabled! Wait for it to change states...");
                context.registerReceiver(btReceiver, new IntentFilter(
                        BluetoothAdapter.ACTION_STATE_CHANGED));
            } else {
                // ask user for permission to start Bluetooth
                Log.v(TAG, "Bluetooth is not enabled! Asking user to start Bluetooth...");
                Intent startBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startBt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(startBt);

                // listen for the adapter state to change to STATE_ON
                context.registerReceiver(btReceiver, new IntentFilter(
                        BluetoothAdapter.ACTION_STATE_CHANGED));
            }
        }

        public void stop() {
            try {
                Log.i(TAG, "Stopping the HxM service");
                updateHandler.removeCallbacks(updateThread);
                btSocket2_1.close();

                context.unregisterReceiver(btReceiver);

            } catch (Exception e) {
                Log.e(TAG, "Error in stopping the HxM service:" + e.getMessage());
            }
        }

    }

    void sendNotification(String text) {
        NotificationManager mgr = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        int icon = R.drawable.ic_stat_notify_sense_alert;
        CharSequence tickerText = text;
        long when = System.currentTimeMillis();

        Notification note = new Notification(icon, tickerText, when);
        note.defaults |= Notification.FLAG_ONLY_ALERT_ONCE | Notification.DEFAULT_ALL;
        note.flags |= Notification.FLAG_AUTO_CANCEL | Notification.FLAG_ONLY_ALERT_ONCE;
        final Intent notifIntent = new Intent(context.getString(R.string.action_sense_app));
        notifIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notifIntent, 0);
        final CharSequence contentTitle = "Sense Platform";
        note.setLatestEventInfo(context, contentTitle, tickerText, contentIntent);
        mgr.notify(2, note);

    }

    private static final String TAG = "Zephyr HxM";
    private BluetoothAdapter btAdapter;
    private final Context context;
    private boolean hxmEnabled = false;
    private UUID serial_uid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    HandlerThread connectHT = null;
    HandlerThread updateHT = null;    
    private Handler connectHandler = null;//new Handler(Looper.getMainLooper());
    private Handler updateHandler = null; //new Handler(Looper.getMainLooper());
    private int updateInterval = 0;
    private UpdateThread updateThread = null;
    @SuppressWarnings("unused")
    private long lastSampleTime = 0;
    private HxMConnectThread2_1 hxmConnectThread2_1 = null;
    private HxMConnectThread1_6 hxmConnectThread1_6 = null;
    @SuppressWarnings("unused")
    private boolean streamEnabled = false; // TODO use this
    private BluetoothSocket btSocket2_1 = null;
    private it.gerdavax.android.bluetooth.BluetoothSocket btSocket1_6 = null;
    private ProcessZephyrHxMMessage processZHxMMessage = null;
    @SuppressWarnings("unused")
    private boolean notifyOnNoConnection = true;
    private boolean notifyOnEmptyBattery = true;
    private int connectionErrorCount = 0;
    private long lastEmptyBatteryNotify = 0;
    private long lastNoChestConnectionNofify = 0;

    public ZephyrHxM(Context context) {
        this.context = context;
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
    }

    public void startHxM(int interval) {
        Log.v(TAG, "Start HxM...");

        updateInterval = interval;
        hxmEnabled = true;
        // create the handler threads
        connectHT = new HandlerThread("Hxm Connect Handler Thread");
        updateHT = new HandlerThread("Hxm Update Handler Thread"); 
        connectHT.start();
        updateHT.start();
        
        // get the looper
        connectHandler = new Handler(connectHT.getLooper());
        updateHandler = new Handler(updateHT.getLooper());
        
        Thread t = new Thread() {

            @Override
            public void run() {
                // Check if the phone version, if it is lower than, 2.1 use the bluetooth lib
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR) {
                    Log.v(TAG, "Use old Bluetooth libary");
                    connectHandler.post(hxmConnectThread1_6 = new HxMConnectThread1_6());
                } else {
                    Log.v(TAG, "Use regular Bluetooth API");
                    connectHandler.post(hxmConnectThread2_1 = new HxMConnectThread2_1());
                }
            }
        };
        this.connectHandler.post(t);
    }

    public void stopHxM() {
        Log.v(TAG, "Stop HxM...");        
        
        hxmEnabled = false;
        try {        		
	            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR) {
	                if (hxmConnectThread1_6 != null) {
	                    hxmConnectThread1_6.stop();
	                    connectHandler.removeCallbacks(hxmConnectThread1_6);
	                }
	            } else {
	                if (hxmConnectThread2_1 != null) {
	                    hxmConnectThread2_1.stop();
	                    connectHandler.removeCallbacks(hxmConnectThread2_1);
	                }
	            }
	            connectHT.getLooper().quit();
        		updateHT.getLooper().quit(); 
        } catch (Exception e) {
            Log.e(TAG, "Exception in stopping Bluetooth scan thread:", e);
        }
    }
}
