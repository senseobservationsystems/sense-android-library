package sensei.sense_os.nl.sensedemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import nl.sense_os.service.SenseService;

/**
 * Created by ted on 10/27/15.
 */
public class HandleMessages extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // check for the relogin
        if(intent.getBooleanExtra(SenseService.EXTRA_RELOGIN, false)) {
            // Session probably experienced handle the re-login for syncing with the backend
            // call login
            SetupUser.login(context);
        }
        // check for reboot
        else if(action.equalsIgnoreCase("android.intent.action.BOOT_COMPLETED"))
        {
            // call login
            SetupUser.login(context);
            // start the data syncer
            StoreAndSyncData.startDataSyncer(context);
        }
    }
}
