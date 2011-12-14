/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.Map;

import nl.sense_os.service.ambience.LightSensor;
import nl.sense_os.service.ambience.NoiseSensor;
import nl.sense_os.service.ambience.PressureSensor;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Auth;
import nl.sense_os.service.constants.SensePrefs.Main.Advanced;
import nl.sense_os.service.constants.SensePrefs.Main.Ambience;
import nl.sense_os.service.constants.SensePrefs.Main.External;
import nl.sense_os.service.constants.SensePrefs.Main.Motion;
import nl.sense_os.service.constants.SensePrefs.Main.PhoneState;
import nl.sense_os.service.constants.SensePrefs.Status;
import nl.sense_os.service.constants.SenseUrls;
import nl.sense_os.service.deviceprox.DeviceProximity;
import nl.sense_os.service.external_sensors.OBD2Dongle;
import nl.sense_os.service.external_sensors.ZephyrBioHarness;
import nl.sense_os.service.external_sensors.ZephyrHxM;
import nl.sense_os.service.location.LocationSensor;
import nl.sense_os.service.motion.MotionSensor;
import nl.sense_os.service.phonestate.BatterySensor;
import nl.sense_os.service.phonestate.PhoneActivitySensor;
import nl.sense_os.service.phonestate.ProximitySensor;
import nl.sense_os.service.phonestate.SensePhoneState;

import org.json.JSONObject;

