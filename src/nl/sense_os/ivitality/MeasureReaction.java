package nl.sense_os.ivitality;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;

public class MeasureReaction extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        showDialog(0);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        final int top = (int) (241 + Math.round(150 * Math.random()));
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reaction time measurement");
        builder.setMessage("Your reaction time is: " + top + " ms");
        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                Intent intent = new Intent("nl.sense_os.app.MsgHandler.NEW_MSG");
                intent.putExtra("sensor_name", "reaction time");
                intent.putExtra("sensor_device", "reaction time emulator");
                intent.putExtra("timestamp", System.currentTimeMillis());
                intent.putExtra("data_type", "int");
                intent.putExtra("value", top);
                startService(intent);

                setResult(RESULT_OK);
                MeasureReaction.this.finish();
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
                MeasureReaction.this.finish();
            }
        });
        return builder.create();
    }
}
