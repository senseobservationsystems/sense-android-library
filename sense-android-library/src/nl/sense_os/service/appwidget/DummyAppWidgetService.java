package nl.sense_os.service.appwidget;

import android.app.IntentService;
import android.content.Intent;

/**
 * Trivial intent service that can be used to receiver app widget update intents, when the
 * application does not have an app widget of its own. Otherwise, a warning is produced whenever the
 * Sense service status changes.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class DummyAppWidgetService extends IntentService {

    public DummyAppWidgetService() {
        super("DummyAppWidgetService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // do nothing
    }
}
