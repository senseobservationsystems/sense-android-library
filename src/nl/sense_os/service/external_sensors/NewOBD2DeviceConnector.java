package nl.sense_os.service.external_sensors;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import nl.sense_os.service.R;
import nl.sense_os.service.commonsense.SensorRegistrator;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;

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

public class NewOBD2DeviceConnector implements Runnable{
	protected final String TAG = "OBD-II";
    protected final UUID serial_uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
	
    //remember the state you are in
	enum State {AWAITING_BLUETOOTH, BLUETOOTH_ENABLED, CONNECTION_READY, DEVICE_POWERED, READY, STOPPED}
	protected State previousState, currentState;
	protected boolean sensorsenabled = false;
	
	//handling threads
    protected Handler stateMachineHandler = null;
    protected HandlerThread stateMachineHandlerThread = null;
    protected StateMachine stateMachine = null;

    //global variables
	protected final Context context;
	protected final int interval;
	protected BluetoothDevice device;
	protected String databuffer = "";
		
	public NewOBD2DeviceConnector(Context context, BluetoothDevice device, int interval){
		this.context = context;
		this.device = device;
		if(interval < 0)
			this.interval = 1;
		else
			this.interval = interval;
		
	}

	public NewOBD2DeviceConnector(Context context, int interval){
		this(context, null, interval);
	}
	
	@Override
	public void run() {
		sensorsenabled = true;
		currentState = State.AWAITING_BLUETOOTH;
		
        if(stateMachineHandlerThread == null){
        	stateMachineHandlerThread = new HandlerThread(TAG);
        	stateMachineHandlerThread.start();
        }
        
        if(stateMachineHandler == null){
        	stateMachineHandler = new Handler(stateMachineHandlerThread.getLooper());        	
        }
        
        stateMachineHandler.post(stateMachine = new StateMachine());
	}

