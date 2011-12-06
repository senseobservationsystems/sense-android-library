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

import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * @author roelofvandenberg
 *
 */
public class OBD2Sensor extends ExternalSensor {
	protected final String TAG = "OBD-II";
    private final String deviceType = "TestOBD";
	protected final String devicename = "OBDII";
	
	
	public OBD2Sensor(Context context){
		super(context);
	}
	
	public class UpdateThread extends Thread{
	}

	@Override
	public boolean isDevice(BluetoothDevice dev) {
		return dev.getName().contains(devicename);
	} 

    /**
     * Tries to set the echo off for the OBD-II connection
     * 
     * @return whether or not the initialization was successful
     */
	@Override
	protected boolean initializeSensor() {
    	Log.v(TAG, "starting initializeDataStream");
    	while(connected){
        	String result = request("ateo","").replace(" ","");
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
    }
    
    /**
     * polls most common dynamic PID's 
     */
	@Override
	protected void pollSensor() {
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

    /**
     * 
     * @param mode
     *            indicating mode of operation as described in the latest OBD-II standard SAE
     *            J1979
     * @param PID
     *            coded standard OBD-II PID as defined by SAE J1979
     * @return the data bytes found in the OBD-II response formatted as a String
     */
    private String request(String mode, String PID) {
    	OBD2Command command = new OBD2Command(mode, PID);
   		execute(command);
   		return command.getData();
    }
	
	private OBD2Command execute(OBD2Command command){
		command.run();
		while (connected && updateThread!=null){
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
	
	public class OBD2Command extends Thread{
    	protected final String mode;
    	protected final String PID;
    	protected final InputStream in;
    	protected final OutputStream out;
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
    		this.in = mmInStream;
    		this.out = mmOutStream; 
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
            i.putExtra(DataPoint.TIMESTAMP, System.currentTimeMillis());
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
}
