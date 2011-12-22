function SensePlatform() {

}

/**
 * Initializes the Sense Platform plugin. 
 */
SensePlatform.prototype.init = function() {
    var successCallback = function(result) {
        window.plugins.sense.isInitialized = true;
        console.log('Sense plugin initialized');
    };
    var failureCallback = function(error) {
        window.plugins.sense.isInitialized = false;
        console.log('Failed to initialize Sense PhoneGap plugin: ' + error);
    };
    return PhoneGap.exec(successCallback, failureCallback, 'SensePlatform', 'init', []);
};

/**
 * @param username 
 * @param password 
 * @param successCallback
 *            The callback which will be called when Sense login is successful
 * @param failureCallback
 *            The callback which will be called when Sense login fails
 */
SensePlatform.prototype.changeLogin = function(username, password, successCallback, failureCallback) {
    if (!window.plugins.sense.isInitialized) {
        window.plugins.sense.init();
    }
    return PhoneGap.exec(successCallback, failureCallback, 'SensePlatform', 'change_login', [username, password]);
};

/**
 * @param sensorName
 *            String containing sensor name to lookup
 * @param successCallback
 *            The callback which will be called when sensor data was retrieved
 * @param failureCallback
 *            The callback which will be called when execution fails
 *            
 * @return JSONArray containing data
 */
SensePlatform.prototype.getData = function(sensorName, successCallback, failureCallback) {
    if (!window.plugins.sense.isInitialized) {
        window.plugins.sense.init();
    }
    return PhoneGap.exec(successCallback, failureCallback, 'SensePlatform', 'get_data', [sensorName]);
};

/**
 * @param key
 *            String containing preference key to retrieve
 * @param successCallback
 *            The callback which will be called when preference is successfully retrieved
 * @param failureCallback
 *            The callback which will be called when execution fails
 *            
 * @return JSONArray containing data
 */
SensePlatform.prototype.getPref = function(key, successCallback, failureCallback) {
    if (!window.plugins.sense.isInitialized) {
        window.plugins.sense.init();
    }
    return PhoneGap.exec(successCallback, failureCallback, 'SensePlatform', 'get_pref', [key]);
};

/**
 * @param successCallback
 *            The callback which will be called when Sense registration is successful
 * @param failureCallback
 *            The callback which will be called when Sense registration fails
 */
SensePlatform.prototype.getStatus = function(successCallback, failureCallback) {
    if (!window.plugins.sense.isInitialized) {
        window.plugins.sense.init();
    }
    return PhoneGap.exec(successCallback, failureCallback, 'SensePlatform', 'get_status', []);
};

/**
 * @param successCallback
 *            The callback which will be called when Sense registration is successful
 * @param failureCallback
 *            The callback which will be called when Sense registration fails
 */
SensePlatform.prototype.getSessionId = function(successCallback, failureCallback) {
    if (!window.plugins.sense.isInitialized) {
        window.plugins.sense.init();
    }
    return PhoneGap.exec(successCallback, failureCallback, 'SensePlatform', 'get_session', []);
};

/**
 * @param username 
 * @param password 
 * @param firstName 
 * @param surname 
 * @param email 
 * @param phone 
 * @param successCallback
 *            The callback which will be called when Sense registration is successful
 * @param failureCallback
 *            The callback which will be called when Sense registration fails
 */
SensePlatform.prototype.register = function(username, password, firstName, surname, email, phone, successCallback, failureCallback) {
    if (!window.plugins.sense.isInitialized) {
        window.plugins.sense.init();
    }
    return PhoneGap.exec(successCallback, failureCallback, 'SensePlatform', 'register', [username, password, firstName, surname, email, phone]);
};

/**
 * @param key
 *            String containing preference key to set
 * @param value
 *            String containing preference value to set
 * @param successCallback
 *            The callback which will be called when preference is successfully set
 * @param failureCallback
 *            The callback which will be called when execution fails
 *            
 * @return JSONArray containing data
 */
SensePlatform.prototype.setPref = function(key, value, successCallback, failureCallback) {
    if (!window.plugins.sense.isInitialized) {
        window.plugins.sense.init();
    }
    return PhoneGap.exec(successCallback, failureCallback, 'SensePlatform', 'set_pref', [key, value]);
};

/**
 * @param active
 *            boolean to indicate desired state
 * @param successCallback
 *            The callback which will be called when Sense status is successfully changed
 * @param failureCallback
 *            The callback which will be called when execution fails
 */
SensePlatform.prototype.toggleMain = function(active, successCallback, failureCallback) {
    if (!window.plugins.sense.isInitialized) {
        window.plugins.sense.init();
    }
    return PhoneGap.exec(successCallback, failureCallback, 'SensePlatform', 'toggle_main', [active]);
};

/**
 * @param active
 *            boolean to indicate desired state
 * @param successCallback
 *            The callback which will be called when ambience sensor status is successfully changed
 * @param failureCallback
 *            The callback which will be called when execution fails
 */
SensePlatform.prototype.toggleAmbience = function(active, successCallback, failureCallback) {
    if (!window.plugins.sense.isInitialized) {
        window.plugins.sense.init();
    }
    return PhoneGap.exec(successCallback, failureCallback, 'SensePlatform', 'toggle_ambience', [active]);
};

