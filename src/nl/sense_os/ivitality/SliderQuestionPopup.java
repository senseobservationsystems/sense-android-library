package nl.sense_os.ivitality;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class SliderQuestionPopup extends Activity {

    @SuppressWarnings("unused")
    private static final String TAG = "SliderQuestionPopup";
    private String question;
    private String questionId;
    private int min;
    private int max;
    private double step;

    private SeekBar slider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        questionId = extras.getString("questionId");
        question = extras.getString("question");
        min = extras.getInt("min", 0);
        max = extras.getInt("max", 10);
        step = extras.getDouble("step", 1);
        showDialog(0);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("iVitality Question");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        TextView q = new TextView(this);
        q.setText(question);
        q.setPadding(20, 5, 5, 5);
        layout.addView(q);

        final TextView sliderSummary = new TextView(this);
        sliderSummary.setText("" + min);
        sliderSummary.setPadding(20, 25, 20, 5);

        slider = new SeekBar(this);
        slider.setPadding(20, 5, 20, 5);
        slider.setMax((int) Math.round((max - min) / step));
        slider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // do nothing
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // do nothing
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sliderSummary.setText("" + getSliderValue());
            }
        });

        layout.addView(sliderSummary);
        layout.addView(slider);
        scrollView.addView(layout);

        builder.setView(scrollView);

        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                setResult(RESULT_OK);
                submit();
                SliderQuestionPopup.this.finish();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                setResult(RESULT_CANCELED);
                SliderQuestionPopup.this.finish();
            }
        });
        return builder.create();
    }

    private double getSliderValue() {
        return (slider.getProgress() * step + min);
    }

    private void submit() {

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("question ID", questionId);
        map.put("question", question.replace("?", "-"));
        map.put("answer ID", -1);
        map.put("answer", getSliderValue());
        JSONObject json = new JSONObject(map);

        Intent intent = new Intent("nl.sense_os.app.MsgHandler.NEW_MSG");
        intent.putExtra("sensor_name", "ivitality question");
        intent.putExtra("sensor_device", "ivitality question");
        intent.putExtra("timestamp", System.currentTimeMillis());
        intent.putExtra("data_type", "json");
        intent.putExtra("value", json.toString());
        startService(intent);
    }
}
