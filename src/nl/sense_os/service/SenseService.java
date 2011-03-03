/*
 ************************************************************************************************************
 *     Copyright (C)  2010 Sense Observation Systems, Rotterdam, the Netherlands.  All rights reserved.     *
 ************************************************************************************************************
 */
package nl.sense_os.service;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;

import nl.sense_os.app.R;
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
import nl.sense_os.service.popquiz.SenseAlarmManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class SenseService extends Service {
    private class ConnectivityListener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            ConnectivityManager mgr = (ConnectivityManager) context
                    .getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo info = mgr.getActiveNetworkInfo();
            if ((null != info) && (info.isConnectedOrConnecting())) {
                // check that we are not logged in yet before logging in
                if (false == SenseService.isLoggedIn) {
                    Log.d(TAG, "Connectivity! Trying to log in...");

                    senseServiceLogin();
                } else {
                    Log.d(TAG, "Connectivity! Staying logged in...");
                }
            } else {
                Log.d(TAG, "No connectivity! Updating login status...");

                // login not possible without connection
                onLogOut();
            }

        }
    };

    /**
     * BroadcastReceiver for handling ACTION_SCREEN_OFF.
     */
    private class ScreenOffListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Check action just to be on the safe side.
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                if (statusMotion) {
                    Runnable motionThread = new Runnable() {
                        @Override
                        public void run() {
                            // Unregisters the motion listener and registers it again.
                            Log.d(TAG, "Screen went off, re-registering the Motion sensor");
                            // wait a few seconds and re-register
                            toggleMotion(false);
                            toggleMotion(true);
                        };
                    };

                    Handler mtHandler = new Handler();
                    mtHandler.postDelayed(motionThread, 500);
                }
            }
        }
    }

    /**
     * Implementation of the service's AIDL interface.
     */
    private class SenseServiceStub extends ISenseService.Stub {

        @Override
        public void getStatus(ISenseServiceCallback callback) {
            try {
                callback.statusReport(SenseService.this.getStatus());
            } catch (final RemoteException e) {
                Log.e(TAG, "RemoteException sending status report.", e);
            }
        }

        @Override
        public boolean changeLogin() throws RemoteException {
            return senseServiceLogin();
        }

        @Override
        public boolean serviceRegister() throws RemoteException {
            return senseServiceRegister();
        }

        @Override
        public String serviceResponse() {
            return "";
        }

        @Override
        public void toggleDeviceProx(boolean active, ISenseServiceCallback callback) {
            SenseService.this.toggleDeviceProx(active);
            // this.getStatus(callback);
        }

        @Override
        public void toggleExternalSensors(boolean active, ISenseServiceCallback callback) {
            SenseService.this.toggleExternalSensors(active);
            // this.getStatus(callback);
        }

        @Override
        public void toggleLocation(boolean active, ISenseServiceCallback callback) {
            SenseService.this.toggleLocation(active);
            // this.getStatus(callback);
        }

        @Override
        public void toggleMotion(boolean active, ISenseServiceCallback callback) {
            SenseService.this.toggleMotion(active);
            // this.getStatus(callback);
        }

        @Override
        public void toggleNoise(boolean active, ISenseServiceCallback callback) {
            SenseService.this.toggleAmbience(active);
            // this.getStatus(callback);
        }

        @Override
        public void togglePhoneState(boolean active, ISenseServiceCallback callback) {
            SenseService.this.togglePhoneState(active);
            // this.getStatus(callback);
        }

        @Override
        public void togglePopQuiz(boolean active, ISenseServiceCallback callback) {
            SenseService.this.togglePopQuiz(active);
            // this.getStatus(callback);
        }
    }

    public static final String ACTION_RELOGIN = "action_relogin";
    public final static String ACTION_SERVICE_BROADCAST = "nl.sense_os.service.Broadcast";
    private static final int NOTIF_ID = 1;
    public static boolean isLoggedIn = false;
    private static final String TAG = "Sense Service";
    private BatterySensor batterySensor;
    private final ISenseService.Stub binder = new SenseServiceStub();
    public BroadcastReceiver screenOffListener = new ScreenOffListener();
    private ConnectivityListener connectivityListener;
    private DeviceProximity deviceProximity;
    private ZephyrBioHarness es_bioHarness;
    private ZephyrHxM es_HxM;
    private LightSensor lightSensor;
    private LocationListener locListener;
    private MotionSensor motionSensor;
    private NoiseSensor noiseSensor;
    private PhoneActivitySensor phoneActivitySensor;
    private PressureSensor pressureSensor;
    private ProximitySensor proximitySensor;
    private PhoneStateListener psl;
    private boolean started;
    private boolean statusAmbience;
    private boolean statusDeviceProx;
    private boolean statusExternalSensors;
    private boolean statusLocation;
    private boolean statusMotion;
    private boolean statusPhoneState;
    private boolean statusPopQuiz;
    private TelephonyManager telMan;
    private final Handler toastHandler = new Handler(Looper.getMainLooper());

    private void checkVersion() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo("nl.sense_os.app", 0);
            String versionName = packageInfo.versionName;
            URI uri = new URI(Constants.URL_VERSION + "?version=" + versionName);
            final JSONObject version = SenseApi.getJSONObject(uri, "");
            if (version == null)
                return;
            Log.d(TAG, "Version: " + version.toString());
            if (version.getString("message").length() > 0)
                showToast(version.getString("message"));

        } catch (Exception e) {
            Log.e(TAG, "Error in getting version:" + e.getMessage());
        }
    }

    public int getDeviceID() {
        try {
            SharedPreferences prefs = getSharedPreferences(Constants.PRIVATE_PREFS, MODE_PRIVATE);
            Editor editor = prefs.edit();
            int device_id = prefs.getInt(Constants.PREF_DEVICE_ID, -1);
            if (device_id != -1)
                return device_id;
            String imei = this.telMan.getDeviceId();
            editor.putString(Constants.PREF_PHONE_IMEI, imei);
            editor.putString(Constants.PREF_PHONE_TYPE, Build.MODEL);
            editor.commit();
            URI uri = new URI(Constants.URL_GET_DEVICES);
            String cookie = prefs.getString(Constants.PREF_LOGIN_COOKIE, "");
            JSONObject response = SenseApi.getJSONObject(uri, cookie);
            if (response != null) {
                JSONArray deviceList = (JSONArray) response.get("devices");
                if (deviceList != null)
                    for (int x = 0; x < deviceList.length(); x++) {
                        JSONObject device = (JSONObject) deviceList.get(x);
                        if (device != null) {
                            String uuid = (String) device.get("uuid");
                            // Found the right device
                            if (uuid.compareToIgnoreCase(imei) == 0)
                                device_id = Integer.parseInt((String) (device.get("id")));
                            editor.putInt(Constants.PREF_DEVICE_ID, device_id);
                            editor.commit();
                            return device_id;
                        }
                    }
            }
            return -1;
        } catch (Exception e) {
            Log.e(TAG, "Error receiving device id:" + e.getMessage());
            return -1;
        }
    }

    private void getRegisteredSensors() {
        try {
            int device_id = getDeviceID();
            if (device_id == -1)
                return;
            URI uri = new URI(Constants.URL_GET_SENSORS.replaceAll("<id>", "" + device_id));
            SharedPreferences prefs = getSharedPreferences(Constants.PRIVATE_PREFS, MODE_PRIVATE);
            String cookie = prefs.getString(Constants.PREF_LOGIN_COOKIE, "");
            JSONObject response = SenseApi.getJSONObject(uri, cookie);
            if (response != null) {
                JSONArray sensorList = (JSONArray) response.get("sensors");
                if (sensorList != null) {
                    Editor editor = prefs.edit();
                    editor.putString(Constants.PREF_JSON_SENSOR_LIST, sensorList.toString());
                    editor.commit();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in retrieving registered sensors:" + e.getMessage());
        }
    }

    /**
     * Gets the status of the sensing components
     * 
     * @return the new status
     */

    private int getStatus() {

        int status = 0;

        status = this.started ? status + Constants.STATUSCODE_RUNNING : status;
        status = isLoggedIn ? status + Constants.STATUSCODE_CONNECTED : status;
        status = this.statusPhoneState ? status + Constants.STATUSCODE_PHONESTATE : status;
        status = this.statusLocation ? status + Constants.STATUSCODE_LOCATION : status;
        status = this.statusAmbience ? status + Constants.STATUSCODE_AMBIENCE : status;
        status = this.statusPopQuiz ? status + Constants.STATUSCODE_QUIZ : status;
        status = this.statusDeviceProx ? status + Constants.STATUSCODE_DEVICE_PROX : status;
        status = this.statusExternalSensors ? status + Constants.STATUSCODE_EXTERNAL : status;
        status = this.statusMotion ? status + Constants.STATUSCODE_MOTION : status;

        return status;
    }

    private void initFields() {
        this.telMan = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

        // listeners
        this.deviceProximity = new DeviceProximity(this);
        // this.locListener = new LocationSensor(this);
        this.motionSensor = new MotionSensor(this);
        this.psl = new SensePhoneState(this);
        this.noiseSensor = new NoiseSensor(this);
        this.proximitySensor = new ProximitySensor(this);
        this.lightSensor = new LightSensor(this);
        this.pressureSensor = new PressureSensor(this);
        this.phoneActivitySensor = new PhoneActivitySensor(this);
        this.pressureSensor = new PressureSensor(this);
        this.phoneActivitySensor = new PhoneActivitySensor(this);
        this.pressureSensor = new PressureSensor(this);
        this.phoneActivitySensor = new PhoneActivitySensor(this);
        this.batterySensor = new BatterySensor(this);
        this.es_bioHarness = new ZephyrBioHarness(this);
        this.es_HxM = new ZephyrHxM(this);

        // statuses
        this.started = false;
        this.statusDeviceProx = false;
        this.statusLocation = false;
        this.statusMotion = false;
        this.statusAmbience = false;
        this.statusPhoneState = false;
        this.statusPopQuiz = false;
        this.statusExternalSensors = false;
    }

    private boolean login(String email, String pass) {
        try {
            URL url = new URL(Constants.URL_LOGIN);
            JSONObject user = new JSONObject();
            user.put("username", email);
            user.put("password", pass);
            HashMap<String, String> response = SenseApi.sendJson(url, user, "POST", "");
            if (response == null)
                return false;

            final Editor editor = getSharedPreferences(Constants.PRIVATE_PREFS, MODE_PRIVATE)
                    .edit();
            if (response.get("http response code").compareToIgnoreCase("200") != 0) {
                // incorrect login
                Log.e(TAG,
                        "CommonSense login incorrect! response code:"
                                + response.get("http response code"));
                editor.putBoolean(email + "_ok", false);
                editor.putString(Constants.PREF_LOGIN_COOKIE, "");
                editor.commit();
                return false;
            }

            if (response.get("set-cookie") == null) {
                // incorrect login
                Log.e(TAG, "CommonSense login incorrect! no cookie");
                editor.putBoolean(email + "_ok", false);
                editor.putString(Constants.PREF_LOGIN_COOKIE, "");
                editor.commit();
                return false;
            }

            String cookie = response.get("set-cookie");
            Log.d(TAG, "CommonSense login ok!");
            editor.putBoolean(email + "_ok", true);
            editor.putString(Constants.PREF_LOGIN_COOKIE, cookie);
            editor.commit();
            return true;

        } catch (Exception e) {
            Log.e(TAG, "IOException during login:" + e.getMessage());
            final Editor editor = getSharedPreferences(Constants.PRIVATE_PREFS, MODE_PRIVATE)
                    .edit();
            editor.putBoolean(email + "_ok", false);
            editor.putString(Constants.PREF_LOGIN_COOKIE, "");
            editor.commit();
            return false;
        }
    }

    private void notifySenseLogin(boolean error) {
        int icon = R.drawable.ic_status_sense;

        if (error) {
            icon = R.drawable.ic_status_sense_disabled;
        }
        final long when = System.currentTimeMillis();
        final Notification note = new Notification(icon, null, when);
        note.flags = Notification.FLAG_NO_CLEAR;

        // extra info text is shown when the statusbar is opened
        final CharSequence contentTitle = "Sense service";
        CharSequence contentText = "";
        if (error) {
            contentText = "Trying to log in...";
        } else {
            SharedPreferences prefs = this.getSharedPreferences(Constants.PRIVATE_PREFS,
                    Context.MODE_PRIVATE);
            contentText = "Logged in as " + prefs.getString(Constants.PREF_LOGIN_MAIL, "UNKNOWN");
        }
        final Intent notifIntent = new Intent("nl.sense_os.app.SenseApp");
        notifIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notifIntent, 0);
        note.setLatestEventInfo(getApplicationContext(), contentTitle, contentText, contentIntent);

        final NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mgr.notify(NOTIF_ID, note);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");

        this.started = true;

        return this.binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        // Init stuff
        initFields();

        // make service as important as regular activities
        startForegroundCompat();

        // Register the receiver for SCREEN OFF events
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenOffListener, filter);
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
        this.started = false;
        stopForegroundCompat();
    }

    /**
     * Restarts the service in the same state as it was the previous time it was stopped.
     */
    private void onLogIn() {
        Log.d(TAG, "Logged in...");

        // set main status
        this.started = true;

        // Retrieve the online registered sensor list
        getRegisteredSensors();

        // save login time
        final SharedPreferences prefs = getSharedPreferences(Constants.STATUSPREFS,
                MODE_WORLD_WRITEABLE);
        prefs.edit().putBoolean(Constants.PREF_FIRSTLOGIN, false).commit();

        // restart individual sensing components
        startSensorModules();

        // start database leeglepelaar
        startTransmitJobs();

        // start the periodic checks of the feedback sensor
        startFeedbackCheck();

        // send broadcast that something has changed in the status
        sendBroadcast(new Intent(ACTION_SERVICE_BROADCAST));
    }

    /**
     * Stops any running sensing services, including the main service. Saves the previous state in
     * the preferences.
     */
    private void onLogOut() {
        // check if we were actually logged to prevent overwriting the last active state..
        if (SenseService.isLoggedIn) {
            Log.d(TAG, "Logged out...");

            // update login status
            SenseService.isLoggedIn = false;
            notifySenseLogin(true);

            // stop active sensing components
            if (true == this.statusDeviceProx) {
                toggleDeviceProx(false);
            }
            if (true == this.statusMotion) {
                toggleMotion(false);
            }
            if (true == this.statusLocation) {
                toggleLocation(false);
            }
            if (true == this.statusAmbience) {
                toggleAmbience(false);
            }
            if (true == this.statusPhoneState) {
                togglePhoneState(false);
            }
            if (true == this.statusPopQuiz) {
                togglePopQuiz(false);
            }
            if (true == this.statusExternalSensors) {
                toggleExternalSensors(false);
            }
        }

        stopFeedbackCheck();
        stopTransmitJobs();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.d(TAG, "onLowMemory");
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.d(TAG, "onRebind");
    }

    /**
     * Deprecated method for starting the service, used in 1.6 and older.
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

    private void onStartCompat(Intent intent, int flags, int startId) {

        boolean relogin = true;
        if (null != intent) {
            relogin = intent.getBooleanExtra(ACTION_RELOGIN, false);
        }

        // try to login immediately
        if (false == isLoggedIn || relogin) {
            senseServiceLogin();
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

    private boolean register(String email, String pass) {
        try {
            URL url = new URL(Constants.URL_REG);
            final JSONObject newUser = new JSONObject();
            JSONObject user = new JSONObject();
            user.put("username", email);
            user.put("password", pass);
            user.put("email", email);
            newUser.put("user", user);
            HashMap<String, String> response = SenseApi.sendJson(url, newUser, "POST", "");
            if (response == null) {
                return false;
            }
            if (response.get("http response code").compareToIgnoreCase("201") != 0) {
                Log.e(TAG, "Error got response code:" + response.get("http response code"));
                return false;
            }

            Log.d(TAG, "CommonSense registration: Successful");
        } catch (final IOException e) {
            Log.e(TAG, "IOException during registration!", e);
            return false;
        } catch (final IllegalAccessError e) {
            Log.e(TAG, "IllegalAccessError during registration!", e);
            return false;
        } catch (JSONException e) {
            Log.e(TAG, "JSONException during registration!", e);
            return false;
        }
        return true;
    }

    /**
     * Tries to login using the email and password from the private preferences and updates the
     * <code>sLoggedIn</code> status accordingly. NB: blocks the calling thread.
     * 
     * @return <code>true</code> if successful.
     */
    public boolean senseServiceLogin() {
        // show notification that we are not logged in (yet)
        notifySenseLogin(true);

        // clear cached settings of the previous user (i.e. device id)
        SharedPreferences prefs = getSharedPreferences(Constants.PRIVATE_PREFS, MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putInt(Constants.PREF_DEVICE_ID, -1);
        editor.commit();

        // try to login
        final String email = prefs.getString(Constants.PREF_LOGIN_MAIL, "");
        final String pass = prefs.getString(Constants.PREF_LOGIN_PASS, "");
        if ((email.length() > 0) && (pass.length() > 0) && login(email, pass)) {
            // logged in successfully
            notifySenseLogin(false);
            isLoggedIn = true;
            onLogIn();

        } else {
            Log.d(TAG, "Login failed");

            isLoggedIn = false;
        }

        return isLoggedIn;
    }

    /**
     * Tries to register a new user using the email and password from the private preferences and
     * updates the <code>sLoggedIn</code> status accordingly. NB: blocks the calling thread.
     * 
     * @return <code>true</code> if successful.
     */
    public boolean senseServiceRegister() {
        final SharedPreferences prefs = getSharedPreferences(Constants.PRIVATE_PREFS, MODE_PRIVATE);
        final String email = prefs.getString(Constants.PREF_LOGIN_MAIL, "");
        final String pass = prefs.getString(Constants.PREF_LOGIN_PASS, "");
        Log.d(TAG, "Registering... Email: " + email + ", pass: " + "********");
        if ((email.length() > 0) && (pass.length() > 0) && register(email, pass)
                && senseServiceLogin()) {
            // Registration and Login successful
            notifySenseLogin(false);
            isLoggedIn = true;
        } else {
            isLoggedIn = false;
        }
        return isLoggedIn;
    }

    private void showToast(final String msg) {
        // show informational Toast
        this.toastHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SenseService.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Starts the checks that periodically check if the service is still alive. Should be started
     * immediately after creation.
     */
    private void startAliveCheck() {

        // put alive status in the preferences
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final Editor editor = prefs.edit();
        editor.putBoolean(Constants.PREF_ALIVE, true);
        editor.commit();

        /* start the checks that periodically check if the service should be alive */
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
    private void startFeedbackCheck() {
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
     * that the service is running, a notification is shown.
     */
    private void startForegroundCompat() {

        startAliveCheck();

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

    private void startSensorModules() {
        final SharedPreferences prefs = getSharedPreferences(Constants.STATUSPREFS,
                MODE_WORLD_WRITEABLE);
        if (started && prefs.getBoolean(Constants.PREF_STATUS_MAIN, false)) {

            if (prefs.getBoolean(Constants.PREF_STATUS_PHONESTATE, false)) {
                Log.d(TAG, "Restart phone state component...");
                togglePhoneState(true);
            }
            if (prefs.getBoolean(Constants.PREF_STATUS_LOCATION, false)) {
                Log.d(TAG, "Restart location component...");
                toggleLocation(true);
            }
            if (prefs.getBoolean(Constants.PREF_STATUS_AMBIENCE, false)) {
                Log.d(TAG, "Restart ambience components...");
                toggleAmbience(true);
            }
            if (prefs.getBoolean(Constants.PREF_STATUS_MOTION, false)) {
                Log.d(TAG, "Restart motion component...");
                toggleMotion(true);
            }
            if (prefs.getBoolean(Constants.PREF_STATUS_DEV_PROX, false)) {
                Log.d(TAG, "Restart neighboring devices components...");
                toggleDeviceProx(true);
            }
            if (prefs.getBoolean(Constants.PREF_STATUS_EXTERNAL, false)) {
                Log.d(TAG, "Restart external sensors service...");
                toggleExternalSensors(true);
            }
            if (prefs.getBoolean(Constants.PREF_STATUS_POPQUIZ, false)) {
                Log.d(TAG, "Restart popquiz component...");
                togglePopQuiz(true);
            }
        }
    }

    /**
     * Start periodic broadcast to trigger the MsgHandler to flush its buffer to CommonSense.
     */
    private void startTransmitJobs() {
        Intent alarm = new Intent(this, DataTransmitter.class);
        PendingIntent operation = PendingIntent.getBroadcast(this, DataTransmitter.REQID, alarm, 0);
        AlarmManager mgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        mgr.cancel(operation);
        mgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), operation);
    }

    private void stopAliveCheck() {
        // remove alive status in the preferences
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final Editor editor = prefs.edit();
        editor.putBoolean(Constants.PREF_ALIVE, false);
        editor.commit();

        /* stop the alive check broadcasts */
        final Intent alarmIntent = new Intent(AliveChecker.ACTION_CHECK_ALIVE);
        final PendingIntent alarmOp = PendingIntent.getBroadcast(this,
                AliveChecker.REQ_CHECK_ALIVE, alarmIntent, 0);
        final AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mgr.cancel(alarmOp);
    }

    private void stopFeedbackCheck() {

        /* stop the feedback check broadcasts */
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

        stopAliveCheck();

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

    private void stopTransmitJobs() {
        Intent alarm = new Intent(this, DataTransmitter.class);
        PendingIntent operation = PendingIntent.getBroadcast(this, DataTransmitter.REQID, alarm, 0);
        AlarmManager mgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        mgr.cancel(operation);
    }

    private void toggleAmbience(boolean active) {

        if (active != this.statusAmbience) {
            this.statusAmbience = active;

            final TelephonyManager telMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (true == active) {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                final int rate = Integer.parseInt(prefs.getString(Constants.PREF_SAMPLE_RATE, "0"));
                int interval = -1;
                switch (rate) {
                    case -2 : // real time
                        interval = -1;
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
                        Log.e(TAG, "Unexpected sample rate preference.");
                }
                telMgr.listen(noiseSensor, PhoneStateListener.LISTEN_CALL_STATE);

                if (prefs.getBoolean(Constants.PREF_AMBIENCE_MIC, true))
                    this.noiseSensor.startListening(interval);
                if (prefs.getBoolean(Constants.PREF_AMBIENCE_LIGHT, true))
                    this.lightSensor.startLightSensing(interval);

            } else {
                telMgr.listen(noiseSensor, PhoneStateListener.LISTEN_NONE);
                this.noiseSensor.stopListening();
                this.lightSensor.stopLightSensing();
            }
        }
    }

    private void toggleDeviceProx(boolean active) {

        if (active != this.statusDeviceProx) {
            this.statusDeviceProx = active;

            if (true == active) {
                // showToast(getString(R.string.toast_toggle_dev_prox));
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                final int rate = Integer.parseInt(prefs.getString(Constants.PREF_SAMPLE_RATE, "0"));
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
                deviceProximity.startEnvironmentScanning(interval);
            } else {
                deviceProximity.stopEnvironmentScanning();
            }
        }
    }

    private void toggleExternalSensors(boolean active) {

        if (active != this.statusExternalSensors) {
            this.statusExternalSensors = active;

            if (true == active) {
                // showToast(getString(R.string.toast_toggle_dev_prox));
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                final int rate = Integer.parseInt(prefs.getString(Constants.PREF_SAMPLE_RATE, "0"));
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
                }
                if (prefs.getBoolean(Constants.PREF_BIOHARNESS, false))
                    es_bioHarness.startBioHarness(interval);
                if (prefs.getBoolean(Constants.PREF_HXM, false))
                    es_HxM.startHxM(interval);
            } else {
                es_bioHarness.stopBioHarness();
                es_HxM.stopHxM();
            }
        }
    }

    private void toggleLocation(boolean active) {

        if (active != this.statusLocation) {
            this.statusLocation = active;

            final LocationManager locMgr = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (true == active) {

                if (locListener != null) {
                    Log.w(TAG, "Location listener is already working");
                    return;
                }

                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                final boolean gps = prefs.getBoolean(Constants.PREF_LOCATION_GPS, true);
                final boolean network = prefs.getBoolean(Constants.PREF_LOCATION_NETWORK, false);
                final int rate = Integer.parseInt(prefs.getString(Constants.PREF_SAMPLE_RATE, "0"));

                // set update parameters according to preference
                long minTime = -1;
                float minDistance = -1;
                if (gps || network) {
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
                    this.locListener = new LocationSensor(this);
                } else {
                    // GPS and network are disabled in the preferences
                    this.statusLocation = false;

                    // show informational Toast
                    showToast(getString(R.string.toast_location_noprovider));
                }

                // start listening to GPS and Network location
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
                locMgr.removeUpdates(this.locListener);
                this.locListener = null;
            }
        }
    }

    private void toggleMotion(boolean active) {

        if (active != this.statusMotion) {
            this.statusMotion = active;

            if (true == active) {

                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                final int rate = Integer.parseInt(prefs.getString(Constants.PREF_SAMPLE_RATE, "0"));
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
                motionSensor.startMotionSensing(interval);
            } else {
                motionSensor.stopMotionSensing();
            }
        }
    }

    private void togglePhoneState(boolean active) {

        if (active != this.statusPhoneState) {
            this.statusPhoneState = active;

            if (true == active) {
                // start listening to any phone connectivity updates
                this.telMan.listen(this.psl, PhoneStateListener.LISTEN_CALL_FORWARDING_INDICATOR
                        | PhoneStateListener.LISTEN_CALL_STATE
                        | PhoneStateListener.LISTEN_DATA_ACTIVITY
                        | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                        | PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR
                        | PhoneStateListener.LISTEN_SERVICE_STATE
                        | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                final int rate = Integer.parseInt(prefs.getString(Constants.PREF_SAMPLE_RATE, "0"));
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

                proximitySensor.startProximitySensing(interval);
                batterySensor.startBatterySensing(interval);
                pressureSensor.startPressureSensing(interval);
                phoneActivitySensor.startPhoneActivitySensing(interval);
            } else {
                // stop phone state listener
                telMan.listen(this.psl, PhoneStateListener.LISTEN_NONE);
                proximitySensor.stopProximitySensing();
                pressureSensor.stopPressureSensing();
                batterySensor.stopBatterySensing();
                phoneActivitySensor.stopPhoneActivitySensing();
            }
        }
    }

    private void togglePopQuiz(boolean active) {

        if (active != statusPopQuiz) {
            this.statusPopQuiz = active;
            final SenseAlarmManager mgr = new SenseAlarmManager(this);
            if (true == active) {

                // create alarm
                mgr.createSyncAlarm();
                this.statusPopQuiz = mgr.createEntry(0, 1);
            } else {

                // cancel alarm
                mgr.cancelEntry();
                mgr.cancelSyncAlarm();
            }
        }
    }

}
