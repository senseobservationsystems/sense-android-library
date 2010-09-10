package nl.sense_os.service.phonestate;


import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import nl.sense_os.app.SenseSettings;
import nl.sense_os.service.MsgHandler;

public class PhoneActivitySensor  {
	@SuppressWarnings("unused")
	private static final String TAG = "Sense Phone activity";
	private MsgHandler msgHandler;
	private long sampleDelay = 0; //in milliseconds    
	private long lastSampleTime;
	
	private Context context;
	private static final String PHONE_ACTIVITY = "phone activity";	

	public PhoneActivitySensor(MsgHandler handler, Context _context) {
		this.msgHandler = handler;
		this.context = _context;
	}

	private BroadcastReceiver screenActivityReceiver = new BroadcastReceiver() {

		public void onReceive(Context context, Intent intent) 
		{	
			if(System.currentTimeMillis() > lastSampleTime+sampleDelay)
			{
				// Send a message when the screen state has changed
				String screen = "";			
				if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) 
					screen = "off";
				else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) 
					screen = "on";		
				// check if the intent was a screen change intent
				if(screen.length() > 0)
				{
					JSONObject json = new JSONObject();
					try {
						json.put("screen", screen);

					} catch (JSONException e) {
						Log.e(TAG, "JSONException preparing screen activity data");
					}

					msgHandler.sendSensorData(PHONE_ACTIVITY, json.toString(), SenseSettings.SENSOR_DATA_TYPE_JSON);	
					lastSampleTime = System.currentTimeMillis();
				}
				// check if the intent was a activity change intent
				
				
			}
		}
	};



	public void setSampleDelay(long _sampleDelay)
	{
		sampleDelay = _sampleDelay;    	
	}

	public void startPhoneActivitySensing(long _sampleDelay)
	{		
		setSampleDelay(_sampleDelay);		
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);	
		filter.addAction(Intent.ACTION_SCREEN_OFF);	
		context.registerReceiver(screenActivityReceiver, filter);
	}

	public void stopPhoneActivitySensing()
	{
		try{			
			context.unregisterReceiver(screenActivityReceiver);
		}catch(Exception e)
		{
			Log.e(TAG, e.getMessage());
		}

	}

	public long getSampleDelay()
	{
		return sampleDelay;
	}
}
