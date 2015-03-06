/**
 * 
 */
package nl.sense_os.service.external_sensors;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.SensorDataPoint;

import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Represents an OBD2 sensor. Connects to a dongle using bluetooth, and collects the sensor data
 * coming from the vehicle.
 * 
 * @author Roelof van den Berg <roelof@sense-os.nl>
 */
public class OBD2Sensor extends ExternalSensor {
	protected final String TAG = "OBD-II";
	
	public OBD2Sensor(Context context){
		super(context);
		devicename = "OBD";
	}
	
	@Override
	public boolean isDevice(BluetoothDevice dev) {
		Log.v(TAG, "isDevice: " + dev.getName() + " contains " + devicename + "? "+dev.getName().contains(devicename));
		return dev.getName().contains(devicename);
	} 

	//String[] mInitInitial = { "AT WS" };
	String[] mInitInitial = { "AT Z", "AT E0" };
	//String[] mInitResume = { "AT CAF 1", "AT H1" , "AT E0"/*, "04"*/};
	
	/*private final String mInitInitial[] = {
            "AT PP 2D SV 0F",                // baud rate to 33.3kbps
            "AT PP 2C SV 40",                // send in 29-bit address mode, receive both(0x60) just 29 (0x40)  
            "AT PP 2D ON",                   // activate baud rate PP. 
            "AT PP 2C ON",                   // activate addressing pp.
            "AT PP 2A OFF"                  // turn off the CAN ERROR checking flags used by wakeUp()
	};*/
	
	/*private final String mInitResume [] = {
            "AT WS",                         // reset chip so changes take effect
            "AT CAF1",                       // CAN auto-formatting on
            "AT SPB",                        // set protocol to B (user defined 1)
            "AT H1",                         // show headers
            "AT R0"                          // responses off - we don't expect responses to what we're sending.
	};*/
	
    /**
     * Tries to set the echo off for the OBD-II connection
     * 
     * @return whether or not the initialization was successful
     */
	@Override
	protected boolean initializeSensor() {
		//TODO set the deviceUUID to be the MAC-adress of the bluetooth dongle
		Log.v(TAG, "starting initializeDataStream");
    	if(connected){
    		OBD2Command response;
    		for (String s: mInitInitial){
    			response = new ATCommand(s);
    			response.execute();
    			Log.v(TAG, "AT Command "+s+" responded with: "+response.getData());
    			if(!response.hasValidResponse())
    				return false;
    		}
    		/*for (String s: mInitResume){
    			response = new OBD2Command(s);
    			response.execute();
    			Log.v(TAG, "AT Command "+s+" responded with: "+response.getData());
    			if(!response.hasValidResponse())
    				return false;
    		}*/
    		return true;
    	}
    	return false;
    }
    
    /**
     * polls most common dynamic PID's 
     */
	@Override
	protected void pollSensor() {
		if(connected){
			OBD2Command request = new OBD2CommandVehicleSpeed();
			request.execute();
			Log.v(TAG, "Vehicle Speed command, responds with: "+ request.getData());
			try {
				request.generateJSON();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			request = new OBD2CommandEngineCoolant();
			request.execute();
			Log.v(TAG, "Engine Coolant, responds with: "+ request.getData());
			/*
			Log.v(TAG, "OBD2CommandEngineLoad(), responds with: "+execute(new OBD2CommandEngineLoad()).getData());
			Log.v(TAG, "OBD2CommandEngineRPM(), responds with: "+execute(new OBD2CommandEngineRPM()).getData());
			Log.v(TAG, "OBD2CommandRunTime(), responds with: "+execute(new OBD2CommandRunTime()).getData());
			Log.v(TAG, "OBD2CommandThrottlePosition(), responds with: "+execute(new OBD2CommandThrottlePosition()).getData());*/
		}
		/*
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
        */
        // TODO these methods cover most of the byte-encoded, single PID commands until Mode 1 PID 0x1F		
	}

    private void sendDemoDataPoint(String description, Object value,
            String dataType) {
    	Log.v(TAG, "attempting to send data point :"+value+": "+description);
    	Log.v(TAG, "HALLO2 devicename: "+devicename+", deviceadress: "+deviceadress);
        Intent intent = new Intent(context.getString(R.string.action_sense_new_data));
        intent.putExtra(DataPoint.SENSOR_NAME, SensorNames.VEHICLE_SPEED);
        intent.putExtra(DataPoint.SENSOR_DESCRIPTION, devicename);
        intent.putExtra(DataPoint.DATA_TYPE, dataType);
        intent.putExtra(DataPoint.DEVICE_UUID, deviceadress);
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
        intent.putExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime());
        intent.setClass(context, nl.sense_os.service.MsgHandler.class);
        context.startService(intent);
    }
	
    long maximumexecutetime = 5000; // maximum time for this execution in milliseconds
    long executioninterval = 100; // time in between execution attempts in milliseconds
	
	public abstract class OBD2Command {
    	protected final String command;
    	protected final InputStream in;
    	protected final OutputStream out;
    	protected String data = new String();
    	protected JSONObject json;
    	protected String validresponse;
    	
