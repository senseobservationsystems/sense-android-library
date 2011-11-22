/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.external_sensors;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

/**
 * @author roelofvandenberg
 * 
 */
public class OBD2Dongle {
    // static device specifics
    private static final String TAG = "OBD-II";
    private static String deviceType = "TestOBD";

    // static connection specifics
    private static final UUID serial_uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");;

    // device variables
    private final Context context;
    private boolean dongleEnabled = false;
    private int updateInterval = 0;

    // connection thread variables
    private BluetoothAdapter btAdapter = null;
    private boolean connectionActive = false;
    private final Handler connectHandler = new Handler(Looper.getMainLooper());
    private ConnectThread connectThread = null;
    private BluetoothSocket socket = null;

    // update thread variables
    private boolean updateActive;
    private Handler updateHandler = new Handler(Looper.getMainLooper());
    private UpdateThread updateThread = null;
    private long lastSampleTime = 0;
    boolean[] pidAvailable; // whether or not certain PIDs are available for this car
    String vin = ""; // Vehicle Identification Number

    public OBD2Dongle(Context context) {
        this.context = context;
    }

    /**
     * start reading the OBD2Dongle by adding a ConnectThread to the connectHandler
     * 
     * @param interval
     *            in milliseconds
     */
    public void start(int interval) {
        Log.e(TAG, "Starting dongle:");
        this.setUpdateInterval(interval);
        dongleEnabled = true;

        Thread t = new Thread() {
            @Override
            public void run() {
                // No check on android version, assume 2.1 or higher
                connectHandler.post(connectThread = new ConnectThread());
            }
        };
        this.connectHandler.post(t);
    }

