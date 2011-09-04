package nl.sense_os.ivitality;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MeasurePressure extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        showDialog(0);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        final int top = (int) (80 + Math.round(50 * Math.random()));
        final int bottom = (int) (top - Math.round(40 * Math.random()));
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Blood pressure measurement");
        builder.setMessage("Your blood pressure is: " + top + " / " + bottom);
        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("systolic", top);
                map.put("diastolic", bottom);
                JSONObject json = new JSONObject(map);

                Intent intent = new Intent("nl.sense_os.app.MsgHandler.NEW_MSG");
                intent.putExtra("sensor_name", "blood pressure");
                intent.putExtra("sensor_device", "blood pressure emulator");
                intent.putExtra("timestamp", System.currentTimeMillis());
                intent.putExtra("data_type", "json");
                intent.putExtra("value", json.toString());
                startService(intent);

                setResult(RESULT_OK);
                MeasurePressure.this.finish();
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
                MeasurePressure.this.finish();
            }
        });
        return builder.create();
    }
}
