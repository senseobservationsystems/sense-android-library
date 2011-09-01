package nl.sense_os.phonegap.plugins;

import org.json.JSONArray;
import org.json.JSONException;

import android.os.RemoteException;
import android.util.Log;

import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;

public class SensePlugin extends AbstractSensePlugin {

    private static class Actions {
        static final String BIND = "bind";
        static final String CHANGE_LOGIN = "change_login";
        static final String REGISTER = "register";
        static final String TOGGLE_MAIN = "toggle_main";
        static final String TOGGLE_AMBIENCE = "toggle_ambience";
        static final String TOGGLE_EXTERNAL = "toggle_external";
        static final String TOGGLE_MOTION = "toggle_motion";
        static final String TOGGLE_NEIGHDEV = "toggle_neighdev";
        static final String TOGGLE_PHONESTATE = "toggle_phonestate";
    }

    private static final String TAG = "PhoneGap Sense";

    private void changeLogin(JSONArray data, String callbackId) {
        Log.v(TAG, "Change login");

        // get the parameters
        String username = null, password = null;
        try {
            username = data.getString(0);
            password = data.getString(1);
        } catch (JSONException e) {
            Log.e(TAG, "JSONException getting login arguments", e);
            error(new PluginResult(Status.JSON_EXCEPTION, e.getMessage()), callbackId);
            return;
        }

        // try the login
        int result = -1;
        try {
            result = service.changeLogin(username, password);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException while trying to log in", e);
            error(new PluginResult(Status.ERROR, e.getMessage()), callbackId);
            return;
        }

        // check the result
        switch (result) {
            case 0 :
                Log.v(TAG, "Logged in");
                success(new PluginResult(Status.OK, "Logged in"), callbackId);
                break;
            case -1 :
                Log.v(TAG, "Login failed! Result: " + result);
                error(new PluginResult(Status.ERROR, "Login failed. Connectivity problems?"),
                        callbackId);
                break;
            case -2 :
                Log.v(TAG, "Login failed! Result: " + result);
                error(new PluginResult(Status.ERROR, "Login failed. Invalid username or password."),
                        callbackId);
                break;
            default :
                Log.w(TAG, "Unexpected login result! Result: " + result);
                error(new PluginResult(Status.ERROR, "Unexpected login result."), callbackId);
                break;
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
    public PluginResult execute(String action, JSONArray data, String callbackId) {
        try {
            if (action == null) {
                Log.e(TAG, "Invalid action: " + action);
                return new PluginResult(Status.INVALID_ACTION);
            } else if (action.equals(Actions.BIND)) {
                bindToSenseService();
                return new PluginResult(Status.NO_RESULT);
            } else if (action.equals(Actions.CHANGE_LOGIN)) {
                changeLogin(data, callbackId);
                return null;
            } else if (action.equals(Actions.REGISTER)) {
                register(data, callbackId);
                return null;
            } else if (action.equals(Actions.TOGGLE_AMBIENCE)) {
                return toggleAmbience(data, callbackId);
            } else if (action.equals(Actions.TOGGLE_EXTERNAL)) {
                return toggleExternal(data, callbackId);
            } else if (action.equals(Actions.TOGGLE_MAIN)) {
                return toggleMain(data, callbackId);
            } else if (action.equals(Actions.TOGGLE_MOTION)) {
                return toggleMotion(data, callbackId);
            } else if (action.equals(Actions.TOGGLE_NEIGHDEV)) {
                return toggleNeighboringDevices(data, callbackId);
            } else if (action.equals(Actions.TOGGLE_PHONESTATE)) {
                return togglePhoneState(data, callbackId);
            } else {
                Log.e(TAG, "Invalid action: " + action);
                return new PluginResult(Status.INVALID_ACTION);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while executing action: " + action, e);
            return new PluginResult(Status.ERROR, e.getMessage());
        }
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
        if (action == null) {
            Log.w(TAG, "Invalid action: " + action);
            return false;
        } else if (action.equals(Actions.BIND)) {
            return true;
        } else if (action.equals(Actions.REGISTER)) {
            return false;
        } else if (action.equals(Actions.CHANGE_LOGIN)) {
            return false;
        } else if (action.equals(Actions.TOGGLE_MAIN)) {
            return true;
        } else if (action.equals(Actions.TOGGLE_AMBIENCE) || action.equals(Actions.TOGGLE_EXTERNAL)
                || action.equals(Actions.TOGGLE_MOTION) || action.equals(Actions.TOGGLE_NEIGHDEV)
                || action.equals(Actions.TOGGLE_PHONESTATE)) {
            return true;
        } else {
            Log.w(TAG, "Invalid action: " + action);
            return false;
        }
    }

    private void register(JSONArray data, String callbackId) {
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
            error(new PluginResult(Status.JSON_EXCEPTION, e.getMessage()), callbackId);
            return;
        }

        // do the registration
        int result = -1;
        try {
            result = service.register(username, password, name, surname, email, phone);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException while trying to register", e);
            error(new PluginResult(Status.ERROR, e.getMessage()), callbackId);
            return;
        }

        // check the result
        switch (result) {
            case 0 :
                Log.v(TAG, "Registered successfully");
                success(new PluginResult(Status.OK, "Registered successfully"), callbackId);
                break;
            case -1 :
                Log.v(TAG, "Registration failed! Result: " + result);
                error(new PluginResult(Status.ERROR, "Registration failed. Connectivity problems?"),
                        callbackId);
                break;
            case -2 :
                Log.v(TAG, "Registration failed! Result: " + result);
                error(new PluginResult(Status.ERROR, "Registration failed. Username already taken."),
                        callbackId);
                break;
            default :
                Log.w(TAG, "Unexpected registration result! Result: " + result);
                error(new PluginResult(Status.ERROR, "Unexpected registration result."), callbackId);
                break;
        }
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
                service.toggleExternalSensors(active);
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
                service.toggleExternalSensors(active);
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
                service.toggleExternalSensors(active);
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