    /**
     * stop reading the OBD2Dongle, also removing its threads from the connectHandler
     */
    public void stop() {
        this.dongleEnabled = false;
        this.connectionActive = false;
        this.updateActive = false;
        try {
            // No check on android version, assume 2.1 or higher
            if (connectThread != null) {
                connectThread.stop();
                connectHandler.removeCallbacks(connectThread);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in stopping Bluetooth scan thread:", e);
        }
    }

    /**
     * 
     * @return updateInterval in milliseconds
     */
    public int getUpdateInterval() {
        return this.updateInterval;
    }

    /**
     * 
     * @param interval
     *            in milliseconds
     */
    public void setUpdateInterval(int interval) {
        this.updateInterval = interval;
    }

    public class ConnectThread implements Runnable {

        private BroadcastReceiver btReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (!dongleEnabled) {
                    return;
                }

                String action = intent.getAction();

                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.STATE_OFF);
                    if (state == BluetoothAdapter.STATE_ON) {
                        stop();
                        connectHandler.post(connectThread = new ConnectThread());
                        return;
                    }
                }
            }
        };

        /*
         * Connect to the default BluetTooth adapter
         */
        public ConnectThread() {
            // send address
            try {
                btAdapter = BluetoothAdapter.getDefaultAdapter();
            } catch (Exception e) {
                Log.e(TAG, "Exception preparing Bluetooth scan thread:", e);
            }
        }

        @Override
        public void run() {
            Log.v(TAG, ">>>>>>>>>> RUN CONNECT THREAD <<<<<<<<");

            if (dongleEnabled) {
                connectionActive = false;
                if (btAdapter.isEnabled()) {
                    boolean foundDevice = false;

                    // check if there is a paired device with the name BioHarness
                    Set<android.bluetooth.BluetoothDevice> pairedDevices = btAdapter
                            .getBondedDevices();
                    // If there are paired devices
                    if (pairedDevices.size() > 0) {
                        // Search for the correct Bluetooth Devices
                        for (BluetoothDevice device : pairedDevices) {
                            // device name specific
                            if (device.getName().contains("OBDII")) {
                                Log.v(TAG, ">>>>>>>>>> Found OBDII device <<<<<<<<");
                                // Get a BluetoothSocket to connect with the given BluetoothDevice
                                try {
                                    socket = device.createRfcommSocketToServiceRecord(serial_uuid);
                                    socket.connect();
                                    connectionActive = true;
                                } catch (Exception e1) {
                                    Log.e(TAG, "Error creating socket to " + device.getName()
                                            + ", attempting workaround ");
                                    // if creating the socket did not work, try the workaround
                                    try {
                                        Method m = device.getClass().getMethod(
                                                "createRfcommSocket", new Class[] { int.class });
                                        socket = (BluetoothSocket) m.invoke(device, 1);
                                        socket.connect();
                                        connectionActive = true;
                                    } catch (Exception e2) {
                                        Log.e(TAG, "Error creating socket to " + device.getName()
                                                + " in workaround", e2);
                                    }
                                }
                                if (connectionActive) {
                                    deviceType = device.getName();
                                    updateHandler.post(updateThread = new UpdateThread());
                                    foundDevice = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (!foundDevice) {
                        // Log.v(TAG, "No Paired BioHarness device found. Sleeping for 10 seconds");
                        connectHandler.postDelayed(connectThread = new ConnectThread(), 10000);
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
            connectionActive = false;
            updateActive = false;
            try {
                if (updateHandler != null)
                    updateHandler.removeCallbacks(updateThread);
                if (socket != null)
                    socket.close();
                context.unregisterReceiver(btReceiver);

            } catch (IllegalArgumentException e) {
                // ignore
            } catch (Exception e) {
                Log.e(TAG, "Error in stopping OBD2Dongle service" + e.getMessage());
            }
        }

    }

    public class UpdateThread extends Thread {
        private InputStream sockInputStream;
        private OutputStream sockOutputStream;

        /**
         * Create an UpdateThread object which has the sockInStream and sockOutStream connected with
         * the BlueTooth socket
         */
        public UpdateThread() {
            if (sockInputStream == null || sockOutputStream == null) {
                // using temporary objects because member streams are final
                InputStream tempInputStream = null;
                OutputStream tempOutputStream = null;

                try {
                    tempInputStream = socket.getInputStream();
                    tempOutputStream = socket.getOutputStream();
                } catch (Exception e) {
                    Log.e(TAG, "Error in update thread constructor:" + e.getMessage());
                }
                sockInputStream = tempInputStream;
                sockOutputStream = tempOutputStream;
                Log.v(TAG, "socket: " + socket);
                Log.v(TAG, "socketInputStream: " + sockInputStream);
                Log.v(TAG, "socketOutputStream: " + sockOutputStream);
            }
        }

        @Override
        public void run() {
            Log.v(TAG, ">>>>>>>>>> RUN UPDATE THREAD <<<<<<<<");
            if (dongleEnabled && connectionActive) {
                try {
                    if (!updateActive) {
                        // initialize the datastream by checking available PIDs and VIN
                        bruteForceTryOut();
                        updateActive = initializeDataStream();
                        updateHandler.post(updateThread = new UpdateThread());
                    }
                    // TODO not necessary to check connection alive now, is it?
                    else if (System.currentTimeMillis() > lastSampleTime + updateInterval) {
                        // invoke dynamic data gathering subroutines
                        updateDTCStatus();
                        updateFuelStatus();
                        updateEngineLoad();
                        updateEngineCoolant();
                        updateFuelPercentTrim();
                        updateFuelPressure();
                        updateIntakeManifoldPressure();
                        updateEngineRPM();
                        updateVehicleSpeed();
                        updateTimingAdvance();
                        updateIntakeAirTemperature();
                        updateMAFAirFlowRate();
                        updateThrottlePosition();
                        updateCommandedSecondaryAirStatus();
                        updateOxygenSensors();
                        updateAuxiliaryInput();
                        updateRunTime();
                        // TODO these methods cover the PIDs until Mode 1 PID 0x1F
                    }
                    // update a new upDateThread every second
                    updateHandler.postDelayed(updateThread = new UpdateThread(), 1000);
                } catch (Exception e) {
                    Log.e(TAG, "Error in update cycle while reading OBDII data: ", e);
                    // re-connect
                    connectHandler.postDelayed(connectThread = new ConnectThread(), 1000);
                }
            } else
                cancel();
        }

        /* Call this from the main Activity to shutdown the connection */
        public void cancel() {
            try {
                Log.i(TAG, "Stopping the OBD2Dongle service");
                updateActive = false;
                socket.close();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }

        private final Handler inputHandler = new Handler(Looper.getMainLooper());

        protected void bruteForceTryOut() {
            // create and start a listening thread
            inputHandler.post(new listeningThread());

            // start sending queries (without listening, this is done by listening thread)
            for (int mode = 0; mode < 10; mode++) {
                for (int PID = 0; PID < 100; PID++) {
                    askByte(mode, PID);
                }
            }
        }

        protected void askByte(int mode, int PID) {
            try {
                // request data, first encode it from char to byte
                byte[] request = new byte[] { Byte.MIN_VALUE + 0x02,
                        (byte) (Byte.MIN_VALUE + mode), (byte) (Byte.MIN_VALUE + PID),
                        Byte.MIN_VALUE + 0x00, Byte.MIN_VALUE + 0x00, Byte.MIN_VALUE + 0x00,
                        Byte.MIN_VALUE + 0x00, Byte.MIN_VALUE + 0x00 };
                sockOutputStream.write(request);
                String message = "askByte (int): 0x02," + mode + "," + PID
                        + ",0x00,0x00,0x00,0x00,0x00";
                Log.w(TAG, message);
                message = "askByte (byte): ";
                for (int b : request)
                    message += (b + ",");
                Log.w(TAG, message);
                // writeToFile(request);
            } catch (Exception e) {
                Log.e(TAG, "error in askByte: ", e);
            }
        }

        public class listeningThread extends Thread {

            public listeningThread() {
            }

            public int decryptByte(byte b) {
                if (b < 0)
                    return Byte.MAX_VALUE - b;
                else
                    return (int) b;
            }

            public byte encryptByte(int i) {
                if (i > Byte.MAX_VALUE)
                    return (byte) -(i - Byte.MAX_VALUE);
                else
                    return (byte) i;
            }

            /*
             * public void intrun() { Log.v(TAG, ">>>>>>>>RUN Listening Thread<<<<<<<<"); int[]
             * dataframe = null; int resultIndex = 0; int currentByte = -1; //listen to the
             * datainputstream //TODO: set the variable in the while loop to a changeable variable
             * while(connectionActive && updateActive){ try{ currentByte = sockInputStream.read();
             * //sleep for a while if no input is available if(currentByte == -1){
             * Thread.sleep(500); Log.v(TAG,
             * "LT: Sleeping, while sockInputStream.available = "+sockInputStream
             * .available()+" and currentByte = "+currentByte ); } Log.v(TAG,
             * "LT: encrypted currentByte = "+currentByte); currentByte = decryptByte(currentByte);
             * Log.v(TAG, "LT: decrypted currentByte = "+currentByte); //if the current Byte read
             * has a value > 0 and no result array has been created, //it indicates the start of a
             * data-frame, first byte names its size if(dataframe == null && currentByte > 0 &&
             * currentByte <8){ dataframe = new int[currentByte]; Log.v(TAG,
             * "LT: Created new dataframe of size "+dataframe.length); } //while reading values,
             * fill the result array else if(dataframe != null && resultIndex<dataframe.length &&
             * currentByte>0){ dataframe[resultIndex] = currentByte; resultIndex++; //check if
             * data-frame has been collected, if so, post it if(resultIndex==dataframe.length){
             * //post the data-frame processDataFrame(dataframe); //reset the data-frame dataframe =
             * null; resultIndex = 0; Log.v(TAG, "LT: dataframe has been sent and reset"); } } }
             * catch (Exception e){ Log.e(TAG, "LT: problem in running listeningThread: ", e); } } }
             */

            public void run() {
                Log.v(TAG, ">>>>>>>>RUN Listening Thread<<<<<<<<");
                byte[] buffer = new byte[15];
                byte[] dataframe = new byte[8];
                int offset = 0, bytesread = 0;
                int dataframestart = 0;
                // make it possible to write a logfile
                initWriteToFile();
                // TODO: set the variable in the while loop to a changeable variable
                while (connectionActive && updateActive) {
                    try {
                        bytesread = sockInputStream.read(buffer, offset, buffer.length - offset);
                        // sleep for a while if no input is available
                        if (bytesread == -1) {
                            Thread.sleep(500);
                            Log.v(TAG, "LT: Sleeping, while bytesread = " + bytesread
                                    + " and buffer " + buffer);
                        } else {
                            offset += bytesread;
                        }
                        if (offset == buffer.length) {
                            // locate the start of the dataframe in the buffer
                            while (buffer[dataframestart] == 0 && dataframestart < buffer.length) {
                                dataframestart++;
                            }
                            // shift the dataframe to the front of the buffer
                            if (dataframestart > 0) {
                                // shift dataframe to the front of the buffer
                                System.arraycopy(buffer.clone(), dataframestart, buffer, 0,
                                        buffer.length - dataframestart);
                                // set the pointer to start of frame to 0
                                dataframestart = 0;
                                // adjust the offset
                                offset -= dataframestart;
                                // reset the last #datastart bytes of the buffer
                                System.arraycopy(new byte[dataframestart], 0, buffer, buffer.length
                                        - 1 - dataframestart, dataframestart);
                            }
                            // if possible, extract the dataframe from the buffer (first 8 bytes in
                            // buffer)
                            if (dataframestart < buffer.length - 9 && buffer[0] < 8) {
                                // extract dataframe (first 8 bytes according to protocol) from the
                                // buffer
                                System.arraycopy(buffer, 0, dataframe, 0, 8);
                                // remove dataframe from the buffer
                                System.arraycopy(buffer.clone(), 8, buffer, 0, buffer.length - 8);
                                // adjust the offset
                                offset -= 8;
                                // reset the last 8 bytes of the buffer
                                System.arraycopy(new byte[8], 0, buffer, buffer.length - 1 - 8, 8);
                                // process the dataframe
                                int[] decodedDataFrame = decodeDataFrame(dataframe);
                                processDataFrame(decodedDataFrame);
                                // reset the data-frame
                                dataframe = new byte[8];
                                Log.v(TAG, "LT: dataframe has been sent and reset");
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "LT: problem in running listeningThread: ", e);
                    }
                }
            }

            public void cancel() {
                updateActive = false;
                Log.i(TAG, "Cancel method of listeningThread called");
            }

            protected int[] decodeDataFrame(byte[] data) {
                int[] readable = new int[data.length];
                for (byte i = 0; i < readable.length; i++) {
                    if (data[i] < 0)
                        readable[i] = Math.abs(data[i]) + Byte.MAX_VALUE;
                    else
                        readable[i] = data[i];
                }
                // alternative method 1
                /*
                 * for(byte i = 0; i<readable.length; i++){ if(data[i] == Byte.MIN_VALUE)
                 * readable[i] = 128; else if(data[i] < 0) readable[i] = Math.abs(data[i]) +
                 * Byte.MAX_VALUE+1; else readable[i] = data[i]; }
                 */
                return readable;
            }

            protected void processDataFrame(int[] readable) {
                if (readable[0] >= 2) {
                    int mode = readable[0];
                    int PID = readable[1];
                    String message = "dataframe found: <mode: " + mode + ", PID: " + PID
                            + ", data: ";
                    for (int i = 2; i < readable.length; i++)
                        message += ";" + readable[i];
                    message += ">";
                    writeToFile(readable, "decoded dataframe ");
                    Log.v(TAG, message);
                } else
                    Log.e(TAG, "dataframe invalid");
            }

        }

        /**
         * 
         * @return whether or not the initialization was successful
         */
        private boolean initializeDataStream() {
            try {
                // initialize pidAvailable
                pidAvailable = new boolean[(byte) 0x60 + 1];

                boolean[] current;
                // Mode01, PID00/PID20/PID40 : check if PIDs 0x01 - 0x60 are available
                for (char index : new char[] { 0x00, 0x20, 0x60 }) {
                    Log.v(TAG, "initializing, index: " + index);
                    current = queryBit((char) 0x01, index);
                    if (current != null) {
                        pidAvailable[index] = true;
                        System.arraycopy(current, 0, pidAvailable, index + 1, current.length);
                    }
                }
                String message = ">>>>>>>>>>>>>>>>>>>>>> pidAvailable: ";
                for (boolean activePID : pidAvailable) {
                    if (activePID)
                        message += "T,";
                    else
                        message += "F,";
                }
                Log.i(TAG, message);

                // deviceType += " (" + getOBDStandards() + ")";

                // TODO add code to find out about the VIN
                // found at mode 09, PID 01 and 02
                // using the ISO 15765-2 protocol

                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error in initialization of data stream: ", e);
                return false;
            }
        }

        private String strStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        private String strFileDestination = "/sense/datalog.log";

        private void initWriteToFile() {
            Log.v(TAG, ">>>>>>>> INITIALIZING WRITE TO FILE <<<<<<<<");
            try {
                // try creating the file if it not already exists
                new File(strStoragePath + strFileDestination).createNewFile();
                String command = "chmod 666 " + strStoragePath + strFileDestination;
                Runtime.getRuntime().exec(command);
                Log.i(TAG, "Initialized file: " + strStoragePath + strFileDestination);
            } catch (Exception e) {
                Log.e(TAG,
                        "Error in initializing the file: " + strStoragePath + strFileDestination, e);
            }
        }

        private void writeToFile(int[] buffer, String identifier) {
            Log.v(TAG, ">>>>>>>> WRITING TO FILE <<<<<<<<");
            try {
                FileOutputStream fos = new FileOutputStream(strStoragePath + strFileDestination);
                for (int i : buffer) {
                    fos.write(i);
                }
                fos.close();
                Log.i(TAG, " wrote " + identifier + buffer.toString() + " to file "
                        + strStoragePath + strFileDestination);
            } catch (Exception e) {
                Log.e(TAG, "Error in logging to file: " + strStoragePath + strFileDestination, e);
            }
        }

        /**
         * 
         * @param mode
         *            indicating mode of operation as described in the latest OBD-II standard SAE
         *            J1979
         * @param PID
         *            coded standard OBD-II PID as defined by SAE J1979
         * @return the data bytes found in the OBD-II response
         */
        private char[] queryByte(char mode, char PID) {
            if (!(mode <= Byte.MAX_VALUE) || !(PID <= Byte.MAX_VALUE)) {
                Log.e(TAG, "queryByte received invalid mode (" + mode + ") or PID (" + PID + ")");
                return null;
            }
            try {
                // request data, first encode it from char to byte
                byte[] request = new byte[] { Byte.MIN_VALUE + 0x02,
                        (byte) (Byte.MIN_VALUE + mode), (byte) (Byte.MIN_VALUE + PID),
                        Byte.MIN_VALUE + 0x00, Byte.MIN_VALUE + 0x00, Byte.MIN_VALUE + 0x00,
                        Byte.MIN_VALUE + 0x00, Byte.MIN_VALUE + 0x00 };
                sockOutputStream.write(request);
                String message = "queryByte request (byte): ";
                for (byte b : request)
                    message += (b + ",");
                Log.w(TAG, message);
                // writeToFile(request);

                // read the response, sleep while waiting
                /*
                 * while(sockInputStream.available()<8){ Thread.sleep(500); }
                 */
                byte[] encodedresponse = new byte[8];
                sockInputStream.read(encodedresponse);
                char[] response = new char[encodedresponse.length];
                message = "queryByte response (byte): ";
                for (int i = 0; i < encodedresponse.length; i++) {
                    response[i] = (char) (encodedresponse[i] - Byte.MIN_VALUE);
                    message += (encodedresponse[i] + ",");
                }
                Log.w(TAG, message);
                // writeToFile(encodedresponse);

                Log.e(TAG, "buffer (mode=" + mode + ", PID=" + PID + ") found: " + response[0]
                        + "," + response[1] + "," + response[2] + "," + response[3] + ","
                        + response[4] + "," + response[5] + "," + response[6] + "," + response[7]);
                Log.v(TAG, "buffer[1] - (byte) 0x40 = " + (response[1] - (byte) 0x40));
                Log.v(TAG, "buffer[2] = " + response[2]);
                if (response[1] == mode && response[2] == PID && response[0] >= 2) {
                    char[] result = new char[response[0] - 2];
                    System.arraycopy(response, 2, result, 0, result.length);
                    Log.w(TAG, "correct buffer read for mode=" + mode + ",PID=" + PID
                            + ", result.length=" + result.length);
                    return result;
                } else {
                    Log.w(TAG, "No valid response gotten in queryByte(mode=" + mode + ",PID=" + PID
                            + ")");
                    return null;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in exchanging data: ", e);
                return null;
            }
        }

        /**
         * 
         * @param mode
         *            indicating mode of operation as described in the latest OBD-II standard SAE
         *            J1979
         * @param PID
         *            coded standard OBD-II PID as defined by SAE J1979
         * @return the bit representation of the data found in the OBD-II response
         */
        private boolean[] queryBit(char mode, char PID) {
            char[] input = queryByte(mode, PID);
            if (input != null) {
                Log.e(TAG, "queryBit got " + input.length + " bytes (in a char[]) from queryByte");
                boolean[] result = new boolean[input.length * 8];
                for (byte byteIndex = 0; byteIndex < input.length; byteIndex++) {
                    for (byte bitIndex = 0; bitIndex < 8; bitIndex++) {
                        result[byteIndex * 8 + bitIndex] = ((input[byteIndex]
                                & (byte) (bitIndex + 1) ^ 2) != (byte) 0x00);
                    }
                }
                return result;
            } else {
                return null;
            }
        }

        /**
         * 
         * @param sensor_name
         *            a sensor name String gotten from the SensorData class
         * @param value
         *            prepared object
         */
        private void sendIntent(String sensor_name, JSONObject value) {
            Intent i = new Intent(context.getString(R.string.action_sense_new_data));
            i.putExtra(DataPoint.SENSOR_NAME, sensor_name);
            i.putExtra(DataPoint.SENSOR_DESCRIPTION, deviceType);
            i.putExtra(DataPoint.VALUE, value.toString());
            i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
            i.putExtra(DataPoint.TIMESTAMP, System.currentTimeMillis());
            context.startService(i);
        }

        /**
         * 
         * @return whether or not the call was successful
         */
        private boolean updateDTCStatus() {
            char mode = (char) 0x01;
            char PID = (char) 0x01;
            // Check whether or not query of this PID is possible
            if (pidAvailable[PID]) {
                // Query for "monitor status since DTCs cleared".
                boolean[] response = queryBit(mode, PID);
                if (response != null) {
                    try {
                        JSONObject result = new JSONObject();
                        result.put("MIL on", response[0 * 8 + 7]);
                        // bit B readout
                        result.put("Misfire test available", response[1 * 8 + 0]);
                        result.put("Fuel System test available", response[1 * 8 + 1]);
                        result.put("Components test available", response[1 * 8 + 2]);
                        result.put("Misfire test complete", response[1 * 8 + 4]);
                        result.put("Fuel System test complete", response[1 * 8 + 5]);
                        result.put("Components test complete", response[1 * 8 + 6]);
                        // when Compression ignition monitors supported
                        if (response[1 * 8 + 3]) {
                            result.put("Type of ignition monitors supported", "compression");
                            result.put("Catalyst test available", response[2 * 8 + 0]);
                            result.put("Heated Catalyst test available", response[2 * 8 + 1]);
                            result.put("Evaporative System test available", response[2 * 8 + 2]);
                            result.put("Secondary Air System test available", response[2 * 8 + 3]);
                            result.put("A/C Refrigerant test available", response[2 * 8 + 4]);
                            result.put("Oxygen Sensor test available", response[2 * 8 + 5]);
                            result.put("Oxygen Sensor Heater test available", response[2 * 8 + 6]);
                            result.put("ERG System test available", response[2 * 8 + 7]);
                            result.put("Catalyst test complete", response[3 * 8 + 0]);
                            result.put("Heated Catalyst test complete", response[3 * 8 + 1]);
                            result.put("Evaporative System test complete", response[3 * 8 + 2]);
                            result.put("Secondary Air System test complete", response[3 * 8 + 3]);
                            result.put("A/C Refrigerant test complete", response[3 * 8 + 4]);
                            result.put("Oxygen Sensor test complete", response[3 * 8 + 5]);
                            result.put("Oxygen Sensor Heater test complete", response[3 * 8 + 6]);
                            result.put("ERG System test complete", response[3 * 8 + 7]);
                        }
                        // when Spark ignition monitors supported
                        else {
                            result.put("Type of ignition monitors supported", "spark");
                            result.put("NMHC Cat test available", response[2 * 8 + 0]);
                            result.put("NOx/SCR Monitor test available", response[2 * 8 + 1]);
                            result.put("Boost Pressure test available", response[2 * 8 + 3]);
                            result.put("Exhaust Gas Sensor test available", response[2 * 8 + 5]);
                            result.put("PM filter monitoring test available", response[2 * 8 + 6]);
                            result.put("EGR and/or VVT System test available", response[2 * 8 + 7]);
                            result.put("NMHC Cat test complete", response[3 * 8 + 0]);
                            result.put("NOx/SCR Monitor test complete", response[3 * 8 + 1]);
                            result.put("Boost Pressure test complete", response[3 * 8 + 3]);
                            result.put("Exhaust Gas Sensor test complete", response[3 * 8 + 5]);
                            result.put("PM filter monitoring test complete", response[3 * 8 + 6]);
                            result.put("EGR and/or VVT System test complete", response[3 * 8 + 7]);
                        }
                        sendIntent(SensorNames.MONITOR_STATUS, result);
                        return true;
                    } catch (JSONException e) {
                        Log.e(TAG, "Error in updateDTCStatus:" + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }

        /**
         * 
         * @return whether or not the call was successful
         */
        private boolean updateFuelStatus() {
            char mode = (char) 0x01;
            char PID = (char) 0x03;
            // Check whether or not query of this PID is possible
            if (pidAvailable[PID]) {
                boolean[] response = queryBit(mode, PID);
                if (response != null) {
                    try {
                        JSONObject result = new JSONObject();
                        // A request for this PID returns 2 bytes of data.
                        // The first byte describes fuel system #1.
                        // Only one bit should ever be set per system.
                        for (int system = 1; system < 3; system++) {
                            int numtrue = 0;
                            int loctrue = -1;
                            for (int index = Byte.SIZE * (system - 1); index < ((system - 1) * Byte.SIZE) + 8; index++) {
                                if (response[index]) {
                                    numtrue += 1;
                                    loctrue = index;
                                }
                            }
                            // only use result when valid
                            if (numtrue == 1) {
                                String value = "";
                                switch (loctrue) {
                                case 0:
                                    value = "Open loop due to insufficient engine temperature";
                                case 1:
                                    value = "Closed loop, using oxygen sensor feedback to determine fuel mix";
                                case 2:
                                    value = "Open loop due to engine load OR fuel cut due to deacceleration";
                                case 3:
                                    value = "Open loop due to system failure";
                                case 4:
                                    value = "Closed loop, using at least one oxygen sensor but there is a fault in the feedback system";
                                default:
                                    value = "unknown";
                                }
                                String name = String.format("Fuel system #%d status", system);
                                result.put(name, value);
                            }
                        }
                        sendIntent(SensorNames.FUEL_SYSTEM_STATUS, result);
                        return true;
                    } catch (JSONException e) {
                        Log.e(TAG, "Error in updateFuelStatus:" + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }

        /**
         * 
         * @return whether or not the call was successful
         */
        private boolean updateEngineLoad() {
            char mode = (char) 0x01;
            char PID = (char) 0x04;
            // Check whether or not query of this PID is possible
            if (pidAvailable[PID]) {
                char[] response = queryByte(mode, PID);
                if (response != null) {
                    try {
                        JSONObject result = new JSONObject();
                        double value = ((double) response[0]) * 100d / 255d;
                        result.put("Calculated engine load value (\u0025)", value);
                        sendIntent(SensorNames.ENGINE_LOAD, result);
                        return true;
                    } catch (JSONException e) {
                        Log.e(TAG, "Error in updateEngineLoad:" + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }

        private boolean updateEngineCoolant() {
            char mode = (char) 0x01;
            char PID = (char) 0x05;
            // Check whether or not query of this PID is possible
            if (pidAvailable[PID]) {
                char[] response = queryByte(mode, PID);
                if (response != null) {
                    try {
                        JSONObject result = new JSONObject();
                        int value = ((int) response[0]) - 40;
                        result.put("temperature (\u00B0C)", value);
                        sendIntent(SensorNames.ENGINE_COOLANT, result);
                        return true;
                    } catch (JSONException e) {
                        Log.e(TAG, "Error in updateEngineCoolant:" + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }

        private boolean updateFuelPercentTrim() {
            char mode = (char) 0x01;
            char[] PIDs = new char[] { 0x06, 0x07, 0x08, 0x09 };
            JSONObject result = new JSONObject();
            for (char PID : PIDs) {
                if (pidAvailable[PID]) {
                    char[] response = queryByte(mode, PID);
                    if (response != null) {
                        try {
                            double value = (((double) response[0]) - 128d) * (100d / 128d);
                            switch (PID) {
                            case 0x06:
                                result.put("Short term fuel % trim—Bank 1", value);
                            case 0x07:
                                result.put("Long term fuel % trim—Bank 1", value);
                            case 0x08:
                                result.put("Short term fuel % trim—Bank 2", value);
                            case 0x09:
                                result.put("Long term fuel % trim—Bank 2", value);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error in updateFuelPercentTrim:" + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
            if (result.length() > 0) {
                sendIntent(SensorNames.ENGINE_COOLANT, result);
                return true;
            }
            return false;
        }

        private boolean updateFuelPressure() {
            char mode = (char) 0x01;
            char PID = (char) 0x0A;
            // Check whether or not query of this PID is possible
            if (pidAvailable[PID]) {
                char[] response = queryByte(mode, PID);
                if (response != null) {
                    try {
                        JSONObject result = new JSONObject();
                        int value = ((int) response[0]) * 3;
                        result.put("Fuel pressure (kPa (gauge))", value);
                        sendIntent(SensorNames.FUEL_PRESSURE, result);
                        return true;
                    } catch (JSONException e) {
                        Log.e(TAG, "Error in updateFuelPressure:" + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }

        private boolean updateIntakeManifoldPressure() {
            char mode = (char) 0x01;
            char PID = (char) 0x0B;
            // Check whether or not query of this PID is possible
            if (pidAvailable[PID]) {
                char[] response = queryByte(mode, PID);
                if (response != null) {
                    try {
                        JSONObject result = new JSONObject();
                        char value = response[0];
                        result.put("Fuel pressure (kPa (gauge))", value);
                        sendIntent(SensorNames.INTAKE_PRESSURE, result);
                        return true;
                    } catch (JSONException e) {
                        Log.e(TAG, "Error in updateIntakeManifoldPressure:" + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }

        private boolean updateEngineRPM() {
            char mode = (char) 0x01;
            char PID = (char) 0x0C;
            // Check whether or not query of this PID is possible
            if (pidAvailable[PID]) {
                char[] response = queryByte(mode, PID);
                if (response != null) {
                    try {
                        JSONObject result = new JSONObject();
                        double value = ((((double) response[0]) * 256d) + ((double) response[1])) / 4d;
                        result.put("Engine RPM (rpm)", value);
                        sendIntent(SensorNames.ENGINE_RPM, result);
                        return true;
                    } catch (JSONException e) {
                        Log.e(TAG, "Error in updateEngineRPM:" + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }

        private boolean updateVehicleSpeed() {
            char mode = (char) 0x01;
            char PID = (char) 0x0D;
            // Check whether or not query of this PID is possible
            if (pidAvailable[PID]) {
                char[] response = queryByte(mode, PID);
                if (response != null) {
                    try {
                        JSONObject result = new JSONObject();
                        char value = response[0];
                        result.put("Vehicle speed (km/h)", value);
                        sendIntent(SensorNames.VEHICLE_SPEED, result);
                        return true;
                    } catch (JSONException e) {
                        Log.e(TAG, "Error in updateVehicleSpeed:" + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }

        private boolean updateTimingAdvance() {
            char mode = (char) 0x01;
            char PID = (char) 0x0E;
            // Check whether or not query of this PID is possible
            if (pidAvailable[PID]) {
                char[] response = queryByte(mode, PID);
                if (response != null) {
                    try {
                        JSONObject result = new JSONObject();
                        double value = ((double) response[0]) / 2d - 64d;
                        result.put("Timing advance (\u00B0 relative to #1 cylinder)", value);
                        sendIntent(SensorNames.TIMING_ADVANCE, result);
                        return true;
                    } catch (JSONException e) {
                        Log.e(TAG, "Error in updateTimingAdvance:" + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }

        private boolean updateIntakeAirTemperature() {
            char mode = (char) 0x01;
            char PID = (char) 0x0F;
            // Check whether or not query of this PID is possible
            if (pidAvailable[PID]) {
                char[] response = queryByte(mode, PID);
                if (response != null) {
                    try {
                        JSONObject result = new JSONObject();
                        int value = (int) response[0] - 40;
                        result.put("Intake air temperature (\u00B0C)", value);
                        sendIntent(SensorNames.INTAKE_TEMPERATURE, result);
                        return true;
                    } catch (JSONException e) {
                        Log.e(TAG, "Error in updateIntakeAirTemperature:" + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }

        private boolean updateMAFAirFlowRate() {
            char mode = (char) 0x01;
            char PID = (char) 0x10;
            // Check whether or not query of this PID is possible
            if (pidAvailable[PID]) {
                char[] response = queryByte(mode, PID);
                if (response != null) {
                    try {
                        JSONObject result = new JSONObject();
                        double value = (((double) response[0]) * 256d + ((double) response[1])) / 100d;
                        result.put("MAF air flow rate (gram/sec)", value);
                        sendIntent(SensorNames.MAF_AIRFLOW, result);
                        return true;
                    } catch (JSONException e) {
                        Log.e(TAG, "Error in updateMAFAirFlowRate:" + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }

        private boolean updateThrottlePosition() {
            char mode = (char) 0x01;
            char PID = (char) 0x11;
            // Check whether or not query of this PID is possible
            if (pidAvailable[PID]) {
                char[] response = queryByte(mode, PID);
                if (response != null) {
                    try {
                        JSONObject result = new JSONObject();
                        double value = ((double) response[0]) * 100d / 255d;
                        result.put("Throttle Position (%)", value);
                        sendIntent(SensorNames.THROTTLE_POSITION, result);
                        return true;
                    } catch (JSONException e) {
                        Log.e(TAG, "Error in updateThrottlePosition:" + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }

        private boolean updateCommandedSecondaryAirStatus() {
            char mode = (char) 0x01;
            char PID = (char) 0x12;
            // Check whether or not query of this PID is possible
            if (pidAvailable[PID]) {
                boolean[] response = queryBit(mode, PID);
                if (response != null) {
                    try {
                        JSONObject result = new JSONObject();
                        // Only one bit should ever be set per system.
                        int numtrue = 0;
                        int loctrue = -1;
                        for (int index = 0; index < +8; index++) {
                            if (response[index]) {
                                numtrue += 1;
                                loctrue = index;
                            }
                        }
                        // only use result when valid
                        if (numtrue == 1) {
                            String value = "";
                            switch (loctrue) {
                            case 0:
                                value = "Upstream of catalytic converter";
                            case 1:
                                value = "Downstream of catalytic converter";
                            case 2:
                                value = "From the outside atmosphere or off";
                            default:
                                value = "unknown";
                            }
                            result.put("Commanded secondary air status", value);
                        }
                        sendIntent(SensorNames.AIR_STATUS, result);
                        return true;
                    } catch (JSONException e) {
                        Log.e(TAG, "Error in updateCommandedSecondaryAirStatus:" + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }

        private boolean updateOxygenSensors() {
            char mode = (char) 0x01;
            char PID = (char) 0x13;
            if (pidAvailable[PID]) {
                boolean[] sensorspresent = queryBit(mode, PID);
                if (sensorspresent != null) {
                    try {
                        JSONObject result = new JSONObject();
                        for (byte index = 0; index < 8; index++) {
                            if (sensorspresent[index]) {
                                char[] current = queryByte(mode, (char) (PID + index + 0x01));
                                if (current != null) {
                                    int bank = index < 4 ? 1 : 2;
                                    int sensor = index % 4;
                                    String name = String.format(
                                            "Bank %d, Sensor %d: Oxygen sensor voltage", bank,
                                            sensor);
                                    double value = ((double) current[0]) / 200d;
                                    result.put(name, value);
                                    if (current[1] != 0xFF) {
                                        name = String.format(
                                                "Bank %d, Sensor %d: Short term fuel trim", bank,
                                                sensor);
                                        value = (((double) current[1]) - 128d) * (100d / 128d);
                                        result.put(name, value);
                                    }
                                }
                            }
                        }
                        if (result.length() > 0) {
                            sendIntent(SensorNames.OXYGEN_SENSORS, result);
                            return true;
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error in updateOxygenSensors:" + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }

        private String getOBDStandards() {
            char mode = (char) 0x01;
            char PID = (char) 0x1C;
            // Check whether or not query of this PID is possible
            if (pidAvailable[PID]) {
                char[] response = queryByte(mode, PID);
                if (response != null) {
                    String value;
                    switch (response[0]) {
                    case 0x01:
                        value = "OBD-II as defined by the CARB";
                    case 0x02:
                        value = "OBD as defined by the EPA";
                    case 0x03:
                        value = "OBD and OBD-II";
                    case 0x04:
                        value = "OBD-I";
                    case 0x05:
                        value = "Not meant to comply with any OBD standard";
                    case 0x06:
                        value = "EOBD (Europe)";
                    case 0x07:
                        value = "EOBD and OBD-II";
                    case 0x08:
                        value = "EOBD and OBD";
                    case 0x09:
                        value = "EOBD, OBD and OBD II";
                    case 0x0A:
                        value = "JOBD (Japan)";
                    case 0x0B:
                        value = "JOBD and OBD II";
                    case 0x0C:
                        value = "JOBD and EOBD";
                    case 0x0D:
                        value = "JOBD, EOBD, and OBD II";
                    default:
                        value = "unknown";
                    }
                    return value;
                }
            }
            return null;
        }

        private boolean updateAuxiliaryInput() {
            char mode = (char) 0x01;
            char PID = (char) 0x1E;
            if (pidAvailable[PID]) {
                boolean[] response = queryBit(mode, PID);
                if (response != null) {
                    try {
                        JSONObject result = new JSONObject();
                        result.put("Auxiliary input status", response[0]);
                        sendIntent(SensorNames.AUXILIARY_INPUT, result);
                        return true;
                    } catch (JSONException e) {
                        Log.e(TAG, "Error in updateAuxiliaryInput:" + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }

        private boolean updateRunTime() {
            char mode = (char) 0x01;
            char PID = (char) 0x1F;
            if (pidAvailable[PID]) {
                char[] response = queryByte(mode, PID);
                if (response != null) {
                    try {
                        JSONObject result = new JSONObject();
                        int value = (((int) response[0]) * 256) + response[1];
                        result.put("Run time since engine start (seconds)", value);
                        sendIntent(SensorNames.RUN_TIME, result);
                        return true;
                    } catch (JSONException e) {
                        Log.e(TAG, "Error in updateRunTime:" + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }

        // TODO these methods cover the PIDs until Mode 1 PID 0x1F
    }
}
