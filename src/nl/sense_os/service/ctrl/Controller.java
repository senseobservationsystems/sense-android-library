package nl.sense_os.service.ctrl;

import nl.sense_os.service.location.LocationSensor;
import android.content.Context;
import android.location.Location;

public class Controller
{
	
	protected Controller()
	{

	}

	public static synchronized Controller getController(Context context)
	{
		if (ref == null) {
			ref = new CtrlDefault(context);
			//ref = new CtrlExtended(context);
			locListener = locListener.getInstance(context);			
		}
		return ref;
	}

	public Object clone()
			throws CloneNotSupportedException
	{
		throw new CloneNotSupportedException(); 
	}

	private static Controller ref;
	
	public static LocationSensor locListener; 
    
	
	/*********************************************************************************************************************/
	/*																													 *
	 * 								    Location Sensor Controlling Functions										     *
	 *																													 *
	 *********************************************************************************************************************/
	
	public void checkSensorSettings(boolean isGpsAllowed, boolean isListeningNw, boolean isListeningGps, long time, Location lastGpsFix, 
											long listenGpsStart, Location lastNwFix, long listenNwStart, long listenGpsStop, long listenNwStop) {
	}
	
	
	/*********************************************************************************************************************/
	/*																													 *
	 * 								    Light Sensor Controlling Functions										         *
	 *																													 *
	 *********************************************************************************************************************/
	
	public void checkLightSensor(float value) {
	}
	
	/*********************************************************************************************************************/
	/*																													 *
	 * 								    Noise Sensor Controlling Functions										         *
	 *																													 *
	 *********************************************************************************************************************/
	
	public void checkNoiseSensor(double dB) {
	}

	/*********************************************************************************************************************/
	/*																													 *
	 * 								    Data Transmitter Controlling Functions										     *
	 *																													 *
	 *********************************************************************************************************************/

	public void send_data(long interval) {
	}
	
	public void scheduleTransmissions(){
	}
	
}
