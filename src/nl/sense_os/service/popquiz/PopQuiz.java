/*
 * ***********************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 * **
 * ************************************************************************************************
 * *********
 */
package nl.sense_os.service.popquiz;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import nl.sense_os.service.Constants;
import nl.sense_os.service.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.Toast;

public class PopQuiz extends Activity {
    /**
     * Listener for clicks on the list with possible answers.
     */
    private class MyListListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // get tab activity label from the view
            Question q = PopQuiz.this.quiz.questions.get(PopQuiz.this.tabs.getCurrentTab());
            Answer a = q.answers.get((int) id);

            Log.d(TAG, "Question answered!");
            Log.d(TAG, q.id + ") " + q.value + "\n --> " + a.id + ": " + a.value);

            // save selection in preferences
            SharedPreferences prefs = PopQuiz.this.getSharedPreferences(
                    SenseAlarmManager.PREFS_LOCATION, Context.MODE_PRIVATE);
            Editor editor = prefs.edit();
            editor.putInt(PREF_QSTN_ID, q.id);
            editor.putInt(PREF_ANSW_ID, a.id);
            editor.putString(PREF_QSTN_VAL, q.value);
            editor.putString(PREF_ANSW_VAL, a.value);
            editor.commit();

            showDialog(DIALOG_CONFIRM);
        }
    }

    private static final int DIALOG_CONFIRM = 1;
    private static final int DIALOG_CONFIRM_CLOSE = 2;
    // private static final int DIALOG_LOGIN = 3;
    // private static final int DIALOG_LOGIN_PROGRESS = 4;
    private static final int DIALOG_MISSED = 5;
    private static final int DIALOG_WELCOME = 6;
    private static final int DIALOG_WELCOME_CATCHUP = 7;
    private static final int LED_NOTE_ID = 42;
    private static final int MENU_LOGIN = 1;
    private static final String TAG = "Sense PopQuiz";
    protected static final int MISSED_REQ = 1;
    protected static final String KEY_MISSED = "nl.sense_os.service.popquiz.Missed";
    protected static final String PREF_QSTN_ID = "nl.sense_os.question_id";
    protected static final String PREF_QSTN_VAL = "nl.sense_os.question_value";
    protected static final String PREF_ANSW_ID = "nl.sense_os.answer_id";
    protected static final String PREF_ANSW_VAL = "nl.sense_os.answer_value";
    private long entryId;
    private Quiz quiz;
    private boolean loggedIn;
    private ArrayList<Question> questions;
    private int selectedTab;
    private TabHost tabs;
    private boolean selectionOk;

    private Dialog createDialogConfirm() {
        // get registered activity and login name from preferences
        final SharedPreferences loginPrefs = getSharedPreferences(Constants.AUTH_PREFS,
                MODE_PRIVATE);
        String name = loginPrefs.getString("login_name", "ERROR");

        final SharedPreferences popPrefs = getSharedPreferences(SenseAlarmManager.PREFS_LOCATION,
                MODE_PRIVATE);
        String answVal = popPrefs.getString(PREF_ANSW_VAL, "ERROR");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm");
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setMessage("Register answer: " + answVal + " for " + name + "?");
        builder.setPositiveButton(R.string.button_ok, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // get tab id and activity id from preferences
                int qstnId = popPrefs.getInt(PREF_QSTN_ID, -1);
                long answId = popPrefs.getInt(PREF_ANSW_ID, -1);

                // save selection in database
                SenseAlarmManager mgr = new SenseAlarmManager(PopQuiz.this);
                boolean result = mgr.update(PopQuiz.this.entryId, qstnId, answId);

                // set result code
                if (true == result) {
                    // check whether we are processing missed alerts
                    if (PopQuiz.this.getIntent().getBooleanExtra(KEY_MISSED, false)) {
                        // get alarm entries that are not finished yet.
                        final long[] todos = mgr.readMissed();

                        // start activity to handle the next unfinished registration
                        if (todos.length > 0) {
                            final Intent missedPopQuiz = new Intent(
                                    "nl.sense_os.service.DoPeriodic");
                            missedPopQuiz.putExtra(SenseAlarmManager.KEY_QUIZ_ID, todos[0]);
                            missedPopQuiz.putExtra(KEY_MISSED, true);
                            startActivity(missedPopQuiz);
                        }
                    }
                } else {
                    Log.w(TAG, "Something went wrong during update of popquiz answer.");
                }

                finish();
            }
        });
        builder.setNegativeButton(R.string.button_cancel, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                removeDialog(DIALOG_CONFIRM);
            }
        });

        return builder.create();
    }

    private Dialog createDialogConfirmClose() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("No answer chosen.");
        builder.setMessage("Are you sure you don't want to answer anything right now? "
                + "\n\nUnanswered quizes have to be completed later on!");
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setPositiveButton("Close", new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                removeDialog(DIALOG_CONFIRM_CLOSE);
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        builder.setNegativeButton("Return", new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissDialog(DIALOG_CONFIRM_CLOSE);
            }
        });
        return builder.create();
    }

    private Dialog createDialogMissed() {

        // get alarm entries that are not finished yet.
        final SenseAlarmManager mgr = new SenseAlarmManager(PopQuiz.this);
        final long[] todos = mgr.readMissed();

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setTitle(R.string.pq_missed_title);
        String msg = getString(R.string.pq_missed_msg);
        builder.setMessage(msg.replace("?1", "" + (todos.length - 1)));
        builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                // start activity to handle the next unfinished registration
                if (todos.length > 0) {
                    final Intent missedPopQuiz = new Intent("nl.sense_os.service.DoPeriodic");
                    missedPopQuiz.putExtra(SenseAlarmManager.KEY_QUIZ_ID, todos[0]);
                    missedPopQuiz.putExtra(KEY_MISSED, true);
                    startActivityForResult(missedPopQuiz, MISSED_REQ);
                }

                dialog.dismiss();
            }
        });
        builder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.setCancelable(false);
        return builder.create();
    }

    /**
     * Creates the 'welcome' dialog, which explains the purpose of the activity. Used for the
     * default onCreateDialog() method.
     * 
     * @return the created dialog.
     */
    private Dialog createDialogWelcome(boolean catchUp) {
        // get alarm time from database
        String timeString = timeString(this.entryId);

        // get login name from preferences
        final SharedPreferences prefs = getSharedPreferences(Constants.AUTH_PREFS, MODE_PRIVATE);
        String nameString = prefs.getString("login_name", "ERROR");
        if (nameString.equals("ERROR")) {
            Log.e(TAG, "Cannot fetch name from preferences.");
            return null;
        }

        // build dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_popup_reminder);
        builder.setTitle(R.string.pq_welcome_title);
        String msgPart1 = getResources().getString(R.string.pq_welcome_msg1);
        String msgPart2 = getResources().getString(R.string.pq_welcome_msg2);
        if (true == catchUp) {
            builder.setMessage("Catching up:\n\n" + msgPart1 + " " + timeString + "\n" + msgPart2
                    + " " + nameString + ".");
        } else {
            builder.setMessage(msgPart1 + " " + timeString + "\n" + msgPart2 + " " + nameString
                    + ".");
        }
        builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                removeDialog(DIALOG_WELCOME);
            }
        });
        // builder.setNeutralButton("Change login", new OnClickListener() {
        //
        // public void onClick(DialogInterface dialog, int which) {
        // removeDialog(DIALOG_WELCOME);
        // // TODO: fix login changing in PopQuiz
        // // showDialog(DIALOG_LOGIN);
        // }
        // });
        builder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                removeDialog(DIALOG_WELCOME);
                showDialog(DIALOG_CONFIRM_CLOSE);
            }
        });
        builder.setCancelable(false);

        return builder.create();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == MISSED_REQ) {
            // get alarm entries that are not finished yet.
            final SenseAlarmManager mgr = new SenseAlarmManager(PopQuiz.this);
            final long[] todos = mgr.readMissed();

            // start activity to handle the next unfinished registration
            if (todos.length > 1) {
                final Intent missedPopQuiz = new Intent("nl.sense_os.service.DoPeriodic");
                missedPopQuiz.putExtra(SenseAlarmManager.KEY_QUIZ_ID, todos[0]);
                missedPopQuiz.putExtra(KEY_MISSED, true);
                startActivityForResult(missedPopQuiz, MISSED_REQ);
            } else {
                Toast.makeText(this, R.string.pq_missed_done, Toast.LENGTH_SHORT).show();
                showDialog(DIALOG_WELCOME);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.pq_main);

        final SenseAlarmManager mgr = new SenseAlarmManager(this);

        final Object retainedData = getLastNonConfigurationInstance();
        this.selectedTab = 0;
        this.loggedIn = false;
        this.selectionOk = false;
        if (null != retainedData) {
            Object[] data = (Object[]) retainedData;
            this.selectedTab = (Integer) data[0];
            this.loggedIn = (Boolean) data[1];
            this.selectionOk = (Boolean) data[2];
        } else {
            // get previously selected entry from AlarmManager
            final int[] lastSelected = mgr.readLatestEntry();
            this.selectedTab = lastSelected[0];
        }

        // get ID of the alarm entry to be handled
        this.entryId = getIntent().getLongExtra(SenseAlarmManager.KEY_ENTRY_ID, -1);
        int quizId = getIntent().getIntExtra(SenseAlarmManager.KEY_QUIZ_ID, -1);
        if ((this.entryId == -1) || (quizId == -1)) {
            Log.e(TAG, "Cannot find entry ID");
            finish();
        } else {
            this.quiz = mgr.readQuiz(quizId);

            setNotification(true);

            // if the catchUp key is set in the Extras, do not check for missed alerts
            final boolean catchUp = getIntent().getBooleanExtra(KEY_MISSED, false);
            if (true == catchUp) {
                showDialog(DIALOG_WELCOME_CATCHUP);
            } else {
                if (mgr.readMissed().length > 1) {
                    showDialog(DIALOG_MISSED);
                } else {
                    showDialog(DIALOG_WELCOME);
                }
            }

            populateTabs();
            // populateLists();
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;

        switch (id) {
            case DIALOG_CONFIRM :
                dialog = createDialogConfirm();
                break;
            case DIALOG_CONFIRM_CLOSE :
                dialog = createDialogConfirmClose();
                break;
            case DIALOG_MISSED :
                dialog = createDialogMissed();
                break;
            // case DIALOG_LOGIN:
            // dialog = createDialogLogin();
            // break;
            // case DIALOG_LOGIN_PROGRESS:
            // dialog = new ProgressDialog(this);
            // ((ProgressDialog) dialog).setIcon(R.drawable.icon);
            // dialog.setTitle("Een ogenblik geduld");
            // ((ProgressDialog) dialog).setMessage("Inloggegevens controleren...");
            // break;
            case DIALOG_WELCOME :
                dialog = createDialogWelcome(false);
                break;
            case DIALOG_WELCOME_CATCHUP :
                dialog = createDialogWelcome(true);
                break;
            default :
                dialog = null;
        }

        return dialog;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_LOGIN, Menu.NONE, "Inloggegevens");

        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK :
                if (false == this.selectionOk) {
                    showDialog(DIALOG_CONFIRM_CLOSE);
                    handled = true;
                }
                break;
        }
        return handled;
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        Object[] saveMe = {this.tabs.getCurrentTab(), this.loggedIn, this.selectionOk};
        return saveMe;
    }

    @Override
    protected void onStop() {
        setNotification(false);
        super.onStop();
    }

    @SuppressWarnings("unused")
    private void getQuestions(JSONObject obj) {
        // get list of questions
        this.questions = new ArrayList<Question>();
        try {
            JSONArray questions = obj.getJSONArray("questions");
            for (int i = 0; i < questions.length(); i++) {
                Question q = new Question(questions.getJSONObject(i));
                this.questions.add(q);
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSONException parsing the received question list:", e);
        }

        // do something with the questions
    }

    /**
     * Adds tabs to the TabHost. The tab labels are automatically shown in the TabWidget, which are
     * linked to specific content.
     */
    private void populateTabs() {
        this.tabs = (TabHost) findViewById(R.id.tabhost);
        this.tabs.setup();
        final OnItemClickListener listener = new MyListListener();

        for (final Question question : this.quiz.questions) {
            Log.d(TAG, "Displaying question " + question.id + ": " + question.value);

            TabSpec spec = this.tabs.newTabSpec("q" + question.id);
            spec.setContent(new TabHost.TabContentFactory() {

                @Override
                public View createTabContent(String tag) {
                    // -- this tab contains a single control - the listview -- //
                    ListView qList = new ListView(PopQuiz.this);
                    String[] answers = new String[question.answers.size()];
                    for (int i = 0; i < answers.length; i++) {
                        answers[i] = question.answers.get(i).value;
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(PopQuiz.this,
                            R.layout.pq_list_row, R.id.label, answers);
                    qList.setAdapter(adapter);
                    qList.setOnItemClickListener(listener);
                    return qList;
                }
            });
            spec.setIndicator(question.value);
            this.tabs.addTab(spec);
        }

        // select the right tab (if activity was already running)
        this.tabs.setCurrentTab(this.selectedTab);
    }

    /**
     * Sets the notification to alert the user that the activity requires input. Uses the default
     * alert settings for the sound, vibration and LED.
     * 
     * @param state
     *            set the notification state on/off.
     */
    private void setNotification(boolean state) {
        NotificationManager noteMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (true == state) {
            Notification notif = new Notification();
            notif.defaults = Notification.DEFAULT_ALL;
            noteMgr.notify(LED_NOTE_ID, notif);
        } else {
            noteMgr.cancel(LED_NOTE_ID);
        }
    }

    /**
     * @param time
     *            the length of time, in milliseconds.
     * @return the time period, formatted as hh:mm
     */
    private String timeString(long time) {
        return new SimpleDateFormat("HH:mm").format(new Date(time));
    }
}
