package nl.sense_os.service.feedback;

import java.net.URI;
import java.net.URISyntaxException;

import nl.sense_os.app.SenseSettings;
import nl.sense_os.service.SenseApi;

import org.json.JSONObject;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class FeedbackChecker extends IntentService {

    public static final String ACTION_CHECK_FEEDBACK = "nl.sense_os.service.DoCheckFeedback";
    private static final String TAG = "FeedbackChecker";

    public FeedbackChecker() {
        super("Sense FeedbackChecker");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        String action = intent.getAction();
        if (action.equals(ACTION_CHECK_FEEDBACK)) {
            checkFeedback();
        }
    }

    private void checkFeedback() {
        Log.d(TAG, "checkFeedback");

        String sensorName = "feedback";
        String sensorValue = "hoi";
        String dataType = "string";
        String deviceType = "feedback";
        String url = SenseApi.getSensorURL(this, sensorName, sensorValue, dataType, deviceType);

        SharedPreferences prefs = getSharedPreferences(SenseSettings.PRIVATE_PREFS, MODE_PRIVATE);
        String cookie = prefs.getString(SenseSettings.PREF_LOGIN_COOKIE, "");

        try {
            JSONObject json = SenseApi.getJSONObject(new URI(url), cookie);
        } catch (URISyntaxException e) {
            Log.e(TAG, "URISyntaxException checking feedback sensor", e);
        }

    }
}
