package nl.sense_os.service.feedback;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import nl.sense_os.service.commonsense.SenseApi;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Auth;
import nl.sense_os.service.constants.SensePrefs.Main.Advanced;
import nl.sense_os.service.constants.SenseUrls;
import nl.sense_os.util.json.EncryptionHelper;


import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Class for giving feedback on state sensors in CommonSense. Used to 'teach' state sensors the
 * correct label for certain inputs.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class FeedbackManager {

    private static final String TAG = "FeedbackManager";
    private final Context context;

    public FeedbackManager(Context context) {
        this.context = context;
    }

    /**
     * Teach the state sensor the correct label for a given time period.
     * 
     * @param name
     *            Name of the state sensor
     * @param start
     *            Start timestamp
     * @param end
     *            End timestamp
     * @param label
     *            Correct label for this time period
     * @return true of the operation completed successfully
     * @throws IOException
     * @throws JSONException
     */
    public boolean giveFeedback(String name, long start, long end, String label)
            throws IOException, JSONException {

        // get sensor ID
        String stateSensorId = SenseApi.getSensorId(context, name, null, null, null);
        if (null == stateSensorId) {
            Log.w(TAG, "Cannot find the requested state sensor: " + name);
            return false;
        }

        // get connected sensors
        List<String> connectedIds = SenseApi.getConnectedSensors(context, stateSensorId);
        if (connectedIds.size() == 0) {
            Log.w(TAG, "No sensors are connected to the state sensor: " + name);
            return false;
        }
        String sourceSensorId = connectedIds.get(0);

        manualLearn(sourceSensorId, stateSensorId, start, end, label);

        return true;
    }

    private String manualLearn(String sourceSensorId, String stateSensorId, long start, long end,
            String label) throws IOException, JSONException {

        SharedPreferences authPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS,
                Context.MODE_PRIVATE);

        // create POST data
        NumberFormat format = new DecimalFormat("##########.###", new DecimalFormatSymbols(
                Locale.ENGLISH));

        JSONObject json = new JSONObject();
        json.put("start_date", format.format(start / 1000d));
        json.put("end_date", format.format(end / 1000d));
        json.put("class_label", label);

        // request fresh list of sensors for this device from CommonSense
        String cookie = authPrefs.getString(Auth.LOGIN_COOKIE, null);
        SharedPreferences prefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
                Context.MODE_PRIVATE);

        boolean encrypt_credential = prefs.getBoolean(Advanced.ENCRYPT_CREDENTIAL, false);
        if (encrypt_credential) {
            EncryptionHelper decryptor = new EncryptionHelper(context);
            try {
                cookie = decryptor.decrypt(cookie);
            } catch (EncryptionHelper.EncryptionHelperException e) {
                Log.w(TAG, "Error decrypting cookie. Assume data is not encrypted");
            }
        }

        boolean devMode = prefs.getBoolean(Advanced.DEV_MODE, false);
        if (devMode) {
            Log.i(TAG, "Using development server to get connected sensors");
        }
        String url = devMode ? SenseUrls.MANUAL_LEARN_DEV : SenseUrls.MANUAL_LEARN;
        url = url.replace("%1", sourceSensorId).replace("%2", stateSensorId);
        Map<String, String> response = SenseApi.request(context, url, json, cookie);

        String responseCode = response.get(SenseApi.RESPONSE_CODE);
        if (!"200".equals(responseCode)) {
            Log.w(TAG, "Failed to perform manual learn method! Response code: " + responseCode);
            throw new IOException("Incorrect response from CommonSense: " + responseCode);
        }

        // parse response and store the list
        JSONObject content = new JSONObject(response.get(SenseApi.RESPONSE_CONTENT));
        String msg = content.getString("msg");

        return msg;
    }
}
