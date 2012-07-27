package nl.sense_os.service;

import nl.sense_os.service.commonsense.SenseApi;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Auth;
import nl.sense_os.service.constants.SensePrefs.Main.Advanced;
import nl.sense_os.service.constants.SensePrefs.Status;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.RemoteException;
import android.util.Log;

/**
 * Implementation of the service's AIDL interface. Very closely linked with {@link SenseService}.
 */
public class SenseServiceStub extends ISenseService.Stub {

    private static final String TAG = "SenseServiceStub";
    private SenseService service;

    public SenseServiceStub(SenseService service) {
	super();
	this.service = service;
    }

    @Override
    public void changeLogin(final String username, final String password,
	    final ISenseServiceCallback callback) throws RemoteException {
	// Log.v(TAG, "Change login");

	// perform login on separate thread and respond via callback
	new Thread() {

	    @Override
	    public void run() {
		int result = service.changeLogin(username, password);
		try {
		    callback.onChangeLoginResult(result);
		} catch (RemoteException e) {
		    Log.e(TAG, "Failed to call back to bound activity after login change: " + e);
		}
	    }
	}.start();
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean getPrefBool(String key, boolean defValue) throws RemoteException {
	// Log.v(TAG, "Get preference: " + key);
	SharedPreferences prefs;
	if (key.equals(Status.AMBIENCE) || key.equals(Status.DEV_PROX)
		|| key.equals(Status.EXTERNAL) || key.equals(Status.LOCATION)
		|| key.equals(Status.MAIN) || key.equals(Status.MOTION)
		|| key.equals(Status.PHONESTATE) || key.equals(Status.POPQUIZ)
		|| key.equals(Status.AUTOSTART)) {
	    prefs = service.getSharedPreferences(SensePrefs.STATUS_PREFS, Context.MODE_PRIVATE);
	} else {
	    prefs = service.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
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
	SharedPreferences prefs = service.getSharedPreferences(SensePrefs.MAIN_PREFS,
		Context.MODE_PRIVATE);
	try {
	    return prefs.getFloat(key, defValue);
	} catch (ClassCastException e) {
	    return defValue;
	}
    }

    @Override
    public int getPrefInt(String key, int defValue) throws RemoteException {
	// Log.v(TAG, "Get preference: " + key);
	SharedPreferences prefs = service.getSharedPreferences(SensePrefs.MAIN_PREFS,
		Context.MODE_PRIVATE);
	try {
	    return prefs.getInt(key, defValue);
	} catch (ClassCastException e) {
	    return defValue;
	}
    }

    @Override
    public long getPrefLong(String key, long defValue) throws RemoteException {
	// Log.v(TAG, "Get preference: " + key);
	SharedPreferences prefs;
	if (key.equals(Auth.SENSOR_LIST_COMPLETE_TIME)) {
	    prefs = service.getSharedPreferences(SensePrefs.AUTH_PREFS, Context.MODE_PRIVATE);
	} else {
	    prefs = service.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
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
	SharedPreferences prefs;
	if (key.equals(Auth.LOGIN_COOKIE) || key.equals(Auth.LOGIN_PASS)
		|| key.equals(Auth.LOGIN_USERNAME) || key.equals(Auth.SENSOR_LIST_COMPLETE)
		|| key.equals(Auth.DEVICE_ID) || key.equals(Auth.PHONE_IMEI)
		|| key.equals(Auth.PHONE_TYPE)) {
	    prefs = service.getSharedPreferences(SensePrefs.AUTH_PREFS, Context.MODE_PRIVATE);
	} else {
	    // all other preferences
	    prefs = service.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
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
	    return SenseApi.getSessionId(service, appSecret);
	} catch (IllegalAccessException e) {
	    throw new RemoteException();
	}
    }

    @Override
    public void getStatus(ISenseServiceCallback callback) throws RemoteException {
	callback.statusReport(ServiceStateHelper.getInstance(service).getStatusCode());
    }

    public void logout() {
	service.logout();
    }

    @Override
    public void register(final String username, final String password, final String email,
	    final String address, final String zipCode, final String country, final String name,
	    final String surname, final String mobile, final ISenseServiceCallback callback)
	    throws RemoteException {
	// Log.v(TAG, "Register: '" + username + "'");

	// perform registration on separate thread and respond via callback
	new Thread() {

	    @Override
	    public void run() {
		int result = service.register(username, password, email, address, zipCode, country,
			name, surname, mobile);
		try {
		    callback.onRegisterResult(result);
		} catch (RemoteException e) {
		    Log.e(TAG, "Failed to call back to bound activity after registration: " + e);
		}
	    }
	}.start();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setPrefBool(String key, final boolean value) throws RemoteException {
	// Log.v(TAG, "Set preference: '" + key + "': '" + value + "'");

	SharedPreferences prefs;
	if (key.equals(Status.AMBIENCE) || key.equals(Status.DEV_PROX)
		|| key.equals(Status.EXTERNAL) || key.equals(Status.LOCATION)
		|| key.equals(Status.MAIN) || key.equals(Status.MOTION)
		|| key.equals(Status.PHONESTATE) || key.equals(Status.POPQUIZ)
		|| key.equals(Status.AUTOSTART)) {
	    prefs = service.getSharedPreferences(SensePrefs.STATUS_PREFS, Context.MODE_PRIVATE);
	} else {
	    prefs = service.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
	}

	// store value
	boolean oldValue = prefs.getBoolean(key, !value);
	if (value != oldValue) {
	    boolean stored = prefs.edit().putBoolean(key, value).commit();
	    if (stored == false) {
		Log.w(TAG, "Preference '" + key + "' not stored!");
	    } else if (key.equals(Advanced.DEV_MODE)
		    && ServiceStateHelper.getInstance(service).isLoggedIn()) {
		logout();
		//reset GCM id
		SharedPreferences authPrefs = service.getSharedPreferences(SensePrefs.AUTH_PREFS,
				Context.MODE_PRIVATE);
		authPrefs.edit().putString(Auth.GCM_REGISTRATION_ID, "").commit();
	    } else if (key.equals(Advanced.USE_COMMONSENSE)) {
		// login on a separate thread
		new Thread() {
		    public void run() {
			if (value) {
			    Log.w(TAG, "USE_COMMONSENSE setting changed: try to log in");
			    service.login();
			} else {
			    Log.w(TAG, "USE_COMMONSENSE setting changed: logging out");
			    service.logout();
			}
		    }
		}.start();
	    }
	}
    }

    @Override
    public void setPrefFloat(String key, float value) throws RemoteException {
	// Log.v(TAG, "Set preference: " + key + ": \'" + value + "\'");
	SharedPreferences prefs = service.getSharedPreferences(SensePrefs.MAIN_PREFS,
		Context.MODE_PRIVATE);

	// store value
	boolean stored = prefs.edit().putFloat(key, value).commit();
	if (stored == false) {
	    Log.w(TAG, "Preference " + key + " not stored!");
	}
    }

    @Override
    public void setPrefInt(String key, int value) throws RemoteException {
	// Log.v(TAG, "Set preference: " + key + ": \'" + value + "\'");
	SharedPreferences prefs = service.getSharedPreferences(SensePrefs.MAIN_PREFS,
		Context.MODE_PRIVATE);

	// store value
	boolean stored = prefs.edit().putFloat(key, value).commit();
	if (stored == false) {
	    Log.w(TAG, "Preference " + key + " not stored!");
	}
    }

    @Override
    public void setPrefLong(String key, long value) throws RemoteException {
	// Log.v(TAG, "Set preference: " + key + ": \'" + value + "\'");
	SharedPreferences prefs;
	if (key.equals(Auth.SENSOR_LIST_COMPLETE_TIME)) {
	    prefs = service.getSharedPreferences(SensePrefs.AUTH_PREFS, Context.MODE_PRIVATE);
	} else {
	    prefs = service.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
	}

	// store value
	boolean stored = prefs.edit().putLong(key, value).commit();
	if (stored == false) {
	    Log.w(TAG, "Preference " + key + " not stored!");
	}
    }

    @Override
    public void setPrefString(String key, String value) throws RemoteException {
	Log.v(TAG, "Set preference: " + key + ": \'" + value + "\'");
	SharedPreferences prefs;
	if (key.equals(Auth.LOGIN_COOKIE) || key.equals(Auth.LOGIN_PASS)
		|| key.equals(Auth.LOGIN_USERNAME) || key.equals(Auth.SENSOR_LIST_COMPLETE)
		|| key.equals(Auth.DEVICE_ID) || key.equals(Auth.PHONE_IMEI)
		|| key.equals(Auth.PHONE_TYPE)) {
	    prefs = service.getSharedPreferences(SensePrefs.AUTH_PREFS, Context.MODE_PRIVATE);
	} else {
	    // all other preferences
	    prefs = service.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
	}

	// store value
	String oldValue = prefs.getString(key, null);
	if (!value.equals(oldValue)) {
	    boolean stored = prefs.edit().putString(key, value).commit();
	    if (stored == false) {
		Log.w(TAG, "Preference " + key + " not stored!");
	    }

	    // special check for sync and sample rate changes
	    if (key.equals(SensePrefs.Main.SAMPLE_RATE)) {
		service.onSampleRateChange();
	    } else if (key.equals(SensePrefs.Main.SYNC_RATE)) {
		service.onSyncRateChange();
	    }
	}
    }

    @Override
    public void toggleAmbience(boolean active) {
	// Log.v(TAG, "Toggle ambience: " + active);
	SharedPreferences prefs = service.getSharedPreferences(SensePrefs.STATUS_PREFS,
		Context.MODE_PRIVATE);
	prefs.edit().putBoolean(Status.AMBIENCE, active).commit();
	service.toggleAmbience(active);
    }

    @Override
    public void toggleDeviceProx(boolean active) {
	// Log.v(TAG, "Toggle neighboring devices: " + active);
	SharedPreferences prefs = service.getSharedPreferences(SensePrefs.STATUS_PREFS,
		Context.MODE_PRIVATE);
	prefs.edit().putBoolean(Status.DEV_PROX, active).commit();
	service.toggleDeviceProx(active);
    }

    @Override
    public void toggleExternalSensors(boolean active) {
	// Log.v(TAG, "Toggle external sensors: " + active);
	SharedPreferences prefs = service.getSharedPreferences(SensePrefs.STATUS_PREFS,
		Context.MODE_PRIVATE);
	prefs.edit().putBoolean(Status.EXTERNAL, active).commit();
	service.toggleExternalSensors(active);
    }

    @Override
    public void toggleLocation(boolean active) {
	// Log.v(TAG, "Toggle location: " + active);
	SharedPreferences prefs = service.getSharedPreferences(SensePrefs.STATUS_PREFS,
		Context.MODE_PRIVATE);
	prefs.edit().putBoolean(Status.LOCATION, active).commit();
	service.toggleLocation(active);
    }

    @Override
    public void toggleMain(boolean active) {
	// Log.v(TAG, "Toggle main: " + active);
	SharedPreferences prefs = service.getSharedPreferences(SensePrefs.STATUS_PREFS,
		Context.MODE_PRIVATE);
	prefs.edit().putBoolean(Status.MAIN, active).commit();
	service.toggleMain(active);
    }

    @Override
    public void toggleMotion(boolean active) {
	// Log.v(TAG, "Toggle motion: " + active);
	SharedPreferences prefs = service.getSharedPreferences(SensePrefs.STATUS_PREFS,
		Context.MODE_PRIVATE);
	prefs.edit().putBoolean(Status.MOTION, active).commit();
	service.toggleMotion(active);
    }

    @Override
    public void togglePhoneState(boolean active) {
	// Log.v(TAG, "Toggle phone state: " + active);
	SharedPreferences prefs = service.getSharedPreferences(SensePrefs.STATUS_PREFS,
		Context.MODE_PRIVATE);
	prefs.edit().putBoolean(Status.PHONESTATE, active).commit();
	service.togglePhoneState(active);
    }

    @Override
    public void togglePopQuiz(boolean active) {
	Log.w(TAG, "Toggle questionnaire ignored: this functionality is no longer supported!");
    }
}
