function SensePlatform() {

}

/**
 * Initializes the Sense Platform plugin.
 */
SensePlatform.prototype.init = function() {
	var success = function(result) {
		window.plugins.sense.isInitialized = true;
		console.log('Sense plugin initialized');
	};
	var failure = function(error) {
		window.plugins.sense.isInitialized = false;
		console.log('Failed to initialize Sense PhoneGap plugin: ' + error);
	};
	return cordova.exec(success, failure, 'SensePlatform', 'init', []);
};

/**
 * Adds a new data point to one of the Sense user's sensors. The data point will be put in Sense's
 * buffer on the until it is synchronized with CommonSense. The sync rate depends on the
 * application.
 * 
 * @param name
 *            String with name of the sensor that the data belongs to. Typically this should be one
 *            of the standardized sensor names that are used throughout CommonSense.
 * @param displayName
 *            String with pretty display name for the sensor. (Optional)
 * @param description
 *            String with description of the sensor, e.g. the hardware model name. (Optional)
 * @param dataType
 *            String with data type of the sensor. Can be "bool", "float", "int", "json", "string".
 * @param value
 *            String with value for the data point. Take care to escape any slashes and quotes.
 * @param timestamp
 *            Number with timestamp of the data point, in milliseconds since epoch.
 * @param success
 *            The callback which will be called when the point was successfully given to Sense.
 * @param failure
 *            The callback which will be called when Sense login fails
 */
SensePlatform.prototype.addDataPoint = function(name, displayName, description, dataType, value,
		timestamp, success, failure) {
	if (!window.plugins.sense.isInitialized) {
		window.plugins.sense.init();
	}
	return cordova.exec(success, failure, 'SensePlatform', 'add_data_point', [ name, displayName,
			description, dataType, value, timestamp ]);
};

/**
 * @param username
 * @param password
 * @param success
 *            The callback which will be called when Sense login is successful
 * @param failure
 *            The callback which will be called when Sense login fails
 */
SensePlatform.prototype.changeLogin = function(username, password, success, failure) {
	if (!window.plugins.sense.isInitialized) {
		window.plugins.sense.init();
	}
	return cordova.exec(success, failure, 'SensePlatform', 'change_login', [ username, password ]);
};

/**
 * Forces a flush of buffered sensor data to CommonSense. The data is not guaranteed to be removed
 * from the phone, this is done by a separate process.
 */
SensePlatform.prototype.flushBuffer = function(success, failure) {
	return cordova.exec(success, failure, 'SensePlatform', 'flush_buffer', []);
};

/**
 * @param name
 *            String containing sensor name to lookup
 * @param onlyThisDevice
 *            Boolean to specify only to look for sensors that are connected to this specific phone
 * @param success
 *            The callback which will be called when sensor data was retrieved
 * @param failure
 *            The callback which will be called when execution fails
 * 
 * @return JSONArray containing data
 */
SensePlatform.prototype.getRemoteData = function(name, onlyThisDevice, success, failure) {
	if (!window.plugins.sense.isInitialized) {
		window.plugins.sense.init();
	}
	return cordova.exec(success, failure, 'SensePlatform', 'get_commonsense_data', [ name,
			onlyThisDevice ]);
};

/**
 * @param name
 *            String containing sensor name to lookup
 * @param success
 *            The callback which will be called when sensor data was retrieved
 * @param failure
 *            The callback which will be called when execution fails
 * 
 * @return JSONArray containing data
 */
SensePlatform.prototype.getLocalData = function(name, success, failure) {
	if (!window.plugins.sense.isInitialized) {
		window.plugins.sense.init();
	}
	return cordova.exec(success, failure, 'SensePlatform', 'get_data', [ name ]);
};

/**
 * @param name
 *            String containing sensor name to lookup
 * @param success
 *            The callback which will be called when sensor data was retrieved
 * @param failure
 *            The callback which will be called when execution fails
 * 
 * @return JSONArray containing data
 * @deprecated use getLocalData instead
 */
SensePlatform.prototype.getData = function(name, success, failure) {
	return window.plugins.sense.getLocalData(name, success, failure);
};

/**
 * @param key
 *            String containing preference key to retrieve. A list of keys is provided with the
 *            SensePlatform class
 * @param success
 *            Callback which will be called when action was performed successfully
 * @param failure
 *            Callback which will be called when execution fails
 * 
 * @return JSONArray containing data
 */
SensePlatform.prototype.getPref = function(key, success, failure) {
	if (!window.plugins.sense.isInitialized) {
		window.plugins.sense.init();
	}
	return cordova.exec(success, failure, 'SensePlatform', 'get_pref', [ key ]);
};

