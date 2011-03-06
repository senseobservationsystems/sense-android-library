/*
 * ***********************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 * **
 * ************************************************************************************************
 * *********
 */
package nl.sense_os.service.popquiz;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import nl.sense_os.service.Constants;

public class PopQuizSync extends BroadcastReceiver {

    private String TAG = "PopQuizSync";
    // private Context context;
    private SenseAlarmManager mgr;

    Thread syncThread = new Thread(new Runnable() {

        @Override
        public void run() {
            try {
                // SharedPreferences loginPrefs = context.getSharedPreferences(AUTHENTICATION_PREFS,
                // Context.MODE_PRIVATE);
                // String cookie = loginPrefs.getString(SenseSettings.PREF_LOGIN_COOKIE, "");
                // URI uri = new URI(SenseSettings.URL_QUIZ_GET_QSTNS);
                // HttpPost post = new HttpPost(uri);
                // post.setHeader("Cookie", cookie);
                // HttpClient client = new DefaultHttpClient();
                // client.getConnectionManager().closeIdleConnections(2, TimeUnit.SECONDS);
                // HttpResponse response = client.execute(post);
                //
                // // parse response, creating Quiz object
                // if (response != null) {
                // // create JSON object from response
                // int value = 0;
                // String body = "";
                // InputStream ir = response.getEntity().getContent();
                // while ((value = ir.read()) != -1) {
                // body += "" + (char) value;
                // }
                //
                // // quick hack to handle empty response with dummy quiz
                // if (body.length() == 0) {
                Log.w(TAG, "No response from CommonSense, inserting dummy quiz instead!");

                Quiz quiz = new Quiz();
                quiz.id = 1;
                quiz.description = "Quiz description";
                Question q1 = new Question();
                q1.id = 1;
                q1.value = "How are you feeling?";
                Answer a1 = new Answer();
                a1.id = 1;
                a1.value = "Good";
                Answer a2 = new Answer();
                a2.id = 2;
                a2.value = "Medium";
                Answer a3 = new Answer();
                a3.id = 3;
                a3.value = "Bad";

                q1.answers.add(a1);
                q1.answers.add(a2);
                q1.answers.add(a3);

                quiz.questions.add(q1);

                // store in database
                mgr.createQuiz(quiz);
                // } else {
                // JSONObject jsono = new JSONObject(body);
                //
                // // create Quiz object from JSON
                // // TODO: get quiz ID and description from CommonSense
                // Quiz quiz = new Quiz();
                // quiz.id = 1;
                // quiz.description = "Quiz description";
                // JSONArray questions = jsono.getJSONArray("questions");
                // for (int i = 0; i < questions.length(); i++) {
                // Question q = new Question(questions.getJSONObject(i));
                // quiz.questions.add(q);
                // }
                // Log.d(TAG, "Received new quiz:");
                // Log.d(TAG, quiz.toString());
                //
                // // store in database
                // mgr.createQuiz(quiz);
                // }
                // } else {
                // Log.w(TAG, "No response when updating quiz values");
                // }
            } catch (Exception e) {
                Log.e(TAG, "Exception updating quiz values", e);
            }
        }
    });

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Synchronizing quiz data with CommonSense...");

        // this.context = context;

        // save this sync time in the preferences
        SharedPreferences mainPrefs = context.getSharedPreferences(Constants.MAIN_PREFS,
                Context.MODE_WORLD_WRITEABLE);
        Editor editor = mainPrefs.edit();
        editor.putLong(Constants.PREF_QUIZ_SYNC_TIME, System.currentTimeMillis());
        editor.commit();

        // set next sync alarm
        mgr = new SenseAlarmManager(context);
        mgr.createSyncAlarm();

        syncThread.run();
    }
}
