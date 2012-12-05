package nl.sense_os.service.configuration;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;



public class RequirementReceiver extends BroadcastReceiver {

	/**
	 * Main entrance of the function, start a ConfigurationService and pass
	 * the requirement
	 * @param context
	 * @param intent Intenet contain requirements in JSON format
	 */
	@Override
	public void onReceive(Context context, Intent intent) {						
		Intent reqsIntent = new Intent(context, ConfigurationService.class);
		
		//pass the requirement
		reqsIntent.putExtra("requirements", intent.getStringExtra("requirements"));
		context.startService(reqsIntent);
	}
}
