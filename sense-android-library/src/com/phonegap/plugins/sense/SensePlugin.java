/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package com.phonegap.plugins.sense;

import java.io.IOException;
import java.util.Date;
import java.util.Locale;

import nl.sense_os.platform.SensePlatform;
import nl.sense_os.service.ISenseServiceCallback;
import nl.sense_os.service.SenseServiceStub;
import nl.sense_os.service.commonsense.SenseApi;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Auth;
import nl.sense_os.service.constants.SensePrefs.Main;

import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.RemoteException;
import android.util.Log;

/**
 * PhoneGap plugin implementation for the Sense Platform. Provides PhoneGap applications with an
 * interface to the native Sense service from JavaScript.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class SensePlugin extends CordovaPlugin {

    /**
     * Standard action labels that correspond to the actions that are used in the JavaScript part of
     * the plugin.
     */
    private static class Actions {
        static final String ADD_DATA_POINT = "add_data_point";
        static final String CHANGE_LOGIN = "change_login";
        static final String FLUSH_BUFFER = "flush_buffer";
        static final String GET_COMMONSENSE_DATA = "get_commonsense_data";
        static final String GET_DATA = "get_data";
        static final String GET_STATUS = "get_status";
        static final String GET_SESSION = "get_session";
        static final String GET_PREF = "get_pref";
        static final String GIVE_FEEDBACK = "give_feedback";
        static final String INIT = "init";
        static final String LOGOUT = "logout";
        static final String REGISTER = "register";
        static final String SET_PREF = "set_pref";
        static final String TOGGLE_MAIN = "toggle_main";
        static final String TOGGLE_AMBIENCE = "toggle_ambience";
        static final String TOGGLE_EXTERNAL = "toggle_external";
        static final String TOGGLE_MOTION = "toggle_motion";
        static final String TOGGLE_NEIGHDEV = "toggle_neighdev";
        static final String TOGGLE_PHONESTATE = "toggle_phonestate";
        static final String TOGGLE_POSITION = "toggle_position";
    }

    private class SenseServiceCallback extends ISenseServiceCallback.Stub {

        @Override
        public void onChangeLoginResult(int result) throws RemoteException {
            switch (result) {
            case 0:
                Log.v(TAG, "Change login OK");
                changeLoginCallback.success(result);
                onLoginSuccess();
                break;
            case -1:
                Log.v(TAG, "Login failed! Connectivity problems?");
                changeLoginCallback.error("Error logging in, probably connectivity problems.");
                break;
            case -2:
                Log.v(TAG, "Login failed! Invalid username or password.");
                changeLoginCallback.error("Invalid username or password.");
                break;
            default:
                Log.w(TAG, "Unexpected login result! Unexpected result: " + result);
                changeLoginCallback.error("Unexpected result: " + result);
            }
        }

        @Override
        public void onRegisterResult(int result) throws RemoteException {
            switch (result) {
            case 0:
                Log.v(TAG, "Registration OK");
                registerCallback.success(result);
                break;
            case -1:
                Log.v(TAG, "Registration failed! Connectivity problems?");
                registerCallback.error(result);
                break;
            case -2:
                Log.v(TAG, "Registration failed! Username already taken.");
                registerCallback.error(result);
                break;
            default:
                Log.w(TAG, "Unexpected registration result! Unexpected registration result: "
                        + result);
                registerCallback.error(result);
                break;
            }
        }

        @Override
        public void statusReport(final int status) throws RemoteException {
            // Log.d(TAG, "Received Sense Platform service status: " + status);
            getStatusCallback.success(status);
        }
    }

    private static final String TAG = "PhoneGap Sense";

    private ISenseServiceCallback callback = new SenseServiceCallback();
    private CallbackContext getStatusCallback;
    private CallbackContext changeLoginCallback;
    private CallbackContext registerCallback;

    private SensePlatform sensePlatform;

    private void addDataPoint(CordovaArgs args, final CallbackContext callbackContext)
            throws JSONException {

        // get the parameters
        final String name = args.getString(0);
        final String displayName = args.getString(1);
        final String description = args.getString(2);
        final String dataType = args.getString(3);
        final String value = args.getString(4);
        final long timestamp = args.getLong(5);
        Log.v(TAG, "addDataPoint('" + name + "', '" + displayName + "', '" + description + "', '"
                + dataType + "', '" + value + "', " + timestamp + ")");

        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                boolean result = sensePlatform.addDataPoint(name, displayName, description,
                        dataType, value, timestamp);

                if (result) {
                    callbackContext.success();
                } else {
                    Log.w(TAG, "Could not start MsgHandler service!");
                    callbackContext.error("could not add data to Sense service");
                }
            }
        });
    }

    private void changeLogin(final CordovaArgs args, final CallbackContext callbackContext)
            throws JSONException {

        // get the parameters
        final String username = args.getString(0).toLowerCase(Locale.ENGLISH);
        final String password = args.getString(1);
        Log.v(TAG, "changeLogin('" + username + "', '" + password + "')");

        changeLoginCallback = callbackContext;
        cordova.getThreadPool().execute(new Runnable() {

            @Override
            public void run() {
                try {
                    sensePlatform.login(username, password, callback);
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to call changeLogin()! " + e);
                    callbackContext.error(e.getMessage());
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Failed to bind to service in time!");
                    callbackContext.error("Failed to bind to service in time!");
                }
            };
        });
    }

    /**
     * Executes the request.
     * 
     * This method is called from the WebView thread. To do a non-trivial amount of work, use:
     * cordova.getThreadPool().execute(runnable);
     * 
     * To run on the UI thread, use: cordova.getActivity().runOnUiThread(runnable);
     * 
     * @param action
     *            The action to execute.
     * @param args
     *            The exec() arguments, wrapped with some Cordova helpers.
     * @param callbackContext
     *            The callback context used when calling back into JavaScript.
     * @return Whether the action was valid.
     */
    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext)
            throws JSONException {
        try {
            if (Actions.INIT.equals(action)) {
                init(args, callbackContext);
            } else if (Actions.ADD_DATA_POINT.equals(action)) {
                addDataPoint(args, callbackContext);
            } else if (Actions.CHANGE_LOGIN.equals(action)) {
                changeLogin(args, callbackContext);
            } else if (Actions.FLUSH_BUFFER.equals(action)) {
                flushBuffer(args, callbackContext);
            } else if (Actions.GET_COMMONSENSE_DATA.equals(action)) {
                getRemoteValues(args, callbackContext);
            } else if (Actions.GET_DATA.equals(action)) {
                getLocalValues(args, callbackContext);
            } else if (Actions.GET_PREF.equals(action)) {
                getPreference(args, callbackContext);
            } else if (Actions.GET_STATUS.equals(action)) {
                getStatus(args, callbackContext);
            } else if (Actions.GET_SESSION.equals(action)) {
                getSession(args, callbackContext);
            } else if (Actions.GIVE_FEEDBACK.equals(action)) {
                giveFeedback(args, callbackContext);
            } else if (Actions.LOGOUT.equals(action)) {
                logout(args, callbackContext);
            } else if (Actions.REGISTER.equals(action)) {
                register(args, callbackContext);
            } else if (Actions.SET_PREF.equals(action)) {
                setPreference(args, callbackContext);
            } else if (Actions.TOGGLE_AMBIENCE.equals(action)) {
                toggleAmbience(args, callbackContext);
            } else if (Actions.TOGGLE_EXTERNAL.equals(action)) {
                toggleExternal(args, callbackContext);
            } else if (Actions.TOGGLE_MAIN.equals(action)) {
                toggleMain(args, callbackContext);
            } else if (Actions.TOGGLE_MOTION.equals(action)) {
                toggleMotion(args, callbackContext);
            } else if (Actions.TOGGLE_NEIGHDEV.equals(action)) {
                toggleNeighboringDevices(args, callbackContext);
            } else if (Actions.TOGGLE_PHONESTATE.equals(action)) {
                togglePhoneState(args, callbackContext);
            } else if (Actions.TOGGLE_POSITION.equals(action)) {
                togglePosition(args, callbackContext);
            } else {
                Log.e(TAG, "Invalid action: '" + action + "'");
                return false;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException communicating with Sense Platform for action '" + action
                    + "'", e);
            return false;
        } catch (IllegalAccessException e) {
            Log.e(TAG, "IllegalAccessException getting session id", e);
            return false;
        }

        return true;
    }

    private void flushBuffer(CordovaArgs args, CallbackContext callbackContext) {
        Log.v(TAG, "flushBuffer()");

        cordova.getThreadPool().execute(new Runnable() {

            @Override
            public void run() {
                sensePlatform.flushData();
            }
        });
    }

    private void getLocalValues(CordovaArgs data, CallbackContext callbackContext)
            throws JSONException {

        final String sensorName = data.getString(0);
        final int limit = data.optInt(1);
        Log.v(TAG, "getLocalValues('" + sensorName + "', " + limit + ")");

        JSONArray result = sensePlatform.getLocalData(sensorName, limit);

        // convert the date to seconds
        for (int i = 0; i < result.length(); i++) {
            JSONObject dataPoint = result.getJSONObject(i);
            dataPoint.put("date", dataPoint.getDouble("date") / 1000d);
            result.put(i, dataPoint);
        }

        Log.v(TAG, "Found " + result.length() + " '" + sensorName
                + "' data points in the local storage");
        callbackContext.success(result);
    }

    private void getPreference(CordovaArgs data, CallbackContext callbackContext)
            throws JSONException {

        String key = data.getString(0);
        Log.v(TAG, "getPreference('" + key + "')");

        SenseServiceStub service = sensePlatform.getService();
        if (key.equals(Main.SAMPLE_RATE) || key.equals(Main.SYNC_RATE)
                || key.equals(Auth.LOGIN_USERNAME)) {
            String result = service.getPrefString(key, null);
            callbackContext.success(result);
        } else {
            boolean result = service.getPrefBool(key, false);
            callbackContext.success("" + result);
        }
    }

    private void getRemoteValues(CordovaArgs data, final CallbackContext callbackContext)
            throws JSONException {

        final String sensorName = data.getString(0);
        final boolean onlyThisDevice = data.getBoolean(1);
        final int limit = data.optInt(2);
        Log.v(TAG, "getRemoteValues('" + sensorName + "', " + onlyThisDevice + ", " + limit + ")");

        cordova.getThreadPool().execute(new Runnable() {

            @Override
            public void run() {
                JSONArray result;
                try {
                    result = sensePlatform.getData(sensorName, onlyThisDevice, limit);

                    Log.v(TAG, "Found " + result.length() + " '" + sensorName
                            + "' data points in the CommonSense");

                    // convert the date to seconds
                    for (int i = 0; i < result.length(); i++) {
                        JSONObject dataPoint = result.getJSONObject(i);
                        dataPoint.put("date", dataPoint.getDouble("date") / 1000d);
                        result.put(i, dataPoint);
                    }

                    callbackContext.success(result);

                } catch (IllegalStateException e) {
                    callbackContext.error(e.getMessage());
                } catch (JSONException e) {
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void getSession(CordovaArgs data, CallbackContext callbackContext)
            throws IllegalAccessException {

        Log.v(TAG, "getSessionId()");

        SenseServiceStub service = sensePlatform.getService();
        if (null != service) {

            // try the login
            String sessionId = SenseApi.getCookie(cordova.getActivity());

            // check the result
            if (null != sessionId) {
                Log.v(TAG, "Received session ID from Sense");
                callbackContext.success(sessionId);
            } else {
                Log.v(TAG, "No session ID");
                callbackContext.error("no session id");
            }

        } else {
            Log.e(TAG, "No connection to the Sense Platform service.");
            callbackContext.error("No connection to the Sense Platform service.");
        }
    }

    private void getStatus(CordovaArgs data, final CallbackContext callbackContext)
            throws RemoteException {

        Log.v(TAG, "getStatus()");

        SenseServiceStub service = sensePlatform.getService();
        if (null != service) {
            getStatusCallback = callbackContext;
            service.getStatus(callback);
        } else {
            Log.e(TAG, "No connection to the Sense Platform service.");
            callbackContext.error("No connection to the Sense Platform service.");
        }
    }

    private void giveFeedback(CordovaArgs args, final CallbackContext callbackContext)
            throws JSONException {

        // get the parameters
        final String name = args.getString(0);
        final long start = args.getLong(1);
        final long end = args.getLong(2);
        final String label = args.getString(3);

        Log.v(TAG, "giveFeedback('" + name + "', " + start + ", " + end + ", '" + label + "')");

        cordova.getThreadPool().execute(new Runnable() {

            @Override
            public void run() {
                try {
                    boolean result = sensePlatform.giveFeedback(name, new Date(start),
                            new Date(end), label);
                    callbackContext.success("" + result);
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Failed to bind to service in time!");
                    callbackContext.error("Failed to bind to service in time!");
                } catch (IOException e) {
                    callbackContext.error(e.getMessage());
                } catch (JSONException e) {
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void init(CordovaArgs args, CallbackContext callback) {
        sensePlatform = new SensePlatform(cordova.getActivity());
        callback.success();
    }

    private void logout(CordovaArgs data, CallbackContext callbackContext) throws RemoteException {
        Log.v(TAG, "logout()");
        sensePlatform.logout();
        callbackContext.success();
    }

    /**
     * The final call you receive before your activity is destroyed.
     */
    @Override
    public void onDestroy() {
        sensePlatform.close();
        super.onDestroy();
    }

    private void onLoginSuccess() {
        // special for ivitality
        String packageName = cordova.getActivity().getPackageName();
        if (packageName.equals("nl.sense_os.ivitality")) {
            Log.w(TAG, "Set special iVitality sensor settings");
            SenseServiceStub service = sensePlatform.getService();
            service.setPrefString(SensePrefs.Main.SAMPLE_RATE, SensePrefs.Main.SampleRate.NORMAL);
            service.setPrefString(SensePrefs.Main.SYNC_RATE, SensePrefs.Main.SyncRate.ECO_MODE);

            service.setPrefBool(SensePrefs.Main.Ambience.MIC, true);
            service.setPrefBool(SensePrefs.Main.Ambience.LIGHT, true);
            service.setPrefBool(SensePrefs.Main.Ambience.PRESSURE, false);
            service.setPrefBool(SensePrefs.Main.Ambience.MAGNETIC_FIELD, true);
            service.setPrefBool(SensePrefs.Main.Ambience.CAMERA_LIGHT, true);
            service.setPrefBool(SensePrefs.Main.Ambience.AUDIO_SPECTRUM, false);
            service.toggleAmbience(true);

            service.setPrefBool(SensePrefs.Main.Motion.MOTION_ENERGY, true);
            service.toggleMotion(true);

            service.setPrefBool(SensePrefs.Main.PhoneState.BATTERY, true);
            service.setPrefBool(SensePrefs.Main.PhoneState.APP_INFO, true);
            service.setPrefBool(SensePrefs.Main.PhoneState.PROXIMITY, true);
            service.setPrefBool(SensePrefs.Main.PhoneState.SCREEN_ACTIVITY, true);
            service.setPrefBool(SensePrefs.Main.PhoneState.CALL_STATE, false);
            service.setPrefBool(SensePrefs.Main.PhoneState.DATA_CONNECTION, false);
            service.setPrefBool(SensePrefs.Main.PhoneState.IP_ADDRESS, false);
            service.setPrefBool(SensePrefs.Main.PhoneState.SERVICE_STATE, false);
            service.setPrefBool(SensePrefs.Main.PhoneState.SIGNAL_STRENGTH, false);
            service.setPrefBool(SensePrefs.Main.PhoneState.UNREAD_MSG, false);
            service.togglePhoneState(true);

            service.toggleMain(true);

            service.setPrefBool(SensePrefs.Status.AUTOSTART, true);
        }
    }

    private void register(CordovaArgs data, final CallbackContext callbackContext)
            throws JSONException {

        // get the parameters
        final String username = data.getString(0);
        final String password = data.getString(1);
        final String email = data.getString(2);
        final String address = data.getString(3);
        final String zipCode = data.getString(4);
        final String country = data.getString(5);
        final String name = data.getString(6);
        final String surname = data.getString(7);
        final String phone = data.getString(8);
        Log.v(TAG, "register('" + username + "', '" + password + "', '" + name + "', '" + surname
                + "', '" + email + "', '" + phone + "')");

        // do the registration
        registerCallback = callbackContext;
        cordova.getThreadPool().execute(new Runnable() {

            @Override
            public void run() {
                try {
                    sensePlatform.registerUser(username, password, email, address, zipCode,
                            country, name, surname, phone, callback);
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to call register()! " + e);
                    callbackContext.error(e.getMessage());
                } catch (IllegalStateException e) {
                    callbackContext.error("Failed to bind to service in time!");
                }
            }
        });
    }

    private void setPreference(CordovaArgs data, CallbackContext callbackContext)
            throws JSONException {

        // get the preference key
        String key = data.getString(0);

        // get the preference value
        SenseServiceStub service = sensePlatform.getService();
        if (key.equals(Main.SAMPLE_RATE) || key.equals(Main.SYNC_RATE)
                || key.equals(Auth.LOGIN_USERNAME)) {
            String value = data.getString(1);
            Log.v(TAG, "setPreference('" + key + "', '" + value + "')");
            service.setPrefString(key, value);
        } else {
            boolean value = data.getBoolean(1);
            Log.v(TAG, "setPreference('" + key + "', " + value + ")");
            service.setPrefBool(key, value);
        }
        callbackContext.success();
    }

    private void toggleAmbience(CordovaArgs data, CallbackContext callbackContext)
            throws JSONException {

        // get the argument
        boolean active = data.getBoolean(0);
        Log.v(TAG, (active ? "Enable" : "Disable") + " ambience sensors");

        // do the call
        SenseServiceStub service = sensePlatform.getService();
        if (null != service) {
            service.toggleAmbience(active);
        } else {
            Log.e(TAG, "Failed to bind to service in time!");
            callbackContext.error("Failed to bind to service in time!");
        }

        callbackContext.success();
    }

    private void toggleExternal(CordovaArgs data, CallbackContext callbackContext)
            throws JSONException {

        // get the argument
        boolean active = data.getBoolean(0);
        Log.v(TAG, (active ? "Enable" : "Disable") + " external sensors");

        // do the call
        SenseServiceStub service = sensePlatform.getService();
        if (null != service) {
            service.toggleExternalSensors(active);
        } else {
            Log.e(TAG, "Failed to bind to service in time!");
            callbackContext.error("Failed to bind to service in time!");
        }

        callbackContext.success();
    }

    private void toggleMain(CordovaArgs data, CallbackContext callbackContext) throws JSONException {

        // get the argument
        boolean active = data.getBoolean(0);
        Log.v(TAG, (active ? "Enable" : "Disable") + " main status");

        // do the call
        SenseServiceStub service = sensePlatform.getService();
        if (null != service) {
            service.toggleMain(active);
        } else {
            Log.e(TAG, "Failed to bind to service in time!");
            callbackContext.error("Failed to bind to service in time!");
        }

        callbackContext.success();
    }

    private void toggleMotion(CordovaArgs data, CallbackContext callbackContext)
            throws JSONException {

        // get the argument
        boolean active = data.getBoolean(0);
        Log.v(TAG, (active ? "Enable" : "Disable") + " motion sensors");

        // do the call
        SenseServiceStub service = sensePlatform.getService();
        if (null != service) {
            service.toggleMotion(active);
        } else {
            Log.e(TAG, "Failed to bind to service in time!");
            callbackContext.error("Failed to bind to service in time!");
        }

        callbackContext.success();
    }

    private void toggleNeighboringDevices(CordovaArgs data, CallbackContext callbackContext)
            throws JSONException {

        // get the argument
        boolean active = data.getBoolean(0);
        Log.v(TAG, (active ? "Enable" : "Disable") + " neighboring devices sensors");

        // do the call
        SenseServiceStub service = sensePlatform.getService();
        if (null != service) {
            service.toggleDeviceProx(active);
        } else {
            Log.e(TAG, "Failed to bind to service in time!");
            callbackContext.error("Failed to bind to service in time!");
        }

        callbackContext.success();
    }

    private void togglePhoneState(CordovaArgs data, CallbackContext callbackContext)
            throws JSONException {

        // get the argument
        boolean active = data.getBoolean(0);
        Log.v(TAG, (active ? "Enable" : "Disable") + " phone state sensors");

        // do the call
        SenseServiceStub service = sensePlatform.getService();
        if (null != service) {
            service.togglePhoneState(active);
        } else {
            Log.e(TAG, "Failed to bind to service in time!");
            callbackContext.error("Failed to bind to service in time!");
        }

        callbackContext.success();
    }

    private void togglePosition(CordovaArgs data, CallbackContext callbackContext)
            throws JSONException {

        // get the argument
        boolean active = data.getBoolean(0);
        Log.v(TAG, (active ? "Enable" : "Disable") + " position sensors");

        // do the call
        SenseServiceStub service = sensePlatform.getService();
        if (null != service) {
            service.toggleLocation(active);
        } else {
            Log.e(TAG, "Failed to bind to service in time!");
            callbackContext.error("Failed to bind to service in time!");
        }

        callbackContext.success();
    }
}
