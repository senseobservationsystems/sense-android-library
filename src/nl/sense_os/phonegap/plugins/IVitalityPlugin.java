package nl.sense_os.phonegap.plugins;

import org.json.JSONArray;

import android.util.Log;

import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;

public class IVitalityPlugin extends AbstractSensePlugin {

    private static class Actions {
        static final String BIND = "bind";
        static final String CHECK_SENSE_STATUS = "check_sense_status";
        static final String MEASURE_PRESSURE = "measure_pressure";
        static final String MEASURE_REACTION = "measure_reaction";
        static final String SHOW_QUESTION = "show_question";
    }

    private static final String TAG = "PhoneGap iVitality";

    private PluginResult checkSenseStatus(JSONArray data, String callbackId) {
        Log.v(TAG, "Check Sense status");
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Executes the request and returns PluginResult.
     * 
     * @param action
     *            The action to execute.
     * @param args
     *            JSONArray of arguments for the plugin.
     * @param callbackId
     *            The callback id used when calling back into JavaScript.
     * @return A PluginResult object with a status and message.
     */
    @Override
    public PluginResult execute(String action, JSONArray data, String callbackId) {

        if (action == null) {
            Log.e(TAG, "Cannot execute action. action=" + action);
            return new PluginResult(Status.INVALID_ACTION);

        } else if (action.equals(Actions.BIND)) {
            bindToSenseService();
            return new PluginResult(Status.NO_RESULT);

        } else if (action.equals(Actions.CHECK_SENSE_STATUS)) {
            return checkSenseStatus(data, callbackId);

        } else if (action.equals(Actions.MEASURE_REACTION)) {
            measureReaction(data, callbackId);

        } else if (action.equals(Actions.MEASURE_PRESSURE)) {
            measurePressure(data, callbackId);

        } else if (action.equals(Actions.SHOW_QUESTION)) {
            showQuestion(data, callbackId);

        } else {
            Log.e(TAG, "Cannot execute action. action=" + action);
            return new PluginResult(Status.INVALID_ACTION);
        }

        return null;
    }

    /**
     * Identifies if action to be executed returns a value and should be run synchronously.
     * 
     * @param action
     *            The action to execute
     * @return true
     */
    @Override
    public boolean isSynch(String action) {
        if (action == null) {
            return false;

        } else if (action.equals(Actions.BIND)) {
            return true;

        } else if (action.equals(Actions.CHECK_SENSE_STATUS)) {
            return true;

        } else if (action.equals(Actions.MEASURE_REACTION)) {
            return false;

        } else if (action.equals(Actions.MEASURE_PRESSURE)) {
            return false;

        } else if (action.equals(Actions.SHOW_QUESTION)) {
            return false;

        }
        return true;
    }

    private void measurePressure(JSONArray data, String callbackId) {
        Log.v(TAG, "Measure pressure");

        // TODO Auto-generated method stub

    }

    private void measureReaction(JSONArray data, String callbackId) {
        Log.v(TAG, "Measure reaction");
        // TODO Auto-generated method stub

    }

    private void showQuestion(JSONArray data, String callbackId) {
        Log.v(TAG, "Show question");
        // TODO Auto-generated method stub

    }
}