/**
 * @param success
 *            The callback which will be called when Sense registration is successful
 * @param failure
 *            The callback which will be called when Sense registration fails
 */
SensePlatform.prototype.getStatus = function(success, failure) {
	if (!window.plugins.sense.isInitialized) {
		window.plugins.sense.init();
	}
	return cordova.exec(success, failure, 'SensePlatform', 'get_status', []);
};

/**
 * @param success
 *            The callback which will be called when Sense registration is successful
 * @param failure
 *            The callback which will be called when Sense registration fails
 */
SensePlatform.prototype.getSessionId = function(success, failure) {
	if (!window.plugins.sense.isInitialized) {
		window.plugins.sense.init();
	}
	return cordova.exec(success, failure, 'SensePlatform', 'get_session', []);
};

/**
 * @param username
 * @param password
 * @param success
 *            The callback which will be called when Sense login is successful
 * @param failure
 *            The callback which will be called when Sense login fails
 */
SensePlatform.prototype.logout = function(success, failure) {
	if (!window.plugins.sense.isInitialized) {
		window.plugins.sense.init();
	}
	return cordova.exec(success, failure, 'SensePlatform', 'logout', []);
};

/**
 * Gives feedback for a CommonSense state sensor.
 * 
 * @param name
 *            String with state sensor name
 * @param start
 *            Number with start date of feedback period (in milliseconds since epoch)
 * @param end
 *            Number with end date of feedback period (in milliseconds since epoch)
 * @param label
 *            String with desired label for the feedback period
 * @param success
 *            Callback which will be called when action was performed successfully
 * @param failure
 *            Callback which will be called when execution fails
 */
SensePlatform.prototype.giveFeedback = function(name, start, end, label, success, failure) {
	return cordova.exec(success, failure, 'SensePlatform', 'give_feedback', [ name, start, end,
			label ]);
};

/**
 * @param username
 * @param password
 * @param firstName
 * @param surname
 * @param email
 * @param phone
 * @param success
 *            The callback which will be called when Sense registration is successful
 * @param failure
 *            The callback which will be called when Sense registration fails
 */
SensePlatform.prototype.register = function(username, password, firstName, surname, email, phone,
		success, failure) {
	if (!window.plugins.sense.isInitialized) {
		window.plugins.sense.init();
	}
	return cordova.exec(success, failure, 'SensePlatform', 'register', [ username, password,
			firstName, surname, email, phone ]);
};

/**
 * @param key
 *            String containing preference key to set. A list of valid keys is provided with the
 *            SensePlatform class.
 * @param value
 *            String containing preference value to set
 * @param success
 *            The callback which will be called when preference is successfully set
 * @param failure
 *            The callback which will be called when execution fails
 * 
 * @return JSONArray containing data
 */
SensePlatform.prototype.setPref = function(key, value, success, failure) {
	if (!window.plugins.sense.isInitialized) {
		window.plugins.sense.init();
	}
	return cordova.exec(success, failure, 'SensePlatform', 'set_pref', [ key, value ]);
};

/**
 * @param active
 *            boolean to indicate desired state
 * @param success
 *            The callback which will be called when Sense status is successfully changed
 * @param failure
 *            The callback which will be called when execution fails
 */
SensePlatform.prototype.toggleMain = function(active, success, failure) {
	if (!window.plugins.sense.isInitialized) {
		window.plugins.sense.init();
	}
	return cordova.exec(success, failure, 'SensePlatform', 'toggle_main', [ active ]);
};

/**
 * @param active
 *            boolean to indicate desired state
 * @param success
 *            The callback which will be called when ambience sensor status is successfully changed
 * @param failure
 *            The callback which will be called when execution fails
 */
SensePlatform.prototype.toggleAmbience = function(active, success, failure) {
	if (!window.plugins.sense.isInitialized) {
		window.plugins.sense.init();
	}
	return cordova.exec(success, failure, 'SensePlatform', 'toggle_ambience', [ active ]);
};

/**
 * @param active
 *            boolean to indicate desired state
 * @param success
 *            The callback which will be called when external sensor status is successfully changed
 * @param failure
 *            The callback which will be called when execution fails
 */
SensePlatform.prototype.toggleExternal = function(active, success, failure) {
	if (!window.plugins.sense.isInitialized) {
		window.plugins.sense.init();
	}
	return cordova.exec(success, failure, 'SensePlatform', 'toggle_external', [ active ]);
};

/**
 * @param active
 *            boolean to indicate desired state
 * @param success
 *            The callback which will be called when motion sensor status is successfully changed
 * @param failure
 *            The callback which will be called when execution fails
 */
