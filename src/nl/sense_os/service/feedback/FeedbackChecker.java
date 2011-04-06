package nl.sense_os.service.feedback;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import nl.sense_os.service.Constants;
import nl.sense_os.service.MsgHandler;
import nl.sense_os.service.SenseApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class FeedbackChecker extends IntentService {

    public static final String ACTION_CHECK_FEEDBACK = "nl.sense_os.service.DoCheckFeedback";
    private static final String TAG = "FeedbackChecker";

    public FeedbackChecker() {
        super("Sense FeedbackChecker");
    }

    private void checkFeedback() {
        // Log.d(TAG, "checkFeedback");

        String url = getFeedbackUrl();

        if (null != url) {
            // only get the last value
            url += "?last=1";

            // get cookie for authentication
            final SharedPreferences authPrefs = getSharedPreferences(Constants.AUTH_PREFS,
                    MODE_PRIVATE);
            String cookie = authPrefs.getString(Constants.PREF_LOGIN_COOKIE, null);

            // get last feedback sensor value
            if (cookie != null) {
                try {
                    JSONObject json = SenseApi.getJsonObject(new URI(url), cookie);
                    parseFeedback(json);
                } catch (URISyntaxException e) {
                    Log.e(TAG, "URISyntaxException checking feedback sensor", e);
                }
            }
        }
    }

    /**
     * @return URL of the feedback sensor for this user on CommonSense
     */
    private String getFeedbackUrl() {

        // prepare dummy JSON object as value to create new feedback sensor
        Map<String, Object> jsonValues = new HashMap<String, Object>();
        jsonValues.put("action", "none");
        jsonValues.put("status", "idle");
        jsonValues.put("uri", "http://foo.bar");

        // get URL of feedback sensor data
        String sensorName = "feedback";
        String sensorValue = new JSONObject(jsonValues).toString();
        String dataType = "json";
        String deviceType = "feedback";

        return SenseApi.getSensorUrl(this, sensorName, sensorValue, dataType, deviceType);
    }

    /**
     * Handles the feedback request, starting activities based on the properties of the feedback
     * 
     * @param status
     *            status of the feedback value (idle/unread/read)
     * @param action
     *            (optional) action to take (popup)
     * @param uri
     *            (optional) resource that can be used to take the action
     * @param timestamp
     *            timestamp of the feedback message
     */
    private void handleFeedback(String status, String action, String uri, double timestamp) {

        Log.d(TAG, "Feedback status: " + status);
        if (status.equals("idle") || status.equals("read")) {
            // do nothing

        } else if (status.equals("unread")) {
            Log.d(TAG, "unread feedback: \'" + action + "\' (" + uri + ")");
            try {
                Intent myAsk = new Intent("nl.sense_os.myask.FEEDBACK");
                myAsk.putExtra("uri", uri);
                myAsk.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(myAsk);
                unsetFeedback(timestamp);
            } catch (ActivityNotFoundException e) {
                String msg = "Cannot find MyASK activity";
                Log.e(TAG, msg);
                signalError(msg, timestamp);
            } catch (Exception e) {
                String msg = "Exception starting MyASK activity ";
                Log.e(TAG, msg, e);
                signalError(msg + e.getMessage(), timestamp);
            }

        } else {
            Log.w(TAG, "Unexpected feedback status: \'" + status + "\'");
        }
    }

    /**
     * Called when the service gets in Intent to handle. Looks at the associated action String to
     * handle the task.
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        String action = intent.getAction();
        if (action.equals(ACTION_CHECK_FEEDBACK)) {
            checkFeedback();
        } else {
            Log.w(TAG, "Unexpected intent action: \'" + action + "\'");
        }
    }

    /**
     * Parses the properties of the feedback sensor value, passing them on to
     * {@link #handleFeedback(String, String, String)}.
     * 
     * @param json
     *            raw JSON sensor value, with status, action and uri properties
     */
    private void parseFeedback(JSONObject json) {

        if (null == json) {
            Log.w(TAG, "Feedback checking failed...");
            return;
        }

        try {
            // Log.d(TAG, "Feedback: " + json.toString(2));

            int total = json.getInt("total");
            if (total > 0) {
                // get feedback value
                JSONArray data = json.getJSONArray("data");
                JSONObject dataPoint = data.getJSONObject(0);
                String value = dataPoint.getString("value");
                double date = Double.parseDouble(dataPoint.getString("date"));

                // strip slashes, re-create JSON sensor value
                value = value.replaceAll("\\\\", "");
                JSONObject feedback = new JSONObject(value);

                // get feedback properties
                String status = feedback.getString("status");
                String action = feedback.optString("action");
                String uri = feedback.optString("uri");
                handleFeedback(status, action, uri, date);

            } else {
                // Log.d(TAG, "No feedback available (yet)...");
            }

        } catch (JSONException e) {
            Log.e(TAG, "JSONException parsing feedback value", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected exception parsing feedback!", e);
        }
    }

    /**
     * Sets feedback sensor to "error" state
     * 
     * @param message
     *            error message
     * @param date
     *            timestamp of last feedback value
     */
    private void signalError(String message, double timestamp) {

        Map<String, Object> jsonValues = new HashMap<String, Object>();
        jsonValues.put("status", "error");
        jsonValues.put("action", message);
        jsonValues.put("uri", "" + timestamp);
        JSONObject json = new JSONObject(jsonValues);

        // send new feedback sensor value
        Intent newMsg = new Intent(MsgHandler.ACTION_NEW_MSG);
        newMsg.putExtra(MsgHandler.KEY_SENSOR_NAME, "feedback");
        newMsg.putExtra(MsgHandler.KEY_SENSOR_DEVICE, "feedback");
        newMsg.putExtra(MsgHandler.KEY_DATA_TYPE, Constants.SENSOR_DATA_TYPE_JSON);
        if (timestamp > System.currentTimeMillis() / 1000) {
            // make sure we really add this update as the newest value
            newMsg.putExtra(MsgHandler.KEY_TIMESTAMP, (long) ((timestamp + 1) * 1000));
        } else {
            newMsg.putExtra(MsgHandler.KEY_TIMESTAMP, System.currentTimeMillis());
        }
        newMsg.putExtra(MsgHandler.KEY_VALUE, json.toString());
        startService(newMsg);

        // make sure the value is sent immediately
        Intent startSending = new Intent(MsgHandler.ACTION_SEND_DATA);
        startService(startSending);
    }

    /**
     * Sets the status of the feedback sensor to "read"
     * 
     * @param timestamp
     *            time of the last feedback value
     */
    private void unsetFeedback(double timestamp) {

        Map<String, Object> jsonValues = new HashMap<String, Object>();
        jsonValues.put("status", "read");
        jsonValues.put("action", "");
        jsonValues.put("uri", "" + timestamp);
        JSONObject json = new JSONObject(jsonValues);

        // send new feedback sensor value
        Intent newMsg = new Intent(MsgHandler.ACTION_NEW_MSG);
        newMsg.putExtra(MsgHandler.KEY_SENSOR_NAME, "feedback");
        newMsg.putExtra(MsgHandler.KEY_SENSOR_DEVICE, "feedback");
        newMsg.putExtra(MsgHandler.KEY_DATA_TYPE, Constants.SENSOR_DATA_TYPE_JSON);
        if (timestamp > System.currentTimeMillis() / 1000) {
            // make sure we really add this update as the newest value
            newMsg.putExtra(MsgHandler.KEY_TIMESTAMP, (long) ((timestamp + 1) * 1000));
        } else {
            newMsg.putExtra(MsgHandler.KEY_TIMESTAMP, System.currentTimeMillis());
        }
        newMsg.putExtra(MsgHandler.KEY_VALUE, json.toString());
        startService(newMsg);

        // make sure the value is sent immediately
        Intent startSending = new Intent(MsgHandler.ACTION_SEND_DATA);
        startService(startSending);
    }
}
