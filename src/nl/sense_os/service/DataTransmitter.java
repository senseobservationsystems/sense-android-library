package nl.sense_os.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DataTransmitter extends BroadcastReceiver {

    @SuppressWarnings("unused")
    private static final String TAG = "Sense DataTransmitter";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        
        // start database leeglepelaar
        Intent alarm = new Intent(context, DataTransmitter.class);
        PendingIntent operation = PendingIntent.getBroadcast(context, 1, alarm, 0);
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5000, operation);
        
        // start send task
        Intent task = new Intent(context, MsgHandler.class);
        task.putExtra(MsgHandler.KEY_INTENT_TYPE, MsgHandler.TYPE_SEND_MSG);
        context.startService(task);
    }
}
