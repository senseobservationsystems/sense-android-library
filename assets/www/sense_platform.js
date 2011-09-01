/**
 * JavaScript implementation of plugin for Sense Platform interaction.
 *
 * @author steven
 */

/**
 * 
 * @return Object literal singleton instance of SensePlatform plugin.
 */
var SensePlatform = {
        
    isBound : false,
    
    /** 
     * Executes "bind" action on the Plugin, if we are not bound yet.
     */ 
    bindToSense : function() {
        if (!this.isBound) {                
        PhoneGap.exec(
                function(r){
                    console.log('bindToSense success: ' + r); 
                    isBound=true;
                    }, 
                function(e){console.log('bindToSense error: ' + e)}, 
                'SensePlatform', 
                'bind', 
                []);
        } else {
            // already bound
            return;
        }
    },

    /**
     * @param authentication
     *            Array with username and password
     * @param successCallback
     *            The callback which will be called when Sense login is successful
     * @param failureCallback
     *            The callback which will be called when Sense login fails
     */
    changeLogin : function(authentication, successCallback, failureCallback) {
        this.bindToSense();
        PhoneGap.exec(successCallback,
                failureCallback, 
                'SensePlatform', 
                'change_login', 
                 authentication ); 
    },

    /**
     * @param authentication
     *            Array with username, password, first name, surname, email, phone number strings
     * @param successCallback
     *            The callback which will be called when Sense registration is successful
     * @param failureCallback
     *            The callback which will be called when Sense registration fails
     */
    register : function(authentication, successCallback, failureCallback) {
        this.bindToSense();
        PhoneGap.exec(successCallback,
                failureCallback, 
                'SensePlatform', 
                'register', 
                 authentication ); 
    },

    /**
     * @param active
     *            boolean to indicate desired state
     * @param successCallback
     *            The callback which will be called when Sense status is successfully changed
     * @param failureCallback
     *            The callback which will be called when execution fails
     */
    toggleMain : function(active, successCallback, failureCallback) {
        this.bindToSense();
        return PhoneGap.exec(successCallback,
                failureCallback, 
                'SensePlatform', 
                'toggle_main', 
                [ active ]); 
    },

    /**
     * @param active
     *            boolean to indicate desired state
     * @param successCallback
     *            The callback which will be called when ambience sensor status is successfully changed
     * @param failureCallback
     *            The callback which will be called when execution fails
     */
    toggleAmbience : function(active, successCallback, failureCallback) {
        this.bindToSense();
        return PhoneGap.exec(successCallback,
                failureCallback, 
                'SensePlatform', 
                'toggle_ambience', 
                [ active ]); 
    },

    /**
     * @param active
     *            boolean to indicate desired state
     * @param successCallback
     *            The callback which will be called when external sensor status is successfully changed
     * @param failureCallback
     *            The callback which will be called when execution fails
     */
    toggleExternal : function(active, successCallback, failureCallback) {
        this.bindToSense();
        return PhoneGap.exec(successCallback,
                failureCallback, 
                'SensePlatform', 
                'toggle_external', 
                [ active ]); 
    },

    /**
     * @param active
     *            boolean to indicate desired state
     * @param successCallback
     *            The callback which will be called when motion sensor status is successfully changed
     * @param failureCallback
     *            The callback which will be called when execution fails
     */
    toggleMotion : function(active, successCallback, failureCallback) {
        this.bindToSense();
        return PhoneGap.exec(successCallback,
                failureCallback, 
                'SensePlatform', 
                'toggle_motion', 
                [ active ]); 
    },

    /**
     * @param active
     *            boolean to indicate desired state
     * @param successCallback
     *            The callback which will be called when neighboring devices sensor status is successfully changed
     * @param failureCallback
     *            The callback which will be called when execution fails
     */
    toggleNeighDev : function(active, successCallback, failureCallback) {
        this.bindToSense();
        return PhoneGap.exec(successCallback,
                failureCallback, 
                'SensePlatform', 
                'toggle_neighdev', 
                [ active ]); 
    },

    /**
     * @param active
     *            boolean to indicate desired state
     * @param successCallback
     *            The callback which will be called when phone state sensor status is successfully changed
     * @param failureCallback
     *            The callback which will be called when execution fails
     */
    togglePhoneState : function(active, successCallback, failureCallback) {
        this.bindToSense();
        return PhoneGap.exec(successCallback,
                failureCallback, 
                'SensePlatform', 
                'toggle_phonestate', 
                [ active ]); 
    }
};