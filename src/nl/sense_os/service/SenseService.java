/*
 * ***********************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 * **
 * ************************************************************************************************
 * *********
 */
package nl.sense_os.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;

import nl.sense_os.service.ambience.LightSensor;
import nl.sense_os.service.ambience.NoiseSensor;
import nl.sense_os.service.deviceprox.DeviceProximity;
import nl.sense_os.service.external_sensors.ZephyrBioHarness;
import nl.sense_os.service.external_sensors.ZephyrHxM;
import nl.sense_os.service.feedback.FeedbackRx;
import nl.sense_os.service.location.LocationSensor;
import nl.sense_os.service.motion.MotionSensor;
import nl.sense_os.service.phonestate.BatterySensor;
import nl.sense_os.service.phonestate.PhoneActivitySensor;
import nl.sense_os.service.phonestate.PressureSensor;
import nl.sense_os.service.phonestate.ProximitySensor;
import nl.sense_os.service.phonestate.SensePhoneState;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class SenseService extends Service {

    /**
     * BroadcastReceiver that listens for changes in the network connectivity and updates the logged
     * in status accordingly.
     */
    private class ConnectivityListener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            final ConnectivityManager mgr = (ConnectivityManager) context
                    .getSystemService(CONNECTIVITY_SERVICE);
            final NetworkInfo info = mgr.getActiveNetworkInfo();
            if ((null != info) && (info.isConnectedOrConnecting())) {

                // check that we are not logged in yet before logging in
                if (false == isLoggedIn) {
                    Log.d(TAG, "Connectivity! Trying to log in...");
                    login();

                } else {
                    // Log.d(TAG, "Connectivity! Staying logged in...");
                }

            } else {
                // login not possible without connection
                Log.d(TAG, "No connectivity! Updating login status...");
                onLogOut();
            }

        }
    };

    /**
     * BroadcastReceiver that listens for screen state changes. Re-registers the motion sensor when
     * the screen turns off.
     */
    private class ScreenOffListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Check action just to be on the safe side.
            if (false == intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                return;
            }

            if (isMotionActive) {
                // wait half a second and re-register
                Runnable motionThread = new Runnable() {
                    @Override
                    public void run() {
                        // Unregisters the motion listener and registers it again.
                        Log.d(TAG, "Screen went off, re-registering the Motion sensor");
                        toggleMotion(false);
                        toggleMotion(true);
                    };
                };

                Handler mtHandler = new Handler();
                mtHandler.postDelayed(motionThread, 500);
            }
        }
    }

    /**
     * Implementation of the service's AIDL interface.
     */
    private class SenseServiceStub extends ISenseService.Stub {

        @Override
        public boolean changeLogin() throws RemoteException {
            return SenseService.this.changeLogin();
        }

        @Override
        public void getStatus(ISenseServiceCallback callback) {
            try {
                callback.statusReport(SenseService.this.getStatus());
            } catch (final RemoteException e) {
                Log.e(TAG, "RemoteException sending status report.", e);
            }
        }

        @Override
        public boolean register() throws RemoteException {
            return SenseService.this.register();
        }

        @Override
        public void toggleDeviceProx(boolean active, ISenseServiceCallback callback) {
            SenseService.this.toggleDeviceProx(active);
        }

        @Override
        public void toggleExternalSensors(boolean active, ISenseServiceCallback callback) {
            SenseService.this.toggleExternalSensors(active);
        }

        @Override
        public void toggleLocation(boolean active, ISenseServiceCallback callback) {
            SenseService.this.toggleLocation(active);
        }

        @Override
        public void toggleMotion(boolean active, ISenseServiceCallback callback) {
            SenseService.this.toggleMotion(active);
        }

        @Override
        public void toggleNoise(boolean active, ISenseServiceCallback callback) {
            SenseService.this.toggleAmbience(active);
        }

        @Override
        public void togglePhoneState(boolean active, ISenseServiceCallback callback) {
            SenseService.this.togglePhoneState(active);
        }

        @Override
        public void togglePopQuiz(boolean active, ISenseServiceCallback callback) {
            SenseService.this.togglePopQuiz(active);
        }
    }

    private static final String TAG = "Sense Service";
    public static final String ACTION_RELOGIN = "action_relogin";
    public final static String ACTION_SERVICE_BROADCAST = "nl.sense_os.service.Broadcast";
    private static final int NOTIF_ID = 1;
    private final ISenseService.Stub binder = new SenseServiceStub();
    public BroadcastReceiver screenOffListener = new ScreenOffListener();
    private ConnectivityListener connectivityListener;
    private BatterySensor batterySensor;
    private DeviceProximity deviceProximity;
    private LightSensor lightSensor;
    private LocationListener locListener;
    private MotionSensor motionSensor;
    private NoiseSensor noiseSensor;
    private PhoneActivitySensor phoneActivitySensor;
    private PressureSensor pressureSensor;
    private ProximitySensor proximitySensor;
    private PhoneStateListener phoneStateListener;
    private ZephyrBioHarness es_bioHarness;
    private ZephyrHxM es_HxM;
    private boolean isLoggedIn;
    // private boolean isStarted;
    private boolean isAmbienceActive;
    private boolean isDevProxActive;
    private boolean isExternalActive;
    private boolean isLocationActive;
    private boolean isMotionActive;
    private boolean isPhoneStateActive;
    private boolean isQuizActive;
    private TelephonyManager telMgr;
    private final Handler toastHandler = new Handler(Looper.getMainLooper());

    /**
     * Changes login of the Sense service. Removes "private" data of the previous user from the
     * preferences. Can be called by Activities that are bound to the service.
     * 
     * @return <code>true</code> if login was changed successfully
     */
    private boolean changeLogin() {

        // log out before changing to a new user
        onLogOut();

        // clear cached settings of the previous user (i.e. device id)
        final SharedPreferences authPrefs = getSharedPreferences(Constants.AUTH_PREFS, MODE_PRIVATE);
        final Editor editor = authPrefs.edit();
        editor.remove(Constants.PREF_DEVICE_ID);
        editor.remove(Constants.PREF_DEVICE_TYPE);
        editor.remove(Constants.PREF_LOGIN_COOKIE);
        editor.remove(Constants.PREF_SENSOR_LIST);
        editor.commit();

        return login();
    }

    /**
     * Checks if the installed Sense Platform application has an update available, alerting the user
     * via a Toast message.
     */
    private void checkVersion() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo("nl.sense_os.app", 0);
            String versionName = packageInfo.versionName;
            URI uri = new URI(Constants.URL_VERSION + "?version=" + versionName);
            final JSONObject version = SenseApi.getJsonObject(uri, "");

            if (version == null) {
                return;
            }

            if (version.getString("message").length() > 0) {
                Log.w(TAG, "Version: " + version.toString());
                showToast(version.getString("message"));
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception while getting version!", e);
        }
    }

    /**
     * @return the current status of the sensing modules
     */
    private int getStatus() {

        int status = 0;

        status = status + Constants.STATUSCODE_RUNNING;
        status = this.isLoggedIn ? status + Constants.STATUSCODE_CONNECTED : status;
        status = this.isPhoneStateActive ? status + Constants.STATUSCODE_PHONESTATE : status;
        status = this.isLocationActive ? status + Constants.STATUSCODE_LOCATION : status;
        status = this.isAmbienceActive ? status + Constants.STATUSCODE_AMBIENCE : status;
        status = this.isQuizActive ? status + Constants.STATUSCODE_QUIZ : status;
        status = this.isDevProxActive ? status + Constants.STATUSCODE_DEVICE_PROX : status;
        status = this.isExternalActive ? status + Constants.STATUSCODE_EXTERNAL : status;
        status = this.isMotionActive ? status + Constants.STATUSCODE_MOTION : status;

        return status;
    }

    /**
     * Initializes the global fields of this class.
     */
    private void initFields() {

        this.telMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

        // statuses
        this.isDevProxActive = false;
        this.isLocationActive = false;
        this.isMotionActive = false;
        this.isAmbienceActive = false;
        this.isPhoneStateActive = false;
        this.isQuizActive = false;
        this.isExternalActive = false;
    }

    /**
     * Tries to login using the username and password from the private preferences and updates the
     * {@link #isLoggedIn} status accordingly. Can also be called from Activities that are bound to
     * the service.
     * 
     * @return <code>true</code> if successful.
     */
    private boolean login() {
        // show notification that we are not logged in (yet)
        showNotification(true);

        // get login parameters from the preferences
        final SharedPreferences authPrefs = getSharedPreferences(Constants.AUTH_PREFS, MODE_PRIVATE);
        final String username = authPrefs.getString(Constants.PREF_LOGIN_USERNAME, null);
        final String pass = authPrefs.getString(Constants.PREF_LOGIN_PASS, null);

        // try to log in
        if ((username != null) && (pass != null)) {
            this.isLoggedIn = SenseApi.login(this, username, pass);

            if (this.isLoggedIn) {
                // logged in successfully
                onLogIn();
            } else {
                Log.d(TAG, "Login failed");
            }

        } else {
            Log.d(TAG, "Cannot login: username or password unavailable... Username: " + username
                    + ", password: " + pass);
            this.isLoggedIn = false;
        }

        return this.isLoggedIn;
    }
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return this.binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        // initialize stuff
        initFields();

        // make service as important as regular activities
        startForegroundCompat();

        // Register the receiver for SCREEN OFF events
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(this.screenOffListener, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        // stop listening for possibility to login
        if (null != this.connectivityListener) {
            try {
                unregisterReceiver(this.connectivityListener);
                this.connectivityListener = null;
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Ignoring exception when trying to unregister connectivity listener");
            }
        }

        // stop listening to screen off receiver
        try {
            unregisterReceiver(this.screenOffListener);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Ignoring exception when trying to unregister screen off listener");
        }

        // stop active sensing components
        onLogOut();

        // stop the main service
        stopForegroundCompat();
    }

    /**
     * Performs tasks after successful login: gets list of registered sensors; starts the sensing
     * modules in the same state as before logout; starts periodic alarms for data transmission and
     * feedback checking.
     */
    private void onLogIn() {
        Log.d(TAG, "Logged in! Starting service...");

        // Retrieve the online registered sensor list
        SenseApi.getRegisteredSensors(this);

        // restart individual sensing components
        startSensorModules();

        // start database leeglepelaar
        startTransmitAlarms();

        // start the periodic checks of the feedback sensor
        startFeedbackChecks();

        // show notification
        showNotification(false);

        // send broadcast that something has changed in the status
        sendBroadcast(new Intent(ACTION_SERVICE_BROADCAST));
    }

    /**
     * Performs cleanup tasks when the service is logged out: stops any running sensing modules;
     * updates the status bar notification; stops the periodic alarms for foorback and data
     * transmission.
     */
    private void onLogOut() {
        // check if we were actually logged to prevent overwriting the last active state..
        if (this.isLoggedIn) {
            Log.d(TAG, "Logged out...");

            // stop active sensing components
            stopSensorModules();

            // update login status
            this.isLoggedIn = false;
        }

        showNotification(true);

        stopFeedbackChecks();
        stopTransmitAlarms();
    }

    /**
     * Deprecated method for starting the service, used in Android 1.6 and older.
     */
    @Override
    public void onStart(Intent intent, int startid) {
        onStartCompat(intent, 0, startid);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onStartCompat(intent, flags, startId);
        return START_STICKY;
    }

    /**
     * Starts the Sense service. Tries to log in and start sensing; starts listening for network
     * connectivity broadcasts.
     * 
     * @param intent
     *            The Intent supplied to {@link Activity#startService(Intent)}. This may be null if
     *            the service is being restarted after its process has gone away.
     * @param flags
     *            Additional data about this start request. Currently either 0,
     *            {@link Service#START_FLAG_REDELIVERY} , or {@link Service#START_FLAG_RETRY}.
     * @param startId
     *            A unique integer representing this specific request to start. Use with
     *            {@link #stopSelfResult(int)}.
     */
    private void onStartCompat(Intent intent, int flags, int startId) {

        // intent is null when the Service is recreated by Android after it was killed
        boolean relogin = true;
        if (null != intent) {
            relogin = intent.getBooleanExtra(ACTION_RELOGIN, false);
        }

        // try to login immediately
        if (false == isLoggedIn || relogin) {
            login();
        } else {
            checkVersion();

            // restart the individual modules
            startSensorModules();
        }

        // register broadcast receiver for login in case of Internet connection changes
        if (null == this.connectivityListener) {
            this.connectivityListener = new ConnectivityListener();
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            registerReceiver(this.connectivityListener, filter);
        }
    }

    /**
     * Tries to register a new user using the username and password from the private preferences and
     * updates the {@link #isLoggedIn} status accordingly. Can also be called from Activities that
     * are bound to the service.
     * 
     * @return <code>true</code> if successful.
     */
    private boolean register() {

        // log out before registering a new user
        onLogOut();

        // clear cached settings of the previous user (i.e. device id)
        final SharedPreferences authPrefs = getSharedPreferences(Constants.AUTH_PREFS, MODE_PRIVATE);
        final Editor authEditor = authPrefs.edit();
        authEditor.remove(Constants.PREF_DEVICE_ID);
        authEditor.remove(Constants.PREF_DEVICE_TYPE);
        authEditor.remove(Constants.PREF_LOGIN_COOKIE);
        authEditor.remove(Constants.PREF_SENSOR_LIST);
        authEditor.commit();

        // get login parameters for the new user from the preferences
        final String username = authPrefs.getString(Constants.PREF_LOGIN_USERNAME, null);
        final String password = authPrefs.getString(Constants.PREF_LOGIN_PASS, null);

        // try to register
        if ((null != username) && (null != password)) {
            Log.d(TAG, "Registering... Username: " + username + ", password: " + password);

            boolean registered = SenseApi.register(this, username, password);
            if (registered) {
                login();
            } else {
                Log.w(TAG, "Registration failed");
            }
        } else {
            Log.w(TAG, "Cannot register: username or password unavailable... Username: " + username
                    + ", password: " + password);
        }
        return isLoggedIn;
    }
    /**
     * Shows a status bar notification that the Sense service is active, also displaying the
     * username if the service is logged in.
     * 
     * @param error
     *            set to <code>true</code> if the service is not running properly.
     */
    private void showNotification(boolean error) {

        // select the icon resource
        int icon = R.drawable.ic_status_sense;
        if (error) {
            icon = R.drawable.ic_status_sense_disabled;
        }

        final long when = System.currentTimeMillis();
        final Notification note = new Notification(icon, null, when);
        note.flags = Notification.FLAG_NO_CLEAR;

        // extra info text is shown when the status bar is opened
        final CharSequence contentTitle = "Sense service";
        CharSequence contentText = "";
        if (error) {
            contentText = "Trying to log in...";
        } else {
            final SharedPreferences authPrefs = getSharedPreferences(Constants.AUTH_PREFS,
                    MODE_PRIVATE);
            contentText = "Logged in as "
                    + authPrefs.getString(Constants.PREF_LOGIN_USERNAME, "UNKNOWN");
        }
        final Intent notifIntent = new Intent("nl.sense_os.app.SenseApp");
        notifIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notifIntent, 0);
        note.setLatestEventInfo(getApplicationContext(), contentTitle, contentText, contentIntent);

        final NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mgr.notify(NOTIF_ID, note);
    }
    /**
     * Displays a Toast message using the process's main Thread.
     * 
     * @param message
     *            Toast message to display to the user
     */
    private void showToast(final String message) {
        this.toastHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SenseService.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Starts the checks that periodically check if the service is still alive. Should be started
     * immediately after creation.
     */
    private void startAliveChecks() {

        // put alive status in the preferences
        final SharedPreferences statusPrefs = getSharedPreferences(Constants.STATUS_PREFS,
                MODE_WORLD_WRITEABLE);
        final Editor editor = statusPrefs.edit();
        editor.putBoolean(Constants.PREF_ALIVE, true);
        editor.commit();

        // start the alarms
        final Intent alarmIntent = new Intent(AliveChecker.ACTION_CHECK_ALIVE);
        final PendingIntent alarmOp = PendingIntent.getBroadcast(this,
                AliveChecker.REQ_CHECK_ALIVE, alarmIntent, 0);
        final long alarmTime = System.currentTimeMillis() + AliveChecker.PERIOD_CHECK_ALIVE;
        final AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mgr.cancel(alarmOp);
        mgr.set(AlarmManager.RTC_WAKEUP, alarmTime, alarmOp);
    }

    /**
     * Starts the checks that periodically check if CommonSense needs feedback. Should be started
     * after successful login.
     */
    private void startFeedbackChecks() {
        final Intent alarmIntent = new Intent(FeedbackRx.ACTION_CHECK_FEEDBACK);
        final PendingIntent alarmOp = PendingIntent.getBroadcast(this,
                FeedbackRx.REQ_CHECK_FEEDBACK, alarmIntent, 0);
        final long alarmTime = System.currentTimeMillis() + 1000 * 10;
        final AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mgr.cancel(alarmOp);
        mgr.set(AlarmManager.RTC_WAKEUP, alarmTime, alarmOp);
    }

    /**
     * Makes this service a foreground service, as important as 'real' activities. As a reminder
     * that the service is running, a notification is shown in the status bar.
     */
    private void startForegroundCompat() {

        startAliveChecks();

        @SuppressWarnings("rawtypes")
        final Class[] startForegroundSignature = new Class[]{int.class, Notification.class};
        Method startForeground = null;
        try {
            startForeground = getClass().getMethod("startForeground", startForegroundSignature);
        } catch (NoSuchMethodException e) {
            // Running on an older platform.
            startForeground = null;
        }

        // call startForeground in fancy way so old systems do not get confused by unknown methods
        if (startForeground == null) {
            setForeground(true);
        } else {
            // create notification
            final int icon = R.drawable.ic_status_sense_disabled;
            final long when = System.currentTimeMillis();
            final Notification n = new Notification(icon, null, when);
            final Intent notifIntent = new Intent("nl.sense_os.app.SenseApp");
            notifIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            final PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notifIntent, 0);
            n.setLatestEventInfo(this, "Sense service", "", contentIntent);

            Object[] startArgs = {Integer.valueOf(NOTIF_ID), n};
            try {
                startForeground.invoke(this, startArgs);
            } catch (InvocationTargetException e) {
                // Should not happen.
                Log.e(TAG, "Unable to invoke startForeground", e);
            } catch (IllegalAccessException e) {
                // Should not happen.
                Log.e(TAG, "Unable to invoke startForeground", e);
            }
        }
    }

    /**
     * Toggles the individual sensor modules according to the status that was stored in the
     * preferences.
     */
    private void startSensorModules() {

        final SharedPreferences statusPrefs = getSharedPreferences(Constants.STATUS_PREFS,
                MODE_WORLD_WRITEABLE);

        if (statusPrefs.getBoolean(Constants.PREF_STATUS_MAIN, true)) {

            if (statusPrefs.getBoolean(Constants.PREF_STATUS_PHONESTATE, true)) {
                // Log.d(TAG, "Restart phone state component...");
                togglePhoneState(true);
            }
            if (statusPrefs.getBoolean(Constants.PREF_STATUS_LOCATION, false)) {
                // Log.d(TAG, "Restart location component...");
                toggleLocation(true);
            }
            if (statusPrefs.getBoolean(Constants.PREF_STATUS_AMBIENCE, false)) {
                // Log.d(TAG, "Restart ambience components...");
                toggleAmbience(true);
            }
            if (statusPrefs.getBoolean(Constants.PREF_STATUS_MOTION, false)) {
                // Log.d(TAG, "Restart motion component...");
                toggleMotion(true);
            }
            if (statusPrefs.getBoolean(Constants.PREF_STATUS_DEV_PROX, false)) {
                // Log.d(TAG, "Restart neighboring devices components...");
                toggleDeviceProx(true);
            }
            if (statusPrefs.getBoolean(Constants.PREF_STATUS_EXTERNAL, false)) {
                // Log.d(TAG, "Restart external sensors service...");
                toggleExternalSensors(true);
            }
            if (statusPrefs.getBoolean(Constants.PREF_STATUS_POPQUIZ, false)) {
                // Log.d(TAG, "Restart popquiz component...");
                togglePopQuiz(true);
            }
        }
    }

    /**
     * Start periodic broadcast to trigger the MsgHandler to flush its buffer to CommonSense.
     */
    private void startTransmitAlarms() {
        Intent alarm = new Intent(this, DataTransmitter.class);
        PendingIntent operation = PendingIntent.getBroadcast(this, DataTransmitter.REQID, alarm, 0);
        AlarmManager mgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        mgr.cancel(operation);
        mgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), operation);
    }

    /**
     * Stops the periodic checks to keep the service alive.
     */
    private void stopAliveChecks() {
        // remove alive status in the preferences
        final SharedPreferences statusPrefs = getSharedPreferences(Constants.STATUS_PREFS,
                MODE_WORLD_WRITEABLE);
        final Editor editor = statusPrefs.edit();
        editor.putBoolean(Constants.PREF_ALIVE, false);
        editor.commit();

        // stop the alive check broadcasts
        final Intent alarmIntent = new Intent(AliveChecker.ACTION_CHECK_ALIVE);
        final PendingIntent alarmOp = PendingIntent.getBroadcast(this,
                AliveChecker.REQ_CHECK_ALIVE, alarmIntent, 0);
        final AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mgr.cancel(alarmOp);
    }

    /**
     * Stops the periodic checks for feedback from CommonSense.
     */
    private void stopFeedbackChecks() {
        final Intent alarmIntent = new Intent(FeedbackRx.ACTION_CHECK_FEEDBACK);
        final PendingIntent alarmOp = PendingIntent.getBroadcast(this,
                FeedbackRx.REQ_CHECK_FEEDBACK, alarmIntent, 0);
        final AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mgr.cancel(alarmOp);
    }

    /**
     * Makes this service a foreground service, as important as 'real' activities. As a reminder
     * that the service is running, a notification is shown.
     */
    private void stopForegroundCompat() {

        stopAliveChecks();

        @SuppressWarnings("rawtypes")
        final Class[] stopForegroundSignature = new Class[]{boolean.class};
        Method stopForeground = null;
        try {
            stopForeground = getClass().getMethod("stopForeground", stopForegroundSignature);
        } catch (NoSuchMethodException e) {
            // Running on an older platform.
            stopForeground = null;
        }

        // remove the notification that the service is running
        final NotificationManager noteMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        noteMgr.cancel(NOTIF_ID);

        // call stopForeground in fancy way so old systems do net get confused by unknown methods
        if (stopForeground == null) {
            setForeground(false);
        } else {
            Object[] stopArgs = {Boolean.TRUE};
            try {
                stopForeground.invoke(this, stopArgs);
            } catch (InvocationTargetException e) {
                // Should not happen.
                Log.w(TAG, "Unable to invoke stopForeground", e);
            } catch (IllegalAccessException e) {
                // Should not happen.
                Log.w(TAG, "Unable to invoke stopForeground", e);
            }
        }
    }

    /**
     * Stops any running sensor modules.
     */
    private void stopSensorModules() {
        if (this.isDevProxActive) {
            toggleDeviceProx(false);
        }
        if (this.isMotionActive) {
            toggleMotion(false);
        }
        if (this.isLocationActive) {
            toggleLocation(false);
        }
        if (this.isAmbienceActive) {
            toggleAmbience(false);
        }
        if (this.isPhoneStateActive) {
            togglePhoneState(false);
        }
        if (this.isQuizActive) {
            togglePopQuiz(false);
        }
        if (this.isExternalActive) {
            toggleExternalSensors(false);
        }
    }

    /**
     * Stops the periodic alarms to flush the MsgHandler buffer to CommonSense.
     */
    private void stopTransmitAlarms() {
        Intent alarm = new Intent(this, DataTransmitter.class);
        PendingIntent operation = PendingIntent.getBroadcast(this, DataTransmitter.REQID, alarm, 0);
        AlarmManager mgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        mgr.cancel(operation);
    }

    private void toggleAmbience(boolean active) {

        if (active != this.isAmbienceActive) {
            this.isAmbienceActive = active;

            if (true == active) {

                // check noise sensor presence
                if (null != this.noiseSensor) {
                    Log.w(TAG, "Noise sensor is already present!");
                    this.noiseSensor.disable();
                    this.noiseSensor = null;
                }

                // check light sensor presence
                if (null != this.lightSensor) {
                    Log.w(TAG, "Light sensor is already present!");
                    this.lightSensor.stopLightSensing();
                    this.lightSensor = null;
                }

                // get sample rate from preferences
                final SharedPreferences mainPrefs = getSharedPreferences(Constants.MAIN_PREFS,
                        MODE_WORLD_WRITEABLE);
                final int rate = Integer.parseInt(mainPrefs.getString(Constants.PREF_SAMPLE_RATE,
                        "0"));
                int interval = -1;
                switch (rate) {
                    case -2 : // real time
                        interval = -1;
                        break;
                    case -1 : // often
                        interval = 10 * 1000;
                        break;
                    case 0 : // normal
                        interval = 60 * 1000;
                        break;
                    case 1 : // rarely (15 minutes)
                        interval = 15 * 60 * 1000;
                        break;
                    default :
                        Log.e(TAG, "Unexpected sample rate preference.");
                }
                final int finalInterval = interval;

                // instantiate the sensors on the main process thread
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {

                    @Override
                    public void run() {

                        if (mainPrefs.getBoolean(Constants.PREF_AMBIENCE_MIC, true)) {
                            noiseSensor = new NoiseSensor(SenseService.this);
                            noiseSensor.enable(finalInterval);
                        }
                        if (mainPrefs.getBoolean(Constants.PREF_AMBIENCE_LIGHT, true)) {
                            lightSensor = new LightSensor(SenseService.this);
                            lightSensor.startLightSensing(finalInterval);
                        }
                    }

                });

            } else {

                // stop sensing
                if (null != this.noiseSensor) {
                    this.telMgr.listen(this.noiseSensor, PhoneStateListener.LISTEN_NONE);
                    this.noiseSensor.disable();
                    this.noiseSensor = null;
                }
                if (null != this.lightSensor) {
                    this.lightSensor.stopLightSensing();
                    this.lightSensor = null;
                }
            }
        }
    }

    private void toggleDeviceProx(boolean active) {

        if (active != this.isDevProxActive) {
            this.isDevProxActive = active;

            if (true == active) {

                // check device proximity sensor presence
                if (null != this.deviceProximity) {
                    Log.w(TAG, "Device proximity sensor is already present!");
                    this.deviceProximity.stopEnvironmentScanning();
                    this.deviceProximity = null;
                }

                // get sample rate
                final SharedPreferences mainPrefs = getSharedPreferences(Constants.MAIN_PREFS,
                        MODE_WORLD_WRITEABLE);
                final int rate = Integer.parseInt(mainPrefs.getString(Constants.PREF_SAMPLE_RATE,
                        "0"));
                int interval = 1;
                switch (rate) {
                    case -2 :
                        interval = 1 * 1000;
                        break;
                    case -1 :
                        // often
                        interval = 15 * 1000;
                        break;
                    case 0 :
                        // normal
                        interval = 60 * 1000;
                        break;
                    case 1 :
                        // rarely (15 hour)
                        interval = 15 * 60 * 1000;
                        break;
                    default :
                        Log.e(TAG, "Unexpected device proximity rate preference.");
                }
                final int finalInterval = interval;

                // instantiate the sensor on the main process thread
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        deviceProximity = new DeviceProximity(SenseService.this);

                        // start sensing
                        deviceProximity.startEnvironmentScanning(finalInterval);
                    }

                });

            } else {

                // stop sensing
                if (null != this.deviceProximity) {
                    this.deviceProximity.stopEnvironmentScanning();
                    this.deviceProximity = null;
                }
            }
        }
    }

    private void toggleExternalSensors(boolean active) {

        if (active != this.isExternalActive) {
            this.isExternalActive = active;

            if (true == active) {

                // check BioHarness sensor presence
                if (null != this.es_bioHarness) {
                    Log.w(TAG, "Bioharness sensor is already present!");
                    this.es_bioHarness.stopBioHarness();
                    this.es_bioHarness = null;
                }

                // check HxM sensor presence
                if (null != this.es_HxM) {
                    Log.w(TAG, "HxM sensor is already present!");
                    this.es_HxM.stopHxM();
                    this.es_HxM = null;
                }

                // get sample rate
                final SharedPreferences mainPrefs = getSharedPreferences(Constants.MAIN_PREFS,
                        MODE_WORLD_WRITEABLE);
                final int rate = Integer.parseInt(mainPrefs.getString(Constants.PREF_SAMPLE_RATE,
                        "0"));
                int interval = 1;
                switch (rate) {
                    case -2 :
                        interval = 1 * 1000;
                        break;
                    case -1 :
                        // often
                        interval = 5 * 1000;
                        break;
                    case 0 :
                        // normal
                        interval = 60 * 1000;
                        break;
                    case 1 :
                        // rarely (15 minutes)
                        interval = 15 * 60 * 1000;
                        break;
                    default :
                        Log.e(TAG, "Unexpected external sensor rate preference.");
                        return;
                }
                final int finalInterval = interval;

                // instantiate the sensor on the main process thread
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        if (mainPrefs.getBoolean(Constants.PREF_BIOHARNESS, false)) {
                            es_bioHarness = new ZephyrBioHarness(SenseService.this);
                            es_bioHarness.startBioHarness(finalInterval);
                        }
                        if (mainPrefs.getBoolean(Constants.PREF_HXM, false)) {
                            es_HxM = new ZephyrHxM(SenseService.this);
                            es_HxM.startHxM(finalInterval);
                        }
                    }
                });

            } else {

                // stop sensing
                if (null != this.es_bioHarness) {
                    Log.w(TAG, "Bioharness sensor is already present!");
                    this.es_bioHarness.stopBioHarness();
                    this.es_bioHarness = null;
                }

                // check HxM sensor presence
                if (null != this.es_HxM) {
                    Log.w(TAG, "HxM sensor is already present!");
                    this.es_HxM.stopHxM();
                    this.es_HxM = null;
                }
            }
        }
    }

    private void toggleLocation(boolean active) {

        if (active != this.isLocationActive) {
            this.isLocationActive = active;

            final LocationManager locMgr = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (true == active) {

                // check location sensor presence
                if (this.locListener != null) {
                    Log.w(TAG, "location listener is already present!");
                    locMgr.removeUpdates(this.locListener);
                    this.locListener = null;
                }

                // get sample rate
                final SharedPreferences mainPrefs = getSharedPreferences(Constants.MAIN_PREFS,
                        MODE_WORLD_WRITEABLE);
                final int rate = Integer.parseInt(mainPrefs.getString(Constants.PREF_SAMPLE_RATE,
                        "0"));
                long minTime = -1;
                float minDistance = -1;
                switch (rate) {
                    case -2 : // real-time
                        minTime = 1000;
                        minDistance = 0;
                        break;
                    case -1 : // often
                        minTime = 15 * 1000;
                        minDistance = 10;
                        break;
                    case 0 : // normal
                        minTime = 60 * 1000;
                        minDistance = 25;
                        break;
                    case 1 : // rarely
                        minTime = 15 * 60 * 1000;
                        minDistance = 25;
                        break;
                    default :
                        Log.e(TAG, "Unexpected commonsense rate: " + rate);
                        break;
                }

                // check if any providers are selected in the preferences
                final boolean gps = mainPrefs.getBoolean(Constants.PREF_LOCATION_GPS, true);
                final boolean network = mainPrefs.getBoolean(Constants.PREF_LOCATION_NETWORK, true);
                if (gps || network) {
                    this.locListener = new LocationSensor(this);
                } else {
                    // GPS and network are disabled in the preferences
                    this.isLocationActive = false;

                    // show informational Toast
                    showToast(getString(R.string.toast_location_noprovider));
                }

                // start listening to GPS and/or Network location
                if (true == gps) {
                    final String gpsProvider = LocationManager.GPS_PROVIDER;
                    locMgr.requestLocationUpdates(gpsProvider, minTime, minDistance,
                            this.locListener, Looper.getMainLooper());
                }

                if (true == network) {
                    final String nwProvider = LocationManager.NETWORK_PROVIDER;
                    locMgr.requestLocationUpdates(nwProvider, minTime, minDistance,
                            this.locListener, Looper.getMainLooper());
                }

            } else {

                // stop location listener
                if (null != this.locListener) {
                    locMgr.removeUpdates(this.locListener);
                    this.locListener = null;
                }
            }
        }
    }

    private void toggleMotion(boolean active) {

        if (active != this.isMotionActive) {
            this.isMotionActive = active;

            if (true == active) {

                // check motion sensor presence
                if (this.motionSensor != null) {
                    Log.w(TAG, "motion sensor is already present!");
                    this.motionSensor.stopMotionSensing();
                    this.motionSensor = null;
                }

                // get sample rate
                final SharedPreferences mainPrefs = getSharedPreferences(Constants.MAIN_PREFS,
                        MODE_WORLD_WRITEABLE);
                final int rate = Integer.parseInt(mainPrefs.getString(Constants.PREF_SAMPLE_RATE,
                        "0"));
                int interval = -1;
                switch (rate) {
                    case -2 : // real time
                        interval = 0;
                        break;
                    case -1 : // often
                        interval = 5 * 1000;
                        break;
                    case 0 : // normal
                        interval = 60 * 1000;
                        break;
                    case 1 : // rarely (15 minutes)
                        interval = 15 * 60 * 1000;
                        break;
                    default :
                        Log.e(TAG, "Unexpected commonsense rate: " + rate);
                        break;
                }
                final int finalInterval = interval;

                // instantiate the sensors on the main process thread
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        motionSensor = new MotionSensor(SenseService.this);
                        motionSensor.startMotionSensing(finalInterval);
                    }

                });

            } else {

                // stop sensing
                if (null != this.motionSensor) {
                    this.motionSensor.stopMotionSensing();
                    this.motionSensor = null;
                }
            }
        }
    }

    private void togglePhoneState(boolean active) {

        if (active != this.isPhoneStateActive) {
            this.isPhoneStateActive = active;

            if (true == active) {

                // check phone state sensor presence
                if (this.phoneStateListener != null) {
                    Log.w(TAG, "phone state sensor is already present!");
                    this.telMgr.listen(this.phoneStateListener, PhoneStateListener.LISTEN_NONE);
                    this.phoneStateListener = null;
                }

                // check proximity sensor presence
                if (this.proximitySensor != null) {
                    Log.w(TAG, "proximity sensor is already present!");
                    this.proximitySensor.stopProximitySensing();
                    this.proximitySensor = null;
                }

                // check battery sensor presence
                if (this.batterySensor != null) {
                    Log.w(TAG, "battery sensor is already present!");
                    this.batterySensor.stopBatterySensing();
                    this.batterySensor = null;
                }

                // check pressure sensor presence
                if (this.pressureSensor != null) {
                    Log.w(TAG, "pressure sensor is already present!");
                    this.pressureSensor.stopPressureSensing();
                    this.pressureSensor = null;
                }

                // check phone activity sensor presence
                if (this.phoneActivitySensor != null) {
                    Log.w(TAG, "phone activity sensor is already present!");
                    this.phoneActivitySensor.stopPhoneActivitySensing();
                    this.phoneActivitySensor = null;
                }

                // get sample rate
                final SharedPreferences mainPrefs = getSharedPreferences(Constants.MAIN_PREFS,
                        MODE_WORLD_WRITEABLE);
                final int rate = Integer.parseInt(mainPrefs.getString(Constants.PREF_SAMPLE_RATE,
                        "0"));
                int interval = -1;
                switch (rate) {
                    case -2 : // real time
                        interval = 0;
                        break;
                    case -1 : // often
                        interval = 5 * 1000;
                        break;
                    case 0 : // normal
                        interval = 60 * 1000;
                        break;
                    case 1 : // rarely (15 minutes)
                        interval = 15 * 60 * 1000;
                        break;
                    default :
                        Log.e(TAG, "Unexpected commonsense rate: " + rate);
                        break;
                }
                final int finalInterval = interval;

                // instantiate the sensors on the main process thread
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        phoneStateListener = new SensePhoneState(SenseService.this);
                        proximitySensor = new ProximitySensor(SenseService.this);
                        batterySensor = new BatterySensor(SenseService.this);
                        pressureSensor = new PressureSensor(SenseService.this);
                        phoneActivitySensor = new PhoneActivitySensor(SenseService.this);

                        // start sensing
                        telMgr.listen(phoneStateListener,
                                PhoneStateListener.LISTEN_CALL_FORWARDING_INDICATOR
                                        | PhoneStateListener.LISTEN_CALL_STATE
                                        | PhoneStateListener.LISTEN_DATA_ACTIVITY
                                        | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                                        | PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR
                                        | PhoneStateListener.LISTEN_SERVICE_STATE
                                        | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
                        proximitySensor.startProximitySensing(finalInterval);
                        batterySensor.startBatterySensing(finalInterval);
                        pressureSensor.startPressureSensing(finalInterval);
                        phoneActivitySensor.startPhoneActivitySensing(finalInterval);
                    }

                });

            } else {

                // stop sensing
                if (null != this.phoneStateListener) {
                    this.telMgr.listen(this.phoneStateListener, PhoneStateListener.LISTEN_NONE);
                    this.phoneStateListener = null;
                }
                if (null != this.proximitySensor) {
                    this.proximitySensor.stopProximitySensing();
                    this.proximitySensor = null;
                }
                if (null != this.pressureSensor) {
                    this.pressureSensor.stopPressureSensing();
                    this.pressureSensor = null;
                }
                if (null != this.batterySensor) {
                    this.batterySensor.stopBatterySensing();
                    this.batterySensor = null;
                }
                if (null != this.phoneActivitySensor) {
                    this.phoneActivitySensor.stopPhoneActivitySensing();
                    this.phoneActivitySensor = null;
                }
            }
        }
    }

    private void togglePopQuiz(boolean active) {

        // if (active != isQuizActive) {
        // this.isQuizActive = active;
        // final SenseAlarmManager mgr = new SenseAlarmManager(this);
        // if (true == active) {
        //
        // // create alarm
        // mgr.createSyncAlarm();
        // this.isQuizActive = mgr.createEntry(0, 1);
        // } else {
        //
        // // cancel alarm
        // mgr.cancelEntry();
        // mgr.cancelSyncAlarm();
        // }
        // }
    }
}