    	/**
         * @param command
         *            indicating mode of operation as described in the latest OBD-II standard SAE
         *            J1979 and coded standard OBD-II PID as defined by SAE J1979 
    	 */
    	public OBD2Command(String command) {
    		this.command = command;
    		this.in = mmInStream;
    		this.out = mmOutStream;
    		validresponse = (char)(command.toCharArray()[0] + 4) + command.substring(1);
    	}
 	
    	public void execute(){
    		try{
				//determine the endtime of this execution
    			long endtime = System.currentTimeMillis() + maximumexecutetime;
    			
    			//send the command once
				sendRequest();
				
				while(connected && System.currentTimeMillis() < endtime){
					//read the response
					//TODO: if this does not work all the time, have a thorough look at readUpToPrompt from ELMBT.java in com.gtosoft.libvoyager.android;
					receiveResponse();					
					if(this.hasValidResponse()){
						//trim the data
						data.trim();
						return;
					}
					try{
						Thread.sleep(executioninterval);
					}
					catch(InterruptedException e){
						Log.e(TAG, "trouble sleeping while executing command ",e);
					}
				}
				if(!this.hasValidResponse()){
					Log.e(TAG, "command " + this.getCommand() + " did not get a valid response. Instead got: " +this.getData());
				}
				return;
    		}
    		catch (Exception e) {
				Log.e(TAG, e.getClass().getName() + " while executing command ("+this.getCommand()+")");
			}
    	}
    	
		protected void sendRequest() throws IOException{
    		String cmd = command + "\r\n"; 
    		out.write(cmd.getBytes());
    		out.flush();
    	}
    	
    	protected void receiveResponse() throws IOException, InterruptedException{
    		String response = "";
    		char currentchar = 0;
			
    		while (in.available()>0) {
				currentchar = (char)in.read();
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
			data += response;
    	}
    	
    	protected void generateJSON() throws JSONException{
    		this.json = new JSONObject();
    	}
    	
    	protected String sensor_name;
    	//TODO: fix what is commented out
    	public void sendIntent(){
    		
    		// useless because no one will subscribe to the OBD2Command class
    		// TODO: refactor this sensor
    		notifySubscribers();
    		SensorDataPoint dataPoint = new SensorDataPoint(getJSON());
    		dataPoint.sensorName = sensor_name;
    		dataPoint.sensorDescription = sensor_name;
    		dataPoint.timeStamp = SNTP.getInstance().getTime();        
    		sendToSubscribers(dataPoint);
    		
            Intent i = new Intent(context.getString(R.string.action_sense_new_data));
            i.putExtra(DataPoint.SENSOR_NAME, sensor_name);
            //i.putExtra(DataPoint.SENSOR_DESCRIPTION, deviceType);
            i.putExtra(DataPoint.VALUE, getJSON().toString());
            i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
            i.putExtra(DataPoint.TIMESTAMP, dataPoint.timeStamp);
            //TODO: i.putExtra(DataPoint.DEVICE_UUID, )
            i.setClass(context, nl.sense_os.service.MsgHandler.class);
            context.startService(i);
    	}
    	
    	protected boolean hasValidResponse(){
    		Log.v(TAG, "hasvalidresponse checks on: "+validresponse+", returns: "+data.contains(validresponse));
    		return data.contains(validresponse);
    	}
    	
    	/**
    	 * 
    	 * @return the String-formatted data from the OBD-II sent command 
    	 */
    	public String getData(){
    		return data;
    	}
    	
    	public JSONObject getJSON(){
            return this.json;
    	}
    	
    	public String getCommand(){
    		return command;
    	}
    }
	
	public class ATCommand extends OBD2Command{
		public ATCommand(String command) {
			super(command);
		}

		@Override
		protected boolean hasValidResponse() {
			return (data.contains("OK") || data.contains(command));  		
		}
	}
	

    public class OBD2CommandStandards extends OBD2Command{
    	public OBD2CommandStandards(InputStream in, OutputStream out){
    		super("01 1C");
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
    		super("01 0C");
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
    		super("01 1F");
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
			super("01 11");
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
			super("01 04");
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
			super("01 10");
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
			super("01 05");
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
			super("01 0A");
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
			super("01 0B");
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
			super("01 0D");
			sensor_name = SensorNames.VEHICLE_SPEED;
		}
		
    	@Override
    	protected void generateJSON() throws JSONException{
    		super.generateJSON();
    		int i = data.indexOf(validresponse);
			int value = Integer.parseInt(data.substring(i+6,i+8),16);
            sendDemoDataPoint("Vehicle speed (km/h)", value, SenseDataTypes.INT);
		}    	
	}
    
    public class OBD2CommandTimingAdvance extends OBD2Command{
		public OBD2CommandTimingAdvance(){
			super("01 0E");
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
			super("01 0F");
			sensor_name = SensorNames.INTAKE_TEMPERATURE;
		}
		
    	@Override
    	protected void generateJSON() throws JSONException{
    		super.generateJSON();
			int value = Integer.parseInt(data.substring(0,2),16) - 40;
            json.put("Intake air temperature (\u00B0C)", value);
		}
	}	
}