SensePlatform.prototype.togglePosition = function(active, success, failure) {
	if (!window.plugins.sense.isInitialized) {
		window.plugins.sense.init();
	}
	return cordova.exec(success, failure, 'SensePlatform', 'toggle_position', [ active ]);
};

/**
 * @param active
 *            boolean to indicate desired state
 * @param success
 *            The callback which will be called when motion sensor status is successfully changed
 * @param failure
 *            The callback which will be called when execution fails
 */
SensePlatform.prototype.toggleMotion = function(active, success, failure) {
	if (!window.plugins.sense.isInitialized) {
		window.plugins.sense.init();
	}
	return cordova.exec(success, failure, 'SensePlatform', 'toggle_motion', [ active ]);
};

/**
 * @param active
 *            boolean to indicate desired state
 * @param success
 *            The callback which will be called when neighboring devices sensor status is
 *            successfully changed
 * @param failure
 *            The callback which will be called when execution fails
 */
SensePlatform.prototype.toggleNeighDev = function(active, success, failure) {
	if (!window.plugins.sense.isInitialized) {
		window.plugins.sense.init();
	}
	return cordova.exec(success, failure, 'SensePlatform', 'toggle_neighdev', [ active ]);
};

/**
 * @param active
 *            boolean to indicate desired state
 * @param success
 *            The callback which will be called when phone state sensor status is successfully
 *            changed
 * @param failure
 *            The callback which will be called when execution fails
 */
SensePlatform.prototype.togglePhoneState = function(active, success, failure) {
	if (!window.plugins.sense.isInitialized) {
		window.plugins.sense.init();
	}
	return cordova.exec(success, failure, 'SensePlatform', 'toggle_phonestate', [ active ]);
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

/* sync rate settings */
SensePlatform.SYNC_ECO = "1";
SensePlatform.SYNC_NORMAL = "0";
SensePlatform.SYNC_OFTEN = "-1";
SensePlatform.SYNC_REALTIME = "-2";

/* sample rate settings */
SensePlatform.SAMPLE_RARELY = "1";
SensePlatform.SAMPLE_NORMAL = "0";
SensePlatform.SAMPLE_OFTEN = "-1";
SensePlatform.SAMPLE_REALTIME = "-2";

/* keys for main prefs */
SensePlatform.PREF_LOGIN_USERNAME = "login_mail";
SensePlatform.PREF_SAMPLE_RATE = "commonsense_rate";
SensePlatform.PREF_SYNC_RATE = "sync_rate";
SensePlatform.PREF_AUTOSTART = "autostart";

/* keys for phone state sensors */
SensePlatform.PREF_BATTERY = "phonestate_battery";
SensePlatform.PREF_SCREEN_ACTIVITY = "phonestate_screen_activity";
SensePlatform.PREF_PROXIMITY = "phonestate_proximity";
SensePlatform.PREF_IP_ADDRESS = "phonestate_ip";
SensePlatform.PREF_DATA_CONNECTION = "phonestate_data_connection";
SensePlatform.PREF_UNREAD_MSG = "phonestate_unread_msg";
SensePlatform.PREF_SERVICE_STATE = "phonestate_service_state";
SensePlatform.PREF_SIGNAL_STRENGTH = "phonestate_signal_strength";
SensePlatform.PREF_CALL_STATE = "phonestate_call_state";

/* keys for ambience sensor prefs */
SensePlatform.PREF_AUDIO_SPECTRUM = "ambience_audio_spectrum";
SensePlatform.PREF_CAMERA_LIGHT = "ambience_camera_light";
SensePlatform.PREF_LIGHT = "ambience_light";
SensePlatform.PREF_MIC = "ambience_mic";
SensePlatform.PREF_PRESSURE = "ambience_pressure";
SensePlatform.PREF_TEMPERATURE = "ambience_temperature";

/* keys for device proximity sensor prefs */
SensePlatform.PREF_BLUETOOTH = "proximity_bt";
SensePlatform.PREF_WIFI = "proximity_wifi";
SensePlatform.PREF_NFC = "proximity_nfc";

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
SensePlatform.PREF_OBD2 = "obd2sensor";

/* keys for advanced prefs */
SensePlatform.PREF_DEV_MODE = "devmode";
SensePlatform.PREF_USE_COMMONSENSE = "use_commonsense";
SensePlatform.PREF_COMPRESS = "compression";

/* register the plugin */
if (!window.plugins)
	window.plugins = {};
if (!window.plugins.sense)
	window.plugins.sense = new SensePlatform();
