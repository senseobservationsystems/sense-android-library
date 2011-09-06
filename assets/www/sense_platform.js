function SensePlatform() {

}

SensePlatform.prototype.test = function() {
    console.log('SensePlatform.test()');
    var successCallback = function(result) {
        console.log('Test result: ' + result);
    };
    var failureCallback = function(error) {
        alert('Failed to test: ' + error);
    };
    return PhoneGap.exec(successCallback, failureCallback, 'SensePlatform', 'test', []);
};

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

SensePlatform.STATUSCODE_AMBIENCE = 0x01;
SensePlatform.STATUSCODE_CONNECTED = 0x02;
SensePlatform.STATUSCODE_DEVICE_PROX = 0x04;
SensePlatform.STATUSCODE_EXTERNAL = 0x08;
SensePlatform.STATUSCODE_LOCATION = 0x10;
SensePlatform.STATUSCODE_MOTION = 0x20;
SensePlatform.STATUSCODE_PHONESTATE = 0x40;
SensePlatform.STATUSCODE_QUIZ = 0x80;
SensePlatform.STATUSCODE_RUNNING = 0x100;

PhoneGap.addConstructor(function() {
    PhoneGap.addPlugin("sense", new SensePlatform());
});
