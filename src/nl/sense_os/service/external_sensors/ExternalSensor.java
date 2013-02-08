/**************************************************************************************************
 * Copyright (C) 2011 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.external_sensors;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

/**
 * Generic class to represent an external sensor that needs to connect to the phone via Bluetooth.
 * 
 * @author Roelof van den Berg <roelof@sense-os.nl>
 */
public abstract class ExternalSensor {
    // static device specifics
    // TODO terug goedmaken
    protected static final String TAG = "OBD-II";

    // static connection specifics
    protected static final UUID serial_uuid = UUID
            .fromString("00001101-0000-1000-8000-00805f9b34fb");;

    // final parameters
    protected final Context context;
    protected final HandlerThread updateHandlerThread;
    protected final Handler updateHandler;

    // variables
    private int updateinterval;
    protected BluetoothAdapter mBluetoothAdapter = null;
    protected BluetoothSocket mBluetoothSocket = null;
    protected UpdateThread updateThread;
    protected LinkedList<Runnable> runningThreads = new LinkedList<Runnable>();
    protected InputStream mmInStream;
    protected OutputStream mmOutStream;

    // flag values
    protected boolean connected = false;

    public ExternalSensor(Context context) {
        this.context = context;
        updateHandlerThread = new HandlerThread("HandlerThread for External Sensor");
        updateHandlerThread.start();
        updateHandler = new Handler(updateHandlerThread.getLooper());
        Log.v(TAG, "created " + this.getClass().toString());
    }

    public void start(int interval) {
        Log.v(TAG, "starting " + this.getClass().toString());
        this.setUpdateInterval(interval);
        this.startAdapter();
    }

    public void stop() {
        // if connected, close the connection
        Log.v(TAG, "stopping " + this.getClass().toString());
        // stop listening for bluetooth changes
        try {
            context.unregisterReceiver(bluetoothReceiver);
        } catch (Exception e) {
            Log.e(TAG,
                    "unable to unregister bluetoothReceiver, probably due to it not being registred");
        }
        // if (connected) {
        // try to close the bluetooth socket
        try {
            mBluetoothSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Error on stopping service", e);
        }
        connected = false;
        // and stop polling the sensor, stop all running threads
        for (Runnable r : runningThreads) {
            updateHandler.removeCallbacks(r);
        }
        runningThreads.clear();
        // }
        Log.v(TAG, this.getClass().toString() + " stopped");
    }

    public void setUpdateInterval(int interval) {
        this.updateinterval = interval;
    }

    protected int getUpdateInterval() {
        return this.updateinterval;
    }

    /**
     * Tries to connect to the external sensor using the local {@link BluetoothAdapter}
     */
    protected void startAdapter() {
        Log.v(TAG, "starting adapter " + this.getClass().toString());
        // Get local BluetoothAdapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // if there is at least a BluetoothAdapter
        if (mBluetoothAdapter != null) {
            // if the adapter is enabled, run the sensor setup
            if (mBluetoothAdapter.isEnabled()) {
                this.connectSensor();
            }
            // if the adapter is not enabled, request it to be enabled.
            // sensor setup will be called through onReceive(Context, Intent) method
            else {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                enableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(enableIntent);
                context.registerReceiver(bluetoothReceiver, new IntentFilter(
                        BluetoothAdapter.ACTION_STATE_CHANGED));
            }
        }
        // if there is no BluetoothAdapter at all, stop the sensor.
        else {
            this.stop();
        }
    }

