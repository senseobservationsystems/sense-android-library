/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package com.phonegap.plugins.sense;

import nl.sense_os.service.ISenseService;
import nl.sense_os.service.ISenseServiceCallback;
import nl.sense_os.service.R;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Auth;
import nl.sense_os.service.constants.SensePrefs.Main;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.storage.LocalStorage;

import org.apache.cordova.api.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;

public class SensePlugin extends Plugin {

    /**
     * Standard action labels that correspond to the actions that are used in the JavaScript part of
     * the plugin.
     */
    private static class Actions {
        static final String ADD_DATA_POINT = "add_data_point";
        static final String CHANGE_LOGIN = "change_login";
        static final String GET_DATA = "get_data";
        static final String GET_STATUS = "get_status";
        static final String GET_SESSION = "get_session";
        static final String GET_PREF = "get_pref";
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

    /**
     * Service connection to handle connection with the Sense service. Manages the
     * <code>service</code> field when the service is connected or disconnected.
     */
    private class SenseServiceConn implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.v(TAG, "Connection to Sense Platform service established...");
            service = ISenseService.Stub.asInterface(binder);
            isServiceBound = true;

            // only for ivitality
            String packageName = ctx.getPackageName();
            if (packageName.equals("nl.sense_os.ivitality")) {
                Log.w(TAG, "Set special iVitality sensor settings");
                try {
                    service.setPrefString(SensePrefs.Main.SAMPLE_RATE, "0");
                    service.setPrefString(SensePrefs.Main.SYNC_RATE, "1");

                    service.setPrefBool(SensePrefs.Main.Ambience.MIC, true);
                    service.setPrefBool(SensePrefs.Main.Ambience.LIGHT, true);
                    service.setPrefBool(SensePrefs.Main.Ambience.PRESSURE, false);
                    service.setPrefBool(SensePrefs.Main.Ambience.CAMERA_LIGHT, true);
                    service.toggleAmbience(true);

                    service.setPrefBool(SensePrefs.Main.Motion.MOTION_ENERGY, true);
                    service.toggleMotion(true);

                    service.setPrefBool(SensePrefs.Main.PhoneState.BATTERY, true);
                    service.setPrefBool(SensePrefs.Main.PhoneState.PROXIMITY, true);
                    service.setPrefBool(SensePrefs.Main.PhoneState.SCREEN_ACTIVITY, true);
                    service.setPrefBool(SensePrefs.Main.PhoneState.CALL_STATE, false);
                    service.setPrefBool(SensePrefs.Main.PhoneState.DATA_CONNECTION, false);
                    service.setPrefBool(SensePrefs.Main.PhoneState.IP_ADDRESS, false);
                    service.setPrefBool(SensePrefs.Main.PhoneState.SERVICE_STATE, false);
                    service.setPrefBool(SensePrefs.Main.PhoneState.SIGNAL_STRENGTH, false);
                    service.setPrefBool(SensePrefs.Main.PhoneState.UNREAD_MSG, false);
                    service.togglePhoneState(true);

                    service.setPrefBool(SensePrefs.Status.AUTOSTART, true);
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to init default sense setttings");
                }
            } else if (packageName.equals("nl.ask.paige.app")) {
                // Log.w(TAG, "Set special Paige sensor settings");
                // try {
                // service.setPrefBool(SensePrefs.Main.Advanced.USE_COMMONSENSE, true);
                //
                // service.setPrefBool(SensePrefs.Main.Motion.FALL_DETECT_DEMO, true);
                // service.toggleMotion(true);
                //
                // } catch (RemoteException e) {
                // Log.e(TAG, "Failed to init default sense setttings");
                // }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.v(TAG, "Connection to Sense Platform service lost...");

            /* this is not called when the service is stopped, only when it is suddenly killed! */
            service = null;
            isServiceBound = false;
        }
    }

    private static final String TAG = "PhoneGap Sense";
    private static final String SECRET = "0$HTLi8e_}9^s7r#[_L~-ndz=t5z)e}I-ai#L22-?0+i7jfF2,~)oyi|H)q*GL$Y";
    private final ServiceConnection conn = new SenseServiceConn();
    private boolean isServiceBound;
    private ISenseService service;

    private PhoneGapSensorRegistrator sensorRegistrator;

