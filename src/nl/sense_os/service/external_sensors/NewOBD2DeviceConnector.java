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
	
	enum State {AWAITING_BLUETOOTH, BLUETOOTH_ENABLED, CONNECTION_READY, DEVICE_POWERED, READY, ERROR}
	protected State previousState, currentState;
	protected boolean sensorsenabled = false;
	
    protected Handler stateMachineHandler = null;
    protected HandlerThread stateMachineHandlerThread = null;
    protected StateMachine stateMachine = null;

	protected final Context context;
	protected BluetoothDevice device;
	protected final int interval;
	protected long lastrun = System.currentTimeMillis();
	protected BluetoothAdapter adapter;
	protected BluetoothSocket socket;
	
	public NewOBD2DeviceConnector(Context context, BluetoothDevice device, int interval){
		this.context = context;
		this.device = device;
		this.interval = interval;
	}

	public NewOBD2DeviceConnector(Context context, int interval){
		this.context = context;
		this.device = null;
		this.interval = interval;
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
	
	
	InputStream input;
	OutputStream output;
	String databuffer = "";
	
	public class StateMachine implements Runnable{
		BTReceiver btReceiver;
		
		@Override
		public void run() {
			lastrun = System.currentTimeMillis();
			if(sensorsenabled){
				Log.v(TAG, "Current State: "+currentState+" (past:"+previousState+")");
				previousState = currentState;
				switch (currentState){
					case AWAITING_BLUETOOTH:
						//try to locate the default bluetooth adapter, exit in error if this is impossible
			            try { adapter = BluetoothAdapter.getDefaultAdapter(); } 
			            catch (Exception e) { currentState = State.ERROR; break;}
			            
			            if (adapter != null){
				            
			                // listen to adapter changes. btReceiver will handle state changes
							if(btReceiver == null)
								btReceiver = new BTReceiver();
				            context.registerReceiver(btReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
				            
			            	//look at the current state of the adapter 
			            	//if the adapter is turned on, set the state to BLUETOOTH_ENABLED
			            	if(adapter.getState() == BluetoothAdapter.STATE_ON){
			            		currentState = State.BLUETOOTH_ENABLED;
			            		break;
			            	}
			            	else if(adapter.getState() != BluetoothAdapter.STATE_TURNING_ON){
								Log.v(TAG, "TURNING ON");
			            		//if the adapter is not being turned on, ask user permission to turn it on
				                Intent startBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				                startBt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				                context.startActivity(startBt);						
							}
						}
			            else
			            	currentState = State.ERROR;
						break;
					case BLUETOOTH_ENABLED:
						//try to connect to an OBD2 Dongle
						socket = connectSocket();
						Log.v(TAG, "socket == null?"+ (socket == null)); 
						if(socket == null)
							break;
						//Log.v(TAG, "socket.isConnected(): "+ socket.isConnected());
						try{
							input = socket.getInputStream();
							output = socket.getOutputStream();
							currentState = State.CONNECTION_READY;
							break;
						} catch (IOException e) {
							currentState = State.ERROR;
						}
						break;
					case ERROR:
						break;
					default:
						if(socket != null ){
							if(currentState == State.CONNECTION_READY){
							//wait until the device is ready to receive HAYES-commands
							databuffer += readUntilPrompt();
							if(databuffer.contains("ELM327"))
								currentState = State.DEVICE_POWERED;
							}
							if(currentState == State.DEVICE_POWERED){
								if(initializeUsingHayes())
									currentState = State.READY;
								else
									currentState = State.ERROR;
							}
							if(currentState == State.READY){
								pollSensors();
							}
						}
						else{
							//return to the BLUETOOTH_ENABLED state, to create a new socket
							currentState = State.BLUETOOTH_ENABLED;
						}
						break;
				}
			}
			Log.v(TAG, "ENDING AT currentState: "+currentState);
			if(currentState != State.ERROR && sensorsenabled)
				runStateMachine();
			else
				stop();
		}
		
		protected void runStateMachine(){
			try
			{
			long timepast = System.currentTimeMillis() - lastrun;
			if(currentState != previousState || timepast > interval){
				Log.v(TAG, "RUN StateMachine NOW");
				if(stateMachineHandler != null)
					stateMachineHandler.post(stateMachine = new StateMachine());
			}
			else{
				Log.v(TAG, "RUN StateMachine in "+(interval -timepast) +"milliseconds");
				if(stateMachineHandler != null)
					stateMachineHandler.postDelayed(stateMachine = new StateMachine(), interval - timepast);
			}
			}catch(Exception e)
			{
				Log.d(TAG, "Error in runstatemachine",e);
			}
		}

		public void stop(){
			Log.v(TAG, "stopping the StateMachine");
			//removing the btListener
            if(btReceiver != null)
            	context.unregisterReceiver(btReceiver);
            btReceiver = null;
            currentState = State.AWAITING_BLUETOOTH;
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
		                    	runStateMachine();
		                    }
		                }
		            }
		            else{
		            	if (state != BluetoothAdapter.STATE_ON){
		            		currentState = State.AWAITING_BLUETOOTH;
		            		runStateMachine();
		            	}
		            }
		    	}
		    	else{
		    		Log.e(TAG, "why is this method still running, stop method is not functioning properly");
		    	}
		    }
		}

		protected BluetoothSocket connectSocket() {
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
	    protected BluetoothSocket connectSocket(BluetoothDevice dev) {
	    	BluetoothSocket tempsocket;
	        try {
	        	tempsocket = dev.createRfcommSocketToServiceRecord(serial_uuid);
	        	tempsocket.connect();
	            Log.v(TAG, "Connected to "+ dev.getAddress() +" via normal method");
	            return tempsocket;
	        } catch (IOException e) {
	            try {
	            	if(e.toString().contains("Service"))
	            		return connectSocket(dev);
	            	else
	            	{
	            		Method m = dev.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
	            		tempsocket = (BluetoothSocket) m.invoke(dev, Integer.valueOf(1));
	            		tempsocket.connect();
	            		Log.v(TAG, "Connected to "+dev.getName()+" via reflection work aroud");
	            		return tempsocket;
	            	}
	            }
	            // if all has failed, stop this sensor
	            catch (Exception ex) {
	                Log.d(TAG, "No socket connected to " + dev.getName(), ex);
	                return null;
	            }
	        } catch (Exception ex) {
	            Log.e(TAG, "Failed to connect socket to " + dev.getName(), ex);
	            return null;
	        }
	    }
		
		//The ‘>’ character indicates that the device is in the idle state, ready to receive characters.
		protected String readUntilPrompt(){
			String response = "";
    		try {			
			while(socket != null && input != null){
	    		char currentchar = 0;
				
	    		
					while (input != null && input.available()>0) {
						currentchar = (char)input.read();
						if(currentchar == '>')
							break;
						//valid characters for the response
						if(currentchar >= 32 && currentchar <=  127){
							response += currentchar;
						}
						//represent CR/LF characters as |
						else if (currentchar == 13 || currentchar == 10){
							response += "|";
						}
					}
				
				}			
    		} catch (Exception e) {
				Log.e(TAG, "Exception in readUntilPrompt, response thusfar: "+response);
				
			}
    		return response;
		}
		
		//perform a warm start, turn automatic formatting ON, turn headers ON 
		String[] hayescommands = {"AT WS", "AT CAF 1", "AT H 1"}; 
		
		protected boolean initializeUsingHayes() {
			//wait until the ELM unit is powered and connected
			for(String command: hayescommands){
				if(!sendHayesCommand(command))
					return false;
			}
			return true;
		}
		
		// an internal timer will automatically abort incomplete messages after about 20 seconds
		protected final int timeout = 20000;
		protected final int sleeptime = 500;

		/**
    	 * 
    	 * @param command the Hayes command (AT command) to be sent
    	 * @return whether or not the execution of this command was successful
    	 */
    	protected boolean sendHayesCommand(String command) {
    		return sendCommand(command, "OK") != null;
    	}
    	
    	/**
    	 * 
    	 * @param command the Mode and PID for the OBD-command to be checked
    	 * @return the formatted response gotten, or null iff the command was invalid
    	 */
    	protected String[] sendOBDCommand(String command){
    		if(command != null && command.length()>=2){
    			String validresponse = (char)(command.toCharArray()[0] + 4) + command.substring(1);
    			String[] responses = sendCommand(command, validresponse).split("|");
    			for(String response: responses){
    				if(response.contains(validresponse))
    					return response.split(" ");
    			}
    		}
    		return null;
    	}
    	
    	private String sendCommand(String command, String validresponse){
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
		
			String datareceived = "";
			//try to find the reply 'OK' which indicates a correct reading of the Hayes command
			while(System.currentTimeMillis() < deadline){
				datareceived += readUntilPrompt();
				if(datareceived.contains(validresponse))
					return datareceived;
			}
			return null;		
		}

		//messages to the ELM327 must be terminated with a CR character (#0D) before it will be acted upon
		protected boolean trySend(String data){
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

		protected void pollSensors() {
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
		
		protected HashMap<String, String> verifiedsensors = new HashMap<String, String>();
		protected EmptyRegistrator registrator = new EmptyRegistrator(context);
		
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
