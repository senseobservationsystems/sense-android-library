package nl.sense_os.ivitality;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

public class MultipleChoicePopup extends Activity {

    private String question;
    private String questionId;
    private String[] answers;

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
        String msg = questionId + ". " + question + "\n";
        for (String answer : answers) {
            msg += "\n - " + answer;
        }
        builder.setMessage(msg);
        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
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
        return builder.create();
    }
}
