/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.sense_os.service.ambience.CameraLightSensor;
import nl.sense_os.service.ambience.LightSensor;
import nl.sense_os.service.ambience.MagneticFieldSensor;
import nl.sense_os.service.ambience.NoiseSensor;
import nl.sense_os.service.ambience.PressureSensor;
import nl.sense_os.service.ambience.TemperatureSensor;
import nl.sense_os.service.commonsense.DefaultSensorRegistrationService;
import nl.sense_os.service.commonsense.SenseApi;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Auth;
import nl.sense_os.service.constants.SensePrefs.Main.Advanced;
import nl.sense_os.service.constants.SensePrefs.Main.Ambience;
import nl.sense_os.service.constants.SensePrefs.Main.External;
import nl.sense_os.service.constants.SensePrefs.Main.PhoneState;
import nl.sense_os.service.constants.SensePrefs.Status;
import nl.sense_os.service.constants.SenseUrls;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.ctrl.Controller;
import nl.sense_os.service.deviceprox.DeviceProximity;
import nl.sense_os.service.external_sensors.NewOBD2DeviceConnector;
import nl.sense_os.service.external_sensors.ZephyrBioHarness;
import nl.sense_os.service.external_sensors.ZephyrHxM;
import nl.sense_os.service.location.LocationSensor;
import nl.sense_os.service.motion.MotionSensor;
import nl.sense_os.service.phonestate.BatterySensor;
import nl.sense_os.service.phonestate.PhoneActivitySensor;
import nl.sense_os.service.phonestate.ProximitySensor;
import nl.sense_os.service.phonestate.SensePhoneState;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.scheduler.ScheduleAlarmTool;
import nl.sense_os.service.shared.DataProcessor;
import nl.sense_os.service.shared.DataProducer;

import org.json.JSONObject;

import android.app.Activity;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

