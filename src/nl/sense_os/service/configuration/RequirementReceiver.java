/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.configuration;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Entrance class for requirement changes from GCM.<br/>
 * <br/>
 * The requirements should be put in an Intent extra of the broadcast. When a new requirement is
 * received, a ConfigurationService is started and passed the requirements.
 *
 * @author Ahmy Yulrizka <ahmy@sense-os.nl>
 */
public class RequirementReceiver extends BroadcastReceiver {

    /**
     * Key for Intent extra containing the requirements
     */
    public static final String EXTRA_REQUIREMENTS = "requirements";
	
	@Override
	public void onReceive(Context context, Intent intent) {						
		Intent reqsIntent = new Intent(context, ConfigurationService.class);
		
		//pass the requirement
        reqsIntent.putExtra(EXTRA_REQUIREMENTS, intent.getStringExtra(EXTRA_REQUIREMENTS));
		context.startService(reqsIntent);
	}
}