import android.app.Activity;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class SenseService extends Service {

    /**
     * BroadcastReceiver that listens for changes in the network connectivity and updates the logged
     * in status accordingly.
     */
    private class ConnectivityListener extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, Intent intent) {

            if (!state.isStarted()) {
                // Log.v(TAG, "Connectivity changed, but service is not activated...");
                return;
            }

            ConnectivityManager mgr = (ConnectivityManager) context
                    .getSystemService(CONNECTIVITY_SERVICE);
            final NetworkInfo info = mgr.getActiveNetworkInfo();
            if ((null != info) && info.isConnectedOrConnecting()) {

                // check that we are not logged in yet before logging in
                if (false == state.isLoggedIn()) {
                    // Log.v(TAG, "Regained connectivity! Try to log in...");

                    // thread for undisturbed execution
                    HandlerThread connectionThread = new HandlerThread("Connectivity thread");
                    connectionThread.start();
                    final Handler handler = new Handler(connectionThread.getLooper());

                    // post login task
                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            login();
                            handler.getLooper().quit();
                        }
                    });

                } else {
                    // still connected, stay logged in
                }

            } else {
                // login not possible without connection
                // Log.v(TAG, "Lost connectivity! Updating login status...");
                state.setLoggedIn(false);
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

            SharedPreferences prefs = getSharedPreferences(SensePrefs.MAIN_PREFS, MODE_PRIVATE);
            boolean useFix = prefs.getBoolean(Motion.SCREENOFF_FIX, false);
            if (state.isMotionActive() && useFix) {
                // wait half a second and re-register
                Runnable motionThread = new Runnable() {

                    @Override
                    public void run() {
                        // Unregisters the motion listener and registers it again.
                        // Log.v(TAG, "Screen went off, re-registering the Motion sensor");
                        toggleMotion(false);
                        toggleMotion(true);
                    };
                };

                new Handler().postDelayed(motionThread, 500);
            }
        }
    }

    /**
     * Implementation of the service's AIDL interface.
     */
    private class SenseServiceStub extends ISenseService.Stub {

        private static final String TAG = "SenseServiceStub";

        @Override
        public int changeLogin(String username, String password) throws RemoteException {
            // Log.v(TAG, "Change login");
            return SenseService.this.changeLogin(username, password);
        }

        @Override
        public boolean getPrefBool(String key, boolean defValue) throws RemoteException {
            // Log.v(TAG, "Get preference: " + key);
            SharedPreferences prefs = getSharedPreferences(SensePrefs.MAIN_PREFS, MODE_PRIVATE);
            if (key.equals(Status.AMBIENCE) || key.equals(Status.DEV_PROX)
                    || key.equals(Status.EXTERNAL) || key.equals(Status.LOCATION)
                    || key.equals(Status.MAIN) || key.equals(Status.MOTION)
                    || key.equals(Status.PHONESTATE) || key.equals(Status.POPQUIZ)
                    || key.equals(Status.AUTOSTART)) {
                prefs = getSharedPreferences(SensePrefs.STATUS_PREFS, MODE_PRIVATE);
            } else if (key.equals(Auth.DEV_MODE)) {
                prefs = getSharedPreferences(SensePrefs.AUTH_PREFS, MODE_PRIVATE);
            }

            // return the preference value
            try {
                return prefs.getBoolean(key, defValue);
            } catch (ClassCastException e) {
                return defValue;
            }
        }

        @Override
        public float getPrefFloat(String key, float defValue) throws RemoteException {
            // Log.v(TAG, "Get preference: " + key);
            SharedPreferences prefs = getSharedPreferences(SensePrefs.MAIN_PREFS, MODE_PRIVATE);
            try {
                return prefs.getFloat(key, defValue);
            } catch (ClassCastException e) {
                return defValue;
            }
        }

        @Override
        public int getPrefInt(String key, int defValue) throws RemoteException {
            // Log.v(TAG, "Get preference: " + key);
            SharedPreferences prefs = getSharedPreferences(SensePrefs.MAIN_PREFS, MODE_PRIVATE);
            try {
                return prefs.getInt(key, defValue);
            } catch (ClassCastException e) {
                return defValue;
            }
        }

        @Override
        public long getPrefLong(String key, long defValue) throws RemoteException {
            // Log.v(TAG, "Get preference: " + key);
            SharedPreferences prefs = getSharedPreferences(SensePrefs.MAIN_PREFS, MODE_PRIVATE);
            if (key.equals(Auth.SENSOR_LIST_TIME)) {
                prefs = getSharedPreferences(SensePrefs.AUTH_PREFS, MODE_PRIVATE);
            }

            try {
                return prefs.getLong(key, defValue);
            } catch (ClassCastException e) {
                return defValue;
            }
        }

        @Override
        public String getPrefString(String key, String defValue) throws RemoteException {
            // Log.v(TAG, "Get preference: " + key);
            SharedPreferences prefs = getSharedPreferences(SensePrefs.MAIN_PREFS, MODE_PRIVATE);
            if (key.equals(Auth.LOGIN_COOKIE) || key.equals(Auth.LOGIN_PASS)
                    || key.equals(Auth.LOGIN_USERNAME) || key.equals(Auth.SENSOR_LIST)
                    || key.equals(Auth.DEVICE_ID) || key.equals(Auth.PHONE_IMEI)
                    || key.equals(Auth.PHONE_TYPE)) {
                prefs = getSharedPreferences(SensePrefs.AUTH_PREFS, MODE_PRIVATE);
            }

            // return the preference value
            try {
                return prefs.getString(key, defValue);
            } catch (ClassCastException e) {
                return defValue;
            }
        }

        @Override
        public String getSessionId(String appSecret) throws RemoteException {
            try {
                return SenseApi.getSessionId(SenseService.this, appSecret);
            } catch (IllegalAccessException e) {
                throw new RemoteException();
            }
        }

        @Override
        public void getStatus(ISenseServiceCallback callback) throws RemoteException {
            callback.statusReport(state.getStatusCode());
        }

        @Override
        public int register(String username, String password, String name, String surname,
                String email, String mobile) throws RemoteException {
            return SenseService.this.register(username, password, name, surname, email, mobile);
        }

        @Override
        public void setPrefBool(String key, boolean value) throws RemoteException {
            // Log.v(TAG, "Set preference: '" + key + "': '" + value + "'");

            SharedPreferences prefs = getSharedPreferences(SensePrefs.MAIN_PREFS, MODE_PRIVATE);
            if (key.equals(Status.AMBIENCE) || key.equals(Status.DEV_PROX)
                    || key.equals(Status.EXTERNAL) || key.equals(Status.LOCATION)
                    || key.equals(Status.MAIN) || key.equals(Status.MOTION)
                    || key.equals(Status.PHONESTATE) || key.equals(Status.POPQUIZ)
                    || key.equals(Status.AUTOSTART)) {
                prefs = getSharedPreferences(SensePrefs.STATUS_PREFS, MODE_PRIVATE);
            } else if (key.equals(Auth.DEV_MODE)) {
                prefs = getSharedPreferences(SensePrefs.AUTH_PREFS, MODE_PRIVATE);
            }

            // store value
            boolean stored = prefs.edit().putBoolean(key, value).commit();
            if (stored == false) {
                Log.w(TAG, "Preference '" + key + "' not stored!");
            } else if (key.equals(Auth.DEV_MODE) && state.isLoggedIn()) {
                login();
            } else if (key.equals(Advanced.USE_COMMONSENSE)) {
                if (value) {
                    login();
                } else {
                    onLogOut();
                }
            }
        }

        @Override
        public void setPrefFloat(String key, float value) throws RemoteException {
            // Log.v(TAG, "Set preference: " + key + ": \'" + value + "\'");
            SharedPreferences prefs = getSharedPreferences(SensePrefs.MAIN_PREFS, MODE_PRIVATE);

            // store value
            boolean stored = prefs.edit().putFloat(key, value).commit();
            if (stored == false) {
                Log.w(TAG, "Preference " + key + " not stored!");
            }
        }

        @Override
        public void setPrefInt(String key, int value) throws RemoteException {
            // Log.v(TAG, "Set preference: " + key + ": \'" + value + "\'");
            SharedPreferences prefs = getSharedPreferences(SensePrefs.MAIN_PREFS, MODE_PRIVATE);

            // store value
            boolean stored = prefs.edit().putFloat(key, value).commit();
            if (stored == false) {
                Log.w(TAG, "Preference " + key + " not stored!");
            }
        }

        @Override
        public void setPrefLong(String key, long value) throws RemoteException {
            // Log.v(TAG, "Set preference: " + key + ": \'" + value + "\'");
            SharedPreferences prefs = getSharedPreferences(SensePrefs.MAIN_PREFS, MODE_PRIVATE);
            if (key.equals(Auth.SENSOR_LIST_TIME)) {
                prefs = getSharedPreferences(SensePrefs.AUTH_PREFS, MODE_PRIVATE);
            }

            // store value
            boolean stored = prefs.edit().putLong(key, value).commit();
            if (stored == false) {
                Log.w(TAG, "Preference " + key + " not stored!");
            }
        }

        @Override
        public void setPrefString(String key, String value) throws RemoteException {
            // Log.v(TAG, "Set preference: " + key + ": \'" + value + "\'");
            SharedPreferences prefs = getSharedPreferences(SensePrefs.MAIN_PREFS, MODE_PRIVATE);
            if (key.equals(Auth.LOGIN_COOKIE) || key.equals(Auth.LOGIN_PASS)
                    || key.equals(Auth.LOGIN_USERNAME) || key.equals(Auth.SENSOR_LIST)
                    || key.equals(Auth.DEVICE_ID) || key.equals(Auth.PHONE_IMEI)
                    || key.equals(Auth.PHONE_TYPE)) {
                prefs = getSharedPreferences(SensePrefs.AUTH_PREFS, MODE_PRIVATE);
            }

            // store value
            boolean stored = prefs.edit().putString(key, value).commit();
            if (stored == false) {
                Log.w(TAG, "Preference " + key + " not stored!");
            }

            // special check for sync and sample rate changes
            if (key.equals(SensePrefs.Main.SAMPLE_RATE)) {
                onSampleRateChange();
            } else if (key.equals(SensePrefs.Main.SYNC_RATE)) {
                onSyncRateChange();
            }
        }

        @Override
        public void toggleAmbience(boolean active) {
            // Log.v(TAG, "Toggle ambience: " + active);
            SharedPreferences prefs = getSharedPreferences(SensePrefs.STATUS_PREFS, MODE_PRIVATE);
            prefs.edit().putBoolean(Status.AMBIENCE, active).commit();
            SenseService.this.toggleAmbience(active);
        }

        @Override
        public void toggleDeviceProx(boolean active) {
            // Log.v(TAG, "Toggle neighboring devices: " + active);
            SharedPreferences prefs = getSharedPreferences(SensePrefs.STATUS_PREFS, MODE_PRIVATE);
            prefs.edit().putBoolean(Status.DEV_PROX, active).commit();
            SenseService.this.toggleDeviceProx(active);
        }

        @Override
        public void toggleExternalSensors(boolean active) {
            // Log.v(TAG, "Toggle external sensors: " + active);
            SharedPreferences prefs = getSharedPreferences(SensePrefs.STATUS_PREFS, MODE_PRIVATE);
            prefs.edit().putBoolean(Status.EXTERNAL, active).commit();
            SenseService.this.toggleExternalSensors(active);
        }

        @Override
        public void toggleLocation(boolean active) {
            // Log.v(TAG, "Toggle location: " + active);
            SharedPreferences prefs = getSharedPreferences(SensePrefs.STATUS_PREFS, MODE_PRIVATE);
            prefs.edit().putBoolean(Status.LOCATION, active).commit();
            SenseService.this.toggleLocation(active);
        }

        @Override
        public void toggleMain(boolean active) {
            // Log.v(TAG, "Toggle main: " + active);
            SharedPreferences prefs = getSharedPreferences(SensePrefs.STATUS_PREFS, MODE_PRIVATE);
            prefs.edit().putBoolean(Status.MAIN, active).commit();
            SenseService.this.toggleMain(active);
        }

        @Override
        public void toggleMotion(boolean active) {
            // Log.v(TAG, "Toggle motion: " + active);
            SharedPreferences prefs = getSharedPreferences(SensePrefs.STATUS_PREFS, MODE_PRIVATE);
            prefs.edit().putBoolean(Status.MOTION, active).commit();
            SenseService.this.toggleMotion(active);
        }

        @Override
        public void togglePhoneState(boolean active) {
            // Log.v(TAG, "Toggle phone state: " + active);
            SharedPreferences prefs = getSharedPreferences(SensePrefs.STATUS_PREFS, MODE_PRIVATE);
            prefs.edit().putBoolean(Status.PHONESTATE, active).commit();
            SenseService.this.togglePhoneState(active);
        }

        @Override
        public void togglePopQuiz(boolean active) {
            // Log.v(TAG, "Toggle questionnaire: " + active);
            SharedPreferences prefs = getSharedPreferences(SensePrefs.STATUS_PREFS, MODE_PRIVATE);
            prefs.edit().putBoolean(Status.POPQUIZ, active).commit();
            SenseService.this.togglePopQuiz(active);
        }
    }

    private static final String TAG = "Sense Service";

    /**
     * Intent action to force a re-login attempt when the service is started.
     */
    public static final String INTENT_EXTRA_RELOGIN = "action_relogin";

    /**
     * Intent action for broadcasts that the service state has changed.
     */
    public final static String ACTION_SERVICE_BROADCAST = "nl.sense_os.service.Broadcast";

    private final ISenseService.Stub binder = new SenseServiceStub();

    private ServiceStateHelper state;

    // broadcast receivers
    private final BroadcastReceiver screenOffListener = new ScreenOffListener();
    private final ConnectivityListener connectivityListener = new ConnectivityListener();

    private BatterySensor batterySensor;
    private DeviceProximity deviceProximity;
    private LightSensor lightSensor;
    private LocationSensor locListener;
    private MotionSensor motionSensor;
    private NoiseSensor noiseSensor;
    private PhoneActivitySensor phoneActivitySensor;
    private PressureSensor pressureSensor;
    private ProximitySensor proximitySensor;
    private SensePhoneState phoneStateListener;
    private ZephyrBioHarness es_bioHarness;
    private ZephyrHxM es_HxM;
    private OBD2Dongle es_obd2dongle;

    /**
     * Handler on main application thread to display toasts to the user.
     */
    private final Handler toastHandler = new Handler(Looper.getMainLooper());

    // separate threads for the sensing modules
    private HandlerThread ambienceThread, motionThread, deviceProxThread, extSensorsThread,
            locationThread, phoneStateThread;

    /**
     * Changes login of the Sense service. Removes "private" data of the previous user from the
     * preferences. Can be called by Activities that are bound to the service.
     * 
     * @return <code>true</code> if login was changed successfully
     */
    private int changeLogin(String username, String password) {

        // log out before changing to a new user
        onLogOut();

        // stop active sensing components
        stopSensorModules();

        // clear cached settings of the previous user (i.e. device id)
        final SharedPreferences authPrefs = getSharedPreferences(SensePrefs.AUTH_PREFS,
                MODE_PRIVATE);
        final Editor editor = authPrefs.edit();

        // save new username and password in the preferences
        editor.putString(Auth.LOGIN_USERNAME, username);

        boolean skipHash = getPackageName().equals("nl.sense_os.ivitality");
        if (!skipHash) {
            editor.putString(Auth.LOGIN_PASS, SenseApi.hashPassword(password));
        } else {
            Log.w(TAG, "Skip password hashing!");
            editor.putString(Auth.LOGIN_PASS, password);
        }

        // remove old session data
        editor.remove(Auth.DEVICE_ID);
        editor.remove(Auth.DEVICE_TYPE);
        editor.remove(Auth.LOGIN_COOKIE);
        editor.remove(Auth.SENSOR_LIST);
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
            String versionName = URLEncoder.encode(packageInfo.versionName);
            Log.i(TAG, "Running Sense Platform version '" + versionName + "'");

            if (versionName.equals("unstable")) {
                return;
            }

            String url = SenseUrls.VERSION + "?version=" + versionName;
            Map<String, String> response = SenseApi.request(this, url, null, null);
            JSONObject content = new JSONObject(response.get("content"));

            if (content.getString("message").length() > 0) {
                Log.i(TAG, "Newer Sense Platform version available: " + content.toString());
                showToast(content.getString("message"));
            }

        } catch (Exception e) {
            if (null != e.getMessage()) {
                Log.e(TAG, "Failed to get Sense Platform version! Message: " + e.getMessage());
            } else {
                Log.e(TAG, "Failed to get Sense Platform version!", e);
            }
        }
    }

    /**
     * Tries to login using the username and password from the private preferences and updates the
     * {@link #isLoggedIn} status accordingly. Can also be called from Activities that are bound to
     * the service.
     * 
     * @return 0 if login completed successfully, -2 if login was forbidden, and -1 for any other
     *         errors.
     */
    private int login() {
        // Log.v(TAG, "Log in...");

        // check that we are actually allowed to log in
        SharedPreferences mainPrefs = getSharedPreferences(SensePrefs.MAIN_PREFS, MODE_PRIVATE);
        boolean allowed = mainPrefs.getBoolean(Advanced.USE_COMMONSENSE, true);
        if (!allowed) {
            Log.w(TAG, "Not logging in. Use of CommonSense is disabled.");
            return -1;
        }

        // get login parameters from the preferences
        SharedPreferences authPrefs = getSharedPreferences(SensePrefs.AUTH_PREFS, MODE_PRIVATE);
        final String username = authPrefs.getString(Auth.LOGIN_USERNAME, null);
        final String pass = authPrefs.getString(Auth.LOGIN_PASS, null);

        // try to log in
        int result = -1;
        if ((username != null) && (pass != null)) {
            try {
                result = SenseApi.login(this, username, pass);
            } catch (Exception e) {
                Log.w(TAG, "Exception during login: '" + e.getMessage() + "'. Connection problems?");
                // handle result later
            }

            if (0 == result) {
                // logged in successfully
                state.setLoggedIn(true);
                onLogIn();
            } else if (-2 == result) {
                Log.w(TAG, "Login forbidden!");
                state.setLoggedIn(false);
            } else {
                Log.w(TAG, "Login failed!");
                state.setLoggedIn(false);
            }

        } else {
            Log.w(TAG, "Cannot login: username or password unavailable... Username: " + username
                    + ", password: " + pass);
            state.setLoggedIn(false);
        }

        return result;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Log.v(TAG, "onBind...");
        return binder;
    }

    /**
     * Does nothing except poop out a log message. The service is really started in onStart,
     * otherwise it would also start when an activity binds to it.
     */
    @Override
    public void onCreate() {
        // Log.v(TAG, "---------->  Sense Platform service is being created...  <----------");
        super.onCreate();

        state = ServiceStateHelper.getInstance(this);

        // register broadcast receiver for login in case of Internet connection changes
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectivityListener, filter);
    }

    @Override
    public void onDestroy() {
        // Log.v(TAG, "----------> Sense Platform service is being destroyed... <----------");

        // stop listening for possibility to login
        try {
            unregisterReceiver(connectivityListener);
        } catch (IllegalArgumentException e) {
            // Log.d(TAG, "Ignoring exception when trying to unregister connectivity listener");
        }

        // stop listening to screen off receiver
        try {
            unregisterReceiver(screenOffListener);
        } catch (IllegalArgumentException e) {
            // Log.d(TAG, "Ignoring exception when trying to unregister screen off listener");
        }

        // stop active sensing components
        stopSensorModules();

        // update login status
        onLogOut();

        // stop the main service
        stopForegroundCompat();

        super.onDestroy();
    }

    /**
     * Performs tasks after successful login: gets list of registered sensors; starts the sensing
     * modules in the same state as before logout; starts periodic alarms for data transmission.
     * Method is synchronized to make sure {@link SenseApi#getRegisteredSensors(Context)} is only
     * called by one thread at a time.
     */
    private void onLogIn() {
        Log.i(TAG, "Logged in!");

        // start database leeglepelaar
        DataTransmitter.scheduleTransmissions(this);

        checkVersion();
    }

    /**
     * Performs cleanup tasks when the service is logged out: stops any running sensing modules;
     * updates the status bar notification; stops the periodic alarms for data transmission.
     */
    private void onLogOut() {

        // update login status
        state.setLoggedIn(false);

        DataTransmitter.stopTransmissions(this);

        // completely stop the MsgHandler service
        stopService(new Intent(getString(R.string.action_sense_new_data)));
        stopService(new Intent(getString(R.string.action_sense_send_data)));
    }

    private void onSampleRateChange() {
        // Log.v(TAG, "Sample rate changed...");
        if (state.isStarted()) {
            stopSensorModules();
            startSensorModules();
        }
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
    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        // Log.v(TAG, "onStart...");

        HandlerThread startThread = new HandlerThread("Start thread",
                Process.THREAD_PRIORITY_FOREGROUND);
        startThread.start();
        new Handler(startThread.getLooper()) {

            @Override
            public void handleMessage(Message msg) {

                try {
                    final SharedPreferences prefs = getSharedPreferences(SensePrefs.STATUS_PREFS,
                            MODE_PRIVATE);
                    state.setStarted(prefs.getBoolean(Status.MAIN, true));
                    if (false == state.isStarted()) {
                        Log.w(TAG, "Sense service was started when the main status is not set!");
                        stopForegroundCompat();
                        return;
                    }

                    // make service as important as regular activities
                    if (false == state.isForeground()) {
                        startForegroundCompat();
                    }

                    // intent is null when the Service is recreated by Android after it was killed
                    boolean relogin = true;
                    if (null != intent) {
                        relogin = intent.getBooleanExtra(INTENT_EXTRA_RELOGIN, false);
                    }

                    // try to login immediately
                    if ((false == state.isLoggedIn()) || relogin) {
                        login();
                    } else {
                        checkVersion();
                    }

                    // restart the individual modules
                    startSensorModules();

                } finally {
                    getLooper().quit();
                }
            };
        }.sendEmptyMessage(0);

        return START_NOT_STICKY;
    }

    private void onSyncRateChange() {
        // Log.v(TAG, "Sync rate changed...");
        if (state.isStarted()) {
            DataTransmitter.scheduleTransmissions(this);
        }

        // update any widgets
        startService(new Intent(getString(R.string.action_widget_update)));
    }

    /**
     * Tries to register a new user using the username and password from the private preferences and
     * updates the {@link #isLoggedIn} status accordingly. Can also be called from Activities that
     * are bound to the service.
     * 
     * @param mobile
     * @param email
     * @param surname
     * @param name
     * 
     * @return 0 if registration completed successfully, -2 if the user already exists, and -1 for
     *         any other errors.
     */
    private int register(String username, String password, String name, String surname,
            String email, String mobile) {

        // log out before registering a new user
        onLogOut();

        // stop active sensing components
        stopSensorModules();

        String hashPass = SenseApi.hashPassword(password);

        // clear cached settings of the previous user (i.e. device id)
        final SharedPreferences authPrefs = getSharedPreferences(SensePrefs.AUTH_PREFS,
                MODE_PRIVATE);
        final Editor authEditor = authPrefs.edit();

        // save username and password in preferences
        authEditor.putString(Auth.LOGIN_USERNAME, username);
        authEditor.putString(Auth.LOGIN_PASS, hashPass);

        // remove old session data
        authEditor.remove(Auth.DEVICE_ID);
        authEditor.remove(Auth.DEVICE_TYPE);
        authEditor.remove(Auth.LOGIN_COOKIE);
        authEditor.remove(Auth.SENSOR_LIST);
        authEditor.commit();

        // try to register
        int registered = -1;
        if ((null != username) && (null != password)) {
            // Log.v(TAG, "Registering... Username: " + username + ", password hash: " + hashPass);

            try {
                registered = SenseApi.registerUser(this, username, hashPass, name, surname, email,
                        mobile);
            } catch (Exception e) {
                Log.w(TAG, "Exception during registration: '" + e.getMessage()
                        + "'. Connection problems?");
                // handle result later
            }

            if (registered == 0) {
                login();
            } else {
                Log.w(TAG, "Registration failed");
                state.setLoggedIn(false);
            }
        } else {
            // Log.d(TAG, "Cannot register: username or password unavailable... Username: " +
            // username + ", password hash: " + hashPass);
            state.setLoggedIn(false);
        }
        return registered;
    }

    /**
     * Displays a Toast message using the process's main Thread.
     * 
     * @param message
     *            Toast message to display to the user
     */
    private void showToast(final String message) {
        toastHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SenseService.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Makes this service a foreground service, as important as 'real' activities. As a reminder
     * that the service is running, a notification is shown in the status bar.
     */
    private void startForegroundCompat() {
        // Log.v(TAG, "Enable foreground status...");

        // call startForeground in fancy way so old systems do not get confused by unknown methods
        try {
            // try newer startForeground(int, Notification) method
            final Class<?>[] startForegroundSignature = new Class[] { int.class, Notification.class };
            Method startForeground = getClass().getMethod("startForeground",
                    startForegroundSignature);

            // create notification
            Notification n = state.getStateNotification();

            // call the startForeground method
            Object[] startArgs = { Integer.valueOf(ServiceStateHelper.NOTIF_ID), n };
            startForeground.invoke(this, startArgs);

        } catch (NoSuchMethodException e) {
            // try older setForeground(boolean) method
            final Class<?>[] setForegroundSignature = new Class[] { boolean.class };
            Method setForeground = null;

            // call the setForeground methods
            try {
                setForeground = getClass().getMethod("setForeground", setForegroundSignature);

                // call the setForeground method
                Object[] startArgs = { Boolean.TRUE };
                setForeground.invoke(this, startArgs);

            } catch (Exception e1) {
                // Should not happen.
                Log.e(TAG, "Unable to invoke setForeground(boolean)", e1);
                return;
            }

        } catch (InvocationTargetException e) {
            // Should not happen.
            Log.e(TAG, "Unable to invoke startForeground(int, Notification)", e);
            return;
        } catch (IllegalAccessException e) {
            // Should not happen.
            Log.e(TAG, "Unable to invoke startForeground(int, Notification)", e);
            return;
        }

        state.setStarted(true);
        AliveChecker.scheduleChecks(this);

        // update state
        state.setForeground(true);
    }

    /**
     * Toggles the individual sensor modules according to the status that was stored in the
     * preferences.
     */
    private void startSensorModules() {

        // make sure the IDs of all sensors are known
        SensorRegistration.checkSensorsAtCommonSense(this);

        final SharedPreferences statusPrefs = getSharedPreferences(SensePrefs.STATUS_PREFS,
                MODE_PRIVATE);

        if (statusPrefs.getBoolean(Status.MAIN, false)) {

            toggleMain(true);

            if (statusPrefs.getBoolean(Status.PHONESTATE, false)) {
                // Log.d(TAG, "Restart phone state component...");
                togglePhoneState(true);
            }
            if (statusPrefs.getBoolean(Status.LOCATION, false)) {
                // Log.d(TAG, "Restart location component...");
                toggleLocation(true);
            }
            if (statusPrefs.getBoolean(Status.AMBIENCE, false)) {
                // Log.d(TAG, "Restart ambience components...");
                toggleAmbience(true);
            }
            if (statusPrefs.getBoolean(Status.MOTION, false)) {
                // Log.d(TAG, "Restart motion component...");
                toggleMotion(true);
            }
            if (statusPrefs.getBoolean(Status.DEV_PROX, false)) {
                // Log.d(TAG, "Restart neighboring devices components...");
                toggleDeviceProx(true);
            }
            if (statusPrefs.getBoolean(Status.EXTERNAL, false)) {
                // Log.d(TAG, "Restart external sensors service...");
                toggleExternalSensors(true);
            }
            if (statusPrefs.getBoolean(Status.POPQUIZ, false)) {
                // Log.d(TAG, "Restart popquiz component...");
                togglePopQuiz(true);
            }
        }

        // send broadcast that something has changed in the status
        sendBroadcast(new Intent(ACTION_SERVICE_BROADCAST));
    }

    /**
     * Lowers importance of this service back to normal again.
     */
    private void stopForegroundCompat() {
        // Log.v(TAG, "Remove foreground status...");

        state.setStarted(false);
        AliveChecker.stopChecks(this);

        try {
            // try the newer stopForeground(boolean) method
            final Class<?>[] stopForegroundSignature = new Class[] { boolean.class };
            Method stopForeground = getClass().getMethod("stopForeground", stopForegroundSignature);

            // call the stopForeground method
            Object[] stopArgs = { Boolean.TRUE };
            stopForeground.invoke(this, stopArgs);

        } catch (NoSuchMethodException e) {
            // try older setForeground(boolean) method
            final Class<?>[] setForegroundSignature = new Class[] { boolean.class };
            Method setForeground = null;

            // call the setForeground methods
            try {
                setForeground = getClass().getMethod("setForeground", setForegroundSignature);

                // call the setForeground method
                Object[] startArgs = { Boolean.FALSE };
                setForeground.invoke(this, startArgs);

            } catch (Exception e1) {
                // Should not happen.
                Log.e(TAG, "Unable to invoke setForeground(boolean)", e1);
                return;
            }

        } catch (InvocationTargetException e) {
            // Should not happen.
            Log.w(TAG, "Unable to invoke stopForeground", e);
            return;
        } catch (IllegalAccessException e) {
            // Should not happen.
            Log.w(TAG, "Unable to invoke stopForeground", e);
            return;
        }

        // update state field
        state.setForeground(false);
    }

    /**
     * Stops any running sensor modules.
     */
    private void stopSensorModules() {

        if (state.isDevProxActive()) {
            toggleDeviceProx(false);
        }
        if (state.isMotionActive()) {
            toggleMotion(false);
        }
        if (state.isLocationActive()) {
            toggleLocation(false);
        }
        if (state.isAmbienceActive()) {
            toggleAmbience(false);
        }
        if (state.isPhoneStateActive()) {
            togglePhoneState(false);
        }
        if (state.isQuizActive()) {
            togglePopQuiz(false);
        }
        if (state.isExternalActive()) {
            toggleExternalSensors(false);
        }

        // send broadcast that something has changed in the status
        sendBroadcast(new Intent(ACTION_SERVICE_BROADCAST));
    }

    private void toggleAmbience(boolean active) {

        if (active != state.isAmbienceActive()) {
            state.setAmbienceActive(active);

            if (true == active) {

                // check noise sensor presence
                if (null != noiseSensor) {
                    Log.w(TAG, "Noise sensor is already present!");
                    noiseSensor.disable();
                    noiseSensor = null;
                }

                // check light sensor presence
                if (null != lightSensor) {
                    Log.w(TAG, "Light sensor is already present!");
                    lightSensor.stopLightSensing();
                    lightSensor = null;
                }

                // check pressure sensor presence
                if (pressureSensor != null) {
                    Log.w(TAG, "pressure sensor is already present!");
                    pressureSensor.stopPressureSensing();
                    pressureSensor = null;
                }

                if ((ambienceThread != null) && ambienceThread.isAlive()) {
                    Log.w(TAG, "Ambience thread is already present! Quitting the thread...");
                    ambienceThread.getLooper().quit();
                    ambienceThread = null;
                }

                // get sample rate from preferences
                final SharedPreferences mainPrefs = getSharedPreferences(SensePrefs.MAIN_PREFS,
                        MODE_PRIVATE);
                final int rate = Integer.parseInt(mainPrefs.getString(SensePrefs.Main.SAMPLE_RATE,
                        "0"));
                int interval = -1;
                switch (rate) {
                case -2: // real time
                    interval = -1;
                    break;
                case -1: // often
                    interval = 10 * 1000;
                    break;
                case 0: // normal
                    interval = 60 * 1000;
                    break;
                case 1: // rarely (15 minutes)
                    interval = 15 * 60 * 1000;
                    break;
                default:
                    Log.e(TAG, "Unexpected sample rate preference.");
                }
                // special interval for Agostino
                final boolean agostinoMode = mainPrefs.getBoolean("agostino_mode", false);
                if (agostinoMode) {
                    Log.i(TAG, "Ambience sensor is in Agostino mode!");
                    interval = 60 * 1000;
                }
                final int finalInterval = interval;

                ambienceThread = new HandlerThread("Ambience thread",
                        Process.THREAD_PRIORITY_DEFAULT);
                ambienceThread.start();
                new Handler(ambienceThread.getLooper()).post(new Runnable() {

                    @Override
                    public void run() {

                        if (mainPrefs.getBoolean(Ambience.MIC, true)) {
                            noiseSensor = new NoiseSensor(SenseService.this);
                            noiseSensor.enable(finalInterval);
                        }
                        if (mainPrefs.getBoolean(Ambience.LIGHT, true)) {
                            lightSensor = new LightSensor(SenseService.this);
                            lightSensor.startLightSensing(finalInterval);
                        }
                        if (mainPrefs.getBoolean(Ambience.PRESSURE, true)) {
                            pressureSensor = new PressureSensor(SenseService.this);
                            pressureSensor.startPressureSensing(finalInterval);
                        }
                    }
                });

            } else {

                // stop sensing
                if (null != noiseSensor) {
                    noiseSensor.disable();
                    noiseSensor = null;
                }
                if (null != lightSensor) {
                    lightSensor.stopLightSensing();
                    lightSensor = null;
                }
                if (null != pressureSensor) {
                    pressureSensor.stopPressureSensing();
                    pressureSensor = null;
                }

                if ((ambienceThread != null) && ambienceThread.isAlive()) {
                    ambienceThread.getLooper().quit();
                    ambienceThread = null;
                }
            }
        }
    }

    private void toggleDeviceProx(boolean active) {

        if (active != state.isDevProxActive()) {
            state.setDevProxActive(active);

            if (true == active) {

                // check device proximity sensor presence
                if (null != deviceProximity) {
                    Log.w(TAG, "Device proximity sensor is already present!");
                    deviceProximity.stopEnvironmentScanning();
                    deviceProximity = null;
                }

                if ((deviceProxThread != null) && deviceProxThread.isAlive()) {
                    Log.w(TAG, "Device proximity thread is already present! Quitting the thread...");
                    deviceProxThread.getLooper().quit();
                    deviceProxThread = null;
                }

                // get sample rate
                final SharedPreferences mainPrefs = getSharedPreferences(SensePrefs.MAIN_PREFS,
                        MODE_PRIVATE);
                final int rate = Integer.parseInt(mainPrefs.getString(SensePrefs.Main.SAMPLE_RATE,
                        "0"));
                int interval = 1;
                switch (rate) {
                case -2:
                    interval = 1 * 1000;
                    break;
                case -1:
                    // often
                    interval = 60 * 1000;
                    break;
                case 0:
                    // normal
                    interval = 5 * 60 * 1000;
                    break;
                case 1:
                    // rarely (15 mins)
                    interval = 15 * 60 * 1000;
                    break;
                default:
                    Log.e(TAG, "Unexpected device proximity rate preference.");
                }
                final int finalInterval = interval;

                deviceProxThread = new HandlerThread("Device proximity thread",
                        Process.THREAD_PRIORITY_DEFAULT);
                deviceProxThread.start();
                new Handler(deviceProxThread.getLooper()).post(new Runnable() {

                    @Override
                    public void run() {
                        deviceProximity = new DeviceProximity(SenseService.this);

                        // start sensing
                        deviceProximity.startEnvironmentScanning(finalInterval);
                    }
                });

            } else {

                // stop sensing
                if (null != deviceProximity) {
                    deviceProximity.stopEnvironmentScanning();
                    deviceProximity = null;
                }

                if ((deviceProxThread != null) && deviceProxThread.isAlive()) {
                    deviceProxThread.getLooper().quit();
                    deviceProxThread = null;
                }
            }
        }
    }

    private void toggleExternalSensors(boolean active) {

        if (active != state.isExternalActive()) {
            state.setExternalActive(active);

            if (true == active) {

                // check BioHarness sensor presence
                if (null != es_bioHarness) {
                    Log.w(TAG, "Bioharness sensor is already present!");
                    es_bioHarness.stopBioHarness();
                    es_bioHarness = null;
                }

                // check HxM sensor presence
                if (null != es_HxM) {
                    Log.w(TAG, "HxM sensor is already present!");
                    es_HxM.stopHxM();
                    es_HxM = null;
                }

                // check OBD-II dongle presence
                if (null != es_obd2dongle) {
                    Log.w(TAG, "OBD-II dongle is already present!");
                    es_obd2dongle.stop();
                    es_obd2dongle = null;
                }

                if ((extSensorsThread != null) && extSensorsThread.isAlive()) {
                    Log.w(TAG, "Ext. sensors thread is already present! Quitting the thread...");
                    extSensorsThread.getLooper().quit();
                    extSensorsThread = null;
                }

                // get sample rate
                final SharedPreferences mainPrefs = getSharedPreferences(SensePrefs.MAIN_PREFS,
                        MODE_PRIVATE);
                final int rate = Integer.parseInt(mainPrefs.getString(SensePrefs.Main.SAMPLE_RATE,
                        "0"));
                int interval = 1;
                switch (rate) {
                case -2:
                    interval = 1 * 1000;
                    break;
                case -1:
                    // often
                    interval = 5 * 1000;
                    break;
                case 0:
                    // normal
                    interval = 60 * 1000;
                    break;
                case 1:
                    // rarely (15 minutes)
                    interval = 15 * 60 * 1000;
                    break;
                default:
                    Log.e(TAG, "Unexpected external sensor rate preference.");
                    return;
                }
                final int finalInterval = interval;

                extSensorsThread = new HandlerThread("Ext. sensors thread",
                        Process.THREAD_PRIORITY_DEFAULT);
                extSensorsThread.start();
                new Handler(extSensorsThread.getLooper()).post(new Runnable() {

                    @Override
                    public void run() {
                        if (mainPrefs.getBoolean(External.ZephyrBioHarness.MAIN, false)) {
                            es_bioHarness = new ZephyrBioHarness(SenseService.this);
                            es_bioHarness.startBioHarness(finalInterval);
                        }
                        if (mainPrefs.getBoolean(External.ZephyrHxM.MAIN, false)) {
                            es_HxM = new ZephyrHxM(SenseService.this);
                            es_HxM.startHxM(finalInterval);
                        }
                        if (mainPrefs.getBoolean(External.OBD2Dongle.MAIN, false)) {
                            es_obd2dongle = new OBD2Dongle(SenseService.this);
                            es_obd2dongle.start(finalInterval);
                        }
                    }
                });

            } else {

                // stop sensing
                if (null != es_bioHarness) {
                    Log.w(TAG, "Bioharness sensor is already present!");
                    es_bioHarness.stopBioHarness();
                    es_bioHarness = null;
                }

                // check HxM sensor presence
                if (null != es_HxM) {
                    Log.w(TAG, "HxM sensor is already present!");
                    es_HxM.stopHxM();
                    es_HxM = null;
                }

                // check OBD-II dongle presence
                if (null != es_obd2dongle) {
                    Log.w(TAG, "OBD-II dongle is already present!");
                    es_obd2dongle.stop();
                    es_obd2dongle = null;
                }

                if ((extSensorsThread != null) && extSensorsThread.isAlive()) {
                    extSensorsThread.getLooper().quit();
                    extSensorsThread = null;
                }
            }
        }
    }

    private void toggleLocation(boolean active) {

        if (active != state.isLocationActive()) {
            state.setLocationActive(active);

            if (true == active) {

                // check location sensor presence
                if (locListener != null) {
                    Log.w(TAG, "location sensor is already present!");
                    locListener.disable();
                    locListener = null;
                }

                if ((locationThread != null) && locationThread.isAlive()) {
                    Log.w(TAG, "Location thread is already present! Quitting the thread...");
                    locationThread.getLooper().quit();
                    locationThread = null;
                }

                // get sample rate
                final SharedPreferences mainPrefs = getSharedPreferences(SensePrefs.MAIN_PREFS,
                        MODE_PRIVATE);
                final int rate = Integer.parseInt(mainPrefs.getString(SensePrefs.Main.SAMPLE_RATE,
                        "0"));
                long minTime = -1;
                float minDistance = -1;
                switch (rate) {
                case -2: // real-time
                    minTime = 1000;
                    minDistance = 0;
                    break;
                case -1: // often
                    minTime = 30 * 1000;
                    minDistance = 0;
                    break;
                case 0: // normal
                    minTime = 5 * 60 * 1000;
                    minDistance = 0;
                    break;
                case 1: // rarely
                    minTime = 15 * 60 * 1000;
                    minDistance = 0;
                    break;
                default:
                    Log.e(TAG, "Unexpected commonsense rate: " + rate);
                    break;
                }
                // special interval for Agostino
                final boolean agostinoMode = mainPrefs.getBoolean("agostino_mode", false);
                if (agostinoMode) {
                    Log.i(TAG, "Location sensor is in Agostino mode!");
                    minTime = 60 * 1000;
                    minDistance = 100;
                }

                final long time = minTime;
                final float distance = minDistance;

                locationThread = new HandlerThread("Location thread",
                        Process.THREAD_PRIORITY_DEFAULT);
                locationThread.start();
                new Handler(locationThread.getLooper()).post(new Runnable() {

                    @Override
                    public void run() {
                        locListener = new LocationSensor(SenseService.this);
                        locListener.enable(time, distance);
                    }
                });

            } else {

                // stop location listener
                if (null != locListener) {
                    locListener.disable();
                    locListener = null;
                }

                if ((locationThread != null) && locationThread.isAlive()) {
                    locationThread.getLooper().quit();
                    locationThread = null;
                }
            }
        }
    }

    private void toggleMain(boolean active) {
        // Log.v(TAG, "Toggle main: " + active);

        if (true == active) {
            // properly start the service to start sensing
            if (!state.isStarted()) {
                Log.i(TAG, "Start service...");
                startService(new Intent(getString(R.string.action_sense_service)));
            }

        } else {
            if (state.isStarted()) {
                Log.i(TAG, "Stop service...");
            }
            onLogOut();
            stopSensorModules();
            stopForegroundCompat();
        }
    }

    private void toggleMotion(boolean active) {

        if (active != state.isMotionActive()) {
            state.setMotionActive(active);

            if (true == active) {

                // Register the receiver for SCREEN OFF events
                IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
                registerReceiver(screenOffListener, filter);

                // check motion sensor presence
                if (motionSensor != null) {
                    Log.w(TAG, "Motion sensor is already present! Stopping the sensor...");
                    motionSensor.stopMotionSensing();
                    motionSensor = null;
                }

                if ((motionThread != null) && motionThread.isAlive()) {
                    Log.w(TAG, "Motion thread is already present! Quitting the thread...");
                    motionThread.getLooper().quit();
                    motionThread = null;
                }

                // get sample rate
                final SharedPreferences mainPrefs = getSharedPreferences(SensePrefs.MAIN_PREFS,
                        MODE_PRIVATE);
                final int rate = Integer.parseInt(mainPrefs.getString(SensePrefs.Main.SAMPLE_RATE,
                        "0"));
                int interval = -1;
                switch (rate) {
                case -2: // real time
                    interval = 1 * 1000;
                    break;
                case -1: // often
                    interval = 5 * 1000;
                    break;
                case 0: // normal
                    interval = 60 * 1000;
                    break;
                case 1: // rarely (15 minutes)
                    interval = 15 * 60 * 1000;
                    break;
                default:
                    Log.e(TAG, "Unexpected commonsense rate: " + rate);
                    break;
                }
                // special interval for Agostino
                final boolean agostinoMode = mainPrefs.getBoolean("agostino_mode", false);
                if (agostinoMode) {
                    Log.i(TAG, "Motion sensor is in Agostino mode!");
                    interval = 1 * 1000;
                }

                final int finalInterval = interval;

                // instantiate the sensors on the main process thread
                motionThread = new HandlerThread("Motion thread", Process.THREAD_PRIORITY_DEFAULT);
                motionThread.start();
                new Handler(motionThread.getLooper()).post(new Runnable() {

                    @Override
                    public void run() {
                        motionSensor = new MotionSensor(SenseService.this);
                        motionSensor.startMotionSensing(finalInterval);
                    }
                });

            } else {

                // Unregister the receiver for SCREEN OFF events
                try {
                    unregisterReceiver(screenOffListener);
                } catch (IllegalArgumentException e) {
                    // Log.v(TAG, "Ignoring exception when unregistering screen off listener");
                }

                // stop sensing
                if (null != motionSensor) {
                    motionSensor.stopMotionSensing();
                    motionSensor = null;
                }

                // quit thread
                if (null != motionThread) {
                    motionThread.getLooper().quit();
                    motionThread = null;
                }
            }
        }
    }

    private void togglePhoneState(boolean active) {

        if (active != state.isPhoneStateActive()) {
            ServiceStateHelper.getInstance(this).setPhoneStateActive(active);

            if (true == active) {

                // check phone state sensor presence
                if (phoneStateListener != null) {
                    Log.w(TAG, "phone state sensor is already present!");
                    phoneStateListener.stopSensing();
                    phoneStateListener = null;
                }

                // check proximity sensor presence
                if (proximitySensor != null) {
                    Log.w(TAG, "proximity sensor is already present!");
                    proximitySensor.stopProximitySensing();
                    proximitySensor = null;
                }

                // check battery sensor presence
                if (batterySensor != null) {
                    Log.w(TAG, "battery sensor is already present!");
                    batterySensor.stopBatterySensing();
                    batterySensor = null;
                }

                // check phone activity sensor presence
                if (phoneActivitySensor != null) {
                    Log.w(TAG, "phone activity sensor is already present!");
                    phoneActivitySensor.stopPhoneActivitySensing();
                    phoneActivitySensor = null;
                }

                // chekc presence of other phone state thread
                if ((phoneStateThread != null) && phoneStateThread.isAlive()) {
                    phoneStateThread.getLooper().quit();
                    phoneStateThread = null;
                }

                // get sample rate
                final SharedPreferences mainPrefs = getSharedPreferences(SensePrefs.MAIN_PREFS,
                        MODE_PRIVATE);
                final int rate = Integer.parseInt(mainPrefs.getString(SensePrefs.Main.SAMPLE_RATE,
                        "0"));
                int interval = -1;
                switch (rate) {
                case -2: // real time
                    interval = 1 * 1000;
                    break;
                case -1: // often
                    interval = 10 * 1000;
                    break;
                case 0: // normal
                    interval = 60 * 1000;
                    break;
                case 1: // rarely (15 minutes)
                    interval = 15 * 60 * 1000;
                    break;
                default:
                    Log.e(TAG, "Unexpected commonsense rate: " + rate);
                    break;
                }
                final int finalInterval = interval;

                // start sensing on a separate thread
                phoneStateThread = new HandlerThread("Phone state thread",
                        Process.THREAD_PRIORITY_DEFAULT);
                phoneStateThread.start();
                new Handler(phoneStateThread.getLooper()).post(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            if (mainPrefs.getBoolean(PhoneState.BATTERY, true)) {
                                batterySensor = new BatterySensor(SenseService.this);
                                batterySensor.startBatterySensing(finalInterval);
                            }
                            if (mainPrefs.getBoolean(PhoneState.SCREEN_ACTIVITY, true)) {
                                phoneActivitySensor = new PhoneActivitySensor(SenseService.this);
                                phoneActivitySensor.startPhoneActivitySensing(finalInterval);
                            }
                            if (mainPrefs.getBoolean(PhoneState.PROXIMITY, true)) {
                                proximitySensor = new ProximitySensor(SenseService.this);
                                proximitySensor.startProximitySensing(finalInterval);
                            }
                            phoneStateListener = new SensePhoneState(SenseService.this);
                            phoneStateListener.startSensing(finalInterval);
                        } catch (Exception e) {
                            Log.e(TAG, "Phone state thread failed to start!");
                            togglePhoneState(false);
                        }
                    }
                });

            } else {

                // stop sensing
                if (null != phoneStateListener) {
                    phoneStateListener.stopSensing();
                    phoneStateListener = null;
                }
                if (null != proximitySensor) {
                    proximitySensor.stopProximitySensing();
                    proximitySensor = null;
                }
                if (null != batterySensor) {
                    batterySensor.stopBatterySensing();
                    batterySensor = null;
                }
                if (null != phoneActivitySensor) {
                    phoneActivitySensor.stopPhoneActivitySensing();
                    phoneActivitySensor = null;
                }
                if ((phoneStateThread != null) && phoneStateThread.isAlive()) {
                    phoneStateThread.getLooper().quit();
                    phoneStateThread = null;
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
