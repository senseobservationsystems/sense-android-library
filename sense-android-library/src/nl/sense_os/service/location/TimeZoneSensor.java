package nl.sense_os.service.location;
import java.util.TimeZone;

import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.SensorDataPoint;
import nl.sense_os.service.subscription.BaseDataProducer;

public class TimeZoneSensor extends BaseDataProducer 
{
    public static String TAG = Class.class.getSimpleName();
    TimeZoneChangeReceiver tzChangeReceiver;
    boolean active = false;
    protected static TimeZoneSensor instance;
    protected Context context;

    protected TimeZoneSensor(Context context) 
    {
        this.context = context;
    }

    /**
     * Start sensing
     * 
     * register for time zone changes
     * send the current time zone
     */
    public void startSensing() 
    {
        if(tzChangeReceiver != null)
            return;

        tzChangeReceiver = new TimeZoneChangeReceiver();
        context.registerReceiver(tzChangeReceiver, new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED));
        sendTimeZone();
    }

    /*
     * Stop sensing
     * 
     * unregister for time zone changes
     */
    public void stopSensing() 
    {
        if(tzChangeReceiver == null)
            return;

        context.unregisterReceiver(tzChangeReceiver);
        tzChangeReceiver = null;
    }

    public static TimeZoneSensor getInstance(Context context)
    {
        if(instance == null)
            instance = new TimeZoneSensor(context);
        return instance;
    }

    protected void sendTimeZone()
    {
        try
        {
            // send to subscribers
            this.notifySubscribers();

            TimeZone timeZone = TimeZone.getDefault();
            JSONObject timeZoneJson = new JSONObject();
            timeZoneJson.put("offset", timeZone.getOffset(SNTP.getInstance().getTime())/1000l);
            timeZoneJson.put("id", timeZone.getID());
            SensorDataPoint dataPoint = new SensorDataPoint(timeZoneJson);
            dataPoint.sensorName = SensorNames.TIME_ZONE;
            dataPoint.sensorDescription = "Current time zone";
            dataPoint.timeStamp = SNTP.getInstance().getTime();
            //Log.d(TAG,"time zone subscribers: " + this.);
            this.sendToSubscribers(dataPoint);
        }
        catch(Exception e)
        {
            Log.e(TAG, "Error in sending TimeZone");
        }
    }

    private class TimeZoneChangeReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            // time zone has changed
            if (!Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction()))
                return;

            TimeZoneSensor.this.sendTimeZone();
            Log.d(TAG, "TimeZone change");
        }
    };
}
