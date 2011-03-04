/*
 ***************************************************************************************************
 * Copyright (C)  2010 Sense Observation Systems, Rotterdam, the Netherlands.  All rights reserved.     *
 ***************************************************************************************************
 */
package nl.sense_os.app;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import nl.sense_os.service.Constants;
import nl.sense_os.service.DataTransmitter;
import nl.sense_os.service.ISenseService;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

public class SenseSettings extends PreferenceActivity {
    /**
     * AsyncTask to check the login data with CommonSense. Takes no arguments to execute. Clears any
     * open login dialogs before start, and displays a progress dialog during operation. If the
     * check fails, the login dialog is shown again.
     */
    private class CheckLoginTask extends AsyncTask<Void, Void, Boolean> {
        private static final String TAG = "CheckLoginTask";

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean success = false;
            if (SenseSettings.this.service != null) {
                try {
                    success = SenseSettings.this.service.changeLogin();
                } catch (final RemoteException e) {
                    e.printStackTrace();
                }
            } else {
                Log.d(TAG, "Service not bound. Skipping login task.");
            }
            return success;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            try {
                dismissDialog(DIALOG_PROGRESS);
            } catch (final IllegalArgumentException e) {
                // do nothing
            }
            if (result != true) {
                Toast.makeText(SenseSettings.this, R.string.toast_login_fail, Toast.LENGTH_LONG)
                        .show();
                showDialog(DIALOG_LOGIN);
            } else {
                Toast.makeText(SenseSettings.this, R.string.toast_login_ok, Toast.LENGTH_LONG)
                        .show();

                setupLoginPref();

                final Intent serviceIntent = new Intent(ISenseService.class.getName());
                if (null == startService(serviceIntent)) {
                    Log.w(TAG, "Could not start Sense service after login!");
                }
            }
        }

