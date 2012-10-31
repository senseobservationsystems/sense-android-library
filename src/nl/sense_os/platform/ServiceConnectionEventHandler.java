package nl.sense_os.platform;

import nl.sense_os.service.ISenseService;
import android.content.ComponentName;

public interface ServiceConnectionEventHandler {
	public void onServiceConnected(ComponentName className, ISenseService service);

	public void onServiceDisconnected(ComponentName className);
}