/**
 * Main Sense service class.<br/>
 * <br/>
 * Activities can bind to this service and call functions to:
 * <ul>
 * <li>log in;</li>
 * <li>register;</li>
 * <li>start sensing;</li>
 * <li>start/stop individual sensor modules;</li>
 * <li>set and get properties;</li>
 * </ul>
 * When the {@link #toggleMain(boolean)} method is called to start the sensing, the service starts
 * itself and registers itself as a foreground service so it does not get easily killed by Android.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class SenseService extends Service {

    /**
     * Class used for the client Binder. Because we know this service always runs in the same
     * process as its clients, we don't need to deal with IPC.
     * 
     * @see http://developer.android.com/guide/components/bound-services.html
     */
    public class SenseBinder extends Binder {

        public SenseServiceStub getService() {
            return new SenseServiceStub(SenseService.this);
        }
    }

    private static final String TAG = "Sense Service";

    /**
     * Intent action to force a re-login attempt when the service is started.
     */
    public static final String EXTRA_RELOGIN = "relogin";

    /**
     * Intent action for broadcasts that the service state has changed.
     */
    public final static String ACTION_SERVICE_BROADCAST = "nl.sense_os.service.Broadcast";

    private IBinder binder = new SenseBinder();

    private ServiceStateHelper state;

    private BatterySensor batterySensor;
    private DeviceProximity deviceProximity;
    private LightSensor lightSensor;
    private CameraLightSensor cameraLightSensor;
    private TemperatureSensor temperatureSensor;
    private LocationSensor locListener;
    private Controller controller;
    private MotionSensor motionSensor;
    private NoiseSensor noiseSensor;
    private PhoneActivitySensor phoneActivitySensor;
    private PressureSensor pressureSensor;
    private ProximitySensor proximitySensor;
    private SensePhoneState phoneStateListener;
    private ZephyrBioHarness es_bioHarness;
    private ZephyrHxM es_HxM;
    private NewOBD2DeviceConnector es_obd2sensor;
    private MagneticFieldSensor magneticFieldSensor;

    /**
     * All registered DataProducers, mapped by sensor name.
     */
    private Map<String, List<DataProducer>> registeredProducers = new HashMap<String, List<DataProducer>>();
    /**
     * All subscribed DataProcessors, mapped by the sensor name they are subscribed to.
     */
    private Map<String, List<DataProcessor>> subscribedProcessors = new HashMap<String, List<DataProcessor>>();
    /**
     * Handler on main application thread to display toasts to the user.
     */
    private static Handler toastHandler = new Handler(Looper.getMainLooper());
    private static Handler initHandler;

    // separate threads for the sensing modules
    private static Handler ambienceHandler, devProxHandler, extSensorHandler, locationHandler,
            motionHandler, phoneStateHandler;

    /**
     * Changes login of the Sense service. Removes "private" data of the previous user from the
     * preferences. Can be called by Activities that are bound to the service.
     * 
     * @param username
     *            Username
     * @param password
     *            Hashed password
     * @return 0 if login completed successfully, -2 if login was forbidden, and -1 for any other
     *         errors.
     */
    int changeLogin(String username, String password) {
        Log.v(TAG, "Change login");

        logout();

        // save new username and password in the preferences
        Editor authEditor = getSharedPreferences(SensePrefs.AUTH_PREFS, MODE_PRIVATE).edit();
        authEditor.putString(Auth.LOGIN_USERNAME, username);
        authEditor.putString(Auth.LOGIN_PASS, password);
        authEditor.commit();

        return login();
    }

    /**
     * Checks if the installed Sense Platform application has an update available, alerting the user
     * via a Toast message.
     */
    private void checkVersion() {
        try {
            String packageName = getPackageName();
            if ("nl.sense_os.app".equals(packageName)) {
                PackageInfo packageInfo = getPackageManager().getPackageInfo(packageName, 0);
                String versionName = URLEncoder.encode(packageInfo.versionName, "UTF-8");
                Log.i(TAG, "Running Sense App version '" + versionName + "'");

                if (versionName.contains("unstable") || versionName.contains("testing")) {
                    return;
                }

                String url = SenseUrls.VERSION + "?version=" + versionName;
                Map<String, String> response = SenseApi.request(this, url, null, null);
                JSONObject content = new JSONObject(response.get("content"));

                if (content.getString("message").length() > 0) {
                    Log.i(TAG, "Newer Sense App version available: " + content.toString());
                    showToast(content.getString("message"));
                }
            } else {
                // this is a third party app
            }

        } catch (Exception e) {
            Log.w(TAG, "Failed to get Sense App version: " + e);
        }
    }

    /**
     * @param sensorName
     *            The name of the DataProducer
     * @return The list with DataProducers which are registered under the given sensor name
     */
    public List<DataProducer> getRegisteredDataProducers(String sensorName) {
        return registeredProducers.get(sensorName);
    }

    /**
     * @param sensorName
     *            The name of the DataProcessor
     * @return The list with DataProcessors which are subscribed to the given sensor name
     */
    public List<DataProcessor> getSubscribedDataProcessors(String sensorName) {
        return subscribedProcessors.get(sensorName);
    }

    /**
     * @param sensorName
     *            The name of the DataProducer (SenseSensor/DataProcessor)
     * @param sensor
     *            The AtomicReference of the DataProducer object
     * @return true if this DataProducer is already registered under this sensor name
     */
    public boolean isDataProcessorSubscribed(String sensorName, DataProcessor dataProcessor) {

        if (!subscribedProcessors.containsKey(sensorName)) {
            // nothing is subscribed to this sensor name
            return false;
        }

        // check if dataProcessor is in the list of subscribed processors
        List<DataProcessor> processors = subscribedProcessors.get(sensorName);
        for (DataProcessor registeredProcessor : processors) {
            if (registeredProcessor.equals(dataProcessor)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param sensorName
     *            The name of the DataProducer
     * @return true if a data producer is already registered under this sensor name
     */
    public boolean isDataProducerRegistered(String sensorName) {
        return registeredProducers.containsKey(sensorName);
    }

    /**
     * @param sensorName
     *            The name of the DataProducer
     * @param sensor
     *            The AtomicReference of the DataProducer object
     * @return true if the data producer is already registered under this sensor name
     */
    public boolean isDataProducerRegistered(String sensorName, DataProducer dataProducer) {

        if (!registeredProducers.containsKey(sensorName)) {
            // nothing is registered under this sensor name
            return false;
        }

        // check if dataProducer is already in the list of registered producers
        List<DataProducer> producers = registeredProducers.get(sensorName);
        for (DataProducer registeredProducer : producers) {
            if (registeredProducer.equals(dataProducer)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tries to login using the username and password from the private preferences and updates the
     * {@link #isLoggedIn} status accordingly. Can also be called from Activities that are bound to
     * the service.
     * 
     * @return 0 if login completed successfully, -2 if login was forbidden, and -1 for any other
     *         errors.
     */
    synchronized int login() {

        if (state.isLoggedIn()) {
            // we are already logged in
            Log.v(TAG, "Skip login: already logged in");
            return 0;
        }

        // check that we are actually allowed to log in
        SharedPreferences mainPrefs = getSharedPreferences(SensePrefs.MAIN_PREFS, MODE_PRIVATE);
        boolean allowed = mainPrefs.getBoolean(Advanced.USE_COMMONSENSE, true);
        if (!allowed) {
            Log.w(TAG, "Not logging in. Use of CommonSense is disabled.");
            return -1;
        }

        Log.v(TAG, "Try to log in");

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
                Log.w(TAG, "Exception during login! " + e + ": '" + e.getMessage() + "'");
                // handle result below
            }
        } else {
            Log.w(TAG, "Cannot login: username or password unavailable");
        }

        // handle the result
        switch (result) {
        case 0: // logged in successfully
            onLogIn();
            break;
        case -1: // error
            Log.w(TAG, "Login failed!");
            onLogOut();
            break;
        case -2: // forbidden
            Log.w(TAG, "Login forbidden!");
            onLogOut();
            break;
        default:
            Log.e(TAG, "Unexpected login result: " + result);
            onLogOut();
        }

        return result;
    }

    void logout() {
        Log.v(TAG, "Log out");

        // stop active sensing components
        stopSensorModules();

        // clear cached settings of the previous user (e.g. device id)
        Editor authEditor = getSharedPreferences(SensePrefs.AUTH_PREFS, MODE_PRIVATE).edit();
        authEditor.clear();
        authEditor.commit();

        // log out before changing to a new user
        onLogOut();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "Some component is binding to Sense Platform service");
        return binder;
    }

    /**
     * Does nothing except poop out a log message. The service is really started in onStart,
     * otherwise it would also start when an activity binds to it.
     * 
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        Log.v(TAG, "Sense Platform service is being created");
        state = ServiceStateHelper.getInstance(this);
    }

    /**
     * Stops sensing, logs out, removes foreground status.
     * 
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        Log.v(TAG, "Sense Platform service is being destroyed");

        // stop active sensing components
        stopSensorModules();

        // update login status
        onLogOut();

        // stop the main service
        stopForeground(true);

        super.onDestroy();
    }

    /**
     * Performs tasks after successful login: update status bar notification; start transmitting
     * collected sensor data and register the gcm_id.
     */
    private void onLogIn() {
        Log.i(TAG, "Logged in.");
        // update ntp time
        SNTP.getInstance().requestTime(SNTP.HOST_WORLDWIDE, 2000);

        // update login status
        state.setLoggedIn(true);

        // store this login
        SharedPreferences prefs = getSharedPreferences(SensePrefs.MAIN_PREFS, MODE_PRIVATE);
        prefs.edit().putLong(SensePrefs.Main.LAST_LOGGED_IN, System.currentTimeMillis()).commit();

        checkVersion();
    }

    /**
     * Performs cleanup tasks when the service is logged out: updates the status bar notification;
     * stops the periodic alarms for data transmission.
     */
    private void onLogOut() {
        Log.i(TAG, "Logged out");

        // update login status
        state.setLoggedIn(false);

        DataTransmitter.stopTransmissions(this);

        // completely stop the MsgHandler service
        stopService(new Intent(getString(R.string.action_sense_new_data)));
        stopService(new Intent(getString(R.string.action_sense_send_data)));
    }

    void onSampleRateChange() {
        Log.v(TAG, "Sample rate changed");
        if (state.isStarted()) {
            stopSensorModules();
            ScheduleAlarmTool.getInstance(this).resetNextExecution();
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
        Log.i(TAG, "Sense Platform service is being started");

        if (null == initHandler) {
            HandlerThread startThread = new HandlerThread("Start thread");
            startThread.start();
            initHandler = new Handler(startThread.getLooper());
        }
        initHandler.post(new Runnable() {

            @Override
            public void run() {

                boolean mainStatus = getSharedPreferences(SensePrefs.STATUS_PREFS, MODE_PRIVATE)
                        .getBoolean(Status.MAIN, true);
                if (false == mainStatus) {
                    Log.w(TAG, "Sense service was started when the main status is not set!");
                    AliveChecker.stopChecks(SenseService.this);
                    stopForeground(true);
                    state.setForeground(false);
                    stopSensorModules();

                } else {
                    // make service as important as regular activities
                    if (false == state.isForeground()) {
                        Notification n = state.getStateNotification();
                        startForeground(ServiceStateHelper.NOTIF_ID, n);
                        state.setForeground(true);
                        AliveChecker.scheduleChecks(SenseService.this);
                    }

                    // re-login if necessary
                    boolean relogin = !state.isLoggedIn();
                    relogin |= (null == intent); // intent is null when Service
                    // was killed
                    relogin |= (null != intent) && intent.getBooleanExtra(EXTRA_RELOGIN, false);
                    if (relogin) {
                        login();
                    } else {
                        checkVersion();
                    }

                    // restart the individual modules
                    startSensorModules();
                }
            }
        });

        return START_STICKY;
    }

    void onSyncRateChange() {
        Log.v(TAG, "Sync rate changed");
        if (state.isStarted()) {
            controller = Controller.getController(this);
            controller.scheduleTransmissions();
        }

        // update any widgets
        startService(new Intent(getString(R.string.action_widget_update)));
    }

    /**
     * Tries to register a new user using the username and password from the private preferences and
     * updates the {@link #isLoggedIn} status accordingly. Can also be called from Activities that
     * are bound to the service.
     * 
     * @param username
     * @param password
     *            Hashed password
     * @param email
     * @param address
     * @param zipCode
     * @param country
     * @param name
     * @param surname
     * @param mobile
     * @return 0 if registration completed successfully, -2 if the user already exists, and -1 for
     *         any other unexpected responses.
     */
    synchronized int register(String username, String password, String email, String address,
            String zipCode, String country, String name, String surname, String mobile) {
        Log.v(TAG, "Try to register new user");

        // log out before registering a new user
        logout();

        // stop active sensing components
        stopSensorModules();

        // save username and password in preferences
        Editor authEditor = getSharedPreferences(SensePrefs.AUTH_PREFS, MODE_PRIVATE).edit();
        authEditor.putString(Auth.LOGIN_USERNAME, username);
        authEditor.putString(Auth.LOGIN_PASS, password);
        authEditor.commit();

        // try to register
        int registered = -1;
        if ((null != username) && (null != password)) {
            // Log.v(TAG, "Registering: " + username +
            // ", password hash: " + hashPass);

            try {
                registered = SenseApi.registerUser(this, username, password, name, surname, email,
                        mobile);
            } catch (Exception e) {
                Log.w(TAG, "Exception during registration: '" + e.getMessage()
                        + "'. Connection problems?");
                // handle result below
            }
        } else {
            Log.w(TAG, "Cannot register: username or password unavailable");
        }

        // handle result
        switch (registered) {
        case 0:
            Log.i(TAG, "Successful registration for '" + username + "'");
            login();
            break;
        case -1:
            Log.w(TAG, "Registration failed");
            state.setLoggedIn(false);
            break;
        case -2:
            Log.w(TAG, "Registration failed: user already exists");
            state.setLoggedIn(false);
            break;
        default:
            Log.w(TAG, "Unexpected registration result: " + registered);
        }

        return registered;
    }

    /**
     * <p>
     * Registers a DataProducer with the given name at the SenseService.
     * </p>
     * <p>
     * When a data producer is registered, data processors can subscribe to its sensor data.
     * Registering a data producer with an existing name will add the new data producer only if this
     * data producer is a different instance from the other data producer.
     * </p>
     * 
     * @param name
     *            The name of the data producer
     * @param producer
     *            The data producer
     */
    public void registerDataProducer(String name, DataProducer producer) {

        if (isDataProducerRegistered(name, producer)) {
            // data producer is already registered
            return;
        }

        // add the producer to the list of registered producers
        List<DataProducer> producers = registeredProducers.get(name);
        if (null == producers) {
            producers = new ArrayList<DataProducer>();
        }
        producers.add(producer);
        registeredProducers.put(name, producers);

        // see if there are any data processors subscribed to this sensor name
        if (!subscribedProcessors.containsKey(name)) {
            return;
        }

        // subscribe existing DataProcessors to the new DataProducer
        List<DataProcessor> subscribers = subscribedProcessors.get(name);
        for (DataProcessor subscriber : subscribers) {
            producer.addSubscriber(subscriber);
        }
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
     * Toggles the individual sensor modules according to the status that was stored in the
     * preferences.
     */
    private synchronized void startSensorModules() {
        if (state.isStarted()) {
            Log.v(TAG, "Start sensor modules (probably already started)");
        } else {
            Log.v(TAG, "Start sensor modules");
        }

        // make sure the IDs of all sensors are known
        SharedPreferences mainPrefs = getSharedPreferences(SensePrefs.MAIN_PREFS, MODE_PRIVATE);
        boolean useCommonSense = mainPrefs.getBoolean(Advanced.USE_COMMONSENSE, true);
        if (useCommonSense) {
            verifySensorIds();
        }

        SharedPreferences statusPrefs = getSharedPreferences(SensePrefs.STATUS_PREFS, MODE_PRIVATE);
        if (statusPrefs.getBoolean(Status.MAIN, false)) {
            // start database leeglepelaar
            controller = Controller.getController(this);
            controller.scheduleTransmissions();
            togglePhoneState(statusPrefs.getBoolean(Status.PHONESTATE, false));
            toggleLocation(statusPrefs.getBoolean(Status.LOCATION, false));
            toggleAmbience(statusPrefs.getBoolean(Status.AMBIENCE, false));
            toggleMotion(statusPrefs.getBoolean(Status.MOTION, false));
            toggleDeviceProx(statusPrefs.getBoolean(Status.DEV_PROX, false));
            toggleExternalSensors(statusPrefs.getBoolean(Status.EXTERNAL, false));

            state.setStarted(true);
        }

        // send broadcast that something has changed in the status
        sendBroadcast(new Intent(ACTION_SERVICE_BROADCAST));
    }

    /**
     * Stops any running sensor modules.
     */
    private void stopSensorModules() {
        Log.v(TAG, "Stop sensor modules");

        toggleDeviceProx(false);
        toggleMotion(false);
        toggleLocation(false);
        toggleAmbience(false);
        togglePhoneState(false);
        toggleExternalSensors(false);

        state.setStarted(false);

        // send broadcast that something has changed in the status
        sendBroadcast(new Intent(ACTION_SERVICE_BROADCAST));
    }

    /**
     * Subscribe to a DataProducer<br/>
     * <br/>
     * This method subscribes a DataProcessor to receive SensorDataPoints from a DataProducer. If
     * the DataProducer with name to subscribe to is not registered yet then the data processor will
     * be put in the queue and will be subscribed to the DataProducer when it is registered.
     * 
     * @param name
     *            The name of the registered DataProducer
     * @param processor
     *            The DataProcessor that receives the sensor data
     * @return true if the DataProcessor is successfully subscribed to the DataProducer.
     */
    public boolean subscribeDataProcessor(String name, DataProcessor processor) {

        if (isDataProcessorSubscribed(name, processor)) {
            // processor is already subscribed
            return false;
        }

        // add the processor to the list of subscribed processors
        List<DataProcessor> processors = subscribedProcessors.get(name);
        if (null == processors) {
            processors = new ArrayList<DataProcessor>();
        }
        processors.add(processor);
        subscribedProcessors.put(name, processors);

        // see if there are any producers for this sensor name
        List<DataProducer> producers = registeredProducers.get(name);
        if (producers == null) {
            return false;
        }

        // subscribe the new processor to the existing producers
        boolean subscribed = false;
        for (DataProducer producer : producers) {
            subscribed |= producer.addSubscriber(processor);
        }
        return subscribed;
    }

    synchronized void toggleAmbience(boolean active) {

        if (active != state.isAmbienceActive()) {
            Log.i(TAG, (active ? "Enable" : "Disable") + " ambience sensors");
            state.setAmbienceActive(active);

            if (true == active) {

                // check noise sensor presence
                if (null != noiseSensor) {
                    Log.w(TAG, "Noise sensor is already present!");
                    noiseSensor.stopSensing();
                    noiseSensor = null;
                }

                // check light sensor presence
                if (null != lightSensor) {
                    Log.w(TAG, "Light sensor is already present!");
                    lightSensor.stopSensing();
                    lightSensor = null;
                }

                // check camera light sensor presence
                if (null != cameraLightSensor) {
                    Log.w(TAG, "Camera Light sensor is already present!");
                    cameraLightSensor.stopLightSensing();
                    cameraLightSensor = null;
                }

                // check pressure sensor presence
                if (pressureSensor != null) {
                    Log.w(TAG, "pressure sensor is already present!");
                    pressureSensor.stopSensing();
                    pressureSensor = null;
                }

                // check pressure sensor presence
                if (temperatureSensor != null) {
                    Log.w(TAG, "temperature sensor is already present!");
                    temperatureSensor.stopSensing();
                    temperatureSensor = null;
                }

                // check magnetic field sensor presence
                if (magneticFieldSensor != null) {
                    Log.w(TAG, "magnetic field  sensor is already present!");
                    magneticFieldSensor.stopSensing();
                    magneticFieldSensor = null;
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
                if (mainPrefs.getBoolean(Advanced.AGOSTINO, false)) {
                    Log.i(TAG, "Ambience sensor is in Agostino mode!");
                    interval = 60 * 1000;
                }
                final int finalInterval = interval;

                if (null == ambienceHandler) {
                    HandlerThread handlerThread = new HandlerThread("Ambience thread");
                    handlerThread.start();
                    ambienceHandler = new Handler(handlerThread.getLooper());
                }
                ambienceHandler.post(new Runnable() {

                    @Override
                    public void run() {

                        if (mainPrefs.getBoolean(Ambience.MIC, true)
                                || mainPrefs.getBoolean(Ambience.AUDIO_SPECTRUM, true)) {
                            /*
                             * Notification note=new Notification();
                             * note.flags|=Notification.FLAG_FOREGROUND_SERVICE;
                             * startForeground(1337, note);
                             */
                            noiseSensor = NoiseSensor.getInstance(SenseService.this);
                            registerDataProducer(SensorNames.NOISE, noiseSensor);
                            registerDataProducer(SensorNames.AUDIO_SPECTRUM, noiseSensor);
                            registerDataProducer(SensorNames.LOUDNESS, noiseSensor);
                            registerDataProducer(SensorNames.NOISE,
                                    noiseSensor.getAutoCalibratedNoiseSensor());
                            noiseSensor.startSensing(finalInterval);
                        }
                        if (mainPrefs.getBoolean(Ambience.LIGHT, true)) {
                            lightSensor = LightSensor.getInstance(SenseService.this);
                            registerDataProducer(SensorNames.LIGHT, lightSensor);
                            lightSensor.startSensing(finalInterval);
                        }
                        // only available from Android 2.3 up to 4.0
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD
                                && Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                            if (mainPrefs.getBoolean(Ambience.CAMERA_LIGHT, true)) {
                                cameraLightSensor = CameraLightSensor
                                        .getInstance(SenseService.this);
                                registerDataProducer(SensorNames.CAMERA_LIGHT, cameraLightSensor);
                                cameraLightSensor.startLightSensing(finalInterval);
                            }
                        } else {
                            // Log.v(TAG, "Camera is not supported in this version of Android");
                        }
                        if (mainPrefs.getBoolean(Ambience.MAGNETIC_FIELD, true)) {
                            magneticFieldSensor = MagneticFieldSensor
                                    .getInstance(SenseService.this);
                            registerDataProducer(SensorNames.MAGNETIC_FIELD, magneticFieldSensor);
                            magneticFieldSensor.startSensing(finalInterval);
                        }

                        if (mainPrefs.getBoolean(Ambience.PRESSURE, true)) {
                            pressureSensor = PressureSensor.getInstance(SenseService.this);
                            registerDataProducer(SensorNames.PRESSURE, pressureSensor);
                            pressureSensor.startSensing(finalInterval);
                        }
                        // only available from Android 2.3 up to 4.0
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                            if (mainPrefs.getBoolean(Ambience.TEMPERATURE, true)) {
                                temperatureSensor = TemperatureSensor
                                        .getInstance(SenseService.this);
                                registerDataProducer(SensorNames.TEMPERATURE, temperatureSensor);
                                temperatureSensor.startSensing(finalInterval);
                            }
                        } else {
                            // Log.v(TAG,
                            // "Temperature sensor is not supported in this version of Android");
                        }
                    }
                });

            } else {

                // stop sensing
                if (null != noiseSensor) {
                    noiseSensor.stopSensing();
                    // unregister is not needed for Singleton Sensors
                    unregisterDataProducer(SensorNames.NOISE, noiseSensor);
                    unregisterDataProducer(SensorNames.AUDIO_SPECTRUM, noiseSensor);
                    unregisterDataProducer(SensorNames.LOUDNESS, noiseSensor);
                    unregisterDataProducer(SensorNames.NOISE, noiseSensor);
                    noiseSensor = null;
                }
                if (null != lightSensor) {
                    lightSensor.stopSensing();
                    unregisterDataProducer(SensorNames.LIGHT, lightSensor);
                    lightSensor = null;
                }
                if (null != cameraLightSensor) {
                    cameraLightSensor.stopLightSensing();
                    unregisterDataProducer(SensorNames.CAMERA_LIGHT, cameraLightSensor);
                    cameraLightSensor = null;
                }
                if (null != pressureSensor) {
                    pressureSensor.stopSensing();
                    unregisterDataProducer(SensorNames.PRESSURE, pressureSensor);
                    pressureSensor = null;
                }
                if (null != temperatureSensor) {
                    temperatureSensor.stopSensing();
                    unregisterDataProducer(SensorNames.TEMPERATURE, temperatureSensor);
                    temperatureSensor = null;
                }
                if (null != magneticFieldSensor) {
                    magneticFieldSensor.stopSensing();
                    unregisterDataProducer(SensorNames.MAGNETIC_FIELD, magneticFieldSensor);
                    magneticFieldSensor = null;
                }
            }
        }
    }

    synchronized void toggleDeviceProx(boolean active) {

        if (active != state.isDevProxActive()) {
            Log.i(TAG, (active ? "Enable" : "Disable") + " neighbouring device scan sensors");
            state.setDevProxActive(active);

            if (true == active) {

                // check device proximity sensor presence
                if (null != deviceProximity) {
                    Log.w(TAG, "Device proximity sensor is already present!");
                    deviceProximity.stopEnvironmentScanning();
                    deviceProximity = null;
                }

                // get sample rate
                final SharedPreferences mainPrefs = getSharedPreferences(SensePrefs.MAIN_PREFS,
                        MODE_PRIVATE);
                final int rate = Integer.parseInt(mainPrefs.getString(SensePrefs.Main.SAMPLE_RATE,
                        "0"));
                int interval = 1;
                switch (rate) {
                case -2:
                    interval = 60 * 1000;
                    break;
                case -1:
                    // often
                    interval = 10 * 1000;
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

                if (null == devProxHandler) {
                    HandlerThread handlerThread = new HandlerThread("Device proximity thread");
                    handlerThread.start();
                    devProxHandler = new Handler(handlerThread.getLooper());
                }
                devProxHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        deviceProximity = new DeviceProximity(SenseService.this);

                        registerDataProducer(SensorNames.WIFI_SCAN,
                                deviceProximity.getWIFIDeviceProximity());
                        registerDataProducer(SensorNames.BLUETOOTH_DISCOVERY,
                                deviceProximity.getBluetoothDeviceProximity());
                        registerDataProducer(SensorNames.BLUETOOTH_NEIGHBOURS_COUNT,
                                deviceProximity.getBluetoothDeviceProximity());
                        // start sensing
                        deviceProximity.startEnvironmentScanning(finalInterval);
                    }
                });

            } else {

                // stop sensing
                if (null != deviceProximity) {
                    deviceProximity.stopEnvironmentScanning();
                    unregisterDataProducer(SensorNames.WIFI_SCAN,
                            deviceProximity.getWIFIDeviceProximity());
                    unregisterDataProducer(SensorNames.BLUETOOTH_DISCOVERY,
                            deviceProximity.getBluetoothDeviceProximity());
                    unregisterDataProducer(SensorNames.BLUETOOTH_NEIGHBOURS_COUNT,
                            deviceProximity.getBluetoothDeviceProximity());
                    deviceProximity = null;
                }

                if (null != devProxHandler) {
                    devProxHandler.getLooper().quit();
                    devProxHandler = null;
                }
            }
        }
    }

    synchronized void toggleExternalSensors(boolean active) {

        if (active != state.isExternalActive()) {
            Log.i(TAG, (active ? "Enable" : "Disable") + " external sensors");
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
                if (null != es_obd2sensor) {
                    Log.w(TAG, "OBD-II dongle is already present!");
                    es_obd2sensor.stop();
                    es_obd2sensor = null;
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

                if (null == extSensorHandler) {
                    HandlerThread handlerThread = new HandlerThread("External sensors thread");
                    handlerThread.start();
                    extSensorHandler = new Handler(handlerThread.getLooper());
                }
                extSensorHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        if (mainPrefs.getBoolean(External.ZephyrBioHarness.MAIN, false)) {
                            es_bioHarness = new ZephyrBioHarness(SenseService.this);
                            registerDataProducer(SensorNames.ACCELEROMETER, es_bioHarness);
                            registerDataProducer(SensorNames.HEART_RATE, es_bioHarness);
                            registerDataProducer(SensorNames.RESPIRATION, es_bioHarness);
                            registerDataProducer(SensorNames.TEMPERATURE, es_bioHarness);
                            registerDataProducer(SensorNames.BATTERY_LEVEL, es_bioHarness);
                            registerDataProducer(SensorNames.WORN_STATUS, es_bioHarness);
                            es_bioHarness.startBioHarness(finalInterval);
                        }
                        if (mainPrefs.getBoolean(External.ZephyrHxM.MAIN, false)) {
                            es_HxM = new ZephyrHxM(SenseService.this);
                            registerDataProducer(SensorNames.HEART_RATE, es_HxM);
                            registerDataProducer(SensorNames.SPEED, es_HxM);
                            registerDataProducer(SensorNames.DISTANCE, es_HxM);
                            registerDataProducer(SensorNames.BATTERY_CHARGE, es_HxM);
                            registerDataProducer(SensorNames.STRIDES, es_HxM);
                            es_HxM.startHxM(finalInterval);
                        }
                        if (mainPrefs.getBoolean(External.OBD2Sensor.MAIN, false)) {
                            es_obd2sensor = new NewOBD2DeviceConnector(SenseService.this,
                                    finalInterval);
                            registerDataProducer(SensorNames.OBD_STANDARDS, es_obd2sensor);
                            es_obd2sensor.run();
                        }
                    }
                });

            } else {

                // stop sensing
                if (null != es_bioHarness) {
                    // Log.w(TAG, "Bioharness sensor is already present!");
                    es_bioHarness.stopBioHarness();
                    unregisterDataProducer(SensorNames.ACCELEROMETER, es_bioHarness);
                    unregisterDataProducer(SensorNames.HEART_RATE, es_bioHarness);
                    unregisterDataProducer(SensorNames.RESPIRATION, es_bioHarness);
                    unregisterDataProducer(SensorNames.TEMPERATURE, es_bioHarness);
                    unregisterDataProducer(SensorNames.BATTERY_LEVEL, es_bioHarness);
                    unregisterDataProducer(SensorNames.WORN_STATUS, es_bioHarness);
                    es_bioHarness = null;
                }

                // check HxM sensor presence
                if (null != es_HxM) {
                    // Log.w(TAG, "HxM sensor is already present!");
                    es_HxM.stopHxM();
                    unregisterDataProducer(SensorNames.HEART_RATE, es_HxM);
                    unregisterDataProducer(SensorNames.SPEED, es_HxM);
                    unregisterDataProducer(SensorNames.DISTANCE, es_HxM);
                    unregisterDataProducer(SensorNames.BATTERY_CHARGE, es_HxM);
                    unregisterDataProducer(SensorNames.STRIDES, es_HxM);
                    es_HxM = null;
                }

                // check OBD-II dongle presence
                if (null != es_obd2sensor) {
                    // Log.w(TAG, "OBD-II sensor is already present!");
                    es_obd2sensor.stop();
                    unregisterDataProducer(SensorNames.OBD_STANDARDS, es_obd2sensor);
                    es_obd2sensor = null;
                }

                if (null != extSensorHandler) {
                    extSensorHandler.getLooper().quit();
                    extSensorHandler = null;
                }
            }
        }
    }

    synchronized void toggleLocation(boolean active) {

        if (active != state.isLocationActive()) {
            Log.i(TAG, (active ? "Enable" : "Disable") + " position sensor");
            state.setLocationActive(active);

            if (true == active) {

                // check location sensor presence
                if (locListener != null) {
                    Log.w(TAG, "location sensor is already present!");
                    locListener.stopSensing();
                    locListener = null;
                }

                // get sample rate
                final SharedPreferences mainPrefs = getSharedPreferences(SensePrefs.MAIN_PREFS,
                        MODE_PRIVATE);
                final int rate = Integer.parseInt(mainPrefs.getString(SensePrefs.Main.SAMPLE_RATE,
                        "0"));
                long minTime = -1;
                switch (rate) {
                case -2: // real-time
                    minTime = 1000;
                    break;
                case -1: // often
                    minTime = 30 * 1000;
                    break;
                case 0: // normal
                    minTime = 5 * 60 * 1000;
                    break;
                case 1: // rarely
                    minTime = 15 * 60 * 1000;
                    break;
                default:
                    Log.e(TAG, "Unexpected commonsense rate: " + rate);
                    break;
                }
                // special interval for Agostino
                if (mainPrefs.getBoolean(Advanced.AGOSTINO, false)) {
                    Log.i(TAG, "Location sensor is in Agostino mode!");
                    minTime = 60 * 1000;
                }

                final long time = minTime;

                if (null == locationHandler) {
                    HandlerThread handlerThread = new HandlerThread("Location thread");
                    handlerThread.start();
                    locationHandler = new Handler(handlerThread.getLooper());
                }
                locationHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        locListener = LocationSensor.getInstance(SenseService.this);
                        registerDataProducer(SensorNames.LOCATION, locListener);
                        registerDataProducer(SensorNames.TRAVELED_DISTANCE_1H, locListener);
                        registerDataProducer(SensorNames.TRAVELED_DISTANCE_24H, locListener);
                        locListener.startSensing(time);
                    }
                });

            } else {

                // stop location listener
                if (null != locListener) {
                    locListener.stopSensing();
                    // unregister is not needed for Singleton Sensors
                    unregisterDataProducer(SensorNames.LOCATION, locListener);
                    unregisterDataProducer(SensorNames.TRAVELED_DISTANCE_1H, locListener);
                    unregisterDataProducer(SensorNames.TRAVELED_DISTANCE_24H, locListener);
                    locListener = null;
                }

                if (locationHandler != null) {
                    locationHandler.getLooper().quit();
                    locationHandler = null;
                }
            }
        }
    }

    synchronized void toggleMain(boolean active) {
        Log.i(TAG, (active ? "Enable" : "Disable") + " main sensing status");

        if (true == active) {
            // properly start the service to start sensing
            Log.i(TAG, "Start service");
            startService(new Intent(getString(R.string.action_sense_service)));

        } else {
            Log.i(TAG, "Stop service");

            onLogOut();
            stopSensorModules();

            AliveChecker.stopChecks(this);
            stopForeground(true);
            state.setForeground(false);
        }
    }

    synchronized void toggleMotion(boolean active) {

        if (active != state.isMotionActive()) {
            Log.i(TAG, (active ? "Enable" : "Disable") + " motion sensors");
            state.setMotionActive(active);

            if (true == active) {

                // check motion sensor presence
                if (motionSensor != null) {
                    Log.w(TAG, "Motion sensor is already present! Stopping the sensor");
                    motionSensor.stopSensing();
                    motionSensor = null;
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
                if (mainPrefs.getBoolean(Advanced.AGOSTINO, false)) {
                    Log.i(TAG, "Motion sensor is in Agostino mode!");
                    interval = 1 * 1000;
                }

                final int finalInterval = interval;

                if (null == motionHandler) {
                    HandlerThread motionThread = new HandlerThread("Motion thread");
                    motionThread.start();
                    motionHandler = new Handler(motionThread.getLooper());
                }
                motionHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        motionSensor = MotionSensor.getInstance(SenseService.this);
                        registerDataProducer(SensorNames.MOTION, motionSensor);
                        motionSensor.startSensing(finalInterval);
                    }
                });

            } else {

                // unregister is not needed for Singleton Sensors
                unregisterDataProducer(SensorNames.MOTION, motionSensor);
                // stop sensing
                if (null != motionSensor) {
                    motionSensor.stopSensing();
                    motionSensor = null;
                }

                // quit thread
                if (null != motionHandler) {
                    motionHandler.getLooper().quit();
                    motionHandler = null;
                }
            }
        }
    }

    synchronized void togglePhoneState(boolean active) {

        if (active != state.isPhoneStateActive()) {
            Log.i(TAG, (active ? "Enable" : "Disable") + " phone state sensors");
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
                    proximitySensor.stopSensing();
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

                if (null == phoneStateHandler) {
                    // start sensing on a separate thread
                    HandlerThread handlerThread = new HandlerThread("Phone state thread");
                    handlerThread.start();
                    phoneStateHandler = new Handler(handlerThread.getLooper());
                }
                phoneStateHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            if (mainPrefs.getBoolean(PhoneState.BATTERY, true)) {
                                batterySensor = BatterySensor.getInstance(SenseService.this);
                                batterySensor.startBatterySensing(finalInterval);
                                registerDataProducer(SensorNames.BATTERY_SENSOR, batterySensor);
                            }
                            if (mainPrefs.getBoolean(PhoneState.SCREEN_ACTIVITY, true)) {
                                phoneActivitySensor = PhoneActivitySensor
                                        .getInstance(SenseService.this);
                                phoneActivitySensor.startPhoneActivitySensing(finalInterval);
                                registerDataProducer(SensorNames.SCREEN_ACTIVITY,
                                        phoneActivitySensor);
                            }
                            if (mainPrefs.getBoolean(PhoneState.PROXIMITY, true)) {
                                proximitySensor = ProximitySensor.getInstance(SenseService.this);
                                proximitySensor.startSensing(finalInterval);
                                registerDataProducer(SensorNames.PROXIMITY, proximitySensor);
                            }
                            phoneStateListener = SensePhoneState.getInstance(SenseService.this);
                            phoneStateListener.startSensing(finalInterval);
                            registerDataProducer(SensorNames.CALL_STATE, phoneStateListener);
                            registerDataProducer(SensorNames.DATA_CONN, phoneStateListener);
                            registerDataProducer(SensorNames.SERVICE_STATE, phoneStateListener);
                            registerDataProducer(SensorNames.SIGNAL_STRENGTH, phoneStateListener);
                            registerDataProducer(SensorNames.CONN_TYPE, phoneStateListener);                            
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
                    unregisterDataProducer(SensorNames.CALL_STATE, phoneStateListener);
                    unregisterDataProducer(SensorNames.DATA_CONN, phoneStateListener);
                    unregisterDataProducer(SensorNames.SERVICE_STATE, phoneStateListener);
                    unregisterDataProducer(SensorNames.SIGNAL_STRENGTH, phoneStateListener);
                    unregisterDataProducer(SensorNames.CONN_TYPE, phoneStateListener);
                    phoneStateListener = null;
                }
                if (null != proximitySensor) {
                    proximitySensor.stopSensing();
                    unregisterDataProducer(SensorNames.PROXIMITY, proximitySensor);
                    proximitySensor = null;
                }
                if (null != batterySensor) {
                    batterySensor.stopBatterySensing();
                    unregisterDataProducer(SensorNames.BATTERY_SENSOR, batterySensor);
                    batterySensor = null;
                }
                if (null != phoneActivitySensor) {
                    phoneActivitySensor.stopPhoneActivitySensing();
                    unregisterDataProducer(SensorNames.SCREEN_ACTIVITY, phoneActivitySensor);
                    phoneActivitySensor = null;
                }
                // if (null != phoneStateHandler) {
                // phoneStateHandler.getLooper().quit();
                // phoneStateHandler = null;
                // }
            }
        }
    }

    /**
     * Unregisters a DataProducer.<br/>
     * <br/>
     * No new data processors can subscribe to the DataProducer anymore, but DataProcessors which
     * have already subscribed to the DataProducer will remain subscribed.
     * 
     * @param name
     *            The name that the DataProducer is registered under
     * @param dataProducer
     *            The DataProducer to unregister
     */
    public void unregisterDataProducer(String name, DataProducer dataProducer) {

        if (!registeredProducers.containsKey(name)) {
            // this producer is not registered under this name
            return;
        }

        // remove the producer from the list of registered producers for this sensor name
        List<DataProducer> dataProducers = registeredProducers.get(name);
        dataProducers.remove(dataProducer);
        if (dataProducers.size() == 0) {
            registeredProducers.remove(name);
        } else {
            registeredProducers.put(name, dataProducers);
        }
    }

    /**
     * Unsubscribes a DataProcessor from a DataProducer.
     * 
     * @param name
     *            The name of the DataProducer that the processor registered for
     * @param dataProcessor
     *            The DataProcessor that receives the sensor data
     */
    public void unsubscribeProcessor(String name, DataProcessor dataProcessor) {

        if (!registeredProducers.containsKey(name)) {
            // there are no data producers to unsubscribe from
            return;
        }

        if (!subscribedProcessors.containsKey(name)) {
            // this processor is not registered (?)
            return;
        }

        // remove the processor from the list of subscribed processors
        List<DataProcessor> processors = subscribedProcessors.get(name);
        processors.remove(dataProcessor);
        if (processors.size() == 0) {
            subscribedProcessors.remove(name);
        } else {
            subscribedProcessors.put(name, processors);
        }

        // unsubscribe the processor from the producers for this sensor name
        List<DataProducer> producers = registeredProducers.get(name);
        for (DataProducer registeredProducer : producers) {
            registeredProducer.removeSubscriber(dataProcessor);
        }
        registeredProducers.remove(name);
    }

    private synchronized void verifySensorIds() {
        Log.v(TAG, "Try to verify sensor IDs");
        startService(new Intent(this, DefaultSensorRegistrationService.class));
    }
}
