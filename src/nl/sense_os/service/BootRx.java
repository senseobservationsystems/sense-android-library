/*
 ************************************************************************************************************
 *     Copyright (C)  2010 Sense Observation Systems, Rotterdam, the Netherlands.  All rights reserved.     *
 ************************************************************************************************************
 */
package nl.sense_os.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootRx extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences statusPrefs = context.getSharedPreferences(Constants.STATUS_PREFS,
                Context.MODE_WORLD_WRITEABLE);
        final boolean autostart = statusPrefs.getBoolean(Constants.PREF_AUTOSTART, false);

        // automatically start the Sense service if this is set in the preferences
        if (true == autostart) {
            Intent startService = new Intent("nl.sense_os.service.ISenseService");
            context.startService(startService);
        }
    }
}
