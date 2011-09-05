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
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.ScrollView;
import android.widget.TextView;

public class MultipleChoicePopup extends Activity {

    @SuppressWarnings("unused")
    private static final String TAG = "MultipleChoicePopup";
    private String question;
    private String questionId;
    private String[] answers;

    private AlertDialog dialog;
    private RadioGroup answerFields;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        questionId = extras.getString("questionId");
        question = extras.getString("question");
        answers = extras.getStringArray("answers");
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

        answerFields = new RadioGroup(this);
        answerFields.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(checkedId >= 0);
            }
        });
        int answerId = 1;
        for (String answer : answers) {
            RadioButton answerField = new RadioButton(this);
            answerField.setText(answer);
            answerField.setId(answerId);
            answerId++;
            answerFields.addView(answerField);
        }
        layout.addView(answerFields);
        scrollView.addView(layout);

        builder.setView(scrollView);
        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                submit();
                setResult(RESULT_OK);
                MultipleChoicePopup.this.finish();
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
                MultipleChoicePopup.this.finish();
            }
        });
        dialog = builder.create();
        dialog.setOnShowListener(new OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {
                ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
            }
        });
        return dialog;
    }

    private void submit() {
        int selectionId = answerFields.getCheckedRadioButtonId();

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("question ID", questionId);
        map.put("question", question.replace("?", "-"));
        map.put("answer ID", selectionId);
        map.put("answer", answers[selectionId - 1]);
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
