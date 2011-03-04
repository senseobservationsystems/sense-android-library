/*
 **************************************************************************************************
 * Copyright (C)  2010 Sense Observation Systems, Rotterdam, the Netherlands.  All rights reserved.
 **************************************************************************************************
 */

package nl.sense_os.app;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import nl.sense_os.service.Constants;
import nl.sense_os.service.ISenseService;
import nl.sense_os.service.ISenseServiceCallback;
import nl.sense_os.service.SenseService;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SenseApp extends Activity {
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
            if (SenseApp.this.service != null) {
                try {
                    success = SenseApp.this.service.changeLogin();
                } catch (final RemoteException e) {
                    Log.e(TAG, "RemoteException checking login", e);
                }
            } else {
                Log.e(TAG, "Service not bound. Skipping login task.");
            }
            return success;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            try {
                dismissDialog(DIALOG_PROGRESS);
            } catch (final IllegalArgumentException e) {
                // do nothing, perhaps the progress dialog was already dismissed
            }
            if (result != true) {
                Toast.makeText(SenseApp.this, R.string.toast_login_fail, Toast.LENGTH_LONG).show();
                showDialog(DIALOG_LOGIN);
            } else {
                Toast.makeText(SenseApp.this, R.string.toast_login_ok, Toast.LENGTH_LONG).show();

                // at least turn on phone state sensor after the very first login
                toggleService(true);
                togglePhoneState(true);
                updateUi();
            }
        }

        @Override
        protected void onPreExecute() {
            // close the login dialog before showing the progress dialog
            try {
                dismissDialog(DIALOG_LOGIN);
            } catch (final IllegalArgumentException e) {
                // no problem: login dialog was not displayed
            }

            showDialog(DIALOG_PROGRESS);
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
            if (SenseApp.this.service != null) {
                try {
                    success = SenseApp.this.service.serviceRegister();
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
                Toast.makeText(SenseApp.this, R.string.toast_reg_fail, Toast.LENGTH_LONG).show();
                showDialog(DIALOG_REGISTER);
            } else {
                Toast.makeText(SenseApp.this, R.string.toast_reg_ok, Toast.LENGTH_LONG).show();
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
        }
    }

    /**
     * Service stub for callbacks from the Sense service.
     */
    private class SenseCallback extends ISenseServiceCallback.Stub {

        @Override
        public void statusReport(final int status) {

            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    final boolean running = ((status & Constants.STATUSCODE_RUNNING) > 0);
                    final boolean connected = ((status & Constants.STATUSCODE_CONNECTED) > 0);
                    ((CheckBox) findViewById(R.id.main_cb)).setChecked(running);

                    // show connection status in main service field
                    TextView mainFirstLine = (TextView) findViewById(R.id.main_firstline);
                    if (connected) {
                        mainFirstLine.setText("Sense service");
                    } else {
                        mainFirstLine.setText("Sense service (not logged in)");
                    }

                    // change description of main service field
                    TextView mainDescription = (TextView) findViewById(R.id.main_secondLine);
                    if (running) {
                        mainDescription.setText("Press to stop Sense service completely");
                    } else {
                        mainDescription.setText("Press to start Sense service");
                    }

                    // enable phone state list row
                    CheckBox button = (CheckBox) findViewById(R.id.phonestate_cb);
                    final boolean callstate = ((status & Constants.STATUSCODE_PHONESTATE) > 0);
                    button.setChecked(callstate);
                    button.setEnabled(connected);
                    View text1 = findViewById(R.id.phonestate_firstline);
                    View text2 = findViewById(R.id.phonestate_secondLine);
                    text1.setEnabled(connected);
                    text2.setEnabled(connected);

                    // enable location list row
                    button = (CheckBox) findViewById(R.id.location_cb);
                    final boolean location = ((status & Constants.STATUSCODE_LOCATION) > 0);
                    button.setChecked(location);
                    button.setEnabled(connected);
                    button = (CheckBox) findViewById(R.id.ambience_cb);
                    text1 = findViewById(R.id.location_firstline);
                    text2 = findViewById(R.id.location_secondLine);
                    text1.setEnabled(connected);
                    text2.setEnabled(connected);

                    // enable noise list row
                    button = (CheckBox) findViewById(R.id.ambience_cb);
                    final boolean noise = ((status & Constants.STATUSCODE_AMBIENCE) > 0);
                    button.setChecked(noise);
                    button.setEnabled(connected);
                    button = (CheckBox) findViewById(R.id.ambience_cb);
                    text1 = findViewById(R.id.ambience_firstline);
                    text2 = findViewById(R.id.ambience_secondLine);
                    text1.setEnabled(connected);
                    text2.setEnabled(connected);

                    // enable pop quiz list row
                    button = (CheckBox) findViewById(R.id.popquiz_cb);
                    final boolean popQuiz = ((status & Constants.STATUSCODE_QUIZ) > 0);
                    button.setChecked(popQuiz);
                    button.setEnabled(false);
                    text1 = findViewById(R.id.popquiz_firstline);
                    text2 = findViewById(R.id.popquiz_secondLine);
                    text1.setEnabled(false);
                    text2.setEnabled(false);

                    // enable motion list row
                    button = (CheckBox) findViewById(R.id.motion_cb);
                    final boolean motion = ((status & Constants.STATUSCODE_MOTION) > 0);
                    button.setChecked(motion);
                    button.setEnabled(connected);
                    text1 = findViewById(R.id.motion_firstline);
                    text2 = findViewById(R.id.motion_secondLine);
                    text1.setEnabled(connected);
                    text2.setEnabled(connected);

                    // enable external sensor list row
                    button = (CheckBox) findViewById(R.id.external_sensor_cb);
                    final boolean external_sensors = ((status & Constants.STATUSCODE_EXTERNAL) > 0);
                    button.setChecked(external_sensors);
                    button.setEnabled(connected);
                    text1 = findViewById(R.id.external_sensor_firstline);
                    text2 = findViewById(R.id.external_sensor_secondLine);
                    text1.setEnabled(connected);
                    text2.setEnabled(connected);

                    // enable device proximity row
                    button = (CheckBox) findViewById(R.id.device_prox_cb);
                    final boolean deviceProx = ((status & Constants.STATUSCODE_DEVICE_PROX) > 0);
                    button.setChecked(deviceProx);
                    button.setEnabled(connected);
                    text1 = findViewById(R.id.device_prox_firstline);
                    text2 = findViewById(R.id.device_prox_secondLine);
                    text1.setEnabled(connected);
                    text2.setEnabled(connected);
                }
            });
        }
    };

    /**
     * Service connection to handle connection with the Sense service. Manages the
     * <code>service</code> field when the service is connected or disconnected.
     */
    private class SenseServiceConn implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {

            SenseApp.this.service = ISenseService.Stub.asInterface(binder);
            try {
                SenseApp.this.service.getStatus(SenseApp.this.callback);
            } catch (final RemoteException e) {
                Log.e(TAG, "Error checking service status after binding. ", e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            /* this is not called when the service is stopped, only when it is suddenly killed! */

            SenseApp.this.service = null;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateUi();
                }
            });
        }
    };

    /**
     * Receiver for broadcast events from the Sense Service, e.g. when the status of the service
     * changes.
     */
    private class SenseServiceListener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateUi();
                }
            });
        }
    }

    public static final int COMMONSENSE_VERSION = 3;
    private static final int DIALOG_FAQ = 1;
    private static final int DIALOG_HELP = 2;
    private static final int DIALOG_LOGIN = 3;
    private static final int DIALOG_PROGRESS = 4;
    private static final int DIALOG_REGISTER = 5;
    private static final int DIALOG_UPDATE_ALERT = 6;
    private static final int MENU_FAQ = 1;
    private static final int MENU_LOGIN = 2;
    private static final int MENU_REGISTER = 3;
    private static final int MENU_SETTINGS = 4;
    private static final String TAG = "SenseApp";
    private final ISenseServiceCallback callback = new SenseCallback();
    private boolean isServiceBound;
    private ISenseService service;
    private final ServiceConnection serviceConn = new SenseServiceConn();
    private final SenseServiceListener serviceListener = new SenseServiceListener();

    private Dialog createDialogFaq() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setTitle(R.string.dialog_faq_title);
        builder.setMessage(R.string.dialog_faq_msg);
        builder.setPositiveButton(R.string.button_ok, null);
        return builder.create();
    }

    /**
     * @return a help dialog, which explains the goal of Sense and clicks through to Registration or
     *         Login.
     */
    private Dialog createDialogHelp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setTitle(R.string.dialog_welcome_title);
        builder.setMessage(R.string.dialog_welcome_msg);
        builder.setPositiveButton(R.string.button_login, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                showDialog(DIALOG_LOGIN);
            }
        });
        builder.setNeutralButton(R.string.button_reg, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                showDialog(DIALOG_REGISTER);
            }
        });
        builder.setNegativeButton(R.string.button_faq, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                showDialog(DIALOG_FAQ);
            }
        });
        return builder.create();
    }

    /**
     * @return a login dialog.
     */
    private Dialog createDialogLogin() {
        // start the service if it was not running already
        if (this.service == null) {
            final Intent serviceIntent = new Intent(ISenseService.class.getName());
            bindService(serviceIntent, this.serviceConn, BIND_AUTO_CREATE);
        }

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
        final SharedPreferences privatePrefs = getSharedPreferences(Constants.PRIVATE_PREFS,
                MODE_PRIVATE);
        emailField.setText(privatePrefs.getString(Constants.PREF_LOGIN_MAIL, ""));

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_login_title);
        builder.setView(login);
        builder.setPositiveButton(R.string.button_login, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String name = emailField.getText().toString();
                final String pass = passField.getText().toString();

                final Editor editor = privatePrefs.edit();
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

    /**
     * @return a registration dialog, with fields for email address, login name and password (2x).
     */
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
                        final SharedPreferences privatePrefs = getSharedPreferences(
                                Constants.PRIVATE_PREFS, MODE_PRIVATE);
                        final Editor editor = privatePrefs.edit();
                        editor.putString(Constants.PREF_LOGIN_MAIL, email);
                        editor.putString(Constants.PREF_LOGIN_PASS, MD5Pass);
                        editor.commit();
                        // start registration
                        new CheckRegisterTask().execute();
                    } catch (final NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(SenseApp.this, R.string.toast_reg_pass, Toast.LENGTH_SHORT)
                            .show();

                    passField1.setText("");
                    passField2.setText("");
                    removeDialog(DIALOG_REGISTER);
                    showDialog(DIALOG_REGISTER);
                }
            }
        });
        builder.setNeutralButton(R.string.button_cancel, null);
        builder.setCancelable(false);
        return builder.create();
    }

    /**
     * @return a dialog to alert the user for changes in the CommonSense.
     */
    private Dialog createDialogUpdateAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(R.string.dialog_update_msg);
        builder.setTitle(R.string.dialog_update_title);
        builder.setPositiveButton(R.string.button_ok, null);
        builder.setCancelable(false);
        return builder.create();
    }

    /**
     * Handles clicks on the UI.
     * 
     * @param v
     *            the View that was clicked.
     */
    public void onClick(View v) {

        boolean oldState = false;
        switch (v.getId()) {
            case R.id.main_field :
                final CheckBox cb = (CheckBox) findViewById(R.id.main_cb);
                oldState = cb.isChecked();
                cb.setChecked(!oldState);
                toggleService(!oldState);
                break;
            case R.id.device_prox_field :
                final CheckBox devProx = (CheckBox) findViewById(R.id.device_prox_cb);
                if (devProx.isEnabled()) {
                    oldState = devProx.isChecked();
                    devProx.setChecked(!oldState);
                    toggleDeviceProx(!oldState);
                }
                break;
            case R.id.location_field :
                final CheckBox location = (CheckBox) findViewById(R.id.location_cb);
                if (location.isEnabled()) {
                    oldState = location.isChecked();
                    location.setChecked(!oldState);
                    toggleLocation(!oldState);
                }
                break;
            case R.id.motion_field :
                final CheckBox motion = (CheckBox) findViewById(R.id.motion_cb);
                if (motion.isEnabled()) {
                    oldState = motion.isChecked();
                    motion.setChecked(!oldState);
                    toggleMotion(!oldState);
                }
                break;
            case R.id.external_sensor_field :
                final CheckBox external = (CheckBox) findViewById(R.id.external_sensor_cb);
                if (external.isEnabled()) {
                    oldState = external.isChecked();
                    external.setChecked(!oldState);
                    toggleExternalSensors(!oldState);
                }
                break;
            case R.id.ambience_field :
                final CheckBox ambience = (CheckBox) findViewById(R.id.ambience_cb);
                if (ambience.isEnabled()) {
                    oldState = ambience.isChecked();
                    ambience.setChecked(!oldState);
                    toggleAmbience(!oldState);
                }
                break;
            case R.id.phonestate_field :
                final CheckBox phoneState = (CheckBox) findViewById(R.id.phonestate_cb);
                if (phoneState.isEnabled()) {
                    oldState = phoneState.isChecked();
                    phoneState.setChecked(!oldState);
                    togglePhoneState(!oldState);
                }
                break;
            case R.id.popquiz_field :
                final CheckBox quiz = (CheckBox) findViewById(R.id.popquiz_cb);
                if (quiz.isEnabled()) {
                    oldState = quiz.isChecked();
                    quiz.setChecked(!oldState);
                    togglePopQuiz(!oldState);
                }
                break;
            case R.id.prefs_field :
                startActivity(new Intent("nl.sense_os.app.Settings"));
                break;
            default :
                Log.e(TAG, "Unknown button pressed!");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sense_app);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch (id) {
            case DIALOG_FAQ :
                dialog = createDialogFaq();
                break;
            case DIALOG_LOGIN :
                dialog = createDialogLogin();
                break;
            case DIALOG_PROGRESS :
                dialog = new ProgressDialog(this);
                dialog.setTitle("One moment please");
                ((ProgressDialog) dialog).setMessage("Checking login credentials...");
                dialog.setCancelable(false);
                break;
            case DIALOG_REGISTER :
                dialog = createDialogRegister();
                break;
            case DIALOG_UPDATE_ALERT :
                dialog = createDialogUpdateAlert();
                break;
            case DIALOG_HELP :
                dialog = createDialogHelp();
                break;
            default :
                Log.w(TAG, "Trying to create unexpected dialog, ignoring input...");
                break;
        }
        return dialog;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_SETTINGS, Menu.NONE, "Preferences").setIcon(
                android.R.drawable.ic_menu_preferences);
        menu.add(Menu.NONE, MENU_FAQ, Menu.NONE, "FAQ").setIcon(android.R.drawable.ic_menu_help);
        menu.add(Menu.NONE, MENU_LOGIN, Menu.NONE, "Log in").setIcon(R.drawable.ic_menu_login);
        menu.add(Menu.NONE, MENU_REGISTER, Menu.NONE, "Register")
                .setIcon(R.drawable.ic_menu_invite);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_FAQ :
                showDialog(DIALOG_FAQ);
                break;
            case MENU_SETTINGS :
                startActivity(new Intent("nl.sense_os.app.Settings"));
                break;
            case MENU_LOGIN :
                showDialog(DIALOG_LOGIN);
                break;
            case MENU_REGISTER :
                showDialog(DIALOG_REGISTER);
                break;
            default :
                Log.w(TAG, "Unexpected menu button pressed, ignoring input...");
                return false;
        }
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();

        // unbind from service
        if (true == this.isServiceBound) {
            unbindService(this.serviceConn);
            this.service = null;
            this.isServiceBound = false;
        }

        // unregister service state listener
        try {
            unregisterReceiver(this.serviceListener);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Ignoring exception when trying to unregister service state listener...");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // try to bind to service
        // NB: do not use BIND_AUTO_CREATE so the service is only bound if it already exists
        final Intent serviceIntent = new Intent(ISenseService.class.getName());
        this.isServiceBound = bindService(serviceIntent, this.serviceConn, 0);

        // register receiver for updates
        IntentFilter filter = new IntentFilter(SenseService.ACTION_SERVICE_BROADCAST);
        registerReceiver(this.serviceListener, filter);

        // show dialogs to handle special cases
        SharedPreferences prefs = getSharedPreferences(Constants.STATUSPREFS, MODE_WORLD_WRITEABLE);
        if (prefs.getBoolean(Constants.PREF_FIRSTLOGIN, true)) {
            // show informational dialog on first run
            Editor editor = prefs.edit();
            editor.putInt(Constants.PREF_COMMONSENSE_VERSION, COMMONSENSE_VERSION);
            editor.commit();

            showDialog(DIALOG_HELP);
        } else if (prefs.getInt(Constants.PREF_COMMONSENSE_VERSION, 0) < COMMONSENSE_VERSION) {
            // show alert about new CommonSense version
            Editor editor = prefs.edit();
            editor.putInt(Constants.PREF_COMMONSENSE_VERSION, COMMONSENSE_VERSION);
            editor.commit();

            showDialog(DIALOG_UPDATE_ALERT);
        }

        updateUi();
    }

    private void toggleDeviceProx(boolean active) {

        Editor editor = getSharedPreferences(Constants.STATUSPREFS, MODE_WORLD_WRITEABLE).edit();
        editor.putBoolean(Constants.PREF_STATUS_DEV_PROX, active).commit();

        if (null != this.service) {
            try {
                this.service.toggleDeviceProx(active, callback);

                // show informational Toast
                if (active) {
                    final SharedPreferences prefs = PreferenceManager
                            .getDefaultSharedPreferences(this);
                    final int rate = Integer.parseInt(prefs.getString(Constants.PREF_SAMPLE_RATE,
                            "0"));
                    String interval = "";
                    switch (rate) {
                        case -2 : // real-time
                            interval = "second";
                            break;
                        case -1 : // often
                            interval = "15 seconds";
                            break;
                        case 0 : // normal
                            interval = "minute";
                            break;
                        case 1 : // rarely (15 hour)
                            interval = "15 minutes";
                            break;
                        default :
                            Log.e(TAG, "Unexpected device prox preference.");
                    }
                    final String msg = getString(R.string.toast_toggle_dev_prox);
                    Toast.makeText(this, msg.replace("?", interval), Toast.LENGTH_LONG).show();
                }

            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException toggling device proximity service.");
            }
        } else {
            Log.w(TAG, "Could not toggle device proximity service: Sense service is not bound.");
        }
    }

    private void toggleExternalSensors(boolean active) {

        Editor editor = getSharedPreferences(Constants.STATUSPREFS, MODE_WORLD_WRITEABLE).edit();
        editor.putBoolean(Constants.PREF_STATUS_EXTERNAL, active).commit();

        if (null != this.service) {
            try {
                this.service.toggleExternalSensors(active, callback);

                // show informational toast
                if (active) {
                    final SharedPreferences prefs = PreferenceManager
                            .getDefaultSharedPreferences(this);
                    final int rate = Integer.parseInt(prefs.getString(Constants.PREF_SAMPLE_RATE,
                            "0"));
                    String interval = "";
                    switch (rate) {
                        case -2 : // often
                            interval = "second";
                            break;
                        case -1 : // often
                            interval = "5 seconds";
                            break;
                        case 0 : // normal
                            interval = "minute";
                            break;
                        case 1 : // rarely
                            interval = "15 minutes";
                            break;
                        default :
                            Log.e(TAG, "Unexpected commonsense rate: " + rate);
                            break;
                    }
                    final String msg = getString(R.string.toast_toggle_external_sensors).replace(
                            "?", interval);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }

            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException toggling external sensors service.");
            }
        } else {
            Log.w(TAG, "Could not toggle external sensors service: Sense service is not bound.");
        }
    }

    private void toggleLocation(boolean active) {

        Editor editor = getSharedPreferences(Constants.STATUSPREFS, MODE_WORLD_WRITEABLE).edit();
        editor.putBoolean(Constants.PREF_STATUS_LOCATION, active).commit();

        if (null != this.service) {
            try {
                this.service.toggleLocation(active, callback);

                // show informational toast
                if (active) {
                    final SharedPreferences prefs = PreferenceManager
                            .getDefaultSharedPreferences(this);
                    final int rate = Integer.parseInt(prefs.getString(Constants.PREF_SAMPLE_RATE,
                            "0"));
                    String interval = "";
                    switch (rate) {
                        case -2 : // often
                            interval = "second";
                            break;
                        case -1 : // often
                            interval = "15 seconds";
                            break;
                        case 0 : // normal
                            interval = "minute";
                            break;
                        case 1 : // rarely
                            interval = "15 minutes";
                            break;
                        default :
                            Log.e(TAG, "Unexpected commonsense rate: " + rate);
                            break;
                    }
                    final String msg = getString(R.string.toast_toggle_location).replace("?",
                            interval);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }

            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException toggling location service.");
            }
        } else {
            Log.w(TAG, "Could not toggle location service: Sense service is not bound.");
        }
    }

    private void toggleMotion(boolean active) {

        Editor editor = getSharedPreferences(Constants.STATUSPREFS, MODE_WORLD_WRITEABLE).edit();
        editor.putBoolean(Constants.PREF_STATUS_MOTION, active).commit();

        if (null != this.service) {
            try {
                this.service.toggleMotion(active, callback);

                // show informational toast
                if (active) {
                    final SharedPreferences prefs = PreferenceManager
                            .getDefaultSharedPreferences(this);
                    final int rate = Integer.parseInt(prefs.getString(Constants.PREF_SAMPLE_RATE,
                            "0"));
                    String interval = "";
                    switch (rate) {
                        case -2 : // often
                            interval = "second";
                            break;
                        case -1 : // often
                            interval = "5 seconds";
                            break;
                        case 0 : // normal
                            interval = "minute";
                            break;
                        case 1 : // rarely
                            interval = "15 minutes";
                            break;
                        default :
                            Log.e(TAG, "Unexpected commonsense rate: " + rate);
                            break;
                    }
                    final String msg = getString(R.string.toast_toggle_motion).replace("?",
                            interval);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }

            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException toggling motion service.");
            }
        } else {
            Log.w(TAG, "Could not toggle motion service: Sense service is not bound.");
        }
    }

    private void toggleAmbience(boolean active) {

        Editor editor = getSharedPreferences(Constants.STATUSPREFS, MODE_WORLD_WRITEABLE).edit();
        editor.putBoolean(Constants.PREF_STATUS_AMBIENCE, active).commit();

        if (null != this.service) {
            try {
                this.service.toggleNoise(active, callback);

                // show informational toast
                if (active) {
                    final SharedPreferences prefs = PreferenceManager
                            .getDefaultSharedPreferences(this);
                    final int rate = Integer.parseInt(prefs.getString(Constants.PREF_SAMPLE_RATE,
                            "0"));
                    String intervalString = "";
                    String extraString = "";
                    switch (rate) {
                        case -2 :
                            intervalString = "the whole time";
                            extraString = " A sound stream will be uploaded.";
                            break;
                        case -1 :
                            // often
                            intervalString = "every 5 seconds";
                            break;
                        case 0 :
                            // normal
                            intervalString = "every minute";
                            break;
                        case 1 :
                            // rarely (1 hour)
                            intervalString = "every 15 minutes";
                            break;
                        default :
                            Log.e(TAG, "Unexpected commonsense rate preference.");
                    }
                    String msg = getString(R.string.toast_toggle_ambience).replace("?",
                            intervalString)
                            + extraString;
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }

            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException toggling ambience service.");
            }
        } else {
            Log.w(TAG, "Could not toggle ambience service: Sense service is not bound.");
        }
    }

    private void togglePhoneState(boolean active) {

        // put desired state in preferences
        Editor editor = getSharedPreferences(Constants.STATUSPREFS, MODE_WORLD_WRITEABLE).edit();
        editor.putBoolean(Constants.PREF_STATUS_PHONESTATE, active).commit();

        // toggle state in service
        if (null != this.service) {
            try {
                this.service.togglePhoneState(active, callback);

                // show informational toast
                if (active) {
                    final String msg = getString(R.string.toast_toggle_phonestate);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }

            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException toggling phone state service.");
            }
        } else {
            Log.w(TAG, "Could not toggle phone state service: Sense service is not bound.");
        }
    }

    private void togglePopQuiz(boolean active) {

        Editor editor = getSharedPreferences(Constants.STATUSPREFS, MODE_WORLD_WRITEABLE).edit();
        editor.putBoolean(Constants.PREF_STATUS_POPQUIZ, active).commit();

        if (null != this.service) {
            try {
                this.service.togglePopQuiz(active, callback);

                // show informational toast
                if (active) {
                    final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
                    final int r = Integer.parseInt(p.getString(Constants.PREF_QUIZ_RATE, "0"));
                    String interval = "ERROR";
                    switch (r) {
                        case -1 : // often (5 mins)
                            interval = "5 minutes";
                            break;
                        case 0 : // normal (15 mins)
                            interval = "15 minutes";
                            break;
                        case 1 : // rarely (1 hour)
                            interval = "hour";
                            break;
                        default :
                            Log.e(TAG, "Unexpected quiz rate preference: " + r);
                            break;
                    }

                    String msg = getString(R.string.toast_toggle_quiz).replace("?", interval);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }

            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException toggling periodic popup service.");
            }
        } else {
            Log.w(TAG, "Could not toggle periodic popup service: Sense service is not bound.");
        }
    }

    /**
     * Toggles the Sense service state. The service is started using <code>startService</code>, and
     * then the activity binds to the service. Alternatively, the service is stopped and the
     * Activity unbinds itself.
     * 
     * Afterwards, the UI is updated to make the ToggleButtons show the new service state.
     */
    private void toggleService(boolean active) {

        Editor editor = getSharedPreferences(Constants.STATUSPREFS, MODE_WORLD_WRITEABLE).edit();
        editor.putBoolean(Constants.PREF_STATUS_MAIN, active).commit();

        if (true == active) {
            // start service
            final Intent serviceIntent = new Intent(ISenseService.class.getName());
            if (null == startService(serviceIntent)) {
                Log.w(TAG, "Could not start Sense service!");
            }
            TextView description = (TextView) findViewById(R.id.main_secondLine);
            description.setText("Press to stop Sense service completely");

            this.isServiceBound = bindService(serviceIntent, this.serviceConn, 0);

        } else {
            // stop service
            final boolean stopped = stopService(new Intent(ISenseService.class.getName()));
            if (stopped) {
                unbindService(this.serviceConn);
                this.service = null;
                this.isServiceBound = false;
            } else {
                Log.w(TAG, "Service was not stopped.");
            }

            updateUi();
        }
    }

    /**
     * Updates the ToggleButtons showing the service's state by calling <code>getStatus</code> on
     * the service. This will generate a callback that updates the buttons.
     */
    private void updateUi() {
        if (null != this.service) {
            try {
                // request status report
                this.service.getStatus(this.callback);
            } catch (final RemoteException e) {
                Log.e(TAG, "Error checking service status. ", e);
            }
        } else {
            // service is not running, invoke callback method directly to update UI anyway.
            try {
                this.callback.statusReport(0);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException calling callback directly. ", e);
            }
        }
    }
}
