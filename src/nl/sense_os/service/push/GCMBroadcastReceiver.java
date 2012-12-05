package nl.sense_os.service.push;

import nl.sense_os.service.R;
import android.content.Context;

/**
 * @author Ahmy Yulrizka <ahmy@sense-os.nl>
 */
public class GCMBroadcastReceiver extends
		com.google.android.gcm.GCMBroadcastReceiver {

	@Override
	protected String getGCMIntentServiceClassName(Context context) {            
	return context.getString(R.string.action_sense_gcm_intent_service);
	}	
}