        @Override
        protected void onPreExecute() {

            // close the login dialog before showing the progress dialog
            try {
                dismissDialog(DIALOG_LOGIN);
            } catch (final IllegalArgumentException e) {
                e.printStackTrace();
            }

            showDialog(DIALOG_PROGRESS);

            super.onPreExecute();
        }
    }

    /**
     * AsyncTask to register a new phone/user with CommonSense. Takes no arguments to execute.
     * Clears any open login dialogs before start, and displays a progress dialog during operation.
     * If the check fails, the registration dialog is shown again.
     */
    private class CheckRegisterTask extends AsyncTask<Void, Void, Boolean> {
        private static final String TAG = "CheckRegisterTask";

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean success = false;
            if (SenseSettings.this.service != null) {
                try {
                    success = SenseSettings.this.service.serviceRegister();
                } catch (final RemoteException e) {
                    e.printStackTrace();
                }
            } else {
                Log.d(TAG, "Service not bound. Skipping registration task.");
            }
            return success;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            try {
                dismissDialog(DIALOG_PROGRESS);
            } catch (final IllegalArgumentException e) {
                // do nothing
            }

            if (result != true) {
                Toast.makeText(SenseSettings.this, getString(R.string.toast_reg_fail),
                        Toast.LENGTH_LONG).show();
                showDialog(DIALOG_REGISTER);
            } else {
                Toast.makeText(SenseSettings.this, getString(R.string.toast_reg_ok),
                        Toast.LENGTH_LONG).show();

                setupLoginPref();
            }
        }

        @Override
        protected void onPreExecute() {

            // close the login dialog before showing the progress dialog
            try {
                dismissDialog(DIALOG_REGISTER);
            } catch (final IllegalArgumentException e) {
                e.printStackTrace();
            }
            showDialog(DIALOG_PROGRESS);

            super.onPreExecute();
        }
    }

    private static final int DIALOG_LOGIN = 1;
    private static final int DIALOG_PROGRESS = 2;
    private static final int DIALOG_REGISTER = 3;
    private static final String TAG = "Sense Settings";

    private boolean isServiceBound;
    private ISenseService service = null;
    private final ServiceConnection serviceConn = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            SenseSettings.this.service = ISenseService.Stub.asInterface(binder);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            /* this is not called when the service is stopped, only when it is suddenly killed! */
            SenseSettings.this.service = null;
        }
    };

    private Dialog createDialogLogin() {

        // create View with input fields for dialog content
        final LinearLayout login = new LinearLayout(this);
        login.setOrientation(LinearLayout.VERTICAL);
        final EditText emailField = new EditText(this);
        emailField.setLayoutParams(new LayoutParams(-1, -2));
        emailField.setHint(R.string.dialog_login_hint_mail);
        emailField.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailField.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        login.addView(emailField);
        final EditText passField = new EditText(this);
        passField.setLayoutParams(new LayoutParams(-1, -2));
        passField.setHint(R.string.dialog_login_hint_pass);
        passField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passField.setTransformationMethod(new PasswordTransformationMethod());
        passField.setImeOptions(EditorInfo.IME_ACTION_DONE);
        login.addView(passField);

        // get current login email from preferences
        final SharedPreferences prefs = getSharedPreferences(Constants.PRIVATE_PREFS, MODE_PRIVATE);
        emailField.setText(prefs.getString(Constants.PREF_LOGIN_MAIL, ""));

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_login_title);
        builder.setView(login);
        builder.setPositiveButton(R.string.button_login, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String name = emailField.getText().toString();
                final String pass = passField.getText().toString();

                final Editor editor = prefs.edit();
                editor.putString(Constants.PREF_LOGIN_MAIL, name);
                // put md5 string
                String MD5Pass = "";
                // Register to the Database
                final byte[] defaultBytes = pass.getBytes();
                try {
                    final MessageDigest algorithm = MessageDigest.getInstance("MD5");
                    algorithm.reset();
                    algorithm.update(defaultBytes);
                    final byte messageDigest[] = algorithm.digest();

                    final StringBuffer hexString = new StringBuffer();
                    for (final byte element : messageDigest) {
                        final String hex = Integer.toHexString(0xFF & element);
                        if (hex.length() == 1) {
                            hexString.append(0);
                        }
                        hexString.append(hex);
                    }
                    MD5Pass = hexString.toString();
                } catch (final NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                editor.putString(Constants.PREF_LOGIN_PASS, MD5Pass);
                editor.commit();

                // initiate Login
                new CheckLoginTask().execute();
            }
        });
        builder.setNeutralButton(R.string.button_cancel, null);
        return builder.create();
    }

    private Dialog createDialogLoginProgress() {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setTitle(R.string.dialog_progress_title);
        dialog.setMessage(getString(R.string.dialog_progress_login_msg));
        dialog.setCancelable(false);
        return dialog;
    }

    private Dialog createDialogRegister() {
        // start the service if it was not running already
        if (this.service == null) {
            final Intent serviceIntent = new Intent(ISenseService.class.getName());
            bindService(serviceIntent, this.serviceConn, BIND_AUTO_CREATE);
        }

        // create individual input fields
        final EditText emailField = new EditText(this);
        emailField.setLayoutParams(new LayoutParams(-1, -2));
        emailField.setHint(R.string.dialog_reg_hint_mail);
        emailField.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailField.setImeOptions(EditorInfo.IME_ACTION_NEXT);

        final EditText passField1 = new EditText(this);
        passField1.setLayoutParams(new LayoutParams(-1, -2));
        passField1.setHint(R.string.dialog_reg_hint_pass);
        passField1.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passField1.setTransformationMethod(new PasswordTransformationMethod());
        passField1.setImeOptions(EditorInfo.IME_ACTION_NEXT);

        final EditText passField2 = new EditText(this);
        passField2.setLayoutParams(new LayoutParams(-1, -2));
        passField2.setHint(R.string.dialog_reg_hint_pass2);
        passField2.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passField2.setTransformationMethod(new PasswordTransformationMethod());
        passField2.setImeOptions(EditorInfo.IME_ACTION_DONE);

        // create main dialog content View
        final LinearLayout register = new LinearLayout(this);
        register.setOrientation(LinearLayout.VERTICAL);
        register.addView(emailField);
        register.addView(passField1);
        register.addView(passField2);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_reg_title);
        builder.setView(register);
        builder.setPositiveButton(R.string.button_reg, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String email = emailField.getText().toString();
                final String pass1 = passField1.getText().toString();
                final String pass2 = passField2.getText().toString();

                if (pass1.equals(pass2)) {
                    String MD5Pass = "";
                    // Register to the Database
                    final byte[] defaultBytes = pass1.getBytes();
                    try {
                        final MessageDigest algorithm = MessageDigest.getInstance("MD5");
                        algorithm.reset();
                        algorithm.update(defaultBytes);
                        final byte messageDigest[] = algorithm.digest();

                        final StringBuffer hexString = new StringBuffer();
                        for (final byte element : messageDigest) {
                            final String hex = Integer.toHexString(0xFF & element);
                            if (hex.length() == 1) {
                                hexString.append(0);
                            }
                            hexString.append(hex);
                        }
                        MD5Pass = hexString.toString();

                        // store the login value
                        final SharedPreferences prefs = getSharedPreferences(
                                Constants.PRIVATE_PREFS, MODE_PRIVATE);
                        final Editor editor = prefs.edit();
                        editor.putString(Constants.PREF_LOGIN_MAIL, email);
                        editor.putString(Constants.PREF_LOGIN_PASS, MD5Pass);
                        editor.commit();
                        // start registration
                        new CheckRegisterTask().execute();
                    } catch (final NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(SenseSettings.this, R.string.toast_reg_pass, Toast.LENGTH_SHORT)
                            .show();

                    passField1.setText("");
                    passField2.setText("");
                    removeDialog(DIALOG_REGISTER);
                    showDialog(DIALOG_REGISTER);
                }
            }
        });
        builder.setNeutralButton(R.string.button_cancel, null);
        return builder.create();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        // show list preferences to show their choice in the summary
        showSummaries();

        // setup some preferences with custom dialogs
        setupLoginPref();
        setupRegisterPref();
        setupQuizPref();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;

        switch (id) {
            case DIALOG_LOGIN :
                dialog = createDialogLogin();
                break;
            case DIALOG_REGISTER :
                dialog = createDialogRegister();
                break;
            case DIALOG_PROGRESS :
                dialog = createDialogLoginProgress();
                break;
            default :
                dialog = super.onCreateDialog(id);
                break;
        }
        return dialog;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
            case DIALOG_LOGIN :
                bindToService();
                break;
            case DIALOG_REGISTER :
                bindToService();
                break;
            case DIALOG_PROGRESS :
                break;
            default :
                break;
        }
    }

    private void bindToService() {
        // start the service if it was not running already
        if (this.service == null) {
            final Intent serviceIntent = new Intent(ISenseService.class.getName());
            bindService(serviceIntent, this.serviceConn, BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (true == this.isServiceBound) {
            unbindService(this.serviceConn);
            this.service = null;
            this.isServiceBound = false;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this.prefChangeListener);
    }

    OnSharedPreferenceChangeListener prefChangeListener;

    @Override
    protected void onResume() {
        super.onResume();

        if (this.service == null) {
            // do not use BIND_AUTO_CREATE so the service is only bound if it already exists
            final Intent serviceIntent = new Intent(ISenseService.class.getName());
            this.isServiceBound = bindService(serviceIntent, this.serviceConn, 0);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        this.prefChangeListener = new OnSharedPreferenceChangeListener() {

            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

                if (key.equals(Constants.PREF_SAMPLE_RATE)) {
                    if (isServiceBound) {
                        // stop service
                        final boolean stopped = stopService(new Intent(
                                ISenseService.class.getName()));
                        if (stopped) {
                            unbindService(SenseSettings.this.serviceConn);
                            SenseSettings.this.service = null;
                            SenseSettings.this.isServiceBound = false;
                        } else {
                            Log.w(TAG, "Service was not stopped.");
                        }

                        // restart service
                        final Intent serviceIntent = new Intent(ISenseService.class.getName());
                        if (null == startService(serviceIntent)) {
                            Log.w(TAG, "Could not start Sense service!");
                        }

                        SenseSettings.this.isServiceBound = bindService(serviceIntent,
                                SenseSettings.this.serviceConn, 0);
                    }
                }
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(this.prefChangeListener);
    }

    private void onSampleRateChange(Preference pref, String newValue) {
        switch (Integer.parseInt(newValue)) {
            case -2 : // real time
                pref.setSummary("Current setting: Real-time");
                break;
            case -1 : // often
                pref.setSummary("Current setting: Often");
                break;
            case 0 : // normal
                pref.setSummary("Current setting: Normal");
                break;
            case 1 : // rarely
                pref.setSummary("Current setting: Rarely");
                break;
            default :
                pref.setSummary("ERROR");
        }

    }

    private void onSyncRateChange(Preference pref, String newValue) {
        switch (Integer.parseInt(newValue)) {
            case -2 : // real time
                pref.setSummary("Real-time connection with CommonSense");
                break;
            case -1 : // often
                pref.setSummary("Sync with CommonSense every 5 secs");
                break;
            case 0 : // normal
                pref.setSummary("Sync with CommonSense every minute");
                break;
            case 1 : // rarely
                pref.setSummary("Sync with CommonSense every hour (Eco-mode)");
                break;
            default :
                pref.setSummary("ERROR");
        }

        // re-set sync alarm
        Intent alarm = new Intent(this, DataTransmitter.class);
        PendingIntent operation = PendingIntent.getBroadcast(this, DataTransmitter.REQID, alarm, 0);
        AlarmManager mgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        mgr.cancel(operation);
        mgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), operation);
    }

    private void setupLoginPref() {
        final Preference loginPref = findPreference(Constants.PREF_LOGIN);
        loginPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                showDialog(DIALOG_LOGIN);
                return true;
            }
        });
        final SharedPreferences loginPrefs = getSharedPreferences(Constants.PRIVATE_PREFS,
                MODE_PRIVATE);
        final String email = loginPrefs.getString(Constants.PREF_LOGIN_MAIL, "");
        final String summary = email.length() > 0 ? email : "Enter your login details";
        loginPref.setSummary(summary);
    }

    private void setupQuizPref() {
        final Preference popQuizRefresh = findPreference(Constants.PREF_QUIZ_SYNC);
        popQuizRefresh.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                // start quiz sync broadcast
                final Intent refreshIntent = new Intent(
                        "nl.sense_os.service.AlarmPopQuestionUpdate");
                final PendingIntent refreshPI = PendingIntent.getBroadcast(SenseSettings.this, 0,
                        refreshIntent, 0);
                final AlarmManager mgr = (AlarmManager) getSystemService(ALARM_SERVICE);
                mgr.set(AlarmManager.RTC_WAKEUP, 0, refreshPI);

                // show confirmation Toast
                Toast.makeText(SenseSettings.this, R.string.toast_quiz_refresh, Toast.LENGTH_LONG)
                        .show();

                return true;
            }
        });
    }

    private void setupRegisterPref() {
        final Preference registerPref = findPreference(Constants.PREF_REGISTER);
        registerPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                showDialog(DIALOG_REGISTER);
                return true;
            }
        });
    }

    /**
     * Shows the summaries of the two sync/sense rate list preferences.
     */
    private void showSummaries() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        final Preference samplePref = findPreference(Constants.PREF_SAMPLE_RATE);
        onSampleRateChange(samplePref, prefs.getString(Constants.PREF_SAMPLE_RATE, "0"));
        samplePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                onSampleRateChange(preference, (String) newValue);
                return true;
            }
        });

        final Preference syncPref = findPreference(Constants.PREF_SYNC_RATE);
        onSyncRateChange(syncPref, prefs.getString(Constants.PREF_SYNC_RATE, "0"));
        syncPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                onSyncRateChange(preference, (String) newValue);
                return true;
            }
        });

    }
}
