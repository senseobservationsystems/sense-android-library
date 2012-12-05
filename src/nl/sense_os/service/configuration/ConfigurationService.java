package nl.sense_os.service.configuration;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main;
import nl.sense_os.service.constants.SensePrefs.Main.Advanced;
import nl.sense_os.service.constants.SensePrefs.Main.Ambience;
import nl.sense_os.service.constants.SensePrefs.Main.Location;
import nl.sense_os.service.constants.SensePrefs.Main.Motion;
import nl.sense_os.service.constants.SensePrefs.Main.PhoneState;
import nl.sense_os.service.constants.SensePrefs.Main.DevProx;
import nl.sense_os.service.constants.SensePrefs.Status;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class ConfigurationService extends IntentService {
	
	private Integer samplingRate = null;
	private Integer syncRate = null;
	SharedPreferences statusPrefs;
    SharedPreferences mainPrefs;

	public ConfigurationService() {
	super("ConfigurationService");	
	}	
		
	@Override
	/**
	 * When the service is started, it will look into the requirement and 
	 * set the preference accordingly
	 * Example of the requirement:
	 *    {
	 *      "position":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000,
	 *        "automatic_gps": true
	 *      },
	 *      "traveled distance 1h":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000
	 *      },
	 *      "Location":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000
	 *      },
	 *      "service state":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000
	 *      },
	 *      "battery sensor":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000
	 *      },
	 *      "data connection":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000
	 *      },
	 *      "connection type":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000
	 *      },
	 *      "call state":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000
	 *      },
	 *      "unread msg":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000
	 *      },
	 *      "signal strength":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000
	 *      },
	 *      "proximity":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000
	 *      },
	 *      "screen activity":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000
	 *      },
	 *      "ip address":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000
	 *      },
	 *      "bluetooth_discovery":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000
	 *      },
	 *      "bluetooth neighbours count":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000
	 *      },
	 *      "wifi scan":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000
	 *      },
	 *      "audio_spectrum":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000
	 *      },
	 *      "light":{
	 *        "sampling_rate":60000,
	 *        "sync_rate":60000
	 *      },
	 *      "camera_light":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000
	 *      },
	 *      "noise_sensor":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000
	 *      },
	 *      "loudness":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000
	 *      },
	 *      "pressure":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000
	 *      },
	 *      "orientation":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000
	 *      },
	 *      "linear acceleration":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000
	 *      },
	 *      "accelerometer":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000
	 *      },
	 *      "motion_energy":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000
	 *      },
	 *      "attachToMyrianode":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000
	 *      },
	 *      "Availability":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000
	 *      },
	 *      "Reachability":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000
	 *      },
	 *      "Activity":{
	 *        "sampling_rate":1800000,
	 *        "sync_rate":1800000
	 *      },
	 *      "ambient_temperature":{
	 *        "sampling_rate":30000,
	 *        "sync_rate":60000
	 *      }
	 *    }
	 * @param intent Intent with requirements property in JSON Object format
	 */
	protected void onHandleIntent(Intent intent) {
	
	statusPrefs = getSharedPreferences(SensePrefs.STATUS_PREFS, Context.MODE_PRIVATE);
    mainPrefs = getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
	
    try {
		String reqStr = intent.getStringExtra("requirements");
		JSONObject requirements = new JSONObject(reqStr).getJSONObject("requirements");
		
		if (requirements.length() == 0)
			return;
		
		samplingRate = null;
		syncRate = null;
		
		// process position		
		if (requirements.has("traveled distance 1h"))
			handleTraveledDistance1HReq(requirements.getJSONObject("traveled distance 1h"));		
		if (requirements.has("Location")) 
			handleLocationReq(requirements.getJSONObject("Location"));
		if (requirements.has("position")) 
			handlePositionReq(requirements.getJSONObject("position"));
		if (requirements.has("Availability"))
			handleAvailabilityReq(requirements.getJSONObject("Availability"));
		
		// Phone state sensing
		if (requirements.has("service state"))
			handleServiceStateReq(requirements.getJSONObject("service state"));
		if (requirements.has("battery sensor"))
			handleBatterySensorReq(requirements.getJSONObject("battery sensor"));
		if (requirements.has("data connection"))
			handleDataConnectionReq(requirements.getJSONObject("data connection"));
		if (requirements.has("connection type"))
			handleConnectionTypeReq(requirements.getJSONObject("connection type"));
		if (requirements.has("call state"))
			handleCallStateReq(requirements.getJSONObject("call state"));
		if (requirements.has("unread msg"))
			handleUnreadMsgReq(requirements.getJSONObject("unread msg"));
		if (requirements.has("signal strength"))
			handleSignalStrengthReq(requirements.getJSONObject("signal strength"));
		if (requirements.has("proximity"))
			handleProximityReq(requirements.getJSONObject("proximity"));
		if (requirements.has("screen activity"))
			handleScreenActiviyReq(requirements.getJSONObject("screen activity"));
		if (requirements.has("ip address"))
			handleIPAddressReq(requirements.getJSONObject("ip address"));		
		if (requirements.has("Reachability"))
			handleReachabilityReq(requirements.getJSONObject("Reachability"));		
		
		// device proximity sensing
		if (requirements.has("bluetooth_discovery"))
			handleBluetoothDiscoveryReq(requirements.getJSONObject("bluetooth_discovery"));
		if (requirements.has("bluetooth neighbours count"))
			handleBluetoothNeighboursCountReq(requirements.getJSONObject("bluetooth neighbours count"));
		if (requirements.has("wifi scan"))
			handleWifiScanReq(requirements.getJSONObject("wifi scan"));
		
		//Ambience sensing
		if (requirements.has("audio_spectrum"))
			handleAudioSpectrumReq(requirements.getJSONObject("audio_spectrum"));
		if (requirements.has("light"))
			handleLightReq(requirements.getJSONObject("light"));
		if (requirements.has("camera_light"))
			handleCameraLightReq(requirements.getJSONObject("camera_light"));
		if (requirements.has("noise_sensor"))
			handleNoiseSensorReq(requirements.getJSONObject("noise_sensor"));
		if (requirements.has("loudness"))
			handleLoudnessReq(requirements.getJSONObject("loudness"));
		if (requirements.has("pressure"))
			handlePressureReq(requirements.getJSONObject("pressure"));
		if (requirements.has("magnetic field"))
			handleMagneticFieldReq(requirements.getJSONObject("magnetic field"));
		
		//Motion Sensing
		if (requirements.has("orientation"))
			handleOrientationReq(requirements.getJSONObject("orientation"));
		if (requirements.has("linear acceleration"))
			handleLinearAccelerationReq(requirements.getJSONObject("linear acceleration"));
		if (requirements.has("accelerometer"))
			handleAccelerometerReq(requirements.getJSONObject("accelerometer"));
		if (requirements.has("motion_energy"))
			handleMotionEnergyReq(requirements.getJSONObject("motion_energy"));
		if (requirements.has("Activity"))
			handleActivityReq(requirements.getJSONObject("Activity"));
		
		// Location Feedback (myrianode)
		if (requirements.has("attachToMyrianode"))
			handleAttachToMyrianodeReq(requirements.getJSONObject("attachToMyrianode"));
		
		// update sampling rate if necessary
		if (samplingRate != null) {
			Integer preference_rate = null;
			
			if (samplingRate < 10) // realtime (< 10 sec)
				preference_rate = -2;
			else if (samplingRate >= 10 && samplingRate < 60) // often (10-60) sec
				preference_rate = -1;
			else if (samplingRate >= 60 && samplingRate < 900) // normal (1-15 min)
				preference_rate = 0;
			else if (samplingRate >= 900) // rarely (> 15 min)
				preference_rate = 1;
			
			if (preference_rate != null){
				String oldValue = mainPrefs.getString(Main.SAMPLE_RATE, "0");
				
				//only update if its have higher rate 
				if (preference_rate < Integer.parseInt(oldValue)) {
					mainPrefs.edit().putString(Main.SAMPLE_RATE, preference_rate.toString()).commit();
				}
			}
		}
		
		// update common sense sync rate if necessary
		if (syncRate != null) {
			Integer preference_rate = null;
			
			if (syncRate < 60) // realtime (< 1 min)
				preference_rate = -2;
			else if (syncRate >= 60 && syncRate < 300) // often (1)
				preference_rate = -1;
			else if (syncRate >= 300 && syncRate < 900) // normal (5-29 min)
				preference_rate = 0;
			else if (syncRate >= 900) // Eco-mode (30 min)
				preference_rate = 1;
			
			if (preference_rate != null){
				String oldValue = mainPrefs.getString(Main.SYNC_RATE, "0");
				
				//only update if its have higher rate 
				if (preference_rate < Integer.parseInt(oldValue)) {
					mainPrefs.edit().putString(Main.SYNC_RATE, preference_rate.toString()).commit();
				}
			}
		}
		
		//apply change by starting the service
		startService(new Intent(getString(R.string.action_sense_service)));
		
		
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}
	
	/**
	 * Update sampling rate
	 * @param newRate new sampling rate
	 */
	private void updateSamplingRate(int newRate){
	if ((this.samplingRate == null) || (newRate > 0 && newRate < this.samplingRate)) {
		this.samplingRate = newRate;
	}
	}
	
	/**
	 * Update sync rate to common sense
	 * @param newRate new sync rate
	 */
	private void updateSyncRate(int newRate){
	if ((this.syncRate == null) || (newRate > 0 && newRate < this.syncRate)) {
		this.syncRate = newRate;
	}
	}	
	
	/**
	 * Handle travel distance 1h requirement.
	 * - update sampling_rate if necessary
	 * - enable GPS provider
	 * - enable Network Provider
	 * - enable Automatic GPS
	 * - turn on Location sensing
	 * @param req travel distance 1h sensor requirement
	 */
	private void handleTraveledDistance1HReq(JSONObject req) {	
	handlePositionReq(req);
	}
	
	/**
	 * Handle 1h requirement.
	 * - update sampling_rate if necessary
	 * - enable GPS provider
	 * - enable Network Provider
	 * - enable Automatic GPS
	 * - turn on Location sensing
	 * @param req Location sensor requirement
	 */
	private void handleLocationReq(JSONObject req) {	
	handlePositionReq(req);
	}
	
	/**
	 * Handle position requirement.
	 * - update sampling_rate if necessary
	 * - enable GPS provider
	 * - enable Network Provider
	 * - enable Automatic GPS
	 * - turn on Location sensing
	 * @param req Position sensor requirement
	 * TODO: select GPS / Network base on accuracy criteria ?
	 */
	private void handlePositionReq(JSONObject req) {	
	try {
		if (req.has("sampling_rate"))
			updateSamplingRate(req.getInt("sampling_rate"));
		if (req.has("sync_rate"))
			updateSyncRate(req.getInt("sync_rate"));
		
		mainPrefs.edit()
			.putBoolean(Location.GPS, true)
			.putBoolean(Location.NETWORK, true)
			.putBoolean(Location.AUTO_GPS, true)
			.commit();
		
		statusPrefs.edit().putBoolean(Status.LOCATION, true).commit();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}
	
	/**
	 * Handle Availability requirement.
	 * - update sampling_rate if necessary
	 * - enable GPS provider
	 * - enable Network Provider
	 * - enable Automatic GPS
	 * - turn on Location sensing
	 * @param req Availability sensor requirement
	 */
	private void handleAvailabilityReq(JSONObject req) {	
	handleLocationReq(req);
	}
	
	/**
	 * Handle Service State requirement.
	 * - update sampling_rate if necessary
	 * - enable service state sensing
	 * - turn on Phone State sensing
	 * @param req ServiceState sensor requirement
	 */
	private void handleServiceStateReq(JSONObject req) {	
	try {
		if (req.has("sampling_rate"))
			updateSamplingRate(req.getInt("sampling_rate"));
		if (req.has("sync_rate"))
			updateSyncRate(req.getInt("sync_rate"));
		
		mainPrefs.edit().putBoolean(PhoneState.SERVICE_STATE, true).commit();						
		statusPrefs.edit().putBoolean(Status.PHONESTATE, true).commit();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}

	/**
	 * Handle Battery Sensor requirement.
	 * - update sampling_rate if necessary
	 * - enable Battery State sensing
	 * - turn on Phone State sensing
	 * @param req Battery sensor requirement
	 */
	private void handleBatterySensorReq(JSONObject req) {	
	try {
		if (req.has("sampling_rate"))
			updateSamplingRate(req.getInt("sampling_rate"));
		if (req.has("sync_rate"))
			updateSyncRate(req.getInt("sync_rate"));
		
		mainPrefs.edit().putBoolean(PhoneState.BATTERY, true).commit();						
		statusPrefs.edit().putBoolean(Status.PHONESTATE, true).commit();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}
	
	/**
	 * Handle Data Connection requirement.
	 * - update sampling_rate if necessary
	 * - enable data connection sensing
	 * - turn on Phone State sensing
	 * @param req Data Connection sensor requirement
	 */
	private void handleDataConnectionReq(JSONObject req) {	
	try {
		if (req.has("sampling_rate"))
			updateSamplingRate(req.getInt("sampling_rate"));
		if (req.has("sync_rate"))
			updateSyncRate(req.getInt("sync_rate"));
		
		mainPrefs.edit().putBoolean(PhoneState.DATA_CONNECTION, true).commit();						
		statusPrefs.edit().putBoolean(Status.PHONESTATE, true).commit();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}
	
	/**
	 * Handle Connection Type requirement.
	 * - update sampling_rate if necessary
	 * - enable data connection sensing
	 * - turn on Phone State sensing
	 * @param req Connection type sensor requirement
	 */
	private void handleConnectionTypeReq(JSONObject req) {	
	try {
		if (req.has("sampling_rate"))
			updateSamplingRate(req.getInt("sampling_rate"));
		if (req.has("sync_rate"))
			updateSyncRate(req.getInt("sync_rate"));
		
		mainPrefs.edit().putBoolean(PhoneState.DATA_CONNECTION, true).commit();						
		statusPrefs.edit().putBoolean(Status.PHONESTATE, true).commit();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}
	
	/**
	 * Handle Call State requirement.
	 * - update sampling_rate if necessary
	 * - enable call state sensing
	 * - turn on Phone State sensing
	 * @param req Call state sensor requirement
	 */
	private void handleCallStateReq(JSONObject req) {	
	try {
		if (req.has("sampling_rate"))
			updateSamplingRate(req.getInt("sampling_rate"));
		if (req.has("sync_rate"))
			updateSyncRate(req.getInt("sync_rate"));
		
		mainPrefs.edit().putBoolean(PhoneState.CALL_STATE, true).commit();						
		statusPrefs.edit().putBoolean(Status.PHONESTATE, true).commit();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}
	
	/**
	 * Handle Call State requirement.
	 * - update sampling_rate if necessary
	 * - enable call state sensing
	 * - turn on Phone State sensing
	 * @param req Unread message sensor requirement
	 */
	private void handleUnreadMsgReq(JSONObject req) {	
	try {
		if (req.has("sampling_rate"))
			updateSamplingRate(req.getInt("sampling_rate"));
		if (req.has("sync_rate"))
			updateSyncRate(req.getInt("sync_rate"));
		
		mainPrefs.edit().putBoolean(PhoneState.UNREAD_MSG, true).commit();						
		statusPrefs.edit().putBoolean(Status.PHONESTATE, true).commit();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}
	
	/**
	 * Handle Proximity requirement.
	 * - update sampling_rate if necessary
	 * - enable proximity sensing
	 * - turn on Phone State sensing
	 * @param req Proximity sensor requirement
	 */
	private void handleProximityReq(JSONObject req) {	
	try {
		if (req.has("sampling_rate"))
			updateSamplingRate(req.getInt("sampling_rate"));
		if (req.has("sync_rate"))
			updateSyncRate(req.getInt("sync_rate"));
		
		mainPrefs.edit().putBoolean(PhoneState.PROXIMITY, true).commit();						
		statusPrefs.edit().putBoolean(Status.PHONESTATE, true).commit();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}
	
	/**
	 * Handle Signal Strength requirement.
	 * - update sampling_rate if necessary
	 * - enable signal strength sensing
	 * - turn on Phone State sensing
	 * @param req Signal Strength sensor requirement
	 */
	private void handleSignalStrengthReq(JSONObject req) {	
	try {
		if (req.has("sampling_rate"))
			updateSamplingRate(req.getInt("sampling_rate"));
		if (req.has("sync_rate"))
			updateSyncRate(req.getInt("sync_rate"));
		
		mainPrefs.edit().putBoolean(PhoneState.SIGNAL_STRENGTH, true).commit();						
		statusPrefs.edit().putBoolean(Status.PHONESTATE, true).commit();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}
	
	/**
	 * Handle Screen Activity requirement.
	 * - update sampling_rate if necessary
	 * - enable screen activity sensing
	 * - turn on Phone State sensing
	 * @param req Screen activity sensor requirement
	 */
	private void handleScreenActiviyReq(JSONObject req) {	
	try {
		if (req.has("sampling_rate"))
			updateSamplingRate(req.getInt("sampling_rate"));
		if (req.has("sync_rate"))
			updateSyncRate(req.getInt("sync_rate"));
		
		mainPrefs.edit().putBoolean(PhoneState.SCREEN_ACTIVITY, true).commit();						
		statusPrefs.edit().putBoolean(Status.PHONESTATE, true).commit();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}
	
	/**
	 * Handle IP address requirement.
	 * - update sampling_rate if necessary
	 * - enable IP address sensing
	 * - turn on Phone State sensing
	 * @param req IP Address sensor requirement
	 */
	private void handleIPAddressReq(JSONObject req) {	
	try {
		if (req.has("sampling_rate"))
			updateSamplingRate(req.getInt("sampling_rate"));
		if (req.has("sync_rate"))
			updateSyncRate(req.getInt("sync_rate"));
		
		mainPrefs.edit().putBoolean(PhoneState.IP_ADDRESS, true).commit();						
		statusPrefs.edit().putBoolean(Status.PHONESTATE, true).commit();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}	
	
	/**
	 * Handle Reachability requirement.
	 * - update sampling_rate if necessary
	 * - enable service state sensing
	 * - enable call state sensing
	 * - turn on Phone State sensing
	 * @param req Reachability sensor requirement
	 */
	private void handleReachabilityReq(JSONObject req) {	
	try {
		if (req.has("sampling_rate"))
			updateSamplingRate(req.getInt("sampling_rate"));
		if (req.has("sync_rate"))
			updateSyncRate(req.getInt("sync_rate"));
		
		mainPrefs.edit().putBoolean(PhoneState.SERVICE_STATE, true)
			.putBoolean(PhoneState.CALL_STATE, true)
			.commit();
		
		statusPrefs.edit().putBoolean(Status.PHONESTATE, true).commit();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}
	
	/**
	 * Handle Bluetooth Discovery requirement.
	 * - update sampling_rate if necessary
	 * - enable bluetooth sensing
	 * - turn on Device Proximity sensing
	 * @param req Bluetooth discovery sensor requirement
	 */
	private void handleBluetoothDiscoveryReq(JSONObject req) {	
	try {
		if (req.has("sampling_rate"))
			updateSamplingRate(req.getInt("sampling_rate"));
		if (req.has("sync_rate"))
			updateSyncRate(req.getInt("sync_rate"));
		
		mainPrefs.edit().putBoolean(DevProx.BLUETOOTH, true).commit();						
		statusPrefs.edit().putBoolean(Status.DEV_PROX, true).commit();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}
	
	/**
	 * Handle bluetooth neighbours count requirement.
	 * - update sampling_rate if necessary
	 * - enable bluetooth sensing
	 * - turn on Device Proximity sensing
	 * @param req bluetooth neighbours count sensor requirement
	 */
	private void handleBluetoothNeighboursCountReq(JSONObject req) {	
	try {
		if (req.has("sampling_rate"))
			updateSamplingRate(req.getInt("sampling_rate"));
		if (req.has("sync_rate"))
			updateSyncRate(req.getInt("sync_rate"));
		
		mainPrefs.edit().putBoolean(DevProx.BLUETOOTH, true).commit();						
		statusPrefs.edit().putBoolean(Status.DEV_PROX, true).commit();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}
	
	/**
	 * Handle wifi scan requirement.
	 * - update sampling_rate if necessary
	 * - enable wifi sensing
	 * - turn on Device Proximity sensing
	 * @param req wifi scan sensor requirement
	 */
	private void handleWifiScanReq(JSONObject req) {	
	try {
		if (req.has("sampling_rate"))
			updateSamplingRate(req.getInt("sampling_rate"));
		if (req.has("sync_rate"))
			updateSyncRate(req.getInt("sync_rate"));
		
		mainPrefs.edit().putBoolean(DevProx.WIFI, true).commit();						
		statusPrefs.edit().putBoolean(Status.DEV_PROX, true).commit();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}
	
	/**
	 * Handle audio spectrum requirement.
	 * - update sampling_rate if necessary
	 * - enable mic sensing
	 * - enable audio_specturm sensing
	 * - turn on Ambience sensing
	 * @param req audio_spectrum sensor requirement
	 */
	private void handleAudioSpectrumReq(JSONObject req) {	
	try {
		if (req.has("sampling_rate"))
			updateSamplingRate(req.getInt("sampling_rate"));
		if (req.has("sync_rate"))
			updateSyncRate(req.getInt("sync_rate"));
		
		mainPrefs.edit().putBoolean(Ambience.AUDIO_SPECTRUM, true)
			.putBoolean(Ambience.MIC, true)
			.commit();			
		
		statusPrefs.edit().putBoolean(Status.AMBIENCE, true).commit();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}
	
	/**
	 * Handle light requirement.
	 * - update sampling_rate if necessary
	 * - enable light sensing
	 * - turn on Ambience sensing
	 * @param req light sensor requirement
	 */
	private void handleLightReq(JSONObject req) {	
	try {
		if (req.has("sampling_rate"))
			updateSamplingRate(req.getInt("sampling_rate"));
		if (req.has("sync_rate"))
			updateSyncRate(req.getInt("sync_rate"));
		
		mainPrefs.edit().putBoolean(Ambience.LIGHT, true).commit();			
		
		statusPrefs.edit().putBoolean(Status.AMBIENCE, true).commit();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}
	
	/**
	 * Handle camera light requirement.
	 * - update sampling_rate if necessary
	 * - enable camera light sensing
	 * - turn on Ambience sensing
	 * @param req camera_light sensor requirement
	 */
	private void handleCameraLightReq(JSONObject req) {	
	try {
		if (req.has("sampling_rate"))
			updateSamplingRate(req.getInt("sampling_rate"));
		if (req.has("sync_rate"))
			updateSyncRate(req.getInt("sync_rate"));
		
		mainPrefs.edit().putBoolean(Ambience.CAMERA_LIGHT, true).commit();			
		
		statusPrefs.edit().putBoolean(Status.AMBIENCE, true).commit();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}
	
	/**
	 * Handle noise sensor requirement.
	 * - update sampling_rate if necessary
	 * - enable Mic sensing
	 * - turn on Ambience sensing
	 * @param req noise_sensor requirement
	 */
	private void handleNoiseSensorReq(JSONObject req) {	
	try {
		if (req.has("sampling_rate"))
			updateSamplingRate(req.getInt("sampling_rate"));
		if (req.has("sync_rate"))
			updateSyncRate(req.getInt("sync_rate"));
		
		mainPrefs.edit().putBoolean(Ambience.MIC, true).commit();			
		
		statusPrefs.edit().putBoolean(Status.AMBIENCE, true).commit();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}

	/**
	 * Handle loudness sensor requirement.
	 * - update sampling_rate if necessary
	 * - enable Mic sensing
	 * - turn on Ambience sensing
	 * @param req loudness requirement
	 */
	private void handleLoudnessReq(JSONObject req) {	
	try {
		if (req.has("sampling_rate"))
			updateSamplingRate(req.getInt("sampling_rate"));
		if (req.has("sync_rate"))
			updateSyncRate(req.getInt("sync_rate"));
		
		mainPrefs.edit().putBoolean(Ambience.MIC, true).commit();		
		statusPrefs.edit().putBoolean(Status.AMBIENCE, true).commit();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}

	/**
	 * Handle pressure sensor requirement.
	 * - update sampling_rate if necessary
	 * - enable pressure sensing
	 * - turn on Ambience sensing
	 * @param req pressure requirement
	 */
	private void handlePressureReq(JSONObject req) {	
	try {
		if (req.has("sampling_rate"))
			updateSamplingRate(req.getInt("sampling_rate"));
		if (req.has("sync_rate"))
			updateSyncRate(req.getInt("sync_rate"));
		
		mainPrefs.edit().putBoolean(Ambience.PRESSURE, true).commit();		
		statusPrefs.edit().putBoolean(Status.AMBIENCE, true).commit();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}
	
	/**
	 * Handle magnetic field sensor requirement.
	 * - update sampling_rate if necessary
	 * - enable pressure sensing
	 * - turn on Ambience sensing
	 * @param req magnetic_field requirement
	 */
	private void handleMagneticFieldReq(JSONObject req) {	
	try {
		if (req.has("sampling_rate"))
			updateSamplingRate(req.getInt("sampling_rate"));
		if (req.has("sync_rate"))
			updateSyncRate(req.getInt("sync_rate"));
		
		mainPrefs.edit().putBoolean(Ambience.MAGNETIC_FIELD, true).commit();		
		statusPrefs.edit().putBoolean(Status.AMBIENCE, true).commit();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}
	
	/**
	 * Handle orientation sensor requirement.
	 * - update sampling_rate if necessary
	 * - enable unregister between sample sensing
	 * - enable screenoff_fix
	 * - turn on Motion sensing
	 * @param req orientation requirement
	 */
	private void handleOrientationReq(JSONObject req) {	
	try {
		if (req.has("sampling_rate"))
			updateSamplingRate(req.getInt("sampling_rate"));
		if (req.has("sync_rate"))
			updateSyncRate(req.getInt("sync_rate"));
		
		mainPrefs.edit().putBoolean(Motion.UNREG, true)
			.putBoolean(Motion.SCREENOFF_FIX, true).commit();
		
		statusPrefs.edit().putBoolean(Status.MOTION, true).commit();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}
	
	/**
	 * Handle linear acceleration requirement.
	 * - update sampling_rate if necessary
	 * - enable unregister between sample sensing
	 * - enable screenoff_fix
	 * - turn on Motion sensing
	 * @param req linear acceleration requirement
	 */
	private void handleLinearAccelerationReq(JSONObject req) {	
	try {
		if (req.has("sampling_rate"))
			updateSamplingRate(req.getInt("sampling_rate"));
		if (req.has("sync_rate"))
			updateSyncRate(req.getInt("sync_rate"));
		
		mainPrefs.edit().putBoolean(Motion.UNREG, true)
			.putBoolean(Motion.SCREENOFF_FIX, true).commit();
		
		statusPrefs.edit().putBoolean(Status.MOTION, true).commit();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}
	
	/**
	 * Handle accelerometer requirement.
	 * - update sampling_rate if necessary
	 * - enable unregister between sample sensing
	 * - enable screenoff_fix
	 * - turn on Motion sensing
	 * @param req accelerometer requirement
	 */
	private void handleAccelerometerReq(JSONObject req) {	
	try {
		if (req.has("sampling_rate"))
			updateSamplingRate(req.getInt("sampling_rate"));
		if (req.has("sync_rate"))
			updateSyncRate(req.getInt("sync_rate"));
		
		mainPrefs.edit().putBoolean(Motion.UNREG, true)
			.putBoolean(Motion.SCREENOFF_FIX, true).commit();
		
		statusPrefs.edit().putBoolean(Status.MOTION, true).commit();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}
	
	/**
	 * Handle motion energy requirement.
	 * - update sampling_rate if necessary
	 * - enable unregister between sample sensing
	 * - enable motion energy sensor
	 * - enable screenoff_fix
	 * - turn on Motion sensing
	 * @param req accelerometer requirement
	 */
	private void handleMotionEnergyReq(JSONObject req) {	
	try {
		if (req.has("sampling_rate"))
			updateSamplingRate(req.getInt("sampling_rate"));
		if (req.has("sync_rate"))
			updateSyncRate(req.getInt("sync_rate"));
		
		mainPrefs.edit().putBoolean(Motion.UNREG, true)
			.putBoolean(Motion.SCREENOFF_FIX, true).commit();
		
		statusPrefs.edit().putBoolean(Status.MOTION, true).commit();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}
	
	/**
	 * Handle Activity requirement.
	 * - update sampling_rate if necessary
	 * - enable unregister between sample sensing
	 * - enable screenoff_fix
	 * - turn on Motion sensing
	 * @param req Activity requirement
	 */
	private void handleActivityReq(JSONObject req) {	
	try {
		if (req.has("sampling_rate"))
			updateSamplingRate(req.getInt("sampling_rate"));
		if (req.has("sync_rate"))
			updateSyncRate(req.getInt("sync_rate"));
		
		mainPrefs.edit().putBoolean(Motion.UNREG, true)
			.putBoolean(Motion.SCREENOFF_FIX, true).commit();
		
		statusPrefs.edit().putBoolean(Status.MOTION, true).commit();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	}
	
	/**
	 * Handle attachToMyrianode requirement.
	 * - enable attachToMyrianode preference
	 * @param req attachTo requirement
	 */
	private void handleAttachToMyrianodeReq(JSONObject req) {		
		mainPrefs.edit().putBoolean(Advanced.LOCATION_FEEDBACK, true).commit();			
	}
	
	
}
