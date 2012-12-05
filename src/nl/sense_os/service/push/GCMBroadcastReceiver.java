/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.push;

import nl.sense_os.service.R;
import android.content.Context;

/**
 * This class is just Boilerplate to use GCM.
 * 
 */
public class GCMBroadcastReceiver extends com.google.android.gcm.GCMBroadcastReceiver {

    @Override
    protected String getGCMIntentServiceClassName(Context context) {
        return context.getString(R.string.action_sense_gcm_intent_service);
    }
}
