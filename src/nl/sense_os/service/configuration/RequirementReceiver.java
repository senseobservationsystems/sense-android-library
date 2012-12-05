package nl.sense_os.service.configuration;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


/**
 * @author Ahmy Yulrizka <ahmy@sense-os.nl>
 */
public class RequirementReceiver extends BroadcastReceiver {	
	@Override
	public void onReceive(Context context, Intent intent) {						
		Intent reqsIntent = new Intent(context, ConfigurationService.class);
		
		//pass the requirement
		reqsIntent.putExtra("requirements", intent.getStringExtra("requirements"));
		context.startService(reqsIntent);
	}
}
