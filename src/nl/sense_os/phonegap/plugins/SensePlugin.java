package nl.sense_os.phonegap.plugins;

import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;

import nl.sense_os.service.ISenseServiceCallback;

import org.json.JSONArray;
import org.json.JSONException;

public class SensePlugin extends SenseConnectedPlugin {

    private static class Actions {
        static final String CHANGE_LOGIN = "change_login";
        static final String REGISTER = "register";
        static final String GET_STATUS = "get_status";
        static final String TOGGLE_MAIN = "toggle_main";
        static final String TOGGLE_AMBIENCE = "toggle_ambience";
        static final String TOGGLE_EXTERNAL = "toggle_external";
        static final String TOGGLE_MOTION = "toggle_motion";
        static final String TOGGLE_NEIGHDEV = "toggle_neighdev";
        static final String TOGGLE_PHONESTATE = "toggle_phonestate";
    }

    private static final String TAG = "PhoneGap Sense";

    private PluginResult changeLogin(final JSONArray data, final String callbackId)
            throws JSONException, RemoteException {
        Log.v(TAG, "Change login");

        if (null != service) {

            // get the parameters
            String username = null, password = null;
            username = data.getString(0);
            password = data.getString(1);

            // try the login
            int result = service.changeLogin(username, password);

            // check the result
            switch (result) {
            case 0:
                Log.v(TAG, "Logged in as '" + username + "'");
                return new PluginResult(Status.OK, result);
            case -1:
                Log.v(TAG, "Login failed! Connectivity problems?");
                return new PluginResult(Status.IO_EXCEPTION,
                        "Error loggin in, probably connectivity problems.");
            case -2:
                Log.v(TAG, "Login failed! Invalid username or password.");
                return new PluginResult(Status.ERROR, "Invalid username or password.");
            default:
                Log.w(TAG, "Unexpected login result! Unexpected result: " + result);
                return new PluginResult(Status.ERROR, "Unexpected result: " + result);
            }

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
        Log.d(TAG, "Execute action: '" + action + "'");
        try {
            if (action.equals("test")) {
                return test();
            } else if (Actions.CHANGE_LOGIN.equals(action)) {
                return changeLogin(data, callbackId);
            } else if (Actions.GET_STATUS.equals(action)) {
                return getStatus(data, callbackId);
            } else if (Actions.REGISTER.equals(action)) {
                return register(data, callbackId);
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
            } else {
                return super.execute(action, data, callbackId);
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

    private PluginResult getStatus(JSONArray data, final String callbackId) throws RemoteException {
        // TODO does not work
        if (null != service) {
            service.getStatus(new ISenseServiceCallback.Stub() {

                @Override
                public void statusReport(final int status) throws RemoteException {
                    ctx.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Log.d(TAG, "Received Sense Platform service status: " + status);
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

    /**
     * Identifies if action to be executed returns a value and should be run synchronously.
     * 
     * @param action
     *            The action to execute
     * @return true
     */
    @Override
    public boolean isSynch(String action) {
        boolean synch = false;
        if ("test".equals(action)) {
            synch = true;
        } else if (Actions.CHANGE_LOGIN.equals(action)) {
            synch = true;
        } else if (Actions.REGISTER.equals(action)) {
            synch = true;
        } else if (Actions.GET_STATUS.equals(action)) {
            synch = true;
        } else if (Actions.TOGGLE_MAIN.equals(action)) {
            synch = true;
        } else if (Actions.TOGGLE_AMBIENCE.equals(action) || Actions.TOGGLE_EXTERNAL.equals(action)
                || Actions.TOGGLE_MOTION.equals(action) || Actions.TOGGLE_NEIGHDEV.equals(action)
                || Actions.TOGGLE_PHONESTATE.equals(action)) {
            synch = true;
        } else {
            synch = super.isSynch(action);
        }
        return synch;
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
        Status status = null;
        switch (result) {
        case 0:
            Log.v(TAG, "Registered '" + username + "'");
            status = Status.OK;
            break;
        case -1:
            Log.v(TAG, "Registration failed! Connectivity problems?");
            status = Status.IO_EXCEPTION;
            break;
        case -2:
            Log.v(TAG, "Registration failed! Username already taken.");
            status = Status.ERROR;
            break;
        default:
            Log.w(TAG, "Unexpected registration result! Unexpected registration result: " + result);
            status = Status.ERROR;
            break;
        }
        return new PluginResult(status, result);
    }

    private PluginResult test() {
        ctx.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(ctx, "test", Toast.LENGTH_SHORT).show();
            }
        });
        PluginResult r = new PluginResult(Status.OK);
        // r.setKeepCallback(true);
        return r;
    }

    private PluginResult toggleAmbience(JSONArray data, String callbackId) {
        Log.v(TAG, "Toggle ambience sensors");

        // get the argument
        boolean active = false;
        try {
            active = data.getBoolean(0);
        } catch (JSONException e) {
            Log.e(TAG, "JSONException getting argument for toggling ambience sensors", e);
            return new PluginResult(Status.JSON_EXCEPTION, e.getMessage());
        }

        // do the call
        if (null != service) {
            try {
                service.toggleAmbience(active);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException toggling ambience sensors!");
                return new PluginResult(Status.ERROR, e.getMessage());
            }
        } else {
            Log.e(TAG, "Failed to bind to service in time!");
            return new PluginResult(Status.ERROR, "Failed to bind to service in time!");
        }

        return new PluginResult(Status.OK);
    }

    private PluginResult toggleExternal(JSONArray data, String callbackId) {
        Log.v(TAG, "Toggle external sensors");

        // get the argument
        boolean active = false;
        try {
            active = data.getBoolean(0);
        } catch (JSONException e) {
            Log.e(TAG, "JSONException getting argument for toggling external sensors", e);
            return new PluginResult(Status.JSON_EXCEPTION, e.getMessage());
        }

        // do the call
        if (null != service) {
            try {
                service.toggleExternalSensors(active);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException toggling external sensors!");
                return new PluginResult(Status.ERROR, e.getMessage());
            }
        } else {
            Log.e(TAG, "Failed to bind to service in time!");
            return new PluginResult(Status.ERROR, "Failed to bind to service in time!");
        }

        return new PluginResult(Status.OK);
    }

    private PluginResult toggleMain(JSONArray data, String callbackId) {
        Log.v(TAG, "Toggle main status");

        // get the argument
        boolean active = false;
        try {
            active = data.getBoolean(0);
        } catch (JSONException e) {
            Log.e(TAG, "JSONException getting argument for toggleMain", e);
            return new PluginResult(Status.JSON_EXCEPTION, e.getMessage());
        }

        // do the call
        if (null != service) {
            try {
                service.toggleMain(active);
            } catch (RemoteException e) {
                Log.e(TAG, "Exception toggling main status!");
                return new PluginResult(Status.ERROR, e.getMessage());
            }
        } else {
            Log.e(TAG, "Failed to bind to service in time!");
            return new PluginResult(Status.ERROR, "Failed to bind to service in time!");
        }

        return new PluginResult(Status.OK);
    }

    private PluginResult toggleMotion(JSONArray data, String callbackId) {
        Log.v(TAG, "Toggle motion sensors");

        // get the argument
        boolean active = false;
        try {
            active = data.getBoolean(0);
        } catch (JSONException e) {
            Log.e(TAG, "JSONException getting argument for toggling motion sensors", e);
            return new PluginResult(Status.JSON_EXCEPTION, e.getMessage());
        }

        // do the call
        if (null != service) {
            try {
                service.toggleMotion(active);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException toggling motion sensors!");
                return new PluginResult(Status.ERROR, e.getMessage());
            }
        } else {
            Log.e(TAG, "Failed to bind to service in time!");
            return new PluginResult(Status.ERROR, "Failed to bind to service in time!");
        }

        return new PluginResult(Status.OK);
    }

    private PluginResult toggleNeighboringDevices(JSONArray data, String callbackId) {
        Log.v(TAG, "Toggle neighboring devices sensors");

        // get the argument
        boolean active = false;
        try {
            active = data.getBoolean(0);
        } catch (JSONException e) {
            Log.e(TAG, "JSONException getting argument for toggling neighboring devices sensors", e);
            return new PluginResult(Status.JSON_EXCEPTION, e.getMessage());
        }

        // do the call
        if (null != service) {
            try {
                service.toggleDeviceProx(active);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException toggling neighboring devices sensors!");
                return new PluginResult(Status.ERROR, e.getMessage());
            }
        } else {
            Log.e(TAG, "Failed to bind to service in time!");
            return new PluginResult(Status.ERROR, "Failed to bind to service in time!");
        }

        return new PluginResult(Status.OK);
    }

    private PluginResult togglePhoneState(JSONArray data, String callbackId) {
        Log.v(TAG, "Toggle phone state sensors");

        // get the argument
        boolean active = false;
        try {
            active = data.getBoolean(0);
        } catch (JSONException e) {
            Log.e(TAG, "JSONException getting argument for toggling phone state sensors", e);
            return new PluginResult(Status.JSON_EXCEPTION, e.getMessage());
        }

        // do the call
        if (null != service) {
            try {
                service.togglePhoneState(active);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException toggling phone state sensors!");
                return new PluginResult(Status.ERROR, e.getMessage());
            }
        } else {
            Log.e(TAG, "Failed to bind to service in time!");
            return new PluginResult(Status.ERROR, "Failed to bind to service in time!");
        }

        return new PluginResult(Status.OK);
    }
}
