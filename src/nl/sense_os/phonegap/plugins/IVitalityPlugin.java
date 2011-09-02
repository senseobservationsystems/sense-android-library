package nl.sense_os.phonegap.plugins;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Intent;
import android.util.Log;

import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;

public class IVitalityPlugin extends AbstractSensePlugin {

    private static class Actions {
        static final String BIND = "bind";
        static final String CHECK_SENSE_STATUS = "check_sense_status";
        static final String MEASURE_PRESSURE = "measure_pressure";
        static final String MEASURE_REACTION = "measure_reaction";
        static final String SHOW_MULTIPLE_CHOICE = "show_multiple_choice";
        static final String SHOW_SLIDER_QUESTION = "show_slider_question";
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
        try {
            if (action == null) {
                Log.e(TAG, "Invalid action: " + action);
                return new PluginResult(Status.INVALID_ACTION);
            } else if (action.equals(Actions.BIND)) {
                bindToSenseService();
                return new PluginResult(Status.NO_RESULT);
            } else if (action.equals(Actions.CHECK_SENSE_STATUS)) {
                return checkSenseStatus(data, callbackId);
            } else if (action.equals(Actions.MEASURE_REACTION)) {
                measureReaction(data, callbackId);
                return null;
            } else if (action.equals(Actions.MEASURE_PRESSURE)) {
                measurePressure(data, callbackId);
                return null;
            } else if (action.equals(Actions.SHOW_MULTIPLE_CHOICE)) {
                showMultipleChoice(data, callbackId);
                return null;
            } else if (action.equals(Actions.SHOW_SLIDER_QUESTION)) {
                showSliderQuestion(data, callbackId);
                return null;
            } else {
                Log.e(TAG, "Invalid action: " + action);
                return new PluginResult(Status.INVALID_ACTION);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while executing action: " + action, e);
            return new PluginResult(Status.ERROR, e.getMessage());
        }
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
        } else if (action.equals(Actions.MEASURE_REACTION)
                || action.equals(Actions.MEASURE_PRESSURE)) {
            return false;
        } else if (action.equals(Actions.SHOW_MULTIPLE_CHOICE)
                || action.equals(Actions.SHOW_SLIDER_QUESTION)) {
            return false;
        } else {
            return false;
        }
    }

    private void measurePressure(JSONArray data, String callbackId) {
        Log.v(TAG, "Measure pressure");
        Intent measure = new Intent("nl.sense_os.ivitality.MeasurePressure");
        ctx.startActivity(measure);
    }

    private void measureReaction(JSONArray data, String callbackId) {
        Log.v(TAG, "Measure reaction");
        Intent measure = new Intent("nl.sense_os.ivitality.MeasureReaction");
        ctx.startActivity(measure);
    }

    private void showMultipleChoice(JSONArray data, String callbackId) {
        Log.v(TAG, "Show multiple choice");

        // get arguments
        String question = null, questionId = null;
        String[] answers = null;
        try {
            questionId = data.getString(0);
            question = data.getString(1);
            JSONArray rawAnswers = data.getJSONArray(2);
            answers = new String[rawAnswers.length()];
            for (int i = 0; i < rawAnswers.length(); i++) {
                answers[i] = rawAnswers.getString(i);
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSONException getting multiple choice arguments", e);
            error(new PluginResult(Status.JSON_EXCEPTION, e.getMessage()), callbackId);
            return;
        }

        Intent show = new Intent("nl.sense_os.ivitality.ShowMultipleChoice");
        show.putExtra("questionId", questionId);
        show.putExtra("question", question);
        show.putExtra("answers", answers);
        ctx.startActivity(show);
    }

    private void showSliderQuestion(JSONArray data, String callbackId) {
        Log.v(TAG, "Show slider question");

        // get arguments
        String question = null, questionId = null;
        int sliderMin = -1, sliderMax = -1, step = -1;
        try {
            questionId = data.getString(0);
            question = data.getString(1);
            sliderMin = data.getInt(2);
            sliderMax = data.getInt(3);
            step = data.getInt(4);
        } catch (JSONException e) {
            Log.e(TAG, "JSONException getting multiple choice arguments", e);
            error(new PluginResult(Status.JSON_EXCEPTION, e.getMessage()), callbackId);
            return;
        }

        Intent show = new Intent("nl.sense_os.ivitality.ShowSliderQuestion");
        show.putExtra("questionId", questionId);
        show.putExtra("question", question);
        show.putExtra("sliderMin", sliderMin);
        show.putExtra("sliderMax", sliderMax);
        show.putExtra("step", step);
        ctx.startActivity(show);
    }
}
