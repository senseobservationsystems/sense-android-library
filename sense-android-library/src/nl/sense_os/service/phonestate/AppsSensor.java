package nl.sense_os.service.phonestate;

import java.util.ArrayList;
import java.util.List;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main.PhoneState;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.PeriodicPollAlarmReceiver;
import nl.sense_os.service.shared.PeriodicPollingSensor;
import nl.sense_os.service.shared.SensorDataPoint;
import nl.sense_os.service.subscription.BaseDataProducer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

public class AppsSensor extends BaseDataProducer implements PeriodicPollingSensor {

	private static final String TAG = "Sense Apps Sensor";
	private static AppsSensor instance;

	public static AppsSensor getInstance(Context context) {
		if (null == instance) {
			instance = new AppsSensor(context);
		}
		return instance;
	}

	private final Context context;
	private final PeriodicPollAlarmReceiver alarmReceiver;
	private boolean active;
	private long sampleDelay;

	private AppsSensor(Context context) {
		this.context = context;
		this.alarmReceiver = new PeriodicPollAlarmReceiver(this);
	}

	@Override
	public void doSample() {
		//Log.v(TAG, "do sample");
		List<ResolveInfo> installedApps = getInstalledApps();
		SharedPreferences mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
		if (mainPrefs.getBoolean(PhoneState.FOREGROUND_APP, true)) {
			// get app info from package manager					
			JSONObject fapp = getForgroundApp(installedApps);		
			if(fapp != null)
				sendForegroundApp(fapp);
		}
		// send data about all installed apps
		if (mainPrefs.getBoolean(PhoneState.INSTALLED_APPS, false)) {
			PackageManager pm = context.getPackageManager();
			List<String> installedAppLabels = new ArrayList<String>();
			for (ResolveInfo installedApp : installedApps) {
				String app = installedApp.loadLabel(pm).toString();
				// TODO fix filthy way of filtering out an unparseable character ('tm')
				app = app.replaceAll("\u2122", "");
				installedAppLabels.add(app);
			}
			sendInstalledApps(installedAppLabels);
		}

	}

	private void sendInstalledApps(List<String> apps) {

		try {

			// create value JSON object
			JSONObject value = new JSONObject();
			value.put("installed", new JSONArray(apps));
			// TODO figure out a better way to send an array of objects			
			notifySubscribers();
			SensorDataPoint dataPoint = new SensorDataPoint(value);
			dataPoint.sensorName = SensorNames.APP_INSTALLED;
			dataPoint.sensorDescription = SensorNames.APP_INSTALLED;
			dataPoint.timeStamp = SNTP.getInstance().getTime();        
			sendToSubscribers(dataPoint);
		} catch (JSONException e) {
			Log.e(TAG, "Failed to create data point for installed apps sensor!", e);
		}
	}

	private void sendForegroundApp(JSONObject foreGroundApp) {		
		notifySubscribers();
		SensorDataPoint dataPoint = new SensorDataPoint(foreGroundApp);
		dataPoint.sensorName = SensorNames.APP_FOREGROUND;
		dataPoint.sensorDescription = SensorNames.APP_FOREGROUND;
		dataPoint.timeStamp = SNTP.getInstance().getTime();        
		sendToSubscribers(dataPoint);
	}


	private List<ResolveInfo> getInstalledApps() {
		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		List<ResolveInfo> result = context.getPackageManager().queryIntentActivities(mainIntent, 0);

		return result;
	}


	private JSONObject getForgroundApp(List<ResolveInfo> installedApps) {
		ActivityManager actvityManager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);

		List<RunningTaskInfo> result = actvityManager.getRunningTasks(1);

		JSONObject foregroundApp = new JSONObject();
		// send data about important apps
		PackageManager pm = context.getPackageManager();
		for (ResolveInfo installedApp : installedApps) {
			if(installedApp.activityInfo.processName.compareTo(result.get(0).baseActivity.getPackageName()) == 0)
			{				
				try {
					foregroundApp.put("label",  installedApp.loadLabel(pm));
					foregroundApp.put("process", installedApp.activityInfo.processName);
					foregroundApp.put("activity", result.get(0).topActivity.getShortClassName().replaceFirst(".",""));
					return foregroundApp;
				} catch (JSONException e) {					
					Log.e(TAG, e.getMessage());
				}

			}
		}
		return null;
	}

	@Override
	public boolean isActive() {
		return active;
	}

	@Override
	public void startSensing(long sampleDelay) {
		// TODO Auto-generated method stub
		this.sampleDelay = sampleDelay;
		alarmReceiver.start(context);
		active = true;
	}

	@Override
	public void stopSensing() {
		// TODO Auto-generated method stub
		alarmReceiver.stop(context);
		active = false;
	}

	@Override
	public long getSampleRate() {
		// TODO Auto-generated method stub
		return sampleDelay;
	}

	@Override
	public void setSampleRate(long sampleDelay) {
		// TODO Auto-generated method stub
		this.sampleDelay = sampleDelay;		
	}

}