/**
 * @param active
 *            boolean to indicate desired state
 * @param successCallback
 *            The callback which will be called when external sensor status is successfully changed
 * @param failureCallback
 *            The callback which will be called when execution fails
 */
SensePlatform.prototype.toggleExternal = function(active, successCallback, failureCallback) {
    if (!window.plugins.sense.isInitialized) {
        window.plugins.sense.init();
    }
    return PhoneGap.exec(successCallback, failureCallback, 'SensePlatform', 'toggle_external', [active]);
};

/**
 * @param active
 *            boolean to indicate desired state
 * @param successCallback
 *            The callback which will be called when motion sensor status is successfully changed
 * @param failureCallback
 *            The callback which will be called when execution fails
 */
SensePlatform.prototype.togglePosition = function(active, successCallback, failureCallback) {
    if (!window.plugins.sense.isInitialized) {
        window.plugins.sense.init();
    }
    return PhoneGap.exec(successCallback, failureCallback, 'SensePlatform', 'toggle_position', [active]);
};

/**
 * @param active
 *            boolean to indicate desired state
 * @param successCallback
 *            The callback which will be called when motion sensor status is successfully changed
 * @param failureCallback
 *            The callback which will be called when execution fails
 */
SensePlatform.prototype.toggleMotion = function(active, successCallback, failureCallback) {
    if (!window.plugins.sense.isInitialized) {
        window.plugins.sense.init();
    }
    return PhoneGap.exec(successCallback, failureCallback, 'SensePlatform', 'toggle_motion', [active]);
};

/**
 * @param active
 *            boolean to indicate desired state
 * @param successCallback
 *            The callback which will be called when neighboring devices sensor status is successfully changed
 * @param failureCallback
 *            The callback which will be called when execution fails
 */
SensePlatform.prototype.toggleNeighDev = function(active, successCallback, failureCallback) {
    if (!window.plugins.sense.isInitialized) {
        window.plugins.sense.init();
    }
    return PhoneGap.exec(successCallback, failureCallback, 'SensePlatform', 'toggle_neighdev', [active]);
};

/**
 * @param active
 *            boolean to indicate desired state
 * @param successCallback
 *            The callback which will be called when phone state sensor status is successfully changed
 * @param failureCallback
 *            The callback which will be called when execution fails
 */
SensePlatform.prototype.togglePhoneState = function(active, successCallback, failureCallback) {
    if (!window.plugins.sense.isInitialized) {
        window.plugins.sense.init();
    }
    return PhoneGap.exec(successCallback, failureCallback, 'SensePlatform', 'toggle_phonestate', [active]);
};

/* status codes */
SensePlatform.STATUSCODE_AMBIENCE = 0x01;
SensePlatform.STATUSCODE_CONNECTED = 0x02;
SensePlatform.STATUSCODE_DEVICE_PROX = 0x04;
SensePlatform.STATUSCODE_EXTERNAL = 0x08;
SensePlatform.STATUSCODE_LOCATION = 0x10;
SensePlatform.STATUSCODE_MOTION = 0x20;
SensePlatform.STATUSCODE_PHONESTATE = 0x40;
SensePlatform.STATUSCODE_QUIZ = 0x80;
SensePlatform.STATUSCODE_RUNNING = 0x100;

/* keys for main prefs */
SensePlatform.PREF_LOGIN_USERNAME = "login_mail";
SensePlatform.PREF_SAMPLE_RATE = "commonsense_rate";
SensePlatform.PREF_SYNC_RATE = "sync_rate";
SensePlatform.PREF_AUTOSTART = "autostart";

/* keys for advanced prefs */
SensePlatform.PREF_USE_COMMONSENSE = "use_commonsense";
SensePlatform.PREF_COMPRESS = "compression";

/* keys for ambience sensor prefs */
SensePlatform.PREF_LIGHT = "ambience_light";
SensePlatform.PREF_MIC = "ambience_mic";

/* keys for device proximity sensor prefs */
SensePlatform.PREF_BLUETOOTH = "proximity_bt";
SensePlatform.PREF_WIFI = "proximity_wifi";

/* keys for location sensor prefs */
SensePlatform.PREF_GPS = "location_gps";
SensePlatform.PREF_NETWORK = "location_network";
SensePlatform.PREF_AUTO_GPS = "automatic_gps";

/* keys for motion sensor prefs */
SensePlatform.PREF_FALL_DETECT = "motion_fall_detector";
SensePlatform.PREF_FALL_DETECT_DEMO = "motion_fall_detector_demo";
SensePlatform.PREF_EPIMODE = "epimode";
SensePlatform.PREF_UNREG = "motion_unregister";
SensePlatform.PREF_MOTION_ENERGY = "motion_energy";
SensePlatform.PREF_SCREENOFF_FIX = "screenoff_fix";

/* keys for external sensor prefs */
SensePlatform.PREF_MYGLUCOHEALTH = "myglucohealth";
SensePlatform.PREF_TANITASCALE = "tanita_scale";
SensePlatform.PREF_ZEPHYR_BIOHARNESS = "zephyrBioHarness";
SensePlatform.PREF_ZEPHYR_HXM = "zephyrHxM";
SensePlatform.PREF_ODB2DONGLE = "obd2dongle";

PhoneGap.addConstructor(function() {
    PhoneGap.addPlugin("sense", new SensePlatform());
});
