package nl.sense_os.service.phonestate;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.SensorDataPoint;
import nl.sense_os.service.subscription.BaseDataProducer;


/**
 * Represents the app info sensor. It subscribes itself to the following broadcasts:
 * <ul>
 * <li> LOCALE_CHANGED (sent by Android whenever the language is changed in Android settings)</li>
 * <li> PACKAGE_REPLACED (sent by Android whenever an app is updated)</li>
 * <li> ACTION_AFTER_LOGIN (sent by this class as soon as the sensor is started, ususally after login)</li>
 * </ul>
 * 
 * It sends the following information;
 * <ul>
 * <li> Application build (version code, e.g. 1413357349)</li>
 * <li> Application version (version name, e.g. 3.0.0)</li>
 * <li> Application name (e.g. Goalie)</li>
 * <li> OS (always "Android")</li>
 * <li> OS Version (e.g. 4.2.2)</li>
 * <li> Device model (e.g. Galaxy Nexus)</li>
 * <li> Sense Library Version (UNIMPLEMENTED)</li>
 * <li> Sense Cortex Version (UNIMPLEMENTED)</li>
 * </ul>
 * 
 * @author yfke@sense-os.nl
 *
 */


public class AppInfoSensor extends BaseDataProducer {
	private static final String TAG = "Sense app info sensor";
	private Context context;
	private static AppInfoSensor instance = null;
	
	private static final String ACTION_AFTER_LOGIN = "nl.sense_os.service.phonestate.AFTER_LOGIN";
	
//	public AppInfoSensor() {}
	
	protected AppInfoSensor(Context context) {
		this.context = context;
	}
	
	public static AppInfoSensor getInstance(Context context) {
		if(instance == null)
			instance = new AppInfoSensor(context);
		return instance;
	}
	
	private BroadcastReceiver appInfoReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG,"Received app info broadcast, will send sensor data.");
			
			String action = intent.getAction();
			
			//Language settings were changed
			if(action.equals(Intent.ACTION_LOCALE_CHANGED)) {
				Log.d(TAG,"Locale was changed in settings, sending app info to sensor");
				sendAppInfo();
			}
			
			//Package was updated/replaced
			if(action.equals(Intent.ACTION_PACKAGE_REPLACED)
					&& intent.getData().getSchemeSpecificPart().equals(context.getPackageName())) {
				Log.d(TAG,"Application was updated, sending app info to sensor");
				sendAppInfo();
			}
			
			//Sensing has started (user has logged in)
			if(action.equals(ACTION_AFTER_LOGIN)) {
				Log.d(TAG,"User has logged in, sending app info to sensor");
				sendAppInfo();
			}
		}
		
		private void sendAppInfo() {
			String appBuild              = ""; //set below
			String appName               = ""; //set below
			String appVersion            = ""; //set below
			String locale                = context.getResources().getConfiguration().locale.toString();
			String os                    = "Android";
			String os_version            = android.os.Build.VERSION.RELEASE;
			String device_model          = android.os.Build.MODEL;
			String sense_library_version = AppInfoVersion.SENSE_LIBRARY_VERSION;
			String sense_cortex_version  = AppInfoVersion.CORTEX_VERSION;
			
			try{
				PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(),0);
				appName = pInfo.packageName;
				appBuild = Integer.toString(pInfo.versionCode); //TODO change this to simply versionName?
			    appVersion = pInfo.versionName;
			} catch(NameNotFoundException e) {
				Log.e(TAG, "Cannot get package info. Will send empty application name / version.");
			}
			
			JSONObject json = new JSONObject();
			try {
				json.put("app_build",    appBuild);
				json.put("app_name",     appName);
				json.put("app_version",  appVersion);
				json.put("locale",       locale);
				json.put("os",           os);
				json.put("os_version",   os_version);
				json.put("device_model", device_model);
				json.put("sense_library_version", sense_library_version);
				json.put("sense_cortex_version",  sense_cortex_version);
			} catch(JSONException e) {
				Log.e(TAG,"JSON Exception occurred while sending app info.");
			}
			
			notifySubscribers();
        	SensorDataPoint dataPoint = new SensorDataPoint(json);
        	dataPoint.sensorName = SensorNames.APP_INFO_SENSOR;
        	dataPoint.sensorDescription = SensorNames.APP_INFO_SENSOR;
        	dataPoint.timeStamp = SNTP.getInstance().getTime();        
        	Log.d(TAG,"Going to sendToSubscribers");
        	sendToSubscribers(dataPoint);
        	
            Intent i = new Intent(context.getString(R.string.action_sense_new_data));
            i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
            i.putExtra(DataPoint.VALUE, json.toString());
            i.putExtra(DataPoint.SENSOR_NAME, SensorNames.APP_INFO_SENSOR);
            i.putExtra(DataPoint.TIMESTAMP, dataPoint.timeStamp);
            context.startService(i);
		}
	};
	
	public void startAppInfoSensing() {
		//listen for when the user has changed language settings,
		//or when the app is updated through Play Store (these two
		//actions do not necessarily require the user to re-log)
		IntentFilter filter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
		filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
		context.registerReceiver(appInfoReceiver, filter);
		
		//Also immediately send the app info (this happens e.g. after first login, reboot, etc)
		Intent intent = new Intent(ACTION_AFTER_LOGIN);
		context.sendBroadcast(intent);
	}
	
	public void stopAppInfoSensing() {
		try{
			context.unregisterReceiver(appInfoReceiver);
		} catch(Exception e) {
			Log.e(TAG, e.getMessage());
		}
	}
}