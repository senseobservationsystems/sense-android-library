/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package com.phonegap.plugins.sense;

import java.io.IOException;
import java.util.Date;
import java.util.Locale;

import nl.sense_os.platform.SensePlatform;
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

import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;

/**
 * PhoneGap plugin implementation for the Sense Platform. Provides PhoneGap applications with an
 * interface to the native Sense service from JavaScript.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class SensePlugin extends Plugin {

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
				success(new PluginResult(Status.OK, result), changeLoginCallbackId);
				onLoginSuccess();
				break;
			case -1:
				Log.v(TAG, "Login failed! Connectivity problems?");
				error(new PluginResult(Status.IO_EXCEPTION,
						"Error logging in, probably connectivity problems."), changeLoginCallbackId);
				break;
			case -2:
				Log.v(TAG, "Login failed! Invalid username or password.");
				error(new PluginResult(Status.ERROR, "Invalid username or password."),
						changeLoginCallbackId);
				break;
			default:
				Log.w(TAG, "Unexpected login result! Unexpected result: " + result);
				error(new PluginResult(Status.ERROR, "Unexpected result: " + result),
						changeLoginCallbackId);
			}
		}

		@Override
		public void onRegisterResult(int result) throws RemoteException {
			switch (result) {
			case 0:
				Log.v(TAG, "Registration OK");
				success(new PluginResult(Status.OK, result), registerCallbackId);
				break;
			case -1:
				Log.v(TAG, "Registration failed! Connectivity problems?");
				error(new PluginResult(Status.IO_EXCEPTION, result), registerCallbackId);
				break;
			case -2:
				Log.v(TAG, "Registration failed! Username already taken.");
				error(new PluginResult(Status.ERROR, result), registerCallbackId);
				break;
			default:
				Log.w(TAG, "Unexpected registration result! Unexpected registration result: "
						+ result);
				error(new PluginResult(Status.ERROR, result), registerCallbackId);
				break;
			}
		}

		@Override
		public void statusReport(final int status) throws RemoteException {
			cordova.getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					// Log.d(TAG, "Received Sense Platform service status: " + status);
					SensePlugin.this.success(new PluginResult(Status.OK, status),
							getStatusCallbackId);
				}
			});
		}
	}

	private static final String TAG = "PhoneGap Sense";

	private static final String SECRET = "0$HTLi8e_}9^s7r#[_L~-ndz=t5z)e}I-ai#L22-?0+i7jfF2,~)oyi|H)q*GL$Y";

	private ISenseServiceCallback callback = new SenseServiceCallback();
	private String getStatusCallbackId;
	private String changeLoginCallbackId;
	private String registerCallbackId;

    private SensePlatform sensePlatform;

	private PluginResult addDataPoint(JSONArray data, String callbackId) throws JSONException {

		// get the parameters
		final String name = data.getString(0);
		final String displayName = data.getString(1);
		final String description = data.getString(2);
		final String dataType = data.getString(3);
		final String value = data.getString(4);
		final long timestamp = data.getLong(5);
		Log.v(TAG, "addDataPoint('" + name + "', '" + displayName + "', '" + description + "', '"
				+ dataType + "', '" + value + "', " + timestamp + ")");

        boolean result = sensePlatform.addDataPoint(name, displayName, description, dataType,
                value, timestamp);

        if (result) {
            return new PluginResult(Status.OK);
        } else {
            Log.w(TAG, "Could not start MsgHandler service!");
            return new PluginResult(Status.ERROR, "could not add data to Sense service");
        }
	}

	private PluginResult changeLogin(final JSONArray data, final String callbackId)
			throws JSONException, RemoteException {

		// get the parameters
        final String username = data.getString(0).toLowerCase(Locale.ENGLISH);
		final String password = data.getString(1);
		Log.v(TAG, "changeLogin('" + username + "', '" + password + "')");

        changeLoginCallbackId = callbackId;
        new Thread() {
            public void run() {
                try {
                    sensePlatform.login(username, password, callback);
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to call changeLogin()! " + e);
                    error(new PluginResult(Status.ERROR, e.getMessage()), changeLoginCallbackId);
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Failed to bind to service in time!");
                    error(new PluginResult(Status.ERROR, "Failed to bind to service in time!"),
                            changeLoginCallbackId);
                }
            };
        }.run();

		// keep the callback ID so we can use it when the service returns
		PluginResult r = new PluginResult(Status.NO_RESULT);
		r.setKeepCallback(true);
		return r;
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
		try {
			if (Actions.INIT.equals(action)) {
				return init(data, callbackId);
			} else if (Actions.ADD_DATA_POINT.equals(action)) {
				return addDataPoint(data, callbackId);
			} else if (Actions.CHANGE_LOGIN.equals(action)) {
				return changeLogin(data, callbackId);
			} else if (Actions.FLUSH_BUFFER.equals(action)) {
				return flushBuffer(data, callbackId);
			} else if (Actions.GET_COMMONSENSE_DATA.equals(action)) {
				return getRemoteValues(data, callbackId);
			} else if (Actions.GET_DATA.equals(action)) {
				return getLocalValues(data, callbackId);
			} else if (Actions.GET_PREF.equals(action)) {
				return getPreference(data, callbackId);
			} else if (Actions.GET_STATUS.equals(action)) {
				return getStatus(data, callbackId);
			} else if (Actions.GET_SESSION.equals(action)) {
				return getSession(data, callbackId);
			} else if (Actions.GIVE_FEEDBACK.equals(action)) {
				return giveFeedback(data, callbackId);
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

	private PluginResult flushBuffer(JSONArray data, String callbackId) {

		Log.v(TAG, "flushBuffer()");

        boolean result = sensePlatform.flushData();
        if (result) {
			return new PluginResult(Status.OK);
		} else {
			return new PluginResult(Status.INSTANTIATION_EXCEPTION);
		}
	}

	private PluginResult getLocalValues(JSONArray data, String callbackId) throws JSONException {

		final String sensorName = data.getString(0);
		Log.v(TAG, "getLocalValues('" + sensorName + "')");

		try {
			Uri uri = Uri.parse("content://"
					+ cordova.getActivity().getString(R.string.local_storage_authority)
					+ DataPoint.CONTENT_URI_PATH);
			JSONArray result = getValues(sensorName, null, uri);

			Log.v(TAG, "Found " + result.length() + " '" + sensorName
					+ "' data points in the local storage");
			return new PluginResult(Status.OK, result);

		} catch (JSONException e) {
			throw e;
		} catch (Exception e) {
			return new PluginResult(Status.ERROR, "" + e);
		}
	}

	private PluginResult getPreference(JSONArray data, String callbackId) throws JSONException,
			RemoteException {

		String key = data.getString(0);
		Log.v(TAG, "getPreference('" + key + "')");

        ISenseService service = sensePlatform.getService();
		if (key.equals(Main.SAMPLE_RATE) || key.equals(Main.SYNC_RATE)
				|| key.equals(Auth.LOGIN_USERNAME)) {
			String result = service.getPrefString(key, null);
			return new PluginResult(Status.OK, result);
		} else {
			boolean result = service.getPrefBool(key, false);
			return new PluginResult(Status.OK, result);
		}
	}

	private PluginResult getRemoteValues(JSONArray data, String callbackId) throws JSONException {

		final String sensorName = data.getString(0);
		final boolean onlyThisDevice = data.getBoolean(1);
		Log.v(TAG, "getRemoteValues('" + sensorName + "', " + onlyThisDevice + ")");

		try {
            JSONArray result = sensePlatform.getData(sensorName, onlyThisDevice);

            Log.v(TAG, "Found " + result.length() + " '" + sensorName
                    + "' data points in the CommonSense");
            return new PluginResult(Status.OK, result);

		} catch (JSONException e) {
			throw e;
		} catch (Exception e) {
			return new PluginResult(Status.ERROR, "" + e);
		}
	}

	private PluginResult getSession(JSONArray data, String callbackId) throws RemoteException {

		Log.v(TAG, "getSessionId()");

        ISenseService service = sensePlatform.getService();
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

		Log.v(TAG, "getStatus()");

        ISenseService service = sensePlatform.getService();
		if (null != service) {
			getStatusCallbackId = callbackId;
			service.getStatus(callback);
		} else {
			Log.e(TAG, "No connection to the Sense Platform service.");
			return new PluginResult(Status.ERROR, "No connection to the Sense Platform service.");
		}

		PluginResult r = new PluginResult(Status.NO_RESULT);
		r.setKeepCallback(true);
		return r;
	}

	/**
	 * Gets array of values from the LocalStorage
	 * 
	 * @param sensorName
	 * @param deviceUuid
	 * @param uri
	 * @return JSONArray with values for the sensor with the selected name and device
	 * @throws JSONException
	 */
	private JSONArray getValues(String sensorName, String deviceUuid, Uri uri) throws JSONException {

		Cursor cursor = null;

		try {
			JSONArray result = new JSONArray();

			String[] projection = new String[] { DataPoint.TIMESTAMP, DataPoint.VALUE };
			String selection = DataPoint.SENSOR_NAME + " = '" + sensorName + "'";
			if (null != deviceUuid) {
				selection += " AND " + DataPoint.DEVICE_UUID + "='" + deviceUuid + "'";
			}
			String[] selectionArgs = null;
			String sortOrder = null;
			cursor = LocalStorage.getInstance(cordova.getActivity()).query(uri, projection,
					selection, selectionArgs, sortOrder);

			if (null != cursor && cursor.moveToFirst()) {
				while (!cursor.isAfterLast()) {
					JSONObject val = new JSONObject();
					val.put("timestamp",
							cursor.getString(cursor.getColumnIndex(DataPoint.TIMESTAMP)));
					val.put("value", cursor.getString(cursor.getColumnIndex(DataPoint.VALUE)));
					result.put(val);
					cursor.moveToNext();
				}
			}

			return result;

		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private PluginResult giveFeedback(JSONArray data, final String callbackId) throws JSONException {

		// get the parameters
		final String name = data.getString(0);
		final long start = data.getLong(1);
		final long end = data.getLong(2);
		final String label = data.getString(3);

		Log.v(TAG, "giveFeedback('" + name + "', " + start + ", " + end + ", '" + label + "')");

        new Thread() {
            public void run() {
                try {
                    sensePlatform.giveFeedback(name, new Date(start), new Date(end), label);
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Failed to bind to service in time!");
                    error(new PluginResult(Status.ERROR, "Failed to bind to service in time!"),
                            callbackId);
                } catch (IOException e) {
                    error(new PluginResult(Status.IO_EXCEPTION, e.getMessage()), callbackId);
                } catch (JSONException e) {
                    error(new PluginResult(Status.IO_EXCEPTION, e.getMessage()), callbackId);
                }
            }
        }.start();

		// keep the callback ID so we can use it when the service returns
		PluginResult r = new PluginResult(Status.NO_RESULT);
		r.setKeepCallback(true);
		return r;
	}

	private PluginResult init(JSONArray data, String callbackId) {
        sensePlatform = new SensePlatform(cordova.getActivity());
		return new PluginResult(Status.OK);
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
		} else if (Actions.GET_STATUS.equals(action)) {
			return true;
		} else if (Actions.GET_PREF.equals(action)) {
			return true;
		} else if (Actions.GET_SESSION.equals(action)) {
			return true;
		} else if (Actions.SET_PREF.equals(action)) {
			return true;
		} else {
			return super.isSynch(action);
		}
	}

	private PluginResult logout(JSONArray data, String callbackId) throws RemoteException {
		Log.v(TAG, "logout()");
        sensePlatform.logout();
		return new PluginResult(Status.OK);
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
            ISenseService service = sensePlatform.getService();
			try {
				service.setPrefString(SensePrefs.Main.SAMPLE_RATE, "0");
				service.setPrefString(SensePrefs.Main.SYNC_RATE, "1");

				service.setPrefBool(SensePrefs.Main.Ambience.MIC, true);
				service.setPrefBool(SensePrefs.Main.Ambience.LIGHT, true);
				service.setPrefBool(SensePrefs.Main.Ambience.PRESSURE, false);
				service.setPrefBool(SensePrefs.Main.Ambience.CAMERA_LIGHT, true);
				service.setPrefBool(SensePrefs.Main.Ambience.AUDIO_SPECTRUM, false);
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

				service.toggleMain(true);

				service.setPrefBool(SensePrefs.Status.AUTOSTART, true);
			} catch (RemoteException e) {
				Log.e(TAG, "Failed to init special sense setttings for ivitality");
			}
		}
	}

	private PluginResult register(JSONArray data, String callbackId) throws JSONException,
			RemoteException {

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
        registerCallbackId = callbackId;
        new Thread() {
            public void run() {
                try {
                    sensePlatform.registerUser(username, password, email, address, zipCode,
                            country, name, surname, phone, callback);
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to call register()! " + e);
                    error(new PluginResult(Status.ERROR, e.getMessage()), registerCallbackId);
                } catch (IllegalStateException e) {
                    error(new PluginResult(Status.ERROR, "Failed to bind to service in time!"),
                            registerCallbackId);
                }
            };
        }.run();

		// keep the callback ID so we can use it when the service returns
		PluginResult r = new PluginResult(Status.NO_RESULT);
		r.setKeepCallback(true);
		return r;
	}

	private PluginResult setPreference(JSONArray data, String callbackId) throws JSONException,
			RemoteException {

		// get the preference key
		String key = data.getString(0);

		// get the preference value
        ISenseService service = sensePlatform.getService();
		if (key.equals(Main.SAMPLE_RATE) || key.equals(Main.SYNC_RATE)
				|| key.equals(Auth.LOGIN_USERNAME)) {
			String value = data.getString(1);
			Log.v(TAG, "setPreference('" + key + "', '" + value + "')");
			service.setPrefString(key, value);
			return new PluginResult(Status.OK);
		} else {
			boolean value = data.getBoolean(1);
			Log.v(TAG, "setPreference('" + key + "', " + value + ")");
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
        ISenseService service = sensePlatform.getService();
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
        ISenseService service = sensePlatform.getService();
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
        ISenseService service = sensePlatform.getService();
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
        ISenseService service = sensePlatform.getService();
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
        ISenseService service = sensePlatform.getService();
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
        ISenseService service = sensePlatform.getService();
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
        ISenseService service = sensePlatform.getService();
		if (null != service) {
			service.toggleLocation(active);
		} else {
			Log.e(TAG, "Failed to bind to service in time!");
			return new PluginResult(Status.ERROR, "Failed to bind to service in time!");
		}

		return new PluginResult(Status.OK);
	}
}
