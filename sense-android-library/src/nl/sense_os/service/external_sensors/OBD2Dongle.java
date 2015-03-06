/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.external_sensors;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;

import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;

/**
 * Class that manages connection with an OBD2 dongle via BLuetooth.
 * 
 * @author Roelof van den Berg <roelof@sense-os.nl>
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
    private final Handler connectHandler = new Handler();
    private ConnectThread connectThread = null;
    private BluetoothSocket socket = null;

    // update thread variables
    private boolean updateActive;
    private Handler updateHandler = new Handler();
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
        //set the enabled flag to true
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
        //set the enabled flag to false
    	this.dongleEnabled = false;
        try {
            // assume android version 2.1 or higher to use this method
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
    protected int getUpdateInterval() {
        return this.updateInterval;
    }

    /**
     * 
     * @param interval
     *            in milliseconds
     */
    protected void setUpdateInterval(int interval) {
        this.updateInterval = interval;
    }

    /**
     * The Runnable object creating a connection with the BlueTooth OBD-II dongle
     * 
     * @author roelofvandenberg
     *
     */
    public class ConnectThread implements Runnable {
        /*
         * Connect to the default BluetTooth adapter
         */
        public ConnectThread() {
            // get the BlueTooth adapter
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

                    // check if there is a paired device with the name containing OBDII
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
                                    Log.v(TAG, "0");
                                	socket = device.createRfcommSocketToServiceRecord(serial_uuid);
                                    Log.v(TAG, "1");
                                    socket.connect();
                                    Log.v(TAG, "2");
                                    connectionActive = true;
                                    Log.v(TAG, "3");
                                } catch (Exception e1) {
                                    Log.e(TAG, "Error creating socket to " + device.getName()
                                            + ", attempting workaround. ");
                                    // if creating the socket did not work, try the workaround
                                    try {
                                        Method m = device.getClass().getMethod(
                                                "createRfcommSocket", new Class[] { int.class });
                                        socket = (BluetoothSocket) m.invoke(device, 1);
                                        Log.v(TAG, "4");
                                        socket.connect();
                                        Log.v(TAG, "5");
                                        connectionActive = true;
                                        Log.v(TAG, "Connected to socket via workaroud");
                                    } catch (Exception e2) {
                                        Log.e(TAG, "Error creating socket to " + device.getName()
                                                + " in workaround. ", e2);
                                    }
                                }
                                if (connectionActive) {
                                	Log.v(TAG, "attempting to start UpdateThread from ConnectThread");
                                	deviceType = device.getName();
                                    updateHandler.post(updateThread = new UpdateThread());
                                    foundDevice = true;
                                    break;
                                }
                            }
                        }
                    }
                    //if no device was found, try again in 10 seconds
                    if (!foundDevice) {
                        Log.v(TAG, "No Paired OBD-II Dongle found. Try again in 10 seconds");
                        connectHandler.postDelayed(connectThread = new ConnectThread(), 10000);
                    }
            	}
                // if the adapter is not enabled, wait for state to change (see @onReceive method)                
                else if (btAdapter.getState() == BluetoothAdapter.STATE_TURNING_ON) {
                    context.registerReceiver(btReceiver, new IntentFilter(
                            BluetoothAdapter.ACTION_STATE_CHANGED));
                } 
                // if the BlueTooth-adapter is not enabled, ask user for permission to start BlueTooth            	
            	else {
                    // Log.v(TAG, "Asking user to start BlueTooth");
                    Intent startBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startBt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(startBt);

                    // listen for the adapter state to change to STATE_ON
                    context.registerReceiver(btReceiver, new IntentFilter(
                            BluetoothAdapter.ACTION_STATE_CHANGED));
                }
            }
            // if the OBD2Dongle should not be sensed, stop this Thread
            else {
            	stop();
	        }
        }
        
        /**
         * listen to State-changes of BlueTooth Adapters using a BroadcastReceiver
         */
        private BroadcastReceiver btReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (!dongleEnabled) {
                    return;
                }

                String action = intent.getAction();

                //if the Adapter is turned on, try to connect again
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

        public void stop() {
            connectionActive = false;
            try {
                if (updateHandler != null)
                	updateActive = false;
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

    public class UpdateThread implements Runnable {
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
                    Log.e(TAG, "Error in UpdateThread constructor:" + e.getMessage());
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
            while(connectionActive){
	            try {
	                if (!updateActive) {
	                    // initialize the datastream by checking available PIDs and VIN
	                    sampleCommand();
	                	updateActive = initializeDataStream();
	                    updateHandler.post(updateThread = new UpdateThread());
	                }
	                // TODO not necessary to check connection alive now, is it?
	                else if (System.currentTimeMillis() > lastSampleTime + updateInterval) {
	                    // start sampling, so set the lastSampletime to NOW
	                    lastSampleTime = System.currentTimeMillis();
	                	// invoke data gathering subroutines
	                    pollAll();
	                }
	                // update a new upDateThread every second
	                updateHandler.postDelayed(updateThread = new UpdateThread(), 1000);
	            }
	            catch (Exception e) {
	                Log.e(TAG, "Error in update cycle while reading OBDII data: ", e);
	                // re-connect
	                connectHandler.postDelayed(connectThread = new ConnectThread(), 1000);
	            }
            }
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
        
        private OBD2Command execute(OBD2Command command){
    		command.run();
    		while (!updateActive) {
    			try {
					command.join(300);
				} catch (InterruptedException e) {
	                Log.e(TAG, "Error in execution of command "+command.getClass().toString()+": ", e);
	                break;
				}
    			if (!command.isAlive()) {
    				break;
    			}
    		}
    		return command;
        }
        
        private void sampleCommand(){
        	OBD2Command command = new OBD2CommandEngineRPM();
        	execute(command);
        	command.sendIntent();
        }
        
        /**
         * 
         * @return whether or not the initialization was successful
         */
        private boolean initializeDataStream() {
        	Log.v(TAG, "starting initializeDataStream");
        	while(connectionActive){
            	String result = queryString("ateo","").replace(" ","");
            	if (result != null && result.contains("OK")) {
            		Log.v(TAG, "no more echo on commands");
            		break;
            	}
            	try {
    				Thread.sleep(1500);
    			} catch (InterruptedException e) {
    				Log.e(TAG, "error while sleeping: ", e);
    				return false;
    			}
        	}
        	return true;

        	/*try {
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
            }*/
        }

        /**
         * 
         * @param mode
         *            indicating mode of operation as described in the latest OBD-II standard SAE
         *            J1979
         * @param PID
         *            coded standard OBD-II PID as defined by SAE J1979
         * @return the data bytes found in the OBD-II response formatted as a String
         */
        private String queryString(String mode, String PID) {
        	OBD2Command command = new OBD2Command(mode, PID);
       		execute(command);
       		return command.getData();
        }
                
        /*
         * polls most common dynamic PID's 
         */
        private void pollAll(){
            execute(new OBD2CommandEngineCoolant()).sendIntent();
            execute(new OBD2CommandEngineLoad()).sendIntent();
            execute(new OBD2CommandFuelPressure()).sendIntent();
            execute(new OBD2CommandIntakeAirTemperature()).sendIntent();
            execute(new OBD2CommandIntakeManifoldPressure()).sendIntent();
            execute(new OBD2CommandEngineRPM()).sendIntent();
            execute(new OBD2CommandVehicleSpeed()).sendIntent();
            execute(new OBD2CommandTimingAdvance()).sendIntent();
            execute(new OBD2CommandMAFAirFlowRate()).sendIntent();
            execute(new OBD2CommandThrottlePosition()).sendIntent();
            execute(new OBD2CommandRunTime()).sendIntent();
            // TODO these methods cover most of the byte-encoded, single PID commands until Mode 1 PID 0x1F
        }
        
        public class OBD2Command extends Thread{
        	protected String mode;
        	protected String PID;
        	protected InputStream in = null;
        	protected OutputStream out = null;
        	protected String data;
        	protected JSONObject json;
        	
        	/**
             * @param mode
             *            indicating mode of operation as described in the latest OBD-II standard SAE
             *            J1979
             * @param PID
             *            coded standard OBD-II PID as defined by SAE J1979
        	 * @param in
             *            the InputStream to read responses from the CAN-bus via OBD-II 
        	 * @param out
             *            the OutputStream to write requests to the CAN-bus via OBD-II
        	 */
        	public OBD2Command(String mode, String PID) {
        		this.mode = mode;
        		this.PID = PID;
        		this.in = sockInputStream;
        		this.out = sockOutputStream; 
        	}
        	
        	public void run(){
        		try{
        			sendRequest();
        			clearData();
        			receiveResponse();
        			generateJSON();
        		}
        		catch (IOException e){
        			//TODO catchy exception handling
        		}
        		catch (JSONException e){
        			//TODO catchy exception handling
        		}
        	}
        	
        	protected void sendRequest() throws IOException{
        		String cmd = mode + PID + "\r\n"; 
        		out.write(cmd.getBytes());
        		out.flush();
        		Log.v(TAG, "send a request for Mode: "+mode+", PID: "+PID);
        	}
        	
        	protected void receiveResponse() throws IOException{
        		byte currentbyte = 0;
        		while ((char)(currentbyte = (byte)in.read()) != '>') {
        			data += currentbyte;
        		}
        		String[] temp = data.split("\r");
        		if ("NODATA".equals(data)) {
        			data = null; return;
        		}
        		data = temp[0].replace(" ","");
        		Log.v(TAG, "received a response for Mode: "+data.substring(0, 2)+", PID: "+data.substring(2,4)+", respons: "+data.substring(4));
        		data = data.substring(4);
        	}
        	
        	protected void generateJSON() throws JSONException{
        		this.json = new JSONObject();
        	}
        	
        	protected String sensor_name;
        	public void sendIntent(){
                Intent i = new Intent(context.getString(R.string.action_sense_new_data));
                i.putExtra(DataPoint.SENSOR_NAME, sensor_name);
                i.putExtra(DataPoint.SENSOR_DESCRIPTION, deviceType);
                i.putExtra(DataPoint.VALUE, getJSON().toString());
                i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
                i.putExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime());
                i.setClass(context, nl.sense_os.service.MsgHandler.class);
                context.startService(i);
        	}
        	
        	protected void clearData(){
        		data = new String();
        	}
        	
        	/**
        	 * 
        	 * @return the String-formatted response from the OBD-II sent command 
        	 */
        	public String getData(){
        		return data;
        	}
        	
        	public JSONObject getJSON(){
                return this.json;
        	}
        	
        	/**
        	 * 
        	 * @return the Mode + PID of this Command
        	 */
        	public String getCommand(){
        		return mode + PID;
        	}
        }

        public class OBD2CommandStandards extends OBD2Command{
        	public OBD2CommandStandards(InputStream in, OutputStream out){
        		super("01", "1C");
        		sensor_name = SensorNames.OBD_STANDARDS;
        	}
        	
        	@Override
        	protected void generateJSON() throws JSONException{
        		super.generateJSON();
        		int a = Integer.parseInt(data.substring(0,2),16);
        		String value;
        		switch (a){
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
				json.put("OBD standards this vehicle conforms to", value);
        	}
        }
        
        public class OBD2CommandEngineRPM extends OBD2Command{
        	OBD2CommandEngineRPM(){
        		super("01", "0C");
        		sensor_name = SensorNames.ENGINE_RPM;
        	}
        	
        	@Override
        	protected void generateJSON() throws JSONException{
        		super.generateJSON();
        		double a = Integer.parseInt(data.substring(0,2),16);
        		double b = Integer.parseInt(data.substring(2,4),16);
                double value = ((a * 256d) + b) / 4d;
                json.put("Engine RPM (rpm)", value);        		
        	}
        }
        
        public class OBD2CommandRunTime extends OBD2Command{
        	public OBD2CommandRunTime() {
        		super("01", "1F");
        		sensor_name = SensorNames.RUN_TIME;
			}
        	
        	@Override
        	protected void generateJSON() throws JSONException{
        		super.generateJSON();
        		int a = Integer.parseInt(data.substring(0,2),16);
        		int b = Integer.parseInt(data.substring(2,4),16);
                int value = ( a * 256) + b;
                json.put("Run time since engine start (seconds)", value);
        	}
        }
        
        public class OBD2CommandThrottlePosition extends OBD2Command{
			public OBD2CommandThrottlePosition(){
				super("01","11");
				sensor_name = SensorNames.THROTTLE_POSITION;
			}
			
        	@Override
        	protected void generateJSON() throws JSONException{
        		super.generateJSON();
				double a = Integer.parseInt(data.substring(0,2),16);
				double value = a * 100d / 255d;
		        json.put("Throttle Position (%)", value);
			}
		}

        public class OBD2CommandEngineLoad extends OBD2Command{
			public OBD2CommandEngineLoad(){
				super("01","04");
				sensor_name = SensorNames.ENGINE_LOAD;
			}
			
        	@Override
        	protected void generateJSON() throws JSONException{
        		super.generateJSON();
				double a = Integer.parseInt(data.substring(0,2),16);
				double value = a * 100d / 255d;
		        json.put("Calculated engine load value (\u0025)", value);
			}
		}
        
        public class OBD2CommandMAFAirFlowRate extends OBD2Command{
			public OBD2CommandMAFAirFlowRate(){
				super("01","10");
				sensor_name = SensorNames.MAF_AIRFLOW;
			}
			
        	@Override
        	protected void generateJSON() throws JSONException{
        		super.generateJSON();
				double a = Integer.parseInt(data.substring(0,2),16);
				double b = Integer.parseInt(data.substring(2,4),16);
				double value = a * 256d + b / 100d;
		        json.put("MAF air flow rate (gram/sec)", value);
			}
		}
        
        public class OBD2CommandEngineCoolant extends OBD2Command{
			public OBD2CommandEngineCoolant(){
				super("01","05");
				sensor_name = SensorNames.ENGINE_COOLANT;
			}
			
        	@Override
        	protected void generateJSON() throws JSONException{
        		super.generateJSON();
				int value = Integer.parseInt(data.substring(0,2),16) - 40;
                json.put("temperature (\u00B0C)", value);
			}
		}
        
        public class OBD2CommandFuelPressure extends OBD2Command{
			public OBD2CommandFuelPressure(){
				super("01","0A");
				sensor_name = SensorNames.FUEL_PRESSURE;
			}
			
        	@Override
        	protected void generateJSON() throws JSONException{
        		super.generateJSON();
				int value = Integer.parseInt(data.substring(0,2),16) *3;
                json.put("Fuel pressure (kPa (gauge))", value);
			}
		}

        public class OBD2CommandIntakeManifoldPressure extends OBD2Command{
			public OBD2CommandIntakeManifoldPressure(){
				super("01","0B");
				sensor_name = SensorNames.INTAKE_PRESSURE;
			}
			
        	@Override
        	protected void generateJSON() throws JSONException{
        		super.generateJSON();
				int value = Integer.parseInt(data.substring(0,2),16);
                json.put("Fuel pressure (kPa (gauge))", value);
			}
		}
        
        public class OBD2CommandVehicleSpeed extends OBD2Command{
			public OBD2CommandVehicleSpeed(){
				super("01","0D");
				sensor_name = SensorNames.VEHICLE_SPEED;
			}
			
        	@Override
        	protected void generateJSON() throws JSONException{
        		super.generateJSON();
				int value = Integer.parseInt(data.substring(0,2),16);
                json.put("Vehicle speed (km/h)", value);
			}
		}
        
        public class OBD2CommandTimingAdvance extends OBD2Command{
			public OBD2CommandTimingAdvance(){
				super("01","0E");
				sensor_name = SensorNames.TIMING_ADVANCE;
			}
			
        	@Override
        	protected void generateJSON() throws JSONException{
        		super.generateJSON();
        		double a = Integer.parseInt(data.substring(0,2),16);
				double value = a / 2d - 64d;
                json.put("Timing advance (\u00B0 relative to #1 cylinder)", value);
			}
		}
        
        public class OBD2CommandIntakeAirTemperature extends OBD2Command{
			public OBD2CommandIntakeAirTemperature(){
				super("01","0F");
				sensor_name = SensorNames.INTAKE_TEMPERATURE;
			}
			
        	@Override
        	protected void generateJSON() throws JSONException{
        		super.generateJSON();
				int value = Integer.parseInt(data.substring(0,2),16) - 40;
                json.put("Intake air temperature (\u00B0C)", value);
			}
		}

		/**
		 * 
		 * @return whether or not the call was successful
		 */
        /*
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
		*/
		
		/**
		 * 
		 * @return whether or not the call was successful
		 */
        /*
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
		*/
		
        /*
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
		*/

		
        /*
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
		*/
		
		
        /*
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
		*/
    }
}