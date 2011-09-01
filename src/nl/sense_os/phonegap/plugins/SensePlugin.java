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
    }

    private static final String TAG = "PhoneGap Sense";

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

        if (action == null) {
            Log.e(TAG, "Cannot execute action. action=" + action);
            return new PluginResult(Status.INVALID_ACTION);

        } else if (action.equals(Actions.BIND)) {
            bindToSenseService();
            return new PluginResult(Status.NO_RESULT);

        } else if (action.equals(Actions.CHANGE_LOGIN)) {
            changeLogin(data, callbackId);

        } else if (action.equals(Actions.REGISTER)) {
            register(data, callbackId);

        } else if (action.equals(Actions.TOGGLE_MAIN)) {
            return toggleMain(data, callbackId);

        } else {
            Log.e(TAG, "Cannot execute action. action=" + action);
            return new PluginResult(Status.INVALID_ACTION);
        }

        return null;
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
            return false;

        } else if (action.equals(Actions.BIND)) {
            return true;

        } else if (action.equals(Actions.REGISTER)) {
            return false;

        } else if (action.equals(Actions.CHANGE_LOGIN)) {
            return false;

        } else if (action.equals(Actions.TOGGLE_MAIN)) {
            return true;

        }

        return true;
    }

    private void changeLogin(JSONArray data, String callbackId) {
        Log.v(TAG, "change login");

        int status = -1;
        try {
            if (data.length() == 2) {
                String username = data.getString(0);
                String password = data.getString(1);
                status = service.changeLogin(username, password);
            } else {
                Log.d(TAG, "incorrect data length: " + data.length());
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSONException while trying to log in", e);
            error(new PluginResult(Status.JSON_EXCEPTION, e.getMessage()), callbackId);
            return;
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException while trying to log in", e);
            error(new PluginResult(Status.ERROR, e.getMessage()), callbackId);
            return;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected exception while trying to log in", e);
            error(new PluginResult(Status.ERROR, e.getMessage()), callbackId);
            return;
        }

        if (status == 0) {
            success(new PluginResult(Status.OK, "Logged in"), callbackId);
        } else {
            success(new PluginResult(Status.OK, "Did not log in. Status: " + status), callbackId);
        }
    }

    private void register(JSONArray data, String callbackId) {
        Log.v(TAG, "register");

        int status = -1;
        try {
            if (data.length() == 2) {
                String username = data.getString(0);
                String password = data.getString(1);
                String name = data.getString(2);
                String surname = data.getString(3);
                String email = data.getString(4);
                String phone = data.getString(5);
                status = service.register(username, password, name, surname, email, phone);
            } else {
                Log.w(TAG, "incorrect data length: " + data.length());
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSONException while trying to log in", e);
            error(new PluginResult(Status.JSON_EXCEPTION, e.getMessage()), callbackId);
            return;
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException while trying to log in", e);
            error(new PluginResult(Status.ERROR, e.getMessage()), callbackId);
            return;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected exception while trying to log in", e);
            error(new PluginResult(Status.ERROR, e.getMessage()), callbackId);
            return;
        }

        if (status == 0) {
            success(new PluginResult(Status.OK, "Registered"), callbackId);
        } else {
            success(new PluginResult(Status.OK, "Did not register. Status: " + status), callbackId);
        }
    }

    private PluginResult toggleMain(JSONArray data, String callbackId) {
        Log.v(TAG, "toggle main status");

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 5000 && service == null) {
            ;
        }

        if (null != service) {
            try {
                service.toggleMain(true);
            } catch (RemoteException e) {
                Log.e(TAG, "Exception starting Sense Platform service!");
                return new PluginResult(Status.ERROR);
            }
        } else {
            Log.e(TAG, "Failed to bind to Sense Platform service in time!");
            return new PluginResult(Status.ERROR);
        }

        return new PluginResult(Status.OK);
    }
}