    /**
     * listen to State-changes of BlueTooth Adapters using a BroadcastReceiver
     */
    private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "state change sensed by " + this.getClass().toString());
            String action = intent.getAction();
            // if the state has changed, and the BluetoothAdapter has been turned on, setup the
            // sensor
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.STATE_OFF);
                if (state == BluetoothAdapter.STATE_ON) {
                    stop();
                    connectSensor();
                }
            }
        }
    };

    protected boolean connectSensor() {
        // only attempt to setup the sensor if this has not yet been done
        if (!connected) {
            Log.v(TAG, "attempting to connect Sensor " + this.getClass().toString());
            // first, get the paired devices
            Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter()
                    .getBondedDevices();

            // then find which of these devices is the sensor we look for
            for (BluetoothDevice currentdevice : pairedDevices) {
                // check if this is the correct device
                if (isDevice(currentdevice)) {
                    // try to connect
                    connected = tryConnectSocket(currentdevice);
                    if (connected) {
                        Log.v(TAG, "Socket is connected, starting updateThread");
                        updateThread = new UpdateThread();
                        updateHandler.post(updateThread);
                        runningThreads.add(updateThread);
                        return true;
                    }
                }
            }
            Log.v(TAG, "No suitable device was found to connect to");
        } else {
            Log.v(TAG, "connected is already set to true? " + connected);
        }
        return false;
    }

    /**
     * 
     * @param dev
     *            the device to be tested
     * @return whether or not the device is the device for this Sensor
     */
    public abstract boolean isDevice(BluetoothDevice dev);

    /**
     * 
     * @return whether or not a Socket connection was established
     */
    protected boolean tryConnectSocket(BluetoothDevice dev) {
        Log.v(TAG, "Attempting to connect to bluetooth socket");
        try {
            mBluetoothSocket = dev.createRfcommSocketToServiceRecord(serial_uuid);
            mBluetoothSocket.connect();
            Log.v(TAG, "Connected to socket via normal method");
            return true;
        } catch (IOException e) {
            Log.w(TAG, "Standard method for socket connection failed");
            Log.v(TAG, "Attempting reflection work aroud for connecting socket");
            try {
                Method m = dev.getClass()
                        .getMethod("createRfcommSocket", new Class[] { int.class });
                mBluetoothSocket = (BluetoothSocket) m.invoke(dev, Integer.valueOf(1));
                mBluetoothSocket.connect();
                Log.v(TAG, "Connected to socket via reflection work aroud");
                return true;
            }
            // if all has failed, stop this sensor
            catch (Exception ex) {
                Log.w(TAG, "workaround method also failed to connect to " + dev.getName());
                return false;
            }
        } catch (Exception ex) {
            Log.e(TAG, "attempt at connecting socket to " + dev.getName() + "failed", ex);
            return false;
        }
    }

    protected String devicename;
    protected String deviceadress;

    public class UpdateThread implements Runnable {
        private boolean sensorinitialized = false;

        public UpdateThread() {
            if (mBluetoothSocket != null) {
                devicename = mBluetoothSocket.getRemoteDevice().getName();
                deviceadress = mBluetoothSocket.getRemoteDevice().getAddress();
                boolean gelukt = (new OBD2SensorRegistrator(context)).verifySensorIds(devicename,
                        deviceadress);
                Log.v(TAG, "HOI is het gelukt? " + gelukt);
                Log.v(TAG, "HALLO1 devicename: " + devicename + ", deviceadress: " + deviceadress);
                if (mmInStream == null || mmOutStream == null) {
                    try {
                        mmInStream = mBluetoothSocket.getInputStream();
                        mmOutStream = mBluetoothSocket.getOutputStream();
                    } catch (Exception e) {
                        Log.e(TAG, "Error in UpdateThread constructor:" + e.getMessage());
                    }
                }
            } else {
                Log.e(TAG,
                        "Do not attempt to create an UpdateThread without a connected mBluetoothSocket");
            }
        }

        @Override
        public void run() {
            // while the connection is alive, poll every updateInterval
            long nextpoll, sleeptimer;
            while (connected) {
                nextpoll = System.currentTimeMillis() + updateinterval;
                if (sensorinitialized)
                    pollSensor();
                else
                    sensorinitialized = initializeSensor();
                try {
                    if ((sleeptimer = nextpoll - System.currentTimeMillis()) > 0)
                        Thread.sleep(sleeptimer);
                } catch (InterruptedException e) {
                    Log.w(TAG, "UpdateThread was interrupted in its sleep");
                }

            }
        }

        /* Call this from the main Activity to shutdown the connection */
        public void cancel() {
            try {
                Log.i(TAG, "Stopping the External Sensor");
                if (mBluetoothSocket != null) {
                    mBluetoothSocket.close();
                    mBluetoothSocket = null;
                }
                sensorinitialized = false;
            } catch (Exception e) {
                Log.e(TAG, "Error in stopping the servicve:" + e.getMessage());
            }
        }
    }

    /**
     * Tries to initialize the sensor. Is called from the Run method of the UpdateThread
     * 
     * @return whether or not the sensor initialization was succesfull
     */
    protected abstract boolean initializeSensor();

    /**
     * Polls the sensor. Is called from the Run method of the UpdateThread
     * 
     */
    protected abstract void pollSensor();
}