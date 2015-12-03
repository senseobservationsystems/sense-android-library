package nl.sense_os.service;

import nl.sense_os.datastorageengine.DSEConfig;
import nl.sense_os.datastorageengine.DataStorageEngine;
import nl.sense_os.service.commonsense.SenseApi;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Auth;
import nl.sense_os.service.constants.SensePrefs.Main.Advanced;
import nl.sense_os.service.constants.SensePrefs.Main.Motion;
import nl.sense_os.service.constants.SensePrefs.Status;
import nl.sense_os.util.json.EncryptionHelper;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.RemoteException;
import android.util.Log;

/**
 * Implementation of the service's AIDL interface. Very closely linked with {@link SenseService}.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class SenseServiceStub extends Binder {

    private static final String TAG = "SenseServiceStub";
    
    private static final String[] INVISIBLE_LOGS = {Advanced.ENCRYPT_DATABASE_SALT,
    												Advanced.ENCRYPT_CREDENTIAL_SALT};
    
    private SenseService service;

    public SenseServiceStub(SenseService service) {
        super();
        this.service = service;
    }

    public SenseService getSenseService() {
        return service;
    }

    public void changeLogin(final String username, final String password,
            final ISenseServiceCallback callback) {
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

    public boolean getPrefBool(String key, boolean defValue) {
        // Log.v(TAG, "Get preference: " + key);
        SharedPreferences prefs;
        if (key.equals(Status.AMBIENCE) || key.equals(Status.DEV_PROX)
                || key.equals(Status.EXTERNAL) || key.equals(Status.LOCATION)
                || key.equals(Status.MAIN) || key.equals(Status.MOTION)
                || key.equals(Status.PHONESTATE) || key.equals(Status.AUTOSTART)) {
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

    public float getPrefFloat(String key, float defValue) {
        // Log.v(TAG, "Get preference: " + key);
        SharedPreferences prefs = service.getSharedPreferences(SensePrefs.MAIN_PREFS,
                Context.MODE_PRIVATE);
        try {
            return prefs.getFloat(key, defValue);
        } catch (ClassCastException e) {
            return defValue;
        }
    }

    public int getPrefInt(String key, int defValue) {
        // Log.v(TAG, "Get preference: " + key);
        SharedPreferences prefs = service.getSharedPreferences(SensePrefs.MAIN_PREFS,
                Context.MODE_PRIVATE);
        try {
            return prefs.getInt(key, defValue);
        } catch (ClassCastException e) {
            return defValue;
        }
    }

    public long getPrefLong(String key, long defValue) {
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

    public String getPrefString(String key, String defValue) {
        // Log.v(TAG, "Get preference: " + key);
        SharedPreferences prefs;
        if (key.equals(Auth.LOGIN_COOKIE) || key.equals(Auth.LOGIN_PASS) || key.equals(Auth.LOGIN_SESSION_ID)
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
            String value = prefs.getString(key, defValue);

            if ((key.equals(Auth.LOGIN_USERNAME) || key.equals(Auth.LOGIN_PASS) 
                    || key.equals(Auth.LOGIN_COOKIE) || key.equals(Auth.LOGIN_SESSION_ID))
                    && value != defValue) {
		boolean encrypt_credential = service.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE)
                                                 .getBoolean(Advanced.ENCRYPT_CREDENTIAL, false);
                if (encrypt_credential) {
                    EncryptionHelper decryptor = new EncryptionHelper(service);
                    try {
                      value = decryptor.decrypt(value);
                    } catch (EncryptionHelper.EncryptionHelperException e) {
                        Log.w(TAG, "Error decrypting" + key + ". Assume data is not encrypted");
                    }
                }
            }

            return value;
        } catch (ClassCastException e) {
            return defValue;
        }
    }
    
    public String getCookie() throws IllegalAccessException {
    	return SenseApi.getCookie(service);
    }

    public String getSessionId() throws IllegalAccessException {
        return SenseApi.getSessionId(service);
    }

    public void getStatus(ISenseServiceCallback callback) throws RemoteException {
        callback.statusReport(ServiceStateHelper.getInstance(service).getStatusCode());
    }

    public void logout() {
        service.logout();
    }

    public void register(final String username, final String password, final String email,
            final String address, final String zipCode, final String country, final String name,
            final String surname, final String mobile, final ISenseServiceCallback callback) {
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

    public void setPrefBool(String key, final boolean value) {
        Log.v(TAG, "Set preference: '" + key + "': '" + value + "'");

        SharedPreferences prefs;
        if (key.equals(Status.AMBIENCE) || key.equals(Status.DEV_PROX)
                || key.equals(Status.EXTERNAL) || key.equals(Status.LOCATION)
                || key.equals(Status.MAIN) || key.equals(Status.MOTION)
                || key.equals(Status.PHONESTATE) || key.equals(Status.AUTOSTART)) {
            prefs = service.getSharedPreferences(SensePrefs.STATUS_PREFS, Context.MODE_PRIVATE);
        } else {
            prefs = service.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
        }

        // compare with original preference value
        boolean oldValue = prefs.getBoolean(key, !value);
        if (value == oldValue) {
            // value unchanged
            return;
        }

        // store value
        boolean stored = prefs.edit().putBoolean(key, value).commit();
        if (stored == false) {
            Log.w(TAG, "Preference '" + key + "' not stored!");

        } else if (key.equals(Advanced.DEV_MODE)
                && ServiceStateHelper.getInstance(service).isLoggedIn()) {
            logout();
            // reset GCM id
            SharedPreferences authPrefs = service.getSharedPreferences(SensePrefs.AUTH_PREFS,
                    Context.MODE_PRIVATE);
            authPrefs.edit().putString(Auth.GCM_REGISTRATION_ID, "").commit();

        } else if (key.equals(Advanced.USE_COMMONSENSE)) {
            // login on a separate thread
            new Thread() {

                @Override
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

        } else if (key.equals(Motion.ACCELEROMETER) || key.equals(Motion.LINEAR_ACCELERATION)
                || key.equals(Motion.GYROSCOPE) || key.equals(Motion.ORIENTATION)
                || key.equals(Motion.MOTION_ENERGY) || key.equals(Motion.BURSTMODE)
                || key.equals(Motion.FALL_DETECT) || key.equals(Motion.FALL_DETECT_DEMO)) {
            ServiceStateHelper ssh = ServiceStateHelper.getInstance(service);
            if (ssh.isMotionActive()) {
                // restart motion
                service.toggleMotion(false);
                service.toggleMotion(true);
            }
        }
    }

    public void setPrefFloat(String key, float value) {
        // Log.v(TAG, "Set preference: " + key + ": \'" + value + "\'");
        SharedPreferences prefs = service.getSharedPreferences(SensePrefs.MAIN_PREFS,
                Context.MODE_PRIVATE);

        // store value
        boolean stored = prefs.edit().putFloat(key, value).commit();
        if (stored == false) {
            Log.w(TAG, "Preference " + key + " not stored!");
        }
    }

    public void setPrefInt(String key, int value) {
        // Log.v(TAG, "Set preference: " + key + ": \'" + value + "\'");
        SharedPreferences prefs = service.getSharedPreferences(SensePrefs.MAIN_PREFS,
                Context.MODE_PRIVATE);

        // update the retention hours in the DSE
        if(key.equals(SensePrefs.Main.Advanced.RETENTION_HOURS)){
            DataStorageEngine dse = DataStorageEngine.getInstance(service);
            DSEConfig dseConfig = dse.getConfig();
            dseConfig.localPersistancePeriod = value * 60 * 60 * 1000l;
            dse.setConfig(dseConfig);
        }

        // store value
        boolean stored = prefs.edit().putInt(key, value).commit();
        if (stored == false) {
            Log.w(TAG, "Preference " + key + " not stored!");
        }
    }

    public void setPrefLong(String key, long value) {
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

    public void setPrefString(String key, String value) {
    	
    	if(checkValidLogKey(key)){
    		Log.v(TAG, "Set preference: " + key + ": \'" + value + "\'");
    	}
    	
        SharedPreferences prefs;
        if (key.equals(Auth.LOGIN_COOKIE) || key.equals(Auth.LOGIN_PASS) || key.equals(Auth.LOGIN_SESSION_ID)
                || key.equals(Auth.LOGIN_USERNAME) || key.equals(Auth.SENSOR_LIST_COMPLETE)
                || key.equals(Auth.DEVICE_ID) || key.equals(Auth.PHONE_IMEI)
                || key.equals(Auth.PHONE_TYPE)) {
            prefs = service.getSharedPreferences(SensePrefs.AUTH_PREFS, Context.MODE_PRIVATE);
        } else {
            // all other preferences
            prefs = service.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
        }

        boolean encrypt_credential = false;
        if (key.equals(Auth.LOGIN_USERNAME) || key.equals(Auth.LOGIN_PASS) 
            || key.equals(Auth.LOGIN_COOKIE) || key.equals(Auth.LOGIN_SESSION_ID)) {
            encrypt_credential = service.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE)
                                     .getBoolean(Advanced.ENCRYPT_CREDENTIAL, false);
        }

        // store value
        String oldValue = prefs.getString(key, null);

        if (encrypt_credential && oldValue != null) {
            EncryptionHelper decryptor = new EncryptionHelper(service);
            try {
                oldValue = decryptor.decrypt(oldValue);
            } catch (EncryptionHelper.EncryptionHelperException e) {
                Log.w(TAG, "Error decrypting " + key + ". Assume data is not encrypted");
            }
        }

        if (value == null || value != oldValue) {
            if (encrypt_credential && value != null) {
                EncryptionHelper encryptor = new EncryptionHelper(service);
                value = encryptor.encrypt(value);
            }

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
    
    // Returns false for sensitive logs, such as salts
    public boolean checkValidLogKey(String key){
    	    	
    	for(String invalidKey: INVISIBLE_LOGS){
    		
    		if(invalidKey.equals(key)){
    			return false;
    		}
    	}
    	return true;
    }

    public void toggleAmbience(boolean active) {
        // Log.v(TAG, "Toggle ambience: " + active);
        SharedPreferences prefs = service.getSharedPreferences(SensePrefs.STATUS_PREFS,
                Context.MODE_PRIVATE);
        prefs.edit().putBoolean(Status.AMBIENCE, active).commit();
        service.toggleAmbience(active);
    }

    public void toggleDeviceProx(boolean active) {
        // Log.v(TAG, "Toggle neighboring devices: " + active);
        SharedPreferences prefs = service.getSharedPreferences(SensePrefs.STATUS_PREFS,
                Context.MODE_PRIVATE);
        prefs.edit().putBoolean(Status.DEV_PROX, active).commit();
        service.toggleDeviceProx(active);
    }

    public void toggleExternalSensors(boolean active) {
        // Log.v(TAG, "Toggle external sensors: " + active);
        SharedPreferences prefs = service.getSharedPreferences(SensePrefs.STATUS_PREFS,
                Context.MODE_PRIVATE);
        prefs.edit().putBoolean(Status.EXTERNAL, active).commit();
        service.toggleExternalSensors(active);
    }

    public void toggleLocation(boolean active) {
        // Log.v(TAG, "Toggle location: " + active);
        SharedPreferences prefs = service.getSharedPreferences(SensePrefs.STATUS_PREFS,
                Context.MODE_PRIVATE);
        prefs.edit().putBoolean(Status.LOCATION, active).commit();
        service.toggleLocation(active);
    }

    public void toggleMain(boolean active) {
        // Log.v(TAG, "Toggle main: " + active);
        SharedPreferences prefs = service.getSharedPreferences(SensePrefs.STATUS_PREFS,
                Context.MODE_PRIVATE);
        prefs.edit().putBoolean(Status.MAIN, active).commit();
        service.toggleMain(active);
    }

    public void toggleMotion(boolean active) {
        // Log.v(TAG, "Toggle motion: " + active);
        SharedPreferences prefs = service.getSharedPreferences(SensePrefs.STATUS_PREFS,
                Context.MODE_PRIVATE);
        prefs.edit().putBoolean(Status.MOTION, active).commit();
        service.toggleMotion(active);
    }

    public void togglePhoneState(boolean active) {
        // Log.v(TAG, "Toggle phone state: " + active);
        SharedPreferences prefs = service.getSharedPreferences(SensePrefs.STATUS_PREFS,
                Context.MODE_PRIVATE);
        prefs.edit().putBoolean(Status.PHONESTATE, active).commit();
        service.togglePhoneState(active);
    }
}
