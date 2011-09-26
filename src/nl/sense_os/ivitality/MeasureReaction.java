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
import android.widget.Toast;
import android.widget.ViewFlipper;

public class MeasureReaction extends Activity {

    /**
     * Task to flip the view to the red square, to trigger a reaction from the user.
     */
    private class FlipTask extends TimerTask {

        @Override
        public void run() {
            // Log.v(TAG, "Flip!");
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (count == NR_OF_TESTS - 1) {
                        flipper.setDisplayedChild(STARTLE);
                    } else {
                        flipper.setDisplayedChild(RED_SQUARE);
                    }
                    flipper.forceLayout();
                    startTime = System.currentTimeMillis();
                }
            });
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
    private long redTime;
    private long blueTime;
    private int count;
    private int[] times;
    private final Timer flipTimer = new Timer();
    private TimerTask flipTask;

    private void measureReaction() {

        if (count < times.length) {
            int reaction = (int) (System.currentTimeMillis() - startTime);

            // Log.v(TAG, "Reaction time: " + reaction + " ms");
            Toast.makeText(MeasureReaction.this, reaction + " ms", Toast.LENGTH_SHORT).show();

            times[count] = reaction;
            count++;

            if (count < NR_OF_TESTS) {
                startWaiting();
            } else {
                showDialog(0);
            }
        } else {
            Log.w(TAG, "Too many touch events received, ignoring extra entry.");
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return new Object[] { times, count };
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

        Object config = getLastNonConfigurationInstance();
        if (null == config) {
            flipper.setDisplayedChild(START_SCREEN);
        } else {
            times = (int[]) ((Object[]) config)[0];
            count = (Integer) ((Object[]) config)[1];
            startWaiting();
        }
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

    /**
     * Shows the first screen of the real test. This gives the user some time to figure out what is
     * happening. After some time, {@link #startWaiting()} is called to truly start the reaction
     * time.
     */
    private void startTest() {
        flipper.setDisplayedChild(BLUE_SQUARE);
        times = new int[NR_OF_TESTS];
        count = 0;

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

    /**
     * Receives all touch events for this application. Should be slightly quicker than the touch
     * listeners for the individual views.
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (flipper.getDisplayedChild()) {
        case RED_SQUARE:
            // fall through to STARTLE
        case STARTLE:
            measureReaction();
            return true;
        case BLUE_SQUARE:
            long now = System.currentTimeMillis();
            if (now < redTime && now > blueTime + 300) {
                onCheat();
                return true;
            }
        default:
            return super.dispatchTouchEvent(ev);
        }
    }

    private void onCheat() {
        // Log.v(TAG, "CHEATER CHEATER CHEATER");
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(MeasureReaction.this, R.string.reaction_cheat_warning,
                        Toast.LENGTH_SHORT).show();
                startWaiting();
            }
        });
    }

    /**
     * Shows the normal screen with the blue square. After a random time, the square changes to the
     * red square or the startle screen, and the user can touch the screen to react.
     */
    private void startWaiting() {

        // schedule the task to trigger the reaction after a random amount of time
        if (null != flipTask) {
            flipTask.cancel();
        }
        flipTask = new FlipTask();
        long flipDelay = Math.round(3000 + 1000 * Math.random());
        flipTimer.schedule(flipTask, flipDelay);

        // show the blue screen
        flipper.setDisplayedChild(BLUE_SQUARE);
        blueTime = System.currentTimeMillis();
        redTime = blueTime + flipDelay;
    }
}
