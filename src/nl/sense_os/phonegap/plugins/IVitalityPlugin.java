package nl.sense_os.phonegap.plugins;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;

public class IVitalityPlugin extends Plugin {

    private static class Actions {
        static final String MEASURE_PRESSURE = "measure_pressure";
        static final String MEASURE_REACTION = "measure_reaction";
        static final String SHOW_MULTIPLE_CHOICE = "show_multiple_choice";
        static final String SHOW_SLIDER_QUESTION = "show_slider_question";
    }

    private static class Callbacks {
        static final int PRESSURE = 0x078E5508E;
        static final int REACTION = 0x08EAC7;
        static final int MULTIPLE_CHOICE = 0x0C401CE;
        static final int SLIDER_QUESTION = 0x0571DE8;
    }

    private static final String TAG = "PhoneGap iVitality";
    private String callbackId;

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
            if (Actions.MEASURE_REACTION.equals(action)) {
                return measureReaction(data, callbackId);
            } else if (Actions.MEASURE_PRESSURE.equals(action)) {
                return measurePressure(data, callbackId);
            } else if (Actions.SHOW_MULTIPLE_CHOICE.equals(action)) {
                return showMultipleChoice(data, callbackId);
            } else if (Actions.SHOW_SLIDER_QUESTION.equals(action)) {
                return showSliderQuestion(data, callbackId);
            } else {
                Log.e(TAG, "Invalid action: " + action);
                return new PluginResult(Status.INVALID_ACTION);
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSONException getting arguments for '" + action + "'", e);
            return new PluginResult(Status.JSON_EXCEPTION, e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while executing '" + action + "'", e);
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
        if (Actions.MEASURE_REACTION.equals(action)) {
            return false;
        } else if (Actions.MEASURE_PRESSURE.equals(action)) {
            return false;
        } else if (Actions.SHOW_MULTIPLE_CHOICE.equals(action)) {
            return false;
        } else if (Actions.SHOW_SLIDER_QUESTION.equals(action)) {
            return false;
        } else {
            Log.w(TAG, "Unexpected action: " + action + ". Revert to default isSynch");
            return super.isSynch(action);
        }
    }

    private PluginResult measurePressure(JSONArray data, String callbackId) {
        Log.v(TAG, "Measure pressure: " + callbackId);
        this.callbackId = callbackId;
        Intent measure = new Intent("nl.sense_os.ivitality.MeasurePressure");
        ctx.startActivityForResult(this, measure, Callbacks.PRESSURE);

        PluginResult r = new PluginResult(Status.NO_RESULT);
        r.setKeepCallback(true);
        return r;
    }

    private PluginResult measureReaction(JSONArray data, String callbackId) {
        Log.v(TAG, "Measure reaction: " + callbackId);
        this.callbackId = callbackId;
        Intent measure = new Intent("nl.sense_os.ivitality.MeasureReaction");
        ctx.startActivityForResult(this, measure, Callbacks.REACTION);

        PluginResult r = new PluginResult(Status.NO_RESULT);
        r.setKeepCallback(true);
        return r;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == Activity.RESULT_OK) {
            String value = null != intent ? intent.getStringExtra("result") : "OK";
            success(new PluginResult(Status.OK, value), callbackId);
        } else if (resultCode == Activity.RESULT_CANCELED) {
            error(new PluginResult(Status.ERROR, "canceled"), callbackId);
        } else {
            error(new PluginResult(Status.ERROR, "error"), callbackId);
        }
    }

    private PluginResult showMultipleChoice(JSONArray data, String callbackId) throws JSONException {
        Log.v(TAG, "Show multiple choice");

        // get arguments
        String question = null, questionId = null;
        String[] answers = null;
        questionId = data.getString(0);
        question = data.getString(1);
        JSONArray rawAnswers = data.getJSONArray(2);
        answers = new String[rawAnswers.length()];
        for (int i = 0; i < rawAnswers.length(); i++) {
            answers[i] = rawAnswers.getString(i);
        }

        Intent show = new Intent("nl.sense_os.ivitality.ShowMultipleChoice");
        show.putExtra("questionId", questionId);
        show.putExtra("question", question);
        show.putExtra("answers", answers);
        ctx.startActivityForResult(this, show, Callbacks.MULTIPLE_CHOICE);
        this.callbackId = callbackId;

        PluginResult r = new PluginResult(Status.NO_RESULT);
        r.setKeepCallback(true);
        return r;
    }

    private PluginResult showSliderQuestion(JSONArray data, String callbackId) throws JSONException {
        Log.v(TAG, "Show slider question");

        // get arguments
        String question = null, questionId = null;
        int sliderMin = -1, sliderMax = -1;
        double step = -1;
        questionId = data.getString(0);
        question = data.getString(1);
        sliderMin = data.getInt(2);
        sliderMax = data.getInt(3);
        step = data.getDouble(4);

        Intent show = new Intent("nl.sense_os.ivitality.ShowSliderQuestion");
        show.putExtra("questionId", questionId);
        show.putExtra("question", question);
        show.putExtra("sliderMin", sliderMin);
        show.putExtra("sliderMax", sliderMax);
        show.putExtra("step", step);
        ctx.startActivityForResult(this, show, Callbacks.SLIDER_QUESTION);
        this.callbackId = callbackId;

        PluginResult r = new PluginResult(Status.NO_RESULT);
        r.setKeepCallback(true);
        return r;
    }
}