	public void stop() {
		sensorsenabled = false;
		Log.v(TAG, "stopping OBD2DeviceConnector");
		
		try {
            if (stateMachine != null) {
            	stateMachine.stop();
            }
            if(stateMachineHandler != null){
            	stateMachineHandler.removeCallbacks(stateMachine);            	
            	stateMachineHandler = null;
            }
            if(stateMachineHandlerThread != null){
            	stateMachineHandlerThread.getLooper().quit();
            	stateMachineHandlerThread = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in stopping BluetoothDeviceRegistrator:", e);
        }
	}
		
	public class StateMachine implements Runnable{
		protected long lastrun = System.currentTimeMillis();

		//connection variables
		protected BluetoothAdapter adapter;
		protected BTReceiver btReceiver;
		protected BluetoothSocket socket;
		protected InputStream input;
		protected OutputStream output;
		
		//Hayes Command set: turn automatic formatting ON, turn headers ON 
		final String[] hayescommands = {"AT CAF 1", "AT H 1"};
		
		//OBD variables
		private HashMap<String, String> verifiedsensors = new HashMap<String, String>();
		private EmptyRegistrator registrator = new EmptyRegistrator(context);
		
		//OBD timer variables 
		private final int timeout = 20000;
		private final int sleeptime = 500;
		
		@Override
		public void run() {
			lastrun = System.currentTimeMillis();
			while(sensorsenabled && currentState != State.STOPPED){
				Log.v(TAG, "Current State: "+currentState+" (past:"+previousState+")");
				previousState = currentState;
				switch (currentState){
					case AWAITING_BLUETOOTH:
						currentState = doCheckBluetooth();
						break;
					case BLUETOOTH_ENABLED:
						currentState = doConnectSocket();
						break;
					case CONNECTION_READY:
						//currentState = doWaitForBoot();
						currentState = State.DEVICE_POWERED;
						break;
					case DEVICE_POWERED:
						currentState = doInitializeUsingHayes();
						break;
					case READY:
						currentState = doPollSensors();
					case STOPPED:
						try {
							if(socket != null)
								socket.close();
						} catch (Exception e) {
							Log.e(TAG,"Error closing socket in stopped state");
						}
						socket = null;
						break;
					default:
						currentState = State.AWAITING_BLUETOOTH;
						break;
				}
				if(lastrun + interval < System.currentTimeMillis()){
					try {
						long sleepTime = interval - (System.currentTimeMillis()-lastrun);
						sleepTime = sleepTime<0?1:sleepTime;
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						Log.e(TAG, "Interrupted while sleeping: ", e);
						currentState = State.STOPPED;
					}
				}
			}
			Log.v(TAG, "Stopping StateMachine, while in state "+currentState);
			stop();
		}
		
		public void stop(){
			Log.v(TAG, "stopping the StateMachine");
			//removing the btListener
		    if(btReceiver != null)
		    	context.unregisterReceiver(btReceiver);
		    btReceiver = null;
		    currentState = State.STOPPED;
		}

		protected State doCheckBluetooth() {
			//try to locate the default bluetooth adapter, go to State STOPPED if not found
		    try { adapter = BluetoothAdapter.getDefaultAdapter(); } 
		    catch (Exception e) { return State.STOPPED;}
		    
		    if (adapter != null){
		        
		        // listen to adapter changes. btReceiver will handle state changes
				if(btReceiver == null)
					btReceiver = new BTReceiver();
		        context.registerReceiver(btReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
		        
		    	//look at the current state of the adapter 
		    	//if the adapter is turned on, set the state to BLUETOOTH_ENABLED
		    	if(adapter.getState() == BluetoothAdapter.STATE_ON){
		    		return State.BLUETOOTH_ENABLED;
		    	}
		    	else if(adapter.getState() != BluetoothAdapter.STATE_TURNING_ON){
					Log.v(TAG, "TURNING ON");
		    		//if the adapter is not being turned on, ask user permission to turn it on
		            Intent startBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		            startBt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		            context.startActivity(startBt);
				}
		   		return State.AWAITING_BLUETOOTH;
			}
		    //no good finding bluetooth, then stop the whole StateMachine
		    else
		    	return State.STOPPED;
		}

		protected State doConnectSocket() {
			//try to connect to an OBD2 Dongle
			socket = connectSocket();
			Log.v(TAG, "socket == null?"+ (socket == null)); 
			if(socket == null)
				return State.BLUETOOTH_ENABLED;
			//Log.v(TAG, "socket.isConnected(): "+ socket.isConnected());
			try{
				input = socket.getInputStream();
				output = socket.getOutputStream();
				return State.CONNECTION_READY;
			} catch (IOException e) {
				//Stop if no good socket was created
				Log.e(TAG, "Error in getting stream in doconnectsocket", e);
				return State.STOPPED;
			}
		}

		protected State doWaitForBoot() {
			if(sendCommand("AT WS", "ELM327"))
				return State.DEVICE_POWERED;
			return State.CONNECTION_READY;
		}

		protected State doInitializeUsingHayes() {
			//if the ELM unit is powered and connected, initialize using a set of Hayes commands
			for(String command: hayescommands){
				if(!sendHayesCommand(command))
					return State.DEVICE_POWERED;
			}
			return State.READY;
		}

		protected State doPollSensors() {
			try{
				//TODO select which sensors to poll depending on preferences
				//TODO implementing pollMonitorStatus (is bit-encoded)
				//pollMonitorStatus();
				pollEngineLoad();
				pollEngineCoolant();
				pollFuelPressure();
				pollIntakeManifoldPressure();
				pollEngineRPM();
				pollVehicleSpeed();
				pollIntakeAirTemperature();
				pollThrottlePosition();
				//TODO: 01 1F en verder volgens aangekruisde waardes
				/*pollRunTime();
				pollDistanceTraveledWithMIL();
				pollFuelLevelInput();
				pollBarometricPressure();
				pollRelativeThrottlePosition();
				pollAmbientAirTemperature();*/
				//TODO: 01 47 en verder volgens aangekruisde waardes
				return State.READY;
			}
			catch (Exception e){
				return State.AWAITING_BLUETOOTH;
			}
		}

		protected class BTReceiver extends BroadcastReceiver{
		    @Override
		    public void onReceive(Context context, Intent intent) {
		    	Log.v(TAG, "onReceive method, intent: "+intent.getAction());
		    	if(sensorsenabled){
		        	String action = intent.getAction();
		            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
		        	if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
		                if (currentState == State.AWAITING_BLUETOOTH){
		                    if (state == BluetoothAdapter.STATE_ON) {
		                    	currentState = State.BLUETOOTH_ENABLED;
		                    }
		                }
		                else{
			            	if (state != BluetoothAdapter.STATE_ON){
			            		currentState = State.AWAITING_BLUETOOTH;
			            	}
			            }
		            }		          
		    	}
		    	else{
		    		Log.e(TAG, "why is this method still running, stop method is not functioning properly");
		    	}
		    }
		}

		private BluetoothSocket connectSocket() {
			//try to create a connection to a bluetooth device
			BluetoothSocket tempsocket;
			if(device != null){				
				tempsocket = connectSocket(device);
				if(tempsocket != null){
					return tempsocket;
				}				
			}
		
			Set<BluetoothDevice> paireddevices = adapter.getBondedDevices();
			for(BluetoothDevice tempdev: paireddevices){
				if(tempdev.getName().contains("OBD")){
					tempsocket = connectSocket(tempdev);
					if(tempsocket != null){						
						return tempsocket;
					}
				}
			}
			Log.e(TAG, "no device available");
			return null;
		}
		
	    /**
	     * 
	     * @return whether or not a Socket connection was established
	     */
		private BluetoothSocket connectSocket(BluetoothDevice dev) {
	    	BluetoothSocket tempsocket;
	        try {
	        	tempsocket = dev.createRfcommSocketToServiceRecord(serial_uuid);
	        	tempsocket.connect();
	        	Log.d(TAG, "Connected to obd2 via normal method");
	            return tempsocket;
	        } catch (IOException e) {
	            try {
	            	if(e.toString().contains("Service"))
	            		return connectSocket(dev);
	            	else
	            	{
	            		//Method m = dev.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
	            		//tempsocket = (BluetoothSocket) m.invoke(dev, Integer.valueOf(1));
	            		//tempsocket.connect();
	            		//Log.v(TAG, "Connected to "+dev.getName()+" via reflection work aroud");
	            		//return tempsocket;
	            		return socket;
	            	}
	            }
	            // if all has failed, stop this sensor
	            catch (Exception ex) {
	                Log.d(TAG, "No socket connected to " + dev.getName(), ex);
	                return socket;
	            }
	        } catch (Exception ex) {
	            Log.e(TAG, "Failed to connect socket to " + dev.getName(), ex);
	            return socket;
	        }
	    }
		
		//The ‘>’ character indicates that the device is in the idle state, ready to receive characters.
		private void readUntilPrompt(){
    		try {
    			if(socket != null && input != null){
		    		char currentchar = 0;
		    		while (input != null) {
							currentchar = (char)input.read();
							if(currentchar == '>')
								break;
							//valid characters for the response
							if(currentchar >= 32 && currentchar <=  127){
								databuffer += currentchar;
							}
							//represent CR/LF characters as |
							else if (currentchar == 13 || currentchar == 10){
								databuffer += "|";
							}
						}
				}			
    		} catch (Exception e) {
				Log.e(TAG, "Exception in readUntilPrompt, buffer thusfar: "+databuffer);
			}
		}

    	private void clearBuffer() {
    		databuffer = "";
		}

		/**
    	 * 
    	 * @param command the Hayes command (AT command) to be sent
    	 * @return whether or not the execution of this command was successful
    	 */
		private boolean sendHayesCommand(String command) {
    		clearBuffer();
			return sendCommand(command, "OK");
    	}

		/**
    	 * 
    	 * @param command the Mode and PID for the OBD-command to be checked
    	 * @return the formatted response gotten, or null iff the command was invalid
    	 */
		private String[] sendOBDCommand(String command){
			clearBuffer();
			if(command != null && command.length()>=2){
    			String validresponse = (char)(command.toCharArray()[0] + 4) + command.substring(1);
    			sendCommand(command, validresponse);
    			String[] responses = databuffer.split("|");
    			for(String response: responses){
    				if(response.contains(validresponse))
    					return response.split(" ");
    			}
    		}
    		return null;
    	}
    	
    	private boolean sendCommand(String command, String validresponse){
			long deadline = System.currentTimeMillis() + timeout;
			
			boolean commandsent = false;
			//first try and send the AT command
			while(System.currentTimeMillis() < deadline){
				commandsent = trySend(command);
				if(commandsent)
					break;
				try {
					Thread.sleep(sleeptime);
				} catch (InterruptedException e) {
					Log.e(TAG, "InterruptedException while sleeping in SendATcommand");
				}
			}
		
			//try to find the reply 'OK' which indicates a correct reading of the Hayes command
			while(System.currentTimeMillis() < deadline){
				readUntilPrompt();
				if(databuffer.contains(validresponse))
					return true;
				try {
					Thread.sleep(sleeptime);
				} catch (InterruptedException e) {
					Log.e(TAG, "InterruptedException while sleeping in SendATcommand");
				}
			}
			return false;
		}

		//messages to the ELM327 must be terminated with a CR character (#0D) before it will be acted upon
    	private boolean trySend(String data){
			try {
				if(socket != null){
					byte bytestosend[] = (data + "\r").getBytes();
					output.write(bytestosend);
					return true;
				}				
			} catch (IOException e) {
				Log.e(TAG, "IOException sending ("+data+")");				
			}
			return false;
		}

		private void pollMonitorStatus(){
			String[] hexbytes = sendOBDCommand("01 01");
			Integer.parseInt(hexbytes[2], 2);
			//TODO: handle this bit-encoded PID
		}
		
		private void pollEngineLoad() {
			String[] hexbytes = sendOBDCommand("01 04");
			if(hexbytes.length==3){
				float a = Integer.parseInt(hexbytes[2],16);
				float value = a * 100f / 255f;
				SendDataPoint(SensorNames.ENGINE_LOAD, null, value, SenseDataTypes.FLOAT);
			}
		}

		private void pollEngineCoolant() {
			String[] hexbytes = sendOBDCommand("01 05");
			if(hexbytes.length==3){
				int a = Integer.parseInt(hexbytes[2],16);
				int value = a - 40;
				SendDataPoint(SensorNames.ENGINE_COOLANT, null, value, SenseDataTypes.INT);
			}
		}	

		private void pollFuelPressure() {
			String[] hexbytes = sendOBDCommand("01 0A");
			if(hexbytes.length==3){
				int a = Integer.parseInt(hexbytes[2],16);
				int value = a * 3;
				SendDataPoint(SensorNames.FUEL_PRESSURE, null, value, SenseDataTypes.INT);
			}
		}

		private void pollIntakeManifoldPressure() {
			String[] hexbytes = sendOBDCommand("01 0B");
			if(hexbytes.length==3){
				int a = Integer.parseInt(hexbytes[2],16);
				SendDataPoint(SensorNames.INTAKE_PRESSURE, null, a, SenseDataTypes.INT);
			}
		}

		private void pollEngineRPM() {
			String[] hexbytes = sendOBDCommand("01 0C");
			if(hexbytes.length==4){
	    		float a = Integer.parseInt(hexbytes[2],16);
	    		float b = Integer.parseInt(hexbytes[3],16);
	            float value = ((a * 256f) + b) / 4f;
				SendDataPoint(SensorNames.ENGINE_RPM, null, value, SenseDataTypes.FLOAT);
			}
		}

		private void pollVehicleSpeed(){
			String[] hexbytes = sendOBDCommand("01 0D");
			if(hexbytes.length==3){
				int value = Integer.parseInt(hexbytes[2],16);
				SendDataPoint(SensorNames.VEHICLE_SPEED, null, value, SenseDataTypes.INT);
			}
		}
		
		private void pollIntakeAirTemperature(){
			String[] hexbytes = sendOBDCommand("01 0F");
			if(hexbytes.length==3){
				int a = Integer.parseInt(hexbytes[2],16);
				int value = a-40;
				SendDataPoint(SensorNames.INTAKE_TEMPERATURE, null, value, SenseDataTypes.INT);
			}
		}
		
		private void pollThrottlePosition(){
			String[] hexbytes = sendOBDCommand("01 11");
			if(hexbytes.length==3){
				float a = Integer.parseInt(hexbytes[2],16);
				float value = (a*100)/255;
				SendDataPoint(SensorNames.THROTTLE_POSITION, null, value, SenseDataTypes.FLOAT);
			}
		}
		
		private void SendDataPoint(String sensorName, String sensorDescription, Object value, String dataType) {
			//if necessary, register the sensor
			if(!verifiedsensors.get(sensorName).equals(sensorDescription)){
				if(registrator.checkSensor(sensorName, sensorDescription, dataType, device.getName() +" ("+device.getAddress()+")", ""+ value, device.getName(), device.getAddress())){
					verifiedsensors.put(sensorName, sensorDescription);
				}
			}
			
			//build the intent and send it to commonsense
	        Intent intent = new Intent(context.getString(R.string.action_sense_new_data));
	        intent.putExtra(DataPoint.SENSOR_NAME, sensorName);
	        intent.putExtra(DataPoint.SENSOR_DESCRIPTION, sensorDescription);
	        intent.putExtra(DataPoint.DATA_TYPE, dataType);
	        intent.putExtra(DataPoint.DEVICE_UUID, device.getAddress());
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
		
		private class EmptyRegistrator extends SensorRegistrator{
			public EmptyRegistrator(Context context) {
				super(context);
			}

			@Override
			public boolean verifySensorIds(String deviceType, String deviceUuid) {
				Log.e(TAG, "DO NOT USE verifySensorIds, USE checkSensor methods seperately, because of dynamic implementation");
				return false;
			}
		}		
	}
}
