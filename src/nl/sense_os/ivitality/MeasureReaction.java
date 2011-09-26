package nl.sense_os.ivitality;

import java.util.Timer;
import java.util.TimerTask;

import nl.sense_os.phonegap.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class MeasureReaction extends Activity {

    private class ReactionListener implements OnTouchListener {

        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            if (enabled) {
                measureReaction();
                setEnabled(false);
            }

            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    private static final String TAG = "Reaction Measurement";
    private static final int NR_OF_TESTS = 5;
    private static final int START_SCREEN = 0;
    private static final int BLUE_SQUARE = 1;
    private static final int RED_SQUARE = 2;
    private static final int STARTLE = 3;
    private ViewFlipper flipper;
    private long startTime;
    private int count;
    private int[] times;
    private final ReactionListener touchListener = new ReactionListener();

    private void measureReaction() {

        int reaction = (int) (System.currentTimeMillis() - startTime);

        Log.v(TAG, "Reaction time: " + reaction + " ms");
        Toast.makeText(MeasureReaction.this, reaction + " ms", Toast.LENGTH_SHORT).show();

        times[count] = reaction;
        count++;

        if (count < NR_OF_TESTS) {
            startWaiting();
        } else {
            showDialog(0);
        }
    }

    public void onButtonClick(View v) {
        switch (v.getId()) {
        case R.id.reaction_btn_start:
            startTest();
            break;
        case R.id.reaction_btn_cancel:
            setResult(RESULT_CANCELED);
            finish();
            break;
        default:
            Log.e(TAG, "Unexpected button pressed: " + v);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reaction_game);

        flipper = (ViewFlipper) findViewById(R.id.reaction_flipper);
        flipper.setDisplayedChild(START_SCREEN);

        flipper.getChildAt(RED_SQUARE).setOnTouchListener(touchListener);
        flipper.getChildAt(STARTLE).setOnTouchListener(touchListener);
    }

    @Override
    protected Dialog onCreateDialog(int id) {

        StringBuilder b = new StringBuilder("De reactietijden waren:\n\n");
        int total = 0;
        for (int i = 0; i < times.length; i++) {
            total += times[i];
            b.append((i + 1) + ". " + times[i] + " ms\n");
        }
        final int average = Math.round(total / times.length);
        b.append("\nDe gemiddelde tijd is: " + average + " ms.");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Test voltooid");
        builder.setMessage(b);
        builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                Intent intent = new Intent("nl.sense_os.app.MsgHandler.NEW_MSG");
                intent.putExtra("sensor_name", "reaction time");
                intent.putExtra("sensor_device", "reaction time emulator");
                intent.putExtra("timestamp", System.currentTimeMillis());
                intent.putExtra("data_type", "int");
                intent.putExtra("value", average);
                startService(intent);

                setResult(RESULT_OK);
                MeasureReaction.this.finish();
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

    public void onReactionClick(View v) {
        switch (v.getId()) {
        case R.id.reaction_view_waiting:
            Log.d(TAG, "CHEATER!!");
            break;
        case R.id.reaction_view_active:
            // measureReaction();
            break;
        default:
            Log.e(TAG, "Unexpected view clicked for reaction: " + v);
        }
    }

    private void startTest() {
        flipper.setDisplayedChild(BLUE_SQUARE);
        times = new int[NR_OF_TESTS];

        new Timer().schedule(new TimerTask() {

            @Override
            public void run() {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        startWaiting();
                    }
                });
            }
        }, 2000);
    }

    private void startWaiting() {
        flipper.setDisplayedChild(BLUE_SQUARE);

        new Timer().schedule(new TimerTask() {

            @Override
            public void run() {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        if (count == NR_OF_TESTS - 1) {
                            flipper.setDisplayedChild(STARTLE);
                        } else {
                            flipper.setDisplayedChild(RED_SQUARE);
                        }
                        startTime = System.currentTimeMillis();
                        touchListener.setEnabled(true);
                    }
                });
            }
        }, Math.round(3000 + 1000 * Math.random()));
    }
}