    private PluginResult addDataPoint(JSONArray data, String callbackId) throws JSONException {

        // get the parameters
        final String name = data.getString(0);
        final String displayName = data.getString(1);
        final String description = data.getString(2);
        final String dataType = data.getString(3);
        final String value = data.getString(4);
        final long timestamp = data.getLong(5);
        Log.d(TAG, "Add data point... name: '" + name + "', display name: '" + displayName
                + "', description: '" + description + "', data type: '" + dataType + "', value: '"
                + value + "', timestamp: '" + timestamp + "'");

        // verify sensor ID
        if (null == sensorRegistrator) {
            sensorRegistrator = new PhoneGapSensorRegistrator(ctx.getContext());
        }
        new Thread() {

            @Override
            public void run() {
                sensorRegistrator.checkSensor(name, displayName, dataType, description, value,
                        null, null);
            }
        }.start();

        // send data point
        String action = ctx.getContext().getString(
                nl.sense_os.service.R.string.action_sense_new_data);
        Intent intent = new Intent(action);
        intent.putExtra(DataPoint.SENSOR_NAME, name);
        intent.putExtra(DataPoint.DISPLAY_NAME, displayName);
        intent.putExtra(DataPoint.SENSOR_DESCRIPTION, description);
        intent.putExtra(DataPoint.DATA_TYPE, dataType);
        intent.putExtra(DataPoint.VALUE, value);
        intent.putExtra(DataPoint.TIMESTAMP, timestamp);
        ComponentName serviceName = ctx.getContext().startService(intent);

        if (null != serviceName) {
            return new PluginResult(Status.OK);
        } else {
            Log.w(TAG, "Could not start MsgHandler service!");
            return new PluginResult(Status.ERROR, "could not add data to Sense service");
        }
    }

    /**
     * Binds to the Sense Service, creating it if necessary.
     */
    private void bindToSenseService() {
        if (!isServiceBound) {
            Log.v(TAG, "Try to connect with Sense Platform service");
            final Intent service = new Intent(ctx.getContext().getString(
                    R.string.action_sense_service));
            isServiceBound = ctx.getContext().bindService(service, conn, Context.BIND_AUTO_CREATE);
            if (!isServiceBound) {
                Log.w(TAG, "Failed to connect with the Sense Platform service!");
            }
        } else {
            // already bound
        }
    }

    private PluginResult changeLogin(final JSONArray data, final String callbackId)
            throws JSONException, RemoteException {
        Log.v(TAG, "Change login");

        if (null != service) {

            // get the parameters
            final String username = data.getString(0).toLowerCase();
            final String password = data.getString(1);

            // Log.d(TAG, "New username: '" + username + "'");
            // Log.d(TAG, "New password: '" + password + "'");

            int result = -1;
            try {
                result = service.changeLogin(username, password);
            } catch (RemoteException e) {
                // handle result below
            }

            // check the result
            switch (result) {
            case 0:
                Log.v(TAG, "Logged in as '" + username + "'");
                success(new PluginResult(Status.OK, result), callbackId);
                break;
            case -1:
                Log.v(TAG, "Login failed! Connectivity problems?");
                error(new PluginResult(Status.IO_EXCEPTION,
                        "Error logging in, probably connectivity problems."), callbackId);
                break;
            case -2:
                Log.v(TAG, "Login failed! Invalid username or password.");
                error(new PluginResult(Status.ERROR, "Invalid username or password."), callbackId);
                break;
            default:
                Log.w(TAG, "Unexpected login result! Unexpected result: " + result);
                error(new PluginResult(Status.ERROR, "Unexpected result: " + result), callbackId);
            }

            // the result was already sent back to JavaScript via success() or error()
            return new PluginResult(Status.NO_RESULT);

        } else {
            Log.e(TAG, "No connection to the Sense Platform service.");
            return new PluginResult(Status.ERROR, "No connection to the Sense Platform service.");
        }
    }

