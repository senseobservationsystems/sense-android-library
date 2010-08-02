package nl.sense_os.service;

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
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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

import nl.sense_os.app.R;
import nl.sense_os.app.SenseSettings;
import nl.sense_os.service.deviceprox.DeviceProximity;
import nl.sense_os.service.location.LocationSensor;
import nl.sense_os.service.motion.MotionSensor;
import nl.sense_os.service.noise.NoiseSensor;
import nl.sense_os.service.phonestate.SensePhoneState;
import nl.sense_os.service.popquiz.SenseAlarmManager;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SenseService extends Service {
    private class LoginPossibility extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            ConnectivityManager mgr = (ConnectivityManager) context
                    .getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo info = mgr.getActiveNetworkInfo();
            if ((null != info) && (info.isConnectedOrConnecting())) {
                // check that we are not logged in yet before logging in
                if (false == SenseService.sLoggedIn) {
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

    private class SenseServiceStub extends ISenseService.Stub {

        public void getStatus(ISenseServiceCallback callback) {
            try {
                callback.statusReport(SenseService.this.getStatus());
            } catch (final RemoteException e) {
                Log.e(TAG, "RemoteException sending status report.", e);
            }
        }

        public boolean serviceLogin() throws RemoteException {
            return senseServiceLogin();
        }

        public boolean serviceRegister() throws RemoteException {
            return senseServiceRegister();
        }

        public String serviceResponse() {
            if (SenseService.this.msgHandler != null) {
                return SenseService.this.msgHandler.getHttpResponse();
            } else {
                return "";
            }
        }

        public void setDeviceId(String id) {
            final SharedPreferences prefs = getSharedPreferences(MsgHandler.PREF_MSG_HANDLER,
                    MODE_PRIVATE);
            final Editor editor = prefs.edit();
            editor.putString(MsgHandler.PREF_KEY_DEVICE_ID, id);
            editor.commit();
        }

        public void setUpdateFreq(int freq) {
            final SharedPreferences prefs = getSharedPreferences(MsgHandler.PREF_MSG_HANDLER,
                    MODE_PRIVATE);
            final Editor editor = prefs.edit();
            editor.putInt(MsgHandler.PREF_KEY_UPDATE, freq);
            editor.commit();
        }

        public void setUrl(String url) {
            final SharedPreferences prefs = getSharedPreferences(MsgHandler.PREF_MSG_HANDLER,
                    MODE_PRIVATE);
            final Editor editor = prefs.edit();
            editor.putString(MsgHandler.PREF_KEY_URL, url);
            editor.commit();
        }

        public void toggleDeviceProx(boolean active, ISenseServiceCallback callback) {
            SenseService.this.toggleDeviceProx(active);
            // this.getStatus(callback);
        }

        public void toggleLocation(boolean active, ISenseServiceCallback callback) {
            SenseService.this.toggleLocation(active);
            // this.getStatus(callback);
        }

        public void toggleMotion(boolean active, ISenseServiceCallback callback) {
            SenseService.this.toggleMotion(active);
            // this.getStatus(callback);
        }

        public void toggleNoise(boolean active, ISenseServiceCallback callback) {
            SenseService.this.toggleNoise(active);
            // this.getStatus(callback);
        }

        public void togglePhoneState(boolean active, ISenseServiceCallback callback) {
            SenseService.this.togglePhoneState(active);
            // this.getStatus(callback);
        }

        public void togglePopQuiz(boolean active, ISenseServiceCallback callback) {
            SenseService.this.togglePopQuiz(active);
            // this.getStatus(callback);
        }
    }

    public final static String ACTION_SERVICE_BROADCAST = "nl.sense_os.service.Broadcast";
    public static final String KEY_DEVICE_ID = "nl.sense_os.DeviceId";
    public static final String KEY_UPDATE_TIME = "nl.sense_os.UpdateTime";
    public static final String KEY_URL = "nl.sense_os.Url";
    private static final int NOTIF_ID = 1;
    private static final String PRIVATE_PREFS = SenseSettings.PRIVATE_PREFS;
    public static boolean sLoggedIn = false;
    public static final int STATUS_CONNECTED = 2;
    public static final int STATUS_DEVICE_PROX = 4;
    public static final int STATUS_LOCATION = 8;
    public static final int STATUS_MOTION = 16;
    public static final int STATUS_NOISE = 32;
    public static final int STATUS_PHONESTATE = 64;
    public static final int STATUS_QUIZ = 128;
    public static final int STATUS_RUNNING = 256;
    private static final String TAG = "SenseService";
    private final ISenseService.Stub binder = new SenseServiceStub();
    private DeviceProximity deviceProximity;
    private Handler handler;
    private LocationListener locListener;
    private LoginPossibility loginPossibility;
    private MotionSensor motionSensor;
    private MsgHandler msgHandler;
    private NoiseSensor noiseSensor;
    private PhoneStateListener psl; 
    private boolean started; 
    private boolean statusDeviceProx; 
    private boolean statusLocation;
    private boolean statusMotion;
    private boolean statusNoise;
    private boolean statusPhoneState;
    private boolean statusPopQuiz;
    private TelephonyManager telMan;

    public boolean checkPhoneRegistration() {
        boolean registered = false;
        try {
            final String imei = this.telMan.getDeviceId();
            final URI uri = new URI(SenseSettings.URL_CHECK_PHONE + "?uuid=" + imei);
            final HttpPost post = new HttpPost(uri);
            final SharedPreferences prefs = getSharedPreferences(PRIVATE_PREFS, MODE_PRIVATE);
            final String cookie = prefs.getString(SenseSettings.PREF_LOGIN_COOKIE, "");
            post.setHeader("Cookie", cookie);
            final HttpClient client = new DefaultHttpClient();

            // client.getConnectionManager().closeIdleConnections(2, TimeUnit.SECONDS);
            final HttpResponse response = client.execute(post);
            if (response == null) {
                return false;
            }

            final InputStream stream = response.getEntity().getContent();
            final String responseStr = convertStreamToString(stream);

            registered = responseStr.toLowerCase().contains("ok");

            Log.d(TAG, "CommonSense check phone registration: " + responseStr);
        } catch (final IOException e) {
            Log.e(TAG, "IOException checking phone registration!", e);
            return false;
        } catch (final IllegalAccessError e) {
            Log.e(TAG, "IllegalAccessError checking phone registration!", e);
            return false;
        } catch (final URISyntaxException e) {
            Log.e(TAG, "URISyntaxException checking phone registration!", e);
            return false;
        }
        return registered;
    }

    private String convertStreamToString(InputStream is) {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is), 8000);
        final StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    private int getStatus() {
        int status = 0;
        status = this.started ? status + STATUS_RUNNING : status;
        status = sLoggedIn ? status + STATUS_CONNECTED : status;
        status = this.statusPhoneState ? status + STATUS_PHONESTATE : status;
        status = this.statusLocation ? status + STATUS_LOCATION : status;
        status = this.statusNoise ? status + STATUS_NOISE : status;
        status = this.statusPopQuiz ? status + STATUS_QUIZ : status;
        status = this.statusDeviceProx ? status + STATUS_DEVICE_PROX : status;
        status = this.statusMotion ? status + STATUS_MOTION : status;
        return status;
    }

    private void initFields() {
        this.msgHandler = new MsgHandler(this);
        this.handler = new Handler(Looper.getMainLooper());
        this.loginPossibility = new LoginPossibility();
        this.telMan = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        
        // listeners
        this.deviceProximity = new DeviceProximity(msgHandler, this);
        this.locListener = new LocationSensor(this.msgHandler);
        this.motionSensor = new MotionSensor(this.msgHandler);
        this.psl = new SensePhoneState(this.msgHandler, this.telMan);
        this.noiseSensor = new NoiseSensor(this.msgHandler);
        
        // statuses
        this.started = false;
        this.statusDeviceProx = false;
        this.statusLocation = false;
        this.statusMotion = false;
        this.statusNoise = false;
        this.statusPhoneState = false;
        this.statusPopQuiz = false;       
    }

    private boolean login(String email, String pass) {
        boolean success = false;
        try {
            String cookie = "";
            final URI uri = new URI(SenseSettings.URL_LOGIN + "?email=" + email + "&password="
                    + pass);
            final HttpPost post = new HttpPost(uri);
            final HttpClient client = new DefaultHttpClient();
            // client.getConnectionManager().closeIdleConnections(2, TimeUnit.SECONDS);
            final HttpResponse response = client.execute(post);

            if (response != null) {
                cookie = response.getFirstHeader("Set-Cookie").getValue();
                final InputStream stream = response.getEntity().getContent();
                final String responseStr = convertStreamToString(stream);

                final SharedPreferences prefs = getSharedPreferences(PRIVATE_PREFS, MODE_PRIVATE);
                final Editor editor = prefs.edit();

                Log.d(TAG, "CommonSense login: " + responseStr);
                if (responseStr.toLowerCase().contains("ok")) {
                    editor.putBoolean(email + "_ok", true);
                    editor.putString(SenseSettings.PREF_LOGIN_COOKIE, cookie);
                    editor.commit();
                    success = true;
                } else {
                    // incorrect login
                    editor.putBoolean(email + "_ok", false);
                    editor.putString(SenseSettings.PREF_LOGIN_COOKIE, "");
                    editor.commit();
                    success = false;
                }
            }
        } catch (final IOException e) {
            Log.e(TAG, "IOException during login!", e);
            success = false;
        } catch (final IllegalAccessError e) {
            Log.e(TAG, "IllegalAccessError during login!", e);
            success = false;
        } catch (final URISyntaxException e) {
            Log.e(TAG, "URISyntaxException during login!", e);
            success = false;
        }
        return success;
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
            contentText = "Logged in.";
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
        Log.d(TAG, "onCreate.");

        // Init stuff
        initFields();

        // make service as important as regular activities        
        startForegroundCompat();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy.");

        // stop listening for possibility to login
        try {
            unregisterReceiver(this.loginPossibility);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Ignoring exception when trying to unregister login module...");
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
        Log.d(TAG, "onLogIn.");
        
        // set main status
        this.started = true;

        // restart individual sensing components
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final int status = prefs.getInt(SenseSettings.PREF_LAST_STATUS, 0);
        if ((status & STATUS_RUNNING) > 0) {

            if ((status & STATUS_DEVICE_PROX) > 0) {
                toggleDeviceProx(true);
            }
            if ((status & STATUS_LOCATION) > 0) {
                toggleLocation(true);
            }
            if ((status & STATUS_MOTION) > 0) {
                toggleMotion(true);
            }
            if ((status & STATUS_NOISE) > 0) {
                toggleNoise(true);
            }
            if ((status & STATUS_PHONESTATE) > 0) {
                togglePhoneState(true);
            }
            if ((status & STATUS_QUIZ) > 0) {
                togglePopQuiz(true);
            }
        }

        // send broadcast that something has changed in the status
        sendBroadcast(new Intent(ACTION_SERVICE_BROADCAST));
    }

    /**
     * Stops any running sensing services, including the main service. Saves the previous state in
     * the preferences.
     */
    private void onLogOut() {
        Log.d(TAG, "onLogOut.");

        // update login status
        SenseService.sLoggedIn = false;
        notifySenseLogin(true);

        // save the last status for good restarting of the service
        Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putInt(SenseSettings.PREF_LAST_STATUS, getStatus());
        editor.commit();

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
        if (true == this.statusNoise) {
            toggleNoise(false);
        }
        if (true == this.statusPhoneState) {
            togglePhoneState(false);
        }
        if (true == this.statusPopQuiz) {
            togglePopQuiz(false);
        }

        // send broadcast that something has changed in the status
        sendBroadcast(new Intent(ACTION_SERVICE_BROADCAST));
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.d(TAG, "onLowMemory.");
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.d(TAG, "onRebind.");
    }

    @Override
    public void onStart(Intent intent, int startid) {
        onStartCompat(intent, 0 , startid);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onStartCompat(intent, flags, startId);
        return START_STICKY;
    }
    
    private void onStartCompat(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStart.");

        // try to login immediately
        senseServiceLogin();

        // register broadcast receiver for login in case of Internet connection changes
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(this.loginPossibility, filter);
    }

    private boolean register(String email, String name, String pass) {
        try {
            String cookie = "";
            final URI uri = new URI(SenseSettings.URL_REG + "?email=" + email + "&password=" + pass
                    + "&name=" + name);
            final HttpPost post = new HttpPost(uri);
            final HttpClient client = new DefaultHttpClient();
            // client.getConnectionManager().closeIdleConnections(2, TimeUnit.SECONDS);
            final HttpResponse response = client.execute(post);
            if (response == null) {
                return false;
            }
            cookie = response.getFirstHeader("Set-Cookie").getValue();
            final InputStream stream = response.getEntity().getContent();
            final String responseStr = convertStreamToString(stream);
            final SharedPreferences prefs = getSharedPreferences(PRIVATE_PREFS, MODE_PRIVATE);
            final Editor editor = prefs.edit();
            if (!responseStr.toLowerCase().contains("ok")) {
                editor.putBoolean(name + "_ok", false);
                editor.putString(SenseSettings.PREF_LOGIN_COOKIE, "");
                editor.commit();
                return false;
            } else {
                editor.putBoolean(name + "_ok", true);
                editor.putString(SenseSettings.PREF_LOGIN_COOKIE, cookie);
                editor.commit();
            }
            Log.d(TAG, "CommonSense registration: " + responseStr);
        } catch (final IOException e) {
            Log.e(TAG, "IOException during registration!", e);
            return false;
        } catch (final IllegalAccessError e) {
            Log.e(TAG, "IllegalAccessError during registration!", e);
            return false;
        } catch (final URISyntaxException e) {
            Log.e(TAG, "URISyntaxException during registration!", e);
            return false;
        }
        return true;
    }

    public boolean registerPhone() {
        boolean registered = false;
        try {
            final String imei = this.telMan.getDeviceId();          
            
            final URI uri = new URI((SenseSettings.URL_REG_PHONE + "?uuid=" + imei + "&type=smartPhone"));
            final HttpPost post = new HttpPost(uri);
            final SharedPreferences prefs = getSharedPreferences(PRIVATE_PREFS, MODE_PRIVATE);
            final String cookie = prefs.getString(SenseSettings.PREF_LOGIN_COOKIE, "");
            post.setHeader("Cookie", cookie);
            final HttpClient client = new DefaultHttpClient();
            // client.getConnectionManager().closeIdleConnections(2, TimeUnit.SECONDS);
            final HttpResponse response = client.execute(post);
            if (response == null) {
                return false;
            }

            final InputStream stream = response.getEntity().getContent();
            final String responseStr = convertStreamToString(stream);
            registered = responseStr.toLowerCase().contains("ok");
            Map<String, String> data = new HashMap<String, String>();
            data.put("brand", Build.MANUFACTURER);
            data.put("type", Build.MODEL);            
            msgHandler.sendSensorData("device properties", data);            
            
            Log.d(TAG, "CommonSense phone registration: " + responseStr);
        } catch (final IOException e) {
            Log.e(TAG, "IOException registering phone!", e);
            return false;
        } catch (final IllegalAccessError e) {
            Log.e(TAG, "IllegalAccessError registering phone!", e);
            return false;
        } catch (final URISyntaxException e) {
            Log.e(TAG, "URISyntaxException registering phone!", e);
            return false;
        }
        return registered;
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

        final SharedPreferences prefs = getSharedPreferences(PRIVATE_PREFS, MODE_PRIVATE);
        final String email = prefs.getString(SenseSettings.PREF_LOGIN_MAIL, "");
        final String pass = prefs.getString(SenseSettings.PREF_LOGIN_PASS, "");
        if ((email.length() > 0) && (pass.length() > 0) && login(email, pass)) {
            notifySenseLogin(false);
            SenseService.sLoggedIn = true;
            if (!checkPhoneRegistration()) {
                registerPhone();
            }

            // restart stuff after login
            onLogIn();
        } else {
            SenseService.sLoggedIn = false;
            // notifySenseLogin(true);
        }

        return SenseService.sLoggedIn;
    }

    /**
     * Tries to register a new login using the email, username and password from the private
     * preferences and updates the <code>sLoggedIn</code> status accordingly. NB: blocks the calling
     * thread.
     * 
     * @return <code>true</code> if successful.
     */
    public boolean senseServiceRegister() {
        final SharedPreferences prefs = getSharedPreferences(PRIVATE_PREFS, MODE_PRIVATE);
        final String email = prefs.getString(SenseSettings.PREF_LOGIN_MAIL, "");
        final String name = prefs.getString(SenseSettings.PREF_LOGIN_NAME, "");
        final String pass = prefs.getString(SenseSettings.PREF_LOGIN_PASS, "");
        Log.d(TAG, "Registering... Name: " + name + ", email: " + email + ", pass: " + pass);
        if ((email.length() > 0) && (pass.length() > 0) && register(email, name, pass)) {
            // Registration successful
            notifySenseLogin(false);
            SenseService.sLoggedIn = true;
            if (!checkPhoneRegistration()) {
                registerPhone();
            }
        } else {
            SenseService.sLoggedIn = false;
        }
        return SenseService.sLoggedIn;
    }

    private void showToast(final String msg) {
        // show informational Toast
        this.handler.post(new Runnable() {
            public void run() {
                Toast.makeText(SenseService.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Makes this service a foreground service, as important as 'real' activities. As a reminder
     * that the service is running, a notification is shown.
     */
    private void startForegroundCompat() {
        @SuppressWarnings("rawtypes")
        final Class[] startForegroundSignature = new Class[] { int.class, Notification.class};
        Method startForeground = null;
        try { 
            startForeground = getClass().getMethod("startForeground", startForegroundSignature); 
        } catch (NoSuchMethodException e) { 
            // Running on an older platform. 
            startForeground = null; 
        }

        // call startForeground in fancy way so old systems do net get confused by unknown methods
        if (startForeground == null) {
            setForeground(true);
        } else {
            // create notification
            final int icon = R.drawable.ic_status_sense_disabled; 
            final long when = System.currentTimeMillis();
            final Notification n = new Notification(icon, null, when);
            final Intent notifIntent = new Intent("nl.sense_os.app.SenseApp");
            notifIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            final PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notifIntent, 0);
            n.setLatestEventInfo(this, "Sense service", "", contentIntent);            
            
            Object[] startArgs = {Integer.valueOf(NOTIF_ID), n}; 
            try { 
                startForeground.invoke(this, startArgs); 
            } catch (InvocationTargetException e) { 
                // Should not happen. 
                Log.w(TAG, "Unable to invoke startForeground", e); 
            } catch (IllegalAccessException e) { 
                // Should not happen. 
                Log.w(TAG, "Unable to invoke startForeground", e); 
            } 
        }
    }

    /**
     * Makes this service a foreground service, as important as 'real' activities. As a reminder
     * that the service is running, a notification is shown.
     */
    private void stopForegroundCompat() {
        @SuppressWarnings("rawtypes")
        final Class[] stopForegroundSignature = new Class[] { boolean.class };
        Method stopForeground = null;
        try { 
            stopForeground = getClass().getMethod("stopForeground", stopForegroundSignature); 
        } catch (NoSuchMethodException e) { 
            // Running on an older platform. 
            stopForeground = null; 
        }

        // call stopForeground in fancy way so old systems do net get confused by unknown methods
        if (stopForeground == null) {
            // remove the notification that the service is running
            final NotificationManager noteMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            noteMgr.cancel(NOTIF_ID);
            
            setForeground(false);
        } else {           
            Object[] stopArgs = { Boolean.TRUE }; 
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

    private void toggleDeviceProx(boolean active) {
        
        if (active != this.statusDeviceProx) {
            if (true == active) 
            {
                //  showToast(getString(R.string.toast_toggle_dev_prox));
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                final int rate = Integer.parseInt(prefs.getString(SenseSettings.PREF_COMMONSENSE_RATE, "0"));
                int interval = 1;         
                switch (rate) {
                case -2:
                	interval = 1 * 1000;    
                    break;
                case -1:
                    // often
                    interval = 15 * 1000;
                    break;
                case 0:
                    // normal
                    interval = 60 * 1000;
                    break;
                case 1:
                    // rarely (15 hour)
                    interval = 15 *60 * 1000;
                    break;
                default:
                    Log.e(TAG, "Unexpected quiz rate preference.");
                }
                deviceProximity.startEnvironmentScanning(interval);
                this.statusDeviceProx = true;
            } else {
            	deviceProximity.stopEnvironmentScanning();
                this.statusDeviceProx = false;
            }
            
            // send broadcast that something has changed in the status
            sendBroadcast(new Intent(ACTION_SERVICE_BROADCAST));
        }
    }

    private void toggleLocation(boolean active) {

        if (active != this.statusLocation) {
            final LocationManager locMgr = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (true == active) {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                final boolean gps = prefs.getBoolean(SenseSettings.PREF_LOCATION_GPS, true);
                final boolean network = prefs.getBoolean(SenseSettings.PREF_LOCATION_NETWORK, true);
                final int rate = Integer.parseInt(prefs.getString(
                        SenseSettings.PREF_COMMONSENSE_RATE, "0"));

                // set update parameters according to preference
                long minTime = -1;
                float minDistance = -1;
                if (gps || network) {
                    switch (rate) {
                    case -2: // often
                        minTime = 1000;
                        minDistance = 1;
                        break;
                    case -1: // often
                        minTime = 15 * 1000;
                        minDistance = 10;
                        break;
                    case 0: // normal
                        minTime = 60 * 1000;
                        minDistance = 50;
                        break;
                    case 1: // rarely
                        minTime = 15 * 60 * 1000;
                        minDistance = 1000;
                        break;
                    default:
                        Log.e(TAG, "Unexpected commonsense rate: " + rate);
                        break;
                    }

                    this.statusLocation = true;
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
                this.statusLocation = false;
            }

            // send broadcast that something has changed in the status
            sendBroadcast(new Intent(ACTION_SERVICE_BROADCAST));
        }
    }

    private void toggleMotion(boolean active) {

        if (active != this.statusMotion) {
            SensorManager mgr = (SensorManager) getSystemService(SENSOR_SERVICE);
            if (true == active) {
                List<Sensor> sensors = mgr.getSensorList(Sensor.TYPE_ALL);
                for (Sensor sensor : sensors) {
                	if(sensor.getType()==Sensor.TYPE_ACCELEROMETER ||
                			sensor.getType()==Sensor.TYPE_ORIENTATION || 
                			sensor.getType()==Sensor.TYPE_GYROSCOPE || 
                			sensor.getType()==Sensor.TYPE_PRESSURE)
                	{
                		Log.d(TAG, "registering for sensor " + sensor.getName());
                		mgr.registerListener(this.motionSensor, sensor, SensorManager.SENSOR_DELAY_NORMAL);
                	}
                }
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                final int rate = Integer.parseInt(prefs.getString(
                        SenseSettings.PREF_COMMONSENSE_RATE, "0"));
                int interval = -1;
                switch (rate) {
                case -2: // real time
                    interval = 0;
                    break;
                case -1: // often
                    interval = 5 * 1000;
                    break;
                case 0: // normal
                    interval = 60 * 1000;
                    break;
                case 1: // rarely (1 hour)
                    interval = 15 * 1000;
                    break;
                default:
                    Log.e(TAG, "Unexpected commonsense rate: " + rate);
                    break;
                }
                motionSensor.setSampleDelay(interval);  
                this.statusMotion = true;
            } else {
                mgr.unregisterListener(this.motionSensor);

                this.statusMotion = false;
            }

            // send broadcast that something has changed in the status
            sendBroadcast(new Intent(ACTION_SERVICE_BROADCAST));
        }
    }

    private void toggleNoise(boolean active) {
        
        if (active != this.statusNoise) {
            final TelephonyManager telMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (true == active) {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                final int rate = Integer.parseInt(prefs.getString(
                        SenseSettings.PREF_COMMONSENSE_RATE, "0"));
                int interval = -1;
                switch (rate) {
                case -2: // real time
                    interval = -1;
                    break;
                case -1: // often
                    interval = 5 * 1000;
                    break;
                case 0: // normal
                    interval = 60 * 1000;
                    break;
                case 1: // rarely (1 hour)
                    interval = 15 * 1000;
                    break;
                default:
                    Log.e(TAG, "Unexpected quiz rate preference.");
                }
                telMgr.listen(noiseSensor, PhoneStateListener.LISTEN_CALL_STATE);
                this.noiseSensor.startListening(interval);

                this.statusNoise = true;
            } else {
                telMgr.listen(noiseSensor, PhoneStateListener.LISTEN_NONE);
                this.noiseSensor.stopListening();
                this.statusNoise = false;
            }

            // send broadcast that something has changed in the status
            sendBroadcast(new Intent(ACTION_SERVICE_BROADCAST));
        }
    }

    private void togglePhoneState(boolean active) {

        if (active != this.statusPhoneState) {
            if (true == active) {

                // start listening to any phone connectivity updates
                this.telMan.listen(this.psl, PhoneStateListener.LISTEN_CALL_FORWARDING_INDICATOR
                        | PhoneStateListener.LISTEN_CALL_STATE
                        | PhoneStateListener.LISTEN_DATA_ACTIVITY
                        | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                        | PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR
                        | PhoneStateListener.LISTEN_SERVICE_STATE);

                this.statusPhoneState = true;
            } else {
                // stop phone state listener
                telMan.listen(this.psl, PhoneStateListener.LISTEN_NONE);
                this.statusPhoneState = false;
            }

            // send broadcast that something has changed in the status
            sendBroadcast(new Intent(ACTION_SERVICE_BROADCAST));
        }
    }

    private void togglePopQuiz(boolean active) {

        if (active != statusPopQuiz) {
            final SenseAlarmManager mgr = new SenseAlarmManager(this);
            if (true == active) {

                // create alarm
                mgr.createSyncAlarm();
                this.statusPopQuiz = mgr.createEntry(0, 1);
            } else {

                // cancel alarm
                mgr.cancelEntry();
                mgr.cancelSyncAlarm();

                this.statusPopQuiz = false;
            }

            // send broadcast that something has changed in the status
            sendBroadcast(new Intent(ACTION_SERVICE_BROADCAST));
        }
    }
}
