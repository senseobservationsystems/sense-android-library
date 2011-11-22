/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.popquiz;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import nl.sense_os.service.constants.SensePrefs;

public class PopQuizRx extends BroadcastReceiver {
    private static final String TAG = "PopQuizRx";

    @Override
    public void onReceive(Context context, Intent intent) {

        long entryId = intent.getLongExtra(SenseAlarmManager.KEY_ENTRY_ID, -2);
        int quizId = intent.getIntExtra(SenseAlarmManager.KEY_QUIZ_ID, -2);
        final SenseAlarmManager mgr = new SenseAlarmManager(context);

        if ((entryId < 0) || (quizId < 0)) {
            Log.w(TAG, "Something is wrong, canceling alarms... Entry ID: " + entryId
                    + ", quiz ID: " + quizId);

            // cancel the alarms
            mgr.cancelEntry();
        } else {
            // Log.v(TAG, "Received pop quiz alarm..." + " Quiz " + quizId + ", time "
            // + new SimpleDateFormat("HH:mm:ss").format(new Date(entryId)));

            // set the next alarm
            mgr.createEntry(0, quizId);

            // check whether to show the activity picker or not, depending on the silent mode
            // setting.
            final SharedPreferences mainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
                    Context.MODE_PRIVATE);
            final boolean silentMode = mainPrefs
                    .getBoolean(SensePrefs.Main.Quiz.SILENT_MODE, false);

            if (false == silentMode) {
                // Log.v(TAG, "Starting pop quiz activity...");

                final Intent popQuiz = new Intent("nl.sense_os.service.DoPeriodic");
                popQuiz.addCategory(Intent.CATEGORY_DEFAULT);
                popQuiz.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                popQuiz.putExtra(SenseAlarmManager.KEY_ENTRY_ID, entryId);
                popQuiz.putExtra(SenseAlarmManager.KEY_QUIZ_ID, quizId);
                // context.startActivity(popQuiz);
            } else {
                // ignore this alarm, silent mode is on
                // Log.v(TAG, "Ignored pop quiz alarm because of silent mode.");
            }
        }
    }
}