    /**
     * Executes the request and returns PluginResult.
     * 
     * @param action
     *            The action to execute.
     * @param args
     *            JSONArray of arguments for the plugin.
     * @param callbackId
     *            The callback id used when calling back into JavaScript.
     * @return A PluginResult object with a status and message.
     */
    @Override
    public PluginResult execute(String action, final JSONArray data, final String callbackId) {
        // Log.d(TAG, "Execute action: '" + action + "'");
        try {
            if (Actions.INIT.equals(action)) {
                return init(data, callbackId);
            } else if (Actions.ADD_DATA_POINT.equals(action)) {
                return addDataPoint(data, callbackId);
            } else if (Actions.CHANGE_LOGIN.equals(action)) {
                return changeLogin(data, callbackId);
            } else if (Actions.GET_DATA.equals(action)) {
                return getValues(data, callbackId);
            } else if (Actions.GET_PREF.equals(action)) {
                return getPreference(data, callbackId);
            } else if (Actions.GET_STATUS.equals(action)) {
                return getStatus(data, callbackId);
            } else if (Actions.GET_SESSION.equals(action)) {
                return getSession(data, callbackId);
            } else if (Actions.LOGOUT.equals(action)) {
                return logout(data, callbackId);
            } else if (Actions.REGISTER.equals(action)) {
                return register(data, callbackId);
            } else if (Actions.SET_PREF.equals(action)) {
                return setPreference(data, callbackId);
            } else if (Actions.TOGGLE_AMBIENCE.equals(action)) {
                return toggleAmbience(data, callbackId);
            } else if (Actions.TOGGLE_EXTERNAL.equals(action)) {
                return toggleExternal(data, callbackId);
            } else if (Actions.TOGGLE_MAIN.equals(action)) {
                return toggleMain(data, callbackId);
            } else if (Actions.TOGGLE_MOTION.equals(action)) {
                return toggleMotion(data, callbackId);
            } else if (Actions.TOGGLE_NEIGHDEV.equals(action)) {
                return toggleNeighboringDevices(data, callbackId);
            } else if (Actions.TOGGLE_PHONESTATE.equals(action)) {
                return togglePhoneState(data, callbackId);
            } else if (Actions.TOGGLE_POSITION.equals(action)) {
                return togglePosition(data, callbackId);
            } else {
                Log.e(TAG, "Invalid action: '" + action + "'");
                return new PluginResult(Status.INVALID_ACTION);
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSONException getting arguments for action '" + action + "'", e);
            return new PluginResult(Status.JSON_EXCEPTION, e.getMessage());
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException communicating with Sense Platform for action '" + action
                    + "'", e);
            return new PluginResult(Status.ERROR, e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while executing action: " + action, e);
            return new PluginResult(Status.ERROR, e.getMessage());
        }
    }

    private PluginResult getPreference(JSONArray data, String callbackId) throws JSONException,
            RemoteException {

        String key = data.getString(0);

        if (key.equals(Main.SAMPLE_RATE) || key.equals(Main.SYNC_RATE)
                || key.equals(Auth.LOGIN_USERNAME)) {
            String result = service.getPrefString(key, null);
            return new PluginResult(Status.OK, result);
        } else {
            boolean result = service.getPrefBool(key, false);
            return new PluginResult(Status.OK, result);
        }
    }

    private PluginResult getSession(JSONArray data, String callbackId) throws RemoteException {
        if (null != service) {

            // try the login
            String sessionId = service.getSessionId(SECRET);

            // check the result
            if (null != sessionId) {
                Log.v(TAG, "Received session ID from Sense");
                return new PluginResult(Status.OK, sessionId);
            } else {
                Log.v(TAG, "No session ID");
                return new PluginResult(Status.ERROR, "No session ID.");
            }

        } else {
            Log.e(TAG, "No connection to the Sense Platform service.");
            return new PluginResult(Status.ERROR, "No connection to the Sense Platform service.");
        }
    }

    private PluginResult getStatus(JSONArray data, final String callbackId) throws RemoteException {
        if (null != service) {
            service.getStatus(new ISenseServiceCallback.Stub() {

                @Override
                public void statusReport(final int status) throws RemoteException {
                    ctx.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            // Log.d(TAG, "Received Sense Platform service status: " + status);
                            SensePlugin.this.success(new PluginResult(Status.OK, status),
                                    callbackId);
                        }
                    });
                }
            });
        } else {
            Log.e(TAG, "No connection to the Sense Platform service.");
            return new PluginResult(Status.ERROR, "No connection to the Sense Platform service.");
        }

        PluginResult r = new PluginResult(Status.NO_RESULT);
        r.setKeepCallback(true);
        return r;
    }

    /** Test function to print all location values saved in the sense app */
    private PluginResult getValues(JSONArray data, String callbackId) throws JSONException,
            RemoteException {
        Cursor cursor = null;
        String sensorName = data.getString(0);
        JSONArray returnvalue = new JSONArray();
        try {
            Uri url = Uri.parse("content://"
                    + ctx.getContext().getString(R.string.local_storage_authority)
                    + DataPoint.CONTENT_URI_PATH);
            String[] projection = new String[] { DataPoint.TIMESTAMP, DataPoint.VALUE };
            String selection = DataPoint.SENSOR_NAME + " = '" + sensorName + "'";
            String[] selectionArgs = null;
            String sortOrder = null;
            cursor = LocalStorage.getInstance(ctx.getApplicationContext()).query(url, projection,
                    selection, selectionArgs, sortOrder);

            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    JSONObject val = new JSONObject();
                    val.put("timestamp",
                            cursor.getString(cursor.getColumnIndex(DataPoint.TIMESTAMP)));
                    val.put("value", cursor.getString(cursor.getColumnIndex(DataPoint.VALUE)));
                    returnvalue.put(val);
                    cursor.moveToNext();
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (returnvalue.length() == 0) {
            Log.v(TAG, "No '" + sensorName + "' data points found in the local storage");
            return new PluginResult(Status.NO_RESULT);
        }
        Log.v(TAG, "Found " + returnvalue.length() + " '" + sensorName
                + "' data points in the local storage");
        return new PluginResult(Status.OK, returnvalue);
    }

    private PluginResult init(JSONArray data, String callbackId) {
        if (!isSenseInstalled()) {
            Log.w(TAG, "Sense is not installed!");
            ctx.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    // AlertDialog d = InstallSenseDialog.create(ctx);
                    // d.show();
                }
            });
            return new PluginResult(Status.ERROR);
        } else {
            bindToSenseService();
            return new PluginResult(Status.OK);
        }
    }

    private boolean isSenseInstalled() {
        // Sense is always installed because it is packaged with Paige
        return true;
    }

    /**
     * Identifies if action to be executed returns a value and should be run synchronously.
     * 
     * @param action
     *            The action to execute
     * @return true
     */
    @Override
    public boolean isSynch(String action) {
        if (Actions.INIT.equals(action)) {
            return true;
        } else if (Actions.CHANGE_LOGIN.equals(action)) {
            return false;
        } else if (Actions.REGISTER.equals(action)) {
            return false;
        } else if (Actions.GET_STATUS.equals(action)) {
            return true;
        } else if (Actions.GET_PREF.equals(action)) {
            return true;
        } else if (Actions.GET_SESSION.equals(action)) {
            return true;
        } else if (Actions.GET_DATA.equals(action)) {
            return true;
        } else if (Actions.SET_PREF.equals(action)) {
            return true;
        } else if (Actions.TOGGLE_MAIN.equals(action)) {
            return true;
        } else if (Actions.TOGGLE_AMBIENCE.equals(action) || Actions.TOGGLE_EXTERNAL.equals(action)
                || Actions.TOGGLE_MOTION.equals(action) || Actions.TOGGLE_NEIGHDEV.equals(action)
                || Actions.TOGGLE_PHONESTATE.equals(action)
                || Actions.TOGGLE_POSITION.equals(action)) {
            return true;
        } else {
            return super.isSynch(action);
        }
    }

    private PluginResult logout(JSONArray data, String callbackId) throws RemoteException {
        service.logout();
        return new PluginResult(Status.OK);
    }

    /**
     * The final call you receive before your activity is destroyed.
     */
    @Override
    public void onDestroy() {
        unbindFromSenseService();
        super.onDestroy();
    }

    private PluginResult register(JSONArray data, String callbackId) {
        Log.v(TAG, "Register");

        // get the parameters
        String username = null, password = null, name = null, surname = null, email = null, phone = null;
        try {
            username = data.getString(0);
            password = data.getString(1);
            name = data.getString(2);
            surname = data.getString(3);
            email = data.getString(4);
            phone = data.getString(5);
        } catch (JSONException e) {
            Log.e(TAG, "JSONException getting registration arguments", e);
            return new PluginResult(Status.JSON_EXCEPTION, e.getMessage());
        }

        // do the registration
        int result = -1;
        if (null != service) {
            try {
                result = service.register(username, password, name, surname, email, phone);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException while trying to register", e);
                return new PluginResult(Status.ERROR, e.getMessage());
            }
        } else {
            Log.e(TAG, "Failed to bind to service in time!");
            return new PluginResult(Status.ERROR, "Failed to bind to service in time!");
        }

        // check the result
        switch (result) {
        case 0:
            Log.v(TAG, "Registered '" + username + "'");
            success(new PluginResult(Status.OK, result), callbackId);
            break;
        case -1:
            Log.v(TAG, "Registration failed! Connectivity problems?");
            error(new PluginResult(Status.IO_EXCEPTION, result), callbackId);
            break;
        case -2:
            Log.v(TAG, "Registration failed! Username already taken.");
            error(new PluginResult(Status.ERROR, result), callbackId);
            break;
        default:
            Log.w(TAG, "Unexpected registration result! Unexpected registration result: " + result);
            error(new PluginResult(Status.ERROR, result), callbackId);
            break;
        }

        // the result was already sent back to JavaScript via success() or error()
        return new PluginResult(Status.NO_RESULT);
    }

    private PluginResult setPreference(JSONArray data, String callbackId) throws JSONException,
            RemoteException {

        // get the preference key
        String key = data.getString(0);

        // get the preference value
        if (key.equals(Main.SAMPLE_RATE) || key.equals(Main.SYNC_RATE)
                || key.equals(Auth.LOGIN_USERNAME)) {
            String value = data.getString(1);
            Log.v(TAG, "Set preference '" + key + "': '" + value + "'");
            service.setPrefString(key, value);
            return new PluginResult(Status.OK);
        } else {
            boolean value = data.getBoolean(1);
            Log.v(TAG, "Set preference '" + key + "': " + value);
            service.setPrefBool(key, value);
            return new PluginResult(Status.OK);
        }
    }

    private PluginResult toggleAmbience(JSONArray data, String callbackId) throws RemoteException,
            JSONException {

        // get the argument
        boolean active = data.getBoolean(0);
        Log.v(TAG, (active ? "Enable" : "Disable") + " ambience sensors");

        // do the call
        if (null != service) {
            service.toggleAmbience(active);
        } else {
            Log.e(TAG, "Failed to bind to service in time!");
            return new PluginResult(Status.ERROR, "Failed to bind to service in time!");
        }

        return new PluginResult(Status.OK);
    }

    private PluginResult toggleExternal(JSONArray data, String callbackId) throws RemoteException,
            JSONException {

        // get the argument
        boolean active = data.getBoolean(0);
        Log.v(TAG, (active ? "Enable" : "Disable") + " external sensors");

        // do the call
        if (null != service) {
            service.toggleExternalSensors(active);
        } else {
            Log.e(TAG, "Failed to bind to service in time!");
            return new PluginResult(Status.ERROR, "Failed to bind to service in time!");
        }

        return new PluginResult(Status.OK);
    }

    private PluginResult toggleMain(JSONArray data, String callbackId) throws RemoteException,
            JSONException {

        // get the argument
        boolean active = data.getBoolean(0);
        Log.v(TAG, (active ? "Enable" : "Disable") + " main status");

        // do the call
        if (null != service) {
            service.toggleMain(active);
        } else {
            Log.e(TAG, "Failed to bind to service in time!");
            return new PluginResult(Status.ERROR, "Failed to bind to service in time!");
        }

        return new PluginResult(Status.OK);
    }

    private PluginResult toggleMotion(JSONArray data, String callbackId) throws RemoteException,
            JSONException {

        // get the argument
        boolean active = data.getBoolean(0);
        Log.v(TAG, (active ? "Enable" : "Disable") + " motion sensors");

        // do the call
        if (null != service) {
            service.toggleMotion(active);
        } else {
            Log.e(TAG, "Failed to bind to service in time!");
            return new PluginResult(Status.ERROR, "Failed to bind to service in time!");
        }

        return new PluginResult(Status.OK);
    }

    private PluginResult toggleNeighboringDevices(JSONArray data, String callbackId)
            throws JSONException, RemoteException {

        // get the argument
        boolean active = data.getBoolean(0);
        Log.v(TAG, (active ? "Enable" : "Disable") + " neighboring devices sensors");

        // do the call
        if (null != service) {
            service.toggleDeviceProx(active);
        } else {
            Log.e(TAG, "Failed to bind to service in time!");
            return new PluginResult(Status.ERROR, "Failed to bind to service in time!");
        }

        return new PluginResult(Status.OK);
    }

    private PluginResult togglePhoneState(JSONArray data, String callbackId) throws JSONException,
            RemoteException {

        // get the argument
        boolean active = data.getBoolean(0);
        Log.v(TAG, (active ? "Enable" : "Disable") + " phone state sensors");

        // do the call
        if (null != service) {
            service.togglePhoneState(active);
        } else {
            Log.e(TAG, "Failed to bind to service in time!");
            return new PluginResult(Status.ERROR, "Failed to bind to service in time!");
        }

        return new PluginResult(Status.OK);
    }

    private PluginResult togglePosition(JSONArray data, String callbackId) throws JSONException,
            RemoteException {

        // get the argument
        boolean active = data.getBoolean(0);
        Log.v(TAG, (active ? "Enable" : "Disable") + " position sensors");

        // do the call
        if (null != service) {
            service.toggleLocation(active);
        } else {
            Log.e(TAG, "Failed to bind to service in time!");
            return new PluginResult(Status.ERROR, "Failed to bind to service in time!");
        }

        return new PluginResult(Status.OK);
    }

    /**
     * Unbinds from the Sense service, resets {@link #service} and {@link #isServiceBound}.
     */
    private void unbindFromSenseService() {
        if (true == isServiceBound && null != conn) {
            Log.v(TAG, "Unbind from Sense Platform service");
            ctx.getContext().unbindService(conn);
        } else {
            // already unbound
        }
        service = null;
        isServiceBound = false;
    }
}
